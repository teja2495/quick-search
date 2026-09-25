package com.tk.quicksearch.widgets.searchWidget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.background
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.unit.ColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.app.MainActivity
import com.tk.quicksearch.media.MediaCommand
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetMediaAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
import com.tk.quicksearch.widgets.customButtonsWidget.playPauseIconRes
import com.tk.quicksearch.widgets.customButtonsWidget.rememberWidgetButtonIcon
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.utils.BorderColorOption
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import com.tk.quicksearch.widgets.utils.WidgetColorUtils
import com.tk.quicksearch.widgets.utils.WidgetDefaults
import com.tk.quicksearch.widgets.utils.WidgetLayoutUtils
import com.tk.quicksearch.widgets.utils.WidgetTheme
import com.tk.quicksearch.widgets.utils.applyWidgetPreferences
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Media commands run in-process via [CustomButtonsWidgetMediaAction] instead of launching
 * [WidgetActionActivity], since prev/next are tapped repeatedly and an activity launch on every
 * tap would be slow and would flash over whatever the user is looking at.
 */
private fun Context.customButtonWidgetClickAction(action: CustomWidgetButtonAction): Action =
    if (action is CustomWidgetButtonAction.Media) {
        actionRunCallback<CustomButtonsWidgetMediaAction>(
            actionParametersOf(CustomButtonsWidgetMediaAction.MEDIA_COMMAND_KEY to action.command.value),
        )
    } else {
        actionStartActivity(WidgetActionActivity.createIntent(this, action))
    }

/**
 * Extra `remember` key for [rememberWidgetButtonIcon]: a Play/Pause button's icon depends on live
 * playback state, not just [action] itself, so without this the icon would be cached from the
 * button's first render and never flip again across the redraws that a playback change triggers.
 */
private fun Context.mediaButtonPlaybackKey(action: CustomWidgetButtonAction): Int? =
    if (action is CustomWidgetButtonAction.Media && action.command == MediaCommand.PLAY_PAUSE) {
        playPauseIconRes(this)
    } else {
        null
    }

@Composable
internal fun CustomButtonsOnlyWidgetContent(
    widthDp: Dp,
    heightDp: Dp,
    backgroundBitmap: Bitmap?,
    useDefaultBackground: Boolean,
    textIconColor: Color,
    textIconColorProvider: ColorProvider,
    backgroundColorProvider: ColorProvider,
    borderColorProvider: ColorProvider?,
    borderWidthDp: Dp,
    cornerRadius: Dp,
    iconSize: Dp,
    internalHorizontalPaddingDp: Float,
    internalVerticalPaddingDp: Float,
    customButtons: List<CustomWidgetButtonAction>,
) {
    val context = LocalContext.current
    val iconPackPackage =
        remember(context) {
            UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        }
    val density = context.resources.displayMetrics.density
    val iconSizePx = (iconSize.value * density).roundToInt().coerceAtLeast(1)
    val useCompactSpacing = customButtons.size >= 5 || widthDp <= WidgetLayoutUtils.DEFAULT_WIDTH_DP.dp
    val touchSpace = if (useCompactSpacing) 28.dp else 36.dp
    val outerHorizontalPadding =
        computeSafeOuterHorizontalPadding(
            widthDp = widthDp,
            requestedPaddingDp = internalHorizontalPaddingDp,
        )
    val verticalInset = internalVerticalPaddingDp.finiteOr(0f).dp
    val contentHorizontalPadding = 16.dp
    val barHeight = (heightDp - (verticalInset * 2)).coerceAtLeast(1.dp)
    val minimumGap = if (useCompactSpacing) 4.dp else 8.dp
    val containerWidth = (widthDp.value - (outerHorizontalPadding.value * 2f)).coerceAtLeast(0f)
    val availableWidth = (containerWidth - (contentHorizontalPadding.value * 2f)).coerceAtLeast(0f)
    val maxVisibleButtons =
        if (customButtons.isEmpty()) {
            0
        } else {
            floor((availableWidth - minimumGap.value) / (touchSpace.value + minimumGap.value))
                .toInt()
                .coerceIn(0, customButtons.size)
        }
    val visibleButtons = customButtons.take(maxVisibleButtons)
    val gapWidth =
        if (visibleButtons.isEmpty()) {
            0.dp
        } else {
            val totalButtonWidth = touchSpace.value * visibleButtons.size
            val available =
                (containerWidth - (contentHorizontalPadding.value * 2f) - totalButtonWidth).coerceAtLeast(0f)
            (available / (visibleButtons.size + 1)).dp
        }
    val buttonHorizontalPadding = (gapWidth.value / 2f).dp

    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(0.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    GlanceModifier
                        .fillMaxSize()
                        .padding(start = outerHorizontalPadding, end = outerHorizontalPadding),
                contentAlignment = Alignment.Center,
            ) {
                WidgetBarContainer(
                    barHeight = barHeight,
                    backgroundBitmap = backgroundBitmap,
                    useDefaultBackground = useDefaultBackground,
                    backgroundColorProvider = backgroundColorProvider,
                    borderColorProvider = borderColorProvider,
                    borderWidthDp = borderWidthDp,
                    cornerRadius = cornerRadius,
                    contentHorizontalPadding = contentHorizontalPadding,
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        visibleButtons.forEach { action ->
                            val icon =
                                remember(
                                    action,
                                    iconPackPackage,
                                    iconSizePx,
                                    textIconColor,
                                    context.mediaButtonPlaybackKey(action),
                                ) {
                                    rememberWidgetButtonIcon(
                                        context = context,
                                        action = action,
                                        iconSizePx = iconSizePx,
                                        textIconColor = textIconColor,
                                        iconPackPackage = iconPackPackage,
                                    )
                                }
                            Box(
                                modifier =
                                    GlanceModifier
                                        .padding(horizontal = buttonHorizontalPadding),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier =
                                        GlanceModifier
                                            .size(touchSpace)
                                            .clickable(
                                                onClick = context.customButtonWidgetClickAction(action),
                                                rippleOverride = android.R.color.transparent,
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val imageProvider =
                                        when {
                                            icon.bitmap != null -> ImageProvider(icon.bitmap)
                                            icon.drawableResId != null -> ImageProvider(icon.drawableResId)
                                            else -> ImageProvider(R.drawable.ic_widget_search)
                                        }
                                    Image(
                                        provider = imageProvider,
                                        contentDescription = action.contentDescription(),
                                        modifier = GlanceModifier.size(iconSize),
                                        colorFilter =
                                            if (icon.shouldTint) {
                                                ColorFilter.tint(textIconColorProvider)
                                            } else {
                                                null
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun WidgetContent(
    widthDp: Dp,
    heightDp: Dp,
    backgroundBitmap: Bitmap?,
    useDefaultBackground: Boolean,
    textIconColor: Color,
    textIconColorProvider: ColorProvider,
    backgroundColorProvider: ColorProvider,
    borderColorProvider: ColorProvider?,
    borderWidthDp: Dp,
    cornerRadius: Dp,
    iconSize: Dp,
    showLabel: Boolean,
    showSearchIcon: Boolean,
    showMicIcon: Boolean,
    iconAlignLeft: Boolean,
    internalHorizontalPaddingDp: Float,
    internalVerticalPaddingDp: Float,
    launchIntent: Intent,
    voiceLaunchIntent: Intent,
    customButtons: List<CustomWidgetButtonAction>,
) {
    val context = LocalContext.current
    val micTouchSpace = 36.dp
    val iconPackPackage =
        remember(context) {
            UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
        }
    val density = context.resources.displayMetrics.density
    val iconSizePx = (iconSize.value * density).roundToInt().coerceAtLeast(1)
    val outerHorizontalPadding =
        computeSafeOuterHorizontalPadding(
            widthDp = widthDp,
            requestedPaddingDp = internalHorizontalPaddingDp,
        )
    val verticalInset = internalVerticalPaddingDp.finiteOr(0f).dp
    val contentHorizontalPadding = 16.dp
    val barHeight = (heightDp - (verticalInset * 2)).coerceAtLeast(1.dp)
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .padding(start = outerHorizontalPadding, end = outerHorizontalPadding)
                    .clickable(
                        onClick = actionStartActivity(launchIntent),
                        rippleOverride = android.R.color.transparent,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (iconAlignLeft) {
                // Left alignment: icon on left, text centered
                WidgetBarContainer(
                    barHeight = barHeight,
                    backgroundBitmap = backgroundBitmap,
                    useDefaultBackground = useDefaultBackground,
                    backgroundColorProvider = backgroundColorProvider,
                    borderColorProvider = borderColorProvider,
                    borderWidthDp = borderWidthDp,
                    cornerRadius = cornerRadius,
                    contentHorizontalPadding = contentHorizontalPadding,
                    contentAlignment = Alignment.Center,
                ) {
                    // Text is always centered
                    if (showLabel) {
                        Text(
                            text = context.getString(R.string.app_name),
                            style =
                                TextStyle(
                                    color = textIconColorProvider,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                            maxLines = 1,
                        )
                    }

                    // Icon on the left
                    if (showSearchIcon) {
                        Box(
                            modifier =
                                GlanceModifier
                                    .fillMaxSize()
                                    .padding(start = 10.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_search),
                                contentDescription = context.getString(R.string.common_search),
                                modifier = GlanceModifier.size(iconSize),
                                colorFilter = ColorFilter.tint(textIconColorProvider),
                            )
                        }
                    }
                }
            } else {
                // Center alignment: icon and text together, centered as a unit
                WidgetBarContainer(
                    barHeight = barHeight,
                    backgroundBitmap = backgroundBitmap,
                    useDefaultBackground = useDefaultBackground,
                    backgroundColorProvider = backgroundColorProvider,
                    borderColorProvider = borderColorProvider,
                    borderWidthDp = borderWidthDp,
                    cornerRadius = cornerRadius,
                    contentHorizontalPadding = contentHorizontalPadding,
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (showSearchIcon) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_search),
                                contentDescription = context.getString(R.string.common_search),
                                modifier = GlanceModifier.size(iconSize),
                                colorFilter = ColorFilter.tint(textIconColorProvider),
                            )
                        }
                        if (showLabel) {
                            Text(
                                text = context.getString(R.string.app_name),
                                modifier = GlanceModifier.padding(start = if (showSearchIcon) 8.dp else 0.dp),
                                style =
                                    TextStyle(
                                        color = textIconColorProvider,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            if ((customButtons.isNotEmpty() && widthDp > WidgetLayoutUtils.NARROW_WIDTH_DP.dp) || showMicIcon) {
                Box(
                    modifier =
                        GlanceModifier
                            .fillMaxSize()
                            .padding(end = 14.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.End,
                    ) {
                        if (widthDp > WidgetLayoutUtils.NARROW_WIDTH_DP.dp) {
                            customButtons.forEachIndexed { index, action ->
                                val icon =
                                    remember(
                                        action,
                                        iconPackPackage,
                                        iconSizePx,
                                        textIconColor,
                                        context.mediaButtonPlaybackKey(action),
                                    ) {
                                        rememberWidgetButtonIcon(
                                            context = context,
                                            action = action,
                                            iconSizePx = iconSizePx,
                                            textIconColor = textIconColor,
                                            iconPackPackage = iconPackPackage,
                                        )
                                    }
                                Box(
                                    modifier =
                                        GlanceModifier
                                            .size(micTouchSpace)
                                            .clickable(
                                                onClick = context.customButtonWidgetClickAction(action),
                                                rippleOverride = android.R.color.transparent,
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val imageProvider =
                                        when {
                                            icon.bitmap != null -> ImageProvider(icon.bitmap)
                                            icon.drawableResId != null -> ImageProvider(icon.drawableResId)
                                            else -> ImageProvider(R.drawable.ic_widget_search) // Fallback
                                        }
                                    Image(
                                        provider = imageProvider,
                                        contentDescription = action.contentDescription(),
                                        modifier = GlanceModifier.size(iconSize),
                                        colorFilter =
                                            if (icon.shouldTint) {
                                                ColorFilter.tint(textIconColorProvider)
                                            } else {
                                                null
                                            },
                                    )
                                }
                                if (index != customButtons.lastIndex || showMicIcon) {
                                    Spacer(modifier = GlanceModifier.width(8.dp))
                                }
                            }
                        }

                        if (showMicIcon) {
                            Box(
                                modifier =
                                    GlanceModifier
                                        .size(micTouchSpace)
                                        .clickable(
                                            onClick = actionStartActivity(voiceLaunchIntent),
                                            rippleOverride = android.R.color.transparent,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_mic),
                                    contentDescription = context.getString(R.string.desc_voice_search_icon),
                                    modifier = GlanceModifier.size(iconSize),
                                    colorFilter = ColorFilter.tint(textIconColorProvider),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun computeSafeOuterHorizontalPadding(
    widthDp: Dp,
    requestedPaddingDp: Float,
): Dp {
    val safeWidth = widthDp.value.finiteOr(WidgetLayoutUtils.DEFAULT_WIDTH_DP).coerceAtLeast(1f)
    val requested = requestedPaddingDp.finiteOr(0f).coerceAtLeast(0f)
    val maxPadding = ((safeWidth - MIN_RENDERABLE_WIDTH_DP) / 2f).coerceAtLeast(0f)
    return requested.coerceAtMost(maxPadding).dp
}

@Composable
private fun WidgetBarContainer(
    barHeight: Dp,
    backgroundBitmap: Bitmap?,
    useDefaultBackground: Boolean,
    backgroundColorProvider: ColorProvider,
    borderColorProvider: ColorProvider?,
    borderWidthDp: Dp,
    cornerRadius: Dp,
    contentHorizontalPadding: Dp,
    contentAlignment: Alignment,
    content: @Composable () -> Unit,
) {
    if (useDefaultBackground || backgroundBitmap != null) {
        Box(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .background(
                        if (useDefaultBackground) {
                            ImageProvider(R.drawable.widget_quick_search_placeholder_outline)
                        } else {
                            ImageProvider(backgroundBitmap!!)
                        },
                    ).padding(horizontal = contentHorizontalPadding),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
        return
    }

    val safeBorderWidth = borderWidthDp.coerceAtLeast(0.dp)
    val innerRadius = (cornerRadius - safeBorderWidth).coerceAtLeast(0.dp)
    Box(
        modifier =
            GlanceModifier
                .fillMaxWidth()
                .height(barHeight)
                .background(borderColorProvider ?: backgroundColorProvider)
                .cornerRadius(cornerRadius)
                .padding(safeBorderWidth),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(backgroundColorProvider)
                    .cornerRadius(innerRadius)
                    .padding(horizontal = contentHorizontalPadding),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}

internal fun isDefaultBackgroundStyle(config: WidgetPreferences): Boolean =
    nearlyEqual(config.borderRadiusDp, WidgetDefaults.BORDER_RADIUS_DP) &&
        nearlyEqual(config.borderWidthDp, WidgetDefaults.BORDER_WIDTH_DP) &&
        nearlyEqual(config.backgroundAlpha, WidgetDefaults.BACKGROUND_ALPHA) &&
        nearlyEqual(config.borderAlpha, WidgetDefaults.BORDER_ALPHA) &&
        config.theme == WidgetDefaults.THEME &&
        config.backgroundColor == WidgetDefaults.BACKGROUND_COLOR &&
        config.useDeviceThemeBackground == WidgetDefaults.USE_DEVICE_THEME_BACKGROUND &&
        config.borderColor == WidgetDefaults.BORDER_COLOR_ARGB &&
        config.borderColorOption == WidgetDefaults.BORDER_COLOR_OPTION

private fun nearlyEqual(
    first: Float,
    second: Float,
    epsilon: Float = DEFAULT_FLOAT_COMPARISON_EPSILON,
): Boolean = kotlin.math.abs(first - second) <= epsilon

internal fun deviceThemeBackgroundColorRes(backgroundAlpha: Float): Int {
    val clampedAlpha = backgroundAlpha.coerceIn(0f, 1f)
    if (nearlyEqual(clampedAlpha, WidgetDefaults.BACKGROUND_ALPHA)) {
        return R.color.quick_search_widget_device_primary
    }
    return when ((clampedAlpha * 10f).roundToInt()) {
        0 -> R.color.quick_search_widget_device_primary_alpha_0
        1 -> R.color.quick_search_widget_device_primary_alpha_10
        2 -> R.color.quick_search_widget_device_primary_alpha_20
        3 -> R.color.quick_search_widget_device_primary_alpha_30
        4 -> R.color.quick_search_widget_device_primary_alpha_40
        5 -> R.color.quick_search_widget_device_primary_alpha_50
        6 -> R.color.quick_search_widget_device_primary_alpha_60
        7 -> R.color.quick_search_widget_device_primary_alpha_70
        8 -> R.color.quick_search_widget_device_primary_alpha_80
        9 -> R.color.quick_search_widget_device_primary_alpha_90
        else -> R.color.quick_search_widget_device_primary_alpha_100
    }
}

internal fun Float.finiteOr(default: Float): Float = if (isFinite()) this else default
