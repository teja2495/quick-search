package com.tk.quicksearch.overlay

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Bundle
import android.os.Trace
import android.view.KeyEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.tk.quicksearch.app.startup.StartupCoordinator
import com.tk.quicksearch.app.startup.StartupMode
import com.tk.quicksearch.app.UiSurfaceMemoryManager
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.searchWidget.VoiceSearchHandler
import com.tk.quicksearch.shared.util.lockPortraitOrientationOnPhones

class OverlayActivity : FragmentActivity() {
    private companion object {
        const val TRACE_ON_CREATE_ENTRY = "QS.Startup.OverlayActivity.OnCreate"
    }
    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private lateinit var startupCoordinator: StartupCoordinator
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private var animationToken: Long = 0L
    private var isActivityInstanceTracked = false
    private val voiceInputLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
        }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguageManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection(TRACE_ON_CREATE_ENTRY)
        try {
        AppLanguageManager.applySavedAppLanguage(this)
        // Set transparent background for seamless overlay appearance
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        val navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)

        super.onCreate(savedInstanceState)
        lockPortraitOrientationOnPhones()
        UiSurfaceMemoryManager.onSurfaceCreated()
        isActivityInstanceTracked = true

        // Disable activity opening animation for instant appearance
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        userPreferences = UserAppPreferences(this)
        startupCoordinator =
            StartupCoordinator(
                context = this,
                lifecycleScope = lifecycleScope,
                viewModel = searchViewModel,
                userPreferences = userPreferences,
                mode = StartupMode.OVERLAY,
            )

        if (intent?.getBooleanExtra(OverlayModeController.EXTRA_CLOSE_OVERLAY, false) == true) {
            finish()
            return
        }
        animationToken = intent?.getLongExtra(OverlayModeController.EXTRA_ANIMATION_TOKEN, 0L) ?: 0L
        initializeVoiceSearchHandler()
        handleVoiceIntentIfNeeded(intent)
        handleInitialQueryIfNeeded(intent)

        renderOverlayContent()
        startupCoordinator.scheduleAfterFirstDraw(window)
        } finally {
            Trace.endSection()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(OverlayModeController.EXTRA_CLOSE_OVERLAY, false)) {
            finish()
            return
        }
        animationToken = intent.getLongExtra(OverlayModeController.EXTRA_ANIMATION_TOKEN, animationToken)
        renderOverlayContent()
        handleVoiceIntentIfNeeded(intent)
        handleInitialQueryIfNeeded(intent)
    }

    override fun onStop() {
        super.onStop()
        searchViewModel.handleOnStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isActivityInstanceTracked) return
        isActivityInstanceTracked = false
        UiSurfaceMemoryManager.onSurfaceDestroyed(isChangingConfigurations)
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            UiSurfaceMemoryManager.clearBitmapMemoryCaches()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        UiSurfaceMemoryManager.clearBitmapMemoryCaches()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE && event.action == KeyEvent.ACTION_UP) {
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun initializeVoiceSearchHandler() {
        voiceSearchHandler = VoiceSearchHandler(this, voiceInputLauncher)
    }

    private fun renderOverlayContent() {
        setContent {
            val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val isSystemDarkTheme = isSystemInDarkTheme()
            val wallpaperIsDark by produceState<Boolean?>(
                initialValue = null,
                uiState.backgroundSource,
                uiState.customImageUri,
            ) {
                value =
                    if (uiState.backgroundSource == BackgroundSource.THEME) {
                        null
                    } else {
                        WallpaperUtils.getBackgroundAppearance(
                            context = context,
                            backgroundSource = uiState.backgroundSource,
                            customImageUri = uiState.customImageUri,
                        )?.isDark
                    }
            }
            val useDarkSystemBarsFromTheme =
                when (uiState.appThemeMode) {
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                    AppThemeMode.SYSTEM -> isSystemDarkTheme
                }
            val useDarkSystemBars = wallpaperIsDark ?: useDarkSystemBarsFromTheme
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
                com.tk.quicksearch.reminders.ReminderEditorHost()
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Transparent),
                ) {
                    OverlayRoot(
                        viewModel = searchViewModel,
                        animationToken = animationToken,
                        onCloseRequested = { finish() },
                        reservedKeyboardHeightProvider = { landscape ->
                            userPreferences.getReservedKeyboardHeightDp(landscape).dp
                        },
                        onKeyboardHeightMeasured = { landscape, height ->
                            userPreferences.setReservedKeyboardHeightDp(landscape, height.value)
                        },
                    )
                }
            }
        }
    }

    private fun handleInitialQueryIfNeeded(intent: android.content.Intent?) {
        val query = intent?.getStringExtra(OverlayModeController.EXTRA_INITIAL_QUERY)
            ?.takeIf { it.isNotBlank() } ?: return
        intent.removeExtra(OverlayModeController.EXTRA_INITIAL_QUERY)
        searchViewModel.onQueryChange(query)
    }

    private fun handleVoiceIntentIfNeeded(intent: android.content.Intent?) {
        val shouldStartVoiceSearch =
            intent?.getBooleanExtra(OverlayModeController.EXTRA_START_VOICE_SEARCH, false) ?: false
        if (!shouldStartVoiceSearch) return

        intent?.removeExtra(OverlayModeController.EXTRA_START_VOICE_SEARCH)
        val micAction =
            intent
                ?.getStringExtra(OverlayModeController.EXTRA_MIC_ACTION)
                ?.let { actionString ->
                    MicAction.entries.find { it.value == actionString }
                }
                ?: MicAction.DEFAULT_VOICE_SEARCH
        intent?.removeExtra(OverlayModeController.EXTRA_MIC_ACTION)
        voiceSearchHandler.handleMicAction(micAction)
    }
}
