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
import com.tk.quicksearch.widget.voiceSearch.MicAction
import kotlinx.parcelize.Parcelize
import com.tk.quicksearch.widget.voiceSearch.MicAction.DEFAULT_VOICE_SEARCH
import com.tk.quicksearch.widget.voiceSearch.MicAction.DIGITAL_ASSISTANT
import com.tk.quicksearch.widget.voiceSearch.MicAction.OFF

/**
 * Theme options for the widget appearance.
 */
enum class WidgetTheme(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system")
}

/**
 * Text/Icon color override options.
 */
enum class TextIconColorOverride(val value: String) {
    WHITE("white"),
    BLACK("black"),
    THEME("theme")
}

/**
 * Search icon display options.
 */
enum class SearchIconDisplay(val value: String) {
    LEFT("left"),
    CENTER("center"),
    OFF("off")
}



internal object WidgetDefaults {
    val BORDER_COLOR = Color.Black
    val BORDER_COLOR_ARGB = BORDER_COLOR.toArgb()
    const val BORDER_RADIUS_DP = 29f
    const val BORDER_WIDTH_DP = 1.5f
    const val SHOW_LABEL = true
    // Default to left-aligned search icon (previously was showSearchIcon=true, iconAlignLeft=true)
    val SEARCH_ICON_DISPLAY = SearchIconDisplay.LEFT
    const val SHOW_MIC_ICON = true
    // Default to dark theme for higher contrast out of the box.
    val THEME = WidgetTheme.DARK
    const val BACKGROUND_ALPHA = 0.35f
    val MIC_ACTION = MicAction.DEFAULT_VOICE_SEARCH
    // Default to theme-based colors (null means follow theme)
    val TEXT_ICON_COLOR_OVERRIDE: Boolean? = null
    val CUSTOM_BUTTONS: List<CustomWidgetButtonAction?> = listOf(null, null)
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
    val SEARCH_ICON_DISPLAY = stringPreferencesKey("quick_search_widget_search_icon_display")
    val SHOW_MIC_ICON = booleanPreferencesKey("quick_search_widget_show_mic_icon")
    val THEME = stringPreferencesKey("quick_search_widget_theme")
    val BACKGROUND_ALPHA = floatPreferencesKey("quick_search_widget_background_alpha")
    val MIC_ACTION = stringPreferencesKey("quick_search_widget_mic_action")
    val TEXT_ICON_COLOR_OVERRIDE = stringPreferencesKey("quick_search_widget_text_icon_color_override")
    val CUSTOM_BUTTON_0 = stringPreferencesKey("quick_search_widget_custom_button_0")
    val CUSTOM_BUTTON_1 = stringPreferencesKey("quick_search_widget_custom_button_1")
    // Legacy keys for migration
    val SHOW_SEARCH_ICON = booleanPreferencesKey("quick_search_widget_show_search_icon")
    val ICON_ALIGN_LEFT = booleanPreferencesKey("quick_search_widget_icon_align_left")
    val BACKGROUND_COLOR_IS_WHITE = booleanPreferencesKey("quick_search_widget_background_color_is_white")
    val TEXT_ICON_COLOR_IS_WHITE = booleanPreferencesKey("quick_search_widget_text_icon_color_is_white")
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
    val searchIconDisplay: SearchIconDisplay = WidgetDefaults.SEARCH_ICON_DISPLAY,
    val showMicIcon: Boolean = WidgetDefaults.SHOW_MIC_ICON,
    val theme: WidgetTheme = WidgetDefaults.THEME,
    val backgroundAlpha: Float = WidgetDefaults.BACKGROUND_ALPHA,
    val micAction: MicAction = WidgetDefaults.MIC_ACTION,
    val textIconColorOverride: TextIconColorOverride = TextIconColorOverride.THEME,
    val customButtons: List<CustomWidgetButtonAction?> = WidgetDefaults.CUSTOM_BUTTONS
) : Parcelable {

    companion object {
        /**
         * Default widget preferences instance.
         */
        val Default = QuickSearchWidgetPreferences()
    }

    // Backward compatibility properties
    val showSearchIcon: Boolean
        get() = searchIconDisplay != SearchIconDisplay.OFF

    val iconAlignLeft: Boolean
        get() = searchIconDisplay == SearchIconDisplay.LEFT

    val hasCustomButtons: Boolean
        get() = customButtons.any { it != null }

    /**
     * Get the effective text/icon color considering both theme and override.
     */
    fun getEffectiveTextIconColor(isSystemInDarkTheme: Boolean = false): Boolean {
        return when (textIconColorOverride) {
            TextIconColorOverride.WHITE -> true   // white text/icons
            TextIconColorOverride.BLACK -> false  // black text/icons
            TextIconColorOverride.THEME -> {      // follow theme
                val effectiveTheme = when (theme) {
                    WidgetTheme.SYSTEM -> if (isSystemInDarkTheme) WidgetTheme.DARK else WidgetTheme.LIGHT
                    else -> theme
                }
                effectiveTheme == WidgetTheme.DARK // dark theme uses white text, light theme uses black
            }
        }
    }

    /**
     * Get background color based on current theme.
     */
    fun getBackgroundColor(): Boolean {
        return when (theme) {
            WidgetTheme.LIGHT -> true  // white background
            WidgetTheme.DARK -> false  // black/dark grey background
            WidgetTheme.SYSTEM -> false // default to dark for now (could be made dynamic later)
        }
    }

    /**
     * Get text/icon color based on current theme.
     */
    fun getTextIconColor(): Boolean {
        return when (theme) {
            WidgetTheme.LIGHT -> false // black text on white background
            WidgetTheme.DARK -> true   // white text on dark background
            WidgetTheme.SYSTEM -> true // default to white for now (could be made dynamic later)
        }
    }

    fun coerceToValidRanges(): QuickSearchWidgetPreferences {
        val normalizedButtons = normalizeCustomButtons(customButtons)
        val shouldHideLabel = normalizedButtons.any { it != null }
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
            ),
            customButtons = normalizedButtons,
            showLabel = if (shouldHideLabel) false else showLabel
        )
    }
}

private fun normalizeCustomButtons(
    buttons: List<CustomWidgetButtonAction?>
): List<CustomWidgetButtonAction?> {
    val normalized = buttons.take(2).toMutableList()
    while (normalized.size < 2) {
        normalized.add(null)
    }
    return normalized
}

fun Preferences.toWidgetPreferences(): QuickSearchWidgetPreferences {
    // Handle theme migration from legacy separate color preferences
    val theme = this[WidgetKeys.THEME]?.let { themeString ->
        WidgetTheme.entries.find { it.value == themeString }
    } ?: run {
        // Migration logic: convert old separate color preferences to theme
        val backgroundIsWhite = this[WidgetKeys.BACKGROUND_COLOR_IS_WHITE]
            ?: false // Default to dark (was the old default)
        val textIconIsWhite = this[WidgetKeys.TEXT_ICON_COLOR_IS_WHITE]
            ?: !backgroundIsWhite

        when {
            backgroundIsWhite && !textIconIsWhite -> WidgetTheme.LIGHT
            !backgroundIsWhite && textIconIsWhite -> WidgetTheme.DARK
            else -> WidgetDefaults.THEME
        }
    }

    val customButtons = listOf(
        CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_0]),
        CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_1])
    )

    return QuickSearchWidgetPreferences(
        borderColor = this[WidgetKeys.BORDER_COLOR] ?: WidgetDefaults.BORDER_COLOR_ARGB,
        borderRadiusDp = this[WidgetKeys.BORDER_RADIUS] ?: WidgetDefaults.BORDER_RADIUS_DP,
        borderWidthDp = this[WidgetKeys.BORDER_WIDTH] ?: WidgetDefaults.BORDER_WIDTH_DP,
        showLabel = this[WidgetKeys.SHOW_LABEL] ?: WidgetDefaults.SHOW_LABEL,
        searchIconDisplay = this[WidgetKeys.SEARCH_ICON_DISPLAY]?.let { displayString ->
            SearchIconDisplay.entries.find { it.value == displayString }
        } ?: run {
            // Migration logic: convert old separate boolean preferences to new enum
            val showSearchIcon = this[WidgetKeys.SHOW_SEARCH_ICON] ?: true // Default to true for backward compatibility
            val iconAlignLeft = this[WidgetKeys.ICON_ALIGN_LEFT] ?: true // Default to true for backward compatibility

            when {
                !showSearchIcon -> SearchIconDisplay.OFF
                iconAlignLeft -> SearchIconDisplay.LEFT
                else -> SearchIconDisplay.CENTER
            }
        },
        showMicIcon = true, // Always true now, mic visibility controlled by micAction
        theme = theme,
        backgroundAlpha = this[WidgetKeys.BACKGROUND_ALPHA] ?: WidgetDefaults.BACKGROUND_ALPHA,
        micAction = this[WidgetKeys.MIC_ACTION]?.let { actionString ->
            MicAction.entries.find { it.value == actionString }
        } ?: run {
            // Migration logic: convert old showMicIcon boolean to new micAction enum
            val showMicIcon = this[WidgetKeys.SHOW_MIC_ICON] ?: WidgetDefaults.SHOW_MIC_ICON
            if (showMicIcon) {
                WidgetDefaults.MIC_ACTION // Keep existing or default to DEFAULT_VOICE_SEARCH
            } else {
                OFF // If mic was previously hidden, use OFF
            }
        },
        textIconColorOverride = this[WidgetKeys.TEXT_ICON_COLOR_OVERRIDE]?.let { overrideString ->
            TextIconColorOverride.entries.find { it.value == overrideString }
        } ?: TextIconColorOverride.THEME,
        customButtons = customButtons
    ).coerceToValidRanges()
}

fun MutablePreferences.applyWidgetPreferences(config: QuickSearchWidgetPreferences) {
    val validated = config.coerceToValidRanges()
    this[WidgetKeys.BORDER_COLOR] = validated.borderColor
    this[WidgetKeys.BORDER_RADIUS] = validated.borderRadiusDp
    this[WidgetKeys.BORDER_WIDTH] = validated.borderWidthDp
    this[WidgetKeys.SHOW_LABEL] = validated.showLabel
    this[WidgetKeys.SEARCH_ICON_DISPLAY] = validated.searchIconDisplay.value
    this[WidgetKeys.SHOW_MIC_ICON] = validated.showMicIcon
    this[WidgetKeys.THEME] = validated.theme.value
    this[WidgetKeys.BACKGROUND_ALPHA] = validated.backgroundAlpha
    this[WidgetKeys.MIC_ACTION] = validated.micAction.value
    this[WidgetKeys.TEXT_ICON_COLOR_OVERRIDE] = validated.textIconColorOverride.value
    val customButtons = normalizeCustomButtons(validated.customButtons)
    customButtons.getOrNull(0)?.let { action ->
        this[WidgetKeys.CUSTOM_BUTTON_0] = action.toJson()
    } ?: remove(WidgetKeys.CUSTOM_BUTTON_0)
    customButtons.getOrNull(1)?.let { action ->
        this[WidgetKeys.CUSTOM_BUTTON_1] = action.toJson()
    } ?: remove(WidgetKeys.CUSTOM_BUTTON_1)
}
