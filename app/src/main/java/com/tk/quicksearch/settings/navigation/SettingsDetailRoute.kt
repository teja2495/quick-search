package com.tk.quicksearch.settings.navigation

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.settings.AppShortcutsSettings.*
import com.tk.quicksearch.settings.settingsDetailScreen.*
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.shared.util.isDefaultDigitalAssistant
import com.tk.quicksearch.widgets.utils.requestAddQuickSearchWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsDetailRoute(
        modifier: Modifier = Modifier,
        onBack: () -> Unit,
        viewModel: SearchViewModel,
        detailType: SettingsDetailType,
        sourceDetailType: SettingsDetailType? = null,
        onNavigateToDetail: (SettingsDetailType) -> Unit = {},
        onRequestUsagePermission: () -> Unit = {},
        onRequestContactPermission: () -> Unit = {},
        onRequestFilePermission: () -> Unit = {},
        onRequestCalendarPermission: () -> Unit = {},
        onRequestCallPermission: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val state = uiState.toSettingsScreenState()

    val context = LocalContext.current
    val wallpaperPermissionController =
            rememberWallpaperPermissionController(
                    onSetWallpaperAvailable = viewModel::setWallpaperAvailable,
                    onSetBackgroundSource = viewModel::setBackgroundSource,
                    onOptionalPermissionChanged = viewModel::handleOptionalPermissionChange,
            )

    val overlayCustomImagePickerLauncher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument(),
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                        .onFailure {
                            // Some providers do not support persistable permissions.
                        }
                viewModel.applySettingsCommand(SettingsCommand.CustomImageUriSetting(uri.toString()))
                viewModel.applySettingsCommand(
                    SettingsCommand.BackgroundSourceSetting(BackgroundSource.CUSTOM_IMAGE),
                )
            }

    val onSelectWallpaperSource: () -> Unit = wallpaperPermissionController.onRequestPermission

    val lifecycleOwner = LocalLifecycleOwner.current
    val userPreferences = remember { UserAppPreferences(context) }
    var isDefaultAssistant by remember { mutableStateOf(context.isDefaultDigitalAssistant()) }
    var assistantLaunchVoiceModeEnabled by
            remember {
                mutableStateOf(userPreferences.isAssistantLaunchVoiceModeEnabled())
            }
    var directSearchSetupExpanded by
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

    DisposableEffect(lifecycleOwner, detailType) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.handleOptionalPermissionChange()
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
        Toast.makeText(
            context,
            context.getString(
                R.string.settings_app_shortcuts_add_success_with_app_name,
                addedShortcut.appLabel,
            ),
            Toast.LENGTH_SHORT,
        ).show()
    }
    val onShortcutAddFailed: () -> Unit = {
        Toast.makeText(
            context,
            context.getString(R.string.settings_app_shortcuts_add_failed),
            Toast.LENGTH_SHORT,
        ).show()
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
                        Toast.makeText(
                                        context,
                                        context.getString(
                                                R.string.settings_app_shortcuts_create_not_supported,
                                        ),
                                        Toast.LENGTH_SHORT,
                                )
                                .show()
                    },
            )

    LaunchedEffect(
            detailType,
            state.allApps,
            state.allAppShortcuts,
            context.packageName,
    ) {
        if (detailType != SettingsDetailType.APP_SHORTCUTS) {
            filteredAppShortcutSources = emptyList()
            hasLoadedAppShortcutSources = false
            return@LaunchedEffect
        }

        val appShortcutSources =
                withContext(Dispatchers.Default) {
                    queryAppShortcutSources(
                            packageManager = context.packageManager,
                            repositoryApps = state.allApps,
                    )
                }
        filteredAppShortcutSources =
                withContext(Dispatchers.Default) {
                    filterAppShortcutSources(
                            sources = appShortcutSources,
                            existingShortcuts = state.allAppShortcuts,
                            currentPackageName = context.packageName,
                    )
                }
        hasLoadedAppShortcutSources = true
    }

    LaunchedEffect(detailType) {
        if (detailType == SettingsDetailType.APP_SHORTCUTS) {
            viewModel.refreshAppShortcutsCacheFirst()
        }
    }

    val onBackAction: () -> Unit =
            if (detailType.isLevel2()) {
                {
                    when (detailType) {
                        SettingsDetailType.TOOLS -> {
                            onBack()
                        }
                        SettingsDetailType.GEMINI_API_CONFIG -> {
                            if (sourceDetailType == null) {
                                onBack()
                            } else {
                                onNavigateToDetail(sourceDetailType)
                            }
                        }
                        SettingsDetailType.UNIT_CONVERTER_INFO,
                        SettingsDetailType.DATE_CALCULATOR_INFO -> {
                            onNavigateToDetail(SettingsDetailType.TOOLS)
                        }
                        else -> {
                            onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS)
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
                                    onToggleOverlayMode = viewModel::setOverlayModeEnabled,
                                    onPickCustomImage = {
                                        overlayCustomImagePickerLauncher.launch(arrayOf("image/*"))
                                    },
                                    onToggleDirectDial = viewModel::setDirectDialEnabled,
                                    onToggleSection = onToggleSection,
                                    onOpenDirectSearchConfigure = {
                                        onNavigateToDetail(SettingsDetailType.GEMINI_API_CONFIG)
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
                                    onSetDefaultAssistant = {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                                context,
                                                                context.getString(R.string.settings_unable_to_open_settings),
                                                                Toast.LENGTH_SHORT,
                                                        )
                                                        .show()
                                            }
                                        }
                                    },
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

    val onToggleDirectSearchSetupExpanded = {
        val newExpanded = !directSearchSetupExpanded
        directSearchSetupExpanded = newExpanded
        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
            userPreferences.setDirectSearchSetupExpanded(newExpanded)
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

    if (detailType.isLevel2()) {
        val shouldShowAppShortcutsContent =
                detailType != SettingsDetailType.APP_SHORTCUTS ||
                        hasLoadedAppShortcutSources

        SettingsDetailLevel2Screen(
                modifier = modifier,
                state = resolvedState,
                callbacks = callbacks,
                detailType = detailType,
                hasUsagePermission = uiState.hasUsagePermission,
                appShortcutFocusShortcut = appShortcutFocusShortcut,
                appShortcutFocusPackageName = appShortcutFocusPackageName,
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
                directSearchSetupExpanded = directSearchSetupExpanded,
                onToggleDirectSearchSetupExpanded = onToggleDirectSearchSetupExpanded,
                disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
                onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
                onNavigateToDetail = onNavigateToDetail,
        )
    }

    WallpaperPermissionFallbackDialog(controller = wallpaperPermissionController)
    AppShortcutSourceFlowDialogs(
            flowState = appShortcutSourceFlow,
            sources = filteredAppShortcutSources,
    )
}
