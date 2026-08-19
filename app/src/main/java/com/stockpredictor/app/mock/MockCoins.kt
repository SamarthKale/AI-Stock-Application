package com.stockpredictor.app.mock

import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.PricePoint
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

/**
 * Coin ids are real CoinGecko ids (verified live against the API), not guessed — so this mock
 * data stays a drop-in shape match once real /coins/markets calls replace it.
 */
object MockCoins {

    val all: List<Coin> = listOf(
        Coin(
            id = "bitcoin", symbol = "BTC", name = "Bitcoin", image = null,
            currentPrice = 62384.50, marketCap = null, marketCapRank = 1, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = 912.30, priceChangePercentage24h = 1.48,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(61472.20, 30, 1.48), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "ethereum", symbol = "ETH", name = "Ethereum", image = null,
            currentPrice = 3418.75, marketCap = null, marketCapRank = 2, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = -26.40, priceChangePercentage24h = -0.77,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(3445.15, 30, -0.77), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "solana", symbol = "SOL", name = "Solana", image = null,
            currentPrice = 142.60, marketCap = null, marketCapRank = 5, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = -1.85, priceChangePercentage24h = -1.28,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(144.45, 30, -1.28), lastUpdated = System.currentTimeMillis()
        ),
        // Near-zero change: exercises PriceChangeChip's neutral rendering.
        Coin(
            id = "binancecoin", symbol = "BNB", name = "BNB", image = null,
            currentPrice = 574.20, marketCap = null, marketCapRank = 4, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = 0.15, priceChangePercentage24h = 0.03,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(574.05, 30, 0.0), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "ripple", symbol = "XRP", name = "XRP", image = null,
            currentPrice = 0.5842, marketCap = null, marketCapRank = 6, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = 0.0186, priceChangePercentage24h = 3.29,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(0.5656, 30, 3.29), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "cardano", symbol = "ADA", name = "Cardano", image = null,
            currentPrice = 0.3728, marketCap = null, marketCapRank = 9, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = -0.0094, priceChangePercentage24h = -2.46,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(0.3822, 30, -2.46), lastUpdated = System.currentTimeMillis()
        ),
        // Long history: exercises chart rendering with many points.
        Coin(
            id = "dogecoin", symbol = "DOGE", name = "Dogecoin", image = null,
            currentPrice = 0.1187, marketCap = null, marketCapRank = 8, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = 0.0021, priceChangePercentage24h = 1.80,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(0.1166, 180, 1.80), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "polkadot", symbol = "DOT", name = "Polkadot", image = null,
            currentPrice = 6.14, marketCap = null, marketCapRank = 14, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = -0.09, priceChangePercentage24h = -1.44,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(6.23, 30, -1.44), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "chainlink", symbol = "LINK", name = "Chainlink", image = null,
            currentPrice = 14.28, marketCap = null, marketCapRank = 15, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = 0.31, priceChangePercentage24h = 2.22,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(13.97, 30, 2.22), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "litecoin", symbol = "LTC", name = "Litecoin", image = null,
            currentPrice = 84.60, marketCap = null, marketCapRank = 22, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = 0.70, priceChangePercentage24h = 0.83,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(83.90, 30, 0.83), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "avalanche-2", symbol = "AVAX", name = "Avalanche", image = null,
            currentPrice = 27.35, marketCap = null, marketCapRank = 12, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = -0.21, priceChangePercentage24h = -0.76,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(27.56, 30, -0.76), lastUpdated = System.currentTimeMillis()
        ),
        Coin(
            id = "tron", symbol = "TRX", name = "TRON", image = null,
            currentPrice = 0.1642, marketCap = null, marketCapRank = 10, totalVolume = null,
            high24h = null, low24h = null, priceChange24h = 0.0038, priceChangePercentage24h = 2.37,
            circulatingSupply = null, totalSupply = null, maxSupply = null,
            ath = null, athChangePercentage = null, atl = null, atlChangePercentage = null,
            history = generateHistory(0.1604, 30, 2.37), lastUpdated = System.currentTimeMillis()
        )
    )

    fun findById(id: String): Coin? = all.firstOrNull { it.id == id }

    fun findBySymbol(symbol: String): Coin? = all.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
}
