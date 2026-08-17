package com.stockpredictor.app.model

data class WatchlistItem(
    val symbol: String,
    val addedAt: Long,
    val sortOrder: Int
)
