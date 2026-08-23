package com.stockpredictor.backend.alerts;

import com.stockpredictor.backend.prediction.PredictionCacheEntity;
import com.stockpredictor.backend.prediction.PredictionCacheRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Server-scheduled, 24/7 alert evaluation — no "market hours" gating of any kind (the Phase 5c
 * re-scope's central point: crypto trades continuously, so the stock-era "evaluate only during
 * trading hours" concept simply doesn't apply). One evaluation pass per {@link #evaluate()} call:
 * one Firestore query for every user's watchlist, one batched CoinGecko call for the distinct
 * coin set, one Postgres query for cached predictions, then per-user-per-coin rule checks against
 * that shared bulk data — never a per-user or per-coin duplicate network call.
 *
 * <p><b>Documented limitation, not a defect</b>: prediction-confidence alerts only ever fire for a
 * coin with an existing, fresh {@code prediction_cache} row. That cache is populated lazily by
 * Android's on-demand {@code /api/predictions} calls (Phase 5) — this service deliberately does
 * NOT call the FastAPI prediction service proactively for every watchlisted coin every run, since
 * that would turn a lightweight scheduled job into a second heavy caller of ai-service. A
 * watchlisted coin nobody has viewed in the app recently simply never triggers a
 * prediction-confidence alert; it can still trigger a price-move alert.
 */
@Service
public class AlertRuleService {

    private static final Logger log = LoggerFactory.getLogger(AlertRuleService.class);
    private static final String PREDICTION_HORIZON = "24h";

    private final WatchlistReader watchlistReader;
    private final MarketDataClient marketDataClient;
    private final PredictionCacheRepository predictionCacheRepository;
    private final AlertEvaluator evaluator;
    private final AlertCooldownService cooldownService;
    private final PushSender pushSender;
    private final boolean enabled;

    public AlertRuleService(
            WatchlistReader watchlistReader,
            MarketDataClient marketDataClient,
            PredictionCacheRepository predictionCacheRepository,
            AlertEvaluator evaluator,
            AlertCooldownService cooldownService,
            PushSender pushSender,
            @Value("${alerts.enabled:true}") boolean enabled) {
        this.watchlistReader = watchlistReader;
        this.marketDataClient = marketDataClient;
        this.predictionCacheRepository = predictionCacheRepository;
        this.evaluator = evaluator;
        this.cooldownService = cooldownService;
        this.pushSender = pushSender;
        this.enabled = enabled;
    }

    @Scheduled(fixedRateString = "${alerts.interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void evaluate() {
        if (!enabled) {
            return;
        }
        try {
            runEvaluationPass();
        } catch (Exception e) {
            // Never let an exception escape a @Scheduled method: a fixed-rate task that throws
            // stops all future executions silently, which would be far worse than one missed
            // cycle — this is what makes the scheduler resilient to a transient Firestore/
            // CoinGecko/FCM outage rather than permanently dying on the first one.
            log.warn("Alert evaluation pass failed — will retry next cycle", e);
        }
    }

    void runEvaluationPass() {
        List<WatchlistReader.UserWatchlist> userWatchlists = watchlistReader.getAllUserWatchlists();
        if (userWatchlists.isEmpty()) {
            return;
        }

        Set<String> distinctCoinIds = userWatchlists.stream()
                .flatMap(uw -> uw.coinIds().stream())
                .collect(Collectors.toSet());

        Map<String, MarketDataClient.CoinMarketSnapshot> marketData = marketDataClient.getMarketSnapshots(distinctCoinIds);
        Map<String, PredictionCacheEntity> freshPredictionsByCoinId = predictionCacheRepository.findAll().stream()
                .filter(p -> PREDICTION_HORIZON.equals(p.getHorizon()))
                .filter(p -> p.getExpiresAt() != null && p.getExpiresAt().isAfter(Instant.now()))
                .collect(Collectors.toMap(PredictionCacheEntity::getCoinId, p -> p, (first, second) -> first));

        for (WatchlistReader.UserWatchlist userWatchlist : userWatchlists) {
            for (String coinId : userWatchlist.coinIds()) {
                evaluateForUserAndCoin(userWatchlist, coinId, marketData.get(coinId), freshPredictionsByCoinId.get(coinId));
            }
        }
    }

    private void evaluateForUserAndCoin(
            WatchlistReader.UserWatchlist userWatchlist,
            String coinId,
            MarketDataClient.CoinMarketSnapshot snapshot,
            PredictionCacheEntity prediction) {
        String displayName = snapshot != null && snapshot.name() != null ? snapshot.name() : coinId;

        if (snapshot != null) {
            evaluator.evaluatePriceMove(snapshot.priceChangePercentage24h(), displayName)
                    .ifPresent(message -> maybeSend(userWatchlist, coinId, AlertEvaluator.RULE_PRICE_MOVE, message));
        }
        if (prediction != null) {
            evaluator.evaluatePredictionConfidence(prediction.getConfidence().doubleValue(), prediction.getDirection(), displayName)
                    .ifPresent(message -> maybeSend(userWatchlist, coinId, AlertEvaluator.RULE_PREDICTION_CONFIDENCE, message));
        }
    }

    private void maybeSend(WatchlistReader.UserWatchlist userWatchlist, String coinId, String ruleType, String message) {
        if (cooldownService.isInCooldown(userWatchlist.uid(), coinId, ruleType)) {
            return;
        }
        try {
            pushSender.sendDataMessage(userWatchlist.fcmToken(), Map.of(
                    "coinId", coinId,
                    "ruleType", ruleType,
                    "headline", message));
            // Recorded only after a successful send -- a failed push must not suppress the next
            // attempt via a cooldown it never earned.
            cooldownService.recordSent(userWatchlist.uid(), coinId, ruleType);
        } catch (AlertDataUnavailableException e) {
            log.warn("Failed to send alert push uid={} coinId={} ruleType={}", userWatchlist.uid(), coinId, ruleType, e);
        }
    }
}
