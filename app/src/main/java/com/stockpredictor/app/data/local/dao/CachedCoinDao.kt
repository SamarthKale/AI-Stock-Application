package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.CachedCoinTable
import com.stockpredictor.app.data.local.entity.CachedCoinEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Full CRUD against the `cached_coins` table — the SQLite half of Phase 4's cache-then-network
 *  quote cache. Never call raw SQL from outside this class. */
class CachedCoinDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    suspend fun getAll(): List<CachedCoinEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(CachedCoinTable.NAME, null, null, null, null, null, null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntity()) }
        }
    }

    suspend fun getByCoinId(coinId: String): CachedCoinEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            CachedCoinTable.NAME, null, "${CachedCoinTable.COL_COIN_ID}=?", arrayOf(coinId),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntity() else null }
    }

    /** Upserts via CONFLICT_REPLACE keyed on coin_id — re-caching a coin's quote is the normal update path. */
    suspend fun upsert(entity: CachedCoinEntity): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CachedCoinTable.COL_COIN_ID, entity.coinId)
            put(CachedCoinTable.COL_SYMBOL, entity.symbol)
            put(CachedCoinTable.COL_NAME, entity.name)
            put(CachedCoinTable.COL_IMAGE_URL, entity.imageUrl)
            put(CachedCoinTable.COL_CURRENT_PRICE, entity.currentPrice)
            put(CachedCoinTable.COL_PRICE_CHANGE_24H, entity.priceChange24h)
            put(CachedCoinTable.COL_PRICE_CHANGE_PERCENTAGE_24H, entity.priceChangePercentage24h)
            entity.marketCap?.let { put(CachedCoinTable.COL_MARKET_CAP, it) } ?: putNull(CachedCoinTable.COL_MARKET_CAP)
            entity.marketCapRank?.let { put(CachedCoinTable.COL_MARKET_CAP_RANK, it) } ?: putNull(CachedCoinTable.COL_MARKET_CAP_RANK)
            entity.totalVolume?.let { put(CachedCoinTable.COL_TOTAL_VOLUME, it) } ?: putNull(CachedCoinTable.COL_TOTAL_VOLUME)
            entity.high24h?.let { put(CachedCoinTable.COL_HIGH_24H, it) } ?: putNull(CachedCoinTable.COL_HIGH_24H)
            entity.low24h?.let { put(CachedCoinTable.COL_LOW_24H, it) } ?: putNull(CachedCoinTable.COL_LOW_24H)
            entity.circulatingSupply?.let { put(CachedCoinTable.COL_CIRCULATING_SUPPLY, it) } ?: putNull(CachedCoinTable.COL_CIRCULATING_SUPPLY)
            entity.totalSupply?.let { put(CachedCoinTable.COL_TOTAL_SUPPLY, it) } ?: putNull(CachedCoinTable.COL_TOTAL_SUPPLY)
            entity.maxSupply?.let { put(CachedCoinTable.COL_MAX_SUPPLY, it) } ?: putNull(CachedCoinTable.COL_MAX_SUPPLY)
            entity.ath?.let { put(CachedCoinTable.COL_ATH, it) } ?: putNull(CachedCoinTable.COL_ATH)
            entity.athChangePercentage?.let { put(CachedCoinTable.COL_ATH_CHANGE_PERCENTAGE, it) } ?: putNull(CachedCoinTable.COL_ATH_CHANGE_PERCENTAGE)
            entity.atl?.let { put(CachedCoinTable.COL_ATL, it) } ?: putNull(CachedCoinTable.COL_ATL)
            entity.atlChangePercentage?.let { put(CachedCoinTable.COL_ATL_CHANGE_PERCENTAGE, it) } ?: putNull(CachedCoinTable.COL_ATL_CHANGE_PERCENTAGE)
            entity.sparkline7d?.let { put(CachedCoinTable.COL_SPARKLINE_JSON, Json.encodeToString(it)) } ?: putNull(CachedCoinTable.COL_SPARKLINE_JSON)
            put(CachedCoinTable.COL_CACHED_AT, entity.cachedAt)
        }
        dbHelper.writableDatabase.insertWithOnConflict(
            CachedCoinTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    suspend fun delete(coinId: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(CachedCoinTable.NAME, "${CachedCoinTable.COL_COIN_ID}=?", arrayOf(coinId))
        Unit
    }

    private fun Cursor.toEntity(): CachedCoinEntity {
        val sparklineIndex = getColumnIndexOrThrow(CachedCoinTable.COL_SPARKLINE_JSON)
        return CachedCoinEntity(
            coinId = getString(getColumnIndexOrThrow(CachedCoinTable.COL_COIN_ID)),
            symbol = getString(getColumnIndexOrThrow(CachedCoinTable.COL_SYMBOL)),
            name = getString(getColumnIndexOrThrow(CachedCoinTable.COL_NAME)),
            imageUrl = getStringOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_IMAGE_URL)),
            currentPrice = getDouble(getColumnIndexOrThrow(CachedCoinTable.COL_CURRENT_PRICE)),
            priceChange24h = getDouble(getColumnIndexOrThrow(CachedCoinTable.COL_PRICE_CHANGE_24H)),
            priceChangePercentage24h = getDouble(getColumnIndexOrThrow(CachedCoinTable.COL_PRICE_CHANGE_PERCENTAGE_24H)),
            marketCap = getLongOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_MARKET_CAP)),
            marketCapRank = getIntOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_MARKET_CAP_RANK)),
            totalVolume = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_TOTAL_VOLUME)),
            high24h = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_HIGH_24H)),
            low24h = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_LOW_24H)),
            circulatingSupply = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_CIRCULATING_SUPPLY)),
            totalSupply = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_TOTAL_SUPPLY)),
            maxSupply = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_MAX_SUPPLY)),
            ath = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_ATH)),
            athChangePercentage = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_ATH_CHANGE_PERCENTAGE)),
            atl = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_ATL)),
            atlChangePercentage = getDoubleOrNull(getColumnIndexOrThrow(CachedCoinTable.COL_ATL_CHANGE_PERCENTAGE)),
            sparkline7d = if (isNull(sparklineIndex)) null else Json.decodeFromString(getString(sparklineIndex)),
            cachedAt = getLong(getColumnIndexOrThrow(CachedCoinTable.COL_CACHED_AT)),
        )
    }

    private fun Cursor.getStringOrNull(index: Int): String? = if (isNull(index)) null else getString(index)
    private fun Cursor.getLongOrNull(index: Int): Long? = if (isNull(index)) null else getLong(index)
    private fun Cursor.getIntOrNull(index: Int): Int? = if (isNull(index)) null else getInt(index)
    private fun Cursor.getDoubleOrNull(index: Int): Double? = if (isNull(index)) null else getDouble(index)
}
