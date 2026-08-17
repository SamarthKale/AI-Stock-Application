package com.stockpredictor.app.data.local.entity

data class RecentSearchEntity(
    val id: Long,
    val query: String,
    val searchedAt: Long,
)
