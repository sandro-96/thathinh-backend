package vn.thathinh.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.*;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.AuthService;
import vn.thathinh.service.RateLimitService;
import vn.thathinh.service.TokenService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final RateLimitService rateLimitService;

    @PostMapping("/register")
    public ApiResponseDto<JwtResponse> register(@RequestBody @Valid RegisterRequest request,
                                                HttpServletRequest httpRequest) {
        rateLimitService.checkAuthRate(clientIp(httpRequest));
        return ApiResponseDto.success(ApiCode.SUCCESS, authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponseDto<JwtResponse> login(@RequestBody @Valid LoginRequest request,
                                             HttpServletRequest httpRequest) {
        rateLimitService.checkAuthRate(clientIp(httpRequest));
        return ApiResponseDto.success(ApiCode.SUCCESS, authService.login(request));
    }

    @PostMapping("/login/google")
    public ApiResponseDto<JwtResponse> loginGoogle(@RequestBody @Valid GoogleLoginRequest request,
                                                   HttpServletRequest httpRequest) {
        rateLimitService.checkAuthRate(clientIp(httpRequest));
        return ApiResponseDto.success(ApiCode.SUCCESS, authService.loginWithGoogle(request));
    }

    @PostMapping("/refresh-token")
    public ApiResponseDto<JwtResponse> refresh(@RequestBody java.util.Map<String, String> body) {
        return ApiResponseDto.success(ApiCode.SUCCESS, tokenService.refresh(body.get("refreshToken")));
    }

    @PostMapping("/logout")
    public ApiResponseDto<Void> logout(@RequestBody java.util.Map<String, String> body) {
        tokenService.revoke(body.get("refreshToken"));
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/forgot-password")
    public ApiResponseDto<Void> forgotPassword(@RequestParam String email, HttpServletRequest httpRequest) {
        rateLimitService.checkAuthRate(clientIp(httpRequest));
        rateLimitService.checkEmailRate(email);
        authService.forgotPassword(email);
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    @PostMapping("/reset-password")
    public ApiResponseDto<Void> resetPassword(@RequestParam String token, @RequestParam String password,
                                              HttpServletRequest httpRequest) {
        rateLimitService.checkAuthRate(clientIp(httpRequest));
        authService.resetPassword(token, password);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @GetMapping("/verify-email")
    public ApiResponseDto<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/resend-verification")
    public ApiResponseDto<Void> resendVerification(@RequestBody java.util.Map<String, String> body,
                                                   HttpServletRequest httpRequest) {
        rateLimitService.checkAuthRate(clientIp(httpRequest));
        rateLimitService.checkEmailRate(body.get("email"));
        authService.resendVerification(body.get("email"));
        return ApiResponseDto.success(ApiCode.EMAIL_SENT);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
