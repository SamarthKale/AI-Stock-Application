package com.stockpredictor.app.model

data class PricePoint(
    val timestamp: Long,
    val price: Double
)

data class Stock(
    val symbol: String,
    val name: String,
    val exchange: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val history: List<PricePoint>,
    val lastUpdated: Long
)
