package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.app.navigation.openDefaultAssistantSettings
import com.tk.quicksearch.app.navigation.openDefaultLauncherSettings
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.appShortcutsSettings.*
import com.tk.quicksearch.settings.shared.settingsRoute.AppShortcutSourceFlowDialogs
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsRouteHandlers
import com.tk.quicksearch.settings.shared.settingsRoute.buildSettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.rememberAppShortcutSourceFlow
import com.tk.quicksearch.settings.shared.settingsRoute.rememberSectionToggleHandler
import com.tk.quicksearch.settings.shared.settingsRoute.rememberWallpaperPermissionController
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.shared.util.WallpaperUtils
import com.tk.quicksearch.shared.util.isDefaultDigitalAssistant
import com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsDetailRoute(
        modifier: Modifier = Modifier,
        onBack: () -> Unit,
        viewModel: SearchViewModel,
        detailType: SettingsDetailType,
        pageSettled: Boolean = true,
        sourceDetailType: SettingsDetailType? = null,
        onNavigateToDetail: (SettingsDetailType) -> Unit = {},
        onNavigateToSearch: () -> Unit = {},
        onRequestUsagePermission: () -> Unit = {},
        onRequestContactPermission: () -> Unit = {},
        onRequestFilePermission: () -> Unit = {},
        onRequestCalendarPermission: () -> Unit = {},
        onRequestCallPermission: () -> Unit = {},
        scrollState: androidx.compose.foundation.ScrollState =
                androidx.compose.foundation.rememberScrollState(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val state = uiState.toSettingsScreenState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallpaperPermissionController =
            rememberWallpaperPermissionController(
                    onSetWallpaperAvailable = viewModel::setWallpaperAvailable,
                    onSetBackgroundSource = viewModel::setBackgroundSource,
                    onOptionalPermissionChanged = viewModel::handleOptionalPermissionChange,
            )

    val overlayCustomImagePickerLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                scope.launch {
                    val persistedUri = withContext(Dispatchers.IO) {
                        WallpaperUtils.copyImageToInternalStorage(context, uri)
                    }
                    if (persistedUri != null) {
                        viewModel.applySettingsCommand(SettingsCommand.CustomImageUriSetting(persistedUri))
                        viewModel.applySettingsCommand(
                            SettingsCommand.BackgroundSourceSetting(BackgroundSource.CUSTOM_IMAGE),
                        )
                    } else {
                        AppMessages.show(context, context.getString(R.string.settings_overlay_source_custom_save_failed))
                    }
                }
            }

    val onSelectWallpaperSource: () -> Unit = wallpaperPermissionController.onRequestPermission

    val lifecycleOwner = LocalLifecycleOwner.current
    val userPreferences = remember { UserAppPreferences(context) }
    var isDefaultAssistant by remember { mutableStateOf(context.isDefaultDigitalAssistant()) }
    var assistantLaunchVoiceModeEnabled by
            remember {
                mutableStateOf(userPreferences.isAssistantLaunchVoiceModeEnabled())
            }
    var aiSearchSetupExpanded by
            remember(detailType) {
                mutableStateOf(
                        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                            // Always start expanded in search engine settings screen
                            true
                        } else {
                            true
                        },
                )
            }
    var disabledSearchEnginesExpanded by
            remember(detailType) {
                mutableStateOf(
                        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                            userPreferences.isDisabledSearchEnginesExpanded()
                        } else {
                            true
                        },
                )
            }

    var hasSettled by remember { mutableStateOf(pageSettled) }
    LaunchedEffect(pageSettled) {
        if (pageSettled) hasSettled = true
    }
    DisposableEffect(lifecycleOwner, detailType, hasSettled) {
        if (!hasSettled) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionSnapshotForVisiblePage()
                wallpaperPermissionController.onRefreshPermissionState()
            }
            if (
                    detailType != SettingsDetailType.LAUNCH_OPTIONS
            ) {
                return@LifecycleEventObserver
            }
            when (event) {
                Lifecycle.Event.ON_START -> {
                    isDefaultAssistant = context.isDefaultDigitalAssistant()
                    assistantLaunchVoiceModeEnabled =
                            userPreferences.isAssistantLaunchVoiceModeEnabled()
                }
                Lifecycle.Event.ON_RESUME -> {
                    isDefaultAssistant = context.isDefaultDigitalAssistant()
                    assistantLaunchVoiceModeEnabled =
                            userPreferences.isAssistantLaunchVoiceModeEnabled()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onToggleSection = rememberSectionToggleHandler(viewModel, uiState.disabledSections)

    val onRequestAddHomeScreenWidget = { requestAddQuickSearchWidget(context) }
    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }
    var filteredAppShortcutSources by remember { mutableStateOf<List<AppShortcutSource>>(emptyList()) }
    var hasLoadedAppShortcutSources by remember { mutableStateOf(false) }
    var appShortcutFocusShortcut by remember { mutableStateOf<StaticShortcut?>(null) }
    var appShortcutFocusPackageName by remember { mutableStateOf<String?>(null) }
    val onShortcutAdded: (StaticShortcut) -> Unit = { addedShortcut ->
        appShortcutFocusShortcut = addedShortcut
        appShortcutFocusPackageName = addedShortcut.packageName
        AppMessages.show(context, context.getString(
                R.string.settings_app_shortcuts_add_success_with_app_name,
                addedShortcut.appLabel,
            ))
    }
    val onShortcutAddFailed: () -> Unit = {
        AppMessages.show(context, context.getString(R.string.settings_app_shortcuts_add_failed))
    }
    val appShortcutSourceFlow =
            rememberAppShortcutSourceFlow(
                    context = context,
                    onAddShortcutFromPickerResult = { resultData, sourcePackageName ->
                        viewModel.addCustomAppShortcutFromPickerResult(
                                resultData = resultData,
                                sourcePackageName = sourcePackageName,
                                showDefaultToast = false,
                                onShortcutAdded = onShortcutAdded,
                                onAddFailed = onShortcutAddFailed,
                        )
                    },
                    onAddCustomAppActivityShortcut = { source, activity ->
                        viewModel.addCustomAppActivityShortcut(
                                packageName = source.packageName,
                                activityClassName = activity.className,
                                activityLabel = activity.label,
                                showDefaultToast = false,
                                onShortcutAdded = onShortcutAdded,
                                onAddFailed = onShortcutAddFailed,
                        )
                    },
                    onSourceLaunchUnsupported = {
                        AppMessages.show(context, context.getString(
                                                R.string.settings_app_shortcuts_create_not_supported,
                                        ))
                    },
            )

    // Preview pages can render their shell without scanning packages.
    val pageData = LocalSettingsPageData.current ?: remember { SettingsPageData() }
    var hasRefreshedAppShortcuts by remember(detailType) { mutableStateOf(false) }
    LaunchedEffect(detailType, pageSettled) {
        if (detailType == SettingsDetailType.APP_SHORTCUTS && pageSettled && !hasRefreshedAppShortcuts) {
            viewModel.refreshAppShortcutsAndAwait()
            hasRefreshedAppShortcuts = true
        }
    }

    var appShortcutSources by remember(detailType, state.allApps) { mutableStateOf<List<AppShortcutSource>?>(null) }
    LaunchedEffect(detailType, pageSettled, pageData, state.allApps) {
        if (detailType == SettingsDetailType.APP_SHORTCUTS && pageSettled && appShortcutSources == null) {
            appShortcutSources = pageData.appShortcutSources(context.applicationContext, state.allApps)
        }
    }

    LaunchedEffect(
            detailType,
            hasRefreshedAppShortcuts,
            appShortcutSources,
            state.allAppShortcuts,
    ) {
        if (detailType != SettingsDetailType.APP_SHORTCUTS) {
            filteredAppShortcutSources = emptyList()
            hasLoadedAppShortcutSources = false
            return@LaunchedEffect
        }
        val sources = appShortcutSources
        if (!hasRefreshedAppShortcuts || sources == null) return@LaunchedEffect

        filteredAppShortcutSources =
                withContext(Dispatchers.Default) {
                    filterAppShortcutSources(
                            sources = sources,
                            existingShortcuts = state.allAppShortcuts,
                    )
                }
        hasLoadedAppShortcutSources = true
    }


    val onBackAction: () -> Unit =
            if (detailType.isNestedDetail()) {
                {
                    if (detailType == SettingsDetailType.NOTE_EDITOR && sourceDetailType == null) {
                        onNavigateToSearch()
                    } else {
                        val destination = detailType.resolveBackDestination(sourceDetailType)
                        if (destination == null) {
                            onBack()
                        } else {
                            onNavigateToDetail(destination)
                        }
                    }
                }
            } else {
                onBack
            }

    val callbacks =
            buildSettingsScreenCallbacks(
                    viewModel = viewModel,
                    handlers =
                            SettingsRouteHandlers(
                                    onBack = onBackAction,
                                    onToggleOverlayMode = { viewModel.setOverlayModeEnabled(it) },
                                    onPickCustomImage = {
                                        overlayCustomImagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onRemoveCustomImage = {
                                        viewModel.applySettingsCommand(SettingsCommand.CustomImageUriSetting(null))
                                        scope.launch(Dispatchers.IO) {
                                            WallpaperUtils.deleteCustomImages(context)
                                        }
                                    },
                                    onToggleDirectDial = viewModel::setDirectDialEnabled,
                                    onToggleSection = onToggleSection,
                                    onOpenAiSearchConfigure = {
                                        onNavigateToDetail(
                                            if (state.hasApiKey) {
                                                SettingsDetailType.GEMINI_API_CONFIG
                                            } else {
                                                SettingsDetailType.API_KEY_SETUP
                                            },
                                        )
                                    },
                                    onOpenAddAppShortcutDialog = appShortcutSourceFlow.openSourcePicker,
                                    onAddAppShortcutFromSource = appShortcutSourceFlow.selectSource,
                                    onAddAppDeepLinkShortcut = { packageName, shortcutName, deepLink, iconBase64 ->
                                        viewModel.addCustomAppDeepLinkShortcut(
                                                packageName = packageName,
                                                shortcutName = shortcutName,
                                                deepLink = deepLink,
                                                iconBase64 = iconBase64,
                                                showDefaultToast = false,
                                                onShortcutAdded = onShortcutAdded,
                                                onAddFailed = onShortcutAddFailed,
                                        )
                                    },
                                    onAddSearchTargetQueryShortcut = { target, shortcutName, shortcutQuery, mode ->
                                        viewModel.addSearchTargetQueryShortcut(
                                                target = target,
                                                shortcutName = shortcutName,
                                                shortcutQuery = shortcutQuery,
                                                mode = mode,
                                                showDefaultToast = false,
                                                onShortcutAdded = onShortcutAdded,
                                                onAddFailed = onShortcutAddFailed,
                                        )
                                    },
                                    onAddHomeScreenWidget = onRequestAddHomeScreenWidget,
                                    onAddQuickSettingsTile = onRequestAddQuickSettingsTile,
                                    onSetDefaultAssistant = { openDefaultAssistantSettings(context) },
                                    onSetDefaultLauncher = { openDefaultLauncherSettings(context) },
                                    onToggleAssistantLaunchVoiceMode = { enabled ->
                                        userPreferences.setAssistantLaunchVoiceModeEnabled(enabled)
                                        assistantLaunchVoiceModeEnabled = enabled
                                    },
                                    onRefreshApps = viewModel::refreshApps,
                                    onRefreshContacts = viewModel::refreshContacts,
                                    onRefreshFiles = viewModel::refreshFiles,
                                    onRequestUsagePermission = onRequestUsagePermission,
                                    onRequestContactPermission = onRequestContactPermission,
                                    onRequestFilePermission = onRequestFilePermission,
                                    onRequestCalendarPermission = onRequestCalendarPermission,
                                    onRequestCallPermission = onRequestCallPermission,
                                    onRequestWallpaperPermission = onSelectWallpaperSource,
                            ),
            )

    val onToggleAiSearchSetupExpanded = {
        val newExpanded = !aiSearchSetupExpanded
        aiSearchSetupExpanded = newExpanded
        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
            userPreferences.setAiSearchSetupExpanded(newExpanded)
        }
    }
    val onToggleDisabledSearchEnginesExpanded = {
        val newExpanded = !disabledSearchEnginesExpanded
        disabledSearchEnginesExpanded = newExpanded
        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
            userPreferences.setDisabledSearchEnginesExpanded(newExpanded)
        }
    }
    val resolvedState =
            if (detailType == SettingsDetailType.APPEARANCE) {
                state.copy(hasWallpaperPermission = wallpaperPermissionController.hasWallpaperPermission)
            } else {
                state
            }

    if (detailType.isNestedDetail()) {
        val shouldShowAppShortcutsContent =
                detailType != SettingsDetailType.APP_SHORTCUTS ||
                        hasLoadedAppShortcutSources

        SettingsNestedDetailScreen(
                modifier = modifier,
                state = resolvedState,
                callbacks = callbacks,
                detailType = detailType,
                hasUsagePermission = uiState.hasUsagePermission,
                appShortcutFocusShortcut = appShortcutFocusShortcut,
                appShortcutFocusPackageName = appShortcutFocusPackageName,
                isAppShortcutsLoading = !shouldShowAppShortcutsContent,
                appShortcutSources =
                        if (shouldShowAppShortcutsContent) {
                            filteredAppShortcutSources
                        } else {
                            emptyList()
                        },
                searchTargets =
                        if (shouldShowAppShortcutsContent) {
                            state.searchEngineOrder
                        } else {
                            emptyList()
                        },
                onAppShortcutFocusHandled = {
                    appShortcutFocusShortcut = null
                    appShortcutFocusPackageName = null
                },
                onNavigateToDetail = onNavigateToDetail,
                onNavigateToSearch = onNavigateToSearch,
                onNicknameRemoved = { userPreferences.reloadNicknameCaches() },
                scrollState = scrollState,
        )
    } else {
        SettingsDetailLevel1Screen(
                modifier = modifier,
                uiState = uiState,
                state = resolvedState,
                callbacks = callbacks,
                detailType = detailType,
                hasUsagePermission = uiState.hasUsagePermission,
                isDefaultAssistant = isDefaultAssistant,
                assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
                aiSearchSetupExpanded = aiSearchSetupExpanded,
                onToggleAiSearchSetupExpanded = onToggleAiSearchSetupExpanded,
                disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
                onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
                onNavigateToDetail = onNavigateToDetail,
                scrollState = scrollState,
        )
    }

    AppShortcutSourceFlowDialogs(
            flowState = appShortcutSourceFlow,
            sources = filteredAppShortcutSources,
    )
}
