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
     *
     * `updated_at` is stamped with the device clock purely as a placeholder — something has
     * to be there before this row has ever reached Firestore. FirestoreSyncRepository
     * overwrites it with the real server-resolved timestamp the moment the push succeeds, so
     * this device-clock value never survives long enough to be compared against a remote
     * timestamp in a last-write-wins decision.
     */
    suspend fun insert(symbol: String): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WatchlistTable.COL_SYMBOL, symbol)
            put(WatchlistTable.COL_ADDED_AT, System.currentTimeMillis())
            put(WatchlistTable.COL_SORT_ORDER, nextSortOrder(db))
            put(WatchlistTable.COL_UPDATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(WatchlistTable.NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /**
     * Overwrites (or inserts) the local row with a remote-wins value during Firestore merge —
     * unlike [insert], this always replaces, since the caller has already decided the remote
     * copy is newer.
     */
    suspend fun upsertFromRemote(symbol: String, addedAt: Long, sortOrder: Int, updatedAt: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(WatchlistTable.COL_SYMBOL, symbol)
            put(WatchlistTable.COL_ADDED_AT, addedAt)
            put(WatchlistTable.COL_SORT_ORDER, sortOrder)
            put(WatchlistTable.COL_UPDATED_AT, updatedAt)
        }
        dbHelper.writableDatabase.insertWithOnConflict(WatchlistTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }

    suspend fun update(entity: WatchlistEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(WatchlistTable.COL_SYMBOL, entity.symbol)
            put(WatchlistTable.COL_ADDED_AT, entity.addedAt)
            put(WatchlistTable.COL_SORT_ORDER, entity.sortOrder)
            put(WatchlistTable.COL_UPDATED_AT, entity.updatedAt)
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

    /**
     * Wipes the whole table — used on sign-out so a different account signing in on this
     * device never inherits the previous user's cached rows (see FirestoreSyncRepository).
     * Safe: SQLite is only ever an offline cache of Firestore here, so on next sign-in the
     * table repopulates from the new user's own collection via the sync listener.
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(WatchlistTable.NAME, null, null)
        Unit
    }

    /**
     * Reorders the whole list atomically so a drag/reorder gesture can't leave sort_order
     * corrupted mid-write. Like [insert], `updated_at` here is a device-clock placeholder that
     * FirestoreSyncRepository corrects to a server-resolved value once each row's reorder push
     * confirms — see [insert]'s doc for why that matters for skew-proof conflict resolution.
     */
    suspend fun updateSortOrders(orderedSymbols: List<String>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            orderedSymbols.forEachIndexed { index, symbol ->
                val values = ContentValues().apply {
                    put(WatchlistTable.COL_SORT_ORDER, index)
                    put(WatchlistTable.COL_UPDATED_AT, now)
                }
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
        updatedAt = getLong(getColumnIndexOrThrow(WatchlistTable.COL_UPDATED_AT)),
    )
}
