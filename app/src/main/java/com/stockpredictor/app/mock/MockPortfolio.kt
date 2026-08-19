package com.stockpredictor.app.mock

import com.stockpredictor.app.model.PortfolioHolding

/** `symbol` holds the display ticker (matches MockCoins.symbol); PortfolioViewModel resolves
 *  it to a coin id via MockCoins.findBySymbol for navigation, since PortfolioHolding itself
 *  carries no coin id field. */
object MockPortfolio {
    val holdings: List<PortfolioHolding> = listOf(
        PortfolioHolding("BTC", quantity = 0.42, avgBuyPrice = 54200.00, currentPrice = 62384.50),
        PortfolioHolding("ETH", quantity = 3.1, avgBuyPrice = 3610.00, currentPrice = 3418.75),
        PortfolioHolding("BNB", quantity = 6.0, avgBuyPrice = 520.00, currentPrice = 574.20),
        PortfolioHolding("SOL", quantity = 18.0, avgBuyPrice = 128.40, currentPrice = 142.60),
        PortfolioHolding("LTC", quantity = 12.0, avgBuyPrice = 91.50, currentPrice = 84.60)
    )
}
