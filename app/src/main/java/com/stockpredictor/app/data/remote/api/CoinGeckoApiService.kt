package com.stockpredictor.app.data.remote.api

import com.stockpredictor.app.data.remote.api.dto.CoinDetailDto
import com.stockpredictor.app.data.remote.api.dto.CoinMarketDto
import com.stockpredictor.app.data.remote.api.dto.CoinSearchResponseDto
import com.stockpredictor.app.data.remote.api.dto.MarketChartResponseDto
import com.stockpredictor.app.data.remote.api.dto.TrendingResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** CoinGecko REST endpoints actually used by this app — see RetrofitClient for base URL/auth. */
interface CoinGeckoApiService {

    @GET("coins/markets")
    suspend fun getMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("ids") ids: String? = null,
    ): List<CoinMarketDto>

    @GET("coins/{id}")
    suspend fun getCoinDetail(
        @Path("id") id: String,
        @Query("localization") localization: Boolean = false,
        @Query("tickers") tickers: Boolean = false,
        @Query("market_data") marketData: Boolean = true,
        @Query("community_data") communityData: Boolean = false,
        @Query("developer_data") developerData: Boolean = false,
        @Query("sparkline") sparkline: Boolean = false,
    ): CoinDetailDto

    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: String = "30",
    ): MarketChartResponseDto

    /** The only endpoint a raw user-typed query is ever sent to — its results are the only
     *  source of a resolved coin id (never guessed from a symbol). */
    @GET("search")
    suspend fun search(@Query("query") query: String): CoinSearchResponseDto

    @GET("search/trending")
    suspend fun getTrending(): TrendingResponseDto
}
