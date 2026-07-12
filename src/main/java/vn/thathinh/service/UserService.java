package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.UpdateProfileRequest;
import vn.thathinh.dto.UserProfileResponse;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.exception.ResourceNotFoundException;
import vn.thathinh.model.User;
import vn.thathinh.repository.UserRepository;
import vn.thathinh.validation.NicknameValidator;

import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final FileUploadService fileUploadService;
    private final NicknameValidator nicknameValidator;

    public UserProfileResponse getProfile(String userId) {
        return toResponse(findUser(userId));
    }

    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.getNickname() != null && !request.getNickname().equals(user.getNickname())) {
            nicknameValidator.validate(request.getNickname());
            if (userRepository.existsByNicknameAndDeletedFalse(request.getNickname())) {
                throw new BusinessException(ApiCode.NICKNAME_ALREADY_EXISTS);
            }
            user.setNickname(request.getNickname().trim());
        }
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBirthDate() != null) {
            validateMinimumAge(request.getBirthDate());
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getPreferences() != null) {
            var prefs = request.getPreferences();
            int min = prefs.getMinAge();
            int max = prefs.getMaxAge();
            if (min < 18) min = 18;
            if (max < min) max = min;
            if (max > 99) max = 99;
            prefs.setMinAge(min);
            prefs.setMaxAge(max);
            user.setPreferences(prefs);
        }
        if (request.getBio() != null) {
            String trimmed = request.getBio().trim();
            user.setBio(trimmed.isEmpty() ? null : trimmed);
        }
        if (request.getInterests() != null) {
            user.setInterests(normalizeInterests(request.getInterests()));
        }
        userRepository.save(user);
        return toResponse(user);
    }

    public UserProfileResponse uploadAvatar(String userId, MultipartFile file) {
        User user = findUser(userId);
        String url = fileUploadService.uploadAvatar(userId, file);
        user.setAvatarUrl(url);
        userRepository.save(user);
        return toResponse(user);
    }

    private static final int MAX_PHOTOS = 6;

    public UserProfileResponse addPhoto(String userId, MultipartFile file) {
        User user = findUser(userId);
        java.util.List<String> photos = user.getPhotos() != null
                ? new java.util.ArrayList<>(user.getPhotos()) : new java.util.ArrayList<>();
        if (photos.size() >= MAX_PHOTOS) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, "Tối đa " + MAX_PHOTOS + " ảnh");
        }
        String url = fileUploadService.uploadProfilePhoto(userId, file);
        photos.add(url);
        user.setPhotos(photos);
        userRepository.save(user);
        return toResponse(user);
    }

    public UserProfileResponse removePhoto(String userId, String url) {
        User user = findUser(userId);
        if (user.getPhotos() != null) {
            java.util.List<String> photos = new java.util.ArrayList<>(user.getPhotos());
            photos.remove(url);
            user.setPhotos(photos);
            userRepository.save(user);
        }
        return toResponse(user);
    }

    private java.util.List<String> normalizeInterests(java.util.List<String> interests) {
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (String raw : interests) {
            if (raw == null) continue;
            String t = raw.trim();
            if (!t.isEmpty() && t.length() <= 30) seen.add(t);
            if (seen.size() >= 10) break;
        }
        return new java.util.ArrayList<>(seen);
    }

    public User findUser(String userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.USER_NOT_FOUND));
    }

    public java.util.Map<String, User> findUsersByIds(java.util.Collection<String> userIds) {
        java.util.Map<String, User> result = new java.util.HashMap<>();
        for (User u : userRepository.findAllById(userIds)) {
            if (!u.isDeleted()) {
                result.put(u.getId(), u);
            }
        }
        return result;
    }

    public void ensureActiveAndNotBanned(String userId) {
        User user = findUser(userId);
        if (!user.isActive() || user.isBanned()) {
            throw new BusinessException(ApiCode.USER_BANNED);
        }
    }

    public UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .preferences(user.getPreferences())
                .profileComplete(tokenService.isProfileComplete(user))
                .verified(user.isVerified())
                .banned(null)
                .bio(user.getBio())
                .interests(user.getInterests())
                .photos(user.getPhotos())
                .build();
    }

    public UserProfileResponse toAdminResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .preferences(user.getPreferences())
                .profileComplete(tokenService.isProfileComplete(user))
                .verified(user.isVerified())
                .banned(user.isBanned())
                .bio(user.getBio())
                .interests(user.getInterests())
                .photos(user.getPhotos())
                .build();
    }

    public UserProfileResponse toPublicResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .gender(user.getGender())
                .profileComplete(true)
                .verified(true)
                .bio(user.getBio())
                .interests(user.getInterests())
                .photos(user.getPhotos())
                .build();
    }

    private void validateMinimumAge(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < 18) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR);
        }
    }
}
