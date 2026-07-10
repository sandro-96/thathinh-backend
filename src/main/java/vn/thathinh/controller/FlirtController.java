package vn.thathinh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.*;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.FlirtService;
import vn.thathinh.service.FriendService;

import java.util.List;

@RestController
@RequestMapping("/api/flirt")
@RequiredArgsConstructor
public class FlirtController {

    private final FlirtService flirtService;
    private final FriendService friendService;

    @PostMapping("/start")
    public ApiResponseDto<FlirtStatusResponse> start(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, flirtService.start(user.getUserId()));
    }

    @PostMapping("/cancel")
    public ApiResponseDto<FlirtStatusResponse> cancel(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, flirtService.cancel(user.getUserId()));
    }

    @GetMapping("/status")
    public ApiResponseDto<FlirtStatusResponse> status(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, flirtService.getStatus(user.getUserId()));
    }

    @GetMapping("/history")
    public ApiResponseDto<List<FlirtSessionHistoryResponse>> history(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponseDto.success(ApiCode.SUCCESS, flirtService.listHistory(user.getUserId(), limit));
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponseDto<List<MessageResponse>> messages(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                flirtService.getMessages(sessionId, user.getUserId(), cursor, Math.min(limit, 100)));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponseDto<MessageResponse> send(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid SendMessageRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                flirtService.sendMessage(sessionId, user.getUserId(), request));
    }

    @PostMapping("/{sessionId}/messages/image")
    public ApiResponseDto<MessageResponse> sendImage(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "replyToId", required = false) String replyToId) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                flirtService.sendImageMessage(sessionId, user.getUserId(), file, content, replyToId));
    }

    @PostMapping("/{sessionId}/messages/{messageId}/reactions")
    public ApiResponseDto<MessageResponse> react(
            @PathVariable String sessionId,
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid ReactionRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                flirtService.reactToMessage(sessionId, user.getUserId(), messageId, request.getEmoji()));
    }

    @DeleteMapping("/{sessionId}/messages/{messageId}")
    public ApiResponseDto<MessageResponse> deleteMessage(
            @PathVariable String sessionId,
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                flirtService.deleteMessage(sessionId, user.getUserId(), messageId));
    }

    @PostMapping("/{sessionId}/typing")
    public ApiResponseDto<Void> typing(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        flirtService.notifyTyping(sessionId, user.getUserId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{sessionId}/end")
    public ApiResponseDto<Void> end(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        flirtService.endSession(sessionId, user.getUserId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{sessionId}/report")
    public ApiResponseDto<Void> report(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid ReportRequest request) {
        flirtService.reportSession(sessionId, user.getUserId(), request);
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{sessionId}/friend-request")
    public ApiResponseDto<FriendRequestResponse> friendRequest(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                friendService.requestFromFlirtSession(sessionId, user.getUserId()));
    }

    @GetMapping("/{sessionId}/friend-status")
    public ApiResponseDto<FlirtFriendStatusResponse> friendStatus(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                friendService.getFlirtFriendStatus(sessionId, user.getUserId()));
    }

    @PostMapping("/{sessionId}/import-flirt-history")
    public ApiResponseDto<ImportFlirtHistoryResponse> importFlirtHistory(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                friendService.importFlirtHistoryFromSession(sessionId, user.getUserId()));
    }
}
