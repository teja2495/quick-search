package com.tk.quicksearch

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.navigation.MainContent
import com.tk.quicksearch.permissions.PermissionsScreen
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.ui.SearchRoute
import com.tk.quicksearch.settings.main.SettingsRoute
import com.tk.quicksearch.settings.main.SettingsDetailRoute
import com.tk.quicksearch.settings.main.SettingsDetailType
import com.tk.quicksearch.setup.SearchEngineSetupScreen
import com.tk.quicksearch.setup.FinalSetupScreen
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.ReviewHelper
import com.tk.quicksearch.util.UpdateHelper
import com.tk.quicksearch.search.ui.VoiceSearchHandler
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.widget.QuickSearchWidget
import com.tk.quicksearch.search.ui.MicAction


class MainActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private val voiceInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate for edge-to-edge to work correctly on all versions
        val statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        val navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle, navigationBarStyle)
        
        super.onCreate(savedInstanceState)
        
        initializePreferences()
        initializeVoiceSearchHandler()
        // Initialize ViewModel early to start loading cached data immediately
        // This ensures cached apps are ready when UI renders
        searchViewModel
        setupContent()
        refreshPermissionStateIfNeeded()
        handleIntent(intent)

        // Defer wallpaper preload to after first frame to avoid blocking startup
        window.decorView.post {
            WallpaperUtils.preloadWallpaper(this)
            
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
                    ReviewHelper.requestReviewIfEligible(this, userPreferences)
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
                MainContent(
                    context = this,
                    userPreferences = userPreferences,
                    searchViewModel = searchViewModel,
                    onSearchBackPressed = { moveTaskToBack(true) }
                )
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
        val shouldStartVoiceSearch = intent
            ?.getBooleanExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH, false)
            ?: false
        if (shouldStartVoiceSearch) {
            intent?.removeExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH)
            val micActionString = intent?.getStringExtra(QuickSearchWidget.EXTRA_MIC_ACTION)
            val micAction = micActionString?.let { actionString ->
                MicAction.entries.find { it.value == actionString }
            } ?: MicAction.DEFAULT_VOICE_SEARCH
            voiceSearchHandler.handleMicAction(micAction)
        }
        // ACTION_ASSIST is handled implicitly - search interface is always ready
    }


}
