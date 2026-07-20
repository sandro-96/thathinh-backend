package vn.thathinh.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.Gender;
import vn.thathinh.constant.UserRole;
import vn.thathinh.dto.*;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.model.DatingPreferences;
import vn.thathinh.model.User;
import vn.thathinh.repository.UserRepository;
import vn.thathinh.validation.NicknameValidator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final NicknameValidator nicknameValidator;
    private final EmailService emailService;
    private final AvatarService avatarService;

    @Value("${app.google.client-id:}")
    private String googleClientId;

    @Value("${app.email.verification-required:true}")
    private boolean verificationRequired;

    @Value("${app.verification.expiry-hours:24}")
    private int verificationExpiryHours;

    public JwtResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new BusinessException(ApiCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByNicknameAndDeletedFalse(request.getNickname())) {
            throw new BusinessException(ApiCode.NICKNAME_ALREADY_EXISTS);
        }
        validateMinimumAge(request.getBirthDate());
        nicknameValidator.validate(request.getNickname());
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname().trim())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .avatarUrl(avatarService.randomDefault(request.getGender()))
                .preferences(defaultPreferencesFor(request.getGender()))
                .verified(!verificationRequired)
                .role(UserRole.ROLE_USER)
                .active(true)
                .build();
        if (verificationRequired) {
            issueVerificationToken(user);
        }
        userRepository.save(user);
        if (verificationRequired) {
            emailService.sendVerificationEmail(user);
        }
        return tokenService.issueTokens(user);
    }

    public JwtResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_CREDENTIALS));
        if (user.isBanned()) throw new BusinessException(ApiCode.USER_BANNED);
        ensureVerified(user);
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ApiCode.INVALID_CREDENTIALS);
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return tokenService.issueTokens(user);
    }

    public JwtResponse loginWithGoogle(GoogleLoginRequest request) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) throw new BusinessException(ApiCode.INVALID_TOKEN);

            var payload = idToken.getPayload();
            String googleId = payload.getSubject();
            String email = payload.getEmail();

            User user = userRepository.findByGoogleId(googleId)
                    .or(() -> userRepository.findByEmailAndDeletedFalse(email))
                    .orElseGet(() -> {
                        String baseNick = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
                        if (baseNick.length() < 3) {
                            baseNick = "user" + googleId.substring(0, Math.min(8, googleId.length()));
                        }
                        if (baseNick.length() > 20) {
                            baseNick = baseNick.substring(0, 20);
                        }
                        String nick = baseNick;
                        int i = 1;
                        while (userRepository.existsByNicknameAndDeletedFalse(nick)) {
                            nick = baseNick + i++;
                        }
                        String picture = (String) payload.get("picture");
                        return User.builder()
                                .email(email)
                                .googleId(googleId)
                                .nickname(nick)
                                .avatarUrl(picture != null && !picture.isBlank()
                                        ? picture : avatarService.randomDefault(null))
                                .verified(true)
                                .role(UserRole.ROLE_USER)
                                .active(true)
                                .build();
                    });

            if (user.getGoogleId() == null) user.setGoogleId(googleId);
            if (user.isBanned()) throw new BusinessException(ApiCode.USER_BANNED);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            return tokenService.issueTokens(user);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
    }

    public void forgotPassword(String email) {
        userRepository.findByEmailAndDeletedFalse(email).ifPresent(user -> {
            user.setResetToken(UUID.randomUUID().toString());
            user.setResetTokenExpiry(Instant.now().plusSeconds(900));
            userRepository.save(user);
        });
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_TOKEN));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_TOKEN));
        if (user.getVerificationExpiry() == null || user.getVerificationExpiry().isBefore(Instant.now())) {
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
        user.setVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiry(null);
        userRepository.save(user);
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmailAndDeletedFalse(email.toLowerCase().trim())
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        if (user.isVerified()) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        issueVerificationToken(user);
        userRepository.save(user);
        emailService.sendVerificationEmail(user);
    }

    private void issueVerificationToken(User user) {
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationExpiry(Instant.now().plusSeconds(verificationExpiryHours * 3600L));
    }

    private void ensureVerified(User user) {
        if (verificationRequired && !user.isVerified()) {
            throw new BusinessException(ApiCode.EMAIL_NOT_VERIFIED);
        }
    }

    private void validateMinimumAge(LocalDate birthDate) {
        if (birthDate == null || Period.between(birthDate, LocalDate.now()).getYears() < 18) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
    }

    private DatingPreferences defaultPreferencesFor(Gender gender) {
        if (gender == Gender.MALE) {
            return DatingPreferences.builder()
                    .lookingFor(EnumSet.of(Gender.FEMALE))
                    .build();
        }
        if (gender == Gender.FEMALE) {
            return DatingPreferences.builder()
                    .lookingFor(EnumSet.of(Gender.MALE))
                    .build();
        }
        return DatingPreferences.builder().build();
    }
}
