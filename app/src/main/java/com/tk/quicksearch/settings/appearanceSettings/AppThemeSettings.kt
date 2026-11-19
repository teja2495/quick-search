package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.foundation.selection.selectableGroup
import com.tk.quicksearch.shared.ui.components.SliderWithValue
import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.R
import com.tk.quicksearch.app.HomeActivity
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.ExpandableSegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.roundToInt

@Composable
fun AppThemeCard(
        selectedTheme: AppTheme,
        overlayThemeIntensity: Float,
        onThemeSelected: (AppTheme) -> Unit,
        onOverlayThemeIntensityChange: (Float) -> Unit,
        backgroundSource: BackgroundSource,
        onSetBackgroundSource: (BackgroundSource) -> Unit,
        appThemeMode: AppThemeMode,
        onSetAppThemeMode: (AppThemeMode) -> Unit,
        hasWallpaperPermission: Boolean,
        themedIconsEnabled: Boolean,
        onThemedIconsToggle: (Boolean) -> Unit,
        deviceThemeEnabled: Boolean,
        onDeviceThemeToggle: (Boolean) -> Unit,
        amoledThemeEnabled: Boolean,
        onAmoledThemeToggle: (Boolean) -> Unit,
        expanded: Boolean = true,
        onExpandedChange: (Boolean) -> Unit = {},
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val canShowSystemWallpaperBackdrop =
            (context as? HomeActivity)?.canShowSystemWallpaperBackdrop == true
    val isDarkMode = LocalAppIsDarkTheme.current

    val useMonoThemeFallback =
            backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                    !hasWallpaperPermission &&
                    !canShowSystemWallpaperBackdrop
    val effectiveBackgroundSource =
            if (useMonoThemeFallback) BackgroundSource.THEME else backgroundSource
    val effectiveSelectedTheme =
            if (useMonoThemeFallback) AppTheme.MONOCHROME else selectedTheme
    val isThemeSourceSelected = effectiveBackgroundSource == BackgroundSource.THEME
    val showThemedIconsToggle = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val showMaterialYouToggle = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val showAmoledThemeToggle = isDarkMode && !deviceThemeEnabled
    val amoledPreviewActive = amoledThemeEnabled && isDarkMode
    var showCustomPaletteDialog by rememberSaveable { mutableStateOf(false) }

    val minIntensity = UiPreferences.MIN_OVERLAY_THEME_INTENSITY
    val maxIntensity = UiPreferences.MAX_OVERLAY_THEME_INTENSITY
    val intensityStep = UiPreferences.OVERLAY_THEME_INTENSITY_STEP
    val intensitySteps = (UiPreferences.OVERLAY_THEME_INTENSITY_DELTA_STEPS * 2) - 1
    var lastToneStep by remember {
        mutableStateOf(
                ((overlayThemeIntensity - minIntensity) / intensityStep)
                        .roundToInt()
                        .coerceIn(0, UiPreferences.OVERLAY_THEME_INTENSITY_DELTA_STEPS * 2),
        )
    }

    val themeOptions =
            remember {
                listOf(
                        AppThemeOption(
                                isCustom = false,
                                labelRes = R.string.settings_app_theme_monochrome,
                        ),
                        AppThemeOption(
                                isCustom = true,
                                labelRes = R.string.settings_app_theme_colorful,
                        ),
                )
            }

    if (showCustomPaletteDialog) {
        ThemePaletteDrawer(
                selectedTheme = if (!deviceThemeEnabled) effectiveSelectedTheme else null,
                isDarkMode = isDarkMode,
                intensity = overlayThemeIntensity,
                onPaletteSelected = { palette ->
                    hapticToggle(view)()
                    onThemeSelected(palette)
                    // A wallpaper or custom image background stays; only the theme changes.
                    if (useMonoThemeFallback) onSetBackgroundSource(BackgroundSource.THEME)
                },
                onDismissRequest = { showCustomPaletteDialog = false },
        )
    }

    val count = 2 + listOf(showMaterialYouToggle, showAmoledThemeToggle, showThemedIconsToggle).count { it }
    ExpandableSegmentedGroup(
            title = stringResource(R.string.settings_app_theme_title),
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            tileCount = count,
            modifier = modifier,
            firstTileContent = {
                ConnectedChoiceGroup(
                        options =
                                listOf(
                                        ChoiceOption(AppThemeMode.LIGHT, stringResource(R.string.common_theme_light), MaterialSymbols.LightMode),
                                        ChoiceOption(AppThemeMode.DARK, stringResource(R.string.common_theme_dark), MaterialSymbols.DarkMode),
                                        ChoiceOption(AppThemeMode.SYSTEM, stringResource(R.string.common_theme_system), MaterialSymbols.Settings),
                                ),
                        selected = appThemeMode,
                        onSelect = { mode -> if (mode != appThemeMode) onSetAppThemeMode(mode) },
                )
            },
    ) {
        var index = 1
        SegmentedTile(index = index++, count = count) {
            SegmentedTileContent {
                    Row(
                            modifier = Modifier.fillMaxWidth().selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        themeOptions.forEach { option ->
                            val selectedThemeForDisplay =
                                    if (deviceThemeEnabled) AppTheme.MONOCHROME else effectiveSelectedTheme
                            // The theme stays selected whatever the background; only the background tiles change.
                            val isSelected =
                                    (selectedThemeForDisplay == AppTheme.MONOCHROME) != option.isCustom
                            val showSelection = isSelected && !deviceThemeEnabled
                            val interactionSource = remember { MutableInteractionSource() }
                            Column(
                                    modifier =
                                            Modifier.weight(1f)
                                                    // No ripple: the tile's selection outline and the live theme change are the feedback.
                                                    .selectable(
                                                            selected = showSelection,
                                                            role = Role.RadioButton,
                                                            interactionSource = interactionSource,
                                                            indication = null,
                                                    ) {
                                                        if (deviceThemeEnabled) {
                                                            AppMessages.show(context, context.getString(R.string.settings_device_theme_blocked_toast))
                                                        } else if (option.isCustom) {
                                                            showCustomPaletteDialog = true
                                                        } else {
                                                            if (!isSelected) {
                                                                hapticToggle(view)()
                                                            }
                                                            onThemeSelected(AppTheme.MONOCHROME)
                                                            if (useMonoThemeFallback) {
                                                                onSetBackgroundSource(BackgroundSource.THEME)
                                                            }
                                                        }
                                                    },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                            ) {
                                val previewColors =
                                        themeOptionPreviewColors(
                                                isCustom = option.isCustom,
                                                selectedTheme = selectedThemeForDisplay,
                                                isDarkMode = isDarkMode,
                                                intensity = overlayThemeIntensity,
                                                amoledPreviewActive = amoledPreviewActive,
                                        )
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(AppShapes.medium)
                                                        .background(Brush.linearGradient(previewColors))
                                                        .border(
                                                                width = DesignTokens.BorderWidth,
                                                                color = AppColors.SettingsDivider,
                                                                shape = AppShapes.medium,
                                                        ),
                                        contentAlignment = Alignment.Center,
                                ) {
                                    if (option.isCustom && selectedThemeForDisplay == AppTheme.MONOCHROME) {
                                        // No palette picked yet: the Default look with a palette icon.
                                        Icon(
                                                imageVector = MaterialSymbols.Palette,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(DesignTokens.IconSize),
                                        )
                                    } else if (showSelection) {
                                        Box(
                                                modifier =
                                                        Modifier.size(DesignTokens.IconSize)
                                                                .clip(AppShapes.full)
                                                                .background(MaterialTheme.colorScheme.primary),
                                                contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                    imageVector = MaterialSymbols.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(DesignTokens.ButtonIconSizeExtraSmall),
                                            )
                                        }
                                    }
                                }
                                Text(
                                        text = stringResource(option.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                                if (showSelection) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    if (isThemeSourceSelected && !deviceThemeEnabled) {
                        SliderWithValue(
                            modifier = Modifier.fillMaxWidth(),
                            valueSpacing = DesignTokens.SpacingMedium,
                            valueLabel = {
                                Icon(
                                        imageVector = MaterialSymbols.DarkMode,
                                        contentDescription =
                                                stringResource(R.string.settings_overlay_theme_tone_darker),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            leadingLabel = {
                                Icon(
                                        imageVector = MaterialSymbols.LightMode,
                                        contentDescription =
                                                stringResource(R.string.settings_overlay_theme_tone_lighter),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        ) { sliderModifier ->
                            Slider(
                                    colors = AppColors.sliderColors(),
                                    value = overlayThemeIntensity,
                                    onValueChange = { value ->
                                        val step =
                                                ((value - minIntensity) / intensityStep)
                                                        .roundToInt()
                                                        .coerceIn(
                                                                0,
                                                                UiPreferences.OVERLAY_THEME_INTENSITY_DELTA_STEPS *
                                                                        2,
                                                        )
                                        if (step != lastToneStep) {
                                            hapticToggle(view)()
                                            lastToneStep = step
                                        }
                                        onOverlayThemeIntensityChange(value)
                                    },
                                    valueRange = minIntensity..maxIntensity,
                                    steps = intensitySteps,
                                    modifier = sliderModifier,
                            )
                        }
                    }
            }
        }
        if (showMaterialYouToggle) {
            SegmentedTile(index = index++, count = count) {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_device_theme_title),
                        subtitle = stringResource(R.string.settings_device_theme_desc),
                        checked = deviceThemeEnabled,
                        onCheckedChange = onDeviceThemeToggle,
                        isLastItem = !showAmoledThemeToggle && !showThemedIconsToggle,
                        showDivider = showAmoledThemeToggle || showThemedIconsToggle,
                )
            }
        }
        if (showAmoledThemeToggle) {
            SegmentedTile(index = index++, count = count) {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_amoled_theme_title),
                        subtitle = stringResource(R.string.settings_amoled_theme_desc),
                        checked = amoledThemeEnabled,
                        onCheckedChange = onAmoledThemeToggle,
                        isLastItem = !showThemedIconsToggle,
                        showDivider = showThemedIconsToggle,
                )
            }
        }
        if (showThemedIconsToggle) {
            SegmentedTile(index = index++, count = count) {
                SettingsToggleRow(
                        title = stringResource(R.string.settings_themed_icons_title),
                        subtitle = stringResource(R.string.settings_themed_icons_desc),
                        checked = themedIconsEnabled,
                        onCheckedChange = onThemedIconsToggle,
                        isLastItem = true,
                        showDivider = false,
                )
            }
        }
    }
}

private data class AppThemeOption(
        val isCustom: Boolean,
        val labelRes: Int,
)
