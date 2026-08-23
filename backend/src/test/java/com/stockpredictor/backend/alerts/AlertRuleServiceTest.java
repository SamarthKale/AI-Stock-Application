package com.stockpredictor.backend.alerts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.stockpredictor.backend.prediction.PredictionCacheEntity;
import com.stockpredictor.backend.prediction.PredictionCacheRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Exercises {@link AlertRuleService} end-to-end against a real Postgres (AlertCooldownRepository,
 * PredictionCacheRepository) with only the external services faked (FakeWatchlistReader instead
 * of live Firestore, FakeMarketDataClient instead of live CoinGecko, FakePushSender instead of
 * live FCM) — same pattern as PredictionControllerTest/ChatbotControllerTest.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestAlertBeans.class)
class AlertRuleServiceTest {

    @Autowired
    private AlertRuleService alertRuleService;
    @Autowired
    private WatchlistReader watchlistReader;
    @Autowired
    private MarketDataClient marketDataClient;
    @Autowired
    private PushSender pushSender;
    @Autowired
    private AlertCooldownRepository cooldownRepository;
    @Autowired
    private PredictionCacheRepository predictionCacheRepository;

    private FakeWatchlistReader fakeWatchlistReader;
    private FakeMarketDataClient fakeMarketDataClient;
    private FakePushSender fakePushSender;

    @BeforeEach
    void setUp() {
        fakeWatchlistReader = (FakeWatchlistReader) watchlistReader;
        fakeMarketDataClient = (FakeMarketDataClient) marketDataClient;
        fakePushSender = (FakePushSender) pushSender;

        fakeWatchlistReader.setShouldThrow(false);
        fakeMarketDataClient.setShouldThrow(false);
        fakePushSender.setShouldThrow(false);
        fakeWatchlistReader.setWatchlists(List.of());
        fakeMarketDataClient.setSnapshots(Map.of());
        fakePushSender.getSent().clear();
        cooldownRepository.deleteAll();
        predictionCacheRepository.deleteAll();
    }

    @Test
    void emptyWatchlists_isNoOp() {
        alertRuleService.runEvaluationPass();

        assertThat(fakePushSender.getSent()).isEmpty();
    }

    @Test
    void priceMoveAboveThreshold_sendsAlert() {
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin"), "token-1")));
        fakeMarketDataClient.setSnapshots(Map.of(
                "bitcoin", new MarketDataClient.CoinMarketSnapshot("bitcoin", "Bitcoin", 65000.0, 8.0)));

        alertRuleService.runEvaluationPass();

        assertThat(fakePushSender.getSent()).hasSize(1);
        var sent = fakePushSender.getSent().get(0);
        assertThat(sent.fcmToken()).isEqualTo("token-1");
        assertThat(sent.data()).containsEntry("coinId", "bitcoin").containsEntry("ruleType", "PRICE_MOVE");
    }

    @Test
    void priceMoveBelowThreshold_doesNotSend() {
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin"), "token-1")));
        fakeMarketDataClient.setSnapshots(Map.of(
                "bitcoin", new MarketDataClient.CoinMarketSnapshot("bitcoin", "Bitcoin", 65000.0, 1.0)));

        alertRuleService.runEvaluationPass();

        assertThat(fakePushSender.getSent()).isEmpty();
    }

    @Test
    void predictionConfidenceAlert_onlyFiresForCoinsWithFreshCachedPrediction() {
        // Watches two coins -- only "bitcoin" has a cached prediction, "ethereum" does not.
        // This is the explicit, documented limitation: prediction-confidence alerts can only
        // ever cover coins with an existing cache entry.
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin", "ethereum"), "token-1")));
        predictionCacheRepository.save(freshPrediction("bitcoin", 80.0, "UP"));

        alertRuleService.runEvaluationPass();

        assertThat(fakePushSender.getSent()).hasSize(1);
        var sent = fakePushSender.getSent().get(0);
        assertThat(sent.data()).containsEntry("coinId", "bitcoin").containsEntry("ruleType", "PREDICTION_CONFIDENCE");
    }

    @Test
    void expiredCachedPrediction_doesNotTriggerAlert() {
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin"), "token-1")));
        PredictionCacheEntity expired = freshPrediction("bitcoin", 95.0, "UP");
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        predictionCacheRepository.save(expired);

        alertRuleService.runEvaluationPass();

        assertThat(fakePushSender.getSent()).isEmpty();
    }

    @Test
    void cooldown_suppressesRepeatAlertWithinWindow() {
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin"), "token-1")));
        fakeMarketDataClient.setSnapshots(Map.of(
                "bitcoin", new MarketDataClient.CoinMarketSnapshot("bitcoin", "Bitcoin", 65000.0, 9.0)));

        alertRuleService.runEvaluationPass();
        alertRuleService.runEvaluationPass();

        assertThat(fakePushSender.getSent()).hasSize(1);
    }

    @Test
    void pushSenderFailure_doesNotRecordCooldown_andDoesNotThrow() {
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin"), "token-1")));
        fakeMarketDataClient.setSnapshots(Map.of(
                "bitcoin", new MarketDataClient.CoinMarketSnapshot("bitcoin", "Bitcoin", 65000.0, 9.0)));
        fakePushSender.setShouldThrow(true);

        assertThatCode(() -> alertRuleService.runEvaluationPass()).doesNotThrowAnyException();

        assertThat(fakePushSender.getSent()).isEmpty();
        assertThat(cooldownRepository.findByUserIdAndCoinIdAndRuleType("uid-1", "bitcoin", "PRICE_MOVE")).isEmpty();
    }

    @Test
    void watchlistReaderFailure_doesNotCrashScheduledEvaluate() {
        fakeWatchlistReader.setShouldThrow(true);

        // evaluate() -- the real @Scheduled entry point -- must swallow this internally.
        assertThatCode(() -> alertRuleService.evaluate()).doesNotThrowAnyException();
    }

    @Test
    void marketDataClientFailure_doesNotCrashScheduledEvaluate() {
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin"), "token-1")));
        fakeMarketDataClient.setShouldThrow(true);

        assertThatCode(() -> alertRuleService.evaluate()).doesNotThrowAnyException();
    }

    @Test
    void multipleUsersWatchingSameCoin_bothReceiveIndependentAlerts() {
        fakeWatchlistReader.setWatchlists(List.of(
                new WatchlistReader.UserWatchlist("uid-1", Set.of("bitcoin"), "token-1"),
                new WatchlistReader.UserWatchlist("uid-2", Set.of("bitcoin"), "token-2")));
        fakeMarketDataClient.setSnapshots(Map.of(
                "bitcoin", new MarketDataClient.CoinMarketSnapshot("bitcoin", "Bitcoin", 65000.0, 10.0)));

        alertRuleService.runEvaluationPass();

        assertThat(fakePushSender.getSent()).hasSize(2);
        assertThat(fakePushSender.getSent()).extracting(FakePushSender.SentMessage::fcmToken)
                .containsExactlyInAnyOrder("token-1", "token-2");
    }

    private static PredictionCacheEntity freshPrediction(String coinId, double confidence, String direction) {
        PredictionCacheEntity entity = new PredictionCacheEntity();
        entity.setCoinId(coinId);
        entity.setHorizon("24h");
        entity.setConfidence(BigDecimal.valueOf(confidence));
        entity.setDirection(direction);
        entity.setGeneratedAt(System.currentTimeMillis());
        entity.setExpiresAt(Instant.now().plusSeconds(600));
        return entity;
    }
}
