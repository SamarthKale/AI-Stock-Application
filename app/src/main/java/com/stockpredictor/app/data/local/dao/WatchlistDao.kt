package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.WatchlistTable
import com.stockpredictor.app.data.local.entity.WatchlistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full CRUD against the `watchlist` table. Never call raw SQL from outside this class. */
class WatchlistDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    suspend fun getAll(): List<WatchlistEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            WatchlistTable.NAME, null, null, null, null, null,
            "${WatchlistTable.COL_SORT_ORDER} ASC",
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntity()) }
        }
    }

    suspend fun getById(id: Long): WatchlistEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            WatchlistTable.NAME, null, "${WatchlistTable.COL_ID}=?", arrayOf(id.toString()),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntity() else null }
    }

    suspend fun getBySymbol(symbol: String): WatchlistEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            WatchlistTable.NAME, null, "${WatchlistTable.COL_SYMBOL}=?", arrayOf(symbol),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntity() else null }
    }

    suspend fun isWatchlisted(symbol: String): Boolean = getBySymbol(symbol) != null

    /**
     * Adds [symbol] at the end of the current order. Uses CONFLICT_IGNORE (not REPLACE)
     * because re-adding an already-watchlisted symbol should be a harmless no-op that
     * leaves its existing sort position and added_at untouched, not a reset.
     */
    suspend fun insert(symbol: String): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WatchlistTable.COL_SYMBOL, symbol)
            put(WatchlistTable.COL_ADDED_AT, System.currentTimeMillis())
            put(WatchlistTable.COL_SORT_ORDER, nextSortOrder(db))
        }
        db.insertWithOnConflict(WatchlistTable.NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    suspend fun update(entity: WatchlistEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(WatchlistTable.COL_SYMBOL, entity.symbol)
            put(WatchlistTable.COL_ADDED_AT, entity.addedAt)
            put(WatchlistTable.COL_SORT_ORDER, entity.sortOrder)
        }
        dbHelper.writableDatabase.update(
            WatchlistTable.NAME, values, "${WatchlistTable.COL_ID}=?", arrayOf(entity.id.toString()),
        )
        Unit
    }

    suspend fun delete(symbol: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(WatchlistTable.NAME, "${WatchlistTable.COL_SYMBOL}=?", arrayOf(symbol))
        Unit
    }

    /** Reorders the whole list atomically so a drag/reorder gesture can't leave sort_order corrupted mid-write. */
    suspend fun updateSortOrders(orderedSymbols: List<String>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            orderedSymbols.forEachIndexed { index, symbol ->
                val values = ContentValues().apply { put(WatchlistTable.COL_SORT_ORDER, index) }
                db.update(WatchlistTable.NAME, values, "${WatchlistTable.COL_SYMBOL}=?", arrayOf(symbol))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun nextSortOrder(db: SQLiteDatabase): Int {
        db.query(
            WatchlistTable.NAME, arrayOf("MAX(${WatchlistTable.COL_SORT_ORDER})"), null, null, null, null, null,
        ).use { cursor ->
            return if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) + 1 else 0
        }
    }

    private fun Cursor.toEntity() = WatchlistEntity(
        id = getLong(getColumnIndexOrThrow(WatchlistTable.COL_ID)),
        symbol = getString(getColumnIndexOrThrow(WatchlistTable.COL_SYMBOL)),
        addedAt = getLong(getColumnIndexOrThrow(WatchlistTable.COL_ADDED_AT)),
        sortOrder = getInt(getColumnIndexOrThrow(WatchlistTable.COL_SORT_ORDER)),
    )
}
