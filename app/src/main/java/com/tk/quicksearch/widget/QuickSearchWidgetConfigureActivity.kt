package com.tk.quicksearch.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import kotlinx.coroutines.launch

class QuickSearchWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            QuickSearchTheme {
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
                            setResult(
                                Activity.RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            )
                            finish()
                        }
                    },
                    onCancel = { finish() }
                )
            }
        }
    }

    private suspend fun loadWidgetPreferences(appWidgetId: Int): QuickSearchWidgetPreferences {
        val manager = GlanceAppWidgetManager(this)
        val glanceId = manager.getGlanceIdBy(appWidgetId)
        if (glanceId != null) {
            val prefs = getAppWidgetState(
                context = this,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId
            )
            return prefs.toWidgetPreferences()
        }
        return QuickSearchWidgetPreferences.Default
    }

    private suspend fun saveWidgetPreferences(
        appWidgetId: Int,
        prefs: QuickSearchWidgetPreferences
    ) {
        val manager = GlanceAppWidgetManager(this)
        val glanceId = manager.getGlanceIdBy(appWidgetId) ?: return

        updateAppWidgetState(
            context = this,
            glanceId = glanceId
        ) { mutablePrefs ->
            mutablePrefs.applyWidgetPreferences(prefs)
        }
        QuickSearchWidget().update(this, glanceId)
    }
}

