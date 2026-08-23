package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.NotificationTable
import com.stockpredictor.app.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full CRUD against the `notifications` table. Never call raw SQL from outside this class.
 *  Returns [NotificationItem] directly (Phase 1's model) rather than a separate entity type --
 *  it's already the exact shape this table stores, so a redundant parallel type would just be a
 *  second translation layer to maintain for no benefit. */
class NotificationDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    /** Most recent first. */
    suspend fun getAll(): List<NotificationItem> = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            NotificationTable.NAME, null, null, null, null, null, "${NotificationTable.COL_TIMESTAMP} DESC",
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toItem()) }
        }
    }

    suspend fun insert(title: String, body: String, timestamp: Long, relatedCoinId: String?): Long =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(NotificationTable.COL_TITLE, title)
                put(NotificationTable.COL_BODY, body)
                put(NotificationTable.COL_TIMESTAMP, timestamp)
                put(NotificationTable.COL_IS_READ, 0)
                put(NotificationTable.COL_RELATED_COIN_ID, relatedCoinId)
            }
            dbHelper.writableDatabase.insert(NotificationTable.NAME, null, values)
        }

    suspend fun markRead(id: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put(NotificationTable.COL_IS_READ, 1) }
        dbHelper.writableDatabase.update(NotificationTable.NAME, values, "${NotificationTable.COL_ID}=?", arrayOf(id.toString()))
        Unit
    }

    private fun Cursor.toItem(): NotificationItem {
        val relatedCoinIdIndex = getColumnIndexOrThrow(NotificationTable.COL_RELATED_COIN_ID)
        return NotificationItem(
            id = getLong(getColumnIndexOrThrow(NotificationTable.COL_ID)),
            title = getString(getColumnIndexOrThrow(NotificationTable.COL_TITLE)),
            body = getString(getColumnIndexOrThrow(NotificationTable.COL_BODY)),
            timestamp = getLong(getColumnIndexOrThrow(NotificationTable.COL_TIMESTAMP)),
            isRead = getInt(getColumnIndexOrThrow(NotificationTable.COL_IS_READ)) != 0,
            relatedSymbol = if (isNull(relatedCoinIdIndex)) null else getString(relatedCoinIdIndex),
        )
    }
}
