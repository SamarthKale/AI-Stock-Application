package com.stockpredictor.app.data

/**
 * Static per-exchange location data for the Phase 5c exchange map — re-scoped from the
 * stock-era `ExchangeData` (which had `openLocalTime`/`closeLocalTime` for NYSE/NASDAQ-style
 * trading hours, meaningless for 24/7 crypto markets). No timezone/open-closed fields here at all.
 *
 * [id] is the live-verified CoinGecko exchange id (confirmed via a real `/exchanges` call during
 * planning — never an invented id, matching this project's "never guess a coin id" discipline
 * applied to exchange ids too). [city]/[latitude]/[longitude] are the capital of the exchange's
 * CoinGecko-reported registered country ([country]) — crypto exchanges are online-only businesses
 * with no single physical trading floor (unlike NYSE/NASDAQ), so this is honestly a *registered
 * jurisdiction* marker, not an operational-HQ or trading-venue claim. [tradeVolume24hBtc] and
 * [trustScore] are NOT stored here — they're live, fetched by [com.stockpredictor.app.data.repository.ExchangeRepository]
 * and joined onto this static list at read time, so the map always shows current data.
 */
data class ExchangeLocation(
    val id: String,
    val displayName: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
)

object ExchangeData {
    val all: List<ExchangeLocation> = listOf(
        ExchangeLocation("gdax", "Coinbase Exchange", "San Francisco", "United States", 37.7749, -122.4194),
        ExchangeLocation("binance", "Binance", "George Town", "Cayman Islands", 19.3133, -81.2546),
        ExchangeLocation("kraken", "Kraken", "San Francisco", "United States", 37.7749, -122.4194),
        ExchangeLocation("okex", "OKX", "Victoria", "Seychelles", -4.6796, 55.4920),
        ExchangeLocation("bitget", "Bitget", "Victoria", "Seychelles", -4.6796, 55.4920),
        ExchangeLocation("gate", "Gate", "Panama City", "Panama", 8.9824, -79.5199),
        ExchangeLocation("bitstamp", "Bitstamp by Robinhood", "Luxembourg City", "Luxembourg", 49.6116, 6.1319),
        ExchangeLocation("bybit_spot", "Bybit", "Road Town", "British Virgin Islands", 18.4207, -64.6399),
    )
}
