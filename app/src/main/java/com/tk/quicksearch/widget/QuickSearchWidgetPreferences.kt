package com.tk.quicksearch.widget

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tk.quicksearch.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuickSearchWidgetPreferences(
    val presetId: String = WidgetColorPreset.default.id,
    val backgroundColor: Int = WidgetColorPreset.default.backgroundArgb,
    val borderColor: Int = WidgetColorPreset.default.borderArgb,
    val borderRadiusDp: Float = 24f,
    val borderWidthDp: Float = 1.5f,
    val backgroundAlpha: Float = 0.92f,
    val showLabel: Boolean = true
) : Parcelable {

    fun withPreset(preset: WidgetColorPreset): QuickSearchWidgetPreferences {
        return copy(
            presetId = preset.id,
            backgroundColor = preset.backgroundArgb,
            borderColor = preset.borderArgb
        )
    }

    companion object {
        val Default = QuickSearchWidgetPreferences()
    }
}

@Immutable
data class WidgetColorPreset(
    val id: String,
    @StringRes val labelRes: Int,
    val backgroundColor: Color,
    val borderColor: Color,
    val category: WidgetColorCategory
) {
    val backgroundArgb: Int = backgroundColor.toArgb()
    val borderArgb: Int = borderColor.toArgb()

    companion object {
        val default = WidgetColorPreset(
            id = "dawn",
            labelRes = R.string.widget_preset_dawn,
            backgroundColor = Color(0xFFF5F6FB),
            borderColor = Color(0xFFE2E5F0),
            category = WidgetColorCategory.LIGHT
        )
    }
}

enum class WidgetColorCategory {
    LIGHT,
    DARK
}

object WidgetColorPresets {
    private val lightPresets = listOf(
        WidgetColorPreset.default,
        WidgetColorPreset(
            id = "canvas",
            labelRes = R.string.widget_preset_canvas,
            backgroundColor = Color(0xFFFDF4EB),
            borderColor = Color(0xFFEED9C8),
            category = WidgetColorCategory.LIGHT
        ),
        WidgetColorPreset(
            id = "sunrise",
            labelRes = R.string.widget_preset_sunrise,
            backgroundColor = Color(0xFFF6E5DC),
            borderColor = Color(0xFFE3C7B4),
            category = WidgetColorCategory.LIGHT
        )
    )

    private val darkPresets = listOf(
        WidgetColorPreset(
            id = "ember",
            labelRes = R.string.widget_preset_ember,
            backgroundColor = Color(0xFF2B1F22),
            borderColor = Color(0xFF4E353A),
            category = WidgetColorCategory.DARK
        ),
        WidgetColorPreset(
            id = "midnight",
            labelRes = R.string.widget_preset_midnight,
            backgroundColor = Color(0xFF101924),
            borderColor = Color(0xFF283446),
            category = WidgetColorCategory.DARK
        ),
        WidgetColorPreset(
            id = "void",
            labelRes = R.string.widget_preset_void,
            backgroundColor = Color(0xFF1B1B1B),
            borderColor = Color(0xFF383838),
            category = WidgetColorCategory.DARK
        )
    )

    val all: List<WidgetColorPreset> = lightPresets + darkPresets

    val light: List<WidgetColorPreset> = lightPresets
    val dark: List<WidgetColorPreset> = darkPresets

    fun findById(id: String): WidgetColorPreset? = all.firstOrNull { it.id == id }
}

private val KeyBackgroundColor = intPreferencesKey("quick_search_widget_background_color")
private val KeyBorderColor = intPreferencesKey("quick_search_widget_border_color")
private val KeyBorderRadius = floatPreferencesKey("quick_search_widget_border_radius")
private val KeyBorderWidth = floatPreferencesKey("quick_search_widget_border_width")
private val KeyBackgroundAlpha = floatPreferencesKey("quick_search_widget_background_alpha")
private val KeyShowLabel = booleanPreferencesKey("quick_search_widget_show_label")
private val KeyPresetId = stringPreferencesKey("quick_search_widget_preset_id")

fun Preferences.toWidgetPreferences(): QuickSearchWidgetPreferences {
    val presetId = this[KeyPresetId] ?: WidgetColorPreset.default.id
    return QuickSearchWidgetPreferences(
        presetId = presetId,
        backgroundColor = this[KeyBackgroundColor] ?: WidgetColorPreset.default.backgroundArgb,
        borderColor = this[KeyBorderColor] ?: WidgetColorPreset.default.borderArgb,
        borderRadiusDp = this[KeyBorderRadius] ?: 24f,
        borderWidthDp = this[KeyBorderWidth] ?: 1.5f,
        backgroundAlpha = this[KeyBackgroundAlpha] ?: 0.92f,
        showLabel = this[KeyShowLabel] ?: true
    )
}

fun MutablePreferences.applyWidgetPreferences(config: QuickSearchWidgetPreferences) {
    this[KeyBackgroundColor] = config.backgroundColor
    this[KeyBorderColor] = config.borderColor
    this[KeyBorderRadius] = config.borderRadiusDp
    this[KeyBorderWidth] = config.borderWidthDp
    this[KeyBackgroundAlpha] = config.backgroundAlpha
    this[KeyShowLabel] = config.showLabel
    this[KeyPresetId] = config.presetId
}

