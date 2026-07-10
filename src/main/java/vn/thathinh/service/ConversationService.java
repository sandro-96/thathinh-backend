package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.dto.ConversationResponse;
import vn.thathinh.dto.MessageResponse;
import vn.thathinh.dto.SendMessageRequest;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.exception.ResourceNotFoundException;
import vn.thathinh.model.Conversation;
import vn.thathinh.model.PrivateMessage;
import vn.thathinh.model.User;
import vn.thathinh.repository.ConversationRepository;
import vn.thathinh.repository.PrivateMessageRepository;
import vn.thathinh.service.realtime.RealtimeEventPublisher;
import vn.thathinh.util.ConversationReadUtils;
import vn.thathinh.util.UserPairUtils;
import vn.thathinh.validation.MessageContentValidator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final PrivateMessageRepository messageRepository;
    private final FriendService friendService;
    private final UserService userService;
    private final RealtimeEventPublisher eventPublisher;
    private final MessageContentValidator messageContentValidator;
    private final RateLimitService rateLimitService;
    private final BlockService blockService;
    private final FileUploadService fileUploadService;
    private final UserPresenceService presenceService;
    private final WebPushService webPushService;

    public List<ConversationResponse> listConversations(String userId) {
        List<Conversation> conversations = conversationRepository
                .findByDeletedFalseAndUserLowIdOrDeletedFalseAndUserHighIdOrderByLastMessageAtDesc(
                        userId, userId);
        if (conversations.isEmpty()) {
            return List.of();
        }

        List<String> partnerIds = conversations.stream()
                .map(c -> UserPairUtils.partnerId(c.getUserLowId(), c.getUserHighId(), userId))
                .distinct()
                .collect(Collectors.toList());

        var partners = userService.findUsersByIds(partnerIds);
        var friendshipStatuses = friendService.getFriendshipStatuses(userId);
        var blockedByMe = blockService.blockedIdsBy(userId);
        var blockedMe = blockService.blockerIdsOf(userId);

        return conversations.stream()
                .map(c -> {
                    String partnerId = UserPairUtils.partnerId(c.getUserLowId(), c.getUserHighId(), userId);
                    User partner = partners.get(partnerId);
                    var status = friendshipStatuses.get(partnerId);
                    return ConversationResponse.builder()
                            .id(c.getId())
                            .partnerId(partnerId)
                            .partnerNickname(partner != null ? partner.getNickname() : null)
                            .partnerAvatarUrl(partner != null ? partner.getAvatarUrl() : null)
                            .lastMessagePreview(c.getLastMessagePreview())
                            .lastMessageAt(c.getLastMessageAt())
                            .friendshipStatus(status != null ? status.name() : null)
                            .flirtHistoryImported(c.isFlirtHistoryImported())
                            .blockedByMe(blockedByMe.contains(partnerId))
                            .blockedByPartner(blockedMe.contains(partnerId))
                            .unread(ConversationReadUtils.hasUnread(c, userId))
                            .unreadCount(unreadCount(c, userId))
                            .partnerOnline(presenceService.isOnline(partnerId))
                            .partnerLastSeenAt(presenceService.lastSeenAt(partnerId))
                            .muted(isMutedBy(c, userId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public ConversationResponse getConversation(String conversationId, String userId) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        return toResponse(conversation, userId);
    }

    public List<MessageResponse> getMessages(String conversationId, String userId, String cursor, int limit) {
        getConversationForUser(conversationId, userId);
        List<PrivateMessage> messages;
        if (cursor != null && !cursor.isBlank()) {
            messages = messageRepository.findByConversationIdAndSentAtBeforeOrderBySentAtDesc(
                    conversationId, Instant.parse(cursor), PageRequest.of(0, limit));
        } else {
            messages = messageRepository.findByConversationIdOrderBySentAtDesc(
                    conversationId, PageRequest.of(0, limit));
        }
        Collections.reverse(messages);
        return messages.stream().map(m -> toMessageResponse(m, userId)).collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(String conversationId, String userId, SendMessageRequest request) {
        userService.ensureActiveAndNotBanned(userId);
        Conversation conversation = getConversationForUser(conversationId, userId);
        String partnerId = UserPairUtils.partnerId(
                conversation.getUserLowId(), conversation.getUserHighId(), userId);
        friendService.ensureFriends(userId, partnerId);

        String content = messageContentValidator.validateAndTrim(request.getContent());
        rateLimitService.checkMessageRate(userId);
        User user = userService.findUser(userId);

        PrivateMessage msg = PrivateMessage.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .senderNickname(user.getNickname())
                .senderAvatarUrl(user.getAvatarUrl())
                .content(content)
                .sentAt(Instant.now())
                .build();
        applyReply(msg, conversationId, request.getReplyToId());
        messageRepository.save(msg);

        conversation.setLastMessagePreview(truncate(content));
        conversation.setLastMessageAt(msg.getSentAt());
        conversation.setLastSenderId(userId);
        ConversationReadUtils.markReadAt(conversation, userId, msg.getSentAt());
        conversationRepository.save(conversation);

        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishPrivateMessage(conversationId, response);

        ConversationResponse convA = toResponse(conversation, userId);
        ConversationResponse convB = toResponse(conversation, partnerId);
        eventPublisher.publishFriendEvent(userId, vn.thathinh.constant.WebSocketMessageType.CONVERSATION_UPDATED, convA);
        eventPublisher.publishFriendEvent(partnerId, vn.thathinh.constant.WebSocketMessageType.CONVERSATION_UPDATED, convB);

        if (!isMutedBy(conversation, partnerId)) {
            webPushService.sendToUser(partnerId, "Tin nhắn mới từ " + user.getNickname(),
                    truncate(content), "/chats/" + conversationId, "conv-" + conversationId);
        }

        return response;
    }

    @Transactional
    public MessageResponse sendImageMessage(String conversationId, String userId, MultipartFile file, String caption) {
        return sendImageMessage(conversationId, userId, file, caption, null);
    }

    @Transactional
    public MessageResponse sendImageMessage(String conversationId, String userId, MultipartFile file, String caption, String replyToId) {
        userService.ensureActiveAndNotBanned(userId);
        Conversation conversation = getConversationForUser(conversationId, userId);
        String partnerId = UserPairUtils.partnerId(
                conversation.getUserLowId(), conversation.getUserHighId(), userId);
        friendService.ensureFriends(userId, partnerId);

        rateLimitService.checkMessageRate(userId);
        String imageUrl = fileUploadService.uploadChatImage(userId, file);
        User user = userService.findUser(userId);

        String content = (caption != null && !caption.isBlank())
                ? messageContentValidator.validateAndTrim(caption) : null;

        PrivateMessage msg = PrivateMessage.builder()
                .conversationId(conversationId)
                .senderId(userId)
                .senderNickname(user.getNickname())
                .senderAvatarUrl(user.getAvatarUrl())
                .imageUrl(imageUrl)
                .content(content)
                .sentAt(Instant.now())
                .build();
        applyReply(msg, conversationId, replyToId);
        messageRepository.save(msg);

        conversation.setLastMessagePreview(content != null ? "📷 " + truncate(content) : "📷 Hình ảnh");
        conversation.setLastMessageAt(msg.getSentAt());
        conversation.setLastSenderId(userId);
        ConversationReadUtils.markReadAt(conversation, userId, msg.getSentAt());
        conversationRepository.save(conversation);

        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishPrivateMessage(conversationId, response);

        ConversationResponse convA = toResponse(conversation, userId);
        ConversationResponse convB = toResponse(conversation, partnerId);
        eventPublisher.publishFriendEvent(userId, vn.thathinh.constant.WebSocketMessageType.CONVERSATION_UPDATED, convA);
        eventPublisher.publishFriendEvent(partnerId, vn.thathinh.constant.WebSocketMessageType.CONVERSATION_UPDATED, convB);

        if (!isMutedBy(conversation, partnerId)) {
            webPushService.sendToUser(partnerId, "Tin nhắn mới từ " + user.getNickname(),
                    content != null ? "📷 " + truncate(content) : "📷 Đã gửi một ảnh",
                    "/chats/" + conversationId, "conv-" + conversationId);
        }

        return response;
    }

    @Transactional
    public ConversationResponse markAsRead(String conversationId, String userId) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        if (!ConversationReadUtils.hasUnread(conversation, userId)) {
            return toResponse(conversation, userId);
        }
        ConversationReadUtils.markRead(conversation, userId);
        conversationRepository.save(conversation);
        ConversationResponse response = toResponse(conversation, userId);
        eventPublisher.publishFriendEvent(
                userId, vn.thathinh.constant.WebSocketMessageType.CONVERSATION_UPDATED, response);

        Instant readAt = conversation.getLastReadAtByUser().get(userId);
        eventPublisher.publishPrivateRead(conversationId, Map.of(
                "readerId", userId,
                "readAt", readAt != null ? readAt.toString() : Instant.now().toString()));
        return response;
    }

    private Conversation getConversationForUser(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CONVERSATION_NOT_FOUND));
        if (!userId.equals(conversation.getUserLowId()) && !userId.equals(conversation.getUserHighId())) {
            throw new BusinessException(ApiCode.CONVERSATION_NOT_PARTICIPANT);
        }
        return conversation;
    }

    private ConversationResponse toResponse(Conversation c, String userId) {
        String partnerId = UserPairUtils.partnerId(c.getUserLowId(), c.getUserHighId(), userId);
        User partner = userService.findUser(partnerId);
        var friendshipStatus = friendService.getFriendshipStatus(userId, partnerId);
        return ConversationResponse.builder()
                .id(c.getId())
                .partnerId(partnerId)
                .partnerNickname(partner.getNickname())
                .partnerAvatarUrl(partner.getAvatarUrl())
                .partnerAge(partner.getAge())
                .partnerGender(partner.getGender())
                .partnerBio(partner.getBio())
                .partnerInterests(partner.getInterests())
                .partnerPhotos(partner.getPhotos())
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageAt(c.getLastMessageAt())
                .friendshipStatus(friendshipStatus != null ? friendshipStatus.name() : null)
                .flirtHistoryImported(c.isFlirtHistoryImported())
                .blockedByMe(blockService.hasBlocked(userId, partnerId))
                .blockedByPartner(blockService.hasBlocked(partnerId, userId))
                .unread(ConversationReadUtils.hasUnread(c, userId))
                .unreadCount(unreadCount(c, userId))
                .partnerOnline(presenceService.isOnline(partnerId))
                .partnerLastSeenAt(presenceService.lastSeenAt(partnerId))
                .partnerLastReadAt(c.getLastReadAtByUser() != null ? c.getLastReadAtByUser().get(partnerId) : null)
                .myLastReadAt(c.getLastReadAtByUser() != null ? c.getLastReadAtByUser().get(userId) : null)
                .muted(isMutedBy(c, userId))
                .build();
    }

    private int unreadCount(Conversation c, String userId) {
        if (!ConversationReadUtils.hasUnread(c, userId)) {
            return 0;
        }
        Map<String, Instant> lastRead = c.getLastReadAtByUser();
        Instant readAt = lastRead != null ? lastRead.get(userId) : null;
        long count = (readAt == null)
                ? messageRepository.countByConversationIdAndSenderIdNot(c.getId(), userId)
                : messageRepository.countByConversationIdAndSenderIdNotAndSentAtAfter(c.getId(), userId, readAt);
        return (int) Math.min(count, Integer.MAX_VALUE);
    }

    @Transactional
    public MessageResponse reactToMessage(String conversationId, String userId, String messageId, String emoji) {
        getConversationForUser(conversationId, userId);
        PrivateMessage msg = messageRepository.findById(messageId)
                .filter(m -> conversationId.equals(m.getConversationId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        if (msg.isDeleted()) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        Map<String, Set<String>> reactions = msg.getReactions();
        if (reactions == null) {
            reactions = new HashMap<>();
            msg.setReactions(reactions);
        }
        Set<String> users = reactions.computeIfAbsent(emoji, k -> new HashSet<>());
        if (!users.add(userId)) {
            users.remove(userId);
        }
        if (users.isEmpty()) {
            reactions.remove(emoji);
        }
        messageRepository.save(msg);
        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishPrivateMessageUpdated(conversationId, response);
        return response;
    }

    @Transactional
    public MessageResponse deleteMessage(String conversationId, String userId, String messageId) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        PrivateMessage msg = messageRepository.findById(messageId)
                .filter(m -> conversationId.equals(m.getConversationId()))
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.NOT_FOUND));
        if (!userId.equals(msg.getSenderId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        if (msg.isDeleted()) {
            return toMessageResponse(msg, userId);
        }
        msg.setDeleted(true);
        msg.setDeletedAt(Instant.now());
        msg.setContent(null);
        msg.setImageUrl(null);
        if (msg.getReactions() != null) msg.getReactions().clear();
        messageRepository.save(msg);

        if (isLastMessage(conversation, msg)) {
            conversation.setLastMessagePreview("Tin nhắn đã được thu hồi");
            conversationRepository.save(conversation);
        }

        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishPrivateMessageUpdated(conversationId, response);
        return response;
    }

    @Transactional
    public ConversationResponse toggleMute(String conversationId, String userId) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        Set<String> muted = conversation.getMutedUserIds();
        if (muted == null) {
            muted = new HashSet<>();
            conversation.setMutedUserIds(muted);
        }
        if (!muted.add(userId)) {
            muted.remove(userId);
        }
        conversationRepository.save(conversation);
        return toResponse(conversation, userId);
    }

    private boolean isLastMessage(Conversation c, PrivateMessage msg) {
        return c.getLastMessageAt() != null && msg.getSentAt() != null
                && !msg.getSentAt().isBefore(c.getLastMessageAt());
    }

    private boolean isMutedBy(Conversation c, String userId) {
        return c.getMutedUserIds() != null && c.getMutedUserIds().contains(userId);
    }

    private void applyReply(PrivateMessage msg, String conversationId, String replyToId) {
        if (replyToId == null || replyToId.isBlank()) return;
        messageRepository.findById(replyToId)
                .filter(r -> conversationId.equals(r.getConversationId()) && !r.isDeleted())
                .ifPresent(r -> {
                    msg.setReplyToId(r.getId());
                    msg.setReplyToSenderNickname(r.getSenderNickname());
                    msg.setReplyToPreview(previewOf(r.getContent(), r.getImageUrl()));
                    msg.setReplyToImageUrl(r.getImageUrl());
                });
    }

    private String previewOf(String content, String imageUrl) {
        if (content != null && !content.isBlank()) return truncate(content);
        if (imageUrl != null) return "📷 Hình ảnh";
        return "";
    }

    private Map<String, List<String>> toReactionMap(Map<String, Set<String>> reactions) {
        if (reactions == null || reactions.isEmpty()) return Map.of();
        Map<String, List<String>> out = new LinkedHashMap<>();
        reactions.forEach((emoji, users) -> {
            if (users != null && !users.isEmpty()) out.put(emoji, new ArrayList<>(users));
        });
        return out;
    }

    public void notifyTyping(String conversationId, String userId) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        String partnerId = UserPairUtils.partnerId(
                conversation.getUserLowId(), conversation.getUserHighId(), userId);
        if (blockService.hasBlocked(userId, partnerId) || blockService.hasBlocked(partnerId, userId)) {
            return;
        }
        eventPublisher.publishPrivateTyping(conversationId, Map.of("userId", userId));
    }

    private MessageResponse toMessageResponse(PrivateMessage m, String userId) {
        boolean deleted = m.isDeleted();
        return MessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .senderNickname(m.getSenderNickname())
                .senderAvatarUrl(m.getSenderAvatarUrl())
                .content(deleted ? null : m.getContent())
                .imageUrl(deleted ? null : m.getImageUrl())
                .sentAt(m.getSentAt())
                .mine(userId.equals(m.getSenderId()))
                .replyToId(m.getReplyToId())
                .replyToSenderNickname(m.getReplyToSenderNickname())
                .replyToPreview(m.getReplyToPreview())
                .replyToImageUrl(m.getReplyToImageUrl())
                .reactions(deleted ? Map.of() : toReactionMap(m.getReactions()))
                .deleted(deleted)
                .build();
    }

    private String truncate(String content) {
        if (content == null) return "";
        return content.length() > 80 ? content.substring(0, 80) + "…" : content;
    }
}
