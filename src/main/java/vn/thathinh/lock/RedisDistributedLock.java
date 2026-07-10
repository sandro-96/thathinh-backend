package vn.thathinh.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.exception.BusinessException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Khoá phân tán bằng Redis (SET NX PX + giải phóng an toàn qua Lua).
 * Dùng khi app.redis.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisDistributedLock implements DistributedLock {

    private static final String UNLOCK_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            else
              return 0
            end
            """;

    private static final long ACQUIRE_TIMEOUT_MS = 5000;
    private static final long RETRY_DELAY_MS = 50;

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> unlockScript;

    public RedisDistributedLock(StringRedisTemplate redis) {
        this.redis = redis;
        this.unlockScript = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
    }

    @Override
    public <T> T runLocked(String key, Duration ttl, Supplier<T> action) {
        String lockKey = "lock:" + key;
        String token = UUID.randomUUID().toString();
        if (!acquire(lockKey, token, ttl)) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR);
        }
        try {
            return action.get();
        } finally {
            redis.execute(unlockScript, List.of(lockKey), token);
        }
    }

    private boolean acquire(String lockKey, String token, Duration ttl) {
        long deadline = System.currentTimeMillis() + ACQUIRE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            Boolean ok = redis.opsForValue().setIfAbsent(lockKey, token, ttl);
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
