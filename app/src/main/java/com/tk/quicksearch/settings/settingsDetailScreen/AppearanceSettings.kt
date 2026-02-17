package com.tk.quicksearch.settings.settingsDetailScreen

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.searchScreen.overlayGradientColors
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineAppearanceCard
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.util.hapticToggle
import kotlin.math.roundToInt

/** Combined card for keyboard alignment and icon pack settings. */
@Composable
fun CombinedLayoutIconCard(
        oneHandedMode: Boolean,
        onToggleOneHandedMode: (Boolean) -> Unit,
        iconPackTitle: String,
        iconPackDescription: String,
        onIconPackClick: () -> Unit,
        onRefreshIconPacks: () -> Unit = {},
        modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column {
            // Results alignment toggle
            SettingsToggleRow(
                    title = stringResource(R.string.settings_layout_option_bottom_title),
                    subtitle = stringResource(R.string.settings_layout_option_bottom_desc),
                    checked = oneHandedMode,
                    onCheckedChange = onToggleOneHandedMode,
                    isFirstItem = true,
                    extraVerticalPadding = 8.dp,
            )

            // Icon Pack Section (with navigation)
            val hasIconPacks =
                    iconPackDescription != stringResource(R.string.settings_icon_pack_empty)
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable(onClick = onIconPackClick)
                                    .padding(
                                            start = 24.dp,
                                            top = 16.dp,
                                            end = 24.dp,
                                            bottom = if (hasIconPacks) 16.dp else 20.dp,
                                    ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                            text = iconPackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                            text = iconPackDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                        imageVector =
                                if (hasIconPacks) {
                                    Icons.Rounded.ChevronRight
                                } else {
                                    Icons.Rounded.Refresh
                                },
                        contentDescription =
                                if (hasIconPacks) {
                                    stringResource(R.string.desc_navigate_forward)
                                } else {
                                    stringResource(R.string.settings_refresh_icon_packs)
                                },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier =
                                Modifier.padding(start = 8.dp)
                                        .then(
                                                if (!hasIconPacks) {
                                                    Modifier.clickable(
                                                            interactionSource =
                                                                    remember {
                                                                        MutableInteractionSource()
                                                                    },
                                                            indication = null,
                                                            onClick = onRefreshIconPacks,
                                                    )
                                                } else {
                                                    Modifier
                                                },
                                        ),
                )
            }
        }
    }
}

/** Complete appearance settings section with all appearance-related components and dialogs. */
@Composable
fun AppearanceSettingsSection(
        oneHandedMode: Boolean,
        onToggleOneHandedMode: (Boolean) -> Unit,
        wallpaperBackgroundAlpha: Float,
        wallpaperBlurRadius: Float,
        onWallpaperBackgroundAlphaChange: (Float) -> Unit,
        onWallpaperBlurRadiusChange: (Float) -> Unit,
        overlayGradientTheme: OverlayGradientTheme,
        overlayThemeIntensity: Float,
        onSetOverlayGradientTheme: (OverlayGradientTheme) -> Unit,
        onOverlayThemeIntensityChange: (Float) -> Unit,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
        onSetBackgroundSource: (BackgroundSource) -> Unit,
        onPickCustomImage: () -> Unit,
        onRequestWallpaperPermission: () -> Unit,
        isSearchEngineCompactMode: Boolean,
        onToggleSearchEngineCompactMode: (Boolean) -> Unit,
        selectedIconPackPackage: String?,
        availableIconPacks: List<IconPackInfo>,
        onSelectIconPack: (String?) -> Unit,
        onRefreshIconPacks: () -> Unit,
        onSearchIconPacks: () -> Unit,
        hasWallpaperPermission: Boolean = true,
        modifier: Modifier = Modifier,
) {
    val appearanceContext = androidx.compose.ui.platform.LocalContext.current
    var showIconPackDialog by remember { mutableStateOf(false) }

    val hasIconPacks = availableIconPacks.isNotEmpty()
    val selectedIconPackLabel =
            remember(selectedIconPackPackage, availableIconPacks) {
                availableIconPacks.firstOrNull { it.packageName == selectedIconPackPackage }?.label
                        ?: appearanceContext.getString(R.string.settings_icon_pack_option_system)
            }

    Column(modifier = modifier) {
        OverlayThemeCard(
                selectedTheme = overlayGradientTheme,
                overlayThemeIntensity = overlayThemeIntensity,
                onThemeSelected = onSetOverlayGradientTheme,
                onOverlayThemeIntensityChange = onOverlayThemeIntensityChange,
                wallpaperBackgroundAlpha = wallpaperBackgroundAlpha,
                wallpaperBlurRadius = wallpaperBlurRadius,
                onWallpaperBackgroundAlphaChange = onWallpaperBackgroundAlphaChange,
                onWallpaperBlurRadiusChange = onWallpaperBlurRadiusChange,
                backgroundSource = backgroundSource,
                customImageUri = customImageUri,
                onSetBackgroundSource = onSetBackgroundSource,
                onPickCustomImage = onPickCustomImage,
                hasWallpaperPermission = hasWallpaperPermission,
                onRequestWallpaperPermission = onRequestWallpaperPermission,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Engine Style Card
        SearchEngineAppearanceCard(
                isSearchEngineCompactMode = isSearchEngineCompactMode,
                onToggleSearchEngineCompactMode = onToggleSearchEngineCompactMode,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // One-Handed Mode and Icon Pack Card
        CombinedLayoutIconCard(
                oneHandedMode = oneHandedMode,
                onToggleOneHandedMode = onToggleOneHandedMode,
                iconPackTitle =
                        androidx.compose.ui.res.stringResource(R.string.settings_icon_pack_title),
                iconPackDescription =
                        if (hasIconPacks) {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_icon_pack_selected_label,
                                    selectedIconPackLabel,
                            )
                        } else {
                            androidx.compose.ui.res.stringResource(
                                    R.string.settings_icon_pack_empty,
                            )
                        },
                onIconPackClick = {
                    if (hasIconPacks) {
                        showIconPackDialog = true
                    } else {
                        onSearchIconPacks()
                    }
                },
                onRefreshIconPacks = onRefreshIconPacks,
        )
    }

    // Icon Pack Picker Dialog
    if (showIconPackDialog) {
        IconPackPickerDialog(
                availableIconPacks = availableIconPacks,
                selectedPackage = selectedIconPackPackage,
                onSelect = { packageName: String? ->
                    onSelectIconPack(packageName)
                    showIconPackDialog = false
                },
                onDismiss = { showIconPackDialog = false },
        )
    }
}

@Composable
private fun OverlayThemeCard(
        selectedTheme: OverlayGradientTheme,
        overlayThemeIntensity: Float,
        onThemeSelected: (OverlayGradientTheme) -> Unit,
        onOverlayThemeIntensityChange: (Float) -> Unit,
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
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isThemeSourceSelected = backgroundSource == BackgroundSource.THEME
    val isWallpaperSourceSelected =
            backgroundSource == BackgroundSource.SYSTEM_WALLPAPER
    val isCustomSourceSelected = backgroundSource == BackgroundSource.CUSTOM_IMAGE

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

    val minIntensity = UiPreferences.MIN_OVERLAY_THEME_INTENSITY
    val maxIntensity = UiPreferences.MAX_OVERLAY_THEME_INTENSITY
    val intensityStep = UiPreferences.OVERLAY_THEME_INTENSITY_STEP
    val intensitySteps = (UiPreferences.OVERLAY_THEME_INTENSITY_DELTA_STEPS * 2) - 1
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
                        OverlayThemeOption(
                                theme = OverlayGradientTheme.MONOCHROME,
                                labelRes = R.string.settings_overlay_theme_monochrome,
                        ),
                        OverlayThemeOption(
                                theme = OverlayGradientTheme.FOREST,
                                labelRes = R.string.settings_overlay_theme_forest,
                        ),
                        OverlayThemeOption(
                                theme = OverlayGradientTheme.AURORA,
                                labelRes = R.string.settings_overlay_theme_aurora,
                        ),
                        OverlayThemeOption(
                                theme = OverlayGradientTheme.SUNSET,
                                labelRes = R.string.settings_overlay_theme_sunset,
                        ),
                )
            }

    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                    text = stringResource(R.string.settings_overlay_theme_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                themeOptions.forEach { option ->
                    val isSelected = selectedTheme == option.theme && isThemeSourceSelected
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
                                                onSetBackgroundSource(
                                                        BackgroundSource.THEME
                                                )
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
                                                                        overlayGradientColors(
                                                                                theme =
                                                                                        option.theme,
                                                                                isDarkMode =
                                                                                        isDarkMode,
                                                                                intensity =
                                                                                        overlayThemeIntensity,
                                                                        ),
                                                        ),
                                                )
                                                .border(
                                                        width =
                                                                if (isSelected) {
                                                                    2.dp
                                                                } else {
                                                                    1.dp
                                                                },
                                                        color =
                                                                if (isSelected) {
                                                                    MaterialTheme.colorScheme
                                                                            .primary
                                                                } else {
                                                                    MaterialTheme.colorScheme
                                                                            .outlineVariant
                                                                },
                                                        shape = MaterialTheme.shapes.medium,
                                                ),
                        )
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

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OverlaySourceBox(
                        modifier = Modifier.weight(1f),
                        selected = isWallpaperSourceSelected,
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
            } else {
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
            }
        }
    }
}

@Composable
private fun OverlaySourceBox(
        modifier: Modifier = Modifier,
        selected: Boolean,
        label: String,
        onClick: () -> Unit,
        content: @Composable BoxScope.() -> Unit,
) {
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
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable(onClick = onClick)
                                .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color =
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outlineVariant
                                                },
                                        shape = MaterialTheme.shapes.medium,
                                ),
                contentAlignment = Alignment.Center,
                content = content,
        )
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class OverlayThemeOption(
        val theme: OverlayGradientTheme,
        val labelRes: Int,
)

@Composable
private fun IconPackPickerDialog(
        availableIconPacks: List<IconPackInfo>,
        selectedPackage: String?,
        onSelect: (String?) -> Unit,
        onDismiss: () -> Unit,
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        text =
                                androidx.compose.ui.res.stringResource(
                                        R.string.settings_icon_pack_picker_title,
                                ),
                )
            },
            text = {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(max = 400.dp)
                                        .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (availableIconPacks.isEmpty()) {
                        Text(
                                text =
                                        androidx.compose.ui.res.stringResource(
                                                R.string.settings_icon_pack_empty,
                                        ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconPackOptionRow(
                                    label =
                                            androidx.compose.ui.res.stringResource(
                                                    R.string.settings_icon_pack_option_system,
                                            ),
                                    packageName = null,
                                    selected = selectedPackage == null,
                                    onClick = { onSelect(null) },
                            )
                            availableIconPacks.forEach { pack ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                IconPackOptionRow(
                                        label = pack.label,
                                        packageName = pack.packageName,
                                        selected = selectedPackage == pack.packageName,
                                        onClick = { onSelect(pack.packageName) },
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                            androidx.compose.ui.res.stringResource(R.string.dialog_done),
                    )
                }
            },
    )
}

@Composable
private fun IconPackOptionRow(
        label: String,
        packageName: String?,
        selected: Boolean,
        onClick: () -> Unit,
) {
    val iconBitmap = packageName?.let { rememberAppIcon(packageName = it).bitmap }
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.offset(x = (-4).dp),
        )
        if (iconBitmap != null) {
            Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(24.dp))
        } else {
            Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
        )
    }
}
