package com.stockpredictor.backend.common.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdatePortfolioHoldingRequest(
        @Positive(message = "quantity must be positive") BigDecimal quantity,
        @Positive(message = "avgBuyPrice must be positive") BigDecimal avgBuyPrice
) {
}
