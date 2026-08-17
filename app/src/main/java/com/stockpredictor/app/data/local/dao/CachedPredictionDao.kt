package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.CachedPredictionTable
import com.stockpredictor.app.data.local.entity.CachedPredictionEntity
import com.stockpredictor.app.model.PredictionDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full CRUD against the `cached_predictions` table. Never call raw SQL from outside this class. */
class CachedPredictionDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    suspend fun getAll(): List<CachedPredictionEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(CachedPredictionTable.NAME, null, null, null, null, null, null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntity()) }
        }
    }

    suspend fun getById(id: Long): CachedPredictionEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            CachedPredictionTable.NAME, null, "${CachedPredictionTable.COL_ID}=?", arrayOf(id.toString()),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntity() else null }
    }

    suspend fun getBySymbol(symbol: String): CachedPredictionEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            CachedPredictionTable.NAME, null, "${CachedPredictionTable.COL_SYMBOL}=?", arrayOf(symbol),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntity() else null }
    }

    /** Upserts via CONFLICT_REPLACE keyed on symbol — re-caching a symbol's prediction is the normal update path. */
    suspend fun upsert(
        symbol: String,
        confidence: Float,
        direction: PredictionDirection,
        targetPrice: Double?,
        horizon: String,
        generatedAt: Long,
    ): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CachedPredictionTable.COL_SYMBOL, symbol)
            put(CachedPredictionTable.COL_CONFIDENCE, confidence)
            put(CachedPredictionTable.COL_DIRECTION, direction.toStorageString())
            targetPrice?.let { put(CachedPredictionTable.COL_TARGET_PRICE, it) }
                ?: putNull(CachedPredictionTable.COL_TARGET_PRICE)
            put(CachedPredictionTable.COL_HORIZON, horizon)
            put(CachedPredictionTable.COL_GENERATED_AT, generatedAt)
            put(CachedPredictionTable.COL_CACHED_AT, System.currentTimeMillis())
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            CachedPredictionTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    suspend fun delete(symbol: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(CachedPredictionTable.NAME, "${CachedPredictionTable.COL_SYMBOL}=?", arrayOf(symbol))
        Unit
    }

    private fun PredictionDirection.toStorageString(): String = when (this) {
        PredictionDirection.Up -> "UP"
        PredictionDirection.Down -> "DOWN"
        PredictionDirection.Flat -> "FLAT"
    }

    private fun String.toPredictionDirection(): PredictionDirection = when (this) {
        "UP" -> PredictionDirection.Up
        "DOWN" -> PredictionDirection.Down
        else -> PredictionDirection.Flat
    }

    private fun Cursor.toEntity(): CachedPredictionEntity {
        val targetPriceIndex = getColumnIndexOrThrow(CachedPredictionTable.COL_TARGET_PRICE)
        return CachedPredictionEntity(
            id = getLong(getColumnIndexOrThrow(CachedPredictionTable.COL_ID)),
            symbol = getString(getColumnIndexOrThrow(CachedPredictionTable.COL_SYMBOL)),
            confidence = getFloat(getColumnIndexOrThrow(CachedPredictionTable.COL_CONFIDENCE)),
            direction = getString(getColumnIndexOrThrow(CachedPredictionTable.COL_DIRECTION)).toPredictionDirection(),
            targetPrice = if (isNull(targetPriceIndex)) null else getDouble(targetPriceIndex),
            horizon = getString(getColumnIndexOrThrow(CachedPredictionTable.COL_HORIZON)),
            generatedAt = getLong(getColumnIndexOrThrow(CachedPredictionTable.COL_GENERATED_AT)),
            cachedAt = getLong(getColumnIndexOrThrow(CachedPredictionTable.COL_CACHED_AT)),
        )
    }
}
