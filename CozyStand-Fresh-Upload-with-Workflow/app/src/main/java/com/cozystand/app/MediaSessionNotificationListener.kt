package com.cozystand.app

import android.service.notification.NotificationListenerService

/** Android requires the user to explicitly grant Notification Access before media sessions are visible. */
class MediaSessionNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() { super.onListenerConnected(); instance = this }
    override fun onListenerDisconnected() { instance = null; super.onListenerDisconnected() }
    companion object { @Volatile var instance: MediaSessionNotificationListener? = null }
}
