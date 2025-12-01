package com.tk.quicksearch.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
        val borderColor = Color(config.borderColor).copy(alpha = config.backgroundAlpha)
        val defaultHeight = 64.dp
        val cornerRadius = config.borderRadiusDp.dp
        val displayMetrics = context.resources.displayMetrics
        val borderWidthPx = (config.borderWidthDp * displayMetrics.density).roundToInt()
        val cornerRadiusPx = config.borderRadiusDp * displayMetrics.density
        val widgetSize = LocalSize.current
        fun Dp.resolveOr(default: Dp): Dp = if (this == Dp.Unspecified || this.value <= 0f) default else this
        val widthDp = widgetSize.width.resolveOr(defaultHeight)
        val heightDp = widgetSize.height.resolveOr(defaultHeight)
        val widthPx = (widthDp.value * displayMetrics.density).roundToInt().coerceAtLeast(1)
        val heightPx = (heightDp.value * displayMetrics.density).roundToInt().coerceAtLeast(1)
        val outlineBitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outlineBitmap)
        
        // Draw background color with transparency
        val backgroundColor = if (config.backgroundColorIsWhite) Color.White else Color.Black
        val backgroundWithAlpha = backgroundColor.copy(alpha = config.backgroundAlpha)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = backgroundWithAlpha.toArgb()
        }
        val backgroundRect = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())
        if (cornerRadiusPx > 0f) {
            canvas.drawRoundRect(backgroundRect, cornerRadiusPx, cornerRadiusPx, backgroundPaint)
        } else {
            canvas.drawRect(backgroundRect, backgroundPaint)
        }
        
        // Draw border on top if border width is greater than 0
        if (borderWidthPx > 0) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = borderWidthPx.toFloat()
                color = borderColor.toArgb()
            }
            val inset = borderWidthPx / 2f
            val borderRect = RectF(inset, inset, widthPx - inset, heightPx - inset)
            if (cornerRadiusPx > 0f) {
                canvas.drawRoundRect(borderRect, cornerRadiusPx, cornerRadiusPx, borderPaint)
            } else {
                canvas.drawRect(borderRect, borderPaint)
            }
        }

        // Determine text and icon color based on background and transparency
        // Text and icon should remain fully opaque (no transparency)
        val baseBorderColor = Color(config.borderColor)
        val textIconColor = if (config.backgroundAlpha > 0.6f && config.backgroundColorIsWhite) {
            Color(0xFF424242) // Dark grey
        } else {
            baseBorderColor // Fully opaque border color
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable(actionStartActivity(launchIntent)),
                contentAlignment = Alignment.Center
            ) {
                val widgetModifier = GlanceModifier
                    .fillMaxWidth()
                    .height(heightDp)
                    .cornerRadius(cornerRadius)
                    .background(ImageProvider(outlineBitmap))
                    .padding(horizontal = 16.dp)

                Row(
                    modifier = widgetModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_search),
                        contentDescription = context.getString(R.string.desc_search_icon),
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(textIconColor))
                    )
                    if (config.showLabel) {
                        Text(
                            text = context.getString(R.string.widget_label_text),
                            modifier = GlanceModifier.padding(start = 8.dp),
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
}
