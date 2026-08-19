package com.stockpredictor.backend.common.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** symbol/quantity/avgBuyPrice match Android's model/PortfolioHolding.kt field-for-field (as
 *  JSON numbers, same as its Double fields). currentPrice/currentValue/gainLoss are deliberately
 *  absent — those are live-market-derived and computed on the Android side (Phase 4), never
 *  persisted here. createdAt/updatedAt are backend-only bookkeeping until Android gains local
 *  portfolio persistence. */
public record PortfolioHoldingDto(Long id, String symbol, BigDecimal quantity, BigDecimal avgBuyPrice,
                                   Instant createdAt, Instant updatedAt) {
}
