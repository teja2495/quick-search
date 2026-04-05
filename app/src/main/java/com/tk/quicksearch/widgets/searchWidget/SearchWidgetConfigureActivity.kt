package com.tk.quicksearch.widgets.searchWidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.widgets.WidgetConfigScreen.WidgetConfigScreen
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.utils.applyWidgetPreferences
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetReceiver
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import kotlinx.coroutines.launch

/** Activity for configuring widget preferences when a widget is added or reconfigured. */
class SearchWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetVariant: WidgetVariant = WidgetVariant.STANDARD
    private val searchViewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        widgetVariant = resolveWidgetVariant(appWidgetId)

        setContent {
            val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
            val isSystemDarkTheme = isSystemInDarkTheme()
            val useDarkSystemBars =
                if (uiState.deviceThemeEnabled) {
                    isSystemDarkTheme
                } else {
                    when (uiState.appThemeMode) {
                        AppThemeMode.LIGHT -> false
                        AppThemeMode.DARK -> true
                        AppThemeMode.SYSTEM -> isSystemDarkTheme
                    }
                }
            SideEffect {
                val systemBarStyle =
                    if (useDarkSystemBars) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    }
                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle,
                    navigationBarStyle = systemBarStyle,
                )
            }
            QuickSearchTheme(
                fontScaleMultiplier = uiState.fontScaleMultiplier,
                appTheme = uiState.appTheme,
                appThemeMode = uiState.appThemeMode,
            ) {
                WidgetConfigurationContent(
                    appWidgetId = appWidgetId,
                    widgetVariant = widgetVariant,
                    onConfigurationComplete = { finish() },
                )
            }
        }

        window.decorView.post {
            searchViewModel.startStartupPhasesAfterFirstFrame()
        }
    }

    private suspend fun getGlanceId(appWidgetId: Int): GlanceId? {
        val manager = GlanceAppWidgetManager(this)
        return manager.getGlanceIdBy(appWidgetId)
    }

    private suspend fun loadWidgetPreferences(appWidgetId: Int): WidgetPreferences {
        val glanceId = getGlanceId(appWidgetId) ?: return WidgetPreferences.Default

        val prefs =
            getAppWidgetState(
                context = this,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId,
            )
        return prefs.toWidgetPreferences().enforceVariantConstraints(widgetVariant)
    }

    private suspend fun saveWidgetPreferences(
        appWidgetId: Int,
        prefs: WidgetPreferences,
    ) {
        val glanceId = getGlanceId(appWidgetId) ?: return
        val constrainedPrefs = prefs.enforceVariantConstraints(widgetVariant)

        updateAppWidgetState(context = this, glanceId = glanceId) { mutablePrefs ->
            mutablePrefs.applyWidgetPreferences(constrainedPrefs)
        }
        SearchWidget(widgetVariant).update(this, glanceId)
    }

    private fun createResultIntent(): Intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private fun resolveWidgetVariant(appWidgetId: Int): WidgetVariant {
        val providerClassName =
            AppWidgetManager
                .getInstance(this)
                .getAppWidgetInfo(appWidgetId)
                ?.provider
                ?.className
        return if (providerClassName == CustomButtonsWidgetReceiver::class.java.name) {
            WidgetVariant.CUSTOM_BUTTONS_ONLY
        } else {
            WidgetVariant.STANDARD
        }
    }

    companion object {
    }

    @Composable
    private fun WidgetConfigurationContent(
        appWidgetId: Int,
        widgetVariant: WidgetVariant,
        onConfigurationComplete: () -> Unit,
    ) {
        var config by rememberSaveable {
            mutableStateOf(WidgetPreferences.Default.enforceVariantConstraints(widgetVariant))
        }
        var isLoaded by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(appWidgetId) {
            config = loadWidgetPreferences(appWidgetId)
            isLoaded = true
        }

        WidgetConfigScreen(
            state = config,
            isLoaded = isLoaded,
            isSaveEnabled =
                isLoaded &&
                    (
                        widgetVariant != WidgetVariant.CUSTOM_BUTTONS_ONLY ||
                            config.hasCustomButtons
                    ),
            onStateChange = { config = it.enforceVariantConstraints(widgetVariant) },
            onApply = {
                scope.launch {
                    saveWidgetPreferences(appWidgetId, config)
                    setResult(Activity.RESULT_OK, createResultIntent())
                    onConfigurationComplete()
                }
            },
            onCancel = onConfigurationComplete,
            searchViewModel = searchViewModel,
            widgetVariant = widgetVariant,
            titleResId =
                if (widgetVariant == WidgetVariant.CUSTOM_BUTTONS_ONLY) {
                    R.string.widget_custom_buttons_widget_title
                } else {
                    R.string.widget_settings_title
                },
        )
    }
}
