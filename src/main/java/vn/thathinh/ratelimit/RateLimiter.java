package vn.thathinh.ratelimit;

/**
 * Chiến lược giới hạn tần suất (sliding window). Có 2 hiện thực:
 * in-memory (single instance) và Redis (scale nhiều instance).
 */
public interface RateLimiter {

    /**
     * @return true nếu còn trong hạn mức (được phép), false nếu vượt hạn mức.
     */
    boolean tryAcquire(String key, int maxCount, int windowSeconds);
}
