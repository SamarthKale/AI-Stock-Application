package com.stockpredictor.app.data.remote.api

import com.stockpredictor.app.data.remote.api.mapper.toCoin
import com.stockpredictor.app.data.remote.api.mapper.toCoinSearchResult
import com.stockpredictor.app.data.remote.api.mapper.toPricePoints
import com.stockpredictor.app.data.remote.api.mapper.toTrendingCoin
import com.stockpredictor.app.model.Coin
import com.stockpredictor.app.model.CoinSearchResult
import com.stockpredictor.app.model.PricePoint
import com.stockpredictor.app.model.TrendingCoin

class CoinGeckoDirectDataSource(
    private val api: CoinGeckoApiService = RetrofitClient.coinGeckoApi,
) : CoinDataSource {

    override suspend fun getMarkets(ids: List<String>?, perPage: Int): List<Coin> =
        api.getMarkets(ids = ids?.joinToString(","), perPage = perPage).map { it.toCoin() }

    override suspend fun getCoinDetail(coinId: String): Coin =
        api.getCoinDetail(id = coinId).toCoin()

    override suspend fun getMarketChart(coinId: String, days: String): List<PricePoint> =
        api.getMarketChart(id = coinId, days = days).toPricePoints()

    override suspend fun search(query: String): List<CoinSearchResult> =
        api.search(query).coins.map { it.toCoinSearchResult() }

    override suspend fun getTrending(): List<TrendingCoin> =
        api.getTrending().coins.map { it.item.toTrendingCoin() }
}
