package com.stockpredictor.app.data.local.dao

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.stockpredictor.app.data.local.AppDatabaseHelper
import com.stockpredictor.app.data.local.DbContract.SettingsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generic key-value table. Values are stored as text; typed get/set wrappers live here
 * so callers never parse strings themselves.
 */
class SettingsDao(context: Context) {
    private val dbHelper = AppDatabaseHelper.getInstance(context)

    suspend fun getString(key: String, default: String? = null): String? = withContext(Dispatchers.IO) {
        dbHelper.readableDatabase.query(
            SettingsTable.NAME, arrayOf(SettingsTable.COL_VALUE), "${SettingsTable.COL_KEY}=?", arrayOf(key),
            null, null, null,
        ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else default }
    }

    /** Upserts via CONFLICT_REPLACE keyed on [key] — re-setting an existing key is the normal update path here. */
    suspend fun setString(key: String, value: String) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(SettingsTable.COL_KEY, key)
            put(SettingsTable.COL_VALUE, value)
        }
        dbHelper.writableDatabase.insertWithOnConflict(SettingsTable.NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }

    suspend fun getBoolean(key: String, default: Boolean): Boolean =
        getString(key)?.toBooleanStrictOrNull() ?: default

    suspend fun setBoolean(key: String, value: Boolean) = setString(key, value.toString())
}
