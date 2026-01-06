package com.tk.quicksearch

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.permissions.PermissionsScreen
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.ui.SearchRoute
import com.tk.quicksearch.settings.main.SettingsRoute
import com.tk.quicksearch.settings.main.SettingsDetailRoute
import com.tk.quicksearch.settings.main.SettingsDetailType
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.widget.QuickSearchWidget
import com.tk.quicksearch.widget.MicAction

class MainActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences
    private val voiceInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val spokenText = data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            searchViewModel.onQueryChange(spokenText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        
        initializePreferences()
        setupWindow()
        // Initialize ViewModel early to start loading cached data immediately
        // This ensures cached apps are ready when UI renders
        searchViewModel
        setupContent()
        refreshPermissionStateIfNeeded()
        handleIntent(intent)

        // Defer wallpaper preload to after first frame to avoid blocking startup
        window.decorView.post {
            WallpaperUtils.preloadWallpaper(this)
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

    private fun setupWindow() {
        enableEdgeToEdge()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    private fun setupContent() {
        setContent {
            QuickSearchTheme {
                MainContent()
            }
        }
    }
    

    @Composable
    private fun MainContent() {
        val isFirstLaunch = userPreferences.isFirstLaunch()
        var showPermissions by rememberSaveable { mutableStateOf(isFirstLaunch) }
        var destination by rememberSaveable { mutableStateOf(RootDestination.Search) }
        var settingsDetailType by rememberSaveable { mutableStateOf<SettingsDetailType?>(null) }

        if (showPermissions && isFirstLaunch) {
            PermissionsScreen(
                onPermissionsComplete = {
                    userPreferences.setFirstLaunchCompleted()
                    showPermissions = false
                    searchViewModel.handleOptionalPermissionChange()
                }
            )
        } else {
            NavigationContent(
                destination = destination,
                onDestinationChange = { destination = it },
                settingsDetailType = settingsDetailType,
                onSettingsDetailTypeChange = { settingsDetailType = it },
                viewModel = searchViewModel
            )
        }
    }

    @Composable
    private fun NavigationContent(
        destination: RootDestination,
        onDestinationChange: (RootDestination) -> Unit,
        settingsDetailType: SettingsDetailType?,
        onSettingsDetailTypeChange: (SettingsDetailType?) -> Unit,
        viewModel: SearchViewModel
    ) {
        // Preserve scroll state for forward navigation but reset for back navigation
        val settingsScrollState = rememberScrollState()

        // Reset scroll position when navigating back to search
        LaunchedEffect(destination) {
            if (destination == RootDestination.Search) {
                settingsScrollState.scrollTo(0)
            }
        }

        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                if (targetState == RootDestination.Settings) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "RootNavigation"
        ) { targetDestination ->
            when (targetDestination) {
                RootDestination.Settings -> {
                    AnimatedContent(
                        targetState = settingsDetailType,
                        transitionSpec = {
                            if (targetState != null) {
                                // Navigate to Detail
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> -width } + fadeOut() 
                            } else {
                                // Navigate back to Main Settings
                                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                    slideOutHorizontally { width -> width } + fadeOut()
                            }
                        },
                        label = "SettingsNavigation"
                    ) { targetDetailType ->
                        if (targetDetailType != null) {
                            SettingsDetailRoute(
                                onBack = { onSettingsDetailTypeChange(null) },
                                viewModel = viewModel,
                                detailType = targetDetailType
                            )
                        } else {
                            SettingsRoute(
                                onBack = { onDestinationChange(RootDestination.Search) },
                                viewModel = viewModel,
                                onNavigateToDetail = onSettingsDetailTypeChange,
                                scrollState = settingsScrollState
                            )
                        }
                    }
                }
                RootDestination.Search -> {
                    BackHandler {
                        moveTaskToBack(true)
                    }
                    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                    SearchRoute(
                        viewModel = viewModel,
                        onSettingsClick = { 
                            keyboardController?.hide()
                            onDestinationChange(RootDestination.Settings)
                        },
                        onSearchEngineLongPress = {
                            keyboardController?.hide()
                            onDestinationChange(RootDestination.Settings)
                            onSettingsDetailTypeChange(SettingsDetailType.SEARCH_ENGINES)
                        }
                    )
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
        val shouldStartVoiceSearch = intent
            ?.getBooleanExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH, false)
            ?: false
        if (shouldStartVoiceSearch) {
            intent?.removeExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH)
            val micActionString = intent?.getStringExtra(QuickSearchWidget.EXTRA_MIC_ACTION)
            val micAction = micActionString?.let { actionString ->
                MicAction.entries.find { it.value == actionString }
            } ?: MicAction.DEFAULT_VOICE_SEARCH
            handleMicAction(micAction)
        }
        // ACTION_ASSIST is handled implicitly - search interface is always ready
    }

    private fun handleMicAction(micAction: MicAction) {
        when (micAction) {
            MicAction.DEFAULT_VOICE_SEARCH -> startVoiceInput()
            MicAction.DIGITAL_ASSISTANT -> startDigitalAssistant()
        }
    }

    private fun startVoiceInput() {
        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.widget_label_text))
        }
        try {
            voiceInputLauncher.launch(voiceIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                R.string.voice_input_not_available,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startDigitalAssistant() {
        val assistantIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (assistantIntent.resolveActivity(packageManager) != null) {
            startActivity(assistantIntent)
            finish()
        } else {
            Toast.makeText(
                this,
                R.string.voice_input_not_available,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}

private enum class RootDestination {
    Search,
    Settings
}