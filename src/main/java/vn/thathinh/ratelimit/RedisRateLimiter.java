package vn.thathinh.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Sliding window bằng Redis ZSET (atomic qua Lua) — an toàn khi chạy nhiều instance.
 * Dùng khi app.redis.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisRateLimiter implements RateLimiter {

    private static final String SCRIPT = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[2])
            local count = redis.call('ZCARD', KEYS[1])
            if count >= tonumber(ARGV[3]) then
              return 0
            end
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
            redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[5]))
            return 1
            """;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> script;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.script = new DefaultRedisScript<>(SCRIPT, Long.class);
    }

    @Override
    public boolean tryAcquire(String key, int maxCount, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - (long) windowSeconds * 1000L;
        long ttlMillis = (long) windowSeconds * 1000L;
        String member = now + "-" + UUID.randomUUID();
        Long allowed = redis.execute(
                script,
                List.of("rl:" + key),
                String.valueOf(now),
                String.valueOf(windowStart),
                String.valueOf(maxCount),
                member,
                String.valueOf(ttlMillis));
        return allowed != null && allowed == 1L;
    }
}
