package com.example.fcmpush

import android.content.Context
import com.example.fcmpush.data.DefaultNotificationRepository
import com.example.fcmpush.data.NotificationRepository
import com.example.fcmpush.data.local.NotificationLocalDataSource

class AppContainer(context: Context) {
    val notificationRepository: NotificationRepository = DefaultNotificationRepository(
        localDataSource = NotificationLocalDataSource(context),
        isFirebaseConfigured = BuildConfig.FIREBASE_CONFIGURED,
    )
}
