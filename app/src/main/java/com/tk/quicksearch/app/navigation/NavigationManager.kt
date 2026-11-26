package com.tk.quicksearch.app.navigation

import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.theme.AppMotion
import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.onboarding.FinalSetupScreen
import com.tk.quicksearch.onboarding.LocalWelcomeMorph
import com.tk.quicksearch.onboarding.welcomeBackdropZoom
import com.tk.quicksearch.onboarding.WelcomeMorphOverlay
import com.tk.quicksearch.onboarding.WelcomeMorphState
import com.tk.quicksearch.onboarding.LocalWelcomeExit
import com.tk.quicksearch.onboarding.ImportSettingsScreen
import com.tk.quicksearch.onboarding.SearchEngineSetupScreen
import com.tk.quicksearch.onboarding.permissionScreen.PermissionsScreen
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.searchScreen.components.LocalOpenKeyboardPillMemory
import com.tk.quicksearch.search.searchScreen.components.LocalSearchReturningFromInAppPage
import com.tk.quicksearch.search.searchScreen.components.OpenKeyboardPillMemory
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.searchScreen.LocalSharedPageWallpaper
import com.tk.quicksearch.search.searchScreen.rememberSharedPageWallpaper
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.LocalHomeAppearedContentKeys
import com.tk.quicksearch.search.searchScreen.SharedPageBackdrop
import com.tk.quicksearch.search.searchScreen.searchRoute.SearchRoute
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailRoute
import com.tk.quicksearch.settings.settingsDetailScreen.LocalSettingsPageData
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsPageData
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.toolsSettings.CustomToolNavigationMemory
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsRoute
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.shared.util.FeedbackUtils
import com.tk.quicksearch.widgetsPanel.WidgetsPanelScreen
import com.tk.quicksearch.widgetsPanel.rememberWidgetsPanelHost
import com.tk.quicksearch.shared.ui.components.AppMessageSnackbarHost
import com.tk.quicksearch.shared.ui.components.LocalSegmentedItemModifier
import com.tk.quicksearch.onboarding.welcomeExitItem

enum class RootDestination {
    WidgetsPanel,
    Search,
    Settings,
}

internal const val NAVIGATION_ANIMATION_DURATION_MS = AppMotion.DurationShort

data class NavigationRequest(
    val destination: RootDestination,
    val settingsDetailType: SettingsDetailType? = null,
    val settingsImportUri: String? = null,
    // Unique per request: an equal follow-up set before the handled reset recomposes would
    // otherwise leave the LaunchedEffect key unchanged and never be consumed (e.g. Home ignored).
    val requestId: Long = nextNavigationRequestId.incrementAndGet(),
)

private val nextNavigationRequestId = java.util.concurrent.atomic.AtomicLong()

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
    isFirstLaunch: Boolean,
    allowSystemWallpaperBackdrop: Boolean = false,
    onFirstLaunchCompleted: () -> Unit = {},
    onSearchBackPressed: () -> Unit = {},
    navigationRequest: NavigationRequest? = null,
    onNavigationRequestHandled: () -> Unit = {},
    returnToOverlay: Boolean = false,
    onFinishActivity: () -> Unit = {},
) {
    var currentScreen by remember {
        mutableStateOf(
            when {
                isFirstLaunch -> AppScreen.Permissions
                else -> AppScreen.Main
            },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val welcomeMorph = remember { WelcomeMorphState() }
    val searchInitializing = remember(searchViewModel) {
        searchViewModel.configState.map { it.isInitializing }.distinctUntilChanged()
    }
    var foundQuicksearchFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    val initialDestination = navigationRequest?.destination ?: RootDestination.Search
    val initialSettingsDetailType = navigationRequest?.settingsDetailType
    var destination by rememberSaveable { mutableStateOf(initialDestination) }
    var settingsDetailType by rememberSaveable { mutableStateOf(initialSettingsDetailType) }
    var pendingSettingsImportUri by rememberSaveable {
        mutableStateOf(navigationRequest?.settingsImportUri)
    }
    var previousSettingsDetailType by remember { mutableStateOf<SettingsDetailType?>(null) }

    LaunchedEffect(navigationRequest) {
        navigationRequest?.let { request ->
            when {
                request.destination == RootDestination.Search -> searchViewModel.setSearchLeftForInAppPage(false)
                destination == RootDestination.Search -> searchViewModel.setSearchLeftForInAppPage(true)
            }
            destination = request.destination
            // Only update settingsDetailType when the request explicitly specifies one.
            // When null (e.g. returning to home on app reopen), preserve the last visited
            // settings screen so tapping the settings icon resumes where the user left off.
            request.settingsDetailType?.let { settingsDetailType = it }
            request.settingsImportUri?.let { pendingSettingsImportUri = it }
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

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    animatedRootBackgroundColor(
                        allowSystemWallpaperBackdrop = allowSystemWallpaperBackdrop,
                        // NavigationContent covers Settings itself, in step with the page slide.
                        showsSystemWallpaper = currentScreen == AppScreen.Main,
                    ),
                ),
    ) {
    CompositionLocalProvider(LocalWelcomeMorph provides welcomeMorph) {
    // Built from the import step on, hidden under the setup pages, so finishing setup only reveals it.
    SearchSurfaceLayer(
        composed = currentScreen != AppScreen.Permissions,
        visible = currentScreen == AppScreen.Main,
        warmupStep = currentScreen,
        searchInitializing = searchInitializing,
        welcomeMorph = welcomeMorph,
    ) {
        NavigationContent(
            destination = destination,
            onDestinationChange = {
                if (destination == RootDestination.Search && it != RootDestination.Search) {
                    searchViewModel.setSearchLeftForInAppPage(true)
                }
                destination = it
            },
            settingsDetailType = settingsDetailType,
            previousSettingsDetailType = previousSettingsDetailType,
            onSettingsDetailTypeChange = {
                val currentDetailType = settingsDetailType
                val isReturningToPreviousPage =
                    currentDetailType != null &&
                        it == currentDetailType.resolveBackDestination(previousSettingsDetailType)
                previousSettingsDetailType =
                    when {
                        it == null -> null
                        isReturningToPreviousPage -> previousSettingsDetailType
                        else -> currentDetailType
                    }
                settingsDetailType = it
            },
            onSettingsDetailTypeChangeFromSearch = {
                previousSettingsDetailType = null
                settingsDetailType = it
            },
            pendingSettingsImportUri = pendingSettingsImportUri,
            onPendingSettingsImportUriConsumed = { pendingSettingsImportUri = null },
            viewModel = searchViewModel,
            onSearchBackPressed = onSearchBackPressed,
            returnToOverlay = returnToOverlay,
            onFinishActivity = onFinishActivity,
        )
    }
    AnimatedContent(
        targetState = currentScreen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { appScreenTransform(welcomeMorph) },
        label = "ScreenTransition",
    ) { targetScreen ->
        CompositionLocalProvider(
            LocalWelcomeExit provides welcomeMorph.takeIf { targetScreen != AppScreen.Main },
            LocalSegmentedItemModifier provides { it.welcomeExitItem() },
        ) {
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
                        searchViewModel.handleOptionalPermissionChange()
                        if (hasFilesPermission(context)) {
                            coroutineScope.launch {
                                val files = withContext(Dispatchers.IO) {
                                    findQuicksearchFilesOnDevice(context)
                                }
                                foundQuicksearchFiles = files
                                currentScreen = if (files.isNotEmpty()) {
                                    AppScreen.ImportSettings
                                } else {
                                    AppScreen.SearchEngineSetup
                                }
                            }
                        } else {
                            foundQuicksearchFiles = emptyList()
                            currentScreen = AppScreen.ImportSettings
                        }
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
                    uiState.installedContactProviders.any { it.showsMessagingSetup }
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
                            userPreferences.setFirstLaunchCompleted()
                            if (userPreferences.isOverlayModeEnabled()) {
                                onFinishActivity()
                            } else {
                                onFirstLaunchCompleted()
                                currentScreen = AppScreen.Main
                            }
                        },
                        onSettingsImported = searchViewModel::onSettingsImported,
                        onSkip = { currentScreen = AppScreen.SearchEngineSetup },
                        preFoundFiles = foundQuicksearchFiles,
                    )
                }
            }

            AppScreen.SearchEngineSetup -> {
                val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
                BackHandler {
                    currentScreen = if (!hasFilesPermission(context) || foundQuicksearchFiles.isNotEmpty()) {
                        AppScreen.ImportSettings
                    } else {
                        AppScreen.Permissions
                    }
                }
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
                    uiState.installedContactProviders.any { it.showsMessagingSetup }
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

                val showToast: (String) -> Unit = { message ->
                    AppMessages.show(context, message)
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
                    onShowToast = showToast,
                )
                }
            }

            AppScreen.Main -> Box(Modifier.fillMaxSize()) // The search screen lives in SearchSurfaceLayer.
        }
        }
    }
    }
    WelcomeMorphOverlay(
        state = welcomeMorph,
        onSearchScreen = currentScreen == AppScreen.Main,
        onFinished = searchViewModel::onSearchBarWelcomeAnimationCompleted,
    )
    }
}

@Composable
private fun NavigationContent(
    destination: RootDestination,
    onDestinationChange: (RootDestination) -> Unit,
    settingsDetailType: SettingsDetailType?,
    previousSettingsDetailType: SettingsDetailType?,
    onSettingsDetailTypeChange: (SettingsDetailType?) -> Unit,
    onSettingsDetailTypeChangeFromSearch: (SettingsDetailType?) -> Unit,
    pendingSettingsImportUri: String?,
    onPendingSettingsImportUriConsumed: () -> Unit,
    viewModel: SearchViewModel,
    onSearchBackPressed: () -> Unit,
    returnToOverlay: Boolean,
    onFinishActivity: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var rootAnimationDirectionOverride by remember { mutableStateOf<SwipeAnimationDirection?>(null) }
    var settingsDetailAnimationDirectionOverride by
        remember { mutableStateOf<SwipeAnimationDirection?>(null) }

    InAppReviewOnReturnEffect(
        destination = destination,
        activity = activity,
        userPreferences = viewModel.userPreferences,
        canRequestNow = {
            activity?.isFinishing == false &&
                uiState.query.isBlank() &&
                !uiState.showUpdateCard
        },
    )

    LaunchedEffect(settingsDetailType) {
        settingsDetailAnimationDirectionOverride = null
    }

    // Home and the widgets panel share one wallpaper drawn behind the page switch, so only their
    // content slides. Settings covers it with an opaque background. Everything is driven by the
    // page controller's progress in the draw phase, so the backdrop and pages never drift apart.
    val pageSwipeController =
        rememberPageSwipeController(destination, settingsDetailType, onSettingsDetailTypeChange)
    LaunchedEffect(destination) {
        pageSwipeController.navigateTo(
            destination,
            directionOverride =
                when (rootAnimationDirectionOverride) {
                    SwipeAnimationDirection.LEFT -> 1
                    SwipeAnimationDirection.RIGHT -> -1
                    null -> null
                },
        )
        rootAnimationDirectionOverride = null
    }
    // Outlives the Search page, which is disposed while Widgets or Settings is shown.
    val homeAppearedContentKeys = remember { mutableSetOf<String>() }
    val openKeyboardPillMemory = remember { OpenKeyboardPillMemory() }
    val settingsPageData = remember { SettingsPageData() }
    val widgetsPanelHost = rememberWidgetsPanelHost()
    val homeWidgetHost = com.tk.quicksearch.widgetsPanel.rememberHomeWidgetHost()
    Box(modifier = Modifier.fillMaxSize()) {
        // Keep the backdrop warm across Settings unless the system requests memory.
        val wallpaper = rememberSharedPageWallpaper(
            state = uiState,
            viewModel = viewModel,
            coveredBySettings = pageSwipeController.settled == RootDestination.Settings && pageSwipeController.target == null,
        )
        SharedPageBackdrop(
            state = uiState,
            wallpaperState = wallpaper,
            modifier =
                Modifier
                    .graphicsLayer {
                        alpha = if (pageSwipeController.visibilityOf(RootDestination.Settings) >= 1f) 0f else 1f
                    }.welcomeBackdropZoom(),
        )
        CompositionLocalProvider(
            LocalSharedPageWallpaper provides wallpaper,
            LocalSettingsPageData provides settingsPageData,
            LocalHomeAppearedContentKeys provides homeAppearedContentKeys,
            com.tk.quicksearch.widgetsPanel.LocalHomeWidgetHost provides homeWidgetHost,
            LocalPageSwipeController provides if (returnToOverlay) null else pageSwipeController,
        ) {
            PageSwipeHost(controller = pageSwipeController, destination = destination) { targetDestination ->
                when (targetDestination) {
                    RootDestination.WidgetsPanel -> {
                        val navigateToSearch: () -> Unit = {
                            rootAnimationDirectionOverride = SwipeAnimationDirection.LEFT
                            if (returnToOverlay) {
                                onFinishActivity()
                            } else {
                                onDestinationChange(RootDestination.Search)
                            }
                        }
                        WidgetsPanelScreen(
                            onNavigateToSearch = navigateToSearch,
                            uiState = uiState,
                            appWidgetHost = widgetsPanelHost,
                            folderActions = com.tk.quicksearch.widgetsPanel.rememberWidgetPanelFolderActions(viewModel),
                        )
                    }

                    RootDestination.Settings -> {
                        SettingsNavigationContent(
                            settingsDetailType = settingsDetailType,
                            previousSettingsDetailType = previousSettingsDetailType,
                            settingsDetailAnimationDirectionOverride = settingsDetailAnimationDirectionOverride,
                            onSettingsDetailTypeChange = onSettingsDetailTypeChange,
                            onSettingsDetailAnimationDirectionConsumed = { settingsDetailAnimationDirectionOverride = null },
                            onRootAnimationDirectionChange = { rootAnimationDirectionOverride = it },
                            onDestinationChange = onDestinationChange,
                            pendingSettingsImportUri = pendingSettingsImportUri,
                            onPendingSettingsImportUriConsumed = onPendingSettingsImportUriConsumed,
                            viewModel = viewModel,
                            returnToOverlay = returnToOverlay,
                            onFinishActivity = onFinishActivity,
                        )
                    }

                    RootDestination.Search -> {
                        val pageSettled = LocalRootPageSettled.current
                        BackHandler(enabled = pageSettled) { onSearchBackPressed() }
                        val keyboardController =
                            androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                        val lastOpenedSettingsDetail = SettingsNavigationMemory.getLastOpenedSettingsDetail()
                        // The search field is disposed while another page is on screen, so an open keyboard
                        // is recorded here and restored when search comes back.
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        // Derived so the keyboard animation does not recompose this destination per frame.
                        val imeInsets = WindowInsets.ime
                        val isImeVisible by remember(imeInsets, density) {
                            derivedStateOf { imeInsets.getBottom(density) > 0 }
                        }
                        val leaveSearchSurface: () -> Unit = {
                            viewModel.setSearchKeyboardRestoreRequest(isImeVisible)
                            keyboardController?.hide()
                        }
                        val navigateToSettings: (SettingsDetailType?) -> Unit = { detailType ->
                            onDestinationChange(RootDestination.Settings)
                            onSettingsDetailTypeChange(detailType)
                            leaveSearchSurface()
                        }
                        val navigateToWidgetsPanelFromSwipeRight: () -> Unit = {
                            rootAnimationDirectionOverride = SwipeAnimationDirection.RIGHT
                            onDestinationChange(RootDestination.WidgetsPanel)
                            leaveSearchSurface()
                        }
                        // Read once on entry: returning from Settings/Widgets skips launch auto-focus, so
                        // the keyboard only comes back if it was open when search was left.
                        val returningFromInAppPage = remember { viewModel.searchLeftForInAppPage }
                        LaunchedEffect(pageSettled) {
                            if (pageSettled) viewModel.setSearchLeftForInAppPage(false)
                        }
                        CompositionLocalProvider(
                            LocalSearchReturningFromInAppPage provides returningFromInAppPage,
                            LocalOpenKeyboardPillMemory provides openKeyboardPillMemory,
                        ) {
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
                                                onSetDefaultLauncher = {
                                                    openDefaultLauncherSettings(context)
                                                },
                                                onAddHomeScreenWidget = {
                                                    com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget(context)
                                                },
                                                onAddQuickSettingsTile = {
                                                    com.tk.quicksearch.tile.requestAddQuickSearchTile(context)
                                                },
                                            ),
                                    )
                                },
                                onOpenNotesDetail = {
                                    val destination =
                                        if (it != null) {
                                            SettingsDetailType.NOTE_EDITOR
                                        } else {
                                            SettingsDetailType.NOTES
                                        }
                                    if (destination == SettingsDetailType.NOTE_EDITOR) {
                                        onDestinationChange(RootDestination.Settings)
                                        onSettingsDetailTypeChangeFromSearch(destination)
                                        keyboardController?.hide()
                                    } else {
                                        navigateToSettings(destination)
                                    }
                                },
                                onOpenNotificationHistory = {
                                    navigateToSettings(SettingsDetailType.NOTIFICATION_HISTORY)
                                },
                                onOpenWidgetsPanelFromSwipe = navigateToWidgetsPanelFromSwipeRight,
                                onSearchEngineLongPress = {
                                    navigateToSettings(SettingsDetailType.SEARCH_ENGINES)
                                },
                                onCustomizeSearchEnginesClick = {
                                    navigateToSettings(SettingsDetailType.SEARCH_ENGINES)
                                },
                                onOpenAiSearchConfigure = {
                                    navigateToSettings(
                                        if (viewModel.uiState.value.hasApiKey) {
                                            SettingsDetailType.GEMINI_API_CONFIG
                                        } else {
                                            SettingsDetailType.API_KEY_SETUP
                                        },
                                    )
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
                            )
                        }
                    }
                }
            }
        }
        // In-app messages on Settings and the widgets panel; Search registers its own host.
        AppMessageSnackbarHost(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth())
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
    pendingSettingsImportUri: String?,
    onPendingSettingsImportUriConsumed: () -> Unit,
    viewModel: SearchViewModel,
    returnToOverlay: Boolean,
    onFinishActivity: () -> Unit,
) {
    val context = LocalContext.current
    val settingsScrollState = rememberScrollState()
    val detailScrollStates = remember { mutableStateMapOf<SettingsDetailType, androidx.compose.foundation.ScrollState>() }

    LaunchedEffect(settingsDetailType) {
        onSettingsDetailAnimationDirectionConsumed()
    }

    val backSeek =
        rememberSettingsBackSeek(settingsDetailType, previousSettingsDetailType, settingsDetailAnimationDirectionOverride)
    CompositionLocalProvider(LocalSettingsBackSeek provides backSeek) {
    rememberTransition(backSeek.transitionState, label = "SettingsDetailTransition").AnimatedContent(
        transitionSpec = {
            val isForward =
                when {
                    initialState == null && targetState != null -> true
                    initialState != null && targetState == null -> false
                    initialState != null && targetState != null -> {
                        val initialDetailType = requireNotNull(initialState)
                        val targetDetailType = requireNotNull(targetState)
                        val targetBackDestination =
                            targetDetailType.resolveBackDestination(initialDetailType)
                        val initialBackDestination =
                            initialDetailType.resolveBackDestination(targetDetailType)
                        when {
                            targetBackDestination == initialDetailType &&
                                initialBackDestination != targetDetailType -> true
                            initialBackDestination == targetDetailType &&
                                targetBackDestination != initialDetailType -> false
                            else -> targetDetailType.level() > initialDetailType.level()
                        }
                    }
                    else -> false
                }
            val animationDirection =
                backSeek.directionOverride.takeUnless { backSeek.isSeeking }
                    ?: if (isForward) {
                        SwipeAnimationDirection.LEFT
                    } else {
                        SwipeAnimationDirection.RIGHT
                    }
            directionalNavigationTransition(animationDirection)
        },
    ) { currentDetailType ->
        if (currentDetailType != null) {
            val detailScrollState =
                remember(currentDetailType, previousSettingsDetailType) {
                    if (currentDetailType == previousSettingsDetailType) {
                        detailScrollStates.getOrPut(currentDetailType) {
                            androidx.compose.foundation.ScrollState(initial = 0)
                        }
                    } else {
                        androidx.compose.foundation.ScrollState(initial = 0).also {
                            detailScrollStates[currentDetailType] = it
                        }
                    }
                }
            SettingsDetailRoute(
                onBack = { onSettingsDetailTypeChange(null) },
                viewModel = viewModel,
                detailType = currentDetailType,
                pageSettled = LocalRootPageSettled.current &&
                    !backSeek.isSeeking &&
                    backSeek.transitionState.currentState == currentDetailType &&
                    settingsDetailType == currentDetailType,
                sourceDetailType = previousSettingsDetailType,
                onNavigateToDetail = onSettingsDetailTypeChange,
                onNavigateToSearch = {
                    onRootAnimationDirectionChange(SwipeAnimationDirection.LEFT)
                    onSettingsDetailTypeChange(null)
                    if (returnToOverlay) {
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
                scrollState = detailScrollState,
            )
        } else {
            SettingsRoute(
                onBack = {
                    if (returnToOverlay) {
                        onFinishActivity()
                    } else {
                        onDestinationChange(RootDestination.Search)
                    }
                },
                viewModel = viewModel,
                onNavigateToDetail = onSettingsDetailTypeChange,
                pendingImportUri = pendingSettingsImportUri,
                onPendingImportUriConsumed = onPendingSettingsImportUriConsumed,
                scrollState = settingsScrollState,
            )
        }
    }
    }
}
