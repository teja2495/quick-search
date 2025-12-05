package com.tk.quicksearch

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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

class MainActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initializePreferences()
        setupWindow()
        // Preload wallpaper early so it's ready when navigating to search screen
        WallpaperUtils.preloadWallpaper(this)
        setupContent()
        refreshPermissionStateIfNeeded()
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
                    }
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
}

private enum class RootDestination {
    Search,
    Settings
}