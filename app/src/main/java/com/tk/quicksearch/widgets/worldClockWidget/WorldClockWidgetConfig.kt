package com.tk.quicksearch.widgets.worldClockWidget

import android.content.Context
import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetColors
import com.tk.quicksearch.widgets.noteWidget.withNoteWidgetDefaults
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.applyWidgetPreferences
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import kotlinx.parcelize.Parcelize

/** One clock on a World Clock widget: a time zone and an optional name to show instead of its city. */
@Parcelize
data class WorldClockLocation(
    val zoneId: String,
    val label: String = "",
) : Parcelable

private object WorldClockKeys {
    val LOCATIONS = stringPreferencesKey("world_clock_widget_locations")
    val TEXT_SIZE = floatPreferencesKey("world_clock_widget_text_size")
}

internal object WorldClockWidgetDefaults {
    const val MAX_LOCATIONS = 4
    const val TEXT_SIZE_SP = 28f
    const val TEXT_SIZE_MIN_SP = 12f
    const val TEXT_SIZE_MAX_SP = 56f
    const val LABEL_MAX_LENGTH = 40

    /** Separates locations, and a location's zone from its label, in the stored string. */
    const val LOCATION_SEPARATOR = "\n"
    const val FIELD_SEPARATOR = "\t"
}

/**
 * A World Clock widget's settings. [style] holds the background, text color, corner radius and
 * transparency in the same keys the other widgets use; the rest are world-clock-only keys.
 */
@Parcelize
data class WorldClockWidgetConfig(
    val style: WidgetPreferences = WidgetPreferences.Default.withNoteWidgetDefaults(),
    /** One to [WorldClockWidgetDefaults.MAX_LOCATIONS] clocks, shown left to right. */
    val locations: List<WorldClockLocation> = emptyList(),
    val textSizeSp: Float = WorldClockWidgetDefaults.TEXT_SIZE_SP,
) : Parcelable {
    val canAddLocation: Boolean get() = locations.size < WorldClockWidgetDefaults.MAX_LOCATIONS

    fun coerceToValidRanges(): WorldClockWidgetConfig =
        copy(
            style = style.coerceToValidRanges(),
            locations =
                locations
                    .filter { WorldClockZones.isValid(it.zoneId) }
                    .map { it.copy(label = it.label.sanitizedLabel()) }
                    .take(WorldClockWidgetDefaults.MAX_LOCATIONS),
            textSizeSp =
                if (textSizeSp.isFinite()) {
                    textSizeSp.coerceIn(WorldClockWidgetDefaults.TEXT_SIZE_MIN_SP, WorldClockWidgetDefaults.TEXT_SIZE_MAX_SP)
                } else {
                    WorldClockWidgetDefaults.TEXT_SIZE_SP
                },
        )
}

/** A label without the characters the stored string uses as separators. */
internal fun String.sanitizedLabel(): String =
    replace(WorldClockWidgetDefaults.LOCATION_SEPARATOR, " ")
        .replace(WorldClockWidgetDefaults.FIELD_SEPARATOR, " ")
        .take(WorldClockWidgetDefaults.LABEL_MAX_LENGTH)

private fun encodeLocations(locations: List<WorldClockLocation>): String =
    locations.joinToString(WorldClockWidgetDefaults.LOCATION_SEPARATOR) {
        it.zoneId + WorldClockWidgetDefaults.FIELD_SEPARATOR + it.label.sanitizedLabel()
    }

private fun decodeLocations(value: String?): List<WorldClockLocation> =
    value
        .orEmpty()
        .split(WorldClockWidgetDefaults.LOCATION_SEPARATOR)
        .filter { it.isNotBlank() }
        .map { entry ->
            val parts = entry.split(WorldClockWidgetDefaults.FIELD_SEPARATOR, limit = 2)
            WorldClockLocation(zoneId = parts[0], label = parts.getOrElse(1) { "" })
        }

/** Reads a World Clock widget's settings; a widget that was never saved gets the defaults. */
fun Preferences.toWorldClockWidgetConfig(context: Context): WorldClockWidgetConfig {
    val storedLocations = this[WorldClockKeys.LOCATIONS]
    return WorldClockWidgetConfig(
        // The shared defaults suit the search widget, so an unsaved widget starts from the Note look.
        style = if (storedLocations == null) WorldClockWidgetConfig().style else toWidgetPreferences(context),
        locations = decodeLocations(storedLocations),
        textSizeSp = this[WorldClockKeys.TEXT_SIZE] ?: WorldClockWidgetDefaults.TEXT_SIZE_SP,
    ).coerceToValidRanges()
}

fun MutablePreferences.applyWorldClockWidgetConfig(
    config: WorldClockWidgetConfig,
    context: Context,
) {
    val validated = config.coerceToValidRanges()
    applyWidgetPreferences(validated.style, context)
    this[WorldClockKeys.LOCATIONS] = encodeLocations(validated.locations)
    this[WorldClockKeys.TEXT_SIZE] = validated.textSizeSp
}

/** World Clock widget colors; the transparency setting applies to [background]. */
internal data class WorldClockWidgetColors(
    val background: Color,
    val text: Color,
    val secondaryText: Color,
) {
    companion object {
        private const val SECONDARY_ALPHA = 0.75f

        /** Like the Note widget's colors, plus an optional custom text color. */
        fun forWidget(
            config: WorldClockWidgetConfig,
            isSystemDark: Boolean,
        ): WorldClockWidgetColors {
            val base = NoteWidgetColors.forWidget(config.style, isSystemDark)
            val text = config.style.customTextIconColor?.let(::Color) ?: base.title
            return WorldClockWidgetColors(
                background = base.background,
                text = text,
                secondaryText = text.copy(alpha = SECONDARY_ALPHA),
            )
        }
    }
}
