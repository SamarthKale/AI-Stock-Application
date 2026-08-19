package com.stockpredictor.app.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/** Field names match the backend's Java records exactly (camelCase, no snake_case conversion —
 *  Jackson serializes Java record components as-is, and this Retrofit client's Json instance
 *  uses default kotlinx.serialization naming, not the SnakeCase strategy CoinGecko's client uses). */
@Serializable
data class BackendPricePointDto(val timestamp: Long, val price: Double, val volume: Double)

@Serializable
data class PredictionHistoryRequest(val history: List<BackendPricePointDto>, val btcHistory: List<BackendPricePointDto>)

@Serializable
data class PredictionResponseDto(
    val coinId: String,
    val confidence: Double,
    val direction: String,
    val targetPrice: Double?,
    val horizon: String,
    val generatedAt: Long,
)

interface BackendApiService {
    @POST("api/predictions/{coinId}")
    suspend fun predict(@Path("coinId") coinId: String, @Body request: PredictionHistoryRequest): PredictionResponseDto
}
