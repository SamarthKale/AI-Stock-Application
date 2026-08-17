package com.stockpredictor.app.data.local.entity

import com.stockpredictor.app.model.PredictionDirection

/**
 * [cachedAt] is distinct from [generatedAt] so the UI can eventually show "prediction may
 * be stale" if the cache is old, ahead of Phase 5's real prediction pipeline.
 */
data class CachedPredictionEntity(
    val id: Long,
    val symbol: String,
    val confidence: Float,
    val direction: PredictionDirection,
    val targetPrice: Double?,
    val horizon: String,
    val generatedAt: Long,
    val cachedAt: Long,
)
