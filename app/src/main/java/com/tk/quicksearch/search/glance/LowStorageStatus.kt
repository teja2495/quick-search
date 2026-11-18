package com.tk.quicksearch.search.glance

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.openFirstAvailable
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

private const val LOW_STORAGE_THRESHOLD_PERCENT = 10

internal val lowStorageStatus = GlanceStatusDefinition(
    preferenceKey = "home_show_low_storage",
    settingsTitle = R.string.settings_at_a_glance_storage_title,
    settingsDescription = { it.getString(R.string.settings_at_a_glance_storage_desc, LOW_STORAGE_THRESHOLD_PERCENT) },
    rowTitle = R.string.home_storage_almost_full,
    icon = { MaterialSymbols.Storage },
    read = ::readLowStorage,
    isError = true,
    open = ::openStorageSettings,
    dismissFromWidget = { context ->
        readInternalStorage()?.let { space ->
            GlancePreferences(context).setLowStorageDismissedFreePercent(space.freePercent)
        }
    },
)

private fun readLowStorage(context: Context, surface: GlanceStatusSurface): GlanceStatusValue? {
    val space = readInternalStorage() ?: return null
    val isLow = isLowStorage(space.freeBytes, space.totalBytes)
    // Home clears a recovered dismissal on resume. A widget refresh only updates it for low space.
    if (!isLow && surface == GlanceStatusSurface.WIDGET) return null
    val preferences = GlancePreferences(context)
    val dismissedAt = preferences.getLowStorageDismissedFreePercent()
    val next = nextLowStorageDismissal(dismissedAt, space.freePercent, isLow)
    if (next != dismissedAt) preferences.setLowStorageDismissedFreePercent(next)
    if (next != null || !isLow) return null
    return GlanceStatusValue(
        subtitle = context.getString(
            R.string.home_storage_free,
            Formatter.formatShortFileSize(context, space.freeBytes),
            Formatter.formatShortFileSize(context, space.totalBytes),
        ),
        dismissFromHome = { preferences.setLowStorageDismissedFreePercent(space.freePercent) },
    )
}

internal fun isLowStorage(free: Long, total: Long): Boolean =
    total > 0L && free * 100 <= total * LOW_STORAGE_THRESHOLD_PERCENT

/** Bring the row back after a drop of one percentage point from the highest level since dismissing. */
internal fun nextLowStorageDismissal(dismissedAt: Float?, current: Float, isLow: Boolean): Float? =
    when {
        dismissedAt == null || !isLow || current <= dismissedAt - 1f -> null
        current > dismissedAt -> current
        else -> dismissedAt
    }

private class InternalStorage(val freeBytes: Long, val totalBytes: Long) {
    val freePercent: Float get() = freeBytes * 100f / totalBytes
}

private fun readInternalStorage(): InternalStorage? =
    runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        InternalStorage(stat.availableBytes, stat.totalBytes)
    }.getOrNull()

private fun openStorageSettings(context: Context) {
    openFirstAvailable(
        context,
        Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
}
