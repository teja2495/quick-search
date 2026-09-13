package com.tk.quicksearch.search.apps.notificationDots

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.tk.quicksearch.search.common.UserHandleUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object NotificationDotsStore {
    private val keysState = MutableStateFlow<Set<String>>(emptySet())

    val keys: StateFlow<Set<String>> = keysState.asStateFlow()

    fun updateFromNotifications(notifications: Array<StatusBarNotification>?) {
        keysState.value =
            notifications
                .orEmpty()
                .filter { it.countsForDot() }
                .flatMap { it.dotKeys() }
                .toSet()
    }

    fun clear() {
        keysState.value = emptySet()
    }
}

internal fun StatusBarNotification.countsForDot(): Boolean =
    notification.flags and Notification.FLAG_GROUP_SUMMARY == 0

internal fun StatusBarNotification.dotKeys(): List<String> {
    val userId = UserHandleUtils.getIdentifier(user)
    return listOf(packageName, "$packageName:$userId")
}
