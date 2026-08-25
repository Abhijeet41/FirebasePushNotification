package com.example.fcmpush.data

class FirebaseNotConfiguredException : IllegalStateException(
    "Firebase is not configured. Add app/google-services.json, then rebuild the app.",
)
