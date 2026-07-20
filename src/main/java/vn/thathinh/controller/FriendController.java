package vn.thathinh.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.thathinh.constant.ApiCode;
import jakarta.validation.Valid;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.dto.BlockedUserResponse;
import vn.thathinh.dto.ConversationResponse;
import vn.thathinh.dto.FriendRequestResponse;
import vn.thathinh.dto.ReportRequest;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.BlockService;
import vn.thathinh.service.FriendService;
import vn.thathinh.service.ReportService;

import java.util.List;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final BlockService blockService;
    private final ReportService reportService;

    @GetMapping("/blocked")
    public ApiResponseDto<List<BlockedUserResponse>> blocked(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, blockService.listBlocked(user.getUserId()));
    }

    @GetMapping("/requests")
    public ApiResponseDto<List<FriendRequestResponse>> incomingRequests(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, friendService.listIncomingRequests(user.getUserId()));
    }

    @PostMapping("/{partnerId}/request")
    public ApiResponseDto<FriendRequestResponse> sendRequest(
            @PathVariable String partnerId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                friendService.createRequest(user.getUserId(), partnerId, null));
    }

    @PostMapping("/requests/{id}/accept")
    public ApiResponseDto<ConversationResponse> accept(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, friendService.acceptRequest(id, user.getUserId()));
    }

    @PostMapping("/requests/{id}/decline")
    public ApiResponseDto<Void> decline(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        friendService.declineRequest(id, user.getUserId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @DeleteMapping("/{partnerId}")
    public ApiResponseDto<Void> unfriend(
            @PathVariable String partnerId,
            @AuthenticationPrincipal CustomUserDetails user) {
        friendService.unfriend(user.getUserId(), partnerId);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{partnerId}/block")
    public ApiResponseDto<Void> block(
            @PathVariable String partnerId,
            @AuthenticationPrincipal CustomUserDetails user) {
        friendService.blockUser(user.getUserId(), partnerId);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @DeleteMapping("/{partnerId}/block")
    public ApiResponseDto<Void> unblock(
            @PathVariable String partnerId,
            @AuthenticationPrincipal CustomUserDetails user) {
        friendService.unblockUser(user.getUserId(), partnerId);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{partnerId}/report")
    public ApiResponseDto<Void> report(
            @PathVariable String partnerId,
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        reportService.reportUser(user.getUserId(), partnerId, request.getReason());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
