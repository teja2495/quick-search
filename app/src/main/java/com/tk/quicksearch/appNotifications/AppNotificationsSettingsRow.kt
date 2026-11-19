package com.tk.quicksearch.appNotifications

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotsCheckedChange
import com.tk.quicksearch.appNotifications.AppNotificationFilters
import com.tk.quicksearch.appNotifications.AppNotificationsSettings
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance.rememberAppNotificationsConfig
import com.tk.quicksearch.settings.shared.SettingsToggleRow

/**
 * The App notifications toggle in At a Glance settings. It needs notification access like the
 * other notification rows, counts the apps and keywords set up, and its text opens their page.
 */
@Composable
internal fun AppNotificationsSettingsRow(
    hasNotificationAccess: Boolean,
    isFirstItem: Boolean,
    isLastItem: Boolean,
) {
    val context = LocalContext.current
    // Read off the main thread when Home hasn't already, then kept current as apps, keywords or
    // the toggle change on their page, elsewhere or by import. Shows off and unset until read.
    val config = rememberAppNotificationsConfig()
    val filters = config?.filters ?: AppNotificationFilters()
    val onCheckedChange = rememberNotificationDotsCheckedChange { enabled ->
        AppNotificationsSettings.setEnabled(context, enabled)
    }
    val checked = config?.enabled == true && hasNotificationAccess

    SettingsToggleRow(
        title = stringResource(R.string.settings_at_a_glance_app_notifications_title),
        subtitle = appNotificationsSubtitle(hasNotificationAccess, filters),
        checked = checked,
        onCheckedChange = onCheckedChange,
        // Without access, the text asks for it (and turns the toggle on) instead of opening the page.
        onRowClick = { if (hasNotificationAccess) openAppNotificationsSettings(context) else onCheckedChange(true) },
        showNavigationChevron = true,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
    )
}

/**
 * App notifications in an At a Glance widget's items: on or off for the widget alone, following the
 * app's toggle until changed. Its text opens the widget's own apps and keywords, which start as the
 * app's ([widgetFilters] null) and go to [onFiltersChange] once edited.
 */
@Composable
internal fun WidgetAppNotificationsSettingsRow(
    hasNotificationAccess: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    widgetFilters: AppNotificationFilters?,
    onFiltersChange: (AppNotificationFilters) -> Unit,
    isFirstItem: Boolean,
    isLastItem: Boolean,
) {
    val context = LocalContext.current
    val filters = widgetFilters ?: rememberAppNotificationsConfig()?.filters ?: AppNotificationFilters()
    val onToggle = rememberNotificationDotsCheckedChange(onCheckedChange)
    val editor =
        rememberLauncherForActivityResult(EditWidgetAppNotificationFilters) { edited ->
            if (edited != null && edited != filters) onFiltersChange(edited)
        }

    SettingsToggleRow(
        title = stringResource(R.string.settings_at_a_glance_app_notifications_title),
        subtitle = appNotificationsSubtitle(hasNotificationAccess, filters),
        checked = checked && hasNotificationAccess,
        onCheckedChange = onToggle,
        onRowClick = {
            if (!hasNotificationAccess) {
                onToggle(true)
                return@SettingsToggleRow
            }
            editor.launch(filters)
            @Suppress("DEPRECATION")
            (context as? Activity)?.overridePendingTransition(R.anim.custom_info_slide_in_right, R.anim.custom_info_slide_out_left)
        },
        showNavigationChevron = true,
        isFirstItem = isFirstItem,
        isLastItem = isLastItem,
    )
}

@Composable
private fun appNotificationsSubtitle(
    hasNotificationAccess: Boolean,
    filters: AppNotificationFilters,
): String =
    when {
        !hasNotificationAccess -> stringResource(R.string.settings_overlay_source_needs_permission)
        filters.isEmpty -> stringResource(R.string.settings_at_a_glance_app_notifications_desc)
        else -> stringResource(
            R.string.settings_at_a_glance_app_notifications_summary,
            filters.apps.size,
            filters.keywords.size,
        )
    }

internal fun openAppNotificationsSettings(context: Context) {
    context.startActivity(Intent(context, AppNotificationsActivity::class.java))
    @Suppress("DEPRECATION")
    (context as? Activity)?.overridePendingTransition(R.anim.custom_info_slide_in_right, R.anim.custom_info_slide_out_left)
}
