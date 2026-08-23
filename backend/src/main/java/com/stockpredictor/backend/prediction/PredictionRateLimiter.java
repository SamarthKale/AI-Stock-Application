package com.stockpredictor.backend.prediction;

import com.stockpredictor.backend.ratelimit.RedisRateLimiter;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-user sliding-window rate limiter for {@code POST /api/predictions/{coinId}} — Phase 6's new
 * cost-control measure protecting the AI service's inference cost, same reasoning and mechanism
 * as {@link com.stockpredictor.backend.chatbot.ChatbotRateLimiter} (backed by
 * {@link RedisRateLimiter}). Checked before the Postgres cache lookup in
 * {@link PredictionService#getPrediction}, not only on a cache miss — this limiter protects the
 * endpoint itself from abuse, not only the upstream FastAPI call.
 */
@Component
public class PredictionRateLimiter {

    private static final String KEY_PREFIX = "ratelimit:prediction:";

    private final RedisRateLimiter redisRateLimiter;
    private final long limitPerHour;

    public PredictionRateLimiter(RedisRateLimiter redisRateLimiter, @Value("${prediction-service.rate-limit-per-hour:60}") long limitPerHour) {
        this.redisRateLimiter = redisRateLimiter;
        this.limitPerHour = limitPerHour;
    }

    public boolean tryAcquire(String uid) {
        return redisRateLimiter.tryAcquire(KEY_PREFIX + uid, limitPerHour, Duration.ofHours(1));
    }
}
