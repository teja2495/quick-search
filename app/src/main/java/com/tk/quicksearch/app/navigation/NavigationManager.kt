package com.tk.quicksearch.app.navigation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.onboarding.FinalSetupScreen
import com.tk.quicksearch.onboarding.SearchEngineSetupScreen
import com.tk.quicksearch.onboarding.permissionScreen.PermissionsScreen
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.settings.settingsDetailScreen.level
import com.tk.quicksearch.settings.navigation.SettingsDetailRoute
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.shared.SettingsRoute
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.FeedbackUtils

enum class RootDestination {
    Search,
    Settings,
}

private const val NAVIGATION_ANIMATION_DURATION_MS = 180
private const val QUICK_SEARCH_DEVELOPMENT_URL = "https://github.com/teja2495/quick-search"

data class NavigationRequest(
    val destination: RootDestination,
    val settingsDetailType: SettingsDetailType? = null,
)

enum class AppScreen {
    Permissions,
    SearchEngineSetup,
    FinalSetup,
    Main,
}

@Composable
fun MainContent(
    context: Context,
    userPreferences: UserAppPreferences,
    searchViewModel: SearchViewModel,
    onFirstLaunchCompleted: () -> Unit = {},
    onSearchBackPressed: () -> Unit = {},
    navigationRequest: NavigationRequest? = null,
    onNavigationRequestHandled: () -> Unit = {},
    onFinishActivity: () -> Unit = {},
) {
    val isFirstLaunch = userPreferences.isFirstLaunch()
    var currentScreen by remember {
        mutableStateOf(
            when {
                isFirstLaunch -> AppScreen.Permissions
                else -> AppScreen.Main
            },
        )
    }
    val initialDestination = navigationRequest?.destination ?: RootDestination.Search
    val initialSettingsDetailType = navigationRequest?.settingsDetailType
    var destination by rememberSaveable { mutableStateOf(initialDestination) }
    var settingsDetailType by rememberSaveable { mutableStateOf(initialSettingsDetailType) }
    val uiState by searchViewModel.uiState.collectAsState()

    LaunchedEffect(navigationRequest) {
        navigationRequest?.let { request ->
            destination = request.destination
            settingsDetailType = request.settingsDetailType
            onNavigationRequestHandled()
        }
    }

    LaunchedEffect(settingsDetailType) {
        settingsDetailType?.let(SettingsNavigationMemory::rememberSettingsDetail)
    }

    // Permission request handlers for settings detail screens
    val usagePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result -> searchViewModel.handleOptionalPermissionChange() }

    val contactPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted -> searchViewModel.handleOptionalPermissionChange() }

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted -> searchViewModel.handleOptionalPermissionChange() }

    val filePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result -> searchViewModel.handleOptionalPermissionChange() }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val screenOrder =
                listOf(
                    AppScreen.Permissions,
                    AppScreen.SearchEngineSetup,
                    AppScreen.FinalSetup,
                    AppScreen.Main,
                )
            val fromIndex = screenOrder.indexOf(initialState)
            val toIndex = screenOrder.indexOf(targetState)
            val isForward = toIndex > fromIndex

            if (isForward) {
                // Forward: slide in from right, slide out to left
                slideInHorizontally(
                    animationSpec =
                        androidx.compose.animation.core
                            .tween(NAVIGATION_ANIMATION_DURATION_MS),
                    initialOffsetX = { it },
                ) togetherWith
                    slideOutHorizontally(
                        animationSpec =
                            androidx.compose.animation.core
                                .tween(NAVIGATION_ANIMATION_DURATION_MS),
                        targetOffsetX = { -it },
                    )
            } else {
                // Backward: slide in from left, slide out to right
                slideInHorizontally(
                    animationSpec =
                        androidx.compose.animation.core
                            .tween(NAVIGATION_ANIMATION_DURATION_MS),
                    initialOffsetX = { -it },
                ) togetherWith
                    slideOutHorizontally(
                        animationSpec =
                            androidx.compose.animation.core
                                .tween(NAVIGATION_ANIMATION_DURATION_MS),
                        targetOffsetX = { it },
                    )
            }
        },
        label = "ScreenTransition",
    ) { targetScreen ->
        when (targetScreen) {
            AppScreen.Permissions -> {
                PermissionsScreen(
                    currentStep = 1,
                    onPermissionsComplete = {
                        val hasCalendarPermission =
                            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasCalendarPermission) {
                            searchViewModel.setSectionEnabled(SearchSection.CALENDAR, true)
                        }
                        currentScreen = AppScreen.SearchEngineSetup
                        searchViewModel.handleOptionalPermissionChange()
                    },
                )
            }

            AppScreen.SearchEngineSetup -> {
                BackHandler { currentScreen = AppScreen.Permissions }
                val hasContactsPermission =
                    context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasFilesPermission =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.os.Environment.isExternalStorageManager()
                    } else {
                        context.checkSelfPermission(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                val hasAnyThirdPartyMessagingApp =
                    uiState.isWhatsAppInstalled || uiState.isTelegramInstalled || uiState.isSignalInstalled
                val shouldShowFinalSetup =
                    hasFilesPermission || (hasContactsPermission && hasAnyThirdPartyMessagingApp)
                val skipFinalSetup = !shouldShowFinalSetup
                val searchEngineTotalSteps = if (skipFinalSetup) 2 else 3
                SearchEngineSetupScreen(
                    currentStep = 2,
                    totalSteps = searchEngineTotalSteps,
                    continueButtonTextRes = if (skipFinalSetup) R.string.setup_action_start else R.string.setup_action_next,
                    onContinue = {
                        if (skipFinalSetup) {
                            searchViewModel.requestSearchBarWelcomeAnimationFromOnboarding()
                            searchViewModel.setShowStartSearchingOnOnboarding(true)
                            userPreferences.setFirstLaunchCompleted()
                            if (userPreferences.isOverlayModeEnabled()) {
                                onFinishActivity()
                            } else {
                                onFirstLaunchCompleted()
                                currentScreen = AppScreen.Main
                            }
                        } else {
                            currentScreen = AppScreen.FinalSetup
                        }
                    },
                    viewModel = searchViewModel,
                )
            }

            AppScreen.FinalSetup -> {
                BackHandler { currentScreen = AppScreen.SearchEngineSetup }
                val hasContactsPermission =
                    context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED

                val hasFilesPermission =
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.os.Environment.isExternalStorageManager()
                    } else {
                        context.checkSelfPermission(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }

                val hasCallPermission =
                    context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED

                val showToast: (String) -> Unit = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                FinalSetupScreen(
                    currentStep = 3,
                    totalSteps = 3,
                    onContinue = {
                        searchViewModel.requestSearchBarWelcomeAnimationFromOnboarding()
                        userPreferences.setFirstLaunchCompleted()
                        if (userPreferences.isOverlayModeEnabled()) {
                            onFinishActivity()
                        } else {
                            onFirstLaunchCompleted()
                            currentScreen = AppScreen.Main
                        }
                    },
                    viewModel = searchViewModel,
                    hasContactsPermission = hasContactsPermission,
                    hasFilesPermission = hasFilesPermission,
                    hasCallPermission = hasCallPermission,
                    onShowToast = showToast,
                )
            }

            AppScreen.Main -> {
                NavigationContent(
                    destination = destination,
                    onDestinationChange = { destination = it },
                    settingsDetailType = settingsDetailType,
                    onSettingsDetailTypeChange = { settingsDetailType = it },
                    viewModel = searchViewModel,
                    onSearchBackPressed = onSearchBackPressed,
                    onFinishActivity = onFinishActivity,
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
    onSearchBackPressed: () -> Unit,
    onFinishActivity: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settingsScrollState = rememberScrollState()

    LaunchedEffect(destination) {
        if (destination == RootDestination.Search) {
            settingsScrollState.scrollTo(0)
        }
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val isForward =
                initialState == RootDestination.Search &&
                    targetState == RootDestination.Settings

            if (isForward) {
                // Forward (Search -> Settings): slide in from right, slide out to left
                slideInHorizontally(
                    animationSpec =
                        androidx.compose.animation.core
                            .tween(NAVIGATION_ANIMATION_DURATION_MS),
                    initialOffsetX = { it },
                ) togetherWith
                    slideOutHorizontally(
                        animationSpec =
                            androidx.compose.animation.core
                                .tween(NAVIGATION_ANIMATION_DURATION_MS),
                        targetOffsetX = { -it },
                    )
            } else {
                // Backward (Settings -> Search): slide in from left, slide out to right
                slideInHorizontally(
                    animationSpec =
                        androidx.compose.animation.core
                            .tween(NAVIGATION_ANIMATION_DURATION_MS),
                    initialOffsetX = { -it },
                ) togetherWith
                    slideOutHorizontally(
                        animationSpec =
                            androidx.compose.animation.core
                                .tween(NAVIGATION_ANIMATION_DURATION_MS),
                        targetOffsetX = { it },
                    )
            }
        },
        label = "NavigationTransition",
    ) { targetDestination ->
        when (targetDestination) {
            RootDestination.Settings -> {
                AnimatedContent(
                    targetState = settingsDetailType,
                    transitionSpec = {
                        val initialLevel = initialState?.level() ?: 0
                        val targetLevel = targetState?.level() ?: 0
                        val isForward = targetLevel > initialLevel

                        if (isForward) {
                            // Forward (Settings main -> Level 1, Level 1 -> Level 2)
                            slideInHorizontally(
                                animationSpec =
                                    androidx.compose.animation.core
                                        .tween(NAVIGATION_ANIMATION_DURATION_MS),
                                initialOffsetX = { it },
                            ) togetherWith
                                slideOutHorizontally(
                                    animationSpec =
                                        androidx.compose.animation.core.tween(
                                            NAVIGATION_ANIMATION_DURATION_MS,
                                        ),
                                    targetOffsetX = { -it },
                                )
                        } else {
                            // Backward (Level 2 -> Level 1, Detail -> Settings main)
                            slideInHorizontally(
                                animationSpec =
                                    androidx.compose.animation.core
                                        .tween(NAVIGATION_ANIMATION_DURATION_MS),
                                initialOffsetX = { -it },
                            ) togetherWith
                                slideOutHorizontally(
                                    animationSpec =
                                        androidx.compose.animation.core.tween(
                                            NAVIGATION_ANIMATION_DURATION_MS,
                                        ),
                                    targetOffsetX = { it },
                                )
                        }
                    },
                    label = "SettingsDetailTransition",
                ) { currentDetailType ->
                    if (currentDetailType != null) {
                        SettingsDetailRoute(
                            onBack = { onSettingsDetailTypeChange(null) },
                            viewModel = viewModel,
                            detailType = currentDetailType,
                            onNavigateToDetail = onSettingsDetailTypeChange,
                            onRequestUsagePermission = {
                                PermissionHelper.launchUsageAccessRequest(context)
                            },
                            onRequestContactPermission = viewModel::openContactPermissionSettings,
                            onRequestFilePermission = viewModel::openFilesPermissionSettings,
                            onRequestCallPermission = viewModel::openAppSettings,
                        )
                    } else {
                        SettingsRoute(
                            onBack = {
                                if (uiState.overlayModeEnabled) {
                                    onFinishActivity()
                                } else {
                                    onDestinationChange(RootDestination.Search)
                                }
                            },
                            viewModel = viewModel,
                            onNavigateToDetail = onSettingsDetailTypeChange,
                            scrollState = settingsScrollState,
                        )
                    }
                }
            }

            RootDestination.Search -> {
                BackHandler { onSearchBackPressed() }
                val keyboardController =
                    androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                val lastOpenedSettingsDetail = SettingsNavigationMemory.getLastOpenedSettingsDetail()
                val navigateToSettings: (SettingsDetailType?) -> Unit = { detailType ->
                    onDestinationChange(RootDestination.Settings)
                    onSettingsDetailTypeChange(detailType)
                    keyboardController?.hide()
                }
                SearchRoute(
                    viewModel = viewModel,
                    onCloseAppRequest = onFinishActivity,
                    onSettingsClick = {
                        navigateToSettings(settingsDetailType ?: lastOpenedSettingsDetail)
                    },
                    onOpenAppSettingDestination = { destination ->
                        when (destination) {
                            AppSettingsDestination.APPEARANCE ->
                                navigateToSettings(SettingsDetailType.APPEARANCE)
                            AppSettingsDestination.SEARCH_RESULTS ->
                                navigateToSettings(SettingsDetailType.SEARCH_RESULTS)
                            AppSettingsDestination.SEARCH_ENGINES ->
                                navigateToSettings(SettingsDetailType.SEARCH_ENGINES)
                            AppSettingsDestination.TOOLS ->
                                navigateToSettings(SettingsDetailType.TOOLS)
                            AppSettingsDestination.LAUNCH_OPTIONS ->
                                navigateToSettings(SettingsDetailType.LAUNCH_OPTIONS)
                            AppSettingsDestination.MORE_OPTIONS ->
                                navigateToSettings(SettingsDetailType.MORE_OPTIONS)
                            AppSettingsDestination.PERMISSIONS ->
                                navigateToSettings(SettingsDetailType.PERMISSIONS)
                            AppSettingsDestination.APP_MANAGEMENT ->
                                navigateToSettings(SettingsDetailType.APP_MANAGEMENT)
                            AppSettingsDestination.APP_SHORTCUTS ->
                                navigateToSettings(SettingsDetailType.APP_SHORTCUTS)
                            AppSettingsDestination.CALLS_TEXTS ->
                                navigateToSettings(SettingsDetailType.CALLS_TEXTS)
                            AppSettingsDestination.FILES ->
                                navigateToSettings(SettingsDetailType.FILES)
                            AppSettingsDestination.DEVICE_SETTINGS ->
                                navigateToSettings(SettingsDetailType.DEVICE_SETTINGS)
                            AppSettingsDestination.EXCLUDED_ITEMS ->
                                navigateToSettings(SettingsDetailType.EXCLUDED_ITEMS)
                            AppSettingsDestination.DIRECT_SEARCH_CONFIGURE ->
                                navigateToSettings(SettingsDetailType.DIRECT_SEARCH_CONFIGURE)
                            AppSettingsDestination.RELOAD_APPS ->
                                viewModel.refreshApps(showToast = true)
                            AppSettingsDestination.RELOAD_CONTACTS ->
                                viewModel.refreshContacts(showToast = true)
                            AppSettingsDestination.RELOAD_FILES ->
                                viewModel.refreshFiles(showToast = true)
                            AppSettingsDestination.SEND_FEEDBACK ->
                                FeedbackUtils.launchFeedbackEmail(context = context, feedbackText = null)
                            AppSettingsDestination.RATE_QUICK_SEARCH ->
                                launchRateQuickSearch(context)
                            AppSettingsDestination.DEVELOPMENT ->
                                launchDevelopmentPage(context)
                            AppSettingsDestination.FEATURES_LIST ->
                                navigateToSettings(SettingsDetailType.FEATURES_LIST)
                            AppSettingsDestination.OPEN_SOURCE_LICENSES ->
                                navigateToSettings(SettingsDetailType.OPEN_SOURCE_LICENSES)
                        }
                    },
                    onOpenSearchHistorySettings = {
                        navigateToSettings(SettingsDetailType.SEARCH_RESULTS)
                    },
                    onSearchEngineLongPress = {
                        navigateToSettings(SettingsDetailType.SEARCH_ENGINES)
                    },
                    onCustomizeSearchEnginesClick = {
                        navigateToSettings(SettingsDetailType.SEARCH_ENGINES)
                    },
                    onOpenDirectSearchConfigure = {
                        navigateToSettings(SettingsDetailType.DIRECT_SEARCH_CONFIGURE)
                    },
                    onOpenReleaseNotesFeatures = {
                        navigateToSettings(SettingsDetailType.FEATURES_LIST)
                    },
                    onWelcomeAnimationCompleted = {
                        viewModel.onSearchBarWelcomeAnimationCompleted()
                    },
                    onWallpaperLoaded = { viewModel.setWallpaperAvailable(true) },
                )
            }
        }
    }
}

private fun launchRateQuickSearch(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            },
        )
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ),
            )
        }
    }
}

private fun launchDevelopmentPage(context: Context) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(QUICK_SEARCH_DEVELOPMENT_URL)))
    }
}
