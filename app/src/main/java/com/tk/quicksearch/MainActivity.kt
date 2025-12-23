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
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.permissions.PermissionsScreen
import com.tk.quicksearch.search.SearchRoute
import com.tk.quicksearch.search.SearchViewModel
import com.tk.quicksearch.settings.SettingsRoute
import com.tk.quicksearch.settings.SettingsDetailRoute
import com.tk.quicksearch.settings.SettingsDetailType
import com.tk.quicksearch.ui.theme.QuickSearchTheme
import com.tk.quicksearch.util.WallpaperUtils
import com.tk.quicksearch.widget.QuickSearchWidget

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
        // Preload wallpaper in background (already non-blocking)
        WallpaperUtils.preloadWallpaper(this)
        refreshPermissionStateIfNeeded()
        handleVoiceSearchIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleVoiceSearchIntent(intent)
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
            // Always use dark mode
            SetStatusBarAppearance(darkTheme = true)
            
            QuickSearchTheme {
                MainContent()
            }
        }
    }
    
    @Composable
    private fun SetStatusBarAppearance(darkTheme: Boolean) {
        val view = LocalView.current
        
        DisposableEffect(darkTheme) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ (API 30+)
                // APPEARANCE_LIGHT_STATUS_BARS means dark icons (for light backgrounds)
                // Without it means light icons (for dark backgrounds)
                val windowInsetsController = view.windowInsetsController
                windowInsetsController?.setSystemBarsAppearance(
                    if (darkTheme) 
                        0  // Dark theme: use light icons (no flag)
                    else 
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,  // Light theme: use dark icons
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ (API 23+)
                // SYSTEM_UI_FLAG_LIGHT_STATUS_BAR means dark icons (for light backgrounds)
                // Without it means light icons (for dark backgrounds)
                @Suppress("DEPRECATION")
                var flags = view.systemUiVisibility
                flags = if (darkTheme) {
                    // Dark theme: use light status bar icons (remove the flag)
                    flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                } else {
                    // Light theme: use dark status bar icons (add the flag)
                    flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                view.systemUiVisibility = flags
            }
            
            onDispose { }
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
        val settingsScrollState = rememberScrollState()
        when {
            destination == RootDestination.Settings && settingsDetailType != null -> {
                SettingsDetailRoute(
                    onBack = { onSettingsDetailTypeChange(null) },
                    viewModel = viewModel,
                    detailType = settingsDetailType
                )
            }
            destination == RootDestination.Settings -> {
                SettingsRoute(
                    onBack = { onDestinationChange(RootDestination.Search) },
                    viewModel = viewModel,
                    onNavigateToDetail = { detailType ->
                        onSettingsDetailTypeChange(detailType)
                    },
                    scrollState = settingsScrollState
                )
            }
            else -> {
                SearchRoute(
                    viewModel = viewModel,
                    onSettingsClick = { onDestinationChange(RootDestination.Settings) }
                )
            }
        }
    }

    private fun refreshPermissionStateIfNeeded() {
        if (!userPreferences.isFirstLaunch()) {
            searchViewModel.handleOptionalPermissionChange()
        }
    }

    private fun handleVoiceSearchIntent(intent: Intent?) {
        val shouldStartVoiceSearch = intent
            ?.getBooleanExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH, false)
            ?: false
        if (shouldStartVoiceSearch) {
            intent?.removeExtra(QuickSearchWidget.EXTRA_START_VOICE_SEARCH)
            startVoiceInput()
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
}

private enum class RootDestination {
    Search,
    Settings
}