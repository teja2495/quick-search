package com.tk.quicksearch.widgets.utils

import android.content.Context
import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.appNotifications.AppNotificationFilters
import com.tk.quicksearch.appNotifications.AppNotificationFiltersCodec
import com.tk.quicksearch.search.data.assets.ManagedAssetStore
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

/** Border color options. */
enum class BorderColorOption(
    val value: String,
) {
    DEVICE_THEME("device_theme"),
    WHITE("white"),
    BLACK("black"),
    CUSTOM("custom"),
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
    MEDIA_CONTROLS,
    NOTE,
    AT_A_GLANCE,
}

internal object WidgetButtonSlotConfig {
    const val STANDARD_COUNT = 2
    const val CUSTOM_ONLY_COUNT = 6
    const val MAX_COUNT = CUSTOM_ONLY_COUNT
}

internal object WidgetDefaults {
    val BORDER_COLOR = AppColors.WidgetBorder
    val BORDER_COLOR_ARGB = BORDER_COLOR.toArgb()
    val BORDER_COLOR_OPTION = BorderColorOption.WHITE
    const val BORDER_RADIUS_DP = 29f
    const val BORDER_WIDTH_DP = 0f
    const val SHOW_LABEL = true
    const val ICON_SIZE_SCALE = 1.15f
    const val TEXT_SCALE = 1f
    const val SHOW_EMPTY_DATE = true

    val SEARCH_ICON_DISPLAY = SearchIconDisplay.LEFT

    val THEME = WidgetTheme.LIGHT
    val BACKGROUND_COLOR: Int? = null
    const val BACKGROUND_ALPHA = 0.2f
    const val BORDER_ALPHA = BACKGROUND_ALPHA
    val MIC_ACTION = MicAction.DEFAULT_VOICE_SEARCH
    val TEXT_ICON_COLOR_OVERRIDE = TextIconColorOverride.WHITE
    const val INTERNAL_HORIZONTAL_PADDING_DP = 0f
    const val INTERNAL_VERTICAL_PADDING_DP = 0f
    const val USE_DEVICE_THEME_BACKGROUND = false
    val CUSTOM_BUTTONS: List<CustomWidgetButtonAction?> =
        List(WidgetButtonSlotConfig.STANDARD_COUNT) { null }
}

internal object WidgetRanges {
    const val BORDER_RADIUS_MIN = 0f
    const val BORDER_RADIUS_MAX = 40f
    const val BORDER_WIDTH_MIN = 0f
    const val BORDER_WIDTH_MAX = 4f
    const val BACKGROUND_ALPHA_MIN = 0f
    const val BACKGROUND_ALPHA_MAX = 1f
    const val INTERNAL_HORIZONTAL_PADDING_MIN = 0f
    const val INTERNAL_HORIZONTAL_PADDING_MAX = 16f
    const val INTERNAL_VERTICAL_PADDING_MIN = -12f
    const val INTERNAL_VERTICAL_PADDING_MAX = 12f
    const val ICON_SIZE_SCALE_MIN = 1f
    const val ICON_SIZE_SCALE_MAX = 1.4f
    const val TEXT_SCALE_MIN = 0.8f
    const val TEXT_SCALE_MAX = 1.5f
}

private object WidgetKeys {
    val BORDER_COLOR = intPreferencesKey("quick_search_widget_border_color")
    val BORDER_COLOR_OPTION = stringPreferencesKey("quick_search_widget_border_color_option")
    val BORDER_RADIUS = floatPreferencesKey("quick_search_widget_border_radius")
    val BORDER_WIDTH = floatPreferencesKey("quick_search_widget_border_width")
    val SHOW_LABEL = booleanPreferencesKey("quick_search_widget_show_label")
    val ICON_SIZE_SCALE = floatPreferencesKey("quick_search_widget_icon_size_scale")
    val TEXT_SCALE = floatPreferencesKey("quick_search_widget_text_scale")
    val SEARCH_ICON_DISPLAY = stringPreferencesKey("quick_search_widget_search_icon_display")
    val THEME = stringPreferencesKey("quick_search_widget_theme")
    val BACKGROUND_COLOR = intPreferencesKey("quick_search_widget_background_color")
    val BACKGROUND_ALPHA = floatPreferencesKey("quick_search_widget_background_alpha")
    val BORDER_ALPHA = floatPreferencesKey("quick_search_widget_border_alpha")
    val MIC_ACTION = stringPreferencesKey("quick_search_widget_mic_action")
    val TEXT_ICON_COLOR_OVERRIDE =
        stringPreferencesKey("quick_search_widget_text_icon_color_override")
    val CUSTOM_TEXT_ICON_COLOR = intPreferencesKey("quick_search_widget_custom_text_icon_color")
    val INTERNAL_HORIZONTAL_PADDING =
        floatPreferencesKey("quick_search_widget_internal_horizontal_padding")
    val INTERNAL_VERTICAL_PADDING =
        floatPreferencesKey("quick_search_widget_internal_vertical_padding")
    val USE_DEVICE_THEME_BACKGROUND =
        booleanPreferencesKey("quick_search_widget_use_device_theme_background")
    val USE_HOME_SCREEN_APPEARANCE =
        booleanPreferencesKey("quick_search_widget_use_home_screen_appearance")
    val CUSTOM_BUTTON_0 = stringPreferencesKey("quick_search_widget_custom_button_0")
    val CUSTOM_BUTTON_1 = stringPreferencesKey("quick_search_widget_custom_button_1")
    val CUSTOM_BUTTON_2 = stringPreferencesKey("quick_search_widget_custom_button_2")
    val CUSTOM_BUTTON_3 = stringPreferencesKey("quick_search_widget_custom_button_3")
    val CUSTOM_BUTTON_4 = stringPreferencesKey("quick_search_widget_custom_button_4")
    val CUSTOM_BUTTON_5 = stringPreferencesKey("quick_search_widget_custom_button_5")
    val NOTE_ID = longPreferencesKey("quick_search_widget_note_id")
    val GLANCE_ITEMS = stringPreferencesKey("quick_search_widget_glance_items")
    val GLANCE_APP_NOTIFICATION_FILTERS = stringPreferencesKey("quick_search_widget_glance_app_notification_filters")
    val SHOW_EMPTY_DATE = booleanPreferencesKey("quick_search_widget_show_empty_date")
}

/** Preferences for the Quick Search widget appearance and behavior. */
@Parcelize
data class WidgetPreferences(
    val borderColor: Int = WidgetDefaults.BORDER_COLOR_ARGB,
    val borderColorOption: BorderColorOption = WidgetDefaults.BORDER_COLOR_OPTION,
    val borderRadiusDp: Float = WidgetDefaults.BORDER_RADIUS_DP,
    val borderWidthDp: Float = WidgetDefaults.BORDER_WIDTH_DP,
    val showLabel: Boolean = WidgetDefaults.SHOW_LABEL,
    val iconSizeScale: Float = WidgetDefaults.ICON_SIZE_SCALE,
    /** Multiplies the At a Glance widget's text sizes. */
    val textScale: Float = WidgetDefaults.TEXT_SCALE,
    val searchIconDisplay: SearchIconDisplay = WidgetDefaults.SEARCH_ICON_DISPLAY,
    val theme: WidgetTheme = WidgetDefaults.THEME,
    val backgroundColor: Int? = WidgetDefaults.BACKGROUND_COLOR,
    val backgroundAlpha: Float = WidgetDefaults.BACKGROUND_ALPHA,
    val borderAlpha: Float = WidgetDefaults.BORDER_ALPHA,
    val micAction: MicAction = WidgetDefaults.MIC_ACTION,
    val textIconColorOverride: TextIconColorOverride = WidgetDefaults.TEXT_ICON_COLOR_OVERRIDE,
    /** Custom text and icon color (Media Controls widget only); overrides [textIconColorOverride]. */
    val customTextIconColor: Int? = null,
    val internalHorizontalPaddingDp: Float = WidgetDefaults.INTERNAL_HORIZONTAL_PADDING_DP,
    val internalVerticalPaddingDp: Float = WidgetDefaults.INTERNAL_VERTICAL_PADDING_DP,
    val useDeviceThemeBackground: Boolean = WidgetDefaults.USE_DEVICE_THEME_BACKGROUND,
    /** Keeps this widget's surface in sync with the selected Home appearance. */
    val useHomeScreenAppearance: Boolean = false,
    val customButtons: List<CustomWidgetButtonAction?> = WidgetDefaults.CUSTOM_BUTTONS,
    /** The note shown by the Note widget; null until one is picked. */
    val noteId: Long? = null,
    /** The At a Glance widget's own item toggles by key; items not in it follow the app's. */
    val glanceItems: Map<String, Boolean> = emptyMap(),
    /** The At a Glance widget's own App notifications apps and keywords; null to follow the app's. */
    val glanceAppNotificationFilters: AppNotificationFilters? = null,
    /** Whether the At a Glance widget shows today's date when it has no items, or stays blank. */
    val showEmptyDate: Boolean = WidgetDefaults.SHOW_EMPTY_DATE,
) : Parcelable {
    companion object {
        /** Default widget preferences instance. */
        val Default = WidgetPreferences()
    }

    val showSearchIcon: Boolean
        get() = searchIconDisplay != SearchIconDisplay.OFF

    val iconAlignLeft: Boolean
        get() = searchIconDisplay == SearchIconDisplay.LEFT

    val hasCustomButtons: Boolean
        get() = customButtons.any { it != null }

    fun coerceToValidRanges(): WidgetPreferences {
        val normalizedButtons = normalizeCustomButtons(customButtons, WidgetButtonSlotConfig.MAX_COUNT)
        val hasCustomButtons = normalizedButtons.any { it != null }
        return copy(
            borderRadiusDp =
                borderRadiusDp.coerceFiniteIn(
                    WidgetRanges.BORDER_RADIUS_MIN,
                    WidgetRanges.BORDER_RADIUS_MAX,
                ),
            borderWidthDp =
                borderWidthDp.coerceFiniteIn(
                    WidgetRanges.BORDER_WIDTH_MIN,
                    WidgetRanges.BORDER_WIDTH_MAX,
                ),
            backgroundAlpha =
                backgroundAlpha.coerceFiniteIn(
                    WidgetRanges.BACKGROUND_ALPHA_MIN,
                    WidgetRanges.BACKGROUND_ALPHA_MAX,
                ),
            borderAlpha =
                borderAlpha.coerceFiniteIn(
                    WidgetRanges.BACKGROUND_ALPHA_MIN,
                    WidgetRanges.BACKGROUND_ALPHA_MAX,
                ),
            iconSizeScale =
                iconSizeScale.coerceFiniteIn(
                    WidgetRanges.ICON_SIZE_SCALE_MIN,
                    WidgetRanges.ICON_SIZE_SCALE_MAX,
                ),
            textScale = textScale.coerceFiniteIn(WidgetRanges.TEXT_SCALE_MIN, WidgetRanges.TEXT_SCALE_MAX),
            internalHorizontalPaddingDp =
                internalHorizontalPaddingDp.coerceFiniteIn(
                    WidgetRanges.INTERNAL_HORIZONTAL_PADDING_MIN,
                    WidgetRanges.INTERNAL_HORIZONTAL_PADDING_MAX,
                ),
            internalVerticalPaddingDp =
                internalVerticalPaddingDp.coerceFiniteIn(
                    WidgetRanges.INTERNAL_VERTICAL_PADDING_MIN,
                    WidgetRanges.INTERNAL_VERTICAL_PADDING_MAX,
                ),
            customButtons = normalizedButtons,
            showLabel = if (hasCustomButtons) false else showLabel,
            searchIconDisplay =
                if (hasCustomButtons) {
                    SearchIconDisplay.LEFT
                } else {
                    searchIconDisplay
                },
        )
    }
}

private fun Float.coerceFiniteIn(
    min: Float,
    max: Float,
): Float = if (isFinite()) coerceIn(min, max) else min

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

/** Whether the configure screen has saved this widget's look; refreshes add other keys, so it checks one it always writes. */
fun Preferences.hasStoredWidgetConfig(): Boolean = this[WidgetKeys.THEME] != null

fun Preferences.toWidgetPreferences(context: Context): WidgetPreferences {
    val theme =
        this[WidgetKeys.THEME]?.let { themeString ->
            WidgetTheme.entries.find { it.value == themeString }
        } ?: WidgetDefaults.THEME

    val customButtons =
        listOf(
            customWidgetActionFromManagedJson(context, this[WidgetKeys.CUSTOM_BUTTON_0]),
            customWidgetActionFromManagedJson(context, this[WidgetKeys.CUSTOM_BUTTON_1]),
            customWidgetActionFromManagedJson(context, this[WidgetKeys.CUSTOM_BUTTON_2]),
            customWidgetActionFromManagedJson(context, this[WidgetKeys.CUSTOM_BUTTON_3]),
            customWidgetActionFromManagedJson(context, this[WidgetKeys.CUSTOM_BUTTON_4]),
            customWidgetActionFromManagedJson(context, this[WidgetKeys.CUSTOM_BUTTON_5]),
        )

    return WidgetPreferences(
        borderColor = this[WidgetKeys.BORDER_COLOR] ?: WidgetDefaults.BORDER_COLOR_ARGB,
        borderColorOption =
            this[WidgetKeys.BORDER_COLOR_OPTION]?.let { optionString ->
                BorderColorOption.entries.find { it.value == optionString }
            } ?: WidgetDefaults.BORDER_COLOR_OPTION,
        borderRadiusDp =
            this[WidgetKeys.BORDER_RADIUS]
                ?: WidgetDefaults.BORDER_RADIUS_DP,
        borderWidthDp = this[WidgetKeys.BORDER_WIDTH] ?: WidgetDefaults.BORDER_WIDTH_DP,
        showLabel = this[WidgetKeys.SHOW_LABEL] ?: WidgetDefaults.SHOW_LABEL,
        iconSizeScale = this[WidgetKeys.ICON_SIZE_SCALE] ?: WidgetDefaults.ICON_SIZE_SCALE,
        textScale = this[WidgetKeys.TEXT_SCALE] ?: WidgetDefaults.TEXT_SCALE,
        searchIconDisplay =
            this[WidgetKeys.SEARCH_ICON_DISPLAY]?.let { displayString ->
                SearchIconDisplay.entries.find { it.value == displayString }
            } ?: WidgetDefaults.SEARCH_ICON_DISPLAY,
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
            } ?: WidgetDefaults.MIC_ACTION,
        textIconColorOverride =
            this[WidgetKeys.TEXT_ICON_COLOR_OVERRIDE]?.let { overrideString ->
                TextIconColorOverride.entries.find { it.value == overrideString }
            }
                ?: WidgetDefaults.TEXT_ICON_COLOR_OVERRIDE,
        customTextIconColor = this[WidgetKeys.CUSTOM_TEXT_ICON_COLOR],
        internalHorizontalPaddingDp =
            this[WidgetKeys.INTERNAL_HORIZONTAL_PADDING]
                ?: WidgetDefaults.INTERNAL_HORIZONTAL_PADDING_DP,
        internalVerticalPaddingDp =
            this[WidgetKeys.INTERNAL_VERTICAL_PADDING]
                ?: WidgetDefaults.INTERNAL_VERTICAL_PADDING_DP,
        useDeviceThemeBackground =
            this[WidgetKeys.USE_DEVICE_THEME_BACKGROUND]
                ?: WidgetDefaults.USE_DEVICE_THEME_BACKGROUND,
        useHomeScreenAppearance = this[WidgetKeys.USE_HOME_SCREEN_APPEARANCE] ?: false,
        customButtons = customButtons,
        noteId = this[WidgetKeys.NOTE_ID],
        glanceItems = decodeGlanceItems(this[WidgetKeys.GLANCE_ITEMS]),
        glanceAppNotificationFilters = this[WidgetKeys.GLANCE_APP_NOTIFICATION_FILTERS]?.let(AppNotificationFiltersCodec::decode),
        showEmptyDate = this[WidgetKeys.SHOW_EMPTY_DATE] ?: WidgetDefaults.SHOW_EMPTY_DATE,
    ).coerceToValidRanges()
}

fun MutablePreferences.applyWidgetPreferences(
    config: WidgetPreferences,
    context: Context,
) {
    val validated = config.coerceToValidRanges()
    this[WidgetKeys.BORDER_COLOR] = validated.borderColor
    this[WidgetKeys.BORDER_COLOR_OPTION] = validated.borderColorOption.value
    this[WidgetKeys.BORDER_RADIUS] = validated.borderRadiusDp
    this[WidgetKeys.BORDER_WIDTH] = validated.borderWidthDp
    this[WidgetKeys.SHOW_LABEL] = validated.showLabel
    this[WidgetKeys.ICON_SIZE_SCALE] = validated.iconSizeScale
    this[WidgetKeys.TEXT_SCALE] = validated.textScale
    this[WidgetKeys.SEARCH_ICON_DISPLAY] = validated.searchIconDisplay.value
    this[WidgetKeys.THEME] = validated.theme.value
    validated.backgroundColor?.let { this[WidgetKeys.BACKGROUND_COLOR] = it }
        ?: remove(WidgetKeys.BACKGROUND_COLOR)
    this[WidgetKeys.BACKGROUND_ALPHA] = validated.backgroundAlpha
    this[WidgetKeys.BORDER_ALPHA] = validated.borderAlpha
    this[WidgetKeys.MIC_ACTION] = validated.micAction.value
    this[WidgetKeys.TEXT_ICON_COLOR_OVERRIDE] = validated.textIconColorOverride.value
    validated.customTextIconColor?.let { this[WidgetKeys.CUSTOM_TEXT_ICON_COLOR] = it }
        ?: remove(WidgetKeys.CUSTOM_TEXT_ICON_COLOR)
    this[WidgetKeys.INTERNAL_HORIZONTAL_PADDING] = validated.internalHorizontalPaddingDp
    this[WidgetKeys.INTERNAL_VERTICAL_PADDING] = validated.internalVerticalPaddingDp
    this[WidgetKeys.USE_DEVICE_THEME_BACKGROUND] = validated.useDeviceThemeBackground
    this[WidgetKeys.USE_HOME_SCREEN_APPEARANCE] = validated.useHomeScreenAppearance
    val customButtons = normalizeCustomButtons(validated.customButtons, WidgetButtonSlotConfig.MAX_COUNT)
    customButtons.getOrNull(0)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_0] = action.toManagedJson(context) }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_0)
    customButtons.getOrNull(1)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_1] = action.toManagedJson(context) }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_1)
    customButtons.getOrNull(2)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_2] = action.toManagedJson(context) }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_2)
    customButtons.getOrNull(3)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_3] = action.toManagedJson(context) }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_3)
    customButtons.getOrNull(4)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_4] = action.toManagedJson(context) }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_4)
    customButtons.getOrNull(5)?.let { action -> this[WidgetKeys.CUSTOM_BUTTON_5] = action.toManagedJson(context) }
        ?: remove(WidgetKeys.CUSTOM_BUTTON_5)
    validated.noteId?.let { this[WidgetKeys.NOTE_ID] = it } ?: remove(WidgetKeys.NOTE_ID)
    if (validated.glanceItems.isEmpty()) {
        remove(WidgetKeys.GLANCE_ITEMS)
    } else {
        this[WidgetKeys.GLANCE_ITEMS] = org.json.JSONObject(validated.glanceItems).toString()
    }
    validated.glanceAppNotificationFilters?.let { this[WidgetKeys.GLANCE_APP_NOTIFICATION_FILTERS] = AppNotificationFiltersCodec.encode(it) }
        ?: remove(WidgetKeys.GLANCE_APP_NOTIFICATION_FILTERS)
    this[WidgetKeys.SHOW_EMPTY_DATE] = validated.showEmptyDate
}

private fun decodeGlanceItems(raw: String?): Map<String, Boolean> {
    if (raw.isNullOrBlank()) return emptyMap()
    val json = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return emptyMap()
    return json.keys().asSequence().associateWith { json.optBoolean(it, true) }
}

private fun CustomWidgetButtonAction.toManagedJson(context: Context): String {
    val json = org.json.JSONObject(toJson())
    val store = ManagedAssetStore(context)
    migrateWidgetIcon(json, "customIconBase64", "customIconAssetId", store)
    migrateWidgetIcon(json, "iconBase64", "iconAssetId", store)
    return json.toString()
}

private fun customWidgetActionFromManagedJson(
    context: Context,
    raw: String?,
): CustomWidgetButtonAction? {
    if (raw.isNullOrBlank()) return null
    val json = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return null
    val store = ManagedAssetStore(context)
    restoreWidgetIcon(json, "customIconBase64", "customIconAssetId", store)
    restoreWidgetIcon(json, "iconBase64", "iconAssetId", store)
    return CustomWidgetButtonAction.fromJson(json.toString())
}

private fun migrateWidgetIcon(
    json: org.json.JSONObject,
    inlineKey: String,
    assetKey: String,
    store: ManagedAssetStore,
) {
    val encoded = json.optString(inlineKey).takeIf { it.isNotBlank() && it != "null" } ?: return
    val digest = java.security.MessageDigest.getInstance("SHA-256")
        .digest(encoded.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    val logicalId = "${ManagedAssetStore.WIDGET_ICON_PREFIX}$digest"
    if (store.putBase64(logicalId, encoded)) {
        json.remove(inlineKey)
        json.put(assetKey, logicalId)
    }
}

private fun restoreWidgetIcon(
    json: org.json.JSONObject,
    inlineKey: String,
    assetKey: String,
    store: ManagedAssetStore,
) {
    if (json.optString(inlineKey).isNotBlank()) return
    val logicalId = json.optString(assetKey).takeIf { it.isNotBlank() } ?: return
    store.getBase64(logicalId)?.let { json.put(inlineKey, it) }
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
                micAction = OFF,
                customButtons =
                    normalizeCustomButtons(
                        normalized.customButtons,
                        WidgetButtonSlotConfig.CUSTOM_ONLY_COUNT,
                    ),
            )
        // Only the background (light, dark or a custom color), text and icon color (white, black
        // or a custom color), corner radius and background transparency are configurable.
        WidgetVariant.MEDIA_CONTROLS ->
            normalized.copy(
                theme = if (normalized.theme == WidgetTheme.LIGHT) WidgetTheme.LIGHT else WidgetTheme.DARK,
                useDeviceThemeBackground = false,
                useHomeScreenAppearance = false,
                textIconColorOverride =
                    if (normalized.textIconColorOverride == TextIconColorOverride.BLACK) {
                        TextIconColorOverride.BLACK
                    } else {
                        TextIconColorOverride.WHITE
                    },
                borderWidthDp = 0f,
                showLabel = false,
                searchIconDisplay = SearchIconDisplay.OFF,
                micAction = OFF,
                internalHorizontalPaddingDp = 0f,
                internalVerticalPaddingDp = 0f,
                customButtons = emptyList(),
            )
        // The note, background (system, light, dark or a custom color), text color (automatic,
        // white or black), corner radius and background transparency are configurable. At a
        // Glance offers the same, without a note.
        WidgetVariant.NOTE,
        WidgetVariant.AT_A_GLANCE,
        ->
            normalized.copy(
                useDeviceThemeBackground = false,
                useHomeScreenAppearance = false,
                customTextIconColor = null,
                borderWidthDp = 0f,
                showLabel = false,
                searchIconDisplay = SearchIconDisplay.OFF,
                micAction = OFF,
                internalHorizontalPaddingDp = 0f,
                internalVerticalPaddingDp = 0f,
                customButtons = emptyList(),
            )
    }
}
