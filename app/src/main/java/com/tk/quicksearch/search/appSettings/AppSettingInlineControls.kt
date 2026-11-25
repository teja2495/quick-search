package com.tk.quicksearch.search.appSettings

import androidx.compose.foundation.layout.heightIn
import com.tk.quicksearch.shared.ui.components.SliderWithValue
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.shared.ui.components.AppSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.HomeTextColor
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.settings.appearanceSettings.AccentColorChoices
import com.tk.quicksearch.settings.appearanceSettings.AppIconCornerSlider
import com.tk.quicksearch.settings.appearanceSettings.FONT_SIZE_STEPS
import com.tk.quicksearch.settings.searchEngineSettings.SearchEngineCompactRowChoices
import com.tk.quicksearch.settings.searchEngineSettings.SearchEngineStyleChoices
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val THEME_MODE_SETTING_ID = "app_settings_theme_mode"
internal const val TOP_MATCHES_COUNT_SETTING_ID = "app_settings_top_matches_count"

/** State and callbacks for app-setting rows that edit their value inline instead of toggling. */
data class AppSettingInlineControls(
    val fontScaleMultiplier: Float = UiPreferences.DEFAULT_FONT_SCALE_MULTIPLIER,
    val onFontScaleMultiplierChange: (Float) -> Unit = {},
    val appIconSizeStep: Int = UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
    val onAppIconSizeStepChange: (Int) -> Unit = {},
    val homeTextColorOverride: HomeTextColor? = null,
    val onHomeTextColorChange: (HomeTextColor) -> Unit = {},
    val backgroundSource: BackgroundSource = BackgroundSource.THEME,
    val customImageUri: String? = null,
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val onAppThemeModeChange: (AppThemeMode) -> Unit = {},
    val topMatchesLimit: Int = UiPreferences.DEFAULT_TOP_MATCHES_LIMIT,
    val onTopMatchesLimitChange: (Int) -> Unit = {},
    val hasEnabledSearchEngines: Boolean = false,
    val searchEngineCompactMode: Boolean = false,
    val onSearchEngineCompactModeChange: (Boolean) -> Unit = {},
    val searchEngineCompactRowCount: Int = 1,
    val onSearchEngineCompactRowCountChange: (Int) -> Unit = {},
    val recentQueriesDisplayCount: Int = UiPreferences.DEFAULT_RECENT_QUERIES_DISPLAY_COUNT,
    val onRecentQueriesDisplayCountChange: (Int) -> Unit = {},
    val notificationCountsEnabled: Boolean = false,
    val onNotificationCountsEnabledChange: (Boolean) -> Unit = {},
    val onAppIconCornerPercentChange: (Int) -> Unit = {},
    val appTheme: AppTheme = AppTheme.MONOCHROME,
    val deviceThemeEnabled: Boolean = false,
    val wallpaperAccentAvailable: Boolean = false,
    val accentColorMode: AccentColorMode = AccentColorMode.NONE,
    val customAccentColorArgb: Int = UiPreferences.DEFAULT_CUSTOM_ACCENT_COLOR_ARGB,
    val onAccentColorModeChange: (AccentColorMode) -> Unit = {},
    val onCustomAccentColorChange: (Int) -> Unit = {},
    val aiModel: AiModelInlineState = AiModelInlineState(),
)

val LocalAppSettingInlineControls = compositionLocalOf { AppSettingInlineControls() }

internal val AppSettingResult.hasInlineControl: Boolean
    get() =
        toggleKey == AppSettingsToggleKey.FONT_SIZE ||
            toggleKey == AppSettingsToggleKey.APP_ICON_SIZE ||
            toggleKey == AppSettingsToggleKey.HOME_TEXT_COLOR ||
            toggleKey == AppSettingsToggleKey.ACCENT_COLOR ||
            toggleKey == AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE ||
            id == THEME_MODE_SETTING_ID ||
            id == TOP_MATCHES_COUNT_SETTING_ID ||
            destination == AppSettingsRouteAction.AI_MODEL

@Composable
internal fun AppSettingInlineControlContent(setting: AppSettingResult) {
    val controls = LocalAppSettingInlineControls.current
    when {
        setting.toggleKey == AppSettingsToggleKey.FONT_SIZE ->
            FontSizeInlineSlider(controls.fontScaleMultiplier, controls.onFontScaleMultiplierChange)
        setting.toggleKey == AppSettingsToggleKey.APP_ICON_SIZE ->
            IconSizeInlineSlider(controls.appIconSizeStep, controls.onAppIconSizeStepChange)
        setting.toggleKey == AppSettingsToggleKey.HOME_TEXT_COLOR -> HomeTextColorInlineChips(controls)
        setting.toggleKey == AppSettingsToggleKey.ACCENT_COLOR ->
            AccentColorChoices(
                accentColorMode = controls.accentColorMode,
                customAccentColorArgb = controls.customAccentColorArgb,
                wallpaperAccentAvailable = controls.wallpaperAccentAvailable,
                appTheme = controls.appTheme,
                deviceThemeEnabled = controls.deviceThemeEnabled,
                onAccentColorModeChange = controls.onAccentColorModeChange,
                onCustomAccentColorChange = controls.onCustomAccentColorChange,
                modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
            )
        setting.toggleKey == AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE -> {
            Column(
                modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                SearchEngineStyleChoices(
                    compact = controls.searchEngineCompactMode,
                    onSelect = controls.onSearchEngineCompactModeChange,
                    enabled = controls.hasEnabledSearchEngines,
                )
                if (controls.hasEnabledSearchEngines && controls.searchEngineCompactMode) {
                    SearchEngineCompactRowChoices(
                        rowCount = controls.searchEngineCompactRowCount,
                        onSelect = controls.onSearchEngineCompactRowCountChange,
                    )
                }
            }
        }
        setting.id == THEME_MODE_SETTING_ID ->
            ThemeModeInlineChips(controls)
        setting.id == TOP_MATCHES_COUNT_SETTING_ID ->
            TopMatchesCountInlineSlider(controls.topMatchesLimit, controls.onTopMatchesLimitChange)
        setting.destination == AppSettingsRouteAction.AI_MODEL -> AiModelInlineContent(controls.aiModel)
    }
}

@Composable
private fun FontSizeInlineSlider(
    fontScaleMultiplier: Float,
    onChange: (Float) -> Unit,
) {
    val view = LocalView.current
    val committedIndex =
        FONT_SIZE_STEPS.indices.minByOrNull { abs(FONT_SIZE_STEPS[it].scale - fontScaleMultiplier) } ?: 2
    var sliderValue by remember(committedIndex) { mutableFloatStateOf(committedIndex.toFloat()) }
    val activeIndex = sliderValue.roundToInt().coerceIn(0, FONT_SIZE_STEPS.lastIndex)

    InlineSliderRow(
        value = sliderValue,
        onValueChange = { value ->
            sliderValue = value
            val index = value.roundToInt().coerceIn(0, FONT_SIZE_STEPS.lastIndex)
            if (index != committedIndex) {
                hapticToggle(view)()
                onChange(FONT_SIZE_STEPS[index].scale)
            }
        },
        valueRange = 0f..FONT_SIZE_STEPS.lastIndex.toFloat(),
        steps = FONT_SIZE_STEPS.size - 2,
        label = stringResource(FONT_SIZE_STEPS[activeIndex].labelRes),
    )
}

@Composable
private fun IconSizeInlineSlider(
    appIconSizeStep: Int,
    onChange: (Int) -> Unit,
) {
    val view = LocalView.current
    InlineSliderRow(
        value = appIconSizeStep.toFloat(),
        onValueChange = { value ->
            val step = value.roundToInt()
            if (step != appIconSizeStep) {
                hapticToggle(view)()
                onChange(step)
            }
        },
        valueRange =
            UiPreferences.MIN_APP_ICON_SIZE_STEP.toFloat()..
                UiPreferences.MAX_APP_ICON_SIZE_STEP.toFloat(),
        steps = UiPreferences.MAX_APP_ICON_SIZE_STEP - UiPreferences.MIN_APP_ICON_SIZE_STEP - 1,
        label =
            stringResource(
                R.string.settings_app_icon_size_percent,
                UiPreferences.appIconSizePercent(appIconSizeStep),
            ),
    )
}

@Composable
private fun TopMatchesCountInlineSlider(
    topMatchesLimit: Int,
    onChange: (Int) -> Unit,
) {
    val view = LocalView.current
    val limitOptions = UiPreferences.TOP_MATCHES_LIMIT_OPTIONS
    val currentIndex = limitOptions.indexOf(topMatchesLimit).coerceAtLeast(0)
    InlineSliderRow(
        value = currentIndex.toFloat(),
        onValueChange = { value ->
            val index = value.roundToInt().coerceIn(0, limitOptions.lastIndex)
            if (index != currentIndex) {
                hapticToggle(view)()
                onChange(limitOptions[index])
            }
        },
        valueRange = 0f..limitOptions.lastIndex.toFloat(),
        steps = limitOptions.size - 2,
        label = topMatchesLimit.toString(),
    )
}

@Composable
internal fun SearchHistoryCountSlider() {
    val view = LocalView.current
    val controls = LocalAppSettingInlineControls.current
    val options = UiPreferences.RECENT_QUERIES_DISPLAY_COUNT_OPTIONS
    val savedIndex = options.indexOf(controls.recentQueriesDisplayCount).coerceAtLeast(0)
    var selectedIndex by remember(savedIndex) { mutableIntStateOf(savedIndex) }
    InlineSliderRow(
        value = selectedIndex.toFloat(),
        onValueChange = { value ->
            val index = value.roundToInt().coerceIn(0, options.lastIndex)
            if (index != selectedIndex) {
                selectedIndex = index
                hapticToggle(view)()
            }
        },
        valueRange = 0f..options.lastIndex.toFloat(),
        steps = options.size - 2,
        label = options[selectedIndex].toString(),
        onValueChangeFinished = {
            if (selectedIndex != savedIndex) {
                controls.onRecentQueriesDisplayCountChange(options[selectedIndex])
            }
        },
    )
}

/** Dots or Count for the notification badge, shown under the enabled notification dots row. */
@Composable
internal fun NotificationBadgeStyleInlineChoice() {
    val controls = LocalAppSettingInlineControls.current
    ConnectedChoiceGroup(
        options =
            listOf(
                ChoiceOption(false, stringResource(R.string.notification_dots_style_dots)),
                ChoiceOption(true, stringResource(R.string.notification_dots_style_count)),
            ),
        selected = controls.notificationCountsEnabled,
        onSelect = { showCount ->
            if (showCount != controls.notificationCountsEnabled) {
                controls.onNotificationCountsEnabledChange(showCount)
            }
        },
        modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
    )
}

/** Corner radius slider shown under the enabled Circular App Icons row. */
@Composable
internal fun AppIconCornerInlineSlider() {
    val controls = LocalAppSettingInlineControls.current
    AppIconCornerSlider(
        onSetAppIconCornerPercent = controls.onAppIconCornerPercentChange,
        modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
    )
}

/** Full-width slider for inline app-setting rows, with a track that stays visible on result cards. */
@Composable
internal fun InlineSliderRow(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    label: String,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    SliderWithValue(
        modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SpacingSmall),
        valueSpacing = DesignTokens.SpacingSmall,
        valueLabel = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    ) { sliderModifier ->
        AppSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors =
                SliderDefaults.colors(
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintTonalAlpha),
                    inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            modifier = sliderModifier.heightIn(min = DesignTokens.MinTouchTarget),
        )
    }
}

@Composable
private fun HomeTextColorInlineChips(controls: AppSettingInlineControls) {
    val context = LocalContext.current
    val defaultColor = if (LocalAppIsDarkTheme.current) HomeTextColor.WHITE else HomeTextColor.BLACK
    val wallpaperDefaultColor by produceState(
        initialValue = defaultColor,
        controls.backgroundSource,
        controls.customImageUri,
    ) {
        value =
            WallpaperUtils.getBackgroundAppearance(context, controls.backgroundSource, controls.customImageUri)
                ?.isDark
                ?.let { isDark -> if (isDark) HomeTextColor.WHITE else HomeTextColor.BLACK }
                ?: defaultColor
    }
    val selected = controls.homeTextColorOverride ?: wallpaperDefaultColor
    ConnectedChoiceGroup(
        options =
            listOf(
                ChoiceOption(
                    value = HomeTextColor.WHITE,
                    label = stringResource(R.string.widget_text_icon_color_white),
                    icon = MaterialSymbols.LightMode,
                ),
                ChoiceOption(
                    value = HomeTextColor.BLACK,
                    label = stringResource(R.string.widget_text_icon_color_black),
                    icon = MaterialSymbols.DarkMode,
                ),
            ),
        selected = selected,
        onSelect = { color -> if (color != selected) controls.onHomeTextColorChange(color) },
        modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
    )
}

/** Plain labels: the row shares its width with the trailing chevron, too narrow for three icons. */
@Composable
private fun ThemeModeInlineChips(controls: AppSettingInlineControls) {
    ConnectedChoiceGroup(
        options =
            listOf(
                ChoiceOption(AppThemeMode.LIGHT, stringResource(R.string.common_theme_light)),
                ChoiceOption(AppThemeMode.DARK, stringResource(R.string.common_theme_dark)),
                ChoiceOption(AppThemeMode.SYSTEM, stringResource(R.string.common_theme_system)),
            ),
        selected = controls.appThemeMode,
        onSelect = { mode -> if (mode != controls.appThemeMode) controls.onAppThemeModeChange(mode) },
        modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
    )
}
