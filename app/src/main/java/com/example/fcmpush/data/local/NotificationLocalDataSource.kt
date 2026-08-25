package com.example.fcmpush.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.fcmpush.data.model.NotificationMessage
import com.example.fcmpush.data.model.NotificationSnapshot
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.notificationDataStore by preferencesDataStore(name = "notification_state")

class NotificationLocalDataSource(context: Context) {
    private val dataStore = context.applicationContext.notificationDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val snapshot: Flow<NotificationSnapshot> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map(::decode)

    suspend fun saveToken(token: String) = update { it.copy(fcmToken = token) }

    suspend fun setSubscribedTopic(topic: String) = update {
        it.copy(subscribedTopic = topic)
    }

    suspend fun markNotificationPermissionRequested() = update {
        it.copy(hasRequestedNotificationPermission = true)
    }

    suspend fun addMessage(message: NotificationMessage) = update { current ->
        current.copy(
            messages = buildList {
                add(message)
                addAll(current.messages.filterNot { it.id == message.id }.take(MAX_MESSAGES - 1))
            },
        )
    }

    suspend fun clearMessages() = update { it.copy(messages = emptyList()) }

    private suspend fun update(transform: (NotificationSnapshot) -> NotificationSnapshot) {
        dataStore.edit { preferences ->
            val current = decode(preferences)
            preferences[SNAPSHOT_KEY] = json.encodeToString(transform(current))
        }
    }

    private fun decode(preferences: Preferences): NotificationSnapshot {
        val encoded = preferences[SNAPSHOT_KEY] ?: return NotificationSnapshot()
        return runCatching { json.decodeFromString<NotificationSnapshot>(encoded) }
            .getOrElse { NotificationSnapshot() }
    }

    private companion object {
        val SNAPSHOT_KEY = stringPreferencesKey("notification_snapshot")
        const val MAX_MESSAGES = 50
    }
}
