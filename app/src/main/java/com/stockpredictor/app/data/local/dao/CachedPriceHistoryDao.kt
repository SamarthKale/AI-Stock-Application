package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.CachedPriceHistoryTable
import com.stockpredictor.app.data.local.entity.CachedPriceHistoryEntity
import com.stockpredictor.app.model.PricePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Full CRUD against the `cached_price_history` table — one row per (coin, vs_currency, range)
 *  combination, upserted by CONFLICT_REPLACE. Never call raw SQL from outside this class. */
class CachedPriceHistoryDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    suspend fun get(coinId: String, vsCurrency: String, rangeKey: String): CachedPriceHistoryEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            CachedPriceHistoryTable.NAME, null,
            "${CachedPriceHistoryTable.COL_COIN_ID}=? AND ${CachedPriceHistoryTable.COL_VS_CURRENCY}=? AND ${CachedPriceHistoryTable.COL_RANGE_KEY}=?",
            arrayOf(coinId, vsCurrency, rangeKey),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntity() else null }
    }

    suspend fun upsert(coinId: String, vsCurrency: String, rangeKey: String, points: List<PricePoint>): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CachedPriceHistoryTable.COL_COIN_ID, coinId)
            put(CachedPriceHistoryTable.COL_VS_CURRENCY, vsCurrency)
            put(CachedPriceHistoryTable.COL_RANGE_KEY, rangeKey)
            put(CachedPriceHistoryTable.COL_POINTS_JSON, Json.encodeToString(points))
            put(CachedPriceHistoryTable.COL_CACHED_AT, System.currentTimeMillis())
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            CachedPriceHistoryTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    suspend fun deleteForCoin(coinId: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(CachedPriceHistoryTable.NAME, "${CachedPriceHistoryTable.COL_COIN_ID}=?", arrayOf(coinId))
        Unit
    }

    private fun Cursor.toEntity() = CachedPriceHistoryEntity(
        id = getLong(getColumnIndexOrThrow(CachedPriceHistoryTable.COL_ID)),
        coinId = getString(getColumnIndexOrThrow(CachedPriceHistoryTable.COL_COIN_ID)),
        vsCurrency = getString(getColumnIndexOrThrow(CachedPriceHistoryTable.COL_VS_CURRENCY)),
        rangeKey = getString(getColumnIndexOrThrow(CachedPriceHistoryTable.COL_RANGE_KEY)),
        points = Json.decodeFromString(getString(getColumnIndexOrThrow(CachedPriceHistoryTable.COL_POINTS_JSON))),
        cachedAt = getLong(getColumnIndexOrThrow(CachedPriceHistoryTable.COL_CACHED_AT)),
    )
}
