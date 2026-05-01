package com.tk.quicksearch.app.navigation

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.onboarding.FinalSetupScreen
import com.tk.quicksearch.onboarding.ImportSettingsScreen
import com.tk.quicksearch.onboarding.SearchEngineSetupScreen
import com.tk.quicksearch.onboarding.permissionScreen.PermissionsScreen
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.search.data.CustomCalendarEventRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.searchScreen.SearchRoute
import com.tk.quicksearch.settings.settingsDetailScreen.CreateCalendarEventDialog
import com.tk.quicksearch.settings.settingsDetailScreen.level
import com.tk.quicksearch.settings.navigation.SettingsDetailRoute
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.settingsDetailScreen.CustomToolNavigationMemory
import com.tk.quicksearch.settings.shared.SettingsRoute
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.FeedbackUtils

enum class RootDestination {
    Search,
    Settings,
}

private const val NAVIGATION_ANIMATION_DURATION_MS = 180

private enum class SwipeAnimationDirection {
    LEFT,
    RIGHT,
}

private fun directionalNavigationTransition(
    direction: SwipeAnimationDirection,
) = when (direction) {
    SwipeAnimationDirection.LEFT ->
        slideInHorizontally(
            animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
            initialOffsetX = { it },
        ) togetherWith
            slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
                targetOffsetX = { -it },
            )
    SwipeAnimationDirection.RIGHT ->
        slideInHorizontally(
            animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
            initialOffsetX = { -it },
        ) togetherWith
            slideOutHorizontally(
                animationSpec = androidx.compose.animation.core.tween(NAVIGATION_ANIMATION_DURATION_MS),
                targetOffsetX = { it },
            )
}

data class NavigationRequest(
    val destination: RootDestination,
    val settingsDetailType: SettingsDetailType? = null,
)

enum class AppScreen {
    Permissions,
    ImportSettings,
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
    var previousSettingsDetailType by remember { mutableStateOf<SettingsDetailType?>(null) }

    LaunchedEffect(navigationRequest) {
        navigationRequest?.let { request ->
            destination = request.destination
            // Only update settingsDetailType when the request explicitly specifies one.
            // When null (e.g. returning to home on app reopen), preserve the last visited
            // settings screen so tapping the settings icon resumes where the user left off.
            request.settingsDetailType?.let { settingsDetailType = it }
            onNavigationRequestHandled()
        }
    }

    LaunchedEffect(settingsDetailType) {
        val detail = settingsDetailType
        if (detail != null) {
            SettingsNavigationMemory.rememberSettingsDetail(detail)
        } else {
            SettingsNavigationMemory.clear()
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val screenOrder =
                listOf(
                    AppScreen.Permissions,
                    AppScreen.ImportSettings,
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
                QuickSearchTheme(
                    fontScaleMultiplier = userPreferences.getFontScaleMultiplier(),
                    useSystemFont = userPreferences.shouldUseSystemFont(),
                    appTheme = AppTheme.MONOCHROME,
                ) {
                PermissionsScreen(
                    currentStep = 1,
                    onPermissionsComplete = {
                        val hasCalendarPermission =
                            context.checkSelfPermission(Manifest.permission.READ_CALENDAR) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasCalendarPermission) {
                            searchViewModel.setSectionEnabled(SearchSection.CALENDAR, true)
                        }
                        currentScreen = AppScreen.ImportSettings
                        searchViewModel.handleOptionalPermissionChange()
                    },
                )
                }
            }

            AppScreen.ImportSettings -> {
                val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
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
                val hasAnyThirdPartyMessagingAppForImport =
                    uiState.isWhatsAppInstalled || uiState.isTelegramInstalled || uiState.isSignalInstalled
                val willShowFinalSetup =
                    hasFilesPermission || (hasContactsPermission && hasAnyThirdPartyMessagingAppForImport)
                val importTotalSteps = if (willShowFinalSetup) 4 else 3
                QuickSearchTheme(
                    fontScaleMultiplier = userPreferences.getFontScaleMultiplier(),
                    useSystemFont = userPreferences.shouldUseSystemFont(),
                    appTheme = AppTheme.MONOCHROME,
                ) {
                    ImportSettingsScreen(
                        currentStep = 2,
                        totalSteps = importTotalSteps,
                        onImportSuccess = {
                            searchViewModel.requestSearchBarWelcomeAnimationFromOnboarding()
                            searchViewModel.setShowStartSearchingOnOnboarding(true)
                            userPreferences.setFirstLaunchCompleted()
                            if (userPreferences.isOverlayModeEnabled()) {
                                onFinishActivity()
                            } else {
                                onFirstLaunchCompleted()
                                currentScreen = AppScreen.Main
                            }
                        },
                        onSkip = { currentScreen = AppScreen.SearchEngineSetup },
                    )
                }
            }

            AppScreen.SearchEngineSetup -> {
                val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
                BackHandler { currentScreen = AppScreen.ImportSettings }
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
                val searchEngineTotalSteps = if (skipFinalSetup) 3 else 4
                QuickSearchTheme(
                    fontScaleMultiplier = userPreferences.getFontScaleMultiplier(),
                    useSystemFont = userPreferences.shouldUseSystemFont(),
                    appTheme = AppTheme.MONOCHROME,
                ) {
                SearchEngineSetupScreen(
                    currentStep = 3,
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

                QuickSearchTheme(
                    fontScaleMultiplier = userPreferences.getFontScaleMultiplier(),
                    useSystemFont = userPreferences.shouldUseSystemFont(),
                    appTheme = AppTheme.MONOCHROME,
                ) {
                FinalSetupScreen(
                    currentStep = 4,
                    totalSteps = 4,
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
            }

            AppScreen.Main -> {
                NavigationContent(
                    destination = destination,
                    onDestinationChange = { destination = it },
                    settingsDetailType = settingsDetailType,
                    previousSettingsDetailType = previousSettingsDetailType,
                    onSettingsDetailTypeChange = {
                        previousSettingsDetailType = settingsDetailType
                        settingsDetailType = it
                    },
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
    previousSettingsDetailType: SettingsDetailType?,
    onSettingsDetailTypeChange: (SettingsDetailType?) -> Unit,
    viewModel: SearchViewModel,
    onSearchBackPressed: () -> Unit,
    onFinishActivity: () -> Unit,
) {
    val context = LocalContext.current
    var showCreateCalendarEventDialog by remember { mutableStateOf(false) }
    var rootAnimationDirectionOverride by remember { mutableStateOf<SwipeAnimationDirection?>(null) }
    var settingsDetailAnimationDirectionOverride by
        remember { mutableStateOf<SwipeAnimationDirection?>(null) }

    LaunchedEffect(destination) {
        rootAnimationDirectionOverride = null
    }

    LaunchedEffect(settingsDetailType) {
        settingsDetailAnimationDirectionOverride = null
    }

    AnimatedContent(
        targetState = destination,
        transitionSpec = {
            val isForward =
                initialState == RootDestination.Search &&
                    targetState == RootDestination.Settings
            val animationDirection =
                rootAnimationDirectionOverride
                    ?: if (isForward) {
                        SwipeAnimationDirection.LEFT
                    } else {
                        SwipeAnimationDirection.RIGHT
                    }
            directionalNavigationTransition(animationDirection)
        },
        label = "NavigationTransition",
    ) { targetDestination ->
        when (targetDestination) {
            RootDestination.Settings -> {
                SettingsNavigationContent(
                    settingsDetailType = settingsDetailType,
                    previousSettingsDetailType = previousSettingsDetailType,
                    settingsDetailAnimationDirectionOverride = settingsDetailAnimationDirectionOverride,
                    onSettingsDetailTypeChange = onSettingsDetailTypeChange,
                    onSettingsDetailAnimationDirectionConsumed = {
                        settingsDetailAnimationDirectionOverride = null
                    },
                    onRootAnimationDirectionChange = { rootAnimationDirectionOverride = it },
                    onDestinationChange = onDestinationChange,
                    viewModel = viewModel,
                    onFinishActivity = onFinishActivity,
                )
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
                val navigateToQuickNoteFromSwipeRight: (Long) -> Unit = { _ ->
                    rootAnimationDirectionOverride = SwipeAnimationDirection.RIGHT
                    settingsDetailAnimationDirectionOverride = SwipeAnimationDirection.RIGHT
                    onDestinationChange(RootDestination.Settings)
                    onSettingsDetailTypeChange(SettingsDetailType.NOTE_EDITOR)
                    keyboardController?.hide()
                }
                SearchRoute(
                    viewModel = viewModel,
                    onCloseAppRequest = onFinishActivity,
                    onSettingsClick = {
                        navigateToSettings(settingsDetailType ?: lastOpenedSettingsDetail)
                    },
                    onOpenAppSettingDestination = { destination ->
                        handleAppSettingsDestination(
                            destination = destination,
                            handlers =
                                AppSettingsDestinationHandlers(
                                    onOpenSettingsDetail = { detailType ->
                                        navigateToSettings(detailType)
                                    },
                                    onReloadApps = { viewModel.refreshApps(showToast = true) },
                                    onReloadContacts = { viewModel.refreshContacts(showToast = true) },
                                    onReloadFiles = { viewModel.refreshFiles(showToast = true) },
                                    onSendFeedback = {
                                        FeedbackUtils.launchFeedbackEmail(
                                            context = context,
                                            feedbackText = null,
                                        )
                                    },
                                    onRateQuickSearch = { launchRateQuickSearch(context) },
                                    onOpenDevelopmentPage = { launchDevelopmentPage(context) },
                                    onSetDefaultAssistant = {
                                        openDefaultAssistantSettings(context)
                                    },
                                    onAddHomeScreenWidget = {
                                        com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget(context)
                                    },
                                    onAddQuickSettingsTile = {
                                        com.tk.quicksearch.tile.requestAddQuickSearchTile(context)
                                    },
                                    onCreateCalendarEvent = {
                                        showCreateCalendarEventDialog = true
                                    },
                                ),
                        )
                    },
                    onOpenSearchHistorySettings = {
                        navigateToSettings(SettingsDetailType.SEARCH_RESULTS)
                    },
                    onOpenNotesDetail = {
                        val destination =
                            if (it != null) {
                                SettingsDetailType.NOTE_EDITOR
                            } else {
                                SettingsDetailType.NOTES
                            }
                        navigateToSettings(destination)
                    },
                    onOpenQuickNoteFromSwipe = navigateToQuickNoteFromSwipeRight,
                    onSearchEngineLongPress = {
                        navigateToSettings(SettingsDetailType.SEARCH_ENGINES)
                    },
                    onCustomizeSearchEnginesClick = {
                        navigateToSettings(SettingsDetailType.SEARCH_ENGINES)
                    },
                    onOpenAiSearchConfigure = {
                        navigateToSettings(SettingsDetailType.GEMINI_API_CONFIG)
                    },
                    onOpenToolsSettings = {
                        navigateToSettings(SettingsDetailType.TOOLS)
                    },
                    onOpenCustomToolSettings = { toolId ->
                        CustomToolNavigationMemory.setPendingToolId(toolId)
                        navigateToSettings(SettingsDetailType.CUSTOM_TOOL_EDITOR)
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

    if (showCreateCalendarEventDialog) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val customCalendarEventRepository = remember(context) { CustomCalendarEventRepository(context) }
        CreateCalendarEventDialog(
            onDismiss = { showCreateCalendarEventDialog = false },
            onConfirm = { title, dateTimeMillis, allDay ->
                showCreateCalendarEventDialog = false
                customCalendarEventRepository.createCustomEvent(title, dateTimeMillis, allDay)
                viewModel.onQueryChange(uiState.query)
            },
        )
    }
}

@Composable
private fun SettingsNavigationContent(
    settingsDetailType: SettingsDetailType?,
    previousSettingsDetailType: SettingsDetailType?,
    settingsDetailAnimationDirectionOverride: SwipeAnimationDirection?,
    onSettingsDetailTypeChange: (SettingsDetailType?) -> Unit,
    onSettingsDetailAnimationDirectionConsumed: () -> Unit,
    onRootAnimationDirectionChange: (SwipeAnimationDirection) -> Unit,
    onDestinationChange: (RootDestination) -> Unit,
    viewModel: SearchViewModel,
    onFinishActivity: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsScrollState = rememberScrollState()

    LaunchedEffect(settingsDetailType) {
        onSettingsDetailAnimationDirectionConsumed()
    }

    AnimatedContent(
        targetState = settingsDetailType,
        transitionSpec = {
            val initialLevel = initialState?.level() ?: 0
            val targetLevel = targetState?.level() ?: 0
            val isForward = targetLevel > initialLevel
            val animationDirection =
                settingsDetailAnimationDirectionOverride
                    ?: if (isForward) {
                        SwipeAnimationDirection.LEFT
                    } else {
                        SwipeAnimationDirection.RIGHT
                    }
            directionalNavigationTransition(animationDirection)
        },
        label = "SettingsDetailTransition",
    ) { currentDetailType ->
        if (currentDetailType != null) {
            SettingsDetailRoute(
                onBack = { onSettingsDetailTypeChange(null) },
                viewModel = viewModel,
                detailType = currentDetailType,
                sourceDetailType = previousSettingsDetailType,
                onNavigateToDetail = onSettingsDetailTypeChange,
                onNavigateToSearch = {
                    onRootAnimationDirectionChange(SwipeAnimationDirection.LEFT)
                    onSettingsDetailTypeChange(null)
                    if (uiState.overlayModeEnabled) {
                        onFinishActivity()
                    } else {
                        onDestinationChange(RootDestination.Search)
                    }
                },
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
