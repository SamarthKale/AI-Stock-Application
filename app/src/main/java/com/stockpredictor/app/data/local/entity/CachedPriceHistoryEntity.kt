package com.stockpredictor.app.data.local.entity

import com.stockpredictor.app.model.PricePoint

data class CachedPriceHistoryEntity(
    val id: Long,
    val coinId: String,
    val vsCurrency: String,
    val rangeKey: String,
    val points: List<PricePoint>,
    val cachedAt: Long,
)
