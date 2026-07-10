package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.FlirtStatus;
import vn.thathinh.constant.FriendshipStatus;
import vn.thathinh.constant.WebSocketMessageType;
import vn.thathinh.dto.ConversationResponse;
import vn.thathinh.dto.FlirtFriendStatusResponse;
import vn.thathinh.dto.FriendRequestResponse;
import vn.thathinh.dto.ImportFlirtHistoryResponse;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.exception.ResourceNotFoundException;
import vn.thathinh.model.Conversation;
import vn.thathinh.model.FlirtMessage;
import vn.thathinh.model.FlirtSession;
import vn.thathinh.model.Friendship;
import vn.thathinh.model.PrivateMessage;
import vn.thathinh.model.User;
import vn.thathinh.repository.ConversationRepository;
import vn.thathinh.repository.FlirtMessageRepository;
import vn.thathinh.repository.FlirtSessionRepository;
import vn.thathinh.repository.FriendshipRepository;
import vn.thathinh.repository.PrivateMessageRepository;
import vn.thathinh.service.realtime.RealtimeEventPublisher;
import vn.thathinh.util.ConversationReadUtils;
import vn.thathinh.util.UserPairUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final ConversationRepository conversationRepository;
    private final FlirtSessionRepository flirtSessionRepository;
    private final FlirtMessageRepository flirtMessageRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final UserService userService;
    private final RealtimeEventPublisher eventPublisher;
    private final BlockService blockService;

    @Transactional
    public FriendRequestResponse requestFromFlirtSession(String sessionId, String userId) {
        FlirtSession session = flirtSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.FLIRT_SESSION_NOT_FOUND));
        if (!userId.equals(session.getUserAId()) && !userId.equals(session.getUserBId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        if (session.getStatus() == FlirtStatus.ENDED || session.getStatus() == FlirtStatus.REPORTED) {
            throw new BusinessException(ApiCode.FLIRT_SESSION_ENDED);
        }
        String partnerId = session.getUserAId().equals(userId) ? session.getUserBId() : session.getUserAId();
        return createRequest(userId, partnerId, sessionId);
    }

    @Transactional
    public FriendRequestResponse createRequest(String requesterId, String partnerId, String sourceSessionId) {
        blockService.ensureNotBlocked(requesterId, partnerId);
        UserPairUtils.UserPair pair = UserPairUtils.sort(requesterId, partnerId);
        // Lấy cả bản ghi đã soft-delete: unique index (userLowId, userHighId) không tính 'deleted',
        // nên phải tái sử dụng bản ghi cũ (declined/đã huỷ kết bạn) thay vì tạo mới -> tránh DuplicateKey.
        Friendship existing = friendshipRepository
                .findByUserLowIdAndUserHighId(pair.lowId(), pair.highId())
                .orElse(null);

        if (existing != null && !existing.isDeleted()) {
            if (existing.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new BusinessException(ApiCode.FRIEND_ALREADY_EXISTS);
            }
            if (existing.getStatus() == FriendshipStatus.PENDING) {
                if (existing.getRequestedBy().equals(requesterId)) {
                    return toRequestResponse(existing, requesterId);
                }
                ConversationResponse conv = acceptRequest(existing.getId(), requesterId);
                Friendship updated = friendshipRepository.findById(existing.getId()).orElseThrow();
                FriendRequestResponse response = toRequestResponse(updated, requesterId);
                response.setConversationId(conv.getId());
                return response;
            }
        }

        Friendship friendship = existing != null ? existing : Friendship.builder()
                .userLowId(pair.lowId())
                .userHighId(pair.highId())
                .build();
        friendship.setDeleted(false);
        friendship.setDeletedAt(null);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendship.setRequestedBy(requesterId);
        friendship.setSourceSessionId(sourceSessionId);
        friendship.setAcceptedAt(null);
        friendshipRepository.save(friendship);

        FriendRequestResponse response = toRequestResponse(friendship, requesterId);
        eventPublisher.publishFriendEvent(partnerId, WebSocketMessageType.FRIEND_REQUEST, response);
        return response;
    }

    public List<FriendRequestResponse> listIncomingRequests(String userId) {
        List<Friendship> incoming = new ArrayList<>();
        incoming.addAll(friendshipRepository
                .findByDeletedFalseAndStatusAndUserLowIdAndRequestedByNot(
                        FriendshipStatus.PENDING, userId, userId));
        incoming.addAll(friendshipRepository
                .findByDeletedFalseAndStatusAndUserHighIdAndRequestedByNot(
                        FriendshipStatus.PENDING, userId, userId));
        return incoming.stream()
                .map(f -> toRequestResponse(f, userId))
                .sorted(Comparator.comparing(FriendRequestResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponse acceptRequest(String friendshipId, String userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.FRIEND_REQUEST_NOT_FOUND));

        if (!userId.equals(friendship.getUserLowId()) && !userId.equals(friendship.getUserHighId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        if (userId.equals(friendship.getRequestedBy())) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setAcceptedAt(Instant.now());
        friendshipRepository.save(friendship);

        Conversation conversation = getOrReviveConversation(
                friendship.getUserLowId(), friendship.getUserHighId(), friendship.getSourceSessionId());

        conversationServiceImportFlirtHistory(conversation, userId);

        Conversation freshConversation = conversationRepository.findById(conversation.getId()).orElse(conversation);
        ConversationResponse convResponse = toConversationResponse(freshConversation, userId);
        String partnerId = UserPairUtils.partnerId(
                friendship.getUserLowId(), friendship.getUserHighId(), userId);

        eventPublisher.publishFriendEvent(userId, WebSocketMessageType.FRIEND_ACCEPTED, convResponse);
        eventPublisher.publishFriendEvent(partnerId, WebSocketMessageType.FRIEND_ACCEPTED,
                toConversationResponse(freshConversation, partnerId));
        eventPublisher.publishFriendEvent(userId, WebSocketMessageType.CONVERSATION_UPDATED, convResponse);
        eventPublisher.publishFriendEvent(partnerId, WebSocketMessageType.CONVERSATION_UPDATED,
                toConversationResponse(freshConversation, partnerId));

        return convResponse;
    }

    @Transactional
    public ImportFlirtHistoryResponse importFlirtHistoryForConversation(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findByIdAndDeletedFalse(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CONVERSATION_NOT_FOUND));
        if (!userId.equals(conversation.getUserLowId()) && !userId.equals(conversation.getUserHighId())) {
            throw new BusinessException(ApiCode.CONVERSATION_NOT_PARTICIPANT);
        }
        String partnerId = UserPairUtils.partnerId(conversation.getUserLowId(), conversation.getUserHighId(), userId);
        ensureFriends(userId, partnerId);
        return conversationServiceImportFlirtHistory(conversation, userId);
    }

    @Transactional
    public ImportFlirtHistoryResponse importFlirtHistoryFromSession(String sessionId, String userId) {
        FlirtSession session = flirtSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.FLIRT_SESSION_NOT_FOUND));
        if (!userId.equals(session.getUserAId()) && !userId.equals(session.getUserBId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        String partnerId = session.getUserAId().equals(userId) ? session.getUserBId() : session.getUserAId();
        ensureFriends(userId, partnerId);
        UserPairUtils.UserPair pair = UserPairUtils.sort(userId, partnerId);
        Conversation conversation = conversationRepository
                .findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.CONVERSATION_NOT_FOUND));
        if (conversation.getSourceSessionId() == null) {
            conversation.setSourceSessionId(sessionId);
            conversationRepository.save(conversation);
        }
        return conversationServiceImportFlirtHistory(conversation, userId);
    }

    private Conversation getOrReviveConversation(String lowId, String highId, String sourceSessionId) {
        // Tái sử dụng conversation cũ (kể cả đã soft-delete) để không đụng unique index user_pair.
        return conversationRepository.findByUserLowIdAndUserHighId(lowId, highId)
                .map(c -> {
                    if (c.isDeleted()) {
                        // Kết bạn lại sau khi đã huỷ: bắt đầu hội thoại MỚI hoàn toàn —
                        // xoá tin nhắn cũ và reset trạng thái để không hiện lại lịch sử cũ.
                        privateMessageRepository.deleteByConversationId(c.getId());
                        c.setDeleted(false);
                        c.setDeletedAt(null);
                        c.setLastMessagePreview(null);
                        c.setLastMessageAt(null);
                        c.setLastSenderId(null);
                        c.setFlirtHistoryImported(false);
                        c.setSourceSessionId(sourceSessionId);
                        if (c.getLastReadAtByUser() != null) c.getLastReadAtByUser().clear();
                        if (c.getMutedUserIds() != null) c.getMutedUserIds().clear();
                        return conversationRepository.save(c);
                    }
                    return c;
                })
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .userLowId(lowId)
                        .userHighId(highId)
                        .sourceSessionId(sourceSessionId)
                        .build()));
    }

    private ImportFlirtHistoryResponse conversationServiceImportFlirtHistory(Conversation conversation, String userId) {
        if (conversation.isFlirtHistoryImported()) {
            return ImportFlirtHistoryResponse.builder().importedCount(0).alreadyImported(true).build();
        }
        String sessionId = conversation.getSourceSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(ApiCode.FLIRT_HISTORY_NOT_AVAILABLE);
        }
        List<FlirtMessage> flirtMessages = flirtMessageRepository.findBySessionIdOrderBySentAtAsc(sessionId);
        if (flirtMessages.isEmpty()) {
            conversation.setFlirtHistoryImported(true);
            conversationRepository.save(conversation);
            return ImportFlirtHistoryResponse.builder().importedCount(0).alreadyImported(false).build();
        }

        for (FlirtMessage flirtMessage : flirtMessages) {
            User sender = userService.findUser(flirtMessage.getSenderId());
            privateMessageRepository.save(PrivateMessage.builder()
                    .conversationId(conversation.getId())
                    .senderId(flirtMessage.getSenderId())
                    .senderNickname(sender.getNickname())
                    .senderAvatarUrl(sender.getAvatarUrl())
                    .content(flirtMessage.getContent())
                    .sentAt(flirtMessage.getSentAt())
                    .build());
        }

        FlirtMessage last = flirtMessages.get(flirtMessages.size() - 1);
        conversation.setFlirtHistoryImported(true);
        conversation.setLastMessagePreview(truncate(last.getContent()));
        conversation.setLastMessageAt(last.getSentAt());
        conversation.setLastSenderId(last.getSenderId());
        conversationRepository.save(conversation);

        return ImportFlirtHistoryResponse.builder()
                .importedCount(flirtMessages.size())
                .alreadyImported(false)
                .build();
    }

    private String truncate(String content) {
        if (content == null) return "";
        return content.length() > 80 ? content.substring(0, 80) + "…" : content;
    }

    @Transactional
    public void declineRequest(String friendshipId, String userId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.FRIEND_REQUEST_NOT_FOUND));
        if (!userId.equals(friendship.getUserLowId()) && !userId.equals(friendship.getUserHighId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        friendship.setStatus(FriendshipStatus.DECLINED);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void unfriend(String userId, String partnerId) {
        UserPairUtils.UserPair pair = UserPairUtils.sort(userId, partnerId);
        Friendship friendship = friendshipRepository
                .findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.FRIENDSHIP_NOT_FOUND));
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new BusinessException(ApiCode.FRIEND_NOT_ACCEPTED);
        }
        friendship.setDeleted(true);
        friendship.setDeletedAt(Instant.now());
        friendshipRepository.save(friendship);

        conversationRepository.findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                .ifPresent(conversation -> {
                    conversation.setDeleted(true);
                    conversation.setDeletedAt(Instant.now());
                    conversationRepository.save(conversation);
                });
    }

    @Transactional
    public void blockUser(String userId, String partnerId) {
        unfriendIfExists(userId, partnerId);
        blockService.blockUser(userId, partnerId);
    }

    @Transactional
    public void unblockUser(String userId, String partnerId) {
        blockService.unblockUser(userId, partnerId);
    }

    private void unfriendIfExists(String userId, String partnerId) {
        UserPairUtils.UserPair pair = UserPairUtils.sort(userId, partnerId);
        friendshipRepository.findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                .filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED)
                .ifPresent(f -> {
                    f.setDeleted(true);
                    f.setDeletedAt(Instant.now());
                    friendshipRepository.save(f);
                    conversationRepository.findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                            .ifPresent(c -> {
                                c.setDeleted(true);
                                c.setDeletedAt(Instant.now());
                                conversationRepository.save(c);
                            });
                });
    }

    public FriendshipStatus getFriendshipStatus(String userId, String partnerId) {
        UserPairUtils.UserPair pair = UserPairUtils.sort(userId, partnerId);
        return friendshipRepository.findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                .map(Friendship::getStatus)
                .orElse(null);
    }

    /** Trạng thái bạn bè giữa {@code userId} và tất cả người liên quan (partnerId -> status). */
    public java.util.Map<String, FriendshipStatus> getFriendshipStatuses(String userId) {
        java.util.Map<String, FriendshipStatus> result = new java.util.HashMap<>();
        for (Friendship f : friendshipRepository.findAllActiveForUser(userId)) {
            String partnerId = userId.equals(f.getUserLowId()) ? f.getUserHighId() : f.getUserLowId();
            result.put(partnerId, f.getStatus());
        }
        return result;
    }

    public void ensureFriends(String userId, String partnerId) {
        blockService.ensureNotBlocked(userId, partnerId);
        FriendshipStatus status = getFriendshipStatus(userId, partnerId);
        if (status != FriendshipStatus.ACCEPTED) {
            throw new BusinessException(ApiCode.FRIEND_NOT_ACCEPTED);
        }
    }

    public FlirtFriendStatusResponse getFlirtFriendStatus(String sessionId, String userId) {
        FlirtSession session = flirtSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.FLIRT_SESSION_NOT_FOUND));
        if (!userId.equals(session.getUserAId()) && !userId.equals(session.getUserBId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        String partnerId = session.getUserAId().equals(userId) ? session.getUserBId() : session.getUserAId();
        UserPairUtils.UserPair pair = UserPairUtils.sort(userId, partnerId);

        FlirtFriendStatusResponse.FlirtFriendStatusResponseBuilder builder = FlirtFriendStatusResponse.builder()
                .partnerId(partnerId);
        friendshipRepository.findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                .ifPresent(f -> {
                    builder.status(f.getStatus())
                            .friendshipId(f.getId())
                            .requestedByMe(userId.equals(f.getRequestedBy()));
                    if (f.getStatus() == FriendshipStatus.PENDING && !userId.equals(f.getRequestedBy())) {
                        builder.incomingRequestId(f.getId());
                    }
                });
        conversationRepository.findByUserLowIdAndUserHighIdAndDeletedFalse(pair.lowId(), pair.highId())
                .ifPresent(c -> builder.conversationId(c.getId()).flirtHistoryImported(c.isFlirtHistoryImported()));
        return builder.build();
    }

    public boolean isParticipant(String conversationId, String userId) {
        return conversationRepository.findByIdAndDeletedFalse(conversationId)
                .map(c -> userId.equals(c.getUserLowId()) || userId.equals(c.getUserHighId()))
                .orElse(false);
    }

    private FriendRequestResponse toRequestResponse(Friendship f, String viewerId) {
        String requesterId = f.getRequestedBy();
        User requester = userService.findUser(requesterId);
        return FriendRequestResponse.builder()
                .id(f.getId())
                .requesterId(requesterId)
                .requesterNickname(requester.getNickname())
                .requesterAvatarUrl(requester.getAvatarUrl())
                .status(f.getStatus())
                .sourceSessionId(f.getSourceSessionId())
                .createdAt(f.getCreatedAt())
                .build();
    }

    private ConversationResponse toConversationResponse(Conversation c, String userId) {
        String partnerId = UserPairUtils.partnerId(c.getUserLowId(), c.getUserHighId(), userId);
        User partner = userService.findUser(partnerId);
        FriendshipStatus status = getFriendshipStatus(userId, partnerId);
        return ConversationResponse.builder()
                .id(c.getId())
                .partnerId(partnerId)
                .partnerNickname(partner.getNickname())
                .partnerAvatarUrl(partner.getAvatarUrl())
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageAt(c.getLastMessageAt())
                .friendshipStatus(status != null ? status.name() : null)
                .flirtHistoryImported(c.isFlirtHistoryImported())
                .blockedByMe(blockService.hasBlocked(userId, partnerId))
                .blockedByPartner(blockService.hasBlocked(partnerId, userId))
                .unread(ConversationReadUtils.hasUnread(c, userId))
                .build();
    }
}
