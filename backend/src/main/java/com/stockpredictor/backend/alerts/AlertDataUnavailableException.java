package com.stockpredictor.backend.alerts;

/**
 * Thrown when Firestore, CoinGecko, or FCM sending fails during one {@link AlertRuleService}
 * evaluation pass. Unlike {@code PredictionServiceUnavailableException}/
 * {@code ChatbotServiceUnavailableException}, this never crosses an HTTP boundary — AlertRuleService
 * is not an endpoint, so this is caught and logged per-run by the scheduler itself, never allowed
 * to propagate out of a {@code @Scheduled} method (which would otherwise silently stop future
 * executions).
 */
public class AlertDataUnavailableException extends RuntimeException {
    public AlertDataUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
