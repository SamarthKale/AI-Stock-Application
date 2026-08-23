package com.stockpredictor.backend.alerts;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** No Spring context needed -- AlertEvaluator is pure logic, deliberately separated from
 *  Firestore/CoinGecko/FCM/Postgres I/O for exactly this kind of fast, isolated test. */
class AlertEvaluatorTest {

    private final AlertEvaluator evaluator = new AlertEvaluator(5.0, 70.0);

    @Test
    void priceMove_belowThreshold_doesNotFire() {
        assertThat(evaluator.evaluatePriceMove(3.0, "Bitcoin")).isEmpty();
    }

    @Test
    void priceMove_exactlyAtThreshold_fires() {
        assertThat(evaluator.evaluatePriceMove(5.0, "Bitcoin")).isPresent();
    }

    @Test
    void priceMove_aboveThreshold_fires_up() {
        Optional<String> result = evaluator.evaluatePriceMove(7.5, "Bitcoin");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Bitcoin").contains("up").contains("7.5");
    }

    @Test
    void priceMove_aboveThreshold_fires_down() {
        Optional<String> result = evaluator.evaluatePriceMove(-8.2, "Ethereum");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Ethereum").contains("down").contains("8.2");
    }

    @Test
    void priceMove_nullValue_doesNotFire() {
        assertThat(evaluator.evaluatePriceMove(null, "Bitcoin")).isEmpty();
    }

    @Test
    void predictionConfidence_belowThreshold_doesNotFire() {
        assertThat(evaluator.evaluatePredictionConfidence(65.0, "UP", "Bitcoin")).isEmpty();
    }

    @Test
    void predictionConfidence_atOrAboveThreshold_fires() {
        Optional<String> result = evaluator.evaluatePredictionConfidence(75.0, "UP", "Bitcoin");
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Bitcoin").contains("UP").contains("75");
    }

    @Test
    void predictionConfidence_nullConfidence_doesNotFire() {
        assertThat(evaluator.evaluatePredictionConfidence(null, "UP", "Bitcoin")).isEmpty();
    }

    @Test
    void predictionConfidence_nullDirection_doesNotFire() {
        assertThat(evaluator.evaluatePredictionConfidence(90.0, null, "Bitcoin")).isEmpty();
    }
}
