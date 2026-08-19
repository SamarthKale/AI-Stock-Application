package com.stockpredictor.backend.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Android -> Spring Boot request body for POST /api/predictions/{coinId}. coinId itself
 *  comes from the path, not the body. Android already has both series cached via
 *  CoinRepository (Phase 5 plan section 8 — no backend CoinGecko client needed). */
public record PredictionHistoryRequestDto(
        @NotEmpty @Valid List<PricePointDto> history,
        @NotEmpty @Valid List<PricePointDto> btcHistory
) {
}
