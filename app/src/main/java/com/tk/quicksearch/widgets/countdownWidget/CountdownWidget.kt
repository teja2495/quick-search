package com.tk.quicksearch.widgets.countdownWidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
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
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider as DayNightColorProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.searchWidget.SearchWidgetConfigureActivity
import com.tk.quicksearch.widgets.utils.WidgetBitmapUtils
import com.tk.quicksearch.widgets.utils.WidgetLayoutUtils
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Default size (2 columns by 2 rows) used until the host reports the placed widget's size. */
private const val DEFAULT_SIZE_DP = 110f

class CountdownWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        /** Drawn at until the host reports the placed widget's size. */
        val DEFAULT_SIZE = DpSize(DEFAULT_SIZE_DP.dp, DEFAULT_SIZE_DP.dp)
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
        // Reading the state keeps this composition subscribed to refreshCountdownWidgets' revision
        // bump, so the text below is recomputed for the new day.
        val prefs = currentState<Preferences>()
        val context = LocalContext.current
        val config = prefs.toCountdownWidgetConfig(context)

        val size = LocalSize.current
        val widthDp = WidgetLayoutUtils.resolveOr(size.width, DEFAULT_SIZE_DP.dp)
        val heightDp = WidgetLayoutUtils.resolveOr(size.height, DEFAULT_SIZE_DP.dp)
        val resources = context.resources
        val density = resources.displayMetrics.density
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // Identical unless the background follows the system theme.
        val dayColors = CountdownWidgetColors.forWidget(config, isSystemDark = false)
        val nightColors = CountdownWidgetColors.forWidget(config, isSystemDark = true)

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

        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .then(backgroundModifier)
                    .padding(CountdownWidgetDimens.CONTENT_PADDING)
                    .clickable(
                        onClick = actionStartActivity(configureIntent(context, appWidgetId)),
                        rippleOverride = android.R.color.transparent,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val secondaryColor = DayNightColorProvider(day = dayColors.secondaryText, night = nightColors.secondaryText)
            val content = countdownContent(context, config)
            if (content.title != null) {
                Text(
                    text = content.title,
                    style =
                        TextStyle(
                            color = secondaryColor,
                            fontSize = CountdownWidgetDimens.titleSizeSp(config.textSizeSp).sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                    maxLines = 2,
                )
                Spacer(modifier = GlanceModifier.height(CountdownWidgetDimens.TITLE_SPACING))
            }
            Text(
                text = content.text,
                style =
                    TextStyle(
                        color =
                            if (content.isPlaceholder) {
                                secondaryColor
                            } else {
                                DayNightColorProvider(day = dayColors.text, night = nightColors.text)
                            },
                        fontSize = (if (content.isPlaceholder) CountdownWidgetDimens.PLACEHOLDER_TEXT_SIZE_SP else config.textSizeSp).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                maxLines = 3,
            )
            if (content.progress != null) {
                Spacer(modifier = GlanceModifier.height(CountdownWidgetDimens.PROGRESS_SPACING))
                LinearProgressIndicator(
                    progress = content.progress,
                    modifier = GlanceModifier.fillMaxWidth().height(CountdownWidgetDimens.PROGRESS_HEIGHT),
                    color = DayNightColorProvider(day = dayColors.text, night = nightColors.text),
                    backgroundColor =
                        DayNightColorProvider(day = dayColors.progressTrack, night = nightColors.progressTrack),
                )
            }
        }
    }

    /** Opens this widget's settings, where its date and look can be changed. */
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

/** Text shared by the placed widget and the configure-screen preview. */
internal data class CountdownContent(
    val title: String?,
    val text: String,
    val progress: Float?,
    val isPlaceholder: Boolean,
)

internal fun countdownContent(
    context: Context,
    config: CountdownWidgetConfig,
    nowMillis: Long = System.currentTimeMillis(),
): CountdownContent {
    val title = config.title.trim().takeIf { it.isNotEmpty() }
    val targetEpochDay =
        config.targetEpochDay
            ?: return CountdownContent(
                title = title,
                text = context.getString(R.string.calendar_create_event_select_date),
                progress = null,
                isPlaceholder = true,
            )
    val zone = ZoneId.systemDefault()
    val value =
        CountdownCalculator.compute(
            format = config.format,
            today = LocalDate.now(zone),
            target = LocalDate.ofEpochDay(targetEpochDay),
            nowMillis = nowMillis,
            startMillis = config.startMillis,
            zone = zone,
        )
    return CountdownContent(
        title = title,
        text = CountdownTextFormatter.format(context, value),
        progress = (value as? CountdownValue.Progress)?.fraction?.toFloat(),
        isPlaceholder = false,
    )
}

internal object CountdownWidgetDimens {
    val CONTENT_PADDING = 12.dp
    val TITLE_SPACING = 4.dp
    val PROGRESS_SPACING = 10.dp
    val PROGRESS_HEIGHT = 6.dp
    const val PLACEHOLDER_TEXT_SIZE_SP = 14f

    /** Height of the configure-screen preview, roughly a 2x2 widget on a phone launcher. */
    val PREVIEW_SIZE = 170.dp

    private const val TITLE_SIZE_RATIO = 0.5f
    private const val TITLE_MIN_SP = 12f

    fun titleSizeSp(textSizeSp: Float): Float = (textSizeSp * TITLE_SIZE_RATIO).coerceAtLeast(TITLE_MIN_SP)
}
