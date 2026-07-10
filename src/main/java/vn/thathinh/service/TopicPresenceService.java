package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.WebSocketMessageType;
import vn.thathinh.service.realtime.RealtimeEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TopicPresenceService {

    private static final long ONLINE_TTL_SECONDS = 60;

    private final RealtimeEventPublisher eventPublisher;
    private final Map<String, Map<String, Instant>> topicUsers = new ConcurrentHashMap<>();

    public void join(String topicId, String userId) {
        touch(topicId, userId);
        publishPresence(topicId);
    }

    public void leave(String topicId, String userId) {
        Map<String, Instant> users = topicUsers.get(topicId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                topicUsers.remove(topicId);
            }
        }
        publishPresence(topicId);
    }

    public void heartbeat(String topicId, String userId) {
        touch(topicId, userId);
    }

    public void typing(String topicId, String userId, String nickname) {
        eventPublisher.publishTopicPresence(topicId, WebSocketMessageType.TOPIC_TYPING,
                Map.of("userId", userId, "nickname", nickname != null ? nickname : "Ai đó"));
    }

    public int onlineCount(String topicId) {
        prune(topicId);
        Map<String, Instant> users = topicUsers.get(topicId);
        return users != null ? users.size() : 0;
    }

    private void touch(String topicId, String userId) {
        topicUsers.computeIfAbsent(topicId, k -> new ConcurrentHashMap<>())
                .put(userId, Instant.now());
        publishPresence(topicId);
    }

    private void prune(String topicId) {
        Map<String, Instant> users = topicUsers.get(topicId);
        if (users == null) return;
        Instant cutoff = Instant.now().minus(ONLINE_TTL_SECONDS, ChronoUnit.SECONDS);
        users.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
        if (users.isEmpty()) {
            topicUsers.remove(topicId);
        }
    }

    private void publishPresence(String topicId) {
        prune(topicId);
        int count = onlineCount(topicId);
        eventPublisher.publishTopicPresence(topicId, WebSocketMessageType.TOPIC_PRESENCE,
                Map.of("onlineCount", count));
    }
}
