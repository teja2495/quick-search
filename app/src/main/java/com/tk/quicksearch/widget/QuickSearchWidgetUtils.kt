package com.tk.quicksearch.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.tk.quicksearch.R

fun requestAddQuickSearchWidget(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        requestPinForAndroidOPlus(context)
    } else {
        showUnsupportedVersionToast(context)
    }
}

private fun requestPinForAndroidOPlus(context: Context) {
    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
    if (appWidgetManager?.isRequestPinAppWidgetSupported != true) {
        showUnsupportedVersionToast(context)
        return
    }

    val provider = ComponentName(context, QuickSearchWidgetReceiver::class.java)
    try {
        val requested = appWidgetManager.requestPinAppWidget(provider, null, null)
        if (!requested) {
            showErrorToast(context)
        }
    } catch (e: Exception) {
        showErrorToast(context)
    }
}

private fun showUnsupportedVersionToast(context: Context) {
    showToast(context, R.string.home_screen_widget_not_supported, Toast.LENGTH_LONG)
}

private fun showErrorToast(context: Context) {
    showToast(context, R.string.home_screen_widget_error)
}

private fun showToast(
    context: Context,
    messageResId: Int,
    duration: Int = Toast.LENGTH_SHORT,
) {
    Toast.makeText(context, context.getString(messageResId), duration).show()
}
