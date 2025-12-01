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

private val DefaultBorderColor = Color(0xFFE2E5F0)

@Parcelize
data class QuickSearchWidgetPreferences(
    val borderColor: Int = DefaultBorderColor.toArgb(),
    val borderRadiusDp: Float = 24f,
    val borderWidthDp: Float = 1.5f,
    val showLabel: Boolean = true,
    val backgroundColorIsWhite: Boolean = true,
    val backgroundAlpha: Float = 1f
) : Parcelable {

    companion object {
        val Default = QuickSearchWidgetPreferences()
    }
}

private val KeyBorderColor = intPreferencesKey("quick_search_widget_border_color")
private val KeyBorderRadius = floatPreferencesKey("quick_search_widget_border_radius")
private val KeyBorderWidth = floatPreferencesKey("quick_search_widget_border_width")
private val KeyShowLabel = booleanPreferencesKey("quick_search_widget_show_label")
private val KeyBackgroundColorIsWhite = booleanPreferencesKey("quick_search_widget_background_color_is_white")
private val KeyBackgroundAlpha = floatPreferencesKey("quick_search_widget_background_alpha")

fun Preferences.toWidgetPreferences(): QuickSearchWidgetPreferences {
    val borderWidth = this[KeyBorderWidth] ?: 1.5f
    val borderRadius = this[KeyBorderRadius] ?: 24f
    val backgroundAlpha = this[KeyBackgroundAlpha] ?: 1f
    return QuickSearchWidgetPreferences(
        borderColor = this[KeyBorderColor] ?: DefaultBorderColor.toArgb(),
        borderRadiusDp = borderRadius.coerceIn(0f, 30f),
        borderWidthDp = borderWidth.coerceIn(0f, 4f),
        showLabel = this[KeyShowLabel] ?: true,
        backgroundColorIsWhite = this[KeyBackgroundColorIsWhite] ?: true,
        backgroundAlpha = backgroundAlpha.coerceIn(0f, 1f)
    )
}

fun MutablePreferences.applyWidgetPreferences(config: QuickSearchWidgetPreferences) {
    this[KeyBorderColor] = config.borderColor
    this[KeyBorderRadius] = config.borderRadiusDp
    this[KeyBorderWidth] = config.borderWidthDp
    this[KeyShowLabel] = config.showLabel
    this[KeyBackgroundColorIsWhite] = config.backgroundColorIsWhite
    this[KeyBackgroundAlpha] = config.backgroundAlpha
}

