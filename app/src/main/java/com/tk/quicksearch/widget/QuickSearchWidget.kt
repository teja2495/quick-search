package com.tk.quicksearch.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tk.quicksearch.MainActivity
import com.tk.quicksearch.R
import kotlin.math.roundToInt

class QuickSearchWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetBody() }
    }

    @Composable
    private fun WidgetBody() {
        val prefs = currentState<Preferences>()
        val config = prefs.toWidgetPreferences()
        val context = LocalContext.current
        val widgetSize = LocalSize.current
        
        // Calculate dimensions
        val defaultHeight = WidgetLayoutUtils.DEFAULT_HEIGHT_DP.dp
        val widgetPadding = 8.dp
        val widthDp = WidgetLayoutUtils.resolveOr(widgetSize.width, defaultHeight)
        val heightDp = WidgetLayoutUtils.resolveOr(widgetSize.height, defaultHeight)
        val cornerRadius = config.borderRadiusDp.dp
        
        // Calculate displayed dimensions (widget size minus padding)
        // The bitmap should match the displayed size to avoid stretching
        val displayedWidthDp = widthDp - (widgetPadding * 2)
        val displayedHeightDp = heightDp - (widgetPadding * 2)
        
        val density = context.resources.displayMetrics.density
        val widthPx = (displayedWidthDp.value * density).roundToInt().coerceAtLeast(1)
        val heightPx = (displayedHeightDp.value * density).roundToInt().coerceAtLeast(1)
        val borderWidthPx = (config.borderWidthDp * density).roundToInt()
        val cornerRadiusPx = config.borderRadiusDp * density
        
        // Calculate colors
        val colors = calculateColors(config, borderWidthPx)
        
        // Create bitmap background
        val backgroundBitmap = WidgetBitmapUtils.createWidgetBitmap(
            widthPx = widthPx,
            heightPx = heightPx,
            backgroundColor = colors.backgroundColor,
            borderColor = colors.borderColor,
            borderWidthPx = borderWidthPx,
            cornerRadiusPx = cornerRadiusPx
        )
        
        // Create launch intent
        val launchIntent = createLaunchIntent(context)

        WidgetContent(
            widthDp = widthDp,
            heightDp = heightDp,
            cornerRadius = cornerRadius,
            backgroundBitmap = backgroundBitmap,
            textIconColor = colors.textIconColor,
            showLabel = config.showLabel,
            showSearchIcon = config.showSearchIcon,
            launchIntent = launchIntent
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
        val backgroundColor = WidgetColorUtils.getBackgroundColor(
            config.backgroundColorIsWhite,
            config.backgroundAlpha
        )
        val borderColor = if (borderWidthPx > 0) {
            WidgetColorUtils.getBorderColor(config.borderColor, config.backgroundAlpha)
        } else {
            null
        }
        val textIconColor = WidgetColorUtils.getTextIconColor(
            config.textIconColorIsWhite,
            config.backgroundAlpha
        )
        
        return WidgetColors(
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            textIconColor = textIconColor
        )
    }

    private fun createLaunchIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}

@Composable
private fun WidgetContent(
    widthDp: Dp,
    heightDp: Dp,
    cornerRadius: Dp,
    backgroundBitmap: Bitmap,
    textIconColor: Color,
    showLabel: Boolean,
    showSearchIcon: Boolean,
    launchIntent: Intent
) {
    val context = LocalContext.current
    
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .clickable(actionStartActivity(launchIntent)),
            contentAlignment = Alignment.Center
        ) {
            val widgetModifier = GlanceModifier
                .fillMaxWidth()
                .fillMaxHeight()
                .cornerRadius(cornerRadius)
                .background(ImageProvider(backgroundBitmap))
                .padding(horizontal = 16.dp)

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
    }
}
