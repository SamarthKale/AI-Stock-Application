package com.stockpredictor.app.data.remote.api

import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.CoinSearchResult
import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.model.TrendingCoin

/**
 * Abstraction over "where coin market data actually comes from" — [CoinGeckoDirectDataSource]
 * calls CoinGecko directly (this phase's design: Android→CoinGecko, no backend proxy yet). A
 * future SpringBootProxyDataSource can implement this same interface to route through the
 * backend instead, without CoinRepository/CoinSearchRepository or any ViewModel changing.
 */
interface CoinDataSource {
    suspend fun getMarkets(ids: List<String>?, perPage: Int): List<Coin>
    suspend fun getCoinDetail(coinId: String): Coin
    suspend fun getMarketChart(coinId: String, days: String): List<PricePoint>
    suspend fun search(query: String): List<CoinSearchResult>
    suspend fun getTrending(): List<TrendingCoin>
}
