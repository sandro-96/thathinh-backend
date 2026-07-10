package vn.thathinh.service.flirt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hàng đợi in-memory. Dùng khi app.redis.enabled=false (mặc định).
 */
@Component
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryFlirtQueueStore implements FlirtQueueStore {

    private final ConcurrentHashMap<String, WaitingUser> queue = new ConcurrentHashMap<>();

    @Override
    public boolean contains(String userId) {
        return queue.containsKey(userId);
    }

    @Override
    public Optional<WaitingUser> get(String userId) {
        return Optional.ofNullable(queue.get(userId));
    }

    @Override
    public void put(WaitingUser user) {
        queue.put(user.userId(), user);
    }

    @Override
    public void remove(String userId) {
        queue.remove(userId);
    }

    @Override
    public Collection<WaitingUser> all() {
        return List.copyOf(queue.values());
    }
}
