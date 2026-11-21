package com.tk.quicksearch.search.apps.notificationDots

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.tk.quicksearch.search.utils.UserHandleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object NotificationDotsStore {
    private val countsState = MutableStateFlow<Map<String, Int>>(emptyMap())

    /** Active notification count per app key (package, and package:userId). */
    val counts: StateFlow<Map<String, Int>> = countsState.asStateFlow()

    fun updateFromNotifications(notifications: Array<StatusBarNotification>?) {
        val counts = HashMap<String, Int>()
        notifications
            .orEmpty()
            .filter { it.countsForDot() }
            .forEach { sbn -> sbn.dotKeys().forEach { key -> counts[key] = (counts[key] ?: 0) + 1 } }
        countsState.value = counts
    }

    fun clear() {
        countsState.value = emptyMap()
    }
}

internal fun StatusBarNotification.countsForDot(): Boolean =
    notification.flags and Notification.FLAG_GROUP_SUMMARY == 0

internal fun StatusBarNotification.dotKeys(): List<String> {
    val userId = UserHandleUtils.getIdentifier(user)
    return listOf(packageName, "$packageName:$userId")
}
