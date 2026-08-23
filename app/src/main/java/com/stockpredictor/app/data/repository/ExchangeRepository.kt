package com.stockpredictor.app.data.repository

import com.stockpredictor.app.data.ExchangeData
import com.stockpredictor.app.data.ExchangeLocation
import com.stockpredictor.app.data.remote.api.CoinGeckoApiService
import com.stockpredictor.app.data.remote.api.RetrofitClient
import com.stockpredictor.app.data.remote.api.dto.ExchangeDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ExchangeMarketInfo(
    val location: ExchangeLocation,
    val tradeVolume24hBtc: Double?,
    val trustScore: Int?,
)

private const val TTL_MS = 10 * 60 * 1000L // 10 minutes -- exchange volume moves slowly enough that

/**
 * Live 24h-volume/trust-score data for the static [ExchangeData] list, joined at read time.
 * In-memory-only cache (no SQLite table) -- same precedent as [CoinRepository]'s trending cache:
 * ephemeral discovery-style content doesn't need durable offline storage the way watchlist/coin
 * data does. Singleton so every screen visit within the TTL window shares one cached result
 * instead of re-fetching.
 */
class ExchangeRepository(
    private val api: CoinGeckoApiService = RetrofitClient.coinGeckoApi,
) {
    private val mutex = Mutex()
    private var cached: List<ExchangeMarketInfo>? = null
    private var cachedAt: Long = 0L

    suspend fun getExchanges(): List<ExchangeMarketInfo> = mutex.withLock {
        val now = System.currentTimeMillis()
        cached?.let { if (now - cachedAt < TTL_MS) return@withLock it }

        try {
            val live: List<ExchangeDto> = api.getExchanges(perPage = 100, page = 1)
            val byId = live.associateBy { it.id }
            val result = ExchangeData.all.map { location ->
                val dto = byId[location.id]
                ExchangeMarketInfo(
                    location = location,
                    tradeVolume24hBtc = dto?.tradeVolume24hBtc,
                    trustScore = dto?.trustScore,
                )
            }
            cached = result
            cachedAt = now
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Serve stale cache past its TTL on failure, same fallback philosophy as
            // CoinRepository -- never propagate a raw exception for a secondary discovery screen.
            cached ?: ExchangeData.all.map { ExchangeMarketInfo(it, null, null) }
        }
    }

    companion object {
        @Volatile private var instance: ExchangeRepository? = null

        fun getInstance(): ExchangeRepository =
            instance ?: synchronized(this) {
                instance ?: ExchangeRepository().also { instance = it }
            }
    }
}
