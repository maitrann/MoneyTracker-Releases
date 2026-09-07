package com.personal.moneytracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private val Context.settingsDataStore by preferencesDataStore("tracker_settings")

class TrackedPackageRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringSetPreferencesKey("tracked_packages")
    val packages: Flow<Set<String>> = context.settingsDataStore.data.map { it[key].orEmpty() }
    suspend fun current(): Set<String> = packages.first()
    suspend fun add(packageName: String) {
        val normalized = packageName.trim()
        if (normalized.isNotEmpty()) context.settingsDataStore.edit { it[key] = it[key].orEmpty() + normalized }
    }
    suspend fun remove(packageName: String) {
        context.settingsDataStore.edit { it[key] = it[key].orEmpty() - packageName }
    }
}
