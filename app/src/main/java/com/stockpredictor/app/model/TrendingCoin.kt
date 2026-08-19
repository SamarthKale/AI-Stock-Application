package com.stockpredictor.app.model

/** Result row from CoinGecko's /search/trending. */
data class TrendingCoin(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String?,
    val marketCapRank: Int?
)
