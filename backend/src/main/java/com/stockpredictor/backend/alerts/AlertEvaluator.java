package com.stockpredictor.backend.alerts;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pure rule logic — no Firestore/CoinGecko/FCM/Postgres I/O, so it's directly unit-testable with
 * plain values, independent of {@link AlertRuleService}'s orchestration. Two rule types:
 * {@link #RULE_PRICE_MOVE} (a watchlist coin's 24h % change exceeds a configurable threshold) and
 * {@link #RULE_PREDICTION_CONFIDENCE} (a cached prediction's confidence exceeds a configurable
 * threshold). Both thresholds are config values, not hardcoded, so they can be "manually lowered
 * temporarily" for demoing (CLAUDE.md's own DoD instruction for this phase).
 */
@Component
public class AlertEvaluator {

    public static final String RULE_PRICE_MOVE = "PRICE_MOVE";
    public static final String RULE_PREDICTION_CONFIDENCE = "PREDICTION_CONFIDENCE";

    private final double priceMoveThresholdPct;
    private final double confidenceThreshold;

    public AlertEvaluator(
            @Value("${alerts.price-move-threshold-pct:5.0}") double priceMoveThresholdPct,
            @Value("${alerts.confidence-threshold:70.0}") double confidenceThreshold) {
        this.priceMoveThresholdPct = priceMoveThresholdPct;
        this.confidenceThreshold = confidenceThreshold;
    }

    public Optional<String> evaluatePriceMove(Double priceChangePercentage24h, String coinDisplayName) {
        if (priceChangePercentage24h == null || Math.abs(priceChangePercentage24h) < priceMoveThresholdPct) {
            return Optional.empty();
        }
        String direction = priceChangePercentage24h >= 0 ? "up" : "down";
        return Optional.of(String.format(
                "%s is %s %.1f%% in the last 24h", coinDisplayName, direction, Math.abs(priceChangePercentage24h)));
    }

    /** Only ever called for a coin with an existing cached prediction — see
     *  {@link AlertRuleService}'s documented limitation: this rule cannot evaluate a watchlisted
     *  coin with no prediction cached, since AlertRuleService deliberately does not proactively
     *  call the prediction service for every watchlisted coin every run. */
    public Optional<String> evaluatePredictionConfidence(Double confidence, String direction, String coinDisplayName) {
        if (confidence == null || direction == null || confidence < confidenceThreshold) {
            return Optional.empty();
        }
        return Optional.of(String.format("%s prediction: %s (%.0f%% confidence)", coinDisplayName, direction, confidence));
    }
}
