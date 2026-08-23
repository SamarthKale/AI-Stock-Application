package com.stockpredictor.backend.common;

/**
 * Thrown when a user exceeds the prediction endpoint's per-hour rate limit
 * ({@link com.stockpredictor.backend.prediction.PredictionRateLimiter}) — mapped to 429, same
 * pattern as {@link ChatbotRateLimitExceededException}.
 */
public class PredictionRateLimitExceededException extends RuntimeException {
    public PredictionRateLimitExceededException(String message) {
        super(message);
    }
}
