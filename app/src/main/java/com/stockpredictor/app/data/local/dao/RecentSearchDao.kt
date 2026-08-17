package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.RecentSearchTable
import com.stockpredictor.app.data.local.entity.RecentSearchEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_RECENT_LIMIT = 10

/** Full CRUD against the `recent_searches` table. Never call raw SQL from outside this class. */
class RecentSearchDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    /** Cap is applied here in getAll(), not by deleting rows, so history isn't lost if the cap changes later. */
    suspend fun getAll(limit: Int = DEFAULT_RECENT_LIMIT): List<RecentSearchEntity> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            RecentSearchTable.NAME, null, null, null, null, null,
            "${RecentSearchTable.COL_SEARCHED_AT} DESC", limit.toString(),
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toEntity()) }
        }
    }

    suspend fun getById(id: Long): RecentSearchEntity? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            RecentSearchTable.NAME, null, "${RecentSearchTable.COL_ID}=?", arrayOf(id.toString()),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.toEntity() else null }
    }

    suspend fun update(entity: RecentSearchEntity) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(RecentSearchTable.COL_QUERY, entity.query)
            put(RecentSearchTable.COL_SEARCHED_AT, entity.searchedAt)
        }
        dbHelper.writableDatabase.update(
            RecentSearchTable.NAME, values, "${RecentSearchTable.COL_ID}=?", arrayOf(entity.id.toString()),
        )
        Unit
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(RecentSearchTable.NAME, "${RecentSearchTable.COL_ID}=?", arrayOf(id.toString()))
        Unit
    }

    /**
     * Records a search on submit (not per keystroke, per the caller). De-duplicates by
     * re-timestamping an existing case-insensitive match instead of inserting a new row.
     */
    suspend fun recordSearch(query: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext

        val existing = db.query(
            RecentSearchTable.NAME, arrayOf(RecentSearchTable.COL_ID),
            "LOWER(${RecentSearchTable.COL_QUERY})=LOWER(?)", arrayOf(trimmed),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

        val values = ContentValues().apply {
            put(RecentSearchTable.COL_QUERY, trimmed)
            put(RecentSearchTable.COL_SEARCHED_AT, System.currentTimeMillis())
        }
        if (existing != null) {
            db.update(RecentSearchTable.NAME, values, "${RecentSearchTable.COL_ID}=?", arrayOf(existing.toString()))
        } else {
            db.insert(RecentSearchTable.NAME, null, values)
        }
        Unit
    }

    private fun Cursor.toEntity() = RecentSearchEntity(
        id = getLong(getColumnIndexOrThrow(RecentSearchTable.COL_ID)),
        query = getString(getColumnIndexOrThrow(RecentSearchTable.COL_QUERY)),
        searchedAt = getLong(getColumnIndexOrThrow(RecentSearchTable.COL_SEARCHED_AT)),
    )
}
