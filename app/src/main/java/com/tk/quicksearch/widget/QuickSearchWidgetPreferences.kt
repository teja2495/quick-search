package com.tk.quicksearch.widget

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.parcelize.Parcelize

/**
 * Defines the action to perform when the mic icon is tapped on the widget.
 */
enum class MicAction(val value: String) {
    DEFAULT_VOICE_SEARCH("default_voice_search"),
    DIGITAL_ASSISTANT("digital_assistant")
}

internal object WidgetDefaults {
    val BORDER_COLOR = Color.Black
    val BORDER_COLOR_ARGB = BORDER_COLOR.toArgb()
    const val BORDER_RADIUS_DP = 29f
    const val BORDER_WIDTH_DP = 1.5f
    const val SHOW_LABEL = true
    const val SHOW_SEARCH_ICON = true
    const val SHOW_MIC_ICON = true
    // Default to black background for higher contrast out of the box.
    const val BACKGROUND_COLOR_IS_WHITE = false
    const val BACKGROUND_ALPHA = 0.35f
    // Default text/icon color inverts the background for readability:
    // white text on dark backgrounds, dark text on light backgrounds.
    const val TEXT_ICON_COLOR_IS_WHITE = false
    const val ICON_ALIGN_LEFT = true
    val MIC_ACTION = MicAction.DEFAULT_VOICE_SEARCH
}

private object WidgetRanges {
    const val BORDER_RADIUS_MIN = 0f
    const val BORDER_RADIUS_MAX = 30f
    const val BORDER_WIDTH_MIN = 0f
    const val BORDER_WIDTH_MAX = 4f
    const val BACKGROUND_ALPHA_MIN = 0f
    const val BACKGROUND_ALPHA_MAX = 1f
}

private object WidgetKeys {
    val BORDER_COLOR = intPreferencesKey("quick_search_widget_border_color")
    val BORDER_RADIUS = floatPreferencesKey("quick_search_widget_border_radius")
    val BORDER_WIDTH = floatPreferencesKey("quick_search_widget_border_width")
    val SHOW_LABEL = booleanPreferencesKey("quick_search_widget_show_label")
    val SHOW_SEARCH_ICON = booleanPreferencesKey("quick_search_widget_show_search_icon")
    val SHOW_MIC_ICON = booleanPreferencesKey("quick_search_widget_show_mic_icon")
    val BACKGROUND_COLOR_IS_WHITE = booleanPreferencesKey("quick_search_widget_background_color_is_white")
    val BACKGROUND_ALPHA = floatPreferencesKey("quick_search_widget_background_alpha")
    val TEXT_ICON_COLOR_IS_WHITE = booleanPreferencesKey("quick_search_widget_text_icon_color_is_white")
    val ICON_ALIGN_LEFT = booleanPreferencesKey("quick_search_widget_icon_align_left")
    val MIC_ACTION = stringPreferencesKey("quick_search_widget_mic_action")
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
    val showSearchIcon: Boolean = WidgetDefaults.SHOW_SEARCH_ICON,
    val showMicIcon: Boolean = WidgetDefaults.SHOW_MIC_ICON,
    val backgroundColorIsWhite: Boolean = WidgetDefaults.BACKGROUND_COLOR_IS_WHITE,
    val backgroundAlpha: Float = WidgetDefaults.BACKGROUND_ALPHA,
    val textIconColorIsWhite: Boolean = WidgetDefaults.TEXT_ICON_COLOR_IS_WHITE,
    val iconAlignLeft: Boolean = WidgetDefaults.ICON_ALIGN_LEFT,
    val micAction: MicAction = WidgetDefaults.MIC_ACTION
) : Parcelable {

    companion object {
        /**
         * Default widget preferences instance.
         */
        val Default = QuickSearchWidgetPreferences()
    }

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

fun Preferences.toWidgetPreferences(): QuickSearchWidgetPreferences {
    val backgroundIsWhite = this[WidgetKeys.BACKGROUND_COLOR_IS_WHITE]
        ?: WidgetDefaults.BACKGROUND_COLOR_IS_WHITE
    val textIconIsWhite = this[WidgetKeys.TEXT_ICON_COLOR_IS_WHITE]
        // Sensible default: invert background for readability when no preference saved.
        ?: !backgroundIsWhite

    return QuickSearchWidgetPreferences(
        borderColor = this[WidgetKeys.BORDER_COLOR] ?: WidgetDefaults.BORDER_COLOR_ARGB,
        borderRadiusDp = this[WidgetKeys.BORDER_RADIUS] ?: WidgetDefaults.BORDER_RADIUS_DP,
        borderWidthDp = this[WidgetKeys.BORDER_WIDTH] ?: WidgetDefaults.BORDER_WIDTH_DP,
        showLabel = this[WidgetKeys.SHOW_LABEL] ?: WidgetDefaults.SHOW_LABEL,
        showSearchIcon = this[WidgetKeys.SHOW_SEARCH_ICON] ?: WidgetDefaults.SHOW_SEARCH_ICON,
        showMicIcon = this[WidgetKeys.SHOW_MIC_ICON] ?: WidgetDefaults.SHOW_MIC_ICON,
        backgroundColorIsWhite = backgroundIsWhite,
        backgroundAlpha = this[WidgetKeys.BACKGROUND_ALPHA] ?: WidgetDefaults.BACKGROUND_ALPHA,
        textIconColorIsWhite = textIconIsWhite,
        iconAlignLeft = this[WidgetKeys.ICON_ALIGN_LEFT] ?: WidgetDefaults.ICON_ALIGN_LEFT,
        micAction = this[WidgetKeys.MIC_ACTION]?.let { actionString ->
            MicAction.entries.find { it.value == actionString }
        } ?: WidgetDefaults.MIC_ACTION
    ).coerceToValidRanges()
}

fun MutablePreferences.applyWidgetPreferences(config: QuickSearchWidgetPreferences) {
    val validated = config.coerceToValidRanges()
    this[WidgetKeys.BORDER_COLOR] = validated.borderColor
    this[WidgetKeys.BORDER_RADIUS] = validated.borderRadiusDp
    this[WidgetKeys.BORDER_WIDTH] = validated.borderWidthDp
    this[WidgetKeys.SHOW_LABEL] = validated.showLabel
    this[WidgetKeys.SHOW_SEARCH_ICON] = validated.showSearchIcon
    this[WidgetKeys.SHOW_MIC_ICON] = validated.showMicIcon
    this[WidgetKeys.BACKGROUND_COLOR_IS_WHITE] = validated.backgroundColorIsWhite
    this[WidgetKeys.BACKGROUND_ALPHA] = validated.backgroundAlpha
    this[WidgetKeys.TEXT_ICON_COLOR_IS_WHITE] = validated.textIconColorIsWhite
    this[WidgetKeys.ICON_ALIGN_LEFT] = validated.iconAlignLeft
    this[WidgetKeys.MIC_ACTION] = validated.micAction.value
}

