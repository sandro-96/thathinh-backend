package vn.thathinh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.AccountStatusResponse;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.dto.UpdateProfileRequest;
import vn.thathinh.dto.UserProfileResponse;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.ReportService;
import vn.thathinh.service.UserService;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReportService reportService;

    @GetMapping("/me")
    public ApiResponseDto<UserProfileResponse> me(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, userService.getProfile(user.getUserId()));
    }

    @GetMapping("/me/account-status")
    public ApiResponseDto<AccountStatusResponse> accountStatus(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, reportService.getAccountStatus(user.getUserId()));
    }

    @PutMapping("/me")
    public ApiResponseDto<UserProfileResponse> update(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid UpdateProfileRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, userService.updateProfile(user.getUserId(), request));
    }

    @PostMapping("/me/avatar")
    public ApiResponseDto<UserProfileResponse> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("file") MultipartFile file) {
        return ApiResponseDto.success(ApiCode.SUCCESS, userService.uploadAvatar(user.getUserId(), file));
    }

    @GetMapping("/avatar-presets")
    public ApiResponseDto<java.util.List<vn.thathinh.service.AvatarService.AvatarPreset>> avatarPresets(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, userService.avatarPresets(user.getUserId()));
    }

    @PostMapping("/me/avatar/preset")
    public ApiResponseDto<UserProfileResponse> chooseAvatarPreset(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid vn.thathinh.dto.AvatarPresetRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                userService.chooseAvatarPreset(user.getUserId(), request.getSeed()));
    }

    @PutMapping("/me/location")
    public ApiResponseDto<UserProfileResponse> updateLocation(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid vn.thathinh.dto.UpdateLocationRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                userService.updateLocation(user.getUserId(), request));
    }

    @DeleteMapping("/me/location")
    public ApiResponseDto<UserProfileResponse> disableLocation(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, userService.disableLocation(user.getUserId()));
    }

    @PostMapping("/me/photos")
    public ApiResponseDto<UserProfileResponse> addPhoto(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("file") MultipartFile file) {
        return ApiResponseDto.success(ApiCode.SUCCESS, userService.addPhoto(user.getUserId(), file));
    }

    @DeleteMapping("/me/photos")
    public ApiResponseDto<UserProfileResponse> removePhoto(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("url") String url) {
        return ApiResponseDto.success(ApiCode.SUCCESS, userService.removePhoto(user.getUserId(), url));
    }
}
