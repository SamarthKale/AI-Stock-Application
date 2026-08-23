package com.stockpredictor.backend.chatbot;

import com.stockpredictor.backend.ratelimit.RedisRateLimiter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-user sliding-window rate limiter — the Phase 5b Definition of Done's required "documented
 * rate/cost-control measure" for the chatbot proxy, which fronts a metered, paid third-party API.
 *
 * <p>Phase 6: backed by {@link RedisRateLimiter}, replacing this class's original Phase 5b
 * in-process {@code ConcurrentHashMap} implementation, which did not survive a backend restart
 * and did not coordinate across multiple backend instances/threads — both limitations are now
 * fixed by Redis (see RedisRateLimiter's Javadoc).
 */
@Component
public class ChatbotRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:chatbot:";

    private final RedisRateLimiter redisRateLimiter;
    private final long limitPerHour;

    public ChatbotRateLimiter(RedisRateLimiter redisRateLimiter, @Value("${chatbot.rate-limit-per-hour:20}") long limitPerHour) {
        this.redisRateLimiter = redisRateLimiter;
        this.limitPerHour = limitPerHour;
    }

    /** @return true if the request is allowed (and recorded against the caller's quota), false
     *  if {@code uid} has already made {@code limitPerHour} requests within the trailing hour. */
    public boolean tryAcquire(String uid) {
        return redisRateLimiter.tryAcquire(KEY_PREFIX + uid, limitPerHour, Duration.ofHours(1));
    }
}
