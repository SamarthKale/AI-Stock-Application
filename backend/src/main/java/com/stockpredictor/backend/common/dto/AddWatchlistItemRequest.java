package com.stockpredictor.backend.common.dto;

import jakarta.validation.constraints.NotBlank;

public record AddWatchlistItemRequest(@NotBlank(message = "symbol is required") String symbol) {
}
