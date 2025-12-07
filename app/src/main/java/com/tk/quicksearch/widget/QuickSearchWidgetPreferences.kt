package com.tk.quicksearch.widget

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.parcelize.Parcelize

// Default values
private object WidgetDefaults {
    val BORDER_COLOR = Color(0xFFE2E5F0)
    val BORDER_COLOR_ARGB = BORDER_COLOR.toArgb()
    const val BORDER_RADIUS_DP = 30f
    const val BORDER_WIDTH_DP = 1.5f
    const val SHOW_LABEL = true
    const val BACKGROUND_COLOR_IS_WHITE = true
    const val BACKGROUND_ALPHA = 0.35f
}

// Value ranges for validation
private object WidgetRanges {
    const val BORDER_RADIUS_MIN = 0f
    const val BORDER_RADIUS_MAX = 30f
    const val BORDER_WIDTH_MIN = 0f
    const val BORDER_WIDTH_MAX = 4f
    const val BACKGROUND_ALPHA_MIN = 0f
    const val BACKGROUND_ALPHA_MAX = 1f
}

// Preference keys
private object WidgetKeys {
    val BORDER_COLOR = intPreferencesKey("quick_search_widget_border_color")
    val BORDER_RADIUS = floatPreferencesKey("quick_search_widget_border_radius")
    val BORDER_WIDTH = floatPreferencesKey("quick_search_widget_border_width")
    val SHOW_LABEL = booleanPreferencesKey("quick_search_widget_show_label")
    val BACKGROUND_COLOR_IS_WHITE = booleanPreferencesKey("quick_search_widget_background_color_is_white")
    val BACKGROUND_ALPHA = floatPreferencesKey("quick_search_widget_background_alpha")
}

/**
 * Preferences for the Quick Search widget appearance and behavior.
 */
@Parcelize
data class QuickSearchWidgetPreferences(
    val borderColor: Int = WidgetDefaults.BORDER_COLOR_ARGB,
    val borderRadiusDp: Float = WidgetDefaults.BORDER_RADIUS_DP,
    val borderWidthDp: Float = WidgetDefaults.BORDER_WIDTH_DP,
    val showLabel: Boolean = WidgetDefaults.SHOW_LABEL,
    val backgroundColorIsWhite: Boolean = WidgetDefaults.BACKGROUND_COLOR_IS_WHITE,
    val backgroundAlpha: Float = WidgetDefaults.BACKGROUND_ALPHA
) : Parcelable {

    companion object {
        /**
         * Default widget preferences instance.
         */
        val Default = QuickSearchWidgetPreferences()
    }

    /**
     * Returns a copy of this preferences object with values coerced to valid ranges.
     */
    fun coerceToValidRanges(): QuickSearchWidgetPreferences {
        return copy(
            borderRadiusDp = borderRadiusDp.coerceIn(
                WidgetRanges.BORDER_RADIUS_MIN,
                WidgetRanges.BORDER_RADIUS_MAX
            ),
            borderWidthDp = borderWidthDp.coerceIn(
                WidgetRanges.BORDER_WIDTH_MIN,
                WidgetRanges.BORDER_WIDTH_MAX
            ),
            backgroundAlpha = backgroundAlpha.coerceIn(
                WidgetRanges.BACKGROUND_ALPHA_MIN,
                WidgetRanges.BACKGROUND_ALPHA_MAX
            )
        )
    }
}

/**
 * Converts Preferences to QuickSearchWidgetPreferences, applying defaults and validation.
 */
fun Preferences.toWidgetPreferences(): QuickSearchWidgetPreferences {
    return QuickSearchWidgetPreferences(
        borderColor = this[WidgetKeys.BORDER_COLOR] ?: WidgetDefaults.BORDER_COLOR_ARGB,
        borderRadiusDp = this[WidgetKeys.BORDER_RADIUS] ?: WidgetDefaults.BORDER_RADIUS_DP,
        borderWidthDp = this[WidgetKeys.BORDER_WIDTH] ?: WidgetDefaults.BORDER_WIDTH_DP,
        showLabel = this[WidgetKeys.SHOW_LABEL] ?: WidgetDefaults.SHOW_LABEL,
        backgroundColorIsWhite = this[WidgetKeys.BACKGROUND_COLOR_IS_WHITE]
            ?: WidgetDefaults.BACKGROUND_COLOR_IS_WHITE,
        backgroundAlpha = this[WidgetKeys.BACKGROUND_ALPHA] ?: WidgetDefaults.BACKGROUND_ALPHA
    ).coerceToValidRanges()
}

/**
 * Applies widget preferences to MutablePreferences, with validation.
 */
fun MutablePreferences.applyWidgetPreferences(config: QuickSearchWidgetPreferences) {
    val validated = config.coerceToValidRanges()
    this[WidgetKeys.BORDER_COLOR] = validated.borderColor
    this[WidgetKeys.BORDER_RADIUS] = validated.borderRadiusDp
    this[WidgetKeys.BORDER_WIDTH] = validated.borderWidthDp
    this[WidgetKeys.SHOW_LABEL] = validated.showLabel
    this[WidgetKeys.BACKGROUND_COLOR_IS_WHITE] = validated.backgroundColorIsWhite
    this[WidgetKeys.BACKGROUND_ALPHA] = validated.backgroundAlpha
}

