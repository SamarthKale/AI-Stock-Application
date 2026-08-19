package com.stockpredictor.app.data.repository

import com.stockpredictor.app.data.local.entity.CachedCoinEntity
import com.stockpredictor.app.model.Coin

/** Domain↔cache-entity mapping — distinct from data/remote/api/mapper's DTO↔domain mapping.
 *  [Coin.history]/[Coin.description] aren't persisted here (history has its own cache table via
 *  CachedPriceHistoryDao; description is detail-only and cheap to refetch, not worth a column). */
fun Coin.toCachedEntity(cachedAt: Long = System.currentTimeMillis()): CachedCoinEntity = CachedCoinEntity(
    coinId = id,
    symbol = symbol,
    name = name,
    imageUrl = image,
    currentPrice = currentPrice,
    priceChange24h = priceChange24h,
    priceChangePercentage24h = priceChangePercentage24h,
    marketCap = marketCap,
    marketCapRank = marketCapRank,
    totalVolume = totalVolume,
    high24h = high24h,
    low24h = low24h,
    circulatingSupply = circulatingSupply,
    totalSupply = totalSupply,
    maxSupply = maxSupply,
    ath = ath,
    athChangePercentage = athChangePercentage,
    atl = atl,
    atlChangePercentage = atlChangePercentage,
    sparkline7d = sparkline7d,
    cachedAt = cachedAt,
)

fun CachedCoinEntity.toCoin(): Coin = Coin(
    id = coinId,
    symbol = symbol,
    name = name,
    image = imageUrl,
    currentPrice = currentPrice,
    marketCap = marketCap,
    marketCapRank = marketCapRank,
    totalVolume = totalVolume,
    high24h = high24h,
    low24h = low24h,
    priceChange24h = priceChange24h,
    priceChangePercentage24h = priceChangePercentage24h,
    circulatingSupply = circulatingSupply,
    totalSupply = totalSupply,
    maxSupply = maxSupply,
    ath = ath,
    athChangePercentage = athChangePercentage,
    atl = atl,
    atlChangePercentage = atlChangePercentage,
    sparkline7d = sparkline7d,
    lastUpdated = cachedAt,
)
