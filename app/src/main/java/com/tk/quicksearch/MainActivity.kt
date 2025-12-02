package com.tk.quicksearch

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.permissions.PermissionsScreen
import com.tk.quicksearch.search.SearchRoute
import com.tk.quicksearch.search.SearchViewModel
import com.tk.quicksearch.settings.SettingsRoute
import com.tk.quicksearch.ui.theme.QuickSearchTheme

class MainActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initializePreferences()
        setupWindow()
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
                viewModel = searchViewModel
            )
        }
    }

    @Composable
    private fun NavigationContent(
        destination: RootDestination,
        onDestinationChange: (RootDestination) -> Unit,
        viewModel: SearchViewModel
    ) {
        when (destination) {
            RootDestination.Search -> SearchRoute(
                viewModel = viewModel,
                onSettingsClick = { onDestinationChange(RootDestination.Settings) }
            )

            RootDestination.Settings -> SettingsRoute(
                onBack = { onDestinationChange(RootDestination.Search) },
                viewModel = viewModel
            )
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