package vn.thathinh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.dto.ConversationResponse;
import vn.thathinh.dto.ImportFlirtHistoryResponse;
import vn.thathinh.dto.MessageResponse;
import vn.thathinh.dto.ReactionRequest;
import vn.thathinh.dto.SendMessageRequest;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.ConversationService;
import vn.thathinh.service.FriendService;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final FriendService friendService;

    @GetMapping
    public ApiResponseDto<List<ConversationResponse>> list(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, conversationService.listConversations(user.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponseDto<ConversationResponse> get(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, conversationService.getConversation(id, user.getUserId()));
    }

    @GetMapping("/{id}/messages")
    public ApiResponseDto<List<MessageResponse>> messages(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                conversationService.getMessages(id, user.getUserId(), cursor, Math.min(limit, 100)));
    }

    @PostMapping("/{id}/messages")
    public ApiResponseDto<MessageResponse> send(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid SendMessageRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                conversationService.sendMessage(id, user.getUserId(), request));
    }

    @PostMapping("/{id}/messages/image")
    public ApiResponseDto<MessageResponse> sendImage(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "replyToId", required = false) String replyToId) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                conversationService.sendImageMessage(id, user.getUserId(), file, content, replyToId));
    }

    @PostMapping("/{id}/messages/{messageId}/reactions")
    public ApiResponseDto<MessageResponse> react(
            @PathVariable String id,
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid ReactionRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                conversationService.reactToMessage(id, user.getUserId(), messageId, request.getEmoji()));
    }

    @DeleteMapping("/{id}/messages/{messageId}")
    public ApiResponseDto<MessageResponse> deleteMessage(
            @PathVariable String id,
            @PathVariable String messageId,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                conversationService.deleteMessage(id, user.getUserId(), messageId));
    }

    @PostMapping("/{id}/mute")
    public ApiResponseDto<ConversationResponse> toggleMute(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                conversationService.toggleMute(id, user.getUserId()));
    }

    @PostMapping("/{id}/typing")
    public ApiResponseDto<Void> typing(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        conversationService.notifyTyping(id, user.getUserId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{id}/read")
    public ApiResponseDto<ConversationResponse> markRead(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                conversationService.markAsRead(id, user.getUserId()));
    }

    @PostMapping("/{id}/import-flirt-history")
    public ApiResponseDto<ImportFlirtHistoryResponse> importFlirtHistory(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                friendService.importFlirtHistoryForConversation(id, user.getUserId()));
    }
}
