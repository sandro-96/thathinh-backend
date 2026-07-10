package vn.thathinh.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Khoá cho vùng găng cần đồng bộ. Local (ReentrantLock) khi single instance,
 * Redis khi chạy nhiều instance.
 */
public interface DistributedLock {

    <T> T runLocked(String key, Duration ttl, Supplier<T> action);

    default void runLocked(String key, Duration ttl, Runnable action) {
        runLocked(key, ttl, () -> {
            action.run();
            return null;
        });
    }
}
