package com.stockpredictor.app.data.local.entity

data class WatchlistEntity(
    val id: Long,
    val coinId: String,
    val symbol: String,
    val name: String?,
    val imageUrl: String?,
    val addedAt: Long,
    val sortOrder: Int,
    val updatedAt: Long,
)
