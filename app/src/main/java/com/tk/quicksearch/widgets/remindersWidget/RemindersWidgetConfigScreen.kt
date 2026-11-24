package com.tk.quicksearch.widgets.remindersWidget

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetBackgroundSection
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetTextColorSection
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.drawNow
import com.tk.quicksearch.widgets.utils.rememberWidgetPreviewWallpaper
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetConfigScaffold
import com.tk.quicksearch.widgets.widgetConfigScreen.components.SliderRow
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetLoadingState
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSettingTile
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSlidersSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetTextSizeSection
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Loads [appWidgetId]'s Reminders widget settings, shows [RemindersWidgetConfigScreen] and saves on Save. */
@Composable
fun RemindersWidgetConfigContent(
    appWidgetId: Int,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var config by rememberSaveable { mutableStateOf<RemindersWidgetConfig?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(appWidgetId) {
        if (config == null) config = loadRemindersConfig(context, appWidgetId)
    }

    val loaded = config
    if (loaded == null) {
        Surface(modifier = Modifier.fillMaxSize()) { WidgetLoadingState(innerPadding = PaddingValues()) }
        return
    }
    RemindersWidgetConfigScreen(
        config = loaded,
        onConfigChange = { config = it },
        onSave = {
            scope.launch {
                saveRemindersConfig(context, appWidgetId, loaded)
                onSaved()
            }
        },
        onCancel = onCancel,
    )
}

/** Settings for a Reminders widget: how many days ahead it lists, and its look. */
@Composable
fun RemindersWidgetConfigScreen(
    config: RemindersWidgetConfig,
    onConfigChange: (RemindersWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    WidgetConfigScaffold(
        title = stringResource(R.string.widget_reminders_widget_title),
        onCancel = onCancel,
        onSave = onSave,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Box(modifier = Modifier.padding(horizontal = WidgetConfigConstants.HORIZONTAL_PADDING)) {
                RemindersWidgetPreview(config = config, wallpaperBitmap = rememberWidgetPreviewWallpaper())
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = WidgetConfigConstants.HORIZONTAL_PADDING,
                            top = WidgetConfigConstants.SECTION_SPACING,
                            end = WidgetConfigConstants.HORIZONTAL_PADDING,
                            bottom = WidgetConfigConstants.SCROLLABLE_SECTION_BOTTOM_PADDING,
                        ),
                verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING),
            ) {
                WidgetSettingTile {
                    val days = config.upcomingDays
                    SliderRow(
                        // Days are counted after today, so 1 lists today and tomorrow.
                        label =
                            if (days == 1) {
                                AnnotatedString(stringResource(R.string.widget_reminders_show_today_tomorrow))
                            } else {
                                boldCount(pluralStringResource(R.plurals.widget_reminders_show_next_days, days, days), days)
                            },
                        labelTextStyle = MaterialTheme.typography.bodyLarge,
                        value = days.toFloat(),
                        valueRange =
                            RemindersWidgetDefaults.UPCOMING_DAYS_MIN.toFloat()..RemindersWidgetDefaults.UPCOMING_DAYS_MAX.toFloat(),
                        // One stop per day between the ends.
                        steps = RemindersWidgetDefaults.UPCOMING_DAYS_MAX - RemindersWidgetDefaults.UPCOMING_DAYS_MIN - 1,
                        // The label already says the count.
                        valueFormatter = { "" },
                        onValueChange = { onConfigChange(config.copy(upcomingDays = it.roundToInt())) },
                    )
                }
                SegmentedGroup {
                    NoteWidgetBackgroundSection(
                        state = config.style,
                        index = 0,
                        count = 2,
                        onStateChange = { onConfigChange(config.copy(style = it)) },
                    )
                    NoteWidgetTextColorSection(
                        state = config.style,
                        index = 1,
                        count = 2,
                        onStateChange = { onConfigChange(config.copy(style = it)) },
                    )
                }
                WidgetSlidersSection(
                    state = config.style,
                    showBorderControls = false,
                    onStateChange = { onConfigChange(config.copy(style = it)) },
                )
                WidgetTextSizeSection(
                    state = config.style,
                    onStateChange = { onConfigChange(config.copy(style = it)) },
                )
            }
        }
    }
}

/** [text] with the first [count] in it bold; the number is formatted as the plural resource formats it. */
@Composable
private fun boldCount(
    text: String,
    count: Int,
): AnnotatedString {
    val locale = LocalConfiguration.current.locales[0]
    val number = String.format(locale, "%d", count)
    val start = text.indexOf(number)
    return buildAnnotatedString {
        append(text)
        if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + number.length)
    }
}

private suspend fun glanceIdFor(
    context: Context,
    appWidgetId: Int,
): GlanceId? = runCatching { GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId) }.getOrNull()

private suspend fun loadRemindersConfig(
    context: Context,
    appWidgetId: Int,
): RemindersWidgetConfig {
    val glanceId = glanceIdFor(context, appWidgetId) ?: return RemindersWidgetConfig()
    return getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId).toRemindersWidgetConfig(context)
}

private suspend fun saveRemindersConfig(
    context: Context,
    appWidgetId: Int,
    config: RemindersWidgetConfig,
) {
    val glanceId = glanceIdFor(context, appWidgetId) ?: return
    updateAppWidgetState(context, glanceId) { prefs -> prefs.applyRemindersWidgetConfig(config, context) }
    val widget = RemindersWidget()
    widget.drawNow(context, appWidgetId, glanceId, RemindersWidget.DEFAULT_SIZE)
    widget.update(context, glanceId)
}
