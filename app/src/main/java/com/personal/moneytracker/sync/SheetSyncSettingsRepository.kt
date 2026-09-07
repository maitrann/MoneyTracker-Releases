package com.personal.moneytracker.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sheetSyncDataStore by preferencesDataStore("sheet_sync")

@Singleton
class SheetSyncSettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val spreadsheetId = stringPreferencesKey("spreadsheet_id")
    private val sheetName = stringPreferencesKey("sheet_name")
    val config: Flow<SheetConfig?> = context.sheetSyncDataStore.data.map { p ->
        p[spreadsheetId]?.takeIf { it.isNotBlank() }?.let { SheetConfig(it, p[sheetName].orEmpty().ifBlank { "Transactions" }) }
    }
    suspend fun current(): SheetConfig? = config.first()
    suspend fun save(urlOrId: String, tab: String): Boolean {
        val id = extractSpreadsheetId(urlOrId) ?: return false
        context.sheetSyncDataStore.edit { it[spreadsheetId] = id; it[sheetName] = tab.ifBlank { "Transactions" } }
        return true
    }
    companion object {
        fun extractSpreadsheetId(value: String): String? {
            val trimmed = value.trim()
            val fromUrl = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)").find(trimmed)?.groupValues?.getOrNull(1)
            return fromUrl ?: trimmed.takeIf { it.matches(Regex("[a-zA-Z0-9_-]{20,}")) }
        }
    }
}
