package com.tk.quicksearch.widgets.searchWidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.notes.NotesRoomStore
import com.tk.quicksearch.search.data.preferences.AppGlanceItemSettings
import com.tk.quicksearch.search.data.preferences.newWidgetGlanceItems
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.widgets.widgetConfigScreen.WidgetConfigScreen
import com.tk.quicksearch.widgets.mediaControlsWidget.rememberMediaControlsAccess
import com.tk.quicksearch.widgets.utils.launcherWidgetContentScale
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetTheme
import com.tk.quicksearch.widgets.utils.WidgetVariant
import com.tk.quicksearch.widgets.utils.hasStoredWidgetConfig
import com.tk.quicksearch.widgets.utils.applyWidgetPreferences
import com.tk.quicksearch.widgets.countdownWidget.CountdownWidgetConfigContent
import com.tk.quicksearch.widgets.countdownWidget.CountdownWidgetReceiver
import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidgetConfigContent
import com.tk.quicksearch.widgets.worldClockWidget.WorldClockWidgetReceiver
import com.tk.quicksearch.widgets.customButtonsWidget.CustomButtonsWidgetReceiver
import com.tk.quicksearch.widgets.mediaControlsWidget.MediaControlsWidgetReceiver
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetPickerScreen
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetReceiver
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidget
import com.tk.quicksearch.widgets.atAGlanceWidget.AtAGlanceWidgetReceiver
import com.tk.quicksearch.widgets.noteWidget.withNoteWidgetDefaults
import com.tk.quicksearch.widgets.utils.glanceWidgetFor
import com.tk.quicksearch.widgets.utils.defaultWidgetSize
import com.tk.quicksearch.widgets.utils.drawNow
import com.tk.quicksearch.widgets.utils.enforceVariantConstraints
import com.tk.quicksearch.widgets.utils.toWidgetPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.util.lockPortraitOrientationOnPhones

/** Activity for configuring widget preferences when a widget is added or reconfigured. */
class SearchWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetVariant: WidgetVariant = WidgetVariant.STANDARD
    private val searchViewModel: SearchViewModel by viewModels()
    private data class LoadedWidgetPreferences(
        val preferences: WidgetPreferences,
        val hasStoredConfig: Boolean,
    )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguageManager.applySavedAppLanguage(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        lockPortraitOrientationOnPhones()
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
        val providerClass = providerClassName(appWidgetId)
        val isCountdownWidget = providerClass == CountdownWidgetReceiver::class.java.name
        val isWorldClockWidget = providerClass == WorldClockWidgetReceiver::class.java.name

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
                useSystemFont = uiState.useSystemFont,
                appTheme = uiState.appTheme,
                appThemeMode = uiState.appThemeMode,
                backgroundSource = uiState.backgroundSource,
                customImageUri = uiState.customImageUri,
                accentColorMode = uiState.accentColorMode,
                customAccentColorArgb = uiState.customAccentColorArgb,
                deviceThemeEnabled = uiState.deviceThemeEnabled,
            ) {
                if (isCountdownWidget) {
                    CountdownWidgetConfigContent(
                        appWidgetId = appWidgetId,
                        onSaved = {
                            setResult(Activity.RESULT_OK, createResultIntent())
                            finish()
                        },
                        onCancel = { finish() },
                    )
                } else if (isWorldClockWidget) {
                    WorldClockWidgetConfigContent(
                        appWidgetId = appWidgetId,
                        onSaved = {
                            setResult(Activity.RESULT_OK, createResultIntent())
                            finish()
                        },
                        onCancel = { finish() },
                    )
                } else {
                    WidgetConfigurationContent(
                        appWidgetId = appWidgetId,
                        widgetVariant = widgetVariant,
                        deviceThemeEnabled = uiState.deviceThemeEnabled,
                        useDarkThemeForDeviceTheme = useDarkSystemBars,
                        onConfigurationComplete = { finish() },
                    )
                }
            }
        }

        window.decorView.post {
            searchViewModel.startStartupPhasesAfterFirstFrame()
        }
    }

    private suspend fun getGlanceId(appWidgetId: Int): GlanceId? {
        val manager = GlanceAppWidgetManager(this)
        return try {
            manager.getGlanceIdBy(appWidgetId)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private suspend fun loadWidgetPreferences(appWidgetId: Int): LoadedWidgetPreferences {
        val glanceId =
            getGlanceId(appWidgetId)
                ?: return LoadedWidgetPreferences(WidgetPreferences.Default, hasStoredConfig = false)

        val prefs =
            getAppWidgetState(
                context = this,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId,
            )
        return LoadedWidgetPreferences(
            preferences = prefs.toWidgetPreferences(this@SearchWidgetConfigureActivity).enforceVariantConstraints(widgetVariant),
            hasStoredConfig = prefs.hasStoredWidgetConfig(),
        )
    }

    private suspend fun saveWidgetPreferences(
        appWidgetId: Int,
        prefs: WidgetPreferences,
    ) {
        val glanceId = getGlanceId(appWidgetId) ?: return
        val constrainedPrefs = prefs.enforceVariantConstraints(widgetVariant)

        updateAppWidgetState(context = this, glanceId = glanceId) { mutablePrefs ->
            mutablePrefs.applyWidgetPreferences(constrainedPrefs, this@SearchWidgetConfigureActivity)
        }
        val widget = glanceWidgetFor(widgetVariant)
        // update() alone waits for Glance's session worker, so saved settings would show up late.
        if (widget is AtAGlanceWidget) {
            widget.drawNow(this, appWidgetId, glanceId)
        } else {
            widget.drawNow(this, appWidgetId, glanceId, defaultWidgetSize(widgetVariant))
        }
        widget.update(this, glanceId)
    }

    private fun createResultIntent(): Intent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

    private fun providerClassName(appWidgetId: Int): String? =
        AppWidgetManager
            .getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className

    private fun resolveWidgetVariant(appWidgetId: Int): WidgetVariant {
        return when (providerClassName(appWidgetId)) {
            CustomButtonsWidgetReceiver::class.java.name -> WidgetVariant.CUSTOM_BUTTONS_ONLY
            MediaControlsWidgetReceiver::class.java.name -> WidgetVariant.MEDIA_CONTROLS
            NoteWidgetReceiver::class.java.name -> WidgetVariant.NOTE
            AtAGlanceWidgetReceiver::class.java.name -> WidgetVariant.AT_A_GLANCE
            else -> WidgetVariant.STANDARD
        }
    }

    private fun resolveDeviceThemeBackgroundColor(useDarkTheme: Boolean): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamicScheme =
                if (useDarkTheme) {
                    dynamicDarkColorScheme(this)
                } else {
                    dynamicLightColorScheme(this)
                }
            return dynamicScheme.primary.toArgb()
        }
        return if (useDarkTheme) {
            AppColors.WidgetBackgroundDark.toArgb()
        } else {
            AppColors.WidgetBackgroundLight.toArgb()
        }
    }

    @Composable
    private fun WidgetConfigurationContent(
        appWidgetId: Int,
        widgetVariant: WidgetVariant,
        deviceThemeEnabled: Boolean,
        useDarkThemeForDeviceTheme: Boolean,
        onConfigurationComplete: () -> Unit,
    ) {
        var config by rememberSaveable {
            mutableStateOf(WidgetPreferences.Default.enforceVariantConstraints(widgetVariant))
        }
        var isLoaded by rememberSaveable { mutableStateOf(false) }
        var showNotePicker by rememberSaveable { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(appWidgetId, deviceThemeEnabled, useDarkThemeForDeviceTheme) {
            // The note and At a Glance widgets ignore the app theme, so reloading on a theme change would only
            // discard the note the user just picked.
            if ((widgetVariant == WidgetVariant.NOTE || widgetVariant == WidgetVariant.AT_A_GLANCE) && isLoaded) {
                return@LaunchedEffect
            }
            val loaded = loadWidgetPreferences(appWidgetId)
            config = loaded.preferences
            if (widgetVariant == WidgetVariant.NOTE) {
                val noteId = config.noteId
                if (noteId == null) config = config.withNoteWidgetDefaults()
                // A new widget, or one whose note was deleted, starts at the note picker.
                showNotePicker =
                    noteId == null ||
                    withContext(Dispatchers.IO) { NotesRoomStore(this@SearchWidgetConfigureActivity).getById(noteId) } == null
            } else if (!loaded.hasStoredConfig && widgetVariant == WidgetVariant.AT_A_GLANCE) {
                val glanceItems =
                    withContext(Dispatchers.IO) {
                        newWidgetGlanceItems(AppGlanceItemSettings(this@SearchWidgetConfigureActivity))
                    }
                config = config.withNoteWidgetDefaults().copy(glanceItems = glanceItems)
            } else if (!loaded.hasStoredConfig && widgetVariant == WidgetVariant.MEDIA_CONTROLS) {
                config = config.copy(theme = WidgetTheme.DARK, backgroundColor = null)
            } else if (!loaded.hasStoredConfig && deviceThemeEnabled) {
                config =
                    config.copy(
                        backgroundColor = resolveDeviceThemeBackgroundColor(useDarkThemeForDeviceTheme),
                        useDeviceThemeBackground = true,
                    )
            }
            isLoaded = true
        }

        if (showNotePicker) {
            // Closing the picker before any note is chosen leaves nothing to configure.
            val closePicker = { if (config.noteId == null) onConfigurationComplete() else showNotePicker = false }
            BackHandler(onBack = closePicker)
            NoteWidgetPickerScreen(
                selectedNoteId = config.noteId,
                onSelect = { note ->
                    config = config.copy(noteId = note.noteId)
                    showNotePicker = false
                },
                onClose = closePicker,
            )
            return
        }

        // Media Controls cannot show anything without notification access, so it is required.
        val hasMediaAccess = widgetVariant != WidgetVariant.MEDIA_CONTROLS || rememberMediaControlsAccess()

        WidgetConfigScreen(
            state = config,
            launcherContentScale = remember(appWidgetId) { launcherWidgetContentScale(this, appWidgetId) },
            showDeviceThemeOption = deviceThemeEnabled,
            isLoaded = isLoaded,
            isSaveEnabled =
                isLoaded &&
                    (
                        widgetVariant != WidgetVariant.CUSTOM_BUTTONS_ONLY ||
                            config.hasCustomButtons
                    ) &&
                    (widgetVariant != WidgetVariant.NOTE || config.noteId != null) &&
                    hasMediaAccess,
            showNotificationAccessBanner = !hasMediaAccess,
            saveLabelResId =
                if (hasMediaAccess) R.string.dialog_save else R.string.settings_overlay_source_needs_permission,
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
                when (widgetVariant) {
                    WidgetVariant.CUSTOM_BUTTONS_ONLY -> R.string.widget_custom_buttons_widget_title
                    WidgetVariant.MEDIA_CONTROLS -> R.string.widget_media_controls_widget_title
                    WidgetVariant.NOTE -> R.string.widget_note_widget_title
                    WidgetVariant.AT_A_GLANCE -> R.string.widget_at_a_glance_widget_title
                    WidgetVariant.STANDARD -> R.string.widget_settings_title
                },
            onChangeNote = { showNotePicker = true },
        )
    }
}
