package pl.kalin.dreamlog.dream.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.kalin.dreamlog.user.User;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for dream creation to prevent AI API abuse.
 * Limits users to 20 dream creations per hour.
 *
 * Uses Resilience4j RateLimiter with per-user instances.
 */
@Service
@Slf4j
public class DreamCreationRateLimiter {

    private static final int LIMIT_PER_HOUR = 20;
    private static final Duration REFRESH_PERIOD = Duration.ofHours(1);

    private final Map<UUID, RateLimiter> userLimiters = new ConcurrentHashMap<>();

    /**
     * Checks if user is allowed to create a dream.
     * Returns true if within rate limit, false if exceeded.
     *
     * @param user the user attempting to create a dream
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean allowCreate(User user) {
        RateLimiter limiter = getUserLimiter(user.getId());
        boolean allowed = limiter.acquirePermission();

        if (!allowed) {
            log.warn("Rate limit exceeded for user: {} ({})", user.getId(), user.getEmail());
        }

        return allowed;
    }

    /**
     * Gets or creates a rate limiter for the specified user.
     */
    private RateLimiter getUserLimiter(UUID userId) {
        return userLimiters.computeIfAbsent(userId, id -> {
            RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(LIMIT_PER_HOUR)           // 20 dreams
                .limitRefreshPeriod(REFRESH_PERIOD)       // per hour
                .timeoutDuration(Duration.ZERO)           // Don't wait, fail immediately
                .build();

            return RateLimiter.of("user-" + id, config);
        });
    }

    /**
     * Resets rate limit for a user (for testing or admin purposes).
     */
    public void resetUserLimit(UUID userId) {
        userLimiters.remove(userId);
        log.info("Rate limit reset for user: {}", userId);
    }
}
