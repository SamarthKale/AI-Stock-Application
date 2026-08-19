package com.stockpredictor.backend.common.dto;

/** Field names matched to Android's model/Prediction.kt (confidence, direction, targetPrice,
 *  horizon, generatedAt) and to ai-service's PredictionResponse Pydantic model — locked in
 *  all three places together per the Phase 5 plan (single-confidence output, no probability split). */
public record PredictionResponseDto(
        String coinId,
        Double confidence,
        String direction,
        Double targetPrice,
        String horizon,
        Long generatedAt
) {
}
