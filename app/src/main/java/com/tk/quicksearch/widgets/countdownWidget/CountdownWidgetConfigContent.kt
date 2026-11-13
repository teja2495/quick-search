package com.tk.quicksearch.widgets.countdownWidget

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetLoadingState
import kotlinx.coroutines.launch
import com.tk.quicksearch.widgets.utils.drawNow

/** Loads [appWidgetId]'s Countdown settings, shows [CountdownWidgetConfigScreen] and saves on Save. */
@Composable
fun CountdownWidgetConfigContent(
    appWidgetId: Int,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var config by rememberSaveable { mutableStateOf<CountdownWidgetConfig?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(appWidgetId) {
        if (config == null) config = loadCountdownConfig(context, appWidgetId)
    }

    val loaded = config
    if (loaded == null) {
        Surface(modifier = Modifier.fillMaxSize()) { WidgetLoadingState(innerPadding = PaddingValues()) }
        return
    }
    CountdownWidgetConfigScreen(
        config = loaded,
        onConfigChange = { config = it },
        onSave = {
            scope.launch {
                saveCountdownConfig(context, appWidgetId, loaded)
                onSaved()
            }
        },
        onCancel = onCancel,
    )
}

private suspend fun glanceIdFor(
    context: Context,
    appWidgetId: Int,
): GlanceId? = runCatching { GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId) }.getOrNull()

private suspend fun loadCountdownConfig(
    context: Context,
    appWidgetId: Int,
): CountdownWidgetConfig {
    val glanceId = glanceIdFor(context, appWidgetId) ?: return CountdownWidgetConfig()
    return getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId).toCountdownWidgetConfig(context)
}

private suspend fun saveCountdownConfig(
    context: Context,
    appWidgetId: Int,
    config: CountdownWidgetConfig,
) {
    val glanceId = glanceIdFor(context, appWidgetId) ?: return
    val toSave = if (config.startMillis > 0L) config else config.copy(startMillis = System.currentTimeMillis())
    updateAppWidgetState(context, glanceId) { prefs -> prefs.applyCountdownWidgetConfig(toSave, context) }
    val widget = CountdownWidget()
    widget.drawNow(context, appWidgetId, glanceId, CountdownWidget.DEFAULT_SIZE)
    widget.update(context, glanceId)
    CountdownWidgetReceiver.scheduleNextDayRefresh(context)
}
