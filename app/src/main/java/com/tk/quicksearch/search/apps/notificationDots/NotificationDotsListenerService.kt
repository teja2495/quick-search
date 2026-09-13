package com.tk.quicksearch.search.apps.notificationDots

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationDotsListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationDotsStore.updateFromNotifications(runCatching { activeNotifications }.getOrNull())
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationDotsStore.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        NotificationDotsStore.updateFromNotifications(runCatching { activeNotifications }.getOrNull())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        NotificationDotsStore.updateFromNotifications(runCatching { activeNotifications }.getOrNull())
    }
}
