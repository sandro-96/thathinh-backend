package vn.thathinh.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Theo dõi trạng thái online của người dùng trong bộ nhớ (heartbeat + TTL).
 * Dùng cho hiển thị "đang hoạt động" / "hoạt động gần đây" ở chat riêng và danh sách chat.
 */
@Service
public class UserPresenceService {

    private static final long ONLINE_TTL_SECONDS = 45;

    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public void heartbeat(String userId) {
        if (userId != null) {
            lastSeen.put(userId, Instant.now());
        }
    }

    public boolean isOnline(String userId) {
        if (userId == null) return false;
        Instant seen = lastSeen.get(userId);
        return seen != null && seen.isAfter(cutoff());
    }

    public Instant lastSeenAt(String userId) {
        return userId == null ? null : lastSeen.get(userId);
    }

    public Map<String, Instant> lastSeenFor(Collection<String> userIds) {
        Map<String, Instant> result = new HashMap<>();
        for (String id : userIds) {
            Instant seen = lastSeen.get(id);
            if (seen != null) result.put(id, seen);
        }
        return result;
    }

    private Instant cutoff() {
        return Instant.now().minus(ONLINE_TTL_SECONDS, ChronoUnit.SECONDS);
    }
}
