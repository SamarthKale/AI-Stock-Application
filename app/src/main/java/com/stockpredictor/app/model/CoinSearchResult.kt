package com.stockpredictor.app.model

/** Result row from CoinGecko's /search — the only place a coin id is ever resolved from
 *  user-entered text. Never guess an id from a symbol; always go through a search result. */
data class CoinSearchResult(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String?,
    val marketCapRank: Int?
)
