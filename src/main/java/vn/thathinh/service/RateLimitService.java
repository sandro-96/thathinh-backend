package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.ratelimit.RateLimiter;

/**
 * Giới hạn tần suất theo user/IP. Uỷ quyền cho {@link RateLimiter}
 * (in-memory hoặc Redis tuỳ cấu hình app.redis.enabled).
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimiter rateLimiter;

    @Value("${app.rate-limit.message.max-per-minute:30}")
    private int messageMaxPerMinute;

    @Value("${app.rate-limit.flirt.max-per-hour:10}")
    private int flirtMaxPerHour;

    @Value("${app.rate-limit.auth.max-per-minute:10}")
    private int authMaxPerMinute;

    @Value("${app.rate-limit.email.max-per-hour:5}")
    private int emailMaxPerHour;

    public void checkMessageRate(String userId) {
        enforce("msg:" + userId, messageMaxPerMinute, 60, ApiCode.RATE_LIMIT_MESSAGE);
    }

    public void checkFlirtStartRate(String userId) {
        enforce("flirt:" + userId, flirtMaxPerHour, 3600, ApiCode.RATE_LIMIT_FLIRT);
    }

    public void checkAuthRate(String clientIp) {
        enforce("auth:" + clientIp, authMaxPerMinute, 60, ApiCode.RATE_LIMIT_AUTH);
    }

    public void checkEmailRate(String email) {
        if (email == null || email.isBlank()) return;
        enforce("email:" + email.toLowerCase(), emailMaxPerHour, 3600, ApiCode.RATE_LIMIT_EMAIL);
    }

    private void enforce(String key, int maxCount, int windowSeconds, ApiCode errorCode) {
        if (!rateLimiter.tryAcquire(key, maxCount, windowSeconds)) {
            throw new BusinessException(errorCode);
        }
    }
}
