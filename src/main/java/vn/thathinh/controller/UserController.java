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
