package com.tk.quicksearch.settings.AppearanceSettings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.searchScreen.AppThemeColors
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.WallpaperUtils
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
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val useMonoThemeFallback =
            backgroundSource == BackgroundSource.SYSTEM_WALLPAPER && !hasWallpaperPermission
    val effectiveBackgroundSource =
            if (useMonoThemeFallback) BackgroundSource.THEME else backgroundSource
    val effectiveSelectedTheme =
            if (useMonoThemeFallback) AppTheme.MONOCHROME else selectedTheme
    val isThemeSourceSelected = effectiveBackgroundSource == BackgroundSource.THEME

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
                                theme = AppTheme.MONOCHROME,
                                labelRes = R.string.settings_app_theme_monochrome,
                        ),
                        AppThemeOption(
                                theme = AppTheme.FOREST,
                                labelRes = R.string.settings_app_theme_forest,
                        ),
                        AppThemeOption(
                                theme = AppTheme.AURORA,
                                labelRes = R.string.settings_app_theme_aurora,
                        ),
                        AppThemeOption(
                                theme = AppTheme.SUNSET,
                                labelRes = R.string.settings_app_theme_sunset,
                        ),
                )
            }

    SettingsCard(
            modifier = modifier.fillMaxWidth(),
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                    text = stringResource(R.string.settings_app_theme_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )

            Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                AppModeOption(
                        label = stringResource(R.string.settings_app_theme_light),
                        icon = Icons.Rounded.LightMode,
                        selected = appThemeMode == AppThemeMode.LIGHT,
                        onClick = { onSetAppThemeMode(AppThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f),
                )
                AppModeOption(
                        label = stringResource(R.string.settings_app_theme_dark),
                        icon = Icons.Rounded.DarkMode,
                        selected = appThemeMode == AppThemeMode.DARK,
                        onClick = { onSetAppThemeMode(AppThemeMode.DARK) },
                        modifier = Modifier.weight(1f),
                )
                AppModeOption(
                        label = stringResource(R.string.settings_app_theme_system),
                        icon = Icons.Rounded.Settings,
                        selected = appThemeMode == AppThemeMode.SYSTEM,
                        onClick = { onSetAppThemeMode(AppThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(
                    color = AppColors.SettingsDivider,
                    modifier = Modifier.padding(horizontal = 75.dp, vertical = 12.dp),
            )

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                themeOptions.forEach { option ->
                    val isSelected = effectiveSelectedTheme == option.theme && isThemeSourceSelected
                    val interactionSource = remember { MutableInteractionSource() }
                    Column(
                            modifier =
                                    Modifier.weight(1f)
                                            .clickable(
                                                    interactionSource = interactionSource,
                                                    indication = null,
                                            ) {
                                                if (!isSelected) {
                                                    hapticToggle(view)()
                                                }
                                                onThemeSelected(option.theme)
                                                onSetBackgroundSource(BackgroundSource.THEME)
                                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(44.dp)
                                                .clip(MaterialTheme.shapes.medium)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        AppThemeColors(
                                                                                theme = option.theme,
                                                                                isDarkMode = isDarkMode,
                                                                                intensity = overlayThemeIntensity,
                                                                        ),
                                                        ),
                                                )
                                                .border(
                                                        width = DesignTokens.BorderWidth,
                                                        color = AppColors.SettingsDivider,
                                                        shape = MaterialTheme.shapes.medium,
                                                ),
                                contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Box(
                                        modifier =
                                                Modifier.size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                        Text(
                                text = stringResource(option.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                color =
                                        if (isSelected) {
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

            if (isThemeSourceSelected) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                            imageVector = Icons.Rounded.LightMode,
                            contentDescription =
                                    stringResource(R.string.settings_overlay_theme_tone_lighter),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
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
                            modifier = Modifier.weight(1f),
                    )
                    Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription =
                                    stringResource(R.string.settings_overlay_theme_tone_darker),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Row(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .clickable { onThemedIconsToggle(!themedIconsEnabled) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                ) {
                    Checkbox(
                            checked = themedIconsEnabled,
                            onCheckedChange = onThemedIconsToggle,
                    )
                    Text(
                            text = stringResource(R.string.settings_themed_icons_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun WallpaperCard(
        wallpaperBackgroundAlpha: Float,
        wallpaperBlurRadius: Float,
        onWallpaperBackgroundAlphaChange: (Float) -> Unit,
        onWallpaperBlurRadiusChange: (Float) -> Unit,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
        onSetBackgroundSource: (BackgroundSource) -> Unit,
        onPickCustomImage: () -> Unit,
        hasWallpaperPermission: Boolean,
        onRequestWallpaperPermission: () -> Unit,
        wallpaperAccentEnabled: Boolean,
        onWallpaperAccentToggle: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val wallpaperPreviewBitmap by
            produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                    initialValue = null,
                    key1 = hasWallpaperPermission,
                    key2 = backgroundSource,
            ) {
                value =
                        WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                                ?: WallpaperUtils.getWallpaperBitmap(context)?.asImageBitmap()
            }
    val customPreviewBitmap by
            produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                    initialValue = null,
                    key1 = customImageUri,
            ) {
                value = WallpaperUtils.getOverlayCustomImageBitmap(context, customImageUri)
            }

    val isWallpaperSourceSelected = backgroundSource == BackgroundSource.SYSTEM_WALLPAPER
    val isCustomSourceSelected = backgroundSource == BackgroundSource.CUSTOM_IMAGE

    var lastAlphaStep by remember {
        mutableStateOf((wallpaperBackgroundAlpha * 9).roundToInt().coerceIn(0, 9))
    }
    var lastBlurStep by remember {
        mutableStateOf(
                (wallpaperBlurRadius / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS * 7)
                        .roundToInt()
                        .coerceIn(0, 7),
        )
    }

    SettingsCard(
            modifier = modifier.fillMaxWidth(),
    ) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                    text = stringResource(R.string.settings_wallpaper_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverlaySourceBox(
                        modifier = Modifier.weight(1f),
                        selected = isWallpaperSourceSelected,
                        hasImage = wallpaperPreviewBitmap != null,
                        onClick = {
                            hapticToggle(view)()
                            onRequestWallpaperPermission()
                        },
                        label = stringResource(R.string.settings_overlay_source_wallpaper),
                ) {
                    if (wallpaperPreviewBitmap != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                    bitmap = wallpaperPreviewBitmap!!,
                                    contentDescription = null,
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .then(
                                                            if (isWallpaperSourceSelected) {
                                                                Modifier.blur(wallpaperBlurRadius.dp)
                                                            } else {
                                                                Modifier
                                                            },
                                                    ),
                                    contentScale = ContentScale.Crop,
                            )
                            if (isWallpaperSourceSelected) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .background(
                                                                if (isDarkMode) {
                                                                    Color.Black.copy(
                                                                            alpha =
                                                                                    wallpaperBackgroundAlpha
                                                                    )
                                                                } else {
                                                                    Color.White.copy(
                                                                            alpha =
                                                                                    wallpaperBackgroundAlpha
                                                                    )
                                                                },
                                                        ),
                                )
                            }
                        }
                    } else if (!hasWallpaperPermission) {
                        Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription =
                                            stringResource(
                                                    R.string.settings_overlay_source_permission_required,
                                            ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                            )
                            Text(
                                    text =
                                            stringResource(
                                                    R.string.settings_overlay_source_needs_permission,
                                            ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                OverlaySourceBox(
                        modifier = Modifier.weight(1f),
                        selected = isCustomSourceSelected,
                        hasImage = customPreviewBitmap != null,
                        onClick = {
                            hapticToggle(view)()
                            if (customPreviewBitmap != null) {
                                onSetBackgroundSource(BackgroundSource.CUSTOM_IMAGE)
                            } else {
                                onPickCustomImage()
                            }
                        },
                        label = stringResource(R.string.settings_overlay_source_custom),
                ) {
                    if (customPreviewBitmap != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                    bitmap = customPreviewBitmap!!,
                                    contentDescription = null,
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .then(
                                                            if (isCustomSourceSelected) {
                                                                Modifier.blur(wallpaperBlurRadius.dp)
                                                            } else {
                                                                Modifier
                                                            },
                                                    ),
                                    contentScale = ContentScale.Crop,
                            )
                            if (isCustomSourceSelected) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .background(
                                                                if (isDarkMode) {
                                                                    Color.Black.copy(
                                                                            alpha =
                                                                                    wallpaperBackgroundAlpha
                                                                    )
                                                                } else {
                                                                    Color.White.copy(
                                                                            alpha =
                                                                                    wallpaperBackgroundAlpha
                                                                    )
                                                                },
                                                        ),
                                )
                            }
                        }
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black)
                                                .clickable {
                                                    hapticToggle(view)()
                                                    onPickCustomImage()
                                                },
                                contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription =
                                            stringResource(
                                                    R.string.settings_overlay_source_edit_custom
                                            ),
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                            )
                        }
                    } else {
                        Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription =
                                        stringResource(R.string.settings_overlay_source_add_custom),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (isWallpaperSourceSelected || isCustomSourceSelected) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = stringResource(R.string.settings_wallpaper_transparency_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Slider(
                            value = wallpaperBackgroundAlpha,
                            onValueChange = { value ->
                                val step = (value * 9).roundToInt().coerceIn(0, 9)
                                if (step != lastAlphaStep) {
                                    hapticToggle(view)()
                                    lastAlphaStep = step
                                }
                                onWallpaperBackgroundAlphaChange(value)
                            },
                            valueRange = 0f..1f,
                            steps = 9,
                            modifier = Modifier.weight(1f),
                    )
                    Text(
                            text = "${(wallpaperBackgroundAlpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(min = 48.dp),
                    )
                }

                Text(
                        text = stringResource(R.string.settings_wallpaper_blur_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Slider(
                            value = wallpaperBlurRadius,
                            onValueChange = { value ->
                                val step =
                                        (value / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS * 7)
                                                .roundToInt()
                                                .coerceIn(0, 7)
                                if (step != lastBlurStep) {
                                    hapticToggle(view)()
                                    lastBlurStep = step
                                }
                                onWallpaperBlurRadiusChange(value)
                            },
                            valueRange = 0f..UiPreferences.MAX_WALLPAPER_BLUR_RADIUS,
                            steps = 7,
                            modifier = Modifier.weight(1f),
                    )
                    Text(
                            text =
                                    "${((wallpaperBlurRadius / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS) * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(min = 48.dp),
                    )
                }

                if (isWallpaperSourceSelected || isCustomSourceSelected) {
                    SettingsToggleRow(
                            title = stringResource(R.string.settings_wallpaper_accent_title),
                            subtitle = stringResource(R.string.settings_wallpaper_accent_desc),
                            checked = wallpaperAccentEnabled,
                            onCheckedChange = onWallpaperAccentToggle,
                            horizontalPadding = DesignTokens.SpacingSmall,
                            isLastItem = true,
                            showDivider = false,
                    )
                }

            }
        }
    }
}

@Composable
private fun AppModeOption(
        label: String,
        icon: ImageVector,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val borderColor =
            if (selected) MaterialTheme.colorScheme.primary
            else AppColors.SettingsDivider
    val backgroundColor =
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else Color.Transparent

    Column(
            modifier =
                    modifier
                            .clip(MaterialTheme.shapes.large)
                            .background(color = backgroundColor, shape = MaterialTheme.shapes.large)
                            .border(
                                    width = DesignTokens.BorderWidth,
                                    color = borderColor,
                                    shape = MaterialTheme.shapes.large,
                            )
                            .selectable(
                                    selected = selected,
                                    onClick = onClick,
                                    role = Role.RadioButton,
                            )
                            .padding(
                                    horizontal = DesignTokens.ChipHorizontalPadding,
                                    vertical = DesignTokens.ChipVerticalPadding,
                            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun OverlaySourceBox(
        modifier: Modifier = Modifier,
        selected: Boolean,
        label: String,
        hasImage: Boolean = false,
        onClick: () -> Unit,
        content: @Composable BoxScope.() -> Unit,
) {
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(52.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(Color.Transparent)
                                .clickable(onClick = onClick)
                                .then(
                                        if (!hasImage || isDarkMode) {
                                            Modifier.border(
                                                    width = DesignTokens.BorderWidth,
                                                    color = AppColors.SettingsDivider,
                                                    shape = MaterialTheme.shapes.medium,
                                            )
                                        } else {
                                            Modifier
                                        },
                                ),
                contentAlignment = Alignment.Center,
        ) {
            content()
            if (selected) {
                Box(
                        modifier =
                                Modifier.size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                ) {
                    Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class AppThemeOption(
        val theme: AppTheme,
        val labelRes: Int,
)
