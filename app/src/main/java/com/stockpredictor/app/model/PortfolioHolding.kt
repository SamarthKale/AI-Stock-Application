package com.stockpredictor.app.model

data class PortfolioHolding(
    val symbol: String,
    val quantity: Double,
    val avgBuyPrice: Double,
    val currentPrice: Double
) {
    val currentValue: Double
        get() = quantity * currentPrice

    val gainLossValue: Double
        get() = quantity * (currentPrice - avgBuyPrice)

    val gainLossPercent: Double
        get() = if (avgBuyPrice == 0.0) 0.0 else ((currentPrice - avgBuyPrice) / avgBuyPrice) * 100.0
}
