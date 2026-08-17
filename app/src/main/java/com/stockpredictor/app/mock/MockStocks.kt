package com.stockpredictor.app.mock

import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.model.Stock
import kotlin.math.sin

private const val DAY_MS = 24L * 60 * 60 * 1000

/**
 * Deterministic (non-random) synthetic history so previews/screenshots are stable:
 * a linear trend toward [basePrice] plus a smooth sine ripple for visual texture.
 */
private fun generateHistory(
    startPrice: Double,
    days: Int,
    trendPercent: Double,
    now: Long = System.currentTimeMillis()
): List<PricePoint> {
    val points = mutableListOf<PricePoint>()
    for (daysAgo in days downTo 0) {
        val progress = (days - daysAgo).toDouble() / days
        val trend = startPrice * (trendPercent / 100.0) * progress
        val ripple = startPrice * 0.012 * sin(daysAgo * 0.6)
        val price = (startPrice + trend + ripple).coerceAtLeast(startPrice * 0.5)
        points.add(PricePoint(timestamp = now - daysAgo * DAY_MS, price = price))
    }
    return points
}

object MockStocks {

    val all: List<Stock> = listOf(
        Stock(
            symbol = "RELIANCE.NS",
            name = "Reliance Industries",
            exchange = "NSE",
            price = 2938.45,
            change = 42.10,
            changePercent = 1.45,
            history = generateHistory(2896.35, 30, 1.45),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "TCS.NS",
            name = "Tata Consultancy Services",
            exchange = "NSE",
            price = 3814.20,
            change = -18.60,
            changePercent = -0.48,
            history = generateHistory(3832.80, 30, -0.48),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "INFY.NS",
            name = "Infosys",
            exchange = "NSE",
            price = 1598.10,
            change = -12.35,
            changePercent = -0.77,
            history = generateHistory(1610.45, 30, -0.77),
            lastUpdated = System.currentTimeMillis()
        ),
        // Near-zero change: exercises PriceChangeChip's neutral rendering.
        Stock(
            symbol = "HDFCBANK.NS",
            name = "HDFC Bank",
            exchange = "NSE",
            price = 1642.75,
            change = 0.05,
            changePercent = 0.00,
            history = generateHistory(1642.70, 30, 0.0),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "TATASTEEL.BO",
            name = "Tata Steel",
            exchange = "BSE",
            price = 148.30,
            change = 3.85,
            changePercent = 2.66,
            history = generateHistory(144.45, 30, 2.66),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "WIPRO.BO",
            name = "Wipro",
            exchange = "BSE",
            price = 486.90,
            change = -6.40,
            changePercent = -1.30,
            history = generateHistory(493.30, 30, -1.30),
            lastUpdated = System.currentTimeMillis()
        ),
        // Long history: exercises chart rendering with many points.
        Stock(
            symbol = "ICICIBANK.NS",
            name = "ICICI Bank",
            exchange = "NSE",
            price = 1187.55,
            change = 15.20,
            changePercent = 1.30,
            history = generateHistory(1172.35, 180, 1.30),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "BAJFINANCE.NS",
            name = "Bajaj Finance",
            exchange = "NSE",
            price = 7145.00,
            change = -84.30,
            changePercent = -1.17,
            history = generateHistory(7229.30, 30, -1.17),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "SBIN.BO",
            name = "State Bank of India",
            exchange = "BSE",
            price = 812.65,
            change = 9.40,
            changePercent = 1.17,
            history = generateHistory(803.25, 30, 1.17),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "AAPL",
            name = "Apple Inc.",
            exchange = "NASDAQ",
            price = 227.52,
            change = 1.85,
            changePercent = 0.82,
            history = generateHistory(225.67, 30, 0.82),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "MSFT",
            name = "Microsoft Corp.",
            exchange = "NASDAQ",
            price = 415.30,
            change = -3.20,
            changePercent = -0.77,
            history = generateHistory(418.50, 30, -0.77),
            lastUpdated = System.currentTimeMillis()
        ),
        Stock(
            symbol = "ADANIENT.NS",
            name = "Adani Enterprises",
            exchange = "NSE",
            price = 2456.80,
            change = 58.75,
            changePercent = 2.45,
            history = generateHistory(2398.05, 30, 2.45),
            lastUpdated = System.currentTimeMillis()
        )
    )

    fun findBySymbol(symbol: String): Stock? = all.firstOrNull { it.symbol == symbol }
}
