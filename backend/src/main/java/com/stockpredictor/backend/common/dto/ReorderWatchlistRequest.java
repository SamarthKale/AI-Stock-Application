package com.stockpredictor.backend.common.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderWatchlistRequest(@NotEmpty(message = "orderedIds must not be empty") List<Long> orderedIds) {
}
