package com.stockpredictor.backend.ratelimit;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Generic atomic sliding-window rate limiter backed by Redis (Phase 6 — see CLAUDE.md's Phase 6
 * architecture: "Redis = cache + distributed rate limiting"). Replaces the per-feature in-process
 * {@code ConcurrentHashMap} limiters from Phase 5b, which only coordinated within a single JVM —
 * this coordinates across every backend process/thread sharing the same Redis instance, which is
 * what "distributed" means at this project's single-VM Docker Compose scale (concurrent
 * connections/threads against the one backend container, not multiple hosts).
 *
 * <p>The check-and-record is a single Lua script executed atomically by Redis, so two concurrent
 * requests racing the same key can never both observe "under the limit" and both succeed when
 * only one should — a plain read-then-write from Java would have that race.
 */
@Component
public class RedisRateLimiter {

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1] - ARGV[2])
            local count = redis.call('ZCARD', KEYS[1])
            if count < tonumber(ARGV[3]) then
              redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
              return 1
            else
              return 0
            end
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @param key    fully-qualified rate-limit bucket, e.g. {@code "ratelimit:chatbot:<uid>"}
     * @param limit  max requests allowed inside {@code window}
     * @param window sliding window length
     * @return true if the request is allowed (and has been recorded against the bucket), false if
     *     {@code key} has already reached {@code limit} requests within the trailing {@code window}
     */
    public boolean tryAcquire(String key, long limit, Duration window) {
        long now = System.currentTimeMillis();
        String member = now + ":" + UUID.randomUUID();
        Long allowed = redisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(window.toMillis()),
                String.valueOf(limit),
                member);
        return allowed != null && allowed == 1L;
    }
}
