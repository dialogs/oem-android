package ru.example.oem_test

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushService : FirebaseMessagingService() {

    private val sdk by lazy(LazyThreadSafetyMode.NONE) { (application as OemTestApplication).sdk }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        sdk.pushConsumer.onMessageReceived(message, this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sdk.pushConsumer.onNewToken(token, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sdk.pushConsumer.onDestroy(this)
    }
}