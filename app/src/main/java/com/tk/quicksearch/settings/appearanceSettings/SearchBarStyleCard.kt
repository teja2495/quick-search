package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.shared.ui.components.AppSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.AppIconShape
import com.tk.quicksearch.search.core.SearchBarStyle
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.searchScreen.components.backgroundColor
import com.tk.quicksearch.search.searchScreen.components.borderColor
import com.tk.quicksearch.search.searchScreen.components.borderWidth
import com.tk.quicksearch.search.searchScreen.components.cornerRadius
import com.tk.quicksearch.search.searchScreen.components.readableOn
import com.tk.quicksearch.search.searchScreen.components.searchBarContrastBackground
import com.tk.quicksearch.search.searchScreen.insetCardBackground
import com.tk.quicksearch.searchEngines.inline.InsetSearchBarGeometry
import com.tk.quicksearch.searchEngines.inline.SearchEngineIconsSection
import com.tk.quicksearch.searchEngines.inline.insetCardGlow
import com.tk.quicksearch.searchEngines.inline.rememberInsetCardStyle
import com.tk.quicksearch.shared.ui.components.SliderWithValue
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.ExpandableSegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetColorPickerDialog
import java.util.Locale
import kotlin.math.roundToInt

private const val BorderWidthStepDp = 0.5f
private const val BorderWidthSteps =
    ((SearchBarStyle.MAX_BORDER_WIDTH_DP - SearchBarStyle.MIN_BORDER_WIDTH_DP) / BorderWidthStepDp).toInt() - 1
private const val TransparencySteps = 9
private const val CornerRadiusStepDp = 2f
private const val CornerRadiusSteps =
    ((SearchBarStyle.MAX_CORNER_RADIUS_DP - SearchBarStyle.MIN_CORNER_RADIUS_DP) / CornerRadiusStepDp).toInt() - 1
private val SliderValueWidth = 64.dp
private val PreviewHeight = 56.dp

private enum class SearchBarColorTarget { BORDER, BACKGROUND }

@Composable
fun SearchBarStyleCard(
    searchBarStyle: SearchBarStyle,
    onSetSearchBarStyle: (SearchBarStyle) -> Unit,
    showWallpaperBackground: Boolean,
    settingsIconEnabled: Boolean,
    settingsIconRequired: Boolean,
    onToggleSettingsIcon: (Boolean) -> Unit,
    searchChromeBlurEnabled: Boolean,
    onToggleSearchChromeBlur: (Boolean) -> Unit,
    expanded: Boolean = true,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    fixedShape: Boolean = false,
    enabledSearchEngines: List<SearchTarget> = emptyList(),
    searchEngineCompactRowCount: Int = 1,
    appIconShape: AppIconShape = AppIconShape.DEFAULT,
    iconPackPackage: String? = null,
) {
    val view = LocalView.current
    // Sliders drive the preview locally and persist once the drag settles.
    var borderWidthDp by remember(searchBarStyle.borderWidthDp) {
        mutableFloatStateOf(searchBarStyle.borderWidthDp)
    }
    var transparency by remember(searchBarStyle.backgroundTransparency) {
        mutableFloatStateOf(searchBarStyle.backgroundTransparency)
    }
    var cornerRadiusDp by remember(searchBarStyle.cornerRadiusDp) {
        mutableFloatStateOf(searchBarStyle.cornerRadiusDp)
    }
    var colorPickerTarget by rememberSaveable { mutableStateOf<SearchBarColorTarget?>(null) }
    val previewStyle =
        searchBarStyle
            .copy(
                borderWidthDp = borderWidthDp,
                backgroundTransparency = transparency,
                cornerRadiusDp = cornerRadiusDp,
            ).let { if (fixedShape) it.withFixedShape() else it }

    ExpandableSegmentedGroup(
        title = stringResource(R.string.settings_search_bar_style_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        tileCount = 5,
        modifier = modifier,
        firstTileContent = {

            if (fixedShape && enabledSearchEngines.isNotEmpty()) {
                InsetSearchCardPreview(
                    style = previewStyle,
                    showWallpaperBackground = showWallpaperBackground,
                    showSettingsIcon = settingsIconEnabled,
                    enabledSearchEngines = enabledSearchEngines,
                    compactRowCount = searchEngineCompactRowCount,
                    appIconShape = appIconShape,
                    iconPackPackage = iconPackPackage,
                    modifier = Modifier.padding(bottom = DesignTokens.SpacingMedium),
                )
            } else {
                SearchBarStylePreview(
                    style = previewStyle,
                    showWallpaperBackground = showWallpaperBackground,
                    showSettingsIcon = settingsIconEnabled,
                    modifier = Modifier.padding(bottom = DesignTokens.SpacingMedium),
                )
            }

            ColorChoiceRow(
                label = stringResource(R.string.settings_search_bar_background_color_label),
                defaultLabel = stringResource(R.string.settings_app_theme_title),
                customArgb = searchBarStyle.backgroundColorArgb,
                onSelectDefault = {
                    hapticToggle(view)()
                    onSetSearchBarStyle(searchBarStyle.copy(backgroundColorArgb = null))
                },
                onSelectCustom = {
                    hapticToggle(view)()
                    colorPickerTarget = SearchBarColorTarget.BACKGROUND
                },
            )

            ColorChoiceRow(
                label = stringResource(R.string.widget_border_color),
                defaultLabel = stringResource(R.string.settings_search_bar_color_accent),
                customArgb = searchBarStyle.borderColorArgb,
                onSelectDefault = {
                    hapticToggle(view)()
                    onSetSearchBarStyle(searchBarStyle.copy(borderColorArgb = null))
                },
                onSelectCustom = {
                    hapticToggle(view)()
                    colorPickerTarget = SearchBarColorTarget.BORDER
                },
            )

            // The compact engine card fixes the bar's shape (SearchBarStyle.hasFixedShape).
            if (!fixedShape) {
                SliderSetting(
                    label = stringResource(R.string.widget_slider_border),
                    value = borderWidthDp,
                    valueRange = SearchBarStyle.MIN_BORDER_WIDTH_DP..SearchBarStyle.MAX_BORDER_WIDTH_DP,
                    steps = BorderWidthSteps,
                    valueText = String.format(Locale.getDefault(), "%.1f dp", borderWidthDp),
                    onValueChange = { value ->
                        val snapped = (value / BorderWidthStepDp).roundToInt() * BorderWidthStepDp
                        if (snapped != borderWidthDp) {
                            hapticToggle(view)()
                            borderWidthDp = snapped
                        }
                    },
                    onValueChangeFinished = {
                        onSetSearchBarStyle(searchBarStyle.copy(borderWidthDp = borderWidthDp))
                    },
                )

                SliderSetting(
                    label = stringResource(R.string.widget_slider_radius),
                    value = cornerRadiusDp,
                    valueRange = SearchBarStyle.MIN_CORNER_RADIUS_DP..SearchBarStyle.MAX_CORNER_RADIUS_DP,
                    steps = CornerRadiusSteps,
                    valueText = String.format(Locale.getDefault(), "%.0f dp", cornerRadiusDp),
                    onValueChange = { value ->
                        val snapped = (value / CornerRadiusStepDp).roundToInt() * CornerRadiusStepDp
                        if (snapped != cornerRadiusDp) {
                            hapticToggle(view)()
                            cornerRadiusDp = snapped
                        }
                    },
                    onValueChangeFinished = {
                        onSetSearchBarStyle(searchBarStyle.copy(cornerRadiusDp = cornerRadiusDp))
                    },
                )
            }

            SliderSetting(
                label = stringResource(R.string.settings_wallpaper_transparency_label),
                value = transparency,
                valueRange = 0f..1f,
                steps = TransparencySteps,
                valueText = "${(transparency * 100).roundToInt()}%",
                onValueChange = { value ->
                    val snapped = (value * (TransparencySteps + 1)).roundToInt() / (TransparencySteps + 1).toFloat()
                    if (snapped != transparency) {
                        hapticToggle(view)()
                        transparency = snapped
                    }
                },
                onValueChangeFinished = {
                    onSetSearchBarStyle(searchBarStyle.copy(backgroundTransparency = transparency))
                },
            )
        },
    ) {
      SegmentedTile(index = 1, count = 5) {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_search_chrome_blur_title),
                    subtitle = stringResource(R.string.settings_search_chrome_blur_desc),
                    checked = searchChromeBlurEnabled,
                    onCheckedChange = onToggleSearchChromeBlur,
                    showDivider = false,
                )
      }
      SegmentedTile(index = 2, count = 5) {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_icon_title),
                    subtitle =
                        when {
                            settingsIconRequired -> stringResource(R.string.settings_icon_required_by_gesture_desc)
                            settingsIconEnabled -> stringResource(R.string.settings_icon_desc)
                            else -> stringResource(R.string.settings_icon_off_desc)
                        },
                    checked = settingsIconEnabled,
                    onCheckedChange = onToggleSettingsIcon,
                    enabled = !settingsIconRequired,
                    showDivider = false,
                )
      }
      SegmentedTile(index = 3, count = 5) {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_mic_icon_title),
                    subtitle = stringResource(R.string.settings_mic_icon_desc),
                    checked = searchBarStyle.showMicIcon,
                    onCheckedChange = { enabled ->
                        onSetSearchBarStyle(searchBarStyle.copy(showMicIcon = enabled))
                    },
                    showDivider = false,
                )
      }
      SegmentedTile(index = 4, count = 5) {
            TonalButton(
                text = stringResource(R.string.settings_launcher_icon_reset_default),
                icon = MaterialSymbols.RestartAlt,
                onClick = {
                    onSetSearchBarStyle(SearchBarStyle())
                    if (!settingsIconEnabled) onToggleSettingsIcon(true)
                    if (searchChromeBlurEnabled) onToggleSearchChromeBlur(false)
                },
                enabled = !previewStyle.isDefault || !settingsIconEnabled || searchChromeBlurEnabled,
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
            )
      }
    }

    colorPickerTarget?.let { target ->
        val currentArgb =
            when (target) {
                SearchBarColorTarget.BORDER -> searchBarStyle.borderColorArgb
                SearchBarColorTarget.BACKGROUND -> searchBarStyle.backgroundColorArgb
            }
        val fallbackColor =
            when (target) {
                SearchBarColorTarget.BORDER -> AppColors.Accent
                SearchBarColorTarget.BACKGROUND ->
                    AppColors.getSearchBarBackground(showWallpaperBackground).copy(alpha = 1f)
            }
        WidgetColorPickerDialog(
            initialColor = currentArgb?.let { Color(it) } ?: fallbackColor,
            onDismiss = { colorPickerTarget = null },
            onConfirm = { color ->
                val argb = color.toArgb()
                onSetSearchBarStyle(
                    when (target) {
                        SearchBarColorTarget.BORDER -> searchBarStyle.copy(borderColorArgb = argb)
                        SearchBarColorTarget.BACKGROUND -> searchBarStyle.copy(backgroundColorArgb = argb)
                    },
                )
                colorPickerTarget = null
            },
            title =
                stringResource(
                    when (target) {
                        SearchBarColorTarget.BORDER -> R.string.widget_border_color
                        SearchBarColorTarget.BACKGROUND -> R.string.settings_search_bar_background_color_label
                    },
                ),
        )
    }
}

/**
 * The bottom search bar sitting in its compact engine card, built from the same card and engine row
 * the search screen uses, so the preview shows the card's real shape, fill and glow.
 */
@Composable
private fun InsetSearchCardPreview(
    style: SearchBarStyle,
    showWallpaperBackground: Boolean,
    showSettingsIcon: Boolean,
    enabledSearchEngines: List<SearchTarget>,
    compactRowCount: Int,
    appIconShape: AppIconShape,
    iconPackPackage: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Drawn above the card, which reaches up behind it, matching SearchBarGeometry's insets.
        SearchBarStylePreview(
            style = style,
            showWallpaperBackground = showWallpaperBackground,
            showSettingsIcon = showSettingsIcon,
            insetInCard = true,
            modifier =
                Modifier
                    .zIndex(1f)
                    .padding(
                        start = InsetSearchBarGeometry.BarHorizontalInset,
                        end = InsetSearchBarGeometry.BarHorizontalInset,
                        top = InsetSearchBarGeometry.BarTopSpacing,
                        bottom = InsetSearchBarGeometry.BarBottomSpacing,
                    ),
        )
        SearchEngineIconsSection(
            query = "",
            enabledEngines = enabledSearchEngines,
            onSearchEngineClick = { _, _ -> },
            onSearchEngineLongPress = {},
            showWallpaperBackground = showWallpaperBackground,
            compactRowCount = compactRowCount,
            appIconShape = appIconShape,
            iconPackPackage = iconPackPackage,
            useInsetContainer = true,
            insetOverlap = InsetSearchBarGeometry.ContainerOverlap,
            insetCardStyle = rememberInsetCardStyle(style, showWallpaperBackground),
        )
    }
}

/** @param insetInCard the bar sits in the compact engine card, which paints its fill and outline. */
@Composable
private fun SearchBarStylePreview(
    style: SearchBarStyle,
    showWallpaperBackground: Boolean,
    showSettingsIcon: Boolean,
    modifier: Modifier = Modifier,
    insetInCard: Boolean = false,
) {
    val contrastBackground = searchBarContrastBackground(style, showWallpaperBackground)
    val placeholderColor = AppColors.getSearchBarTextAndIconColor(showWallpaperBackground)
        .readableOn(contrastBackground)
        .copy(alpha = DesignTokens.SearchFieldPlaceholderAlpha)
        .readableOn(contrastBackground)
    val iconTint = AppColors.getSearchBarSecondaryIconTint(showWallpaperBackground).readableOn(contrastBackground, 3f)
    val showSettingsOutside = showSettingsIcon && style.showMicIcon
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(PreviewHeight)
                    .then(
                        if (insetInCard) {
                            Modifier
                        } else {
                            Modifier
                                .insetCardGlow(style.borderColor(AppColors.Accent), RoundedCornerShape(style.cornerRadius()))
                                .searchBarPreviewSurface(style, showWallpaperBackground)
                        },
                    ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpacingLarge),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Search,
                    contentDescription = null,
                    tint = iconTint,
                )
                Text(
                    text = stringResource(R.string.common_search),
                    style = MaterialTheme.typography.titleMedium,
                    color = placeholderColor,
                    modifier = Modifier.weight(1f),
                )
                val trailingIcon =
                    when {
                        style.showMicIcon -> MaterialSymbols.Mic
                        showSettingsIcon -> MaterialSymbols.Settings
                        else -> null
                    }
                trailingIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                    )
                }
            }
        }
        if (showSettingsOutside) {
            Box(
                modifier =
                    Modifier
                        .size(PreviewHeight)
                        .searchBarPreviewSurface(style, showWallpaperBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Settings,
                    contentDescription = null,
                    tint = iconTint,
                )
            }
        }
    }
}

/** Outline, corner radius and fill shared by the preview bar and its outside settings button. */
@Composable
private fun Modifier.searchBarPreviewSurface(
    style: SearchBarStyle,
    showWallpaperBackground: Boolean,
): Modifier {
    val shape = RoundedCornerShape(style.cornerRadius())
    return this
        .then(
            if (style.borderWidthDp > 0f) {
                Modifier.border(style.borderWidth(), style.borderColor(AppColors.Accent), shape)
            } else {
                Modifier
            },
        ).clip(shape)
        .background(insetCardBackground(true, style.backgroundColor(AppColors.getSearchBarBackground(showWallpaperBackground)), anywhere = true))
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // No spacing: the value is centered in the gap between the slider end and the card edge.
        SliderWithValue(
            valueLabel = {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(SliderValueWidth),
                )
            },
        ) { sliderModifier ->
            AppSlider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                modifier = sliderModifier,
            )
        }
    }
}

@Composable
private fun ColorChoiceRow(
    label: String,
    defaultLabel: String,
    customArgb: Int?,
    onSelectDefault: () -> Unit,
    onSelectCustom: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ConnectedChoiceGroup(
            options =
                listOf(
                    ChoiceOption(false, defaultLabel),
                    ChoiceOption(true, stringResource(R.string.common_custom), swatch = customArgb?.let { Color(it) }),
                ),
            selected = customArgb != null,
            onSelect = { custom -> if (custom) onSelectCustom() else if (customArgb != null) onSelectDefault() },
            haptic = false,
        )
    }
}
