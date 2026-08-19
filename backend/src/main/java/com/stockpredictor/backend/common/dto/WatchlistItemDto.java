package com.stockpredictor.backend.common.dto;

/** Field names/types matched to Android's model/WatchlistItem.kt (symbol, addedAt, sortOrder),
 *  plus the backend-only id needed for delete/reorder addressing. */
public record WatchlistItemDto(Long id, String symbol, Long addedAt, Integer sortOrder) {
}
