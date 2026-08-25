package com.example.fcmpush.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fcmpush.data.model.NotificationMessage
import com.example.fcmpush.permission.NotificationPermissionState
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushScreen(
    state: PushUiState,
    snackbarHostState: SnackbarHostState,
    onRequestPermission: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRefreshToken: () -> Unit,
    onTokenCopied: () -> Unit,
    onSubscribe: (String) -> Unit,
    onUnsubscribe: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val background = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FCM Push Lab",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Firebase Cloud Messaging",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        Surface(
                            modifier = Modifier.padding(start = 12.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp).size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    },
                    actions = {
                        ConfigurationBadge(
                            configured = state.isFirebaseConfigured,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                )
            },
        ) { contentPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!state.isFirebaseConfigured) {
                    item { FirebaseSetupCard() }
                }

                item {
                    PermissionCard(
                        permissionState = state.permissionState,
                        onRequestPermission = onRequestPermission,
                        onOpenSettings = onOpenNotificationSettings,
                    )
                }

                item {
                    TokenCard(
                        token = state.fcmToken,
                        configured = state.isFirebaseConfigured,
                        refreshing = state.activeAction == PushAction.REFRESH_TOKEN,
                        enabled = !state.isBusy,
                        onRefresh = onRefreshToken,
                        onCopied = onTokenCopied,
                    )
                }

                item {
                    TopicCard(
                        subscribedTopic = state.subscribedTopic,
                        configured = state.isFirebaseConfigured,
                        activeAction = state.activeAction,
                        onSubscribe = onSubscribe,
                        onUnsubscribe = onUnsubscribe,
                    )
                }

                item {
                    SectionHeader(
                        title = "Message history",
                        subtitle = "Latest ${state.messages.size} of 50 messages",
                        action = if (state.messages.isNotEmpty()) "Clear" else null,
                        onAction = onClearHistory,
                        actionEnabled = !state.isBusy,
                    )
                }

                if (state.messages.isEmpty()) {
                    item { EmptyHistoryCard() }
                } else {
                    items(state.messages, key = NotificationMessage::id) { message ->
                        MessageCard(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigurationBadge(configured: Boolean, modifier: Modifier = Modifier) {
    val container = if (configured) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (configured) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(modifier = modifier, shape = CircleShape, color = container) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (configured) {
                    Icons.Outlined.CheckCircleOutline
                } else {
                    Icons.Outlined.CloudOff
                },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = content,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (configured) "READY" else "SETUP",
                color = content,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FirebaseSetupCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Connect Firebase to go live",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Register com.example.fcmpush in Firebase, copy google-services.json into app/, then rebuild. The project remains buildable without credentials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    permissionState: NotificationPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val granted = permissionState == NotificationPermissionState.GRANTED
    val title = when (permissionState) {
        NotificationPermissionState.CHECKING -> "Checking notification access"
        NotificationPermissionState.NOT_REQUESTED -> "Notification access needed"
        NotificationPermissionState.REQUESTING -> "Waiting for your choice"
        NotificationPermissionState.RATIONALE_REQUIRED -> "Turn on push notifications"
        NotificationPermissionState.GRANTED -> "Notifications are ready"
        NotificationPermissionState.BLOCKED -> "Notifications are blocked"
    }
    val description = when (permissionState) {
        NotificationPermissionState.CHECKING -> "Reading the current system setting…"
        NotificationPermissionState.NOT_REQUESTED -> "Allow access so incoming FCM messages can appear immediately."
        NotificationPermissionState.REQUESTING -> "Choose Allow in the system permission dialog."
        NotificationPermissionState.RATIONALE_REQUIRED -> "Messages are saved in history either way, but permission is required to show alerts."
        NotificationPermissionState.GRANTED -> "Foreground and data messages can be displayed as high-priority alerts."
        NotificationPermissionState.BLOCKED -> "Enable notifications from Android settings to receive visible alerts."
    }

    Card(
        modifier = Modifier.animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (granted) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            ) {
                Icon(
                    imageVector = when {
                        granted -> Icons.Outlined.NotificationsActive
                        permissionState == NotificationPermissionState.BLOCKED -> Icons.Outlined.NotificationsNone
                        else -> Icons.Outlined.WarningAmber
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp).size(22.dp),
                    tint = if (granted) {
                        MaterialTheme.colorScheme.onTertiary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (permissionState) {
                NotificationPermissionState.NOT_REQUESTED,
                NotificationPermissionState.RATIONALE_REQUIRED -> {
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = onRequestPermission) { Text("Allow") }
                }
                NotificationPermissionState.BLOCKED -> {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Open notification settings")
                    }
                }
                NotificationPermissionState.REQUESTING -> {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun TokenCard(
    token: String,
    configured: Boolean,
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    onCopied: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("This device", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "FCM registration token",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = configured && enabled,
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh FCM token")
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            token.isNotBlank() -> token
                            !configured -> "Token available after Firebase setup"
                            else -> "Requesting a token…"
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        enabled = token.isNotBlank(),
                        onClick = {
                            clipboard.setText(AnnotatedString(token))
                            onCopied()
                        },
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy FCM token")
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicCard(
    subscribedTopic: String,
    configured: Boolean,
    activeAction: PushAction?,
    onSubscribe: (String) -> Unit,
    onUnsubscribe: () -> Unit,
) {
    var topicDraft by rememberSaveable { mutableStateOf("") }
    val busy = activeAction != null

    LaunchedEffect(subscribedTopic) {
        if (subscribedTopic.isNotBlank()) topicDraft = subscribedTopic
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Topic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Topic subscription", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (subscribedTopic.isBlank()) {
                            "No active topic"
                        } else {
                            "Listening on /topics/$subscribedTopic"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (subscribedTopic.isNotBlank()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "ACTIVE",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = topicDraft,
                onValueChange = { topicDraft = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = configured && !busy,
                singleLine = true,
                label = { Text("Topic name") },
                placeholder = { Text("e.g. product-updates") },
                prefix = { Text("/topics/") },
                supportingText = { Text("Letters, numbers, and - _ . ~ %") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (topicDraft.isNotBlank() && configured && !busy) onSubscribe(topicDraft)
                    },
                ),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onSubscribe(topicDraft) },
                    enabled = configured && topicDraft.isNotBlank() && !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (activeAction == PushAction.SUBSCRIBE) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (subscribedTopic.isBlank()) "Subscribe" else "Update topic")
                }
                OutlinedButton(
                    onClick = onUnsubscribe,
                    enabled = configured && subscribedTopic.isNotBlank() && !busy,
                ) {
                    Icon(Icons.Outlined.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Leave")
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    action: String?,
    onAction: () -> Unit,
    actionEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (action != null) {
            TextButton(onClick = onAction, enabled = actionEnabled) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(action)
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                modifier = Modifier.size(38.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(10.dp))
            Text("No messages yet", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Send an FCM data message to this token or subscribe to a topic.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageCard(message: NotificationMessage) {
    val timestamp = remember(message.receivedAtEpochMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(message.receivedAtEpochMillis))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(message.body, style = MaterialTheme.typography.bodyMedium)

            if (message.data.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "DATA PAYLOAD",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(5.dp))
                message.data.entries.forEach { (key, value) ->
                    Text(
                        text = "$key = $value",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
