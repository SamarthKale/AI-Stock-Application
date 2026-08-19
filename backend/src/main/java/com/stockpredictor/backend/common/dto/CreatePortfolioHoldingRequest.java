package com.stockpredictor.backend.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePortfolioHoldingRequest(
        @NotBlank(message = "symbol is required") String symbol,
        @Positive(message = "quantity must be positive") BigDecimal quantity,
        @Positive(message = "avgBuyPrice must be positive") BigDecimal avgBuyPrice
) {
}
