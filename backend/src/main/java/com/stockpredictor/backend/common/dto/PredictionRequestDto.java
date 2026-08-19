package com.stockpredictor.backend.common.dto;

import java.util.List;

/** Spring Boot -> FastAPI request body for POST /predict — matches ai-service's
 *  PredictionRequest Pydantic model field-for-field (coinId, history, btcHistory). */
public record PredictionRequestDto(String coinId, List<PricePointDto> history, List<PricePointDto> btcHistory) {
}
