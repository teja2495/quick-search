package com.tk.quicksearch.widgets.worldClockWidget

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

/** Loads [appWidgetId]'s World Clock settings, shows [WorldClockWidgetConfigScreen] and saves on Save. */
@Composable
fun WorldClockWidgetConfigContent(
    appWidgetId: Int,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var config by rememberSaveable { mutableStateOf<WorldClockWidgetConfig?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(appWidgetId) {
        if (config == null) config = loadWorldClockConfig(context, appWidgetId)
    }

    val loaded = config
    if (loaded == null) {
        Surface(modifier = Modifier.fillMaxSize()) { WidgetLoadingState(innerPadding = PaddingValues()) }
        return
    }
    WorldClockWidgetConfigScreen(
        config = loaded,
        onConfigChange = { config = it },
        onSave = {
            scope.launch {
                saveWorldClockConfig(context, appWidgetId, loaded)
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

private suspend fun loadWorldClockConfig(
    context: Context,
    appWidgetId: Int,
): WorldClockWidgetConfig {
    val glanceId = glanceIdFor(context, appWidgetId) ?: return WorldClockWidgetConfig()
    return getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId).toWorldClockWidgetConfig(context)
}

private suspend fun saveWorldClockConfig(
    context: Context,
    appWidgetId: Int,
    config: WorldClockWidgetConfig,
) {
    val glanceId = glanceIdFor(context, appWidgetId) ?: return
    updateAppWidgetState(context, glanceId) { prefs -> prefs.applyWorldClockWidgetConfig(config, context) }
    WorldClockWidget().update(context, glanceId)
}
