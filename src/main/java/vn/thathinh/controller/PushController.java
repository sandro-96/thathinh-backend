package vn.thathinh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.WebPushService;

import java.util.Map;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final WebPushService webPushService;

    @GetMapping("/public-key")
    public ApiResponseDto<Map<String, Object>> publicKey() {
        return ApiResponseDto.success(ApiCode.SUCCESS, Map.of(
                "enabled", webPushService.isEnabled(),
                "publicKey", webPushService.isEnabled() ? webPushService.getPublicKey() : ""));
    }

    @PostMapping("/subscribe")
    @SuppressWarnings("unchecked")
    public ApiResponseDto<Void> subscribe(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {
        String endpoint = (String) body.get("endpoint");
        Map<String, Object> keys = (Map<String, Object>) body.getOrDefault("keys", Map.of());
        webPushService.saveSubscription(
                user.getUserId(), endpoint,
                (String) keys.get("p256dh"), (String) keys.get("auth"));
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/unsubscribe")
    public ApiResponseDto<Void> unsubscribe(@RequestBody Map<String, String> body) {
        webPushService.removeSubscription(body.get("endpoint"));
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
