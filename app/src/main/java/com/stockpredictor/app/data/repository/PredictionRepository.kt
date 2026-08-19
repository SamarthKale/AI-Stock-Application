package com.stockpredictor.app.data.repository

import android.content.Context
import com.stockpredictor.app.data.local.dao.CachedPredictionDao
import com.stockpredictor.app.data.remote.api.BackendApiService
import com.stockpredictor.app.data.remote.api.BackendPricePointDto
import com.stockpredictor.app.data.remote.api.BackendRetrofitClient
import com.stockpredictor.app.data.remote.api.PredictionHistoryRequest
import com.stockpredictor.app.data.remote.api.PredictionResponseDto
import com.stockpredictor.app.model.Prediction
import com.stockpredictor.app.model.PredictionDirection
import com.stockpredictor.app.model.PricePoint
import kotlinx.coroutines.CancellationException

private const val BITCOIN_ID = "bitcoin"

/**
 * Android -> Spring Boot -> FastAPI -> prediction (Phase 5 plan section 8). History for both
 * the target coin and Bitcoin (needed for the model's BTC-relative features — see
 * ai-service/features/feature_engineering.py) comes from [CoinRepository]'s own
 * cache-then-network layer, reusing its TTL/staleness handling rather than duplicating it here.
 * On any failure, falls back to the read-through [CachedPredictionDao] cache (Phase 2) — the
 * exact same table/DAO [com.stockpredictor.app.ui.screens.cryptodetail.CryptoDetailViewModel]
 * already read from for the mock prediction, so this is a data-source swap, not a new pattern.
 */
class PredictionRepository(
    context: Context,
    private val coinRepository: CoinRepository = CoinRepository.getInstance(context),
    private val cachedPredictionDao: CachedPredictionDao = CachedPredictionDao(context.applicationContext),
    private val api: BackendApiService = BackendRetrofitClient.backendApi,
) {
    suspend fun getPrediction(coinId: String): Prediction {
        try {
            val coinHistory = coinRepository.getPriceHistoryForPrediction(coinId).data
            val btcHistory = if (coinId == BITCOIN_ID) coinHistory else coinRepository.getPriceHistoryForPrediction(BITCOIN_ID).data

            val response = api.predict(
                coinId,
                PredictionHistoryRequest(
                    history = coinHistory.map { it.toBackendPoint() },
                    btcHistory = btcHistory.map { it.toBackendPoint() },
                ),
            )
            val prediction = response.toPrediction()
            cachedPredictionDao.upsert(
                symbol = prediction.symbol,
                confidence = prediction.confidence,
                direction = prediction.direction,
                targetPrice = prediction.targetPrice,
                horizon = prediction.horizon,
                generatedAt = prediction.generatedAt,
            )
            return prediction
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = cachedPredictionDao.getBySymbol(coinId)
                ?: throw e
            return Prediction(
                symbol = cached.symbol,
                confidence = cached.confidence,
                direction = cached.direction,
                targetPrice = cached.targetPrice,
                horizon = cached.horizon,
                generatedAt = cached.generatedAt,
            )
        }
    }
}

private fun PricePoint.toBackendPoint() = BackendPricePointDto(timestamp = timestamp, price = price, volume = volume)

private fun PredictionResponseDto.toPrediction() = Prediction(
    symbol = coinId,
    confidence = confidence.toFloat(),
    direction = when (direction) {
        "UP" -> PredictionDirection.Up
        "DOWN" -> PredictionDirection.Down
        else -> PredictionDirection.Flat
    },
    targetPrice = targetPrice,
    horizon = horizon,
    generatedAt = generatedAt,
)
