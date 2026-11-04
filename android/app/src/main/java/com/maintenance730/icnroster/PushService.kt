package com.maintenance730.icnroster

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives the pushes from Apps Script (data messages: title / body / tag / kind). */
class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(m: RemoteMessage) {
        Notifs.show(applicationContext, m.data)
    }

    override fun onNewToken(token: String) {
        // the new token is sent to the server the next time the Alerts screen is opened
    }
}
