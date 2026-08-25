package com.example.fcmpush

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fcmpush.permission.NotificationPermissionState
import com.example.fcmpush.ui.PushScreen
import com.example.fcmpush.ui.PushUiEvent
import com.example.fcmpush.ui.PushViewModel
import com.example.fcmpush.ui.theme.FirebasePushNotificationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PushViewModel by viewModels {
        val app = application as PushSampleApplication
        PushViewModel.Factory(app.container.notificationRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FirebasePushNotificationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) {
                    viewModel.onPermissionRequestCompleted(this@MainActivity)
                }

                val requestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        viewModel.onPermissionRequestStarted()
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.refreshPermission(this@MainActivity)
                    }
                }

                DisposableEffect(lifecycle) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermission(this@MainActivity)
                        }
                    }
                    lifecycle.addObserver(observer)
                    onDispose { lifecycle.removeObserver(observer) }
                }

                // Android 13+ requires a runtime prompt. The persisted "has requested" flag
                // prevents repeatedly showing it after a denial or process restart.
                LaunchedEffect(uiState.permissionState) {
                    if (uiState.permissionState == NotificationPermissionState.NOT_REQUESTED &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        requestNotificationPermission()
                    }
                }

                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is PushUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                        }
                    }
                }

                PushScreen(
                    state = uiState,
                    snackbarHostState = snackbarHostState,
                    onRequestPermission = requestNotificationPermission,
                    onOpenNotificationSettings = ::openNotificationSettings,
                    onRefreshToken = viewModel::refreshToken,
                    onTokenCopied = viewModel::tokenCopied,
                    onSubscribe = viewModel::subscribe,
                    onUnsubscribe = viewModel::unsubscribe,
                    onClearHistory = viewModel::clearHistory,
                )
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            )
        }
        startActivity(intent)
    }
}
