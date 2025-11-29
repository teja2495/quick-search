package com.tk.quicksearch

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import android.provider.Settings
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.permissions.PermissionsScreen
import com.tk.quicksearch.search.SearchRoute
import com.tk.quicksearch.search.SearchViewModel
import com.tk.quicksearch.settings.SettingsRoute
import com.tk.quicksearch.ui.theme.QuickSearchTheme

class MainActivity : ComponentActivity() {

    private val searchViewModel: SearchViewModel by viewModels()
    private lateinit var userPreferences: UserAppPreferences

    private val optionalPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            searchViewModel.handleOptionalPermissionChange()
        }

    private val manageAllFilesPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            searchViewModel.handleOptionalPermissionChange()
        }

    private var hasRequestedAllFilesAccess: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize userPreferences after super.onCreate() when context is ready
        userPreferences = UserAppPreferences(this)
        
        enableEdgeToEdge()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        setContent {
            QuickSearchTheme {
                val isFirstLaunch = userPreferences.isFirstLaunch()
                var showPermissions by rememberSaveable { mutableStateOf(isFirstLaunch) }
                var destination by rememberSaveable { mutableStateOf(RootDestination.Search) }

                // Always check isFirstLaunch to handle app restarts correctly
                if (showPermissions && isFirstLaunch) {
                    PermissionsScreen(
                        onPermissionsComplete = {
                            userPreferences.setFirstLaunchCompleted()
                            showPermissions = false
                            // Request permissions that might have been skipped
                            requestOptionalPermissionsIfNeeded()
                        }
                    )
                } else {
                    showPermissions = false // Ensure it's false if not first launch
                    when (destination) {
                        RootDestination.Search -> SearchRoute(
                            viewModel = searchViewModel,
                            onSettingsClick = { destination = RootDestination.Settings }
                        )

                        RootDestination.Settings -> SettingsRoute(
                            onBack = { destination = RootDestination.Search },
                            viewModel = searchViewModel
                        )
                    }
                }
            }
        }
        // Only request permissions automatically if not first launch
        if (!userPreferences.isFirstLaunch()) {
            requestOptionalPermissionsIfNeeded()
        }
    }

    private fun requestOptionalPermissionsIfNeeded() {
        val runtimePermissions = buildList {
            if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
                add(Manifest.permission.READ_CONTACTS)
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && !hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (runtimePermissions.isEmpty()) {
            searchViewModel.handleOptionalPermissionChange()
        } else {
            optionalPermissionsLauncher.launch(runtimePermissions.toTypedArray())
        }

        requestAllFilesAccessIfNeeded()
    }

    private fun requestAllFilesAccessIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            !Environment.isExternalStorageManager() &&
            !hasRequestedAllFilesAccess
        ) {
            hasRequestedAllFilesAccess = true
            val manageIntent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            runCatching {
                manageAllFilesPermissionLauncher.launch(manageIntent)
            }.onFailure {
                val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageAllFilesPermissionLauncher.launch(fallback)
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private enum class RootDestination {
    Search,
    Settings
}