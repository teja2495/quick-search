package com.tk.quicksearch.app

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.os.Trace
import android.view.ViewTreeObserver
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.app.navigation.MainContent
import com.tk.quicksearch.app.navigation.NavigationRequest
import com.tk.quicksearch.app.navigation.RootDestination
import com.tk.quicksearch.app.navigation.SettingsNavigationMemory
import com.tk.quicksearch.app.startup.StartupCoordinator
import com.tk.quicksearch.app.startup.StartupMode
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.shared.util.FeedbackUtils
import com.tk.quicksearch.widgets.searchWidget.SearchWidget
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.searchWidget.VoiceSearchHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private data class PendingContactActionPickerRequest(
        val contactId: Long,
        val isPrimary: Boolean,
        val serializedAction: String?,
    )

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
        private const val TRACE_ON_CREATE_ENTRY = "QS.Startup.MainActivity.OnCreate"
        private const val TRACE_SET_CONTENT = "QS.Startup.MainActivity.SetContent"
        private const val TRACE_FIRST_FRAME_CALLBACK = "QS.Startup.MainActivity.FirstFrameCallback"
        private const val TRACE_SEARCH_SURFACE_FIRST_COMPOSE =
            "QS.Startup.MainActivity.SearchSurfaceFirstCompose"
        private const val TRACE_CORE_SURFACE_READY = "QS.Startup.CoreSurface.Ready"
        private const val TRACE_WALLPAPER_PREVIEW_READY = "QS.Startup.WallpaperPreview.Ready"
        private const val TRACE_SUGGESTIONS_READY = "QS.Startup.Suggestions.Ready"
    }

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private lateinit var startupCoordinator: StartupCoordinator
    private lateinit var voiceSearchHandler: VoiceSearchHandler
    private val voiceInputLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            voiceSearchHandler.processVoiceInputResult(result, searchViewModel::onQueryChange)
        }
    private val showReviewPromptDialog = mutableStateOf(false)
    private val showFeedbackDialog = mutableStateOf(false)
    private val navigationRequest = mutableStateOf<NavigationRequest?>(null)
    private var pendingSearchTargetShortcut: Pair<String, SearchTarget>? = null
    private var pendingContactActionPickerRequest: PendingContactActionPickerRequest? = null
    private var hasMainUiActivated = false
    private var hasSearchSurfaceComposeTraced = false
    private var hasFirstFrameTraced = false
    private var hasCoreSurfaceReadyTraced = false
    private var hasWallpaperPreviewReadyTraced = false
    private var hasSuggestionsReadyTraced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection(TRACE_ON_CREATE_ENTRY)
        try {
            // Must be called before super.onCreate for edge-to-edge to work correctly on all versions
            val statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            val navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            enableEdgeToEdge(statusBarStyle, navigationBarStyle)

            super.onCreate(savedInstanceState)
            window.setBackgroundDrawable(null)

            // Disable activity opening animation for instant appearance
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)

            initializePreferences()
            if (launchOverlayIfNeeded(intent)) {
                return
            }

            installFirstFrameTrace()

            Trace.beginSection(TRACE_SET_CONTENT)
            try {
                setupContent()
            } finally {
                Trace.endSection()
            }

            startupCoordinator =
                StartupCoordinator(
                    context = this,
                    activity = this,
                    lifecycleScope = lifecycleScope,
                    viewModel = searchViewModel,
                    userPreferences = userPreferences,
                    mode = StartupMode.MAIN,
                    onReviewPromptEligible = { showReviewPromptDialog.value = true },
                )
            startupCoordinator.scheduleAfterFirstFrame(window)

            initializeVoiceSearchHandler()
            handleIntent(intent)
        } finally {
            Trace.endSection()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (launchOverlayIfNeeded(intent)) {
            return
        }
        handleIntent(intent)
        if (hasMainUiActivated) {
            maybeExecutePendingSearchTargetShortcut()
            maybeExecutePendingContactActionPickerRequest()
        }
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
                initialQuery = extractTextFromIntent(intent),
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
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                ) {
                    LaunchedEffect(Unit) {
                        if (!hasSearchSurfaceComposeTraced) {
                            hasSearchSurfaceComposeTraced = true
                            Trace.beginSection(TRACE_SEARCH_SURFACE_FIRST_COMPOSE)
                            Trace.endSection()
                        }
                        if (!hasMainUiActivated) {
                            hasMainUiActivated = true
                            maybeExecutePendingSearchTargetShortcut()
                            maybeExecutePendingContactActionPickerRequest()
                        }
                    }

                    LaunchedEffect(
                        uiState.startupBackgroundPreviewPath,
                        uiState.wallpaperAvailable,
                        uiState.backgroundSource,
                    ) {
                        val hasImageBackground = uiState.backgroundSource != com.tk.quicksearch.search.core.BackgroundSource.THEME
                        val backgroundReady =
                            uiState.startupBackgroundPreviewPath != null || uiState.wallpaperAvailable
                        if (hasImageBackground && backgroundReady && !hasWallpaperPreviewReadyTraced) {
                            hasWallpaperPreviewReadyTraced = true
                            Trace.beginSection(TRACE_WALLPAPER_PREVIEW_READY)
                            Trace.endSection()
                        }
                    }

                    LaunchedEffect(
                        uiState.recentApps,
                        uiState.pinnedApps,
                        uiState.appSuggestionsEnabled,
                    ) {
                        val suggestionsReady =
                            !uiState.appSuggestionsEnabled ||
                                uiState.recentApps.isNotEmpty() ||
                                uiState.pinnedApps.isNotEmpty()
                        if (suggestionsReady && !hasSuggestionsReadyTraced) {
                            hasSuggestionsReadyTraced = true
                            Trace.beginSection(TRACE_SUGGESTIONS_READY)
                            Trace.endSection()
                        }
                    }

                    LaunchedEffect(
                        uiState.isStartupCoreSurfaceReady,
                        uiState.startupBackgroundPreviewPath,
                        uiState.wallpaperAvailable,
                        uiState.backgroundSource,
                        uiState.recentApps,
                        uiState.pinnedApps,
                    ) {
                        val backgroundReady =
                            uiState.backgroundSource == com.tk.quicksearch.search.core.BackgroundSource.THEME ||
                                uiState.startupBackgroundPreviewPath != null ||
                                uiState.wallpaperAvailable
                        val suggestionsReady =
                            !uiState.appSuggestionsEnabled ||
                                uiState.recentApps.isNotEmpty() ||
                                uiState.pinnedApps.isNotEmpty()
                        val ready =
                            uiState.isStartupCoreSurfaceReady || (backgroundReady && suggestionsReady)
                        if (ready && !hasCoreSurfaceReadyTraced) {
                            hasCoreSurfaceReadyTraced = true
                            searchViewModel.markStartupCoreSurfaceReady()
                            Trace.beginSection(TRACE_CORE_SURFACE_READY)
                            Trace.endSection()
                        }
                    }

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

    private fun installFirstFrameTrace() {
        val decorView = window.decorView
        val observer = decorView.viewTreeObserver
        if (!observer.isAlive) return
        observer.addOnDrawListener(
            object : ViewTreeObserver.OnDrawListener {
                override fun onDraw() {
                    if (hasFirstFrameTraced) return
                    hasFirstFrameTraced = true
                    Trace.beginSection(TRACE_FIRST_FRAME_CALLBACK)
                    Trace.endSection()
                    decorView.post {
                        if (decorView.viewTreeObserver.isAlive) {
                            decorView.viewTreeObserver.removeOnDrawListener(this)
                        }
                    }
                }
            },
        )
    }

    private fun extractTextFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        val queryFromCommonExtras =
            listOf(
                    SearchManager.QUERY,
                    Intent.EXTRA_TEXT,
                    "query",
                    "q",
                    "text",
                )
                .asSequence()
                .mapNotNull { key -> intent.extras?.get(key)?.toString()?.trim() }
                .firstOrNull { it.isNotBlank() }

        return when (intent.action) {
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim()
                    ?.takeIf { it.isNotBlank() }
            Intent.ACTION_SEND ->
                if (intent.type == "text/plain")
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotBlank() }
                else null
            Intent.ACTION_SEARCH,
            Intent.ACTION_WEB_SEARCH,
            Intent.ACTION_VIEW,
            Intent.ACTION_MAIN,
            -> queryFromCommonExtras
            else -> null
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (isExplicitLauncherLaunch(intent)) {
            navigationRequest.value = NavigationRequest(destination = RootDestination.Search)
        }

        val incomingText = extractTextFromIntent(intent)
        if (incomingText != null) {
            searchViewModel.onQueryChange(incomingText)
        }

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
                    ?: SettingsNavigationMemory.getLastOpenedSettingsDetail()
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
                pendingContactActionPickerRequest =
                    PendingContactActionPickerRequest(
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

    private fun isExplicitLauncherLaunch(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_MAIN) return false
        return intent.hasCategory(Intent.CATEGORY_LAUNCHER)
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

    private fun maybeExecutePendingContactActionPickerRequest() {
        val pending = pendingContactActionPickerRequest ?: return
        pendingContactActionPickerRequest = null
        searchViewModel.requestContactActionPicker(
            contactId = pending.contactId,
            isPrimary = pending.isPrimary,
            serializedAction = pending.serializedAction,
        )
    }
}
