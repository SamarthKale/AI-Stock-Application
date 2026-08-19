package com.stockpredictor.app.data.repository

import java.io.IOException
import retrofit2.HttpException

/** Thrown only when no cached data exists to fall back on either — see CoinRepository's
 *  cache-then-network + stale-fallback logic for when this actually surfaces. */
class CoinDataUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A specific coin id genuinely doesn't exist on CoinGecko (404), distinct from "we couldn't
 *  reach CoinGecko" — CryptoDetailViewModel maps this to UiState.Empty, not UiState.Error. */
class CoinNotFoundException(coinId: String) : Exception("No data for $coinId")

/** Translates a raw network exception into a domain-level one with a message safe to show
 *  directly in UiState.Error, per the error-code mapping in the Phase 4 migration plan. */
fun Throwable.toCoinDataException(): Exception = when (this) {
    is HttpException -> when (code()) {
        429 -> CoinDataUnavailableException("Market data is busy right now — try again in a moment", this)
        in 500..599 -> CoinDataUnavailableException("Market data temporarily unavailable", this)
        else -> CoinDataUnavailableException("Market data temporarily unavailable", this)
    }
    is IOException -> CoinDataUnavailableException("Check your connection and try again", this)
    else -> CoinDataUnavailableException("Something went wrong. Please try again.", this)
}

/** Used by ViewModels to turn any caught exception into UiState.Error's message. */
fun Throwable.toUserMessage(): String = (this as? CoinDataUnavailableException)?.message
    ?: "Something went wrong. Please try again."
