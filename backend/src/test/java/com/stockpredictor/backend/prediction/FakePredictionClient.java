package com.stockpredictor.backend.prediction;

import com.stockpredictor.backend.common.PredictionServiceUnavailableException;
import com.stockpredictor.backend.common.dto.PredictionRequestDto;
import com.stockpredictor.backend.common.dto.PredictionResponseDto;
import java.util.concurrent.atomic.AtomicInteger;

/** Test double avoiding the need for a live FastAPI process during backend tests — same
 *  reasoning as FakeFirebaseTokenVerifier avoiding a live Firebase project. Counts calls so
 *  tests can assert PredictionService's Postgres cache actually short-circuits a second
 *  identical request rather than just computing a TTL correctly on paper. */
public class FakePredictionClient implements PredictionClient {

    public static final String UNAVAILABLE_COIN_ID = "trigger-unavailable";

    private final AtomicInteger callCount = new AtomicInteger(0);

    @Override
    public PredictionResponseDto predict(PredictionRequestDto request) {
        callCount.incrementAndGet();
        if (UNAVAILABLE_COIN_ID.equals(request.coinId())) {
            throw new PredictionServiceUnavailableException("Prediction service is unavailable", null);
        }
        return new PredictionResponseDto(request.coinId(), 71.5, "UP", 65000.0, "24h", 1_700_000_000_000L);
    }

    public int callCount() {
        return callCount.get();
    }
}
