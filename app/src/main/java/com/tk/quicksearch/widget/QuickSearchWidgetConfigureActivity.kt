package com.tk.quicksearch.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

import androidx.activity.SystemBarStyle

/**
 * Activity for configuring widget preferences when a widget is added or reconfigured.
 */
class QuickSearchWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            QuickSearchTheme {
                WidgetConfigurationContent(
                    appWidgetId = appWidgetId,
                    onConfigurationComplete = { finish() }
                )
            }
        }
    }

    /**
     * Gets the GlanceId for the given appWidgetId, or null if not found.
     */
    private suspend fun getGlanceId(appWidgetId: Int): GlanceId? {
        val manager = GlanceAppWidgetManager(this)
        return manager.getGlanceIdBy(appWidgetId)
    }

    /**
     * Loads widget preferences from storage for the given appWidgetId.
     */
    private suspend fun loadWidgetPreferences(appWidgetId: Int): QuickSearchWidgetPreferences {
        val glanceId = getGlanceId(appWidgetId) ?: return QuickSearchWidgetPreferences.Default

        val prefs = getAppWidgetState(
            context = this,
            definition = PreferencesGlanceStateDefinition,
            glanceId = glanceId
        )
        return prefs.toWidgetPreferences()
    }

    /**
     * Saves widget preferences and updates the widget display.
     */
    private suspend fun saveWidgetPreferences(
        appWidgetId: Int,
        prefs: QuickSearchWidgetPreferences
    ) {
        val glanceId = getGlanceId(appWidgetId) ?: return

        updateAppWidgetState(
            context = this,
            glanceId = glanceId
        ) { mutablePrefs ->
            mutablePrefs.applyWidgetPreferences(prefs)
        }
        QuickSearchWidget().update(this, glanceId)
    }

    /**
     * Creates the result intent with the appWidgetId.
     */
    private fun createResultIntent(): Intent {
        return Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

    @Composable
    private fun WidgetConfigurationContent(
        appWidgetId: Int,
        onConfigurationComplete: () -> Unit
    ) {
        val context = LocalContext.current
        var config by rememberSaveable { mutableStateOf(QuickSearchWidgetPreferences.Default) }
        var isLoaded by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(appWidgetId) {
            config = loadWidgetPreferences(appWidgetId)
            isLoaded = true
        }

        QuickSearchWidgetConfigScreen(
            state = config,
            isLoaded = isLoaded,
            onStateChange = { config = it },
            onApply = {
                scope.launch {
                    saveWidgetPreferences(appWidgetId, config)
                    setResult(Activity.RESULT_OK, createResultIntent())
                    onConfigurationComplete()
                }
            },
            onCancel = onConfigurationComplete
        )
    }
}

