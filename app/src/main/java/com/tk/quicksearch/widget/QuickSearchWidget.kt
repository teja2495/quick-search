package com.tk.quicksearch.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
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
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tk.quicksearch.MainActivity
import com.tk.quicksearch.R
import kotlin.math.max

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

        val surfaceColor = Color(config.backgroundColor).copy(alpha = config.backgroundAlpha)
        val borderColor = Color(config.borderColor)
        val outerRadius = config.borderRadiusDp.dp
        val innerRadius = max(config.borderRadiusDp - config.borderWidthDp, 0f).dp

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(actionStartActivity(launchIntent))
        ) {
            var chipModifier = GlanceModifier
                .fillMaxWidth()
                .height(56.dp)

            chipModifier = if (config.borderWidthDp > 0f) {
                chipModifier
                    .background(ColorProvider(borderColor))
                    .cornerRadius(outerRadius)
                    .padding(config.borderWidthDp.dp)
                    .background(ColorProvider(surfaceColor))
                    .cornerRadius(innerRadius)
            } else {
                chipModifier
                    .background(ColorProvider(surfaceColor))
                    .cornerRadius(outerRadius)
            }

            chipModifier = chipModifier.padding(horizontal = 16.dp)

            Box(modifier = chipModifier) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_search),
                        contentDescription = context.getString(R.string.desc_search_icon),
                        modifier = GlanceModifier.size(20.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(borderColor))
                    )
                    if (config.showLabel) {
                        Text(
                            text = context.getString(R.string.widget_label_text),
                            modifier = GlanceModifier.padding(start = 8.dp),
                            style = TextStyle(
                                color = ColorProvider(borderColor),
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
