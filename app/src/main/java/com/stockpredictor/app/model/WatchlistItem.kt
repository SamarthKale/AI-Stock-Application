package com.stockpredictor.app.model

data class WatchlistItem(
    val coinId: String,
    val symbol: String,
    val name: String?,
    val imageUrl: String?,
    val addedAt: Long,
    val sortOrder: Int
)
