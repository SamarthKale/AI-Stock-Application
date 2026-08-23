package com.stockpredictor.app.data.local

/**
 * Single source of truth for table/column name constants — no raw string literals
 * for schema elements appear anywhere outside this file. Nested per-table objects
 * mirror the DAO-per-table split and keep autocomplete useful as the schema grows.
 */
object DbContract {

    object WatchlistTable {
        const val NAME = "watchlist"
        const val COL_ID = "_id"
        // CoinGecko coin id (e.g. "bitcoin") — the safe lookup key; a ticker symbol alone can
        // be shared by multiple coins, so it's kept only for display (Migration to Crypto Predictor).
        const val COL_COIN_ID = "coin_id"
        const val COL_SYMBOL = "symbol"
        const val COL_NAME = "name"
        const val COL_IMAGE_URL = "image_url"
        const val COL_ADDED_AT = "added_at"
        const val COL_SORT_ORDER = "sort_order"
        // Added in Phase 2.5: compared against Firestore's server timestamp for
        // last-write-wins conflict resolution — see FirestoreSyncRepository.
        const val COL_UPDATED_AT = "updated_at"
    }

    object RecentSearchTable {
        const val NAME = "recent_searches"
        const val COL_ID = "_id"
        const val COL_QUERY = "query"
        const val COL_SEARCHED_AT = "searched_at"
    }

    object SettingsTable {
        const val NAME = "settings"
        const val COL_ID = "_id"
        const val COL_KEY = "key"
        const val COL_VALUE = "value"
    }

    object CachedPredictionTable {
        const val NAME = "cached_predictions"
        const val COL_ID = "_id"
        const val COL_SYMBOL = "symbol"
        const val COL_CONFIDENCE = "confidence"
        const val COL_DIRECTION = "direction"
        const val COL_TARGET_PRICE = "target_price"
        const val COL_HORIZON = "horizon"
        const val COL_GENERATED_AT = "generated_at"
        const val COL_CACHED_AT = "cached_at"
    }

    /** Cache-then-network quote data for a single coin — one row per coin, upserted by
     *  CONFLICT_REPLACE keyed on coin_id, no separate autoincrement row identity needed. */
    object CachedCoinTable {
        const val NAME = "cached_coins"
        const val COL_COIN_ID = "coin_id"
        const val COL_SYMBOL = "symbol"
        const val COL_NAME = "name"
        const val COL_IMAGE_URL = "image_url"
        const val COL_CURRENT_PRICE = "current_price"
        const val COL_PRICE_CHANGE_24H = "price_change_24h"
        const val COL_PRICE_CHANGE_PERCENTAGE_24H = "price_change_percentage_24h"
        const val COL_MARKET_CAP = "market_cap"
        const val COL_MARKET_CAP_RANK = "market_cap_rank"
        const val COL_TOTAL_VOLUME = "total_volume"
        const val COL_HIGH_24H = "high_24h"
        const val COL_LOW_24H = "low_24h"
        const val COL_CIRCULATING_SUPPLY = "circulating_supply"
        const val COL_TOTAL_SUPPLY = "total_supply"
        const val COL_MAX_SUPPLY = "max_supply"
        const val COL_ATH = "ath"
        const val COL_ATH_CHANGE_PERCENTAGE = "ath_change_percentage"
        const val COL_ATL = "atl"
        const val COL_ATL_CHANGE_PERCENTAGE = "atl_change_percentage"
        const val COL_SPARKLINE_JSON = "sparkline_json"
        const val COL_CACHED_AT = "cached_at"
    }

    /** Cached /market_chart points per (coin, vs_currency, range) — a coin can have several
     *  cached ranges at once (e.g. both "7d" and "30d"), so this keeps a composite natural key
     *  behind a normal autoincrement row id, unlike CachedCoinTable's single-row-per-coin shape. */
    object CachedPriceHistoryTable {
        const val NAME = "cached_price_history"
        const val COL_ID = "_id"
        const val COL_COIN_ID = "coin_id"
        const val COL_VS_CURRENCY = "vs_currency"
        const val COL_RANGE_KEY = "range_key"
        const val COL_POINTS_JSON = "points_json"
        const val COL_CACHED_AT = "cached_at"
    }

    /** Phase 5b — local chat history for the "Ask AI" assistant. The backend chatbot proxy is
     *  stateless; this table is the only place a conversation is persisted. */
    object ChatMessageTable {
        const val NAME = "chat_messages"
        const val COL_ID = "_id"
        const val COL_CONVERSATION_ID = "conversation_id"
        const val COL_ROLE = "role"
        const val COL_CONTENT = "content"
        const val COL_TIMESTAMP = "timestamp"
    }

    /** Phase 5c — real alert-push history, replacing mock/MockNotifications.kt. The backend
     *  AlertRuleService is stateless (per Phase 5c's design); this table is the only place a
     *  received alert is persisted, populated by StockPredictorFcmService.onMessageReceived. */
    object NotificationTable {
        const val NAME = "notifications"
        const val COL_ID = "_id"
        const val COL_TITLE = "title"
        const val COL_BODY = "body"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_IS_READ = "is_read"
        const val COL_RELATED_COIN_ID = "related_coin_id"
    }
}
