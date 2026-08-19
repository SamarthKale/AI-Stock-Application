package com.stockpredictor.backend.prediction;

import com.stockpredictor.backend.common.PredictionServiceUnavailableException;
import com.stockpredictor.backend.common.dto.PredictionRequestDto;
import com.stockpredictor.backend.common.dto.PredictionResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Production {@link PredictionClient} — calls the FastAPI prediction service via Spring's
 * {@link RestClient} (already available from spring-boot-starter-web, no new Gradle dependency
 * — Phase 5 plan section 12). This is the ONLY place in the backend that talks to ai-service;
 * Python itself never talks to the market-data provider, and Android never talks to ai-service
 * directly (Phase 5 plan section 8).
 */
@Component
public class FastApiPredictionClient implements PredictionClient {

    private final RestClient restClient;

    public FastApiPredictionClient(@Value("${prediction-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public PredictionResponseDto predict(PredictionRequestDto request) {
        try {
            return restClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(PredictionResponseDto.class);
        } catch (RestClientException e) {
            throw new PredictionServiceUnavailableException("Prediction service is unavailable", e);
        }
    }
}
