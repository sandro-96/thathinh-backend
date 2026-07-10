package vn.thathinh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.UserPresenceService;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final UserPresenceService presenceService;

    @PostMapping("/heartbeat")
    public ApiResponseDto<Void> heartbeat(@AuthenticationPrincipal CustomUserDetails user) {
        presenceService.heartbeat(user.getUserId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
