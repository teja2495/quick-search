package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import android.content.Context
import android.content.Intent
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.data.preferences.GlancePreferences.Companion.LOW_STORAGE_THRESHOLD_PERCENT
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The phone's charge while it is plugged in, shown in the home At a Glance card. */
internal class ChargingGlance(
    val percent: Int,
    val isFull: Boolean,
    /** Plugged in but not charging, such as when held at a charge limit; no time to full then. */
    val isPaused: Boolean,
    val timeToFullMillis: Long?,
    /** Hides the row until the phone is unplugged and plugged in again. */
    val dismiss: () -> Unit,
)

/** Internal storage once free space falls to [LOW_STORAGE_THRESHOLD_PERCENT] or less. */
internal class LowStorageGlance(
    val freeBytes: Long,
    val totalBytes: Long,
    val open: () -> Unit,
    /** Hides the row until free space drops another percent of the total. */
    val dismiss: () -> Unit,
)

@Composable
internal fun ChargingRow(glance: ChargingGlance) {
    val percentLabel =
        remember(glance.percent) { NumberFormat.getPercentInstance().format(glance.percent / 100.0) }
    val timeToFull = glance.timeToFullMillis?.takeUnless { glance.isFull || glance.isPaused }
    val durationLabel = remember(timeToFull) { timeToFull?.let(::formatShortDuration) }
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = if (glance.isFull) MaterialSymbols.BatteryFull else MaterialSymbols.BatteryChargingFull,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title =
            stringResource(
                when {
                    glance.isFull -> R.string.home_charged
                    glance.isPaused -> R.string.home_charging_paused
                    else -> R.string.settings_at_a_glance_charging_title
                },
            ),
        subtitle = durationLabel?.let { stringResource(R.string.home_charging_full_in, it) },
        pillText = percentLabel,
        pillColors =
            GlancePillColors(
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        onClick = null,
        onDismiss = glance.dismiss,
    )
}

/** Hours and minutes in the locale's short unit style, such as "1 hr, 5 min". */
internal fun formatShortDuration(millis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis).coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val format = MeasureFormat.getInstance(Locale.getDefault(), MeasureFormat.FormatWidth.SHORT)
    val measures =
        buildList {
            if (hours > 0) add(Measure(hours, MeasureUnit.HOUR))
            if (minutes > 0 || hours == 0L) add(Measure(minutes, MeasureUnit.MINUTE))
        }
    return format.formatMeasures(*measures.toTypedArray())
}

/** Reads free space on resume while [enabled] and the toggle is on. */
@Composable
internal fun rememberLowStorageGlance(enabled: Boolean): LowStorageGlance? {
    val context = LocalContext.current
    val preferences = remember(context) { GlancePreferences(context.applicationContext) }
    val refreshKey = rememberResumeRefreshKey()
    var space by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var isDismissed by remember { mutableStateOf(false) }

    // Not keyed on [enabled]: typing a query hides the row without reading storage again.
    LaunchedEffect(refreshKey) {
        space =
            if (preferences.isShowLowStorageEnabled()) {
                withContext(Dispatchers.IO) { readInternalStorage() }
            } else {
                null
            }
        isDismissed = space?.let { (free, total) -> updateLowStorageDismissal(preferences, free, total) } ?: false
    }

    if (!enabled) return null
    val (free, total) = space ?: return null
    if (isDismissed || !isLowStorage(free, total)) return null
    return LowStorageGlance(
        freeBytes = free,
        totalBytes = total,
        open = { openStorageSettings(context) },
        dismiss = {
            preferences.setLowStorageDismissedFreePercent(freePercent(free, total))
            isDismissed = true
        },
    )
}

internal fun isLowStorage(
    free: Long,
    total: Long,
) = total > 0L && free * 100 <= total * LOW_STORAGE_THRESHOLD_PERCENT

internal fun freePercent(
    free: Long,
    total: Long,
) = free * 100f / total

/**
 * Whether a dismissed low storage row stays hidden. It returns once free space is a full percent of
 * the total below the level last seen since dismissal, and the dismissal ends once storage is no
 * longer low.
 */
internal fun updateLowStorageDismissal(
    preferences: GlancePreferences,
    free: Long,
    total: Long,
): Boolean {
    val dismissedAt = preferences.getLowStorageDismissedFreePercent() ?: return false
    val current = freePercent(free, total)
    return when {
        !isLowStorage(free, total) || current <= dismissedAt - 1f -> {
            preferences.setLowStorageDismissedFreePercent(null)
            false
        }
        current > dismissedAt -> {
            preferences.setLowStorageDismissedFreePercent(current)
            true
        }
        else -> true
    }
}

internal fun readInternalStorage(): Pair<Long, Long>? =
    runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        stat.availableBytes to stat.totalBytes
    }.getOrNull()

internal fun openStorageSettings(context: Context) {
    openFirstAvailable(
        context,
        Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        Intent(Settings.ACTION_SETTINGS),
    )
}

@Composable
internal fun LowStorageRow(glance: LowStorageGlance) {
    val context = LocalContext.current
    val free = remember(glance.freeBytes) { Formatter.formatShortFileSize(context, glance.freeBytes) }
    val total = remember(glance.totalBytes) { Formatter.formatShortFileSize(context, glance.totalBytes) }
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = stringResource(R.string.home_storage_almost_full),
        subtitle = stringResource(R.string.home_storage_free, free, total),
        onClick = glance.open,
        onDismiss = glance.dismiss,
    )
}
