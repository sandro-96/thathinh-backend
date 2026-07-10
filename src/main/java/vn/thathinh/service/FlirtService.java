package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.FlirtStatus;
import vn.thathinh.constant.WebSocketMessageType;
import vn.thathinh.dto.FlirtSessionHistoryResponse;
import vn.thathinh.dto.FlirtStatusResponse;
import vn.thathinh.dto.MessageResponse;
import vn.thathinh.dto.ReportRequest;
import vn.thathinh.dto.SendMessageRequest;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.exception.ResourceNotFoundException;
import vn.thathinh.lock.DistributedLock;
import vn.thathinh.model.*;
import vn.thathinh.repository.FlirtMessageRepository;
import vn.thathinh.repository.FlirtSessionRepository;
import vn.thathinh.repository.UserReportRepository;
import vn.thathinh.service.flirt.FlirtQueueStore;
import vn.thathinh.service.flirt.WaitingUser;
import vn.thathinh.service.realtime.RealtimeEventPublisher;
import vn.thathinh.validation.MessageContentValidator;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlirtService {

    private final FlirtSessionRepository sessionRepository;
    private final FlirtMessageRepository messageRepository;
    private final UserReportRepository reportRepository;
    private final UserService userService;
    private final RealtimeEventPublisher eventPublisher;
    private final TokenService tokenService;
    private final MessageContentValidator messageContentValidator;
    private final RateLimitService rateLimitService;
    private final BlockService blockService;
    private final FlirtQueueStore queueStore;
    private final DistributedLock distributedLock;
    private final FileUploadService fileUploadService;
    private final WebPushService webPushService;

    @Value("${app.flirt.cooldown-hours:24}")
    private int cooldownHours;

    @Value("${app.flirt.match-timeout-seconds:60}")
    private int matchTimeoutSeconds;

    private static final String MATCH_LOCK_KEY = "flirt:match";
    private static final Duration MATCH_LOCK_TTL = Duration.ofSeconds(10);

    public FlirtStatusResponse start(String userId) {
        User user = userService.findUser(userId);
        if (!user.isActive() || user.isBanned()) throw new BusinessException(ApiCode.USER_BANNED);
        if (!tokenService.isProfileComplete(user)) {
            throw new BusinessException(ApiCode.PROFILE_INCOMPLETE);
        }

        Optional<FlirtSession> active = findActiveSession(userId);
        if (active.isPresent()) {
            FlirtSession session = reconcileSessionWithReport(active.get());
            if (isOngoingFlirt(session.getStatus())) {
                return toStatusResponse(session, userId);
            }
        }
        if (queueStore.contains(userId)) {
            return toWaitingResponse(userId);
        }

        rateLimitService.checkFlirtStartRate(userId);

        DatingPreferences prefs = user.getPreferences() != null
                ? user.getPreferences() : DatingPreferences.builder().build();

        WaitingUser waiting = new WaitingUser(userId, user.getGender(), user.getAge(), prefs, Instant.now());

        return distributedLock.runLocked(MATCH_LOCK_KEY, MATCH_LOCK_TTL, () -> {
            if (queueStore.contains(userId)) {
                return toWaitingResponse(userId);
            }
            queueStore.put(waiting);

            return tryMatchLocked(waiting)
                    .map(session -> toStatusResponse(session, userId))
                    .orElseGet(() -> toWaitingResponse(userId));
        });
    }

    public FlirtStatusResponse cancel(String userId) {
        distributedLock.runLocked(MATCH_LOCK_KEY, MATCH_LOCK_TTL, () -> queueStore.remove(userId));
        return FlirtStatusResponse.builder().status(FlirtStatus.ENDED).build();
    }

    public FlirtStatusResponse getStatus(String userId) {
        if (queueStore.contains(userId)) {
            return toWaitingResponse(userId);
        }
        return findActiveSession(userId)
                .map(this::reconcileSessionWithReport)
                .filter(s -> isOngoingFlirt(s.getStatus()))
                .map(s -> toStatusResponse(s, userId))
                .orElse(FlirtStatusResponse.builder().status(FlirtStatus.ENDED).build());
    }

    public List<FlirtSessionHistoryResponse> listHistory(String userId, int limit) {
        int capped = Math.min(Math.max(limit, 1), 50);
        return sessionRepository.findMatchedHistoryForUser(userId, PageRequest.of(0, capped)).stream()
                .map(session -> toHistoryResponse(session, userId))
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getMessages(String sessionId, String userId, String cursor, int limit) {
        FlirtSession session = getSessionForUser(sessionId, userId);
        List<FlirtMessage> messages;
        if (cursor != null && !cursor.isBlank()) {
            messages = messageRepository.findBySessionIdAndSentAtBeforeOrderBySentAtDesc(
                    session.getId(), Instant.parse(cursor), PageRequest.of(0, limit));
        } else {
            messages = messageRepository.findBySessionIdOrderBySentAtDesc(session.getId(), PageRequest.of(0, limit));
        }
        Collections.reverse(messages);
        return messages.stream().map(m -> toMessageResponse(m, userId)).collect(Collectors.toList());
    }

    public MessageResponse sendMessage(String sessionId, String userId, SendMessageRequest request) {
        FlirtSession session = getSessionForUser(sessionId, userId);
        ensureSessionOpen(session);
        String content = messageContentValidator.validateAndTrim(request.getContent());
        rateLimitService.checkMessageRate(userId);
        promoteToChattingIfNeeded(session);
        ensureSessionOpen(session);
        FlirtMessage msg = FlirtMessage.builder()
                .sessionId(sessionId)
                .senderId(userId)
                .content(content)
                .sentAt(Instant.now())
                .build();
        applyReply(msg, sessionId, request.getReplyToId());
        messageRepository.save(msg);
        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishFlirtMessage(sessionId, response);
        return response;
    }

    public MessageResponse sendImageMessage(String sessionId, String userId, MultipartFile file, String caption) {
        return sendImageMessage(sessionId, userId, file, caption, null);
    }

    public MessageResponse sendImageMessage(String sessionId, String userId, MultipartFile file, String caption, String replyToId) {
        FlirtSession session = getSessionForUser(sessionId, userId);
        ensureSessionOpen(session);
        rateLimitService.checkMessageRate(userId);
        String imageUrl = fileUploadService.uploadChatImage(userId, file);
        promoteToChattingIfNeeded(session);
        ensureSessionOpen(session);
        String content = (caption != null && !caption.isBlank())
                ? messageContentValidator.validateAndTrim(caption) : null;
        FlirtMessage msg = FlirtMessage.builder()
                .sessionId(sessionId)
                .senderId(userId)
                .imageUrl(imageUrl)
                .content(content)
                .sentAt(Instant.now())
                .build();
        applyReply(msg, sessionId, replyToId);
        messageRepository.save(msg);
        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishFlirtMessage(sessionId, response);
        return response;
    }

    public MessageResponse reactToMessage(String sessionId, String userId, String messageId, String emoji) {
        getSessionForUser(sessionId, userId);
        FlirtMessage msg = messageRepository.findById(messageId)
                .filter(m -> sessionId.equals(m.getSessionId()))
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
        eventPublisher.publishFlirtMessageUpdated(sessionId, response);
        return response;
    }

    public MessageResponse deleteMessage(String sessionId, String userId, String messageId) {
        getSessionForUser(sessionId, userId);
        FlirtMessage msg = messageRepository.findById(messageId)
                .filter(m -> sessionId.equals(m.getSessionId()))
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
        MessageResponse response = toMessageResponse(msg, userId);
        eventPublisher.publishFlirtMessageUpdated(sessionId, response);
        return response;
    }

    private void applyReply(FlirtMessage msg, String sessionId, String replyToId) {
        if (replyToId == null || replyToId.isBlank()) return;
        messageRepository.findById(replyToId)
                .filter(r -> sessionId.equals(r.getSessionId()) && !r.isDeleted())
                .ifPresent(r -> {
                    msg.setReplyToId(r.getId());
                    msg.setReplyToSenderId(r.getSenderId());
                    msg.setReplyToPreview(previewOf(r.getContent(), r.getImageUrl()));
                    msg.setReplyToImageUrl(r.getImageUrl());
                });
    }

    private String previewOf(String content, String imageUrl) {
        if (content != null && !content.isBlank()) {
            return content.length() > 80 ? content.substring(0, 80) + "…" : content;
        }
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

    public void notifyTyping(String sessionId, String userId) {
        FlirtSession session = getSessionForUser(sessionId, userId);
        ensureSessionOpen(session);
        eventPublisher.publishFlirtTyping(sessionId, Map.of("userId", userId));
    }

    public void endSession(String sessionId, String userId) {
        FlirtSession session = getSessionForUser(sessionId, userId);
        session.setStatus(FlirtStatus.ENDED);
        session.setEndedAt(Instant.now());
        session.setEndedBy(userId);
        sessionRepository.save(session);
        notifySessionEnded(session, "ENDED");
    }

    public void reportSession(String sessionId, String userId, ReportRequest request) {
        FlirtSession session = getSessionForUser(sessionId, userId);
        String reportedId = session.getUserAId().equals(userId) ? session.getUserBId() : session.getUserAId();
        session.setStatus(FlirtStatus.REPORTED);
        session.setEndedAt(Instant.now());
        session.setEndedBy(userId);
        session.setReportReason(request.getReason());
        sessionRepository.save(session);

        reportRepository.save(UserReport.builder()
                .reporterId(userId)
                .reportedId(reportedId)
                .sessionId(sessionId)
                .reason(request.getReason())
                .build());

        clearWaitingForUser(session.getUserAId());
        clearWaitingForUser(session.getUserBId());
        notifySessionEnded(session, "REPORTED");
    }

    public boolean isParticipant(String sessionId, String userId) {
        return sessionRepository.findByIdAndDeletedFalse(sessionId)
                .map(s -> userId.equals(s.getUserAId()) || userId.equals(s.getUserBId()))
                .orElse(false);
    }

    @Scheduled(fixedRate = 10000)
    public void expireWaiting() {
        Instant cutoff = Instant.now().minus(matchTimeoutSeconds, ChronoUnit.SECONDS);
        distributedLock.runLocked(MATCH_LOCK_KEY, MATCH_LOCK_TTL, () -> {
            for (WaitingUser waiting : queueStore.all()) {
                if (waiting.since().isBefore(cutoff)) {
                    queueStore.remove(waiting.userId());
                    eventPublisher.publishFlirtEvent(
                            waiting.userId(), WebSocketMessageType.FLIRT_CANCELLED,
                            Map.of("reason", "NO_MATCH"));
                }
            }
        });
    }

    private Optional<FlirtSession> tryMatchLocked(WaitingUser newcomer) {
        for (WaitingUser candidate : queueStore.all()) {
            if (candidate.userId().equals(newcomer.userId())) continue;
            if (!isCompatible(newcomer, candidate)) continue;
            if (recentlyMatched(newcomer.userId(), candidate.userId())) continue;
            if (blockService.isBlockedEitherDirection(newcomer.userId(), candidate.userId())) continue;

            User candidateUser = userService.findUser(candidate.userId());
            if (!candidateUser.isActive() || candidateUser.isBanned()) continue;

            queueStore.remove(newcomer.userId());
            queueStore.remove(candidate.userId());

            User userA = userService.findUser(newcomer.userId());
            User userB = userService.findUser(candidate.userId());

            FlirtSession session = FlirtSession.builder()
                    .userAId(newcomer.userId())
                    .userBId(candidate.userId())
                    .status(FlirtStatus.MATCHED)
                    .userAPrefs(newcomer.prefs())
                    .userBPrefs(candidate.prefs())
                    .matchedAt(Instant.now())
                    .build();
            sessionRepository.save(session);

            FlirtStatusResponse respA = toStatusResponse(session, newcomer.userId());
            FlirtStatusResponse respB = toStatusResponse(session, candidate.userId());
            eventPublisher.publishFlirtEvent(newcomer.userId(), WebSocketMessageType.FLIRT_MATCHED, respA);
            eventPublisher.publishFlirtEvent(candidate.userId(), WebSocketMessageType.FLIRT_MATCHED, respB);

            String matchUrl = "/flirt/chat/" + session.getId();
            webPushService.sendToUser(newcomer.userId(), "Đã ghép đôi! 💕",
                    "Bạn vừa được ghép với " + userB.getNickname(), matchUrl, "flirt-match");
            webPushService.sendToUser(candidate.userId(), "Đã ghép đôi! 💕",
                    "Bạn vừa được ghép với " + userA.getNickname(), matchUrl, "flirt-match");
            return Optional.of(session);
        }
        return Optional.empty();
    }

    private boolean isCompatible(WaitingUser a, WaitingUser b) {
        if (!a.prefs().wantsGender(b.gender())) return false;
        if (!b.prefs().wantsGender(a.gender())) return false;
        return agesOverlap(a, b);
    }

    private boolean agesOverlap(WaitingUser a, WaitingUser b) {
        return a.age() >= b.prefs().getMinAge() && a.age() <= b.prefs().getMaxAge()
                && b.age() >= a.prefs().getMinAge() && b.age() <= a.prefs().getMaxAge();
    }

    private boolean recentlyMatched(String userA, String userB) {
        Instant since = Instant.now().minus(cooldownHours, ChronoUnit.HOURS);
        return sessionRepository.findByUserAIdOrUserBIdAndCreatedAtAfter(userA, userB, since).stream()
                .anyMatch(s -> (s.getUserAId().equals(userA) && s.getUserBId().equals(userB))
                        || (s.getUserAId().equals(userB) && s.getUserBId().equals(userA)));
    }

    private Optional<FlirtSession> findActiveSession(String userId) {
        List<FlirtStatus> active = List.of(FlirtStatus.MATCHED, FlirtStatus.CHATTING);
        return sessionRepository.findByUserAIdOrUserBIdAndStatusIn(userId, userId, active).stream().findFirst();
    }

    private FlirtSession reconcileSessionWithReport(FlirtSession session) {
        reportRepository.findFirstBySessionId(session.getId()).ifPresent(report -> {
            if (session.getStatus() != FlirtStatus.REPORTED && session.getStatus() != FlirtStatus.ENDED) {
                session.setStatus(FlirtStatus.REPORTED);
                session.setEndedAt(Instant.now());
                session.setReportReason(report.getReason());
                sessionRepository.save(session);
            }
        });
        return sessionRepository.findByIdAndDeletedFalse(session.getId()).orElse(session);
    }

    private boolean isOngoingFlirt(FlirtStatus status) {
        return status == FlirtStatus.MATCHED || status == FlirtStatus.CHATTING;
    }

    private void ensureSessionOpen(FlirtSession session) {
        if (session.getStatus() == FlirtStatus.ENDED || session.getStatus() == FlirtStatus.REPORTED) {
            throw new BusinessException(ApiCode.FLIRT_SESSION_ENDED);
        }
    }

    private void promoteToChattingIfNeeded(FlirtSession session) {
        if (session.getStatus() != FlirtStatus.MATCHED) return;
        sessionRepository.findByIdAndDeletedFalse(session.getId()).ifPresent(fresh -> {
            if (fresh.getStatus() == FlirtStatus.MATCHED) {
                fresh.setStatus(FlirtStatus.CHATTING);
                sessionRepository.save(fresh);
                session.setStatus(FlirtStatus.CHATTING);
            } else {
                session.setStatus(fresh.getStatus());
            }
        });
    }

    private void clearWaitingForUser(String userId) {
        distributedLock.runLocked(MATCH_LOCK_KEY, MATCH_LOCK_TTL, () -> queueStore.remove(userId));
    }

    private FlirtSession getSessionForUser(String sessionId, String userId) {
        FlirtSession session = sessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException(ApiCode.FLIRT_SESSION_NOT_FOUND));
        if (!userId.equals(session.getUserAId()) && !userId.equals(session.getUserBId())) {
            throw new BusinessException(ApiCode.ACCESS_DENIED);
        }
        return session;
    }

    private void notifySessionEnded(FlirtSession session, String reason) {
        String reportedId = session.getUserAId().equals(session.getEndedBy())
                ? session.getUserBId() : session.getUserAId();
        Map<String, Object> base = new HashMap<>();
        base.put("sessionId", session.getId());
        base.put("reason", reason);

        Map<String, Object> payloadA = new HashMap<>(base);
        Map<String, Object> payloadB = new HashMap<>(base);
        if ("REPORTED".equals(reason)) {
            payloadA.put("role", session.getUserAId().equals(reportedId) ? "REPORTED" : "REPORTER");
            payloadB.put("role", session.getUserBId().equals(reportedId) ? "REPORTED" : "REPORTER");
        }
        eventPublisher.publishFlirtEvent(session.getUserAId(), WebSocketMessageType.FLIRT_ENDED, payloadA);
        eventPublisher.publishFlirtEvent(session.getUserBId(), WebSocketMessageType.FLIRT_ENDED, payloadB);
    }

    private FlirtStatusResponse toStatusResponse(FlirtSession session, String userId) {
        String partnerId = session.getUserAId().equals(userId) ? session.getUserBId() : session.getUserAId();
        User partner = userService.findUser(partnerId);
        return FlirtStatusResponse.builder()
                .status(session.getStatus())
                .sessionId(session.getId())
                .partnerNickname(partner.getNickname())
                .partnerAvatarUrl(partner.getAvatarUrl())
                .partnerAge(partner.getAge())
                .partnerGender(partner.getGender())
                .partnerBio(partner.getBio())
                .partnerInterests(partner.getInterests())
                .partnerPhotos(partner.getPhotos())
                .build();
    }

    private FlirtStatusResponse toWaitingResponse(String userId) {
        WaitingUser waiting = queueStore.get(userId).orElse(null);
        if (waiting == null) {
            return FlirtStatusResponse.builder().status(FlirtStatus.ENDED).build();
        }
        long elapsed = ChronoUnit.SECONDS.between(waiting.since(), Instant.now());
        int remaining = (int) Math.max(0, matchTimeoutSeconds - elapsed);
        return FlirtStatusResponse.builder()
                .status(FlirtStatus.WAITING)
                .preferences(waiting.prefs())
                .waitingSecondsRemaining(remaining)
                .build();
    }

    private FlirtSessionHistoryResponse toHistoryResponse(FlirtSession session, String userId) {
        String partnerId = session.getUserAId().equals(userId) ? session.getUserBId() : session.getUserAId();
        User partner = userService.findUser(partnerId);
        return FlirtSessionHistoryResponse.builder()
                .sessionId(session.getId())
                .partnerId(partnerId)
                .partnerNickname(partner.getNickname())
                .partnerAvatarUrl(partner.getAvatarUrl())
                .status(session.getStatus())
                .matchedAt(session.getMatchedAt())
                .endedAt(session.getEndedAt())
                .build();
    }

    private MessageResponse toMessageResponse(FlirtMessage m, String userId) {
        boolean deleted = m.isDeleted();
        String replyLabel = null;
        if (m.getReplyToId() != null) {
            replyLabel = userId.equals(m.getReplyToSenderId()) ? "Bạn" : "Đối phương";
        }
        return MessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .content(deleted ? null : m.getContent())
                .imageUrl(deleted ? null : m.getImageUrl())
                .sentAt(m.getSentAt())
                .mine(userId.equals(m.getSenderId()))
                .replyToId(m.getReplyToId())
                .replyToSenderId(m.getReplyToSenderId())
                .replyToSenderNickname(replyLabel)
                .replyToPreview(m.getReplyToPreview())
                .replyToImageUrl(m.getReplyToImageUrl())
                .reactions(deleted ? Map.of() : toReactionMap(m.getReactions()))
                .deleted(deleted)
                .build();
    }
}
