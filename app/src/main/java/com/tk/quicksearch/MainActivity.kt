package com.tk.quicksearch

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.search.SearchRoute
import com.tk.quicksearch.search.SearchViewModel
import com.tk.quicksearch.settings.SettingsRoute
import com.tk.quicksearch.ui.theme.QuickSearchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        setContent {
            QuickSearchTheme {
                val searchViewModel: SearchViewModel = viewModel()
                var destination by rememberSaveable { mutableStateOf(RootDestination.Search) }

                when (destination) {
                    RootDestination.Search -> SearchRoute(
                        viewModel = searchViewModel,
                        onSettingsClick = { destination = RootDestination.Settings }
                    )

                    RootDestination.Settings -> SettingsRoute(
                        onBack = { destination = RootDestination.Search }
                    )
                }
            }
        }
    }
}

private enum class RootDestination {
    Search,
    Settings
}