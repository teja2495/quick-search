package com.tk.quicksearch.overlay

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.app.startup.StartupCoordinator
import com.tk.quicksearch.app.startup.StartupMode
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.searchWidget.VoiceSearchHandler

class OverlayActivity : ComponentActivity() {
    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private lateinit var startupCoordinator: StartupCoordinator
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private var animationToken: Long = 0L
    private val voiceInputLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set transparent background for seamless overlay appearance
        window.setBackgroundDrawableResource(android.R.color.transparent)

        val statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        val navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)

        super.onCreate(savedInstanceState)

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
        startupCoordinator.scheduleAfterFirstFrame(window)
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
            val isSystemDarkTheme = isSystemInDarkTheme()
            val useDarkSystemBars =
                when (uiState.appThemeMode) {
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK -> true
                    AppThemeMode.SYSTEM -> isSystemDarkTheme
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
                backgroundSource = uiState.backgroundSource,
                customImageUri = uiState.customImageUri,
                wallpaperAccentEnabled = uiState.wallpaperAccentEnabled,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Transparent),
                ) {
                    OverlayRoot(
                        viewModel = searchViewModel,
                        animationToken = animationToken,
                        onCloseRequested = { finish() },
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
