package com.stockpredictor.backend.prediction;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpredictor.backend.common.PredictionServiceUnavailableException;
import com.stockpredictor.backend.common.dto.PredictionRequestDto;
import com.stockpredictor.backend.common.dto.PredictionResponseDto;
import com.stockpredictor.backend.common.dto.PricePointDto;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Manual, opt-in verification that the REAL {@link FastApiPredictionClient} (not
 * {@link FakePredictionClient}, which every automated test uses instead) actually talks to a
 * real running ai-service over HTTP with matching JSON shapes on both sides — reads the same
 * raw training data ai-service/training/fetch_training_data.py already pulled, so this exercises
 * a realistic 366-point history rather than a hand-typed few points (the FastAPI endpoint
 * requires 41+ points before it can compute features at all). Disabled by default — depends on
 * `uvicorn main:app` running locally on :8000 (see ai-service/README.md) and the raw data files
 * existing; not something CI or a normal `./gradlew test` run should depend on. Remove
 * @Disabled temporarily to re-run this after changing either side of the Spring<->FastAPI contract.
 */
@Disabled("Manual verification only — requires a locally running ai-service (uvicorn on :8000)")
class FastApiPredictionClientLiveVerification {

    @Test
    void realClientThrowsPredictionServiceUnavailable_whenAiServiceIsDown() throws Exception {
        FastApiPredictionClient client = new FastApiPredictionClient("http://localhost:8000");
        ObjectMapper mapper = new ObjectMapper();
        List<PricePointDto> ethHistory = loadPoints(mapper, "ethereum");
        PredictionRequestDto request = new PredictionRequestDto("ethereum", ethHistory, ethHistory);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.predict(request))
                .isInstanceOf(PredictionServiceUnavailableException.class);
    }

    @Test
    void realClientAgainstRealFastApiService() throws Exception {
        FastApiPredictionClient client = new FastApiPredictionClient("http://localhost:8000");
        ObjectMapper mapper = new ObjectMapper();

        List<PricePointDto> ethHistory = loadPoints(mapper, "ethereum");
        List<PricePointDto> btcHistory = loadPoints(mapper, "bitcoin");

        PredictionRequestDto request = new PredictionRequestDto("ethereum", ethHistory, btcHistory);
        PredictionResponseDto response = client.predict(request);

        assertThat(response.coinId()).isEqualTo("ethereum");
        assertThat(response.confidence()).isBetween(0.0, 100.0);
        assertThat(response.direction()).isIn("UP", "DOWN", "FLAT");
        assertThat(response.horizon()).isEqualTo("24h");
    }

    private static List<PricePointDto> loadPoints(ObjectMapper mapper, String coinId) throws Exception {
        File file = new File("../ai-service/data/" + coinId + ".json");
        JsonNode root = mapper.readTree(file);
        JsonNode prices = root.get("prices");
        JsonNode volumes = root.get("total_volumes");

        List<PricePointDto> points = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            long timestamp = prices.get(i).get(0).asLong();
            double price = prices.get(i).get(1).asDouble();
            double volume = i < volumes.size() ? volumes.get(i).get(1).asDouble() : 0.0;
            points.add(new PricePointDto(timestamp, price, volume));
        }
        return points;
    }
}
