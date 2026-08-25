package com.example.fcmpush

import android.app.Application
import com.example.fcmpush.notification.NotificationFactory

class PushSampleApplication : Application() {
    val container by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationFactory.createNotificationChannel(this)
    }
}
