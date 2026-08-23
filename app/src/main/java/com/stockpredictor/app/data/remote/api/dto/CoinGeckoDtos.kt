package com.stockpredictor.app.data.remote.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Flat per-currency fields (vs_currency is a single query param for this endpoint) — do not
 *  confuse with [MarketDataDto]'s per-currency maps, which /coins/{id} returns instead. */
@Serializable
data class CoinMarketDto(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String? = null,
    @SerialName("current_price") val currentPrice: Double? = null,
    @SerialName("market_cap") val marketCap: Long? = null,
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
    @SerialName("total_volume") val totalVolume: Double? = null,
    @SerialName("high_24h") val high24h: Double? = null,
    @SerialName("low_24h") val low24h: Double? = null,
    @SerialName("price_change_24h") val priceChange24h: Double? = null,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double? = null,
    @SerialName("circulating_supply") val circulatingSupply: Double? = null,
    @SerialName("total_supply") val totalSupply: Double? = null,
    @SerialName("max_supply") val maxSupply: Double? = null,
    val ath: Double? = null,
    @SerialName("ath_change_percentage") val athChangePercentage: Double? = null,
    val atl: Double? = null,
    @SerialName("atl_change_percentage") val atlChangePercentage: Double? = null,
    @SerialName("sparkline_in_7d") val sparklineIn7d: SparklineDto? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
)

@Serializable
data class SparklineDto(val price: List<Double>? = null)

@Serializable
data class CoinDetailDto(
    val id: String,
    val symbol: String,
    val name: String,
    val image: CoinImageDto? = null,
    val description: DescriptionDto? = null,
    @SerialName("market_data") val marketData: MarketDataDto? = null,
)

@Serializable
data class CoinImageDto(val thumb: String? = null, val small: String? = null, val large: String? = null)

@Serializable
data class DescriptionDto(val en: String? = null)

/** /coins/{id} returns every supported currency at once, keyed by lowercase ISO code — unlike
 *  [CoinMarketDto]'s flat fields. Only "usd" is read for now (see CoinGeckoMappers). */
@Serializable
data class MarketDataDto(
    @SerialName("current_price") val currentPrice: Map<String, Double>? = null,
    @SerialName("market_cap") val marketCap: Map<String, Double>? = null,
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
    @SerialName("total_volume") val totalVolume: Map<String, Double>? = null,
    @SerialName("high_24h") val high24h: Map<String, Double>? = null,
    @SerialName("low_24h") val low24h: Map<String, Double>? = null,
    @SerialName("price_change_24h") val priceChange24h: Double? = null,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double? = null,
    @SerialName("circulating_supply") val circulatingSupply: Double? = null,
    @SerialName("total_supply") val totalSupply: Double? = null,
    @SerialName("max_supply") val maxSupply: Double? = null,
    val ath: Map<String, Double>? = null,
    @SerialName("ath_change_percentage") val athChangePercentage: Map<String, Double>? = null,
    val atl: Map<String, Double>? = null,
    @SerialName("atl_change_percentage") val atlChangePercentage: Map<String, Double>? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
)

@Serializable
data class CoinSearchResponseDto(val coins: List<CoinSearchItemDto> = emptyList())

@Serializable
data class CoinSearchItemDto(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
    val thumb: String? = null,
    val large: String? = null,
)

@Serializable
data class TrendingResponseDto(val coins: List<TrendingItemDto> = emptyList())

@Serializable
data class TrendingItemDto(val item: TrendingCoinItemDto)

@Serializable
data class TrendingCoinItemDto(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
    val thumb: String? = null,
    val large: String? = null,
)

/** [prices]/[totalVolumes] are arrays of [timestampMs, value] pairs — CoinGecko returns them as
 *  raw two-element number arrays, not objects, hence List<List<Double>> rather than a data class.
 *  [totalVolumes] added in Phase 5 for feature engineering (ai-service needs a volume series,
 *  not just price); harmless to keep fetching even for screens that only chart price. */
/** Phase 5c exchange map -- live-verified fields (no lat/lng; country is a registered
 *  jurisdiction, not a trading-floor location, since crypto exchanges are online-only). */
@Serializable
data class ExchangeDto(
    val id: String,
    val name: String,
    val country: String? = null,
    @SerialName("trust_score") val trustScore: Int? = null,
    @SerialName("trade_volume_24h_btc") val tradeVolume24hBtc: Double? = null,
)

@Serializable
data class MarketChartResponseDto(
    val prices: List<List<Double>> = emptyList(),
    @SerialName("total_volumes") val totalVolumes: List<List<Double>> = emptyList(),
)
