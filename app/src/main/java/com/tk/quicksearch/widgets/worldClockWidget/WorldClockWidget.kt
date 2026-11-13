package com.tk.quicksearch.widgets.worldClockWidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.searchWidget.SearchWidgetConfigureActivity
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import com.tk.quicksearch.widgets.utils.WidgetLayoutUtils
import kotlin.math.roundToInt

/** Default size (4 columns by 1 row) used until the host reports the placed widget's size. */
private const val DEFAULT_WIDTH_DP = 250f
private const val DEFAULT_HEIGHT_DP = 80f

/**
 * Up to four clocks side by side. The times are TextClocks, so they tick on their own; the widget
 * only redraws when its settings, the size, the day offsets or the system clock settings change.
 */
class WorldClockWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        /** Drawn at until the host reports the placed widget's size. */
        val DEFAULT_SIZE = DpSize(DEFAULT_WIDTH_DP.dp, DEFAULT_HEIGHT_DP.dp)
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        provideContent { WidgetBody(appWidgetId) }
    }

    @Composable
    private fun WidgetBody(appWidgetId: Int) {
        // Reading the state keeps this composition subscribed to refreshWorldClockWidgets' revision bump.
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val config = prefs.toWorldClockWidgetConfig(context)

        val size = LocalSize.current
        val widthDp = WidgetLayoutUtils.resolveOr(size.width, DEFAULT_WIDTH_DP.dp)
        val heightDp = WidgetLayoutUtils.resolveOr(size.height, DEFAULT_HEIGHT_DP.dp)
        val resources = context.resources
        val density = resources.displayMetrics.density
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // Identical unless the background follows the system theme.
        val dayColors = WorldClockWidgetColors.forWidget(config, isSystemDark = false)
        val nightColors = WorldClockWidgetColors.forWidget(config, isSystemDark = true)

        val backgroundModifier =
            when {
                config.style.backgroundAlpha <= 0f -> GlanceModifier
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    GlanceModifier
                        .background(DayNightColorProvider(day = dayColors.background, night = nightColors.background))
                        .cornerRadius(config.style.borderRadiusDp.dp)
                // Corner radii need Android 12+ in Glance, so older versions draw the card as a bitmap.
                else ->
                    GlanceModifier.background(
                        ImageProvider(
                            WidgetBitmapUtils.createWidgetBitmap(
                                widthPx = (widthDp.value * density).roundToInt().coerceAtLeast(1),
                                heightPx = (heightDp.value * density).roundToInt().coerceAtLeast(1),
                                backgroundColor = if (isDark) nightColors.background else dayColors.background,
                                borderColor = null,
                                borderWidthPx = 0,
                                cornerRadiusPx = config.style.borderRadiusDp * density,
                            ),
                        ),
                    )
            }
        val secondaryColor = DayNightColorProvider(day = dayColors.secondaryText, night = nightColors.secondaryText)

        Row(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .then(backgroundModifier)
                    .padding(horizontal = WorldClockLayout.HORIZONTAL_PADDING, vertical = WorldClockLayout.VERTICAL_PADDING)
                    .clickable(
                        onClick = actionStartActivity(configureIntent(context, appWidgetId)),
                        rippleOverride = android.R.color.transparent,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (config.locations.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_world_clock_add_location),
                    style =
                        TextStyle(
                            color = secondaryColor,
                            fontSize = WorldClockLayout.PLACEHOLDER_TEXT_SIZE_SP.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                    maxLines = 2,
                )
                return@Row
            }
            val is24Hour = WorldClockFormats.is24Hour(context)
            val layout =
                WorldClockLayout.cellLayout(
                    widthDp = widthDp.value,
                    heightDp = heightDp.value,
                    clockCount = config.locations.size,
                    preferredTimeSizeSp = config.textSizeSp,
                    widths = WorldClockFormats.measure(context, is24Hour, config.locations.map(WorldClockZones::displayName)),
                    fontScale = resources.configuration.fontScale,
                )
            config.locations.forEachIndexed { index, location ->
                if (index > 0) Spacer(modifier = GlanceModifier.width(WorldClockLayout.CELL_SPACING))
                ClockCell(
                    context = context,
                    location = location,
                    layout = layout,
                    colors = ClockColors(day = dayColors, night = nightColors, isDark = isDark),
                    secondaryColor = secondaryColor,
                )
            }
        }
    }

    private class ClockColors(
        val day: WorldClockWidgetColors,
        val night: WorldClockWidgetColors,
        val isDark: Boolean,
    )

    @Composable
    private fun RowScope.ClockCell(
        context: Context,
        location: WorldClockLocation,
        layout: WorldClockCellLayout,
        colors: ClockColors,
        secondaryColor: ColorProvider,
    ) {
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (layout.showLabel) {
                Text(
                    text = WorldClockZones.displayName(location),
                    style =
                        TextStyle(
                            color = secondaryColor,
                            fontSize = layout.labelSizeSp.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
            AndroidRemoteViews(
                remoteViews =
                    clockViews(
                        context = context,
                        layoutId = R.layout.widget_world_clock_time,
                        zoneId = location.zoneId,
                        format12Hour = WorldClockFormats.timeFormat(is24Hour = false),
                        format24Hour = WorldClockFormats.timeFormat(is24Hour = true),
                        textSizeSp = layout.timeSizeSp,
                        dayColor = colors.day.text,
                        nightColor = colors.night.text,
                        isDark = colors.isDark,
                    ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            if (layout.showSubtitle) {
                val subtitle = WorldClockFormats.subtitlePattern()
                AndroidRemoteViews(
                    remoteViews =
                        clockViews(
                            context = context,
                            layoutId = R.layout.widget_world_clock_subtitle,
                            zoneId = location.zoneId,
                            format12Hour = subtitle,
                            format24Hour = subtitle,
                            textSizeSp = layout.subtitleSizeSp,
                            dayColor = colors.day.secondaryText,
                            nightColor = colors.night.secondaryText,
                            isDark = colors.isDark,
                        ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
        }
    }

    /** A TextClock showing [zoneId]'s time; it follows the system's 12/24-hour setting by itself. */
    private fun clockViews(
        context: Context,
        layoutId: Int,
        zoneId: String,
        format12Hour: CharSequence,
        format24Hour: CharSequence,
        textSizeSp: Float,
        dayColor: Color,
        nightColor: Color,
        isDark: Boolean,
    ): RemoteViews =
        RemoteViews(context.packageName, layoutId).apply {
            val viewId = R.id.world_clock_text
            setString(viewId, "setTimeZone", zoneId)
            setCharSequence(viewId, "setFormat12Hour", format12Hour)
            setCharSequence(viewId, "setFormat24Hour", format24Hour)
            setTextViewTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setColorInt(viewId, "setTextColor", dayColor.toArgb(), nightColor.toArgb())
            } else {
                setTextColor(viewId, (if (isDark) nightColor else dayColor).toArgb())
            }
        }

    /** Opens this widget's settings, where its locations and look can be changed. */
    private fun configureIntent(
        context: Context,
        appWidgetId: Int,
    ): Intent =
        Intent(context, SearchWidgetConfigureActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // A task of its own, so finishing it returns to the home screen, not Quick Search.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
}
