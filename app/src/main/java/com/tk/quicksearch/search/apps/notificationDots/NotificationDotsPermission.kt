package com.tk.quicksearch.search.apps.notificationDots

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

object NotificationDotsPermission {
    fun hasNotificationListenerAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun canEnableNotificationDots(context: Context): Boolean = hasNotificationListenerAccess(context)

    fun openNotificationListenerSettings(context: Context) {
        val component = ComponentName(context, NotificationDotsListenerService::class.java)
        val detailIntent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        component.flattenToString(),
                    )
                }
            } else {
                null
            }
        val listIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val activity = findActivity(context)
        listOfNotNull(detailIntent, listIntent).forEach { intent ->
            if (activity != null) {
                val started = runCatching { activity.startActivity(intent) }.isSuccess
                if (started) return
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val started = runCatching { context.startActivity(intent) }.isSuccess
                if (started) return
            }
        }
    }

    fun requestRebind(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        runCatching {
            NotificationListenerService.requestRebind(
                ComponentName(context, NotificationDotsListenerService::class.java),
            )
        }
    }

    private fun findActivity(context: Context): Activity? =
        when (context) {
            is Activity -> context
            is ContextWrapper -> context.baseContext?.let(::findActivity)
            else -> null
        }
}

@Composable
fun rememberNotificationDotsCheckedChange(onEnabledChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val context = LocalContext.current
    val onEnabledChangeState = rememberUpdatedState(onEnabledChange)
    val pendingEnable = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val enableIfPermitted =
        remember(context) {
            {
                if (NotificationDotsPermission.canEnableNotificationDots(context)) {
                    pendingEnable.value = false
                    onEnabledChangeState.value(true)
                    NotificationDotsPermission.requestRebind(context)
                    true
                } else {
                    false
                }
            }
        }

    DisposableEffect(lifecycleOwner, enableIfPermitted) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && pendingEnable.value) {
                    enableIfPermitted()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(enableIfPermitted, context) {
        { enabled ->
            if (!enabled) {
                pendingEnable.value = false
                onEnabledChangeState.value(false)
            } else if (NotificationDotsPermission.canEnableNotificationDots(context)) {
                enableIfPermitted()
            } else {
                pendingEnable.value = true
                NotificationDotsPermission.openNotificationListenerSettings(context)
            }
        }
    }
}
