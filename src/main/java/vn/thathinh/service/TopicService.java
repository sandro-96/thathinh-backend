package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.TopicType;
import vn.thathinh.dto.*;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.exception.ResourceNotFoundException;
import vn.thathinh.model.Topic;
import vn.thathinh.model.TopicMembership;
import vn.thathinh.model.TopicMessage;
import vn.thathinh.model.User;
import vn.thathinh.repository.TopicMembershipRepository;
import vn.thathinh.repository.TopicMessageRepository;
import vn.thathinh.repository.TopicRepository;
import vn.thathinh.service.realtime.RealtimeEventPublisher;
import vn.thathinh.util.SlugUtils;
import vn.thathinh.validation.MessageContentValidator;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicMembershipRepository membershipRepository;
    private final TopicMessageRepository messageRepository;
    private final UserService userService;
    private final RealtimeEventPublisher eventPublisher;
    private final MessageContentValidator messageContentValidator;
    private final RateLimitService rateLimitService;
    private final TopicPresenceService presenceService;

    public List<TopicResponse> listTopics(String userId, String search, TopicType type) {
        List<Topic> topics = topicRepository.findByActiveTrueAndDeletedFalseOrderByMemberCountDesc();
        if (type != null) {
            topics = topics.stream().filter(t -> type.equals(t.getType())).toList();
        }
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            topics = topics.stream()
                    .filter(t -> t.getName().toLowerCase().contains(q)
                            || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q)))
                    .toList();
        }
        Set<String> joinedIds = loadJoinedTopicIds(userId);
        return topics.stream()
                .map(t -> toResponse(t, joinedIds.contains(t.getId())))
                .collect(Collectors.toList());
    }

    public List<TopicResponse> myTopics(String userId) {
        List<TopicMembership> memberships = membershipRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<String> topicIds = memberships.stream().map(TopicMembership::getTopicId).toList();
        Map<String, Topic> topicsById = topicRepository.findByIdInAndDeletedFalse(topicIds).stream()
                .filter(Topic::isActive)
                .collect(Collectors.toMap(Topic::getId, t -> t, (a, b) -> a));
        return memberships.stream()
                .map(m -> topicsById.get(m.getTopicId()))
                .filter(Objects::nonNull)
                .map(t -> toResponse(t, true))
                .collect(Collectors.toList());
    }

    public TopicResponse getTopic(String topicId, String userId) {
        Topic topic = findActiveTopic(topicId);
        return toResponse(topic, userId);
    }

    public TopicResponse getTopicBySlug(String slug, String userId) {
        Topic topic = topicRepository.findBySlugAndDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TOPIC_NOT_FOUND));
        return toResponse(topic, userId);
    }

    @Transactional
    public TopicResponse join(String topicId, String userId) {
        userService.ensureActiveAndNotBanned(userId);
        Topic topic = findActiveTopic(topicId);
        if (membershipRepository.existsByTopicIdAndUserId(topicId, userId)) {
            throw new BusinessException(ApiCode.TOPIC_ALREADY_JOINED);
        }
        membershipRepository.save(TopicMembership.builder()
                .topicId(topicId)
                .userId(userId)
                .joinedAt(Instant.now())
                .build());
        syncMemberCount(topicId);
        topic = findActiveTopic(topicId);
        return toResponse(topic, true);
    }

    @Transactional
    public TopicResponse leave(String topicId, String userId) {
        if (!membershipRepository.existsByTopicIdAndUserId(topicId, userId)) {
            throw new BusinessException(ApiCode.TOPIC_NOT_MEMBER);
        }
        membershipRepository.deleteByTopicIdAndUserId(topicId, userId);
        syncMemberCount(topicId);
        Topic topic = findActiveTopic(topicId);
        return toResponse(topic, false);
    }

    private void syncMemberCount(String topicId) {
        topicRepository.findByIdAndDeletedFalse(topicId).ifPresent(t -> {
            int count = (int) membershipRepository.countByTopicId(topicId);
            if (t.getMemberCount() != count) {
                t.setMemberCount(count);
                topicRepository.save(t);
            }
        });
    }

    private int liveMemberCount(String topicId) {
        return (int) membershipRepository.countByTopicId(topicId);
    }

    private Set<String> loadJoinedTopicIds(String userId) {
        if (userId == null) {
            return Set.of();
        }
        return membershipRepository.findByUserId(userId).stream()
                .map(TopicMembership::getTopicId)
                .collect(Collectors.toSet());
    }

    public List<MessageResponse> getMessages(String topicId, String userId, String cursor, int limit) {
        ensureMember(topicId, userId);
        List<TopicMessage> messages;
        if (cursor != null && !cursor.isBlank()) {
            messages = messageRepository.findByTopicIdAndSentAtBeforeOrderBySentAtDesc(
                    topicId, Instant.parse(cursor), PageRequest.of(0, limit));
        } else {
            messages = messageRepository.findByTopicIdOrderBySentAtDesc(topicId, PageRequest.of(0, limit));
        }
        Collections.reverse(messages);
        return messages.stream().map(m -> toMessageResponse(m, userId)).collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse sendMessage(String topicId, String userId, SendMessageRequest request) {
        userService.ensureActiveAndNotBanned(userId);
        ensureMember(topicId, userId);
        String content = messageContentValidator.validateAndTrim(request.getContent());
        rateLimitService.checkMessageRate(userId);
        User user = userService.findUser(userId);
        TopicMessage msg = TopicMessage.builder()
                .topicId(topicId)
                .senderId(userId)
                .senderNickname(user.getNickname())
                .senderAvatarUrl(user.getAvatarUrl())
                .content(content)
                .sentAt(Instant.now())
                .build();
        messageRepository.save(msg);
        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishTopicMessage(topicId, response);
        return response;
    }

    public Topic createTopic(CreateTopicRequest request) {
        String slug = SlugUtils.toSlug(request.getName());
        String baseSlug = slug;
        int i = 1;
        while (topicRepository.findBySlugAndDeletedFalse(slug).isPresent()) {
            slug = baseSlug + "-" + i++;
        }
        Topic topic = Topic.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .type(request.getType() != null ? request.getType() : TopicType.CUSTOM)
                .slug(slug)
                .coverImageUrl(request.getCoverImageUrl())
                .active(true)
                .memberCount(0)
                .build();
        return topicRepository.save(topic);
    }

    public Topic updateTopic(String id, CreateTopicRequest request) {
        Topic topic = topicRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TOPIC_NOT_FOUND));
        if (request.getName() != null) topic.setName(request.getName().trim());
        if (request.getDescription() != null) topic.setDescription(request.getDescription());
        if (request.getType() != null) topic.setType(request.getType());
        if (request.getCoverImageUrl() != null) topic.setCoverImageUrl(request.getCoverImageUrl());
        return topicRepository.save(topic);
    }

    public void deleteTopic(String id) {
        Topic topic = topicRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TOPIC_NOT_FOUND));
        topic.setActive(false);
        topic.setDeleted(true);
        topic.setDeletedAt(Instant.now());
        topicRepository.save(topic);
    }

    public List<Topic> listAllAdmin() {
        return topicRepository.findAll().stream().filter(t -> !t.isDeleted()).collect(Collectors.toList());
    }

    public boolean isMember(String topicId, String userId) {
        return membershipRepository.existsByTopicIdAndUserId(topicId, userId);
    }

    public void updatePresence(String topicId, String userId, String action) {
        ensureMember(topicId, userId);
        switch (action) {
            case "join" -> presenceService.join(topicId, userId);
            case "leave" -> presenceService.leave(topicId, userId);
            default -> presenceService.heartbeat(topicId, userId);
        }
    }

    public void notifyTyping(String topicId, String userId) {
        ensureMember(topicId, userId);
        User user = userService.findUser(userId);
        presenceService.typing(topicId, userId, user.getNickname());
    }

    private void ensureMember(String topicId, String userId) {
        if (!membershipRepository.existsByTopicIdAndUserId(topicId, userId)) {
            throw new BusinessException(ApiCode.TOPIC_NOT_MEMBER);
        }
    }

    private Topic findActiveTopic(String topicId) {
        Topic topic = topicRepository.findByIdAndDeletedFalse(topicId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.TOPIC_NOT_FOUND));
        if (!topic.isActive()) throw new BusinessException(ApiCode.TOPIC_INACTIVE);
        return topic;
    }

    private TopicResponse toResponse(Topic topic, String userId) {
        boolean joined = userId != null
                && membershipRepository.existsByTopicIdAndUserId(topic.getId(), userId);
        return toResponse(topic, joined);
    }

    private TopicResponse toResponse(Topic topic, boolean joined) {
        return TopicResponse.builder()
                .id(topic.getId())
                .name(topic.getName())
                .description(topic.getDescription())
                .type(topic.getType())
                .slug(topic.getSlug())
                .coverImageUrl(topic.getCoverImageUrl())
                .memberCount(topic.getMemberCount())
                .joined(joined)
                .onlineCount(presenceService.onlineCount(topic.getId()))
                .build();
    }

    private MessageResponse toMessageResponse(TopicMessage m, String userId) {
        return MessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .senderNickname(m.getSenderNickname())
                .senderAvatarUrl(m.getSenderAvatarUrl())
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .mine(userId != null && userId.equals(m.getSenderId()))
                .build();
    }
}
