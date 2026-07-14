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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.WidgetActionActivity
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

private const val MIN_RENDERABLE_WIDTH_DP = 48f
private const val DEFAULT_FLOAT_COMPARISON_EPSILON = 0.001f

class SearchWidget(
    private val variant: WidgetVariant = WidgetVariant.STANDARD,
) : GlanceAppWidget() {
    companion object {
        const val EXTRA_START_VOICE_SEARCH = "com.tk.quicksearch.extra.START_VOICE_SEARCH"
        const val EXTRA_MIC_ACTION = "com.tk.quicksearch.extra.MIC_ACTION"
    }

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent { WidgetBody() }
    }

    @Composable
    private fun WidgetBody() {
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val config = prefs.toWidgetPreferences(context).enforceVariantConstraints(variant)
        val widgetSize = LocalSize.current
        val defaultWidth = WidgetLayoutUtils.DEFAULT_WIDTH_DP.dp
        val defaultHeight = WidgetLayoutUtils.DEFAULT_HEIGHT_DP.dp
        val widgetPadding = 0.dp
        val resolvedWidth = WidgetLayoutUtils.resolveOr(widgetSize.width, defaultWidth)
        val widthDp = (resolvedWidth.value.finiteOr(defaultWidth.value)).dp
        // Force fixed height regardless of grid size
        val heightDp = defaultHeight
        val isNarrowWidth = widthDp <= WidgetLayoutUtils.TWO_COLUMN_WIDTH_DP.dp
        val displayedWidthDp = widthDp - (widgetPadding * 2)
        val displayedHeightDp = heightDp - (widgetPadding * 2)
        val outerHorizontalPadding =
            computeSafeOuterHorizontalPadding(
                widthDp = widthDp,
                requestedPaddingDp = config.internalHorizontalPaddingDp,
            )
        val verticalInset = config.internalVerticalPaddingDp.finiteOr(0f).dp
        val renderedBarWidthDp = (displayedWidthDp - (outerHorizontalPadding * 2)).coerceAtLeast(1.dp)
        val renderedBarHeightDp = (displayedHeightDp - (verticalInset * 2)).coerceAtLeast(1.dp)

        val density = context.resources.displayMetrics.density
        val widthPx = (renderedBarWidthDp.value * density).roundToInt().coerceAtLeast(1)
        val heightPx = (renderedBarHeightDp.value * density).roundToInt().coerceAtLeast(1)
        val borderWidthPx = (config.borderWidthDp * density).roundToInt()
        val cornerRadiusPx = config.borderRadiusDp * density
        val colors = calculateColors(config, borderWidthPx)

        val hasDefaultBackground = isDefaultBackgroundStyle(config)
        val useDynamicSystemBackground =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                config.theme == WidgetTheme.SYSTEM &&
                config.backgroundColor == null &&
                !config.useDeviceThemeBackground
        // Arbitrary colors are not reliably applied by launchers through a Glance
        // ColorProvider. Render custom borders into the background bitmap instead.
        val useResourceBackedBackground =
            useDynamicSystemBackground && config.borderColorOption != BorderColorOption.CUSTOM

        val backgroundBitmap =
            if (!hasDefaultBackground && !useResourceBackedBackground) {
                WidgetBitmapUtils.createWidgetBitmap(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    backgroundColor = colors.backgroundColor,
                    borderColor = colors.borderColor,
                    borderWidthPx = borderWidthPx,
                    cornerRadiusPx = cornerRadiusPx,
                )
            } else {
                null
            }

        val launchIntent = createLaunchIntent(context)
        val voiceLaunchIntent =
            createLaunchIntent(
                context = context,
                startVoiceSearch = true,
                micAction = config.micAction,
            )
        val customButtons = config.customButtons.filterNotNull()

        when (variant) {
            WidgetVariant.STANDARD ->
                WidgetContent(
                    widthDp = widthDp,
                    heightDp = displayedHeightDp, // Pass displayed height for strict sizing
                    backgroundBitmap = backgroundBitmap,
                    useDefaultBackground = hasDefaultBackground,
                    textIconColor = colors.textIconColor,
                    textIconColorProvider = colors.textIconColorProvider,
                    backgroundColorProvider = colors.backgroundColorProvider,
                    borderColorProvider = colors.borderColorProvider,
                    borderWidthDp = config.borderWidthDp.dp,
                    cornerRadius = config.borderRadiusDp.dp,
                    // Hide label only when width is very narrow (≈2 columns) to keep icon visible
                    showLabel = config.showLabel && !isNarrowWidth,
                    showSearchIcon = config.showSearchIcon,
                    showMicIcon = config.micAction != MicAction.OFF,
                    // Force left alignment for icons when the widget collapses to ~2 columns.
                    iconAlignLeft = config.iconAlignLeft || isNarrowWidth,
                    internalHorizontalPaddingDp = config.internalHorizontalPaddingDp,
                    internalVerticalPaddingDp = config.internalVerticalPaddingDp,
                    launchIntent = launchIntent,
                    voiceLaunchIntent = voiceLaunchIntent,
                    customButtons = customButtons,
                )
            WidgetVariant.CUSTOM_BUTTONS_ONLY ->
                CustomButtonsOnlyWidgetContent(
                    widthDp = widthDp,
                    heightDp = displayedHeightDp,
                    backgroundBitmap = backgroundBitmap,
                    useDefaultBackground = hasDefaultBackground,
                    textIconColor = colors.textIconColor,
                    textIconColorProvider = colors.textIconColorProvider,
                    backgroundColorProvider = colors.backgroundColorProvider,
                    borderColorProvider = colors.borderColorProvider,
                    borderWidthDp = config.borderWidthDp.dp,
                    cornerRadius = config.borderRadiusDp.dp,
                    internalHorizontalPaddingDp = config.internalHorizontalPaddingDp,
                    internalVerticalPaddingDp = config.internalVerticalPaddingDp,
                    customButtons = customButtons,
                )
        }
    }

    private data class WidgetColors(
        val backgroundColor: Color,
        val borderColor: Color?,
        val textIconColor: Color,
        val backgroundColorProvider: ColorProvider = ColorProvider(backgroundColor),
        val borderColorProvider: ColorProvider? = borderColor?.let(::ColorProvider),
        val textIconColorProvider: ColorProvider = ColorProvider(textIconColor),
    )

    @Composable
    private fun calculateColors(
        config: WidgetPreferences,
        borderWidthPx: Int,
    ): WidgetColors {
        val context = LocalContext.current
        val isSystemInDarkTheme =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Determine effective theme based on user selection
        val effectiveTheme =
            when (config.theme) {
                WidgetTheme.SYSTEM -> if (isSystemInDarkTheme) WidgetTheme.DARK else WidgetTheme.LIGHT
                else -> config.theme
            }

        val customBackgroundColor = config.backgroundColor?.let(::Color)
        val deviceThemeBackgroundColorRes =
            if (config.useDeviceThemeBackground) {
                deviceThemeBackgroundColorRes(config.backgroundAlpha)
            } else {
                null
            }
        val deviceThemeBackgroundColor =
            deviceThemeBackgroundColorRes?.let { colorRes ->
                Color(ContextCompat.getColor(context, colorRes))
            }
        val resolvedCustomBackgroundColor = deviceThemeBackgroundColor ?: customBackgroundColor
        val useDynamicSystemColors =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                config.theme == WidgetTheme.SYSTEM &&
                resolvedCustomBackgroundColor == null
        val backgroundColor =
            resolvedCustomBackgroundColor?.copy(alpha = config.backgroundAlpha)
                ?: WidgetColorUtils.getBackgroundColor(
                    effectiveTheme,
                    config.backgroundAlpha,
                )
        val borderColor =
            if (borderWidthPx > 0) {
                if (config.borderColorOption == BorderColorOption.DEVICE_THEME && deviceThemeBackgroundColor != null) {
                    deviceThemeBackgroundColor.copy(alpha = config.borderAlpha.coerceAtMost(0.4f))
                } else {
                    WidgetColorUtils.getBorderColor(
                        config.borderColor,
                        config.borderAlpha,
                        effectiveTheme,
                        config.borderColorOption,
                    )
                }
            } else {
                null
            }
        val textIconColor =
            WidgetColorUtils.getTextIconColor(
                config.theme,
                config.backgroundAlpha,
                config.textIconColorOverride,
                customBackgroundColor = resolvedCustomBackgroundColor,
                isSystemInDarkTheme,
            )

        return WidgetColors(
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            textIconColor = textIconColor,
            backgroundColorProvider =
                if (deviceThemeBackgroundColorRes != null) {
                    ColorProvider(deviceThemeBackgroundColorRes)
                } else if (useDynamicSystemColors) {
                    DayNightColorProvider(
                        day = WidgetColorUtils.getBackgroundColor(WidgetTheme.LIGHT, config.backgroundAlpha),
                        night = WidgetColorUtils.getBackgroundColor(WidgetTheme.DARK, config.backgroundAlpha),
                    )
                } else {
                    ColorProvider(backgroundColor)
                },
            borderColorProvider =
                if (borderWidthPx > 0) {
                    if (useDynamicSystemColors && config.borderColorOption != BorderColorOption.CUSTOM) {
                        val lightBackground = WidgetColorUtils.getBackgroundColor(WidgetTheme.LIGHT, config.backgroundAlpha)
                        val darkBackground = WidgetColorUtils.getBackgroundColor(WidgetTheme.DARK, config.backgroundAlpha)
                        DayNightColorProvider(
                            day =
                                WidgetColorUtils.getBorderColor(
                                    config.borderColor,
                                    config.borderAlpha,
                                    WidgetTheme.LIGHT,
                                    config.borderColorOption,
                                ).compositeOver(lightBackground),
                            night =
                                WidgetColorUtils.getBorderColor(
                                    config.borderColor,
                                    config.borderAlpha,
                                    WidgetTheme.DARK,
                                    config.borderColorOption,
                                ).compositeOver(darkBackground),
                        )
                    } else {
                        borderColor?.let(::ColorProvider)
                    }
                } else {
                    null
                },
            textIconColorProvider =
                if (useDynamicSystemColors && config.textIconColorOverride == TextIconColorOverride.THEME) {
                    DayNightColorProvider(
                        day =
                            WidgetColorUtils.getTextIconColor(
                                WidgetTheme.LIGHT,
                                config.backgroundAlpha,
                                config.textIconColorOverride,
                            ),
                        night =
                            WidgetColorUtils.getTextIconColor(
                                WidgetTheme.DARK,
                                config.backgroundAlpha,
                                config.textIconColorOverride,
                            ),
                    )
                } else {
                    ColorProvider(textIconColor)
                },
        )
    }

    private fun createLaunchIntent(
        context: Context,
        startVoiceSearch: Boolean = false,
        micAction: MicAction = MicAction.DEFAULT_VOICE_SEARCH,
    ): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_VOICE_SEARCH, startVoiceSearch)
            putExtra(EXTRA_MIC_ACTION, micAction.value)
        }
}

@Composable
private fun CustomButtonsOnlyWidgetContent(
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
    val iconSizePx = (20.dp.value * density).roundToInt().coerceAtLeast(1)
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
                                remember(action, iconPackPackage, iconSizePx, textIconColor) {
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
                                                onClick =
                                                    actionStartActivity(
                                                        WidgetActionActivity.createIntent(
                                                            context,
                                                            action,
                                                        ),
                                                    ),
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
                                        modifier = GlanceModifier.size(20.dp),
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
private fun WidgetContent(
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
    val iconSizePx = (20.dp.value * density).roundToInt().coerceAtLeast(1)
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
                                modifier = GlanceModifier.size(20.dp),
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
                                modifier = GlanceModifier.size(20.dp),
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
                                    remember(action, iconPackPackage, iconSizePx, textIconColor) {
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
                                                onClick =
                                                    actionStartActivity(
                                                        WidgetActionActivity.createIntent(
                                                            context,
                                                            action,
                                                        ),
                                                    ),
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
                                        modifier = GlanceModifier.size(20.dp),
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
                                    modifier = GlanceModifier.size(20.dp),
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

private fun computeSafeOuterHorizontalPadding(
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

private fun isDefaultBackgroundStyle(config: WidgetPreferences): Boolean =
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

private fun deviceThemeBackgroundColorRes(backgroundAlpha: Float): Int {
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

private fun Float.finiteOr(default: Float): Float = if (isFinite()) this else default
