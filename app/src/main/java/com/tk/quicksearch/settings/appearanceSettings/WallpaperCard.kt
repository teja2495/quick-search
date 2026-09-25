package com.tk.quicksearch.settings.AppearanceSettings

import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
import com.tk.quicksearch.search.core.AccentColorMode
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.search.searchScreen.AppThemeColors
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.WidgetColorPickerDialog
import kotlin.math.roundToInt

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
        wallpaperAvailable: Boolean,
        onRequestWallpaperPermission: () -> Unit,
        accentColorMode: AccentColorMode,
        customAccentColorArgb: Int,
        onAccentColorModeChange: (AccentColorMode) -> Unit,
        onCustomAccentColorChange: (Int) -> Unit,
        deviceThemeEnabled: Boolean = false,
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val canShowSystemWallpaperBackdrop =
            (context as? HomeActivity)?.canShowSystemWallpaperBackdrop == true
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
                value = WallpaperUtils.getOverlayCustomImageBitmap(context, customImageUri)
            }

    val canDisplaySystemWallpaper = hasWallpaperPermission || canShowSystemWallpaperBackdrop
    val isWallpaperSourceSelected =
            backgroundSource == BackgroundSource.SYSTEM_WALLPAPER && canDisplaySystemWallpaper
    val isCustomSourceSelected = backgroundSource == BackgroundSource.CUSTOM_IMAGE
    val wallpaperPixelEffectsAvailable =
            isCustomSourceSelected ||
                    (backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                            hasWallpaperPermission &&
                            wallpaperAvailable)
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
    var showCustomAccentPicker by rememberSaveable { mutableStateOf(false) }

    val selectAccentMode: (AccentColorMode) -> Unit = { mode ->
        val wallpaperAccentAvailable =
                (backgroundSource == BackgroundSource.SYSTEM_WALLPAPER &&
                        hasWallpaperPermission &&
                        wallpaperAvailable) ||
                        isCustomSourceSelected
        val requiresOverride = mode != AccentColorMode.NONE
        if (mode == AccentColorMode.FROM_WALLPAPER && !wallpaperAccentAvailable) {
            Toast.makeText(
                            context,
                            context.getString(R.string.settings_accent_color_wallpaper_required_toast),
                            Toast.LENGTH_SHORT,
                    )
                    .show()
        } else if (requiresOverride && deviceThemeEnabled) {
            Toast.makeText(
                            context,
                            context.getString(R.string.settings_device_theme_blocked_toast),
                            Toast.LENGTH_SHORT,
                    )
                    .show()
        } else {
            hapticToggle(view)()
            if (mode == AccentColorMode.CUSTOM) {
                showCustomAccentPicker = true
            } else if (accentColorMode != mode) {
                onAccentColorModeChange(mode)
            }
        }
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
                if (!shouldHideSystemWallpaperSource) {
                    OverlaySourceBox(
                            modifier = Modifier.weight(1f),
                            selected = isWallpaperSourceSelected,
                            enabled = true,
                            hasImage = wallpaperPreviewBitmap != null,
                            onClick = {
                                hapticToggle(view)()
                                if (isWallpaperSourceSelected) {
                                    onSetBackgroundSource(BackgroundSource.THEME)
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
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription =
                                                stringResource(
                                                        R.string.permission_required_title,
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
                        } else if (canShowSystemWallpaperBackdrop) {
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

                OverlaySourceBox(
                        modifier = Modifier.weight(1f),
                        selected = isCustomSourceSelected,
                        enabled = true,
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
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(AppColors.CustomImageEditButtonBackground)
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
                                    tint = AppColors.CustomImageEditButtonIcon,
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
                            modifier = Modifier.weight(1f),
                    )
                    Text(
                            text = "${(wallpaperAlphaDisplayValue * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.widthIn(min = 48.dp),
                    )
                }

                if (wallpaperPixelEffectsAvailable) {
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

            Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                Text(
                        text = stringResource(R.string.settings_wallpaper_accent_title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                )
                if (wallpaperPixelEffectsAvailable) {
                    Text(
                            text = stringResource(R.string.settings_wallpaper_accent_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                        modifier = Modifier.fillMaxWidth().selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppModeOption(
                            label = stringResource(R.string.settings_accent_color_none),
                            icon = Icons.Rounded.Block,
                            selected = accentColorMode == AccentColorMode.NONE,
                            onClick = { selectAccentMode(AccentColorMode.NONE) },
                            iconLabelSpacing = DesignTokens.SpacingSmall,
                            compactLabel = true,
                            modifier = Modifier.weight(1f),
                    )
                    if (wallpaperPixelEffectsAvailable) {
                        AppModeOption(
                                label = stringResource(R.string.settings_accent_color_from_wallpaper),
                                icon = Icons.Rounded.Image,
                                selected = accentColorMode == AccentColorMode.FROM_WALLPAPER,
                                onClick = { selectAccentMode(AccentColorMode.FROM_WALLPAPER) },
                                iconLabelSpacing = DesignTokens.SpacingSmall,
                                compactLabel = true,
                                modifier = Modifier.weight(1f),
                        )
                    }
                    AppModeOption(
                            label = stringResource(R.string.common_custom),
                            icon = Icons.Rounded.Palette,
                            selected = accentColorMode == AccentColorMode.CUSTOM,
                            onClick = { selectAccentMode(AccentColorMode.CUSTOM) },
                            iconLabelSpacing = DesignTokens.SpacingSmall,
                            compactLabel = true,
                            swatchColor =
                                    if (accentColorMode == AccentColorMode.CUSTOM) {
                                        Color(customAccentColorArgb)
                                    } else {
                                        null
                                    },
                            modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    if (showCustomAccentPicker) {
        WidgetColorPickerDialog(
                initialColor = Color(customAccentColorArgb),
                onDismiss = { showCustomAccentPicker = false },
                onConfirm = { color ->
                    onCustomAccentColorChange(color.toArgb())
                    if (accentColorMode != AccentColorMode.CUSTOM) {
                        onAccentColorModeChange(AccentColorMode.CUSTOM)
                    }
                    showCustomAccentPicker = false
                },
                title = stringResource(R.string.settings_accent_color_picker_title),
        )
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
