package com.stockpredictor.backend.chatbot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory per-user sliding-window rate limiter — the Phase 5b Definition of Done's required
 * "documented rate/cost-control measure" for the chatbot proxy, which fronts a metered, paid
 * third-party API. Redis is deliberately deferred project-wide (see Phase 4/5's precedent); this
 * in-process {@link ConcurrentHashMap} is a documented tradeoff for this project's single-instance
 * scope — it does not survive a backend restart and does not coordinate across multiple backend
 * instances. That would matter for a real multi-instance production deployment, not here.
 */
@Component
public class ChatbotRateLimiter {

    private final long limitPerHour;
    private final Map<String, Deque<Instant>> requestTimestampsByUid = new ConcurrentHashMap<>();

    public ChatbotRateLimiter(@Value("${chatbot.rate-limit-per-hour:20}") long limitPerHour) {
        this.limitPerHour = limitPerHour;
    }

    /** @return true if the request is allowed (and recorded against the caller's quota), false
     *  if {@code uid} has already made {@code limitPerHour} requests within the trailing hour. */
    public synchronized boolean tryAcquire(String uid) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(Duration.ofHours(1));
        Deque<Instant> timestamps = requestTimestampsByUid.computeIfAbsent(uid, key -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
            timestamps.pollFirst();
        }
        if (timestamps.size() >= limitPerHour) {
            return false;
        }
        timestamps.addLast(now);
        return true;
    }
}
