package com.stockpredictor.app.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.stockpredictor.app.data.local.DbContract.CachedPredictionTable
import com.stockpredictor.app.data.local.DbContract.RecentSearchTable
import com.stockpredictor.app.data.local.DbContract.SettingsTable
import com.stockpredictor.app.data.local.DbContract.WatchlistTable
import com.stockpredictor.app.mock.MockStocks

private const val DB_NAME = "stockpredictor.db"
private const val DB_VERSION = 2 // v2 (Phase 2.5): added watchlist.updated_at for Firestore sync

/**
 * Raw [SQLiteOpenHelper] — no Room. Reserves an `_id` PRIMARY KEY on every table even
 * where a natural key (symbol/key) looks sufficient, because Phase 2.5's Firestore sync
 * needs a stable local row identity independent of any remote document ID.
 */
class AppDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${WatchlistTable.NAME} (
                ${WatchlistTable.COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${WatchlistTable.COL_SYMBOL} TEXT NOT NULL UNIQUE,
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
        seedDefaultWatchlist(db)
    }

    /**
     * Fresh installs start with the same first-5-tickers watchlist Phase 1's in-memory
     * seed used, so first-run UX is unchanged now that the table is the real source of
     * truth. This only runs once, at table creation — removing every item afterward
     * correctly leaves the table empty, it is never re-seeded.
     */
    private fun seedDefaultWatchlist(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()
        MockStocks.all.take(5).forEachIndexed { index, stock ->
            val values = ContentValues().apply {
                put(WatchlistTable.COL_SYMBOL, stock.symbol)
                put(WatchlistTable.COL_ADDED_AT, now)
                put(WatchlistTable.COL_SORT_ORDER, index)
                put(WatchlistTable.COL_UPDATED_AT, now)
            }
            db.insert(WatchlistTable.NAME, null, values)
        }
    }

    /**
     * Version-bump-and-recreate: acceptable for this project's pre-launch scope, but this
     * drops all local user data (watchlist, recent searches, settings, cached predictions)
     * on any schema change. Must be replaced with real ALTER TABLE migrations before
     * Phase 6's production hardening if the schema changes again after real users exist.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${WatchlistTable.NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${RecentSearchTable.NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${SettingsTable.NAME}")
        db.execSQL("DROP TABLE IF EXISTS ${CachedPredictionTable.NAME}")
        onCreate(db)
    }

    companion object {
        @Volatile private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context).also { instance = it }
            }
    }
}
