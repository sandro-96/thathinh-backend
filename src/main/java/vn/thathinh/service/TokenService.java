package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.JwtResponse;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.model.RefreshToken;
import vn.thathinh.model.User;
import vn.thathinh.repository.RefreshTokenRepository;
import vn.thathinh.repository.UserRepository;
import vn.thathinh.security.JwtUtil;

import java.time.Instant;
import java.util.UUID;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public JwtResponse issueTokens(User user) {
        String access = jwtUtil.generateToken(user);
        String refresh = UUID.randomUUID().toString();
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(refresh)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .revoked(false)
                .build());
        return JwtResponse.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .profileComplete(isProfileComplete(user))
                .build();
    }

    public JwtResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new BusinessException(ApiCode.INVALID_TOKEN));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ApiCode.INVALID_TOKEN);
        }
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_FOUND));
        return issueTokens(user);
    }

    public void revoke(String refreshToken) {
        refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    public boolean isProfileComplete(User user) {
        if (user == null) return false;
        if (user.getNickname() == null || user.getNickname().isBlank()) return false;
        if (user.getGender() == null) return false;
        if (user.getBirthDate() == null) return false;
        return Period.between(user.getBirthDate(), LocalDate.now()).getYears() >= 18;
    }

    private final UserRepository userRepository;
}
