package com.stockpredictor.app.data.local.entity

data class WatchlistEntity(
    val id: Long,
    val symbol: String,
    val addedAt: Long,
    val sortOrder: Int,
)
