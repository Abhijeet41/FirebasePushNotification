package com.example.fcmpush.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationMessage(
    val id: String,
    val title: String,
    val body: String,
    val receivedAtEpochMillis: Long,
    val data: Map<String, String> = emptyMap(),
)
