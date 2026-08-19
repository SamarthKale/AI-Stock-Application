package com.stockpredictor.app.data.repository

import android.content.Context
import com.stockpredictor.app.data.local.dao.CachedCoinDao
import com.stockpredictor.app.data.local.dao.CachedPriceHistoryDao
import com.stockpredictor.app.data.remote.api.CoinDataSource
import com.stockpredictor.app.data.remote.api.CoinGeckoDirectDataSource
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.model.TrendingCoin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

private const val MARKETS_TTL_MS = 60_000L
private const val DETAIL_TTL_MS = 120_000L
private const val HISTORY_TTL_MS = 6 * 60 * 60 * 1000L
private const val TRENDING_TTL_MS = 5 * 60 * 1000L
private const val VS_CURRENCY = "usd"
private const val HISTORY_DAYS = "30"
private const val HISTORY_RANGE_KEY = "30d"

// Phase 5: the prediction model needs ~40 days of warmup before its first usable feature row
// (see ai-service/features/feature_engineering.py's REQUIRED_WARMUP_DAYS) plus buffer — the
// 30-day chart-UI window above is too short. Cached under a *different* range_key in the same
// cached_price_history table so the two windows don't overwrite each other.
private const val PREDICTION_HISTORY_DAYS = "90"
private const val PREDICTION_HISTORY_RANGE_KEY = "90d"

/** [isStale] is true whenever [data] was served from the SQLite cache past its TTL because the
 *  network call itself failed — the UI marks these results as "may be out of date" rather than
 *  presenting them as fresh, per the Phase 4 migration plan's caching design. */
data class CoinFetchResult<T>(val data: T, val isStale: Boolean)

/**
 * Cache-then-network single source of truth for coin market data, backed by [CachedCoinDao]/
 * [CachedPriceHistoryDao] (SQLite) as the offline cache and [CoinDataSource] (CoinGecko, direct
 * for this phase — see CoinDataSource's doc) as the network source.
 *
 * Singleton (`getInstance`, matching [com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository]'s
 * established pattern in this codebase) — this is what lets Home/Watchlist/Crypto Detail all
 * share one SQLite-backed cache and one in-memory trending cache rather than each ViewModel
 * holding an isolated instance that can't see another screen's freshly-cached data. Never
 * called from a Composable directly — always through a ViewModel.
 *
 * Every public suspend function is `Mutex`-guarded per operation type (markets/detail/trending).
 * This was added specifically to close a defect found in the Step 3 audit: overlapping calls
 * (e.g. a ViewModel's own init-triggered refresh racing its screen's first LaunchedEffect, or
 * Home's concurrent watchlist/general-list/trending fetches) could previously race past the
 * freshness check and each independently decide network was needed, or clobber the in-memory
 * trending cache without synchronization. The mutexes are coarse — one per operation type, not
 * per cache key — which is a deliberate "where practical" tradeoff: today nothing in this app
 * requests two different coins' details or two different market-list shapes concurrently, so a
 * per-key lock would add complexity with no current benefit. If that changes, narrow the lock.
 */
class CoinRepository private constructor(
    context: Context,
    private val dataSource: CoinDataSource,
) {
    private val cachedCoinDao = CachedCoinDao(context.applicationContext)
    private val cachedHistoryDao = CachedPriceHistoryDao(context.applicationContext)

    private val marketsMutex = Mutex()
    private val detailMutex = Mutex()
    private val trendingMutex = Mutex()
    private val predictionHistoryMutex = Mutex()

    // Trending isn't persisted to SQLite (ephemeral discovery content, not part of the durable
    // offline cache) but still needs its own TTL so repeated Home refreshes within a short
    // window don't re-hit the network for it — this plain mutable state is only ever touched
    // from inside trendingMutex.withLock, so it's safe without a dedicated atomic/volatile type.
    private var cachedTrending: List<TrendingCoin>? = null
    private var trendingCachedAt: Long = 0L

    /**
     * [ids] == null means the general top-market-cap list (Home's movers/trending source).
     * [ids] non-null is a batched watchlist price lookup. Both are now TTL-gated the same way —
     * serve the SQLite cache without a network call whenever every requested coin (all of them
     * for the general list, exactly the requested set for a batched lookup) was cached within
     * [MARKETS_TTL_MS]. This is the fix for the Step 3 audit's finding: previously only the
     * `ids == null` path checked freshness, so watchlist re-entry always hit network regardless
     * of how recently it had already been fetched.
     */
    suspend fun getMarkets(ids: List<String>?): CoinFetchResult<List<Coin>> = marketsMutex.withLock {
        val cached = if (ids == null) cachedCoinDao.getAll() else ids.mapNotNull { cachedCoinDao.getByCoinId(it) }
        val fresh = cached.isNotEmpty() &&
            (ids == null || cached.size == ids.size) &&
            cached.all { System.currentTimeMillis() - it.cachedAt < MARKETS_TTL_MS }
        if (fresh) {
            return@withLock CoinFetchResult(cached.map { it.toCoin() }, isStale = false)
        }

        try {
            val coins = dataSource.getMarkets(ids, perPage = 100)
            coins.forEach { cachedCoinDao.upsert(it.toCachedEntity()) }
            CoinFetchResult(coins, isStale = false)
        } catch (e: CancellationException) {
            throw e // never treat "this call was cancelled" as "the network call failed"
        } catch (e: Exception) {
            val fallback = (if (ids != null) ids.mapNotNull { cachedCoinDao.getByCoinId(it) } else cachedCoinDao.getAll())
                .map { it.toCoin() }
            if (fallback.isNotEmpty()) CoinFetchResult(fallback, isStale = true) else throw e.toCoinDataException()
        }
    }

    /** Merges /coins/{id} (quote+detail) and /coins/{id}/market_chart (history) into one [Coin],
     *  each independently cache-then-network so a history-only or detail-only failure doesn't
     *  necessarily blank out the other. */
    suspend fun getCoinDetail(coinId: String): CoinFetchResult<Coin> = detailMutex.withLock {
        val cachedCoin = cachedCoinDao.getByCoinId(coinId)
        val cachedHistory = cachedHistoryDao.get(coinId, VS_CURRENCY, HISTORY_RANGE_KEY)
        val coinFresh = cachedCoin != null && System.currentTimeMillis() - cachedCoin.cachedAt < DETAIL_TTL_MS
        val historyFresh = cachedHistory != null && System.currentTimeMillis() - cachedHistory.cachedAt < HISTORY_TTL_MS
        if (coinFresh && historyFresh) {
            return@withLock CoinFetchResult(cachedCoin!!.toCoin().copy(history = cachedHistory!!.points), isStale = false)
        }

        try {
            coroutineScope {
                val detailDeferred = async { dataSource.getCoinDetail(coinId) }
                val historyDeferred = async {
                    try {
                        dataSource.getMarketChart(coinId, HISTORY_DAYS)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        cachedHistory?.points.orEmpty()
                    }
                }
                val detail = detailDeferred.await()
                val history = historyDeferred.await()
                cachedCoinDao.upsert(detail.toCachedEntity())
                if (history.isNotEmpty()) cachedHistoryDao.upsert(coinId, VS_CURRENCY, HISTORY_RANGE_KEY, history)
                CoinFetchResult(detail.copy(history = history), isStale = false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val fallbackCoin = cachedCoin ?: cachedCoinDao.getByCoinId(coinId)
            when {
                fallbackCoin != null ->
                    CoinFetchResult(fallbackCoin.toCoin().copy(history = cachedHistory?.points.orEmpty()), isStale = true)
                e is HttpException && e.code() == 404 -> throw CoinNotFoundException(coinId)
                else -> throw e.toCoinDataException()
            }
        }
    }

    /** Additive/discovery content, not load-bearing — silently falls back to whatever was last
     *  cached (even past TTL) rather than surfacing a whole-screen error for a secondary Home
     *  section; only truly empty (never fetched, network also failing) returns an empty list. */
    suspend fun getTrending(): List<TrendingCoin> = trendingMutex.withLock {
        val cached = cachedTrending
        if (cached != null && System.currentTimeMillis() - trendingCachedAt < TRENDING_TTL_MS) {
            return@withLock cached
        }
        val fetched = try {
            dataSource.getTrending()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (fetched != null) {
            cachedTrending = fetched
            trendingCachedAt = System.currentTimeMillis()
            fetched
        } else {
            cached.orEmpty()
        }
    }

    /** Longer history window specifically for [com.stockpredictor.app.data.repository.PredictionRepository]
     *  — separate cache slot from [getCoinDetail]'s 30-day chart-UI window (Phase 5 plan section 3:
     *  "do not conflate the two"). Falls back to a stale cached copy on network failure, same
     *  pattern as every other fetch in this class; throws if there's no cache to fall back to. */
    suspend fun getPriceHistoryForPrediction(coinId: String): CoinFetchResult<List<PricePoint>> = predictionHistoryMutex.withLock {
        val cached = cachedHistoryDao.get(coinId, VS_CURRENCY, PREDICTION_HISTORY_RANGE_KEY)
        val fresh = cached != null && System.currentTimeMillis() - cached.cachedAt < HISTORY_TTL_MS
        if (fresh) {
            return@withLock CoinFetchResult(cached!!.points, isStale = false)
        }

        try {
            val history = dataSource.getMarketChart(coinId, PREDICTION_HISTORY_DAYS)
            if (history.isNotEmpty()) {
                cachedHistoryDao.upsert(coinId, VS_CURRENCY, PREDICTION_HISTORY_RANGE_KEY, history)
            }
            CoinFetchResult(history, isStale = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (cached != null) CoinFetchResult(cached.points, isStale = true) else throw e.toCoinDataException()
        }
    }

    companion object {
        @Volatile private var instance: CoinRepository? = null

        fun getInstance(context: Context, dataSource: CoinDataSource = CoinGeckoDirectDataSource()): CoinRepository =
            instance ?: synchronized(this) {
                instance ?: CoinRepository(context.applicationContext, dataSource).also { instance = it }
            }
    }
}
