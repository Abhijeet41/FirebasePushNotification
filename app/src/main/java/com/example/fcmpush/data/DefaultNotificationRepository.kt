package com.example.fcmpush.data

import com.example.fcmpush.data.local.NotificationLocalDataSource
import com.example.fcmpush.data.model.NotificationMessage
import com.example.fcmpush.data.model.NotificationSnapshot
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class DefaultNotificationRepository(
    private val localDataSource: NotificationLocalDataSource,
    override val isFirebaseConfigured: Boolean,
    private val firebaseMessaging: () -> FirebaseMessaging = FirebaseMessaging::getInstance,
) : NotificationRepository {

    override val snapshot: Flow<NotificationSnapshot> = localDataSource.snapshot

    override suspend fun refreshToken(): Result<String> = runCatching {
        ensureFirebaseConfigured()
        firebaseMessaging().token.await().also { token ->
            localDataSource.saveToken(token)
        }
    }

    override suspend fun subscribeToTopic(rawTopic: String): Result<String> = runCatching {
        ensureFirebaseConfigured()
        val topic = TopicRules.normalize(rawTopic)
        val currentTopic = snapshot.first().subscribedTopic

        if (currentTopic == topic) return@runCatching topic

        // The sample deliberately models one active topic. Remove the previous remote
        // subscription before changing the local source of truth.
        if (currentTopic.isNotBlank()) {
            firebaseMessaging().unsubscribeFromTopic(currentTopic).await()
            localDataSource.setSubscribedTopic("")
        }

        firebaseMessaging().subscribeToTopic(topic).await()
        localDataSource.setSubscribedTopic(topic)
        topic
    }

    override suspend fun unsubscribeFromTopic(): Result<Unit> = runCatching {
        ensureFirebaseConfigured()
        val topic = snapshot.first().subscribedTopic
        if (topic.isNotBlank()) {
            firebaseMessaging().unsubscribeFromTopic(topic).await()
            localDataSource.setSubscribedTopic("")
        }
    }

    override suspend fun saveRefreshedToken(token: String) {
        localDataSource.saveToken(token)
    }

    override suspend fun recordIncomingMessage(message: NotificationMessage) {
        localDataSource.addMessage(message)
    }

    override suspend fun markNotificationPermissionRequested() {
        localDataSource.markNotificationPermissionRequested()
    }

    override suspend fun clearMessages() {
        localDataSource.clearMessages()
    }

    private fun ensureFirebaseConfigured() {
        if (!isFirebaseConfigured) throw FirebaseNotConfiguredException()
    }
}
