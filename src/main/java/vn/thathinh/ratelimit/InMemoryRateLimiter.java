package vn.thathinh.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding window in-memory. Dùng khi app.redis.enabled=false (mặc định).
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryRateLimiter implements RateLimiter {

    private final Map<String, Deque<Instant>> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String key, int maxCount, int windowSeconds) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(windowSeconds);
        Deque<Instant> hits = store.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst().isBefore(windowStart)) {
                hits.pollFirst();
            }
            if (hits.size() >= maxCount) {
                return false;
            }
            hits.addLast(now);
            return true;
        }
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(7200);
        store.entrySet().removeIf(entry -> {
            Deque<Instant> hits = entry.getValue();
            synchronized (hits) {
                while (!hits.isEmpty() && hits.peekFirst().isBefore(cutoff)) {
                    hits.pollFirst();
                }
                return hits.isEmpty();
            }
        });
    }
}
