package com.tk.quicksearch.widgets.atAGlanceWidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.runComposition
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
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
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsSnapshot
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetColors
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import com.tk.quicksearch.widgets.utils.WidgetLayoutUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** The Now Playing item's list key, in the same style as rows' own keys such as "alarm" and "missed-calls". */
private const val NOW_PLAYING_ITEM_KEY = "now_playing"

/** Default size (3 columns by 2 rows) used until the host reports the placed widget's size. */
private const val DEFAULT_WIDTH_DP = 180f
private const val DEFAULT_HEIGHT_DP = 110f

/** Shared by the placed widget and the configure-screen preview so both render the same rows. */
internal object AtAGlanceWidgetDimens {
    val ICON_SIZE = 24.dp
    val ICON_TEXT_SPACING = 12.dp
    val TEXT_SPACING = 2.dp
    val BUTTON_SPACING = 8.dp
    val CHIP_TOP_SPACING = 6.dp
    val HORIZONTAL_PADDING = 16.dp
    val LIST_VERTICAL_PADDING = 4.dp
    val ROW_VERTICAL_PADDING = 12.dp
    val DIVIDER_HEIGHT = 1.dp
    // The 16dp icon sits in a larger box so taps near it don't land on the row and open it instead.
    val DISMISS_BUTTON_SIZE = 36.dp
    val DISMISS_ICON_SIZE = 16.dp
    val HANG_UP_BUTTON_SIZE = 36.dp
    val BUTTON_ICON_SIZE = 20.dp
    val CHIP_ICON_SIZE = 16.dp

    // Home's bodyMedium, bodySmall, bodyLarge, headlineSmall, labelMedium and labelSmall.
    val TITLE_FONT_SIZE = 14.sp
    val SUBTITLE_FONT_SIZE = 12.sp
    val LARGE_TITLE_FONT_SIZE = 16.sp
    val LARGE_SUBTITLE_FONT_SIZE = 20.sp
    val CODE_FONT_SIZE = 24.sp
    val LABEL_FONT_SIZE = 12.sp
    val FOOTER_FONT_SIZE = 11.sp
    val EMPTY_DATE_FONT_SIZE = 22.sp
    const val DIVIDER_ALPHA = 0.15f

    /** Rows sit close to the edges, and closer still without a card to inset them from. */
    fun rowListHorizontalPadding(backgroundAlpha: Float) = if (backgroundAlpha <= 0f) 4.dp else 10.dp
}

/**
 * The home At a Glance items as a scrollable list on a card, styled like the Note widget. Rows are
 * read fresh on every redraw; [AtAGlanceWidgetUpdater] and [AtAGlanceWidgetReceiver] decide when.
 */
class AtAGlanceWidget : GlanceAppWidget() {
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
        AtAGlanceWidgetUpdater.start(context)
        provideContent { WidgetBody() }
    }

    /**
     * Draws [appWidgetId] and hands it to the host right away. [update] alone waits for Glance's
     * session worker, which WorkManager can hold back for many seconds, so a saved setting or a
     * changed row would otherwise show up late. The session still redraws the same state after,
     * unless [allSizes] is set: then every size the host reported is drawn, as the session would,
     * and nothing is drawn (returning false) when it has reported none. Returns whether it drew.
     */
    @OptIn(ExperimentalGlanceApi::class)
    suspend fun drawNow(
        context: Context,
        appWidgetId: Int,
        glanceId: GlanceId,
        allSizes: Boolean = false,
    ): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val sizes = if (allSizes) reportedSizes(options).ifEmpty { return false } else listOf(placedSize(options))
        // If this fails, the session's update still draws the widget, only later.
        return runCatching {
            val state = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            val remoteViews = runComposition(context, glanceId, options, sizes, state).first()
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }.onFailure { AtAGlanceWidgetContent.forget(glanceId) }.isSuccess
    }

    /** Every size the host reported for the placed widget (Android 12+), as the session draws them. */
    private fun reportedSizes(options: Bundle): List<DpSize> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
                .orEmpty()
                .filter { it.width > 0f && it.height > 0f }
                .map { DpSize(it.width.dp, it.height.dp) }
                .distinct()
        } else {
            emptyList()
        }

    /** The placed size the host reported, or the default size before it reports one. */
    private fun placedSize(options: Bundle): DpSize {
        val reported =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                @Suppress("DEPRECATION")
                options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)?.firstOrNull()
            } else {
                null
            }
        if (reported != null && reported.width > 0f && reported.height > 0f) {
            return DpSize(reported.width.dp, reported.height.dp)
        }
        val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return if (width > 0 && height > 0) {
            DpSize(width.dp, height.dp)
        } else {
            DEFAULT_SIZE
        }
    }

    @Composable
    private fun WidgetBody() {
        // Reading the state keeps this composition subscribed to refreshAtAGlanceWidgets' revision
        // bump, so the rows below are re-read on every refresh.
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val content = AtAGlanceWidgetContent.load(context, prefs)
        AtAGlanceWidgetContent.composed(LocalGlanceId.current, content)
        val config = content.config
        val snapshot = content.snapshot
        AtAGlanceWidgetReceiver.scheduleRefresh(context, snapshot.nextChangeMillis)
        val nowPlaying = content.nowPlaying
        val isEmpty = snapshot.rows.isEmpty() && nowPlaying == null

        val size = LocalSize.current
        val widthDp = WidgetLayoutUtils.resolveOr(size.width, DEFAULT_WIDTH_DP.dp)
        val heightDp = WidgetLayoutUtils.resolveOr(size.height, DEFAULT_HEIGHT_DP.dp)
        val resources = context.resources
        val density = resources.displayMetrics.density
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // Identical unless the background follows the system theme.
        val dayColors = NoteWidgetColors.forWidget(config, isSystemDark = false)
        val nightColors = NoteWidgetColors.forWidget(config, isSystemDark = true)

        // With no rows and no date there is nothing to sit on the card, so leave the widget transparent.
        val isBlank = isEmpty && !config.showEmptyDate
        val backgroundModifier =
            when {
                // Launchers reapply an update onto the views they already have, so a missing background
                // would leave the old card showing; clear it explicitly instead.
                config.backgroundAlpha <= 0f || isBlank -> GlanceModifier.background(Color.Transparent)
                // Day/night colors let the launcher switch the card with the system theme.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    GlanceModifier
                        .background(DayNightColorProvider(day = dayColors.background, night = nightColors.background))
                        .cornerRadius(config.borderRadiusDp.dp)
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
                                cornerRadiusPx = config.borderRadiusDp * density,
                            ),
                        ),
                    )
            }

        Column(modifier = GlanceModifier.fillMaxSize().then(backgroundModifier)) {
            if (isEmpty) {
                EmptyContent(context, dayColors, nightColors, config.textScale, config.showEmptyDate)
            } else {
                RowList(
                    context,
                    nowPlaying,
                    snapshot.rows,
                    dayColors,
                    nightColors,
                    config.textScale,
                    AtAGlanceWidgetDimens.rowListHorizontalPadding(config.backgroundAlpha),
                    widthDp,
                    // Bitmaps can't follow the day/night switch, so they take the current theme's color.
                    (if (isDark) nightColors else dayColors).title,
                )
            }
        }
    }

    @Composable
    private fun RowList(
        context: Context,
        nowPlaying: MediaControlsSnapshot?,
        rows: List<AtAGlanceWidgetRow>,
        dayColors: NoteWidgetColors,
        nightColors: NoteWidgetColors,
        textScale: Float,
        horizontalPadding: Dp,
        widthDp: Dp,
        shapeColor: Color,
    ) {
        val rowColors = AtAGlanceWidgetRowColors(dayColors, nightColors)
        val dividerColor =
            DayNightColorProvider(
                day = dayColors.title.copy(alpha = AtAGlanceWidgetDimens.DIVIDER_ALPHA),
                night = nightColors.title.copy(alpha = AtAGlanceWidgetDimens.DIVIDER_ALPHA),
            )
        val lastKey = rows.lastOrNull()?.key
        val divider =
            @Composable {
                Box(
                    modifier =
                        GlanceModifier
                            .fillMaxWidth()
                            .height(AtAGlanceWidgetDimens.DIVIDER_HEIGHT)
                            .background(dividerColor),
                ) {}
            }
        LazyColumn(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .padding(
                        horizontal = horizontalPadding,
                        vertical = AtAGlanceWidgetDimens.LIST_VERTICAL_PADDING,
                    ),
        ) {
            // Media leads, as home's Now Playing card sits above the other rows.
            if (nowPlaying != null) {
                item(itemId = NOW_PLAYING_ITEM_KEY.hashCode().toLong()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        AtAGlanceWidgetNowPlayingRow(context, nowPlaying, rowColors, textScale, widthDp - horizontalPadding * 2, shapeColor)
                        if (lastKey != null) divider()
                    }
                }
            }
            items(rows, itemId = { it.key.hashCode().toLong() }) { row ->
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    AtAGlanceWidgetRowContent(context, row, rowColors, textScale)
                    if (row.key != lastKey) divider()
                }
            }
        }
    }

    /**
     * Nothing to show, so today's day and date, where a tap opens Quick Search. With [showDate] off the
     * widget stays empty and ignores taps.
     */
    @Composable
    private fun EmptyContent(
        context: Context,
        dayColors: NoteWidgetColors,
        nightColors: NoteWidgetColors,
        textScale: Float,
        showDate: Boolean,
    ) {
        if (!showDate) return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .padding(AtAGlanceWidgetDimens.HORIZONTAL_PADDING)
                    .let { modifier ->
                        launchIntent?.let {
                            modifier.clickable(onClick = actionStartActivity(it), rippleOverride = android.R.color.transparent)
                        } ?: modifier
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = atAGlanceWidgetEmptyDate(),
                style =
                    TextStyle(
                        color = DayNightColorProvider(day = dayColors.title, night = nightColors.title),
                        fontSize = AtAGlanceWidgetDimens.EMPTY_DATE_FONT_SIZE * textScale,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
        }
    }
}

/** Today's day and date on one line, like "Friday, Oct 23", shown when there are no items. */
private fun atAGlanceWidgetEmptyDate(): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEMMMd")
    return LocalDate.now().format(DateTimeFormatter.ofPattern(pattern, locale))
}
