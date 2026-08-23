package com.stockpredictor.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.stockpredictor.app.data.local.DbContract.CachedCoinTable
import com.stockpredictor.app.data.local.DbContract.CachedPredictionTable
import com.stockpredictor.app.data.local.DbContract.CachedPriceHistoryTable
import com.stockpredictor.app.data.local.DbContract.ChatMessageTable
import com.stockpredictor.app.data.local.DbContract.NotificationTable
import com.stockpredictor.app.data.local.DbContract.RecentSearchTable
import com.stockpredictor.app.data.local.DbContract.SettingsTable
import com.stockpredictor.app.data.local.DbContract.WatchlistTable
import com.stockpredictor.app.mock.MockCoins

private const val DB_NAME = "stockpredictor.db"
private const val DB_VERSION = 5 // v5 (Phase 5c): adds notifications for real AI-alert history

/**
 * Raw [SQLiteOpenHelper] — no Room. Reserves an `_id` PRIMARY KEY on every table even
 * where a natural key (symbol/key) looks sufficient, because Phase 2.5's Firestore sync
 * needs a stable local row identity independent of any remote document ID. (Exception:
 * CachedCoinTable, whose natural key — coin_id — already is the row identity for a pure
 * upsert cache, so it skips the separate autoincrement id.)
 */
class AppDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${WatchlistTable.NAME} (
                ${WatchlistTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${WatchlistTable.COL_COIN_ID} TEXT NOT NULL UNIQUE,
                ${WatchlistTable.COL_SYMBOL} TEXT NOT NULL,
                ${WatchlistTable.COL_NAME} TEXT,
                ${WatchlistTable.COL_IMAGE_URL} TEXT,
                ${WatchlistTable.COL_ADDED_AT} INTEGER NOT NULL,
                ${WatchlistTable.COL_SORT_ORDER} INTEGER NOT NULL,
                ${WatchlistTable.COL_UPDATED_AT} INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE ${RecentSearchTable.NAME} (
                ${RecentSearchTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${RecentSearchTable.COL_QUERY} TEXT NOT NULL,
                ${RecentSearchTable.COL_SEARCHED_AT} INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE ${SettingsTable.NAME} (
                ${SettingsTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${SettingsTable.COL_KEY} TEXT NOT NULL UNIQUE,
                ${SettingsTable.COL_VALUE} TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE ${CachedPredictionTable.NAME} (
                ${CachedPredictionTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${CachedPredictionTable.COL_SYMBOL} TEXT NOT NULL UNIQUE,
                ${CachedPredictionTable.COL_CONFIDENCE} REAL NOT NULL,
                ${CachedPredictionTable.COL_DIRECTION} TEXT NOT NULL,
                ${CachedPredictionTable.COL_TARGET_PRICE} REAL,
                ${CachedPredictionTable.COL_HORIZON} TEXT NOT NULL,
                ${CachedPredictionTable.COL_GENERATED_AT} INTEGER NOT NULL,
                ${CachedPredictionTable.COL_CACHED_AT} INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        createCachedCoinsTable(db)
        createCachedPriceHistoryTable(db)
        createChatMessagesTable(db)
        createNotificationsTable(db)
        seedDefaultWatchlist(db)
    }

    private fun createCachedCoinsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${CachedCoinTable.NAME} (
                ${CachedCoinTable.COL_COIN_ID} TEXT PRIMARY KEY,
                ${CachedCoinTable.COL_SYMBOL} TEXT NOT NULL,
                ${CachedCoinTable.COL_NAME} TEXT NOT NULL,
                ${CachedCoinTable.COL_IMAGE_URL} TEXT,
                ${CachedCoinTable.COL_CURRENT_PRICE} REAL NOT NULL,
                ${CachedCoinTable.COL_PRICE_CHANGE_24H} REAL NOT NULL,
                ${CachedCoinTable.COL_PRICE_CHANGE_PERCENTAGE_24H} REAL NOT NULL,
                ${CachedCoinTable.COL_MARKET_CAP} INTEGER,
                ${CachedCoinTable.COL_MARKET_CAP_RANK} INTEGER,
                ${CachedCoinTable.COL_TOTAL_VOLUME} REAL,
                ${CachedCoinTable.COL_HIGH_24H} REAL,
                ${CachedCoinTable.COL_LOW_24H} REAL,
                ${CachedCoinTable.COL_CIRCULATING_SUPPLY} REAL,
                ${CachedCoinTable.COL_TOTAL_SUPPLY} REAL,
                ${CachedCoinTable.COL_MAX_SUPPLY} REAL,
                ${CachedCoinTable.COL_ATH} REAL,
                ${CachedCoinTable.COL_ATH_CHANGE_PERCENTAGE} REAL,
                ${CachedCoinTable.COL_ATL} REAL,
                ${CachedCoinTable.COL_ATL_CHANGE_PERCENTAGE} REAL,
                ${CachedCoinTable.COL_SPARKLINE_JSON} TEXT,
                ${CachedCoinTable.COL_CACHED_AT} INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun createCachedPriceHistoryTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${CachedPriceHistoryTable.NAME} (
                ${CachedPriceHistoryTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${CachedPriceHistoryTable.COL_COIN_ID} TEXT NOT NULL,
                ${CachedPriceHistoryTable.COL_VS_CURRENCY} TEXT NOT NULL,
                ${CachedPriceHistoryTable.COL_RANGE_KEY} TEXT NOT NULL,
                ${CachedPriceHistoryTable.COL_POINTS_JSON} TEXT NOT NULL,
                ${CachedPriceHistoryTable.COL_CACHED_AT} INTEGER NOT NULL,
                UNIQUE(${CachedPriceHistoryTable.COL_COIN_ID}, ${CachedPriceHistoryTable.COL_VS_CURRENCY}, ${CachedPriceHistoryTable.COL_RANGE_KEY})
            )
            """.trimIndent(),
        )
    }

    private fun createChatMessagesTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${ChatMessageTable.NAME} (
                ${ChatMessageTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${ChatMessageTable.COL_CONVERSATION_ID} TEXT NOT NULL,
                ${ChatMessageTable.COL_ROLE} TEXT NOT NULL,
                ${ChatMessageTable.COL_CONTENT} TEXT NOT NULL,
                ${ChatMessageTable.COL_TIMESTAMP} INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    private fun createNotificationsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${NotificationTable.NAME} (
                ${NotificationTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${NotificationTable.COL_TITLE} TEXT NOT NULL,
                ${NotificationTable.COL_BODY} TEXT NOT NULL,
                ${NotificationTable.COL_TIMESTAMP} INTEGER NOT NULL,
                ${NotificationTable.COL_IS_READ} INTEGER NOT NULL DEFAULT 0,
                ${NotificationTable.COL_RELATED_COIN_ID} TEXT
            )
            """.trimIndent(),
        )
    }

    /**
     * Fresh installs start with the same first-5-coins watchlist Phase 1's in-memory
     * seed used, so first-run UX is unchanged now that the table is the real source of
     * truth. This only runs once, at table creation — removing every item afterward
     * correctly leaves the table empty, it is never re-seeded.
     */
    private fun seedDefaultWatchlist(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        MockCoins.all.take(5).forEachIndexed { index, coin ->
            val values = ContentValues().apply {
                put(WatchlistTable.COL_COIN_ID, coin.id)
                put(WatchlistTable.COL_SYMBOL, coin.symbol)
                put(WatchlistTable.COL_NAME, coin.name)
                put(WatchlistTable.COL_IMAGE_URL, coin.image)
                put(WatchlistTable.COL_ADDED_AT, now)
                put(WatchlistTable.COL_SORT_ORDER, index)
                put(WatchlistTable.COL_UPDATED_AT, now)
            }
            db.insert(WatchlistTable.NAME, null, values)
        }
    }

    /**
     * v1→v2 (Phase 2.5) used a blanket drop-and-recreate, an acceptable documented tradeoff
     * for this project's pre-launch scope. v2→v3 (the Crypto Predictor migration) is a real
     * in-place migration instead: watchlist gains coin_id/name/image_url via ALTER TABLE (not
     * a drop), since a ticker symbol alone is no longer a safe CoinGecko lookup key. Existing
     * rows are cleared afterward — legacy stock-ticker rows (e.g. "RELIANCE.NS") have no
     * CoinGecko equivalent to migrate to — but recent_searches/settings/cached_predictions are
     * left untouched. Must be replaced with real per-column ALTER TABLE migrations (as done
     * here) rather than any further blanket recreate, now that real user data exists.
     *
     * v3→v4 (Phase 5b) and v4→v5 (Phase 5c) follow the same additive-migration discipline:
     * chat_messages/notifications are each a brand new table, so both are plain CREATE TABLEs
     * with no data loss to any existing table.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE ${WatchlistTable.NAME} ADD COLUMN ${WatchlistTable.COL_COIN_ID} TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE ${WatchlistTable.NAME} ADD COLUMN ${WatchlistTable.COL_NAME} TEXT")
            db.execSQL("ALTER TABLE ${WatchlistTable.NAME} ADD COLUMN ${WatchlistTable.COL_IMAGE_URL} TEXT")
            db.execSQL("DELETE FROM ${WatchlistTable.NAME}")
            createCachedCoinsTable(db)
            createCachedPriceHistoryTable(db)
            seedDefaultWatchlist(db)
        }
        if (oldVersion < 4) {
            createChatMessagesTable(db)
        }
        if (oldVersion < 5) {
            createNotificationsTable(db)
        }
    }

    companion object {
        @Volatile private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context).also { instance = it }
            }
    }
}
