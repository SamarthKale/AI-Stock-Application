package com.stockpredictor.backend.common;

/**
 * Thrown when the FastAPI prediction service is unreachable, times out, or errors — mapped to
 * 503, never propagated as a raw 5xx. Predictions are additive, not load-bearing: the caller
 * (Android, via PredictionRepository) treats this the same as "no prediction available yet"
 * rather than a hard failure (Phase 5 plan section 8).
 */
public class PredictionServiceUnavailableException extends RuntimeException {
    public PredictionServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
