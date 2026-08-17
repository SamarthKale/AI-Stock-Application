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
        const val COL_SYMBOL = "symbol"
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
}
