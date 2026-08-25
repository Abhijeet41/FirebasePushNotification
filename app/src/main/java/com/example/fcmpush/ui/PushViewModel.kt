package com.example.fcmpush.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fcmpush.data.NotificationRepository
import com.example.fcmpush.data.model.NotificationSnapshot
import com.example.fcmpush.permission.NotificationPermissionResolver
import com.example.fcmpush.permission.NotificationPermissionState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PushViewModel(
    private val repository: NotificationRepository,
    private val permissionResolver: NotificationPermissionResolver,
) : ViewModel() {
    private val snapshot = repository.snapshot
        .map<NotificationSnapshot, NotificationSnapshot?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val permissionState = MutableStateFlow(NotificationPermissionState.CHECKING)
    private val activeAction = MutableStateFlow<PushAction?>(null)
    private val eventChannel = Channel<PushUiEvent>(capacity = Channel.BUFFERED)

    val events = eventChannel.receiveAsFlow()

    val uiState = combine(snapshot, permissionState, activeAction) {
            currentSnapshot,
            currentPermission,
            currentAction ->
        PushUiState(
            isLoading = currentSnapshot == null,
            isFirebaseConfigured = repository.isFirebaseConfigured,
            fcmToken = currentSnapshot?.fcmToken.orEmpty(),
            subscribedTopic = currentSnapshot?.subscribedTopic.orEmpty(),
            messages = currentSnapshot?.messages.orEmpty(),
            permissionState = currentPermission,
            activeAction = currentAction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PushUiState(isFirebaseConfigured = repository.isFirebaseConfigured),
    )

    init {
        if (repository.isFirebaseConfigured) refreshToken(showSuccess = false)
    }

    fun refreshPermission(activity: Activity) {
        viewModelScope.launch {
            val current = snapshot.filterNotNull().first()
            permissionState.value = permissionResolver.resolve(
                activity = activity,
                hasRequestedPermission = current.hasRequestedNotificationPermission,
            )
        }
    }

    fun onPermissionRequestStarted() {
        permissionState.value = NotificationPermissionState.REQUESTING
        viewModelScope.launch { repository.markNotificationPermissionRequested() }
    }

    fun onPermissionRequestCompleted(activity: Activity) {
        viewModelScope.launch {
            repository.markNotificationPermissionRequested()
            permissionState.value = permissionResolver.resolve(
                activity = activity,
                hasRequestedPermission = true,
            )
        }
    }

    fun refreshToken() = refreshToken(showSuccess = true)

    fun subscribe(topic: String) = runAction(PushAction.SUBSCRIBE) {
        repository.subscribeToTopic(topic).fold(
            onSuccess = { subscribed -> notify("Subscribed to $subscribed") },
            onFailure = ::notifyError,
        )
    }

    fun unsubscribe() = runAction(PushAction.UNSUBSCRIBE) {
        repository.unsubscribeFromTopic().fold(
            onSuccess = { notify("Topic subscription removed") },
            onFailure = ::notifyError,
        )
    }

    fun clearHistory() = runAction(PushAction.CLEAR_HISTORY) {
        repository.clearMessages()
        notify("Message history cleared")
    }

    fun tokenCopied() {
        notify("FCM token copied")
    }

    private fun refreshToken(showSuccess: Boolean) = runAction(PushAction.REFRESH_TOKEN) {
        repository.refreshToken().fold(
            onSuccess = { if (showSuccess) notify("FCM token refreshed") },
            onFailure = ::notifyError,
        )
    }

    private fun runAction(action: PushAction, block: suspend () -> Unit) {
        if (activeAction.value != null) return
        viewModelScope.launch {
            activeAction.value = action
            try {
                block()
            } finally {
                activeAction.value = null
            }
        }
    }

    private fun notify(message: String) {
        eventChannel.trySend(PushUiEvent.ShowMessage(message))
    }

    private fun notifyError(error: Throwable) {
        notify(error.localizedMessage ?: "The Firebase operation failed. Please try again.")
    }

    class Factory(
        private val repository: NotificationRepository,
        private val permissionResolver: NotificationPermissionResolver = NotificationPermissionResolver(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PushViewModel::class.java))
            return PushViewModel(repository, permissionResolver) as T
        }
    }
}
