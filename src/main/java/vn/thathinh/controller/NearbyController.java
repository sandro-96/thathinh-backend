package vn.thathinh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.dto.NearbyUserResponse;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.NearbyService;

import java.util.List;

@RestController
@RequestMapping("/api/nearby")
@RequiredArgsConstructor
public class NearbyController {

    private final NearbyService nearbyService;

    @GetMapping
    public ApiResponseDto<List<NearbyUserResponse>> nearby(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(value = "radiusKm", required = false) Double radiusKm) {
        return ApiResponseDto.success(ApiCode.SUCCESS, nearbyService.findNearby(user.getUserId(), radiusKm));
    }
}
