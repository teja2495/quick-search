package com.tk.quicksearch.settings.appearanceSettings

import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.components.SliderWithValue
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.shared.ui.components.AppSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.tk.quicksearch.R
import com.tk.quicksearch.app.HomeActivity
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.ExpandableSegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.theme.AppShapes

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
        onRemoveCustomImage: () -> Unit,
        hasWallpaperPermission: Boolean,
        wallpaperAvailable: Boolean,
        onRequestWallpaperPermission: () -> Unit,
        accentColorMode: AccentColorMode,
        customAccentColorArgb: Int,
        onAccentColorModeChange: (AccentColorMode) -> Unit,
        onCustomAccentColorChange: (Int) -> Unit,
        appTheme: AppTheme,
        overlayThemeIntensity: Float,
        amoledThemeEnabled: Boolean,
        deviceThemeEnabled: Boolean = false,
        expanded: Boolean = true,
        onExpandedChange: (Boolean) -> Unit = {},
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    // Search draws the system wallpaper behind its window without the wallpaper permission, so the
    // source is usable here too; only ask for the permission when the wallpaper can't be shown.
    val canShowSystemWallpaperBackdrop =
            (context as? MainActivity)?.canShowSystemWallpaperBackdrop == true
    val isDarkMode = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val wallpaperPreviewBitmap by
            produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                    initialValue = null,
                    key1 = hasWallpaperPermission,
                    key2 = backgroundSource,
            ) {
                value =
                        if (hasWallpaperPermission) {
                            WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                                    ?: WallpaperUtils.getWallpaperBitmap(context)?.asImageBitmap()
                        } else {
                            null
                        }
            }
    val customPreviewBitmap by
            produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                    initialValue = null,
                    key1 = customImageUri,
            ) {
                value =
                        WallpaperUtils.getCachedOverlayCustomImageBitmap(customImageUri)
                                ?: WallpaperUtils.getOverlayCustomImageBitmap(context, customImageUri)
            }

    val isLiveWallpaperActive by
            produceState(initialValue = false) {
                value = withContext(Dispatchers.IO) { WallpaperUtils.isLiveWallpaperActive(context) }
            }
    // Android composites a live wallpaper behind Home, but never hands its pixels to apps.
    val isLiveWallpaperUnsupported =
            isLiveWallpaperActive && hasWallpaperPermission && !canShowSystemWallpaperBackdrop
    val canDisplaySystemWallpaper = hasWallpaperPermission || canShowSystemWallpaperBackdrop
    val isWallpaperSourceSelected =
            backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                    canDisplaySystemWallpaper &&
                    !isLiveWallpaperUnsupported
    val isCustomSourceSelected = backgroundSource == BackgroundSource.CUSTOM_IMAGE
    // Home always has the wallpaper behind its window; elsewhere it is only there while it is the
    // selected source, and the preview would otherwise show the app underneath.
    val canPreviewSystemWallpaperBackdrop =
            canShowSystemWallpaperBackdrop && (context is HomeActivity || isWallpaperSourceSelected)
    // A wallpaper source that can't be shown falls back to the theme gradient.
    val isThemeSourceSelected = !isWallpaperSourceSelected && !isCustomSourceSelected
    val wallpaperPixelEffectsAvailable =
            hasWallpaperColorSource(backgroundSource, hasWallpaperPermission, wallpaperAvailable)
    val shouldHideSystemWallpaperSource =
            !canShowSystemWallpaperBackdrop &&
                    !hasWallpaperPermission &&
                    PermissionHelper.checkFilesPermission(context)

    val wallpaperAlphaDisplayValue = (wallpaperBackgroundAlpha / 0.7f).coerceIn(0f, 1f)
    var lastAlphaStep by remember {
        mutableStateOf((wallpaperAlphaDisplayValue * 9).roundToInt().coerceIn(0, 9))
    }
    var lastBlurStep by remember {
        mutableStateOf(
                (wallpaperBlurRadius / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS * 7)
                        .roundToInt()
                        .coerceIn(0, 7),
        )
    }

    ExpandableSegmentedGroup(
            title = stringResource(R.string.settings_wallpaper_card_title),
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            tileCount = 2,
            modifier = modifier,
            firstTileContent = {

                // The gradient of whichever theme is selected (Default or a Colorful palette).
                val themePreviewColors =
                        if (deviceThemeEnabled) {
                            listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.secondaryContainer,
                            )
                        } else {
                            themeOptionPreviewColors(
                                    isCustom = appTheme != AppTheme.MONOCHROME,
                                    selectedTheme = appTheme,
                                    isDarkMode = isDarkMode,
                                    intensity = overlayThemeIntensity,
                                    amoledPreviewActive = amoledThemeEnabled && isDarkMode,
                            )
                        }
                val themeTile: @Composable (Modifier, Dp?) -> Unit = { tileModifier, tileHeight ->
                BackgroundSourceTile(
                        tileHeight = tileHeight,
                        hasImage = true,
                        modifier = tileModifier,
                        selected = isThemeSourceSelected,
                        onClick = {
                            if (!isThemeSourceSelected) {
                                hapticToggle(view)()
                                onSetBackgroundSource(BackgroundSource.THEME)
                            }
                        },
                        label = stringResource(R.string.settings_app_theme_title),
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(themePreviewColors)))
                }
                }

                val wallpaperTile: @Composable (Modifier) -> Unit = { tileModifier ->
                    BackgroundSourceTile(
                            modifier = tileModifier,
                            selected = isWallpaperSourceSelected,
                            hasImage = canDisplaySystemWallpaper && !isLiveWallpaperUnsupported,
                            onClick = {
                                if (isWallpaperSourceSelected) return@BackgroundSourceTile
                                hapticToggle(view)()
                                if (isLiveWallpaperUnsupported) {
                                    AppMessages.show(context, context.getString(
                                                            R.string.settings_overlay_source_live_wallpaper_unsupported,
                                                    ))
                                } else if (canShowSystemWallpaperBackdrop) {
                                    onSetBackgroundSource(BackgroundSource.SYSTEM_WALLPAPER)
                                } else {
                                    onRequestWallpaperPermission()
                                }
                            },
                            label = stringResource(R.string.settings_overlay_source_wallpaper),
                    ) {
                        if (!canDisplaySystemWallpaper) {
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                            ) {
                                Icon(
                                        imageVector = MaterialSymbols.Info,
                                        contentDescription =
                                                stringResource(
                                                        R.string.permission_required_title,
                                                ),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(DesignTokens.ButtonIconSizeExtraSmall),
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
                        } else if (isLiveWallpaperUnsupported) {
                            Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = DesignTokens.SpacingSmall),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement =
                                            Arrangement.spacedBy(DesignTokens.SpacingXSmall, Alignment.CenterHorizontally),
                            ) {
                                Icon(
                                        imageVector = MaterialSymbols.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(DesignTokens.ButtonIconSizeExtraSmall),
                                )
                                Text(
                                        text =
                                                stringResource(
                                                        R.string.settings_overlay_source_live_wallpaper_unsupported,
                                                ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        autoSize =
                                                TextAutoSize.StepBased(
                                                        minFontSize = 8.sp,
                                                        maxFontSize =
                                                                MaterialTheme.typography.labelSmall.fontSize,
                                                ),
                                )
                            }
                        } else if (wallpaperPreviewBitmap != null) {
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
                                                                    AppColors.WallpaperOverlayTint.copy(
                                                                            alpha = wallpaperBackgroundAlpha,
                                                                    ),
                                                            ),
                                    )
                                }
                            }
                        } else if (canPreviewSystemWallpaperBackdrop) {
                            SystemWallpaperBackdropPreview()
                        } else {
                            Box(
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .background(
                                                            Brush.linearGradient(
                                                                    colors =
                                                                            listOf(
                                                                                    MaterialTheme.colorScheme.primaryContainer,
                                                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                                            ),
                                                            ),
                                                    ),
                            )
                        }
                    }
                }

                val customTile: @Composable (Modifier) -> Unit = { tileModifier ->
                BackgroundSourceTile(
                        modifier = tileModifier,
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
                        label = stringResource(R.string.common_custom),
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
                                                                AppColors.WallpaperOverlayTint.copy(
                                                                        alpha = wallpaperBackgroundAlpha,
                                                                ),
                                                        ),
                                )
                            }
                        }
                        Row(
                                modifier = Modifier.align(Alignment.TopEnd).padding(DesignTokens.SpacingSmall),
                                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        ) {
                            Box(
                                    modifier =
                                            Modifier.size(20.dp)
                                                    .clip(AppShapes.full)
                                                    .background(AppColors.CustomImageEditButtonBackground)
                                                    // Compose expands this small target to the 48dp minimum at hit-test time.
                                                    .clickable(role = Role.Button) {
                                                        hapticToggle(view)()
                                                        onPickCustomImage()
                                                    },
                                    contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                        imageVector = MaterialSymbols.Edit,
                                        contentDescription =
                                                stringResource(
                                                        R.string.settings_overlay_source_edit_custom
                                                ),
                                        tint = AppColors.CustomImageEditButtonIcon,
                                        modifier = Modifier.size(12.dp),
                                )
                            }
                            Box(
                                    modifier =
                                            Modifier.size(20.dp)
                                                    .clip(AppShapes.full)
                                                    .background(AppColors.CustomImageEditButtonBackground)
                                                    // Compose expands this small target to the 48dp minimum at hit-test time.
                                                    .clickable(role = Role.Button) {
                                                        hapticToggle(view)()
                                                        if (isCustomSourceSelected) {
                                                            onSetBackgroundSource(
                                                                    if (
                                                                            canDisplaySystemWallpaper &&
                                                                                    !isLiveWallpaperUnsupported &&
                                                                                    !shouldHideSystemWallpaperSource
                                                                    ) {
                                                                        BackgroundSource.SYSTEM_WALLPAPER
                                                                    } else {
                                                                        BackgroundSource.THEME
                                                                    },
                                                            )
                                                        }
                                                        onRemoveCustomImage()
                                                    },
                                    contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                        imageVector = MaterialSymbols.Close,
                                        contentDescription =
                                                stringResource(
                                                        R.string.settings_overlay_source_remove_custom
                                                ),
                                        tint = AppColors.CustomImageEditButtonIcon,
                                        modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    } else {
                        Icon(
                                imageVector = MaterialSymbols.Add,
                                contentDescription =
                                        stringResource(R.string.settings_overlay_source_add_custom),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                }

                // Bento layout: the wallpaper is the large tile, with Custom above a short Theme
                // tile beside it. Without a wallpaper source, Custom takes the large slot.
                Row(
                        modifier = Modifier.fillMaxWidth().height(SOURCE_GRID_HEIGHT),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    if (!shouldHideSystemWallpaperSource) {
                        wallpaperTile(Modifier.weight(HERO_SOURCE_TILE_WEIGHT).fillMaxHeight())
                        Column(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        ) {
                            customTile(Modifier.fillMaxWidth().weight(1f))
                            themeTile(Modifier.fillMaxWidth(), THEME_SOURCE_TILE_HEIGHT)
                        }
                    } else {
                        customTile(Modifier.weight(HERO_SOURCE_TILE_WEIGHT).fillMaxHeight())
                        themeTile(Modifier.weight(1f).fillMaxHeight(), null)
                    }
                }

            if (isWallpaperSourceSelected || isCustomSourceSelected) {
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXSmall))
                Text(
                        text = stringResource(R.string.settings_wallpaper_transparency_label),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                )
                SliderWithValue(
                        valueSpacing = DesignTokens.SpacingLarge,
                        valueLabel = {
                                Text(
                                        text = "${(wallpaperAlphaDisplayValue * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.widthIn(min = 48.dp),
                                )
                        },
                ) { sliderModifier ->
                    AppSlider(
                            value = wallpaperAlphaDisplayValue,
                            onValueChange = { value ->
                                val step = (value * 9).roundToInt().coerceIn(0, 9)
                                if (step != lastAlphaStep) {
                                    hapticToggle(view)()
                                    lastAlphaStep = step
                                }
                                onWallpaperBackgroundAlphaChange(value * 0.7f)
                            },
                            valueRange = 0f..1f,
                            steps = 9,
                            modifier = sliderModifier,
                    )
                }

                if (wallpaperPixelEffectsAvailable) {
                    Text(
                            text = stringResource(R.string.settings_wallpaper_blur_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                    )
                    SliderWithValue(
                            valueSpacing = DesignTokens.SpacingLarge,
                            valueLabel = {
                                    Text(
                                            text =
                                                    "${((wallpaperBlurRadius / UiPreferences.MAX_WALLPAPER_BLUR_RADIUS) * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.widthIn(min = 48.dp),
                                    )
                            },
                    ) { sliderModifier ->
                        AppSlider(
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
                                modifier = sliderModifier,
                        )
                    }
                }

            }
            },
    ) {
      SegmentedTile(index = 1, count = 2) {
        SegmentedTileContent {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpacingXSmall),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                Text(
                        text = stringResource(R.string.settings_wallpaper_accent_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                )
                if (wallpaperPixelEffectsAvailable) {
                    Text(
                            text = stringResource(R.string.settings_wallpaper_accent_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AccentColorChoices(
                        accentColorMode = accentColorMode,
                        customAccentColorArgb = customAccentColorArgb,
                        wallpaperAccentAvailable = wallpaperPixelEffectsAvailable,
                        appTheme = appTheme,
                        deviceThemeEnabled = deviceThemeEnabled,
                        onAccentColorModeChange = onAccentColorModeChange,
                        onCustomAccentColorChange = onCustomAccentColorChange,
                )
            }
        }
      }
    }

}

@Composable
private fun SystemWallpaperBackdropPreview() {
    AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.setFormat(PixelFormat.TRANSLUCENT)
                    setZOrderMediaOverlay(true)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    holder.addCallback(
                            object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    clearWallpaperPreviewSurface(holder)
                                }

                                override fun surfaceChanged(
                                        holder: SurfaceHolder,
                                        format: Int,
                                        width: Int,
                                        height: Int,
                                ) {
                                    clearWallpaperPreviewSurface(holder)
                                }

                                override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
                            },
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
    )
}

private fun clearWallpaperPreviewSurface(holder: SurfaceHolder) {
    val canvas = holder.lockCanvas() ?: return
    try {
        canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    } finally {
        holder.unlockCanvasAndPost(canvas)
    }
}

/** Bento grid of background sources: a large hero tile beside Custom stacked over a short Theme tile. */
private val SOURCE_GRID_HEIGHT = 168.dp
private val THEME_SOURCE_TILE_HEIGHT = 48.dp
private const val HERO_SOURCE_TILE_WEIGHT = 1.4f
