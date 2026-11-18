package com.tk.quicksearch.widgets.utils

import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonIcon
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsWidgetPreview
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetPreview
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetPreview
import com.tk.quicksearch.widgets.searchWidget.MicAction

/** The home screen wallpaper to draw widget previews on, or null without wallpaper access. */
@Composable
fun rememberWidgetPreviewWallpaper(): ImageBitmap? {
    val context = LocalContext.current
    val hasWallpaperAccess = remember(context) { WallpaperUtils.hasWallpaperAccessPermission(context) }
    val wallpaperBitmap by
        produceState<ImageBitmap?>(
            initialValue =
                if (hasWallpaperAccess) {
                    WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                } else {
                    null
                },
            key1 = hasWallpaperAccess,
        ) {
            value =
                if (hasWallpaperAccess) {
                    WallpaperUtils.getCachedWallpaperBitmap()?.asImageBitmap()
                        ?: WallpaperUtils.getWallpaperBitmap(context)?.asImageBitmap()
                } else {
                    null
                }
        }
    return wallpaperBitmap
}

@Composable
fun WidgetPreviewCard(
    state: WidgetPreferences,
    widgetVariant: WidgetVariant = WidgetVariant.STANDARD,
    /** The launcher's scale for the placed widget's content; see [launcherWidgetContentScale]. */
    launcherContentScale: Float = 1f,
) {
    val previewState = state.enforceVariantConstraints(widgetVariant)
    val context = LocalContext.current
    val wallpaperBitmap = rememberWidgetPreviewWallpaper()
    if (widgetVariant == WidgetVariant.MEDIA_CONTROLS) {
        MediaControlsWidgetPreview(state = previewState, wallpaperBitmap = wallpaperBitmap)
        return
    }
    if (widgetVariant == WidgetVariant.NOTE) {
        NoteWidgetPreview(state = previewState, wallpaperBitmap = wallpaperBitmap)
        return
    }
    if (widgetVariant == WidgetVariant.AT_A_GLANCE) {
        AtAGlanceWidgetPreview(
            state = previewState,
            wallpaperBitmap = wallpaperBitmap,
            launcherContentScale = launcherContentScale,
        )
        return
    }
    val colors = calculatePreviewColors(previewState)
    val borderShape = RoundedCornerShape(previewState.borderRadiusDp.dp)
    val shouldShowBorder = previewState.borderWidthDp >= WidgetConfigConstants.BORDER_VISIBILITY_THRESHOLD
    val iconPackPackage =
        remember(context) {
            UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        }
    val previewWidth = WidgetLayoutUtils.DEFAULT_WIDTH_DP.dp
    val outerHorizontalPadding =
        computeSafePreviewOuterPadding(
            previewWidth = previewWidth,
            requestedPaddingDp = previewState.internalHorizontalPaddingDp,
        )
    val verticalInset = previewState.internalVerticalPaddingDp.finiteOr(0f).dp
    val innerHorizontalPadding = WidgetConfigConstants.PREVIEW_INNER_PADDING
    val previewBarHeight = (WidgetConfigConstants.PREVIEW_HEIGHT - (verticalInset * 2)).coerceAtLeast(1.dp)
    val customButtons = previewState.customButtons.filterNotNull()
    val iconSize = WidgetConfigConstants.PREVIEW_ICON_SIZE * previewState.iconSizeScale
    val showMic = previewState.micAction != MicAction.OFF
    val searchShortcuts = if (widgetVariant == WidgetVariant.STANDARD) {
        customButtons.take(
            WidgetLayoutUtils.searchShortcutCapacity(
                widgetWidth = previewWidth,
                innerWidth = previewWidth - outerHorizontalPadding * 2,
                showLabel = previewState.showLabel,
                showSearchIcon = previewState.showSearchIcon,
                iconSize = iconSize,
                showMic = showMic,
            ),
        )
    } else {
        emptyList()
    }
    val searchActionsWidth = WidgetLayoutUtils.searchActionsWidth(searchShortcuts.size + if (showMic) 1 else 0)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(previewBarHeight + (PREVIEW_WALLPAPER_VERTICAL_PADDING * 2))
                .clip(AppShapes.large)
                .then(
                    wallpaperBitmap?.let { bitmap ->
                        Modifier.paint(
                            painter = BitmapPainter(bitmap),
                            sizeToIntrinsics = false,
                            contentScale = ContentScale.Crop,
                        )
                    } ?: Modifier,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = outerHorizontalPadding + PREVIEW_WALLPAPER_HORIZONTAL_PADDING,
                        end = outerHorizontalPadding + PREVIEW_WALLPAPER_HORIZONTAL_PADDING,
                    )
                    .height(previewBarHeight)
                    .background(colors.background, shape = borderShape)
                    .then(
                        if (shouldShowBorder) {
                            Modifier.border(
                                width = previewState.borderWidthDp.dp,
                                color = colors.border,
                                shape = borderShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
        ) {
            if (widgetVariant == WidgetVariant.STANDARD && previewState.iconAlignLeft) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = innerHorizontalPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    if (previewState.showLabel) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(
                                start = if (searchActionsWidth > 0.dp && previewState.showSearchIcon) iconSize else 0.dp,
                                end = searchActionsWidth,
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                color = colors.textIcon,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                        }
                    }

                    if (previewState.showSearchIcon) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_widget_search),
                                contentDescription = stringResource(R.string.common_search),
                                tint = colors.textIcon,
                                modifier = Modifier.size(iconSize),
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                start = innerHorizontalPadding,
                                end = innerHorizontalPadding + searchActionsWidth,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (widgetVariant == WidgetVariant.STANDARD && previewState.showSearchIcon) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_widget_search),
                            contentDescription = stringResource(R.string.common_search),
                            tint = colors.textIcon,
                            modifier = Modifier.size(iconSize),
                        )
                    }
                    if (widgetVariant == WidgetVariant.STANDARD && previewState.showLabel) {
                        Text(
                            text = stringResource(R.string.app_name),
                            modifier =
                                Modifier.padding(
                                    start =
                                        if (previewState.showSearchIcon) {
                                            WidgetConfigConstants.PREVIEW_ICON_TEXT_SPACING
                                        } else {
                                            0.dp
                                        },
                                ),
                            color = colors.textIcon,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            when (widgetVariant) {
                WidgetVariant.STANDARD -> {
                    if (searchShortcuts.isNotEmpty() || showMic) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(end = WidgetLayoutUtils.searchActionsInset),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(WidgetLayoutUtils.searchActionsGap),
                            ) {
                                searchShortcuts.forEach { action ->
                                    Box(
                                        modifier = Modifier.size(DesignTokens.MinTouchTarget),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CustomWidgetButtonIcon(
                                            action = action,
                                            iconSize = iconSize,
                                            iconPackPackage = iconPackPackage,
                                            tintColor = colors.textIcon,
                                        )
                                    }
                                }
                                if (showMic) {
                                    Box(
                                        modifier = Modifier.size(DesignTokens.MinTouchTarget),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_widget_mic),
                                            contentDescription = stringResource(R.string.desc_voice_search_icon),
                                            tint = colors.textIcon,
                                            modifier = Modifier.size(iconSize),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                WidgetVariant.CUSTOM_BUTTONS_ONLY -> {
                    val placeholderIcons =
                        listOf(
                            R.drawable.ic_widget_contact,
                            R.drawable.ic_widget_apps,
                            R.drawable.ic_widget_folder,
                            R.drawable.ic_widget_file,
                            R.drawable.ic_widget_shortcut,
                            R.drawable.ic_widget_settings,
                        )
                    val touchSpace = DesignTokens.MinTouchTarget
                    val availableWidth = previewWidth - (outerHorizontalPadding + WidgetLayoutUtils.customButtonsInset) * 2
                    val capacity = WidgetLayoutUtils.buttonCapacity(
                        availableWidth, WidgetLayoutUtils.customButtonsGap(customButtons.size, previewWidth),
                    )
                    val visibleButtons = customButtons.take(capacity)
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = WidgetLayoutUtils.customButtonsInset),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            if (visibleButtons.isNotEmpty()) {
                                visibleButtons.forEach { action ->
                                    Box(
                                        modifier = Modifier.size(touchSpace),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CustomWidgetButtonIcon(
                                            action = action,
                                            iconSize = iconSize,
                                            iconPackPackage = iconPackPackage,
                                            tintColor = colors.textIcon,
                                        )
                                    }
                                }
                            } else {
                                placeholderIcons.take(capacity).forEach { iconRes ->
                                    Box(
                                        modifier = Modifier.size(touchSpace),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            tint = colors.textIcon,
                                            modifier = Modifier.size(iconSize),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                WidgetVariant.MEDIA_CONTROLS,
                WidgetVariant.NOTE,
                WidgetVariant.AT_A_GLANCE,
                -> Unit
            }
        }
    }
}

private fun computeSafePreviewOuterPadding(
    previewWidth: androidx.compose.ui.unit.Dp,
    requestedPaddingDp: Float,
): androidx.compose.ui.unit.Dp {
    val safeWidth = previewWidth.value.finiteOr(WidgetLayoutUtils.DEFAULT_WIDTH_DP).coerceAtLeast(1f)
    val requested = requestedPaddingDp.finiteOr(0f).coerceAtLeast(0f)
    val maxPadding = ((safeWidth - PREVIEW_MIN_RENDERABLE_WIDTH_DP) / 2f).coerceAtLeast(0f)
    return requested.coerceAtMost(maxPadding).dp
}

private fun Float.finiteOr(default: Float): Float = if (isFinite()) this else default

private const val PREVIEW_MIN_RENDERABLE_WIDTH_DP = 48f
private val PREVIEW_WALLPAPER_HORIZONTAL_PADDING = 8.dp
private val PREVIEW_WALLPAPER_VERTICAL_PADDING = 16.dp

private data class PreviewColors(
    val background: Color,
    val border: Color,
    val textIcon: Color,
)

@Composable
private fun calculatePreviewColors(state: WidgetPreferences): PreviewColors {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    // Determine effective theme based on user selection
    val effectiveTheme =
        when (state.theme) {
            WidgetTheme.SYSTEM -> if (isSystemInDarkTheme) WidgetTheme.DARK else WidgetTheme.LIGHT
            else -> state.theme
        }

    val customBackgroundColor = state.backgroundColor?.let(::Color)
    val deviceThemeBackgroundColor =
        if (state.useDeviceThemeBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isSystemInDarkTheme) {
                dynamicDarkColorScheme(context).primary
            } else {
                dynamicLightColorScheme(context).primary
            }
        } else {
            null
        }
    val resolvedCustomBackgroundColor = deviceThemeBackgroundColor ?: customBackgroundColor
    val background =
        resolvedCustomBackgroundColor?.copy(alpha = state.backgroundAlpha)
            ?: WidgetColorUtils.getBackgroundColor(
                effectiveTheme,
                state.backgroundAlpha,
            )
    val border =
        if (state.borderColorOption == BorderColorOption.DEVICE_THEME && deviceThemeBackgroundColor != null) {
            deviceThemeBackgroundColor.copy(alpha = state.borderAlpha.coerceAtMost(0.4f))
        } else {
            WidgetColorUtils.getBorderColor(
                state.borderColor,
                state.borderAlpha,
                effectiveTheme,
                state.borderColorOption,
            )
        }
    val textIcon =
        WidgetColorUtils.getTextIconColor(
            state.theme,
            state.backgroundAlpha,
            state.textIconColorOverride,
            customBackgroundColor = resolvedCustomBackgroundColor,
            isSystemInDarkTheme,
        )

    return PreviewColors(
        background = background,
        border = border,
        textIcon = textIcon,
    )
}
