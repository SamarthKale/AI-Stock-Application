package com.stockpredictor.backend.common.dto;

/** Field names matched to Android's model/Coin.kt PricePoint (timestamp, price) plus
 *  volume, added for Phase 5's feature engineering (see ai-service/features/feature_engineering.py). */
public record PricePointDto(Long timestamp, Double price, Double volume) {
}
