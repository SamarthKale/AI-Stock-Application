package com.stockpredictor.app.data.repository

import com.stockpredictor.app.data.remote.api.CoinDataSource
import com.stockpredictor.app.data.remote.api.CoinGeckoDirectDataSource
import com.stockpredictor.app.model.CoinSearchResult
import kotlinx.coroutines.CancellationException

/**
 * The only place a coin id is ever resolved from user-entered text (GET /search) — never
 * guessed from a symbol. Deliberately uncached: search results are query-specific and
 * ephemeral, unlike the market data CoinRepository serves cache-then-network.
 */
class CoinSearchRepository(
    private val dataSource: CoinDataSource = CoinGeckoDirectDataSource(),
) {
    suspend fun search(query: String): List<CoinSearchResult> =
        try {
            dataSource.search(query)
        } catch (e: CancellationException) {
            throw e // SearchViewModel's collectLatest cancels superseded searches — let it
        } catch (e: Exception) {
            throw e.toCoinDataException()
        }
}
