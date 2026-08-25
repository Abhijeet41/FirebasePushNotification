# FCM Push Lab — Jetpack Compose sample

A production-shaped Firebase Cloud Messaging sample built entirely with Jetpack Compose. It requests Android's notification runtime permission, receives FCM messages, displays local notifications, manages one topic subscription, persists token/message state, and keeps the UI driven by a single source of truth.

## What is included

- Android 13+ `POST_NOTIFICATIONS` runtime permission, including rationale, denial, and app-settings flows
- Android 12 and lower notification-setting detection
- Install-time `INTERNET` permission (normal permission; no prompt is required)
- FCM registration token retrieval and token-rotation handling
- Foreground/data-message handling with `FirebaseMessagingService`
- Android 8+ high-importance notification channel
- Immutable `StateFlow`-driven Compose UI
- One active FCM topic with subscribe/update/unsubscribe actions
- Durable, bounded history of the latest 50 data/foreground messages
- Atomic Preferences DataStore persistence
- Light/dark and Material You dynamic-color support
- A safe setup mode: the project still builds when `google-services.json` is absent

FCM's transitive manifest contributes the internal permissions it requires (for example wake-lock and boot handling). The app declares only the direct permissions it uses rather than asking for unrelated dangerous permissions.

## Firebase setup

1. Open the [Firebase console](https://console.firebase.google.com/) and create or select a project.
2. Add an **Android app** with this exact package name:

   ```text
   com.example.fcmpush
   ```

3. Download `google-services.json` and copy it to:

   ```text
   app/google-services.json
   ```

4. In Firebase Console, make sure Cloud Messaging is available for the project.
5. Sync Gradle, then run on a physical device or an emulator image with Google Play services.

`google-services.json` is intentionally ignored by Git. When it is present, `app/build.gradle.kts` applies the Google Services plugin automatically. When absent, the app shows a setup banner and Firebase actions stay disabled.

> To use another application ID, update `namespace` and `applicationId` in `app/build.gradle.kts`, move the Kotlin package if desired, and register that exact ID in Firebase before downloading a new configuration file.

## Build

Requirements:

- Android Studio with JDK 17
- Android SDK 36

```bash
./gradlew test
./gradlew assembleDebug
```

This project uses AGP 8.13.2, Kotlin 2.3.20, the Compose 2026.04.01 BOM, and the Firebase 34.18.0 BOM.

## Send a test message

### Firebase Console

1. Run the app and allow notifications.
2. Copy the token shown on the **This device** card.
3. In Firebase Console, open **Messaging** and create a campaign/test message.
4. Select the registered Android app and use **Send test message** to target the copied token.

### FCM HTTP v1

Never put a service-account key or server credential in this Android app. Send from a trusted backend. With the Google Cloud CLI authenticated on your development machine, a data-message test looks like this:

```bash
PROJECT_ID="your-firebase-project-id"
DEVICE_TOKEN="token-copied-from-the-app"
ACCESS_TOKEN="$(gcloud auth print-access-token)"

curl -X POST \
  "https://fcm.googleapis.com/v1/projects/${PROJECT_ID}/messages:send" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"message\": {
      \"token\": \"${DEVICE_TOKEN}\",
      \"data\": {
        \"title\": \"Hello from FCM\",
        \"body\": \"This message is handled by the sample service.\",
        \"destination\": \"inbox\"
      },
      \"android\": { \"priority\": \"high\" }
    }
  }"
```

To target the topic entered in the app, replace `"token": "..."` with:

```json
"topic": "product-updates"
```

Topic names are entered without `/topics/` in HTTP v1 requests.

## Message behavior

| Payload / app state | Behavior |
|---|---|
| Data payload, foreground or background | `PushFirebaseMessagingService` records it and creates the notification. |
| Notification payload, foreground | The service records and displays it. |
| Notification payload, background | The FCM SDK displays it automatically; Android launches the app when tapped. The service is not called, so it is not added to the custom history. |
| Notification permission denied | The message can still be recorded, but no visible system notification is posted. |

Use data messages (or a data + notification strategy designed for your backend) when custom processing and history are required in every app state. Android delivery timing still depends on priority, battery policy, and FCM quotas.

## Single source of truth

```text
Firebase callback / user action
              │
              ▼
   NotificationRepository       ← only write gateway
              │
              ▼
 Preferences DataStore          ← durable source of truth
              │ Flow<NotificationSnapshot>
              ▼
       PushViewModel
              │ StateFlow<PushUiState>
              ▼
        Compose screen
```

- `NotificationSnapshot` atomically contains the token, the one modeled topic, message history, and whether the runtime prompt has been attempted.
- `NotificationLocalDataSource` is the only persistence implementation.
- `DefaultNotificationRepository` is the only write gateway for FCM callbacks and UI actions.
- `PushViewModel` combines repository state with short-lived operation/permission state into one immutable `PushUiState`.
- Compose collects that state with lifecycle awareness; it does not maintain a second token, subscription, or message list.
- Android itself remains the authority for whether notifications are currently allowed. The resolver re-reads the system state every time the Activity resumes.

The topic text field is intentionally local draft state: unsubmitted user input is UI state, not application data. Once subscribed, the repository writes the accepted topic to DataStore.

## Project map

```text
app/src/main/java/com/example/fcmpush/
├── data/
│   ├── local/NotificationLocalDataSource.kt
│   ├── model/NotificationSnapshot.kt
│   ├── DefaultNotificationRepository.kt
│   └── NotificationRepository.kt
├── notification/
│   ├── NotificationFactory.kt
│   └── PushFirebaseMessagingService.kt
├── permission/NotificationPermissionResolver.kt
├── ui/
│   ├── PushScreen.kt
│   ├── PushUiState.kt
│   ├── PushViewModel.kt
│   └── theme/
├── AppContainer.kt
├── MainActivity.kt
└── PushSampleApplication.kt
```

## Permission testing checklist

- Android 13+: first launch shows the system notification prompt.
- Deny once: the in-app card explains why access is useful and can request again when Android permits it.
- Deny permanently / disable in settings: the card opens the app's notification settings.
- Grant access: the card switches to **Notifications are ready**.
- Disable notifications from Android settings and return: state refreshes immediately on Activity resume.
- Android 8+: confirm the **Push messages** channel exists in system settings.
- Rotate the FCM token (for example by reinstalling/clearing app data): the new token is persisted through `onNewToken`.

## Security notes

- FCM registration tokens identify an app installation. Avoid logging or publishing them.
- `google-services.json` identifies the Firebase client project but is excluded here to prevent accidental cross-project configuration.
- Server credentials and service-account files must remain on a trusted backend and must never be bundled in the APK.
