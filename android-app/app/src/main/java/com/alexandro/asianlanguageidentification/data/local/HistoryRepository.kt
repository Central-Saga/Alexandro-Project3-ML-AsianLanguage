package com.alexandro.asianlanguageidentification.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "detection_history")

class HistoryRepository(private val context: Context) {
    private val gson = Gson()
    private val historyKey = stringPreferencesKey("detection_history_items")

    val historyFlow: Flow<List<DetectionHistoryItem>> = context.historyDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val json = preferences[historyKey] ?: ""
            if (json.isBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<DetectionHistoryItem>>() {}.type
                    gson.fromJson<List<DetectionHistoryItem>>(json, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }

    suspend fun addHistory(item: DetectionHistoryItem) {
        context.historyDataStore.edit { preferences ->
            val currentJson = preferences[historyKey] ?: ""
            val currentList = if (currentJson.isBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<DetectionHistoryItem>>() {}.type
                    gson.fromJson<List<DetectionHistoryItem>>(currentJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            // Batasi maksimal 20 item riwayat terbaru
            val updatedList = (listOf(item) + currentList).take(20)
            preferences[historyKey] = gson.toJson(updatedList)
        }
    }

    suspend fun deleteHistory(id: String) {
        context.historyDataStore.edit { preferences ->
            val currentJson = preferences[historyKey] ?: ""
            if (currentJson.isNotBlank()) {
                try {
                    val type = object : TypeToken<List<DetectionHistoryItem>>() {}.type
                    val currentList: List<DetectionHistoryItem> = gson.fromJson(currentJson, type) ?: emptyList()
                    val updatedList = currentList.filterNot { it.id == id }
                    preferences[historyKey] = gson.toJson(updatedList)
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun clearAllHistory() {
        context.historyDataStore.edit { preferences ->
            preferences.remove(historyKey)
        }
    }
}
