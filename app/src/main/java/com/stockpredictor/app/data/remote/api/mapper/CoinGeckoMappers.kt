package com.stockpredictor.app.data.remote.api.mapper

import com.stockpredictor.app.data.remote.api.dto.CoinDetailDto
import com.stockpredictor.app.data.remote.api.dto.CoinMarketDto
import com.stockpredictor.app.data.remote.api.dto.CoinSearchItemDto
import com.stockpredictor.app.data.remote.api.dto.MarketChartResponseDto
import com.stockpredictor.app.data.remote.api.dto.TrendingCoinItemDto
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.CoinSearchResult
import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.model.TrendingCoin
import java.time.Instant
import java.time.format.DateTimeParseException

/**
 * Only this file (plus data/repository's cache-entity mappers) ever sees a CoinGecko DTO —
 * ViewModels and composables only ever see [Coin]/[CoinSearchResult]/[TrendingCoin].
 */
fun CoinMarketDto.toCoin(): Coin = Coin(
    id = id,
    symbol = symbol.uppercase(),
    name = name,
    image = image,
    currentPrice = currentPrice ?: 0.0,
    marketCap = marketCap,
    marketCapRank = marketCapRank,
    totalVolume = totalVolume,
    high24h = high24h,
    low24h = low24h,
    priceChange24h = priceChange24h ?: 0.0,
    priceChangePercentage24h = priceChangePercentage24h ?: 0.0,
    circulatingSupply = circulatingSupply,
    totalSupply = totalSupply,
    maxSupply = maxSupply,
    ath = ath,
    athChangePercentage = athChangePercentage,
    atl = atl,
    atlChangePercentage = atlChangePercentage,
    sparkline7d = sparklineIn7d?.price,
    lastUpdated = parseIsoInstant(lastUpdated),
)

fun CoinDetailDto.toCoin(): Coin {
    val market = marketData
    return Coin(
        id = id,
        symbol = symbol.uppercase(),
        name = name,
        image = image?.large ?: image?.small ?: image?.thumb,
        currentPrice = market?.currentPrice?.get("usd") ?: 0.0,
        marketCap = market?.marketCap?.get("usd")?.toLong(),
        marketCapRank = market?.marketCapRank,
        totalVolume = market?.totalVolume?.get("usd"),
        high24h = market?.high24h?.get("usd"),
        low24h = market?.low24h?.get("usd"),
        priceChange24h = market?.priceChange24h ?: 0.0,
        priceChangePercentage24h = market?.priceChangePercentage24h ?: 0.0,
        circulatingSupply = market?.circulatingSupply,
        totalSupply = market?.totalSupply,
        maxSupply = market?.maxSupply,
        ath = market?.ath?.get("usd"),
        athChangePercentage = market?.athChangePercentage?.get("usd"),
        atl = market?.atl?.get("usd"),
        atlChangePercentage = market?.atlChangePercentage?.get("usd"),
        description = description?.en?.takeIf { it.isNotBlank() },
        lastUpdated = parseIsoInstant(market?.lastUpdated),
    )
}

fun MarketChartResponseDto.toPricePoints(): List<PricePoint> {
    val volumeByTimestamp = totalVolumes.mapNotNull { point ->
        if (point.size < 2) null else point[0].toLong() to point[1]
    }.toMap()
    return prices.mapNotNull { point ->
        if (point.size < 2) null else {
            val timestamp = point[0].toLong()
            PricePoint(timestamp = timestamp, price = point[1], volume = volumeByTimestamp[timestamp] ?: 0.0)
        }
    }
}

fun CoinSearchItemDto.toCoinSearchResult(): CoinSearchResult = CoinSearchResult(
    id = id,
    symbol = symbol.uppercase(),
    name = name,
    image = large ?: thumb,
    marketCapRank = marketCapRank,
)

fun TrendingCoinItemDto.toTrendingCoin(): TrendingCoin = TrendingCoin(
    id = id,
    symbol = symbol.uppercase(),
    name = name,
    image = large ?: thumb,
    marketCapRank = marketCapRank,
)

private fun parseIsoInstant(value: String?): Long {
    if (value.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        Instant.parse(value).toEpochMilli()
    } catch (e: DateTimeParseException) {
        System.currentTimeMillis()
    }
}
