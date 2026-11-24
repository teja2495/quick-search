package com.tk.quicksearch.widgets.remindersWidget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetDimens
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetRowColors
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetRowContent
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetColors
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetDimens
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import com.tk.quicksearch.widgets.utils.WidgetLayoutUtils
import kotlin.math.roundToInt

/** Default size (4 columns by 2 rows) used until the host reports the placed widget's size. */
private const val DEFAULT_WIDTH_DP = 250f
private const val DEFAULT_HEIGHT_DP = 180f

/**
 * Lists overdue reminders and those due in the next few days, in the Note widget's look. Tapping a
 * reminder opens it in Quick Search and the check marks it done. It redraws when reminders change
 * (see [com.tk.quicksearch.widgets.utils.requestRemindersWidgetsRefresh]), when one turns overdue,
 * at midnight, and on clock and locale changes.
 */
class RemindersWidget : GlanceAppWidget() {
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
        provideContent { WidgetBody() }
    }

    @Composable
    private fun WidgetBody() {
        // Reading the state keeps this composition subscribed to refreshRemindersWidgets' revision bump.
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val config = prefs.toRemindersWidgetConfig(context)
        val snapshot = loadRemindersWidgetSnapshot(context, config.upcomingDays)
        LaunchedEffect(snapshot.nextChangeMillis) {
            RemindersWidgetReceiver.scheduleRefresh(context, snapshot.nextChangeMillis)
        }

        val size = LocalSize.current
        val widthDp = WidgetLayoutUtils.resolveOr(size.width, DEFAULT_WIDTH_DP.dp)
        val heightDp = WidgetLayoutUtils.resolveOr(size.height, DEFAULT_HEIGHT_DP.dp)
        val resources = context.resources
        val density = resources.displayMetrics.density
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val style = config.style
        // Identical unless the background follows the system theme.
        val dayColors = NoteWidgetColors.forWidget(style, isSystemDark = false)
        val nightColors = NoteWidgetColors.forWidget(style, isSystemDark = true)

        val backgroundModifier =
            when {
                style.backgroundAlpha <= 0f -> GlanceModifier
                // Day/night colors let the launcher switch the card with the system theme.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    GlanceModifier
                        .background(DayNightColorProvider(day = dayColors.background, night = nightColors.background))
                        .cornerRadius(style.borderRadiusDp.dp)
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
                                cornerRadiusPx = style.borderRadiusDp * density,
                            ),
                        ),
                    )
            }

        Column(modifier = GlanceModifier.fillMaxSize().then(backgroundModifier)) {
            Text(
                text = context.getString(R.string.section_reminders),
                style =
                    TextStyle(
                        color = DayNightColorProvider(day = dayColors.title, night = nightColors.title),
                        fontSize = NoteWidgetDimens.TITLE_FONT_SIZE * style.textScale,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
                modifier =
                    GlanceModifier.padding(
                        start = NoteWidgetDimens.CONTENT_PADDING,
                        top = NoteWidgetDimens.CONTENT_PADDING,
                        end = NoteWidgetDimens.CONTENT_PADDING,
                    ),
            )
            if (snapshot.rows.isEmpty()) {
                Text(
                    text = context.getString(R.string.widget_reminders_empty),
                    style =
                        TextStyle(
                            color = DayNightColorProvider(day = dayColors.placeholder, night = nightColors.placeholder),
                            fontSize = NoteWidgetDimens.BODY_FONT_SIZE * style.textScale,
                        ),
                    modifier =
                        GlanceModifier.padding(
                            start = NoteWidgetDimens.CONTENT_PADDING,
                            top = NoteWidgetDimens.TITLE_BODY_SPACING,
                            end = NoteWidgetDimens.CONTENT_PADDING,
                        ),
                )
            } else {
                ReminderList(context, snapshot, dayColors, nightColors, style.backgroundAlpha, style.textScale)
            }
        }
    }

    @Composable
    private fun ReminderList(
        context: Context,
        snapshot: RemindersWidgetSnapshot,
        dayColors: NoteWidgetColors,
        nightColors: NoteWidgetColors,
        backgroundAlpha: Float,
        textScale: Float,
    ) {
        val rowColors = AtAGlanceWidgetRowColors(dayColors, nightColors)
        val dividerColor =
            DayNightColorProvider(
                day = dayColors.title.copy(alpha = AtAGlanceWidgetDimens.DIVIDER_ALPHA),
                night = nightColors.title.copy(alpha = AtAGlanceWidgetDimens.DIVIDER_ALPHA),
            )
        val lastKey = snapshot.rows.lastOrNull()?.key
        LazyColumn(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .padding(
                        horizontal = AtAGlanceWidgetDimens.rowListHorizontalPadding(backgroundAlpha),
                        vertical = AtAGlanceWidgetDimens.LIST_VERTICAL_PADDING,
                    ),
        ) {
            items(snapshot.rows, itemId = { it.key.hashCode().toLong() }) { row ->
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    AtAGlanceWidgetRowContent(context, row, rowColors, textScale)
                    if (row.key != lastKey) {
                        Box(
                            modifier =
                                GlanceModifier
                                    .fillMaxWidth()
                                    .height(AtAGlanceWidgetDimens.DIVIDER_HEIGHT)
                                    .background(dividerColor),
                        ) {}
                    }
                }
            }
        }
    }
}
