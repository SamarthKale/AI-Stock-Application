package com.stockpredictor.app.model

import kotlinx.serialization.Serializable

@Serializable
data class PricePoint(
    val timestamp: Long,
    val price: Double,
    // Defaulted (not required) so kotlinx.serialization can still decode pre-Phase-5 cached
    // JSON rows in cached_price_history that were written before this field existed.
    val volume: Double = 0.0
)

/**
 * [id] is the CoinGecko coin id (e.g. "bitcoin") — the only safe key for follow-up API calls
 * (search, quote, history). [symbol] is the display ticker (e.g. "BTC"); never used as a lookup
 * key since multiple coins can share a symbol. Detail-only fields (from /coins/{id}) stay null
 * until that endpoint has actually been fetched — /coins/markets alone never populates them.
 */
data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String?,
    val currentPrice: Double,
    val marketCap: Long?,
    val marketCapRank: Int?,
    val totalVolume: Double?,
    val high24h: Double?,
    val low24h: Double?,
    val priceChange24h: Double,
    val priceChangePercentage24h: Double,
    val circulatingSupply: Double?,
    val totalSupply: Double?,
    val maxSupply: Double?,
    val ath: Double?,
    val athChangePercentage: Double?,
    val atl: Double?,
    val atlChangePercentage: Double?,
    val sparkline7d: List<Double>? = null,
    val history: List<PricePoint> = emptyList(),
    val description: String? = null,
    val lastUpdated: Long
)
