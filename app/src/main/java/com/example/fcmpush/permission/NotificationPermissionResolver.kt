package com.example.fcmpush.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class NotificationPermissionState {
    CHECKING,
    NOT_REQUESTED,
    REQUESTING,
    RATIONALE_REQUIRED,
    GRANTED,
    BLOCKED,
}

class NotificationPermissionResolver {
    fun resolve(
        activity: Activity,
        hasRequestedPermission: Boolean,
    ): NotificationPermissionState {
        val apiLevel = Build.VERSION.SDK_INT
        val permissionGranted = apiLevel < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val shouldShowRationale = apiLevel >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )

        return resolveFromFacts(
            apiLevel = apiLevel,
            permissionGranted = permissionGranted,
            notificationsEnabled = NotificationManagerCompat.from(activity)
                .areNotificationsEnabled(),
            shouldShowRationale = shouldShowRationale,
            hasRequestedPermission = hasRequestedPermission,
        )
    }

    internal fun resolveFromFacts(
        apiLevel: Int,
        permissionGranted: Boolean,
        notificationsEnabled: Boolean,
        shouldShowRationale: Boolean,
        hasRequestedPermission: Boolean,
    ): NotificationPermissionState {
        if (apiLevel < Build.VERSION_CODES.TIRAMISU) {
            return if (notificationsEnabled) {
                NotificationPermissionState.GRANTED
            } else {
                NotificationPermissionState.BLOCKED
            }
        }

        if (permissionGranted) {
            return if (notificationsEnabled) {
                NotificationPermissionState.GRANTED
            } else {
                NotificationPermissionState.BLOCKED
            }
        }

        return when {
            shouldShowRationale -> NotificationPermissionState.RATIONALE_REQUIRED
            hasRequestedPermission -> NotificationPermissionState.BLOCKED
            else -> NotificationPermissionState.NOT_REQUESTED
        }
    }
}
