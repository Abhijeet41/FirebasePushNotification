package com.example.fcmpush.data.model

import kotlinx.serialization.Serializable

/**
 * The durable state for this sample. DataStore owns one serialized instance of this model,
 * making updates atomic and avoiding competing caches of tokens, topics, or message history.
 */
@Serializable
data class NotificationSnapshot(
    val fcmToken: String = "",
    val subscribedTopic: String = "",
    val messages: List<NotificationMessage> = emptyList(),
    val hasRequestedNotificationPermission: Boolean = false,
)
