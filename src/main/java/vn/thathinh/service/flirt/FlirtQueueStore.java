package vn.thathinh.service.flirt;

import java.util.Collection;
import java.util.Optional;

/**
 * Kho lưu hàng đợi chờ ghép đôi. In-memory (single instance) hoặc Redis (nhiều instance,
 * sống sót qua restart).
 */
public interface FlirtQueueStore {

    boolean contains(String userId);

    Optional<WaitingUser> get(String userId);

    void put(WaitingUser user);

    void remove(String userId);

    Collection<WaitingUser> all();
}
