package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

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
