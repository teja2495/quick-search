package com.tk.quicksearch.navigation

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.onboarding.permissionScreen.PermissionsScreen
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.settings.settingsScreen.SettingsDetailRoute
import com.tk.quicksearch.settings.settingsScreen.SettingsDetailType
import com.tk.quicksearch.settings.settingsScreen.SettingsRoute
import com.tk.quicksearch.onboarding.SearchEngineSetupScreen
import com.tk.quicksearch.onboarding.FinalSetupScreen

enum class RootDestination {
    Search,
    Settings
}

enum class AppScreen {
    Permissions,
    SearchEngineSetup,
    FinalSetup,
    Main
}

@Composable
fun MainContent(
    context: Context,
    userPreferences: UserAppPreferences,
    searchViewModel: SearchViewModel,
    onFirstLaunchCompleted: () -> Unit = {},
    onSearchBackPressed: () -> Unit = {}
) {
    val isFirstLaunch = userPreferences.isFirstLaunch()
    var currentScreen by remember {
        mutableStateOf(
            when {
                isFirstLaunch -> AppScreen.Permissions
                else -> AppScreen.Main
            }
        )
    }
    var destination by rememberSaveable { mutableStateOf(RootDestination.Search) }
    var settingsDetailType by rememberSaveable { mutableStateOf<SettingsDetailType?>(null) }
    var shouldShowFinalSetup by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            when {
                initialState == AppScreen.Permissions && targetState == AppScreen.SearchEngineSetup -> {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                }
                initialState == AppScreen.SearchEngineSetup && targetState == AppScreen.FinalSetup -> {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                }
                initialState == AppScreen.SearchEngineSetup && targetState == AppScreen.Permissions -> {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                }
                initialState == AppScreen.FinalSetup && targetState == AppScreen.SearchEngineSetup -> {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                }
                (initialState == AppScreen.SearchEngineSetup || initialState == AppScreen.FinalSetup) && targetState == AppScreen.Main -> {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
                }
                else -> {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            }
        },
        label = "AppNavigation"
    ) { targetScreen ->
        when (targetScreen) {
            AppScreen.Permissions -> {
                PermissionsScreen(
                    currentStep = 1,
                    totalSteps = if (shouldShowFinalSetup) 3 else 2,
                    onPermissionsComplete = {
                        val hasContactsPermission = context.checkSelfPermission(
                            android.Manifest.permission.READ_CONTACTS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        val hasFilesPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            android.os.Environment.isExternalStorageManager()
                        } else {
                            context.checkSelfPermission(
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }

                        shouldShowFinalSetup = hasContactsPermission || hasFilesPermission
                        currentScreen = AppScreen.SearchEngineSetup
                        searchViewModel.handleOptionalPermissionChange()
                    }
                )
            }
            AppScreen.SearchEngineSetup -> {
                BackHandler {
                    currentScreen = AppScreen.Permissions
                }
                SearchEngineSetupScreen(
                    currentStep = 2,
                    totalSteps = if (shouldShowFinalSetup) 3 else 2,
                    onContinue = {
                        if (shouldShowFinalSetup) {
                            currentScreen = AppScreen.FinalSetup
                        } else {
                            userPreferences.setFirstLaunchCompleted()
                            onFirstLaunchCompleted()
                            currentScreen = AppScreen.Main
                        }
                    },
                    viewModel = searchViewModel,
                    shouldShowFinalSetup = shouldShowFinalSetup
                )
            }
            AppScreen.FinalSetup -> {
                BackHandler {
                    currentScreen = AppScreen.SearchEngineSetup
                }
                val hasContactsPermission = context.checkSelfPermission(
                    android.Manifest.permission.READ_CONTACTS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val hasFilesPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    context.checkSelfPermission(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }

                val hasCallPermission = context.checkSelfPermission(
                    android.Manifest.permission.CALL_PHONE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                val showToast: (String) -> Unit = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                FinalSetupScreen(
                    currentStep = 3,
                    totalSteps = 3,
                    onContinue = {
                        userPreferences.setFirstLaunchCompleted()
                        onFirstLaunchCompleted()
                        currentScreen = AppScreen.Main
                    },
                    viewModel = searchViewModel,
                    hasContactsPermission = hasContactsPermission,
                    hasFilesPermission = hasFilesPermission,
                    hasCallPermission = hasCallPermission,
                    onShowToast = showToast
                )
            }
            AppScreen.Main -> {
                NavigationContent(
                    destination = destination,
                    onDestinationChange = { destination = it },
                    settingsDetailType = settingsDetailType,
                    onSettingsDetailTypeChange = { settingsDetailType = it },
                    viewModel = searchViewModel,
                    onSearchBackPressed = onSearchBackPressed
                )
            }
        }
    }
}

@Composable
private fun NavigationContent(
    destination: RootDestination,
    onDestinationChange: (RootDestination) -> Unit,
    settingsDetailType: SettingsDetailType?,
    onSettingsDetailTypeChange: (SettingsDetailType?) -> Unit,
    viewModel: SearchViewModel,
    onSearchBackPressed: () -> Unit
) {
    // Preserve scroll state for forward navigation but reset for back navigation
    val settingsScrollState = rememberScrollState()

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
                            slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
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
                    onSearchBackPressed()
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
                    },
                    onCustomizeSearchEnginesClick = {
                        keyboardController?.hide()
                        onDestinationChange(RootDestination.Settings)
                        onSettingsDetailTypeChange(SettingsDetailType.SEARCH_ENGINES)
                    },
                    onWelcomeAnimationCompleted = {
                        viewModel.onSearchBarWelcomeAnimationCompleted()
                    }
                )
            }
        }
    }
}