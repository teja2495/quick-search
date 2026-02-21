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
import com.tk.quicksearch.navigation.MainContent
import com.tk.quicksearch.navigation.NavigationRequest
import com.tk.quicksearch.navigation.RootDestination
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.overlay.OverlayModeController
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.FeedbackUtils
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.widget.QuickSearchWidget
import com.tk.quicksearch.widget.voiceSearch.MicAction
import com.tk.quicksearch.widget.voiceSearch.VoiceSearchHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_VOICE_SEARCH_SHORTCUT = "com.tk.quicksearch.action.VOICE_SEARCH_SHORTCUT"
    }

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private val voiceInputLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
        }
    private val showReviewPromptDialog = mutableStateOf(false)
    private val showFeedbackDialog = mutableStateOf(false)
    private val navigationRequest = mutableStateOf<NavigationRequest?>(null)

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

        if (launchOverlayIfNeeded(intent)) {
            return
        }

        initializeVoiceSearchHandler()
        // Initialize ViewModel early to start loading cached data immediately
        // This ensures cached apps are ready when UI renders
        searchViewModel

        // PRIORITY: Preload wallpaper immediately for seamless visual foundation
        // This ensures wallpaper is available when SearchScreen renders, providing
        // instant visual feedback alongside search bar and app list
        lifecycleScope.launch(Dispatchers.IO) { WallpaperUtils.preloadWallpaper(this@MainActivity) }

        // Handle intent BEFORE composing UI to avoid briefly showing the Search screen
        // (which auto-focuses the search bar and can flash the keyboard) before navigating.
        handleIntent(intent)

        setupContent()
        refreshPermissionStateIfNeeded()

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
        if (launchOverlayIfNeeded(intent)) {
            return
        }
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        searchViewModel.handleOnStop()
    }

    private fun initializePreferences() {
        userPreferences = UserAppPreferences(this)
    }

    private fun launchOverlayIfNeeded(intent: Intent?): Boolean {
        val forceNormalLaunch =
            intent?.getBooleanExtra(OverlayModeController.EXTRA_FORCE_NORMAL_LAUNCH, false)
                ?: false
        if (!forceNormalLaunch && userPreferences.isOverlayModeEnabled()) {
            val isVoiceShortcutLaunch = intent?.action == ACTION_VOICE_SEARCH_SHORTCUT
            val isAssistantLaunch = intent?.action == Intent.ACTION_ASSIST
            val startVoiceForAssistant =
                isAssistantLaunch && userPreferences.isAssistantLaunchVoiceModeEnabled()
            val startVoiceFromShortcut = isVoiceShortcutLaunch
            val startVoiceFromWidget =
                intent?.getBooleanExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH, false) ?: false
            val micAction =
                intent
                    ?.getStringExtra(QuickSearchWidget.EXTRA_MIC_ACTION)
                    ?.let { actionString ->
                        MicAction.entries.find { it.value == actionString }
                    }
                    ?: MicAction.DEFAULT_VOICE_SEARCH
            OverlayModeController.startOverlay(
                context = this,
                startVoiceSearch =
                    startVoiceForAssistant || startVoiceFromShortcut || startVoiceFromWidget,
                micAction = micAction,
            )
            finish()
            return true
        }
        return false
    }

    private fun initializeVoiceSearchHandler() {
        voiceSearchHandler = VoiceSearchHandler(this, voiceInputLauncher)
    }

    private fun setupContent() {
        setContent {
            QuickSearchTheme {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    MainContent(
                        context = this@MainActivity,
                        userPreferences = userPreferences,
                        searchViewModel = searchViewModel,
                        onSearchBackPressed = { moveTaskToBack(true) },
                        navigationRequest = navigationRequest.value,
                        onNavigationRequestHandled = { navigationRequest.value = null },
                        onFinishActivity = {
                            if (userPreferences.isOverlayModeEnabled()) {
                                OverlayModeController.startOverlay(this@MainActivity)
                            }
                            finish()
                        },
                    )
                    if (showReviewPromptDialog.value) {
                        EnjoyingAppDialog(
                            onYes = {
                                showReviewPromptDialog.value = false
                                ReviewHelper.requestReviewIfEligible(
                                    this@MainActivity,
                                    userPreferences,
                                )
                            },
                            onNo = {
                                showReviewPromptDialog.value = false
                                showFeedbackDialog.value = true
                                userPreferences.recordReviewPromptTime()
                                userPreferences.recordAppOpenCountAtPrompt()
                                userPreferences.incrementReviewPromptedCount()
                            },
                            onDismiss = { showReviewPromptDialog.value = false },
                        )
                    }
                    if (showFeedbackDialog.value) {
                        SendFeedbackDialog(
                            onSend = { feedbackText ->
                                FeedbackUtils.launchFeedbackEmail(
                                    this@MainActivity,
                                    feedbackText,
                                )
                            },
                            onDismiss = { showFeedbackDialog.value = false },
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
        if (intent?.action == ACTION_VOICE_SEARCH_SHORTCUT) {
            voiceSearchHandler.handleMicAction(MicAction.DEFAULT_VOICE_SEARCH)
        }
        if (
            intent?.action == Intent.ACTION_ASSIST &&
                userPreferences.isAssistantLaunchVoiceModeEnabled()
        ) {
            voiceSearchHandler.handleMicAction(MicAction.DEFAULT_VOICE_SEARCH)
        }

        if (intent?.getBooleanExtra(OverlayModeController.EXTRA_OPEN_SETTINGS, false) == true) {
            val requestedDetail =
                intent.getStringExtra(OverlayModeController.EXTRA_OPEN_SETTINGS_DETAIL)
                    ?.let { name ->
                        runCatching { SettingsDetailType.valueOf(name) }.getOrNull()
                    }
            navigationRequest.value =
                NavigationRequest(
                    destination = RootDestination.Settings,
                    settingsDetailType = requestedDetail,
                )
            intent.removeExtra(OverlayModeController.EXTRA_OPEN_SETTINGS)
            intent.removeExtra(OverlayModeController.EXTRA_OPEN_SETTINGS_DETAIL)
        }
        val contactActionIntent = intent
        if (contactActionIntent?.getBooleanExtra(
                OverlayModeController.EXTRA_CONTACT_ACTION_PICKER,
                false,
            ) == true
        ) {
            val contactId =
                contactActionIntent.getLongExtra(
                    OverlayModeController.EXTRA_CONTACT_ACTION_PICKER_ID,
                    -1L,
                )
            val isPrimary =
                contactActionIntent.getBooleanExtra(
                    OverlayModeController.EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY,
                    true,
                )
            val serializedAction =
                contactActionIntent.getStringExtra(
                    OverlayModeController.EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION,
                )
            if (contactId != -1L) {
                searchViewModel.requestContactActionPicker(
                    contactId = contactId,
                    isPrimary = isPrimary,
                    serializedAction = serializedAction,
                )
            }
            contactActionIntent.removeExtra(OverlayModeController.EXTRA_CONTACT_ACTION_PICKER)
            contactActionIntent.removeExtra(OverlayModeController.EXTRA_CONTACT_ACTION_PICKER_ID)
            contactActionIntent.removeExtra(
                OverlayModeController.EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY,
            )
            contactActionIntent.removeExtra(
                OverlayModeController.EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION,
            )
        }

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
        // ACTION_ASSIST can optionally start voice typing based on user preference.
    }
}
