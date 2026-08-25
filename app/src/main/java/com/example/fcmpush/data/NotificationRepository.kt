package com.example.fcmpush.data

import com.example.fcmpush.data.model.NotificationMessage
import com.example.fcmpush.data.model.NotificationSnapshot
import kotlinx.coroutines.flow.Flow

/** The only write gateway for token, subscription, and incoming-message state. */
interface NotificationRepository {
    val snapshot: Flow<NotificationSnapshot>
    val isFirebaseConfigured: Boolean

    suspend fun refreshToken(): Result<String>
    suspend fun subscribeToTopic(rawTopic: String): Result<String>
    suspend fun unsubscribeFromTopic(): Result<Unit>
    suspend fun saveRefreshedToken(token: String)
    suspend fun recordIncomingMessage(message: NotificationMessage)
    suspend fun markNotificationPermissionRequested()
    suspend fun clearMessages()
}
