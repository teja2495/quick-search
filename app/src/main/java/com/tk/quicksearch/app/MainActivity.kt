package com.tk.quicksearch.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.tk.quicksearch.navigation.MainContent
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.FeedbackUtils
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.widget.QuickSearchWidget
import com.tk.quicksearch.widget.voiceSearch.MicAction
import com.tk.quicksearch.widget.voiceSearch.VoiceSearchHandler

class MainActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private val voiceInputLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
            }
    private val showReviewPromptDialog = mutableStateOf(false)
    private val showFeedbackDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Set transparent background initially for seamless launch
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Must be called before super.onCreate for edge-to-edge to work correctly on all versions
        val statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        val navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)

        super.onCreate(savedInstanceState)

        // Disable activity opening animation for instant appearance
        overridePendingTransition(0, 0)

        initializePreferences()
        initializeVoiceSearchHandler()
        // Initialize ViewModel early to start loading cached data immediately
        // This ensures cached apps are ready when UI renders
        searchViewModel

        // PRIORITY: Preload wallpaper immediately for seamless visual foundation
        // This ensures wallpaper is available when SearchScreen renders, providing
        // instant visual feedback alongside search bar and app list
        lifecycleScope.launch(Dispatchers.IO) {
            WallpaperUtils.preloadWallpaper(this@MainActivity)
        }

        setupContent()
        refreshPermissionStateIfNeeded()
        handleIntent(intent)

        // Track first app open time and app open count
        // Only track after first launch is complete
        window.decorView.post {
            // Track first app open time and app open count
            // Only track after first launch is complete
            if (!userPreferences.isFirstLaunch()) {
                userPreferences.recordFirstAppOpenTime()
                userPreferences.incrementAppOpenCount()

                // Reset update check session flag at app start
                userPreferences.resetUpdateCheckSession()

                // Check for app updates first (higher priority)
                UpdateHelper.checkForUpdates(this, userPreferences)

                // Only show review prompt if no update check was performed
                // This prevents both prompts from appearing simultaneously
                if (!userPreferences.hasShownUpdateCheckThisSession()) {
                    if (userPreferences.shouldShowReviewPrompt()) {
                        showReviewPromptDialog.value = true
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        searchViewModel.handleOnStop()
    }

    private fun initializePreferences() {
        userPreferences = UserAppPreferences(this)
    }

    private fun initializeVoiceSearchHandler() {
        voiceSearchHandler = VoiceSearchHandler(this, voiceInputLauncher)
    }

    private fun setupContent() {
        setContent {
            QuickSearchTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                ) {
                    MainContent(
                            context = this@MainActivity,
                            userPreferences = userPreferences,
                            searchViewModel = searchViewModel,
                            onSearchBackPressed = { moveTaskToBack(true) }
                    )
                    if (showReviewPromptDialog.value) {
                        EnjoyingAppDialog(
                                onYes = {
                                    showReviewPromptDialog.value = false
                                    ReviewHelper.requestReviewIfEligible(
                                            this@MainActivity,
                                            userPreferences
                                    )
                                },
                                onNo = {
                                    showReviewPromptDialog.value = false
                                    showFeedbackDialog.value = true
                                    userPreferences.recordReviewPromptTime()
                                    userPreferences.recordAppOpenCountAtPrompt()
                                    userPreferences.incrementReviewPromptedCount()
                                },
                                onDismiss = { showReviewPromptDialog.value = false }
                        )
                    }
                    if (showFeedbackDialog.value) {
                        SendFeedbackDialog(
                                onSend = { feedbackText ->
                                    FeedbackUtils.launchFeedbackEmail(this@MainActivity, feedbackText)
                                },
                                onDismiss = { showFeedbackDialog.value = false }
                        )
                    }
                }
            }
        }
    }

    private fun refreshPermissionStateIfNeeded() {
        if (!userPreferences.isFirstLaunch()) {
            searchViewModel.handleOptionalPermissionChange()
        }
    }

    private fun handleIntent(intent: Intent?) {
        // Handle voice search from widget
        val shouldStartVoiceSearch =
                intent?.getBooleanExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH, false) ?: false
        if (shouldStartVoiceSearch) {
            intent?.removeExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH)
            val micActionString = intent?.getStringExtra(QuickSearchWidget.EXTRA_MIC_ACTION)
            val micAction =
                    micActionString?.let { actionString ->
                        MicAction.entries.find { it.value == actionString }
                    }
                            ?: MicAction.DEFAULT_VOICE_SEARCH
            voiceSearchHandler.handleMicAction(micAction)
        }
        // ACTION_ASSIST is handled implicitly - search interface is always ready
    }
}
