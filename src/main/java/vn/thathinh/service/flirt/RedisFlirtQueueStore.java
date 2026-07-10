package vn.thathinh.service.flirt;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hàng đợi lưu trong Redis hash (sống sót qua restart, chia sẻ giữa các instance).
 * Dùng khi app.redis.enabled=true.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisFlirtQueueStore implements FlirtQueueStore {

    private static final String KEY = "flirt:waiting";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisFlirtQueueStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean contains(String userId) {
        return Boolean.TRUE.equals(redis.opsForHash().hasKey(KEY, userId));
    }

    @Override
    public Optional<WaitingUser> get(String userId) {
        Object raw = redis.opsForHash().get(KEY, userId);
        return raw == null ? Optional.empty() : deserialize(raw.toString());
    }

    @Override
    public void put(WaitingUser user) {
        try {
            redis.opsForHash().put(KEY, user.userId(), objectMapper.writeValueAsString(user));
        } catch (Exception e) {
            log.error("Không thể lưu hàng đợi flirt vào Redis cho user {}", user.userId(), e);
            throw new IllegalStateException("Redis flirt queue write failed", e);
        }
    }

    @Override
    public void remove(String userId) {
        redis.opsForHash().delete(KEY, userId);
    }

    @Override
    public Collection<WaitingUser> all() {
        Map<Object, Object> entries = redis.opsForHash().entries(KEY);
        List<WaitingUser> result = new ArrayList<>(entries.size());
        for (Object value : entries.values()) {
            deserialize(value.toString()).ifPresent(result::add);
        }
        return result;
    }

    private Optional<WaitingUser> deserialize(String json) {
        try {
            return Optional.of(objectMapper.readValue(json, WaitingUser.class));
        } catch (Exception e) {
            log.warn("Bỏ qua entry hàng đợi flirt không đọc được: {}", json, e);
            return Optional.empty();
        }
    }
}
