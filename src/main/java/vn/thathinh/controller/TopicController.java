package vn.thathinh.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.*;
import vn.thathinh.security.CustomUserDetails;
import vn.thathinh.service.TopicService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping
    public ApiResponseDto<List<TopicResponse>> list(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) vn.thathinh.constant.TopicType type) {
        String userId = user != null ? user.getUserId() : null;
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.listTopics(userId, search, type));
    }

    @GetMapping("/my")
    public ApiResponseDto<List<TopicResponse>> myTopics(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.myTopics(user.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponseDto<TopicResponse> get(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.getTopic(id, user.getUserId()));
    }

    @GetMapping("/slug/{slug}")
    public ApiResponseDto<TopicResponse> getBySlug(
            @PathVariable String slug,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomUserDetails user) {
        String userId = user != null ? user.getUserId() : null;
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.getTopicBySlug(slug, userId));
    }

    @PostMapping("/{id}/join")
    public ApiResponseDto<TopicResponse> join(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.join(id, user.getUserId()));
    }

    @PostMapping("/{id}/leave")
    public ApiResponseDto<TopicResponse> leave(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.leave(id, user.getUserId()));
    }

    @GetMapping("/{id}/messages")
    public ApiResponseDto<List<MessageResponse>> messages(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponseDto.success(ApiCode.SUCCESS,
                topicService.getMessages(id, user.getUserId(), cursor, Math.min(limit, 100)));
    }

    @PostMapping("/{id}/messages")
    public ApiResponseDto<MessageResponse> send(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody @Valid SendMessageRequest request) {
        return ApiResponseDto.success(ApiCode.SUCCESS, topicService.sendMessage(id, user.getUserId(), request));
    }

    @PostMapping("/{id}/presence")
    public ApiResponseDto<Void> presence(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, String> body) {
        topicService.updatePresence(id, user.getUserId(), body.getOrDefault("action", "heartbeat"));
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }

    @PostMapping("/{id}/typing")
    public ApiResponseDto<Void> typing(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        topicService.notifyTyping(id, user.getUserId());
        return ApiResponseDto.success(ApiCode.SUCCESS);
    }
}
