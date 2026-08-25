package com.example.fcmpush.ui

import com.example.fcmpush.data.model.NotificationMessage
import com.example.fcmpush.permission.NotificationPermissionState

data class PushUiState(
    val isLoading: Boolean = true,
    val isFirebaseConfigured: Boolean = false,
    val fcmToken: String = "",
    val subscribedTopic: String = "",
    val messages: List<NotificationMessage> = emptyList(),
    val permissionState: NotificationPermissionState = NotificationPermissionState.CHECKING,
    val activeAction: PushAction? = null,
) {
    val isBusy: Boolean get() = activeAction != null
}

enum class PushAction {
    REFRESH_TOKEN,
    SUBSCRIBE,
    UNSUBSCRIBE,
    CLEAR_HISTORY,
}

sealed interface PushUiEvent {
    data class ShowMessage(val message: String) : PushUiEvent
}
