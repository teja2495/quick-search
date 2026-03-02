package com.tk.quicksearch.widgets.utils

import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.searchWidget.MicAction.OFF
import kotlinx.parcelize.Parcelize

/** Theme options for the widget appearance. */
enum class WidgetTheme(
    val value: String,
) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"),
}

/** Text/Icon color override options. */
enum class TextIconColorOverride(
    val value: String,
) {
    WHITE("white"),
    BLACK("black"),
    THEME("theme"),
}

/** Search icon display options. */
enum class SearchIconDisplay(
    val value: String,
) {
    LEFT("left"),
    CENTER("center"),
    OFF("off"),
}

enum class WidgetVariant {
    STANDARD,
    CUSTOM_BUTTONS_ONLY,
}

internal object WidgetButtonSlotConfig {
    const val STANDARD_COUNT = 2
    const val CUSTOM_ONLY_COUNT = 6
    const val MAX_COUNT = CUSTOM_ONLY_COUNT
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
    val BACKGROUND_COLOR: Int? = null
    const val BACKGROUND_ALPHA = 0.35f
    const val BORDER_ALPHA = BACKGROUND_ALPHA
    val MIC_ACTION = MicAction.DEFAULT_VOICE_SEARCH
    val CUSTOM_BUTTONS: List<CustomWidgetButtonAction?> =
        List(WidgetButtonSlotConfig.STANDARD_COUNT) { null }
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
    val BACKGROUND_COLOR = intPreferencesKey("quick_search_widget_background_color")
    val BACKGROUND_ALPHA = floatPreferencesKey("quick_search_widget_background_alpha")
    val BORDER_ALPHA = floatPreferencesKey("quick_search_widget_border_alpha")
    val MIC_ACTION = stringPreferencesKey("quick_search_widget_mic_action")
    val TEXT_ICON_COLOR_OVERRIDE =
        stringPreferencesKey("quick_search_widget_text_icon_color_override")
    val CUSTOM_BUTTON_0 = stringPreferencesKey("quick_search_widget_custom_button_0")
    val CUSTOM_BUTTON_1 = stringPreferencesKey("quick_search_widget_custom_button_1")
    val CUSTOM_BUTTON_2 = stringPreferencesKey("quick_search_widget_custom_button_2")
    val CUSTOM_BUTTON_3 = stringPreferencesKey("quick_search_widget_custom_button_3")
    val CUSTOM_BUTTON_4 = stringPreferencesKey("quick_search_widget_custom_button_4")
    val CUSTOM_BUTTON_5 = stringPreferencesKey("quick_search_widget_custom_button_5")

    // Legacy keys for migration
    val SHOW_SEARCH_ICON = booleanPreferencesKey("quick_search_widget_show_search_icon")
    val ICON_ALIGN_LEFT = booleanPreferencesKey("quick_search_widget_icon_align_left")
    val BACKGROUND_COLOR_IS_WHITE =
        booleanPreferencesKey("quick_search_widget_background_color_is_white")
    val TEXT_ICON_COLOR_IS_WHITE =
        booleanPreferencesKey("quick_search_widget_text_icon_color_is_white")
}

/** Preferences for the Quick Search widget appearance and behavior. */
@Parcelize
data class WidgetPreferences(
    val borderColor: Int = WidgetDefaults.BORDER_COLOR_ARGB,
    val borderRadiusDp: Float = WidgetDefaults.BORDER_RADIUS_DP,
    val borderWidthDp: Float = WidgetDefaults.BORDER_WIDTH_DP,
    val showLabel: Boolean = WidgetDefaults.SHOW_LABEL,
    val searchIconDisplay: SearchIconDisplay = WidgetDefaults.SEARCH_ICON_DISPLAY,
    val showMicIcon: Boolean = WidgetDefaults.SHOW_MIC_ICON,
    val theme: WidgetTheme = WidgetDefaults.THEME,
    val backgroundColor: Int? = WidgetDefaults.BACKGROUND_COLOR,
    val backgroundAlpha: Float = WidgetDefaults.BACKGROUND_ALPHA,
    val borderAlpha: Float = WidgetDefaults.BORDER_ALPHA,
    val micAction: MicAction = WidgetDefaults.MIC_ACTION,
    val textIconColorOverride: TextIconColorOverride = TextIconColorOverride.THEME,
    val customButtons: List<CustomWidgetButtonAction?> = WidgetDefaults.CUSTOM_BUTTONS,
) : Parcelable {
    companion object {
        /** Default widget preferences instance. */
        val Default = WidgetPreferences()
    }

    // Backward compatibility properties
    val showSearchIcon: Boolean
        get() = searchIconDisplay != SearchIconDisplay.OFF

    val iconAlignLeft: Boolean
        get() = searchIconDisplay == SearchIconDisplay.LEFT

    val hasCustomButtons: Boolean
        get() = customButtons.any { it != null }

    fun coerceToValidRanges(): WidgetPreferences {
        val normalizedButtons = normalizeCustomButtons(customButtons, WidgetButtonSlotConfig.MAX_COUNT)
        val shouldHideLabel = normalizedButtons.any { it != null }
        return copy(
            borderRadiusDp =
                borderRadiusDp.coerceIn(
                    WidgetRanges.BORDER_RADIUS_MIN,
                    WidgetRanges.BORDER_RADIUS_MAX,
                ),
            borderWidthDp =
                borderWidthDp.coerceIn(
                    WidgetRanges.BORDER_WIDTH_MIN,
                    WidgetRanges.BORDER_WIDTH_MAX,
                ),
            backgroundAlpha =
                backgroundAlpha.coerceIn(
                    WidgetRanges.BACKGROUND_ALPHA_MIN,
                    WidgetRanges.BACKGROUND_ALPHA_MAX,
                ),
            borderAlpha =
                borderAlpha.coerceIn(
                    WidgetRanges.BACKGROUND_ALPHA_MIN,
                    WidgetRanges.BACKGROUND_ALPHA_MAX,
                ),
            customButtons = normalizedButtons,
            showLabel = if (shouldHideLabel) false else showLabel,
            searchIconDisplay =
                if (shouldHideLabel && searchIconDisplay == SearchIconDisplay.CENTER) {
                    SearchIconDisplay.LEFT
                } else {
                    searchIconDisplay
                },
        )
    }
}

private fun normalizeCustomButtons(
    buttons: List<CustomWidgetButtonAction?>,
    maxSlots: Int,
): List<CustomWidgetButtonAction?> {
    val normalized = buttons.take(maxSlots).toMutableList()
    while (normalized.size < maxSlots) {
        normalized.add(null)
    }
    return normalized
}

fun Preferences.toWidgetPreferences(): WidgetPreferences {
    // Handle theme migration from legacy separate color preferences
    val theme =
        this[WidgetKeys.THEME]?.let { themeString ->
            WidgetTheme.entries.find { it.value == themeString }
        }
            ?: run {
                // Migration logic: convert old separate color preferences to theme
                val backgroundIsWhite =
                    this[WidgetKeys.BACKGROUND_COLOR_IS_WHITE]
                        ?: false // Default to dark (was the old default)
                val textIconIsWhite =
                    this[WidgetKeys.TEXT_ICON_COLOR_IS_WHITE] ?: !backgroundIsWhite

                when {
                    backgroundIsWhite && !textIconIsWhite -> WidgetTheme.LIGHT
                    !backgroundIsWhite && textIconIsWhite -> WidgetTheme.DARK
                    else -> WidgetDefaults.THEME
                }
            }

    val customButtons =
        listOf(
            CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_0]),
            CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_1]),
            CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_2]),
            CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_3]),
            CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_4]),
            CustomWidgetButtonAction.fromJson(this[WidgetKeys.CUSTOM_BUTTON_5]),
        )

    return WidgetPreferences(
        borderColor = this[WidgetKeys.BORDER_COLOR] ?: WidgetDefaults.BORDER_COLOR_ARGB,
        borderRadiusDp =
            this[WidgetKeys.BORDER_RADIUS]
                ?: WidgetDefaults.BORDER_RADIUS_DP,
        borderWidthDp = this[WidgetKeys.BORDER_WIDTH] ?: WidgetDefaults.BORDER_WIDTH_DP,
        showLabel = this[WidgetKeys.SHOW_LABEL] ?: WidgetDefaults.SHOW_LABEL,
        searchIconDisplay =
            this[WidgetKeys.SEARCH_ICON_DISPLAY]?.let { displayString ->
                SearchIconDisplay.entries.find { it.value == displayString }
            }
                ?: run {
                    // Migration logic: convert old separate boolean preferences
                    // to new enum
                    val showSearchIcon =
                        this[WidgetKeys.SHOW_SEARCH_ICON]
                            ?: true // Default to true for backward
                    // compatibility
                    val iconAlignLeft =
                        this[WidgetKeys.ICON_ALIGN_LEFT]
                            ?: true // Default to true for backward
                    // compatibility

                    when {
                        !showSearchIcon -> SearchIconDisplay.OFF
                        iconAlignLeft -> SearchIconDisplay.LEFT
                        else -> SearchIconDisplay.CENTER
                    }
                },
        showMicIcon = true, // Always true now, mic visibility controlled by micAction
        theme = theme,
        backgroundColor = this[WidgetKeys.BACKGROUND_COLOR],
        backgroundAlpha =
            this[WidgetKeys.BACKGROUND_ALPHA]
                ?: WidgetDefaults.BACKGROUND_ALPHA,
        borderAlpha =
            this[WidgetKeys.BORDER_ALPHA]
                ?: (this[WidgetKeys.BACKGROUND_ALPHA] ?: WidgetDefaults.BORDER_ALPHA),
        micAction =
            this[WidgetKeys.MIC_ACTION]?.let { actionString ->
                MicAction.entries.find { it.value == actionString }
            }
                ?: run {
                    // Migration logic: convert old showMicIcon boolean to new
                    // micAction enum
                    val showMicIcon =
                        this[WidgetKeys.SHOW_MIC_ICON]
                            ?: WidgetDefaults.SHOW_MIC_ICON
                    if (showMicIcon) {
                        WidgetDefaults.MIC_ACTION // Keep existing or default to
                        // DEFAULT_VOICE_SEARCH
                    } else {
                        OFF // If mic was previously hidden, use OFF
                    }
                },
        textIconColorOverride =
            this[WidgetKeys.TEXT_ICON_COLOR_OVERRIDE]?.let { overrideString ->
                TextIconColorOverride.entries.find { it.value == overrideString }
            }
                ?: TextIconColorOverride.THEME,
        customButtons = customButtons,
    ).coerceToValidRanges()
}

fun MutablePreferences.applyWidgetPreferences(config: WidgetPreferences) {
    val validated = config.coerceToValidRanges()
    this[WidgetKeys.BORDER_COLOR] = validated.borderColor
    this[WidgetKeys.BORDER_RADIUS] = validated.borderRadiusDp
    this[WidgetKeys.BORDER_WIDTH] = validated.borderWidthDp
    this[WidgetKeys.SHOW_LABEL] = validated.showLabel
    this[WidgetKeys.SEARCH_ICON_DISPLAY] = validated.searchIconDisplay.value
    this[WidgetKeys.SHOW_MIC_ICON] = validated.showMicIcon
    this[WidgetKeys.THEME] = validated.theme.value
    validated.backgroundColor?.let { this[WidgetKeys.BACKGROUND_COLOR] = it }
        ?: remove(WidgetKeys.BACKGROUND_COLOR)
    this[WidgetKeys.BACKGROUND_ALPHA] = validated.backgroundAlpha
    this[WidgetKeys.BORDER_ALPHA] = validated.borderAlpha
    this[WidgetKeys.MIC_ACTION] = validated.micAction.value
    this[WidgetKeys.TEXT_ICON_COLOR_OVERRIDE] = validated.textIconColorOverride.value
    val customButtons = normalizeCustomButtons(validated.customButtons, WidgetButtonSlotConfig.MAX_COUNT)
    customButtons.getOrNull(0)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_0] = action.toJson() }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_0)
    customButtons.getOrNull(1)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_1] = action.toJson() }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_1)
    customButtons.getOrNull(2)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_2] = action.toJson() }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_2)
    customButtons.getOrNull(3)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_3] = action.toJson() }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_3)
    customButtons.getOrNull(4)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_4] = action.toJson() }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_4)
    customButtons.getOrNull(5)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_5] = action.toJson() }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_5)
}

fun WidgetPreferences.enforceVariantConstraints(variant: WidgetVariant): WidgetPreferences {
    val normalized = coerceToValidRanges()
    return when (variant) {
        WidgetVariant.STANDARD ->
            normalized.copy(
                customButtons =
                    normalizeCustomButtons(
                        normalized.customButtons,
                        WidgetButtonSlotConfig.STANDARD_COUNT,
                    ),
            )
        WidgetVariant.CUSTOM_BUTTONS_ONLY ->
            normalized.copy(
                showLabel = false,
                searchIconDisplay = SearchIconDisplay.OFF,
                showMicIcon = false,
                micAction = OFF,
                customButtons =
                    normalizeCustomButtons(
                        normalized.customButtons,
                        WidgetButtonSlotConfig.CUSTOM_ONLY_COUNT,
                    ),
            )
    }
}
