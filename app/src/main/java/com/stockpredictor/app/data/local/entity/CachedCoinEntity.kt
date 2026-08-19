package com.stockpredictor.app.data.local.entity

/** [cachedAt] drives the cache-then-network TTL check in the future CoinRepository (Phase 4
 *  step 3+) and lets the UI mark data stale when served past its TTL on a network failure. */
data class CachedCoinEntity(
    val coinId: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val currentPrice: Double,
    val priceChange24h: Double,
    val priceChangePercentage24h: Double,
    val marketCap: Long?,
    val marketCapRank: Int?,
    val totalVolume: Double?,
    val high24h: Double?,
    val low24h: Double?,
    val circulatingSupply: Double?,
    val totalSupply: Double?,
    val maxSupply: Double?,
    val ath: Double?,
    val athChangePercentage: Double?,
    val atl: Double?,
    val atlChangePercentage: Double?,
    val sparkline7d: List<Double>?,
    val cachedAt: Long,
)
