package com.stockpredictor.app.mock

import com.stockpredictor.app.model.PortfolioHolding

object MockPortfolio {
    val holdings: List<PortfolioHolding> = listOf(
        PortfolioHolding("RELIANCE.NS", quantity = 12.0, avgBuyPrice = 2650.00, currentPrice = 2938.45),
        PortfolioHolding("TCS.NS", quantity = 5.0, avgBuyPrice = 3990.00, currentPrice = 3814.20),
        PortfolioHolding("HDFCBANK.NS", quantity = 20.0, avgBuyPrice = 1580.00, currentPrice = 1642.75),
        PortfolioHolding("AAPL", quantity = 8.0, avgBuyPrice = 195.30, currentPrice = 227.52),
        PortfolioHolding("WIPRO.BO", quantity = 40.0, avgBuyPrice = 512.00, currentPrice = 486.90)
    )
}
