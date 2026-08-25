package com.example.fcmpush.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionResolverTest {
    private val resolver = NotificationPermissionResolver()

    @Test
    fun `pre Android 13 uses system notification setting`() {
        assertEquals(
            NotificationPermissionState.GRANTED,
            resolve(api = 32, notificationsEnabled = true),
        )
        assertEquals(
            NotificationPermissionState.BLOCKED,
            resolve(api = 32, notificationsEnabled = false),
        )
    }

    @Test
    fun `Android 13 granted permission still respects global setting`() {
        assertEquals(
            NotificationPermissionState.GRANTED,
            resolve(api = 33, permissionGranted = true, notificationsEnabled = true),
        )
        assertEquals(
            NotificationPermissionState.BLOCKED,
            resolve(api = 33, permissionGranted = true, notificationsEnabled = false),
        )
    }

    @Test
    fun `first Android 13 denial state requests permission`() {
        assertEquals(
            NotificationPermissionState.NOT_REQUESTED,
            resolve(api = 33, permissionGranted = false, hasRequested = false),
        )
    }

    @Test
    fun `temporary denial shows rationale`() {
        assertEquals(
            NotificationPermissionState.RATIONALE_REQUIRED,
            resolve(
                api = 33,
                permissionGranted = false,
                hasRequested = true,
                rationale = true,
            ),
        )
    }

    @Test
    fun `permanent denial opens settings`() {
        assertEquals(
            NotificationPermissionState.BLOCKED,
            resolve(
                api = 33,
                permissionGranted = false,
                hasRequested = true,
                rationale = false,
            ),
        )
    }

    private fun resolve(
        api: Int,
        permissionGranted: Boolean = false,
        notificationsEnabled: Boolean = true,
        rationale: Boolean = false,
        hasRequested: Boolean = false,
    ) = resolver.resolveFromFacts(
        apiLevel = api,
        permissionGranted = permissionGranted,
        notificationsEnabled = notificationsEnabled,
        shouldShowRationale = rationale,
        hasRequestedPermission = hasRequested,
    )
}
