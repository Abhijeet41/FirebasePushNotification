package com.example.fcmpush.notification

import com.example.fcmpush.PushSampleApplication
import com.example.fcmpush.R
import com.example.fcmpush.data.model.NotificationMessage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID
import kotlinx.coroutines.runBlocking

class PushFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = application as PushSampleApplication
        // Firebase invokes this service on its worker. Finish the tiny atomic DataStore write
        // before returning so the process cannot be stopped with a token update still pending.
        runBlocking {
            app.container.notificationRepository.saveRefreshedToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val message = remoteMessage.toNotificationMessage()
        val app = application as PushSampleApplication

        // All incoming data is written through the repository before the callback returns.
        // Compose never owns a second list; this update appears through the DataStore Flow.
        runBlocking {
            app.container.notificationRepository.recordIncomingMessage(message)
        }
        NotificationFactory.showNotification(this, message)
    }

    private fun RemoteMessage.toNotificationMessage(): NotificationMessage {
        val payload = data.toSortedMap()
        return NotificationMessage(
            id = messageId ?: payload["id"] ?: UUID.randomUUID().toString(),
            title = notification?.title ?: payload["title"] ?: getString(R.string.app_name),
            body = notification?.body
                ?: payload["body"]
                ?: payload["message"]
                ?: "A new Firebase message arrived.",
            receivedAtEpochMillis = sentTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            data = payload,
        )
    }
}
