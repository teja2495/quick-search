package com.tk.quicksearch.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import com.tk.quicksearch.widget.voiceSearch.MicAction
import com.tk.quicksearch.widget.customButtons.CustomWidgetButtonAction
import com.tk.quicksearch.widget.customButtons.QuickSearchWidgetActionActivity
import com.tk.quicksearch.widget.customButtons.WidgetButtonIcon
import com.tk.quicksearch.widget.customButtons.rememberWidgetButtonIcon
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tk.quicksearch.app.MainActivity

import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.UserAppPreferences
import kotlin.math.roundToInt

class QuickSearchWidget : GlanceAppWidget() {

    companion object {
        const val EXTRA_START_VOICE_SEARCH = "com.tk.quicksearch.extra.START_VOICE_SEARCH"
        const val EXTRA_MIC_ACTION = "com.tk.quicksearch.extra.MIC_ACTION"
    }

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetBody() }
    }

    @Composable
    private fun WidgetBody() {
        val prefs = currentState<Preferences>()
        val config = prefs.toWidgetPreferences()
        val context = LocalContext.current
        val widgetSize = LocalSize.current
        val defaultWidth = WidgetLayoutUtils.DEFAULT_WIDTH_DP.dp
        val defaultHeight = WidgetLayoutUtils.DEFAULT_HEIGHT_DP.dp
        val widgetPadding = 0.dp
        val widthDp = WidgetLayoutUtils.resolveOr(widgetSize.width, defaultWidth)
        // Force fixed height regardless of grid size
        val heightDp = defaultHeight
        val isNarrowWidth = widthDp <= WidgetLayoutUtils.TWO_COLUMN_WIDTH_DP.dp
        val displayedWidthDp = widthDp - (widgetPadding * 2)
        val displayedHeightDp = heightDp - (widgetPadding * 2)
        
        val density = context.resources.displayMetrics.density
        val widthPx = (displayedWidthDp.value * density).roundToInt().coerceAtLeast(1)
        val heightPx = (displayedHeightDp.value * density).roundToInt().coerceAtLeast(1)
        val borderWidthPx = (config.borderWidthDp * density).roundToInt()
        val cornerRadiusPx = config.borderRadiusDp * density
        val colors = calculateColors(config, borderWidthPx)

        val hasDefaultBackground = config.borderRadiusDp == WidgetDefaults.BORDER_RADIUS_DP &&
                config.borderWidthDp == WidgetDefaults.BORDER_WIDTH_DP &&
                config.backgroundAlpha == WidgetDefaults.BACKGROUND_ALPHA &&
                config.theme == WidgetDefaults.THEME

        val backgroundBitmap = if (!hasDefaultBackground) {
            WidgetBitmapUtils.createWidgetBitmap(
                widthPx = widthPx,
                heightPx = heightPx,
                backgroundColor = colors.backgroundColor,
                borderColor = colors.borderColor,
                borderWidthPx = borderWidthPx,
                cornerRadiusPx = cornerRadiusPx
            )
        } else {
            null
        }

        val launchIntent = createLaunchIntent(context)
        val voiceLaunchIntent = createLaunchIntent(
            context = context,
            startVoiceSearch = true,
            micAction = config.micAction
        )
        val customButtons = config.customButtons.filterNotNull()

        WidgetContent(
            widthDp = widthDp,
            heightDp = displayedHeightDp, // Pass displayed height for strict sizing
            backgroundBitmap = backgroundBitmap,
            useDefaultBackground = hasDefaultBackground,
            textIconColor = colors.textIconColor,
            // Hide label only when width is very narrow (≈2 columns) to keep icon visible
            showLabel = config.showLabel && !isNarrowWidth,
            showSearchIcon = config.showSearchIcon,
            showMicIcon = config.micAction != MicAction.OFF,
            // Force left alignment for icons when the widget collapses to ~2 columns.
            iconAlignLeft = config.iconAlignLeft || isNarrowWidth,
            launchIntent = launchIntent,
            voiceLaunchIntent = voiceLaunchIntent,
            customButtons = customButtons
        )
    }

    private data class WidgetColors(
        val backgroundColor: Color,
        val borderColor: Color?,
        val textIconColor: Color
    )

    @Composable
    private fun calculateColors(
        config: QuickSearchWidgetPreferences,
        borderWidthPx: Int
    ): WidgetColors {
        val context = LocalContext.current
        val isSystemInDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Determine effective theme based on user selection
        val effectiveTheme = when (config.theme) {
            WidgetTheme.SYSTEM -> if (isSystemInDarkTheme) WidgetTheme.DARK else WidgetTheme.LIGHT
            else -> config.theme
        }

        val backgroundColor = WidgetColorUtils.getBackgroundColor(
            effectiveTheme,
            config.backgroundAlpha
        )
        val borderColor = if (borderWidthPx > 0) {
            WidgetColorUtils.getBorderColor(config.borderColor, config.backgroundAlpha)
        } else {
            null
        }
        val textIconColor = WidgetColorUtils.getTextIconColor(
            config.theme,
            config.backgroundAlpha,
            config.textIconColorOverride,
            isSystemInDarkTheme
        )

        return WidgetColors(
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            textIconColor = textIconColor
        )
    }

    private fun createLaunchIntent(
        context: Context,
        startVoiceSearch: Boolean = false,
        micAction: MicAction = MicAction.DEFAULT_VOICE_SEARCH
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_START_VOICE_SEARCH, startVoiceSearch)
            putExtra(EXTRA_MIC_ACTION, micAction.value)
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
    showLabel: Boolean,
    showSearchIcon: Boolean,
    showMicIcon: Boolean,
    iconAlignLeft: Boolean,
    launchIntent: Intent,
    voiceLaunchIntent: Intent,
    customButtons: List<CustomWidgetButtonAction>
) {
    val context = LocalContext.current
    val micTouchSpace = 36.dp
    val iconPackPackage = remember(context) {
        UserAppPreferences(context).uiPreferences.getSelectedIconPackPackage()
    }
    val density = context.resources.displayMetrics.density
    val iconSizePx = (20.dp.value * density).roundToInt().coerceAtLeast(1)
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(0.dp)
                .clickable(
                    onClick = actionStartActivity(launchIntent),
                    rippleOverride = android.R.color.transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            val widgetModifier = GlanceModifier
                .fillMaxWidth()
                .height(heightDp)
                .background(
                    if (useDefaultBackground) 
                        ImageProvider(R.drawable.widget_quick_search_placeholder_outline) 
                    else 
                        ImageProvider(backgroundBitmap!!)
                )
                .padding(horizontal = 16.dp)

            if (iconAlignLeft) {
                // Left alignment: icon on left, text centered
                Box(
                    modifier = widgetModifier,
                    contentAlignment = Alignment.Center
                ) {
                    // Text is always centered
                    if (showLabel) {
                        Text(
                            text = context.getString(R.string.widget_label_text),
                            style = TextStyle(
                                color = ColorProvider(textIconColor),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }

                    // Icon on the left
                    if (showSearchIcon) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(start = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_search),
                                contentDescription = context.getString(R.string.desc_search_icon),
                                modifier = GlanceModifier.size(20.dp),
                                colorFilter = ColorFilter.tint(ColorProvider(textIconColor))
                            )
                        }
                    }
                }
            } else {
                // Center alignment: icon and text together, centered as a unit
                Row(
                    modifier = widgetModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showSearchIcon) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_search),
                            contentDescription = context.getString(R.string.desc_search_icon),
                            modifier = GlanceModifier.size(20.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(textIconColor))
                        )
                    }
                    if (showLabel) {
                        Text(
                            text = context.getString(R.string.widget_label_text),
                            modifier = GlanceModifier.padding(start = if (showSearchIcon) 8.dp else 0.dp),
                            style = TextStyle(
                                color = ColorProvider(textIconColor),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            if ((customButtons.isNotEmpty() && widthDp > WidgetLayoutUtils.NARROW_WIDTH_DP.dp) || showMicIcon) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(end = 14.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.End
                    ) {
                        if (widthDp > WidgetLayoutUtils.NARROW_WIDTH_DP.dp) {
                            customButtons.forEachIndexed { index, action ->
                            val icon = remember(action, iconPackPackage, iconSizePx, textIconColor) {
                                rememberWidgetButtonIcon(
                                    context = context,
                                    action = action,
                                    iconSizePx = iconSizePx,
                                    textIconColor = textIconColor,
                                    iconPackPackage = iconPackPackage
                                )
                            }
                            Box(
                                modifier = GlanceModifier
                                    .size(micTouchSpace)
                                    .clickable(
                                        onClick = actionStartActivity(
                                            QuickSearchWidgetActionActivity.createIntent(
                                                context,
                                                action
                                            )
                                        ),
                                        rippleOverride = android.R.color.transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val imageProvider = when {
                                    icon.bitmap != null -> ImageProvider(icon.bitmap)
                                    icon.drawableResId != null -> ImageProvider(icon.drawableResId)
                                    else -> ImageProvider(R.drawable.ic_widget_search) // Fallback
                                }
                                Image(
                                    provider = imageProvider,
                                    contentDescription = action.contentDescription(),
                                    modifier = GlanceModifier.size(20.dp),
                                    colorFilter = if (icon.shouldTint) {
                                        ColorFilter.tint(ColorProvider(textIconColor))
                                    } else {
                                        null
                                    }
                                )
                            }
                            if (index != customButtons.lastIndex || showMicIcon) {
                                Spacer(modifier = GlanceModifier.width(8.dp))
                            }
                        }
                        }

                        if (showMicIcon) {
                            Box(
                                modifier = GlanceModifier
                                    .size(micTouchSpace)
                                    .clickable(
                                        onClick = actionStartActivity(voiceLaunchIntent),
                                        rippleOverride = android.R.color.transparent
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_widget_mic),
                                    contentDescription = context.getString(R.string.desc_voice_search_icon),
                                    modifier = GlanceModifier.size(20.dp),
                                    colorFilter = ColorFilter.tint(ColorProvider(textIconColor))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
