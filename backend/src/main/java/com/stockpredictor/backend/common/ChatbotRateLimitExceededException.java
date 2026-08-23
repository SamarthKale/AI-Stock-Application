package com.stockpredictor.backend.common;

/**
 * Thrown when a user exceeds the chatbot proxy's per-hour rate limit
 * ({@link com.stockpredictor.backend.chatbot.ChatbotRateLimiter}) — mapped to 429. This is the
 * Phase 5b Definition of Done's required "documented rate/cost-control measure" for an endpoint
 * that proxies a metered, paid third-party API.
 */
public class ChatbotRateLimitExceededException extends RuntimeException {
    public ChatbotRateLimitExceededException(String message) {
        super(message);
    }
}
