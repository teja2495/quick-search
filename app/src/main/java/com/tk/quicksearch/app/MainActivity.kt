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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.app.navigation.MainContent
import com.tk.quicksearch.app.navigation.NavigationRequest
import com.tk.quicksearch.app.navigation.RootDestination
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.FeedbackUtils
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.widgets.searchWidget.SearchWidget
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.searchWidget.VoiceSearchHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_VOICE_SEARCH_SHORTCUT = "com.tk.quicksearch.action.VOICE_SEARCH_SHORTCUT"
        const val ACTION_SEARCH_TARGET_SHORTCUT = "com.tk.quicksearch.action.SEARCH_TARGET_SHORTCUT"
        const val EXTRA_SHORTCUT_QUERY = "com.tk.quicksearch.extra.SHORTCUT_QUERY"
        const val EXTRA_SHORTCUT_TARGET_ENGINE = "com.tk.quicksearch.extra.SHORTCUT_TARGET_ENGINE"

        private const val EXTRA_CONTACT_ACTION_PICKER = "overlay_contact_action_picker"
        private const val EXTRA_CONTACT_ACTION_PICKER_ID = "overlay_contact_action_picker_id"
        private const val EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY = "overlay_contact_action_picker_primary"
        private const val EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION =
            "overlay_contact_action_picker_serialized_action"
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
    private var pendingSearchTargetShortcut: Pair<String, SearchTarget>? = null

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
        maybeExecutePendingSearchTargetShortcut()
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
        maybeExecutePendingSearchTargetShortcut()
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
                intent?.getBooleanExtra(SearchWidget.EXTRA_START_VOICE_SEARCH, false) ?: false
            val micAction =
                intent
                    ?.getStringExtra(SearchWidget.EXTRA_MIC_ACTION)
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
            val uiState = searchViewModel.uiState.collectAsStateWithLifecycle()
            QuickSearchTheme(fontScaleMultiplier = uiState.value.fontScaleMultiplier) {
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
        if (intent?.action == ACTION_SEARCH_TARGET_SHORTCUT) {
            val query = intent.getStringExtra(EXTRA_SHORTCUT_QUERY)?.trim().orEmpty()
            val engineName = intent.getStringExtra(EXTRA_SHORTCUT_TARGET_ENGINE)
            val engine = engineName?.let { runCatching { SearchEngine.valueOf(it) }.getOrNull() }
            if (query.isNotBlank() && engine != null) {
                pendingSearchTargetShortcut = query to SearchTarget.Engine(engine)
            }
        }
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
                EXTRA_CONTACT_ACTION_PICKER,
                false,
            ) == true
        ) {
            val contactId =
                contactActionIntent.getLongExtra(
                    EXTRA_CONTACT_ACTION_PICKER_ID,
                    -1L,
                )
            val isPrimary =
                contactActionIntent.getBooleanExtra(
                    EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY,
                    true,
                )
            val serializedAction =
                contactActionIntent.getStringExtra(
                    EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION,
                )
            if (contactId != -1L) {
                searchViewModel.requestContactActionPicker(
                    contactId = contactId,
                    isPrimary = isPrimary,
                    serializedAction = serializedAction,
                )
            }
            contactActionIntent.removeExtra(EXTRA_CONTACT_ACTION_PICKER)
            contactActionIntent.removeExtra(EXTRA_CONTACT_ACTION_PICKER_ID)
            contactActionIntent.removeExtra(
                EXTRA_CONTACT_ACTION_PICKER_IS_PRIMARY,
            )
            contactActionIntent.removeExtra(
                EXTRA_CONTACT_ACTION_PICKER_SERIALIZED_ACTION,
            )
        }

        // Handle voice search from widget
        val shouldStartVoiceSearch =
            intent?.getBooleanExtra(SearchWidget.EXTRA_START_VOICE_SEARCH, false) ?: false
        if (shouldStartVoiceSearch) {
            intent?.removeExtra(SearchWidget.EXTRA_START_VOICE_SEARCH)
            val micActionString = intent?.getStringExtra(SearchWidget.EXTRA_MIC_ACTION)
            val micAction =
                micActionString?.let { actionString ->
                    MicAction.entries.find { it.value == actionString }
                }
                    ?: MicAction.DEFAULT_VOICE_SEARCH
            voiceSearchHandler.handleMicAction(micAction)
        }
        // ACTION_ASSIST can optionally start voice typing based on user preference.
    }

    private fun maybeExecutePendingSearchTargetShortcut() {
        val pending = pendingSearchTargetShortcut ?: return
        lifecycleScope.launch {
            repeat(30) {
                val dispatched =
                    runCatching {
                        searchViewModel.openSearchTarget(pending.first, pending.second)
                    }.isSuccess
                if (dispatched) {
                    pendingSearchTargetShortcut = null
                    return@launch
                }
                delay(50)
            }
            pendingSearchTargetShortcut = null
        }
    }
}
