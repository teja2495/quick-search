package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppTopAppBar
import com.tk.quicksearch.shared.ui.components.AppFabTone
import com.tk.quicksearch.shared.ui.components.AppFloatingActionButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.app.navigation.SettingsPageBackHandler
import com.tk.quicksearch.app.navigation.settingsSwipeBack
import com.tk.quicksearch.shared.ui.components.predictiveBackPreview
import com.tk.quicksearch.shared.ui.components.rememberPredictiveBackPreviewState
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.settings.appearanceSettings.AppearanceSettingsSection
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.aboutSettings.FeaturesList
import com.tk.quicksearch.settings.aboutSettings.OpenSourceLicenseEntry
import com.tk.quicksearch.settings.aboutSettings.OpenSourceLicensesList
import com.tk.quicksearch.settings.aboutSettings.downloadAndShareFeatures
import com.tk.quicksearch.settings.searchEngineSettings.SearchEngines
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState
import com.tk.quicksearch.settings.shared.isAppSettingToggleEnabled
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.settings.shared.settingsScrollClip
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.QuickSearchTheme
import kotlinx.coroutines.launch

@Composable
internal fun SettingsDetailLevel1Screen(
    modifier: Modifier = Modifier,
    uiState: SearchUiState,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    hasUsagePermission: Boolean,
    isDefaultAssistant: Boolean,
    assistantLaunchVoiceModeEnabled: Boolean,
    aiSearchSetupExpanded: Boolean = true,
    onToggleAiSearchSetupExpanded: (() -> Unit)? = null,
    disabledSearchEnginesExpanded: Boolean = true,
    onToggleDisabledSearchEnginesExpanded: (() -> Unit)? = null,
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState =
        androidx.compose.foundation.rememberScrollState(),
) {
    if (detailType.isNestedDetail()) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportFeatures: () -> Unit = {
        coroutineScope.launch {
            runCatching { downloadAndShareFeatures(context) }
                .onFailure {
                    AppMessages.show(context, R.string.settings_features_export_failed)
                }
        }
    }
    val legacyStoragePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                exportFeatures()
            } else {
                AppMessages.show(context, R.string.settings_features_export_failed)
            }
        }
    val onExportFeatures: () -> Unit = {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            legacyStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            exportFeatures()
        }
    }
    var selectedOpenSourceLicense by
        remember(detailType) { mutableStateOf<OpenSourceLicenseEntry?>(null) }
    val onBackAction: () -> Unit =
        if (detailType == SettingsDetailType.OPEN_SOURCE_LICENSES && selectedOpenSourceLicense != null) {
            { selectedOpenSourceLicense = null }
        } else {
            callbacks.onBack
        }
    val backPreview = rememberPredictiveBackPreviewState()
    // Closing an open license entry keeps this page, so it gets no page-level preview.
    val backPreviewsPage = !(detailType == SettingsDetailType.OPEN_SOURCE_LICENSES && selectedOpenSourceLicense != null)
    SettingsPageBackHandler(
        previewState = backPreview,
        preview = backPreviewsPage,
        onBack = onBackAction,
    )
    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty() ||
            state.excludedOtherItems.isNotEmpty()
    val shouldForceMonochromeTheme = detailType == SettingsDetailType.FEATURES_LIST
    val effectiveAppTheme = if (shouldForceMonochromeTheme) AppTheme.MONOCHROME else state.appTheme
    val effectiveDeviceThemeEnabled = if (shouldForceMonochromeTheme) false else state.deviceThemeEnabled

    SettingsScreenBackground(
        appTheme = effectiveAppTheme,
        overlayThemeIntensity = state.overlayThemeIntensity,
        deviceThemeEnabled = effectiveDeviceThemeEnabled,
        amoledThemeEnabled = state.amoledThemeEnabled,
        modifier =
            modifier
                .predictiveBackPreview(backPreview)
                .settingsSwipeBack(seekable = backPreviewsPage, onBack = onBackAction),
    ) {
    MonoThemeWrapper(
        applyMono = shouldForceMonochromeTheme,
        appThemeMode = state.appThemeMode,
        fontScaleMultiplier = state.fontScaleMultiplier,
        useSystemFont = state.useSystemFont,
    ) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailHeader(
                title = stringResource(detailType.titleResId()),
                onBack = onBackAction,
                trailingContent =
                    if (detailType == SettingsDetailType.FEATURES_LIST) {
                        {
                            AppIconButton(onClick = onExportFeatures, tooltip = stringResource(R.string.settings_features_export)) {
                                Icon(
                                    imageVector = MaterialSymbols.Download,
                                    contentDescription =
                                        stringResource(R.string.settings_features_export),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    } else {
                        null
                    },
            )

            Column(
                modifier =
                    Modifier
                        .settingsContentWidth()
                        .fillMaxHeight()
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                        .settingsScrollClip(scrollState)
                        .verticalScroll(scrollState)
                        .padding(bottom = DesignTokens.SectionTopPadding),
            ) {
                when (detailType) {
                    SettingsDetailType.SEARCH_ENGINES -> {
                        SearchEngines(
                            searchEngineOrder = state.searchEngineOrder,
                            disabledSearchEngines = state.disabledSearchEngines,
                            onToggleSearchEngine = callbacks.onToggleSearchEngine,
                            onReorderSearchEngines = callbacks.onReorderSearchEngines,
                            onAddCustomSearchEngine = callbacks.onAddCustomSearchEngine,
                            onUpdateCustomSearchEngine = callbacks.onUpdateCustomSearchEngine,
                            onDeleteCustomSearchEngine = callbacks.onDeleteCustomSearchEngine,
                            shortcutCodes = state.shortcutCodes,
                            setAliasCode = callbacks.setAliasCode,
                            shortcutEnabled = state.shortcutEnabled,
                            amazonDomain = state.amazonDomain,
                            onSetAmazonDomain = callbacks.onSetAmazonDomain,
                            onOpenAiSearchConfigure = callbacks.onOpenAiSearchConfigure,
                            aiSearchAvailable = state.hasApiKey,
                            showTitle = false,
                            disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
                            onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
                            isSearchEngineAliasSuffixEnabled = state.isSearchEngineAliasSuffixEnabled,
                            onToggleSearchEngineAliasSuffixEnabled = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SEARCH_ENGINE_ALIAS_SUFFIX,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            isAliasTriggerAfterSpaceEnabled = state.isAliasTriggerAfterSpaceEnabled,
                            onToggleAliasTriggerAfterSpaceEnabled =
                                callbacks.onToggleAliasTriggerAfterSpaceEnabled,
                            showAiSearchAtTop = true,
                        )
                    }

                    SettingsDetailType.SEARCH_RESULTS -> {
                        SearchResultsSettingsSection(
                            state = state,
                            callbacks = callbacks,
                            hasUsagePermission = hasUsagePermission,
                            hasContactPermission = PermissionUtils.hasContactsPermission(context),
                            hasFilePermission = PermissionHelper.checkFilesPermission(context),
                            hasCalendarPermission = PermissionUtils.hasCalendarPermission(context),
                            onNavigateToExcludedItems = {
                                if (hasExcludedItems) {
                                    onNavigateToDetail(SettingsDetailType.EXCLUDED_ITEMS)
                                } else {
                                    AppMessages.show(context, R.string.settings_excluded_items_empty)
                                }
                            },
                            onNavigateToNicknames = {
                                onNavigateToDetail(SettingsDetailType.NICKNAMES)
                            },
                            onNavigateToTriggers = {
                                onNavigateToDetail(SettingsDetailType.TRIGGERS)
                            },
                            onNavigateToAppManagement = {
                                onNavigateToDetail(SettingsDetailType.APP_MANAGEMENT)
                            },
                            onNavigateToAppShortcuts = {
                                onNavigateToDetail(SettingsDetailType.APP_SHORTCUTS)
                            },
                            onNavigateToCallsTexts = {
                                onNavigateToDetail(SettingsDetailType.CALLS_TEXTS)
                            },
                            onNavigateToFiles = {
                                onNavigateToDetail(SettingsDetailType.FILES)
                            },
                            onNavigateToDeviceSettings = {
                                onNavigateToDetail(SettingsDetailType.DEVICE_SETTINGS)
                            },
                            onNavigateToCalendarEvents = {
                                onNavigateToDetail(SettingsDetailType.CALENDAR_EVENTS)
                            },
                            onNavigateToNotes = {
                                onNavigateToDetail(SettingsDetailType.NOTES)
                            },
                            onNavigateToReminders = {
                                onNavigateToDetail(SettingsDetailType.REMINDERS)
                            },
                            onNavigateToBookmarks = {
                                onNavigateToDetail(SettingsDetailType.BOOKMARKS)
                            },
                        )
                    }

                    SettingsDetailType.APPEARANCE -> {
                        val enabledSearchEngines =
                            state.searchEngineOrder.filter { target ->
                                target.getId() !in state.disabledSearchEngines
                            }

                        AppearanceSettingsSection(
                            oneHandedMode = state.oneHandedMode,
                            onToggleOneHandedMode = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.ONE_HANDED_MODE,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            bottomSearchBarEnabled = state.bottomSearchBarEnabled,
                            onToggleBottomSearchBar = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.BOTTOM_SEARCHBAR,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            compactLayoutEnabled = !state.cardLayoutEnabled,
                            onToggleCompactLayout = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.COMPACT_LAYOUT,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            unifiedPinnedItemsEnabled = state.unifiedPinnedItemsEnabled,
                            onToggleUnifiedPinnedItems = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.UNIFIED_PINNED_ITEMS,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            homePinnedSectionOrder = state.homePinnedSectionOrder,
                            onHomePinnedSectionOrderChange = { order ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.HomePinnedSectionOrder(order),
                                )
                            },
                            pinnedAppShortcutsInAppGrid = state.pinnedAppShortcutsInAppGrid,
                            onTogglePinnedAppShortcutsInAppGrid = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.PINNED_APP_SHORTCUTS_IN_APP_GRID,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            showStatusBar = state.showStatusBar,
                            onToggleShowStatusBar = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SHOW_STATUS_BAR,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            searchChromeBlurEnabled = state.searchChromeBlurEnabled,
                            onToggleSearchChromeBlur = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SEARCH_CHROME_BLUR,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            settingsIconEnabled = state.settingsIconEnabled,
                            onToggleSettingsIcon = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SETTINGS_ICON,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                            wallpaperBlurRadius = state.wallpaperBlurRadius,
                            onWallpaperBackgroundAlphaChange = { alpha ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.WallpaperBackgroundAlpha(alpha),
                                )
                            },
                            onWallpaperBlurRadiusChange = { radius ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.WallpaperBlurRadius(radius),
                                )
                            },
                            appTheme = state.appTheme,
                            overlayThemeIntensity = state.overlayThemeIntensity,
                            appThemeMode = state.appThemeMode,
                            fontScaleMultiplier = state.fontScaleMultiplier,
                            useSystemFont = state.useSystemFont,
                            homeTextColorOverride = state.homeTextColorOverride,
                            onSetAppTheme = { theme ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.AppThemeSetting(theme),
                                )
                            },
                            onOverlayThemeIntensityChange = { intensity ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.OverlayThemeIntensity(intensity),
                                )
                            },
                            onSetAppThemeMode = { themeMode ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.AppThemeModeSetting(themeMode),
                                )
                            },
                            onFontScaleMultiplierChange = { multiplier ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.FontScaleMultiplier(multiplier),
                                )
                            },
                            onUseSystemFontChange = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.UseSystemFont(enabled),
                                )
                            },
                            onHomeTextColorChange = { color ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.HomeTextColorOverride(color),
                                )
                            },
                            backgroundSource = state.backgroundSource,
                            customImageUri = state.customImageUri,
                            onSetBackgroundSource = { source ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.BackgroundSourceSetting(source),
                                )
                            },
                            onPickCustomImage = callbacks.onPickCustomImage,
                            onRemoveCustomImage = callbacks.onRemoveCustomImage,
                            onRequestWallpaperPermission = callbacks.onRequestWallpaperPermission,
                            isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                            searchEngineCompactRowCount = state.searchEngineCompactRowCount,
                            enabledSearchEngines = enabledSearchEngines,
                            onToggleSearchEngineCompactMode = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            onSetSearchEngineCompactRowCount = { rowCount ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.SearchEngineCompactRowCount(rowCount),
                                )
                            },
                            selectedIconPackPackage = state.selectedIconPackPackage,
                            availableIconPacks = state.availableIconPacks,
                            maskUnsupportedIconPackIcons = state.maskUnsupportedIconPackIcons,
                            showAppLabels = state.showAppLabels,
                            onToggleAppLabels = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.APP_LABELS,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            phoneAppGridColumns = state.phoneAppGridColumns,
                            onSetPhoneAppGridColumns = { columns ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.PhoneAppGridColumns(columns),
                                )
                            },
                            appIconSizeStep = state.appIconSizeStep,
                            onSetAppIconSizeStep = { step ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.AppIconSizeStep(step),
                                )
                            },
                            onSelectIconPack = { packageName ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.IconPackPackageSetting(packageName),
                                )
                            },
                            onResetAllIcons = {
                                callbacks.onApplySettingsCommand(SettingsCommand.ResetAllAppIcons)
                            },
                            onSetMaskUnsupportedIconPackIcons =
                                    callbacks.onSetMaskUnsupportedIconPackIcons,
                            onRefreshIconPacks = {
                                callbacks.onRefreshIconPacks()
                            },
                            onSearchIconPacks = callbacks.onSearchIconPacks,
                            appIconShape = state.appIconShape,
                            onSetAppIconShape = { shape ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.AppIconShapeSetting(shape),
                                )
                            },
                            onSetAppIconCornerPercent = { percent ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.AppIconCornerPercent(percent),
                                )
                            },
                            launcherAppIcon = state.launcherAppIcon,
                            onSetLauncherAppIcon = { icon ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.LauncherAppIconSetting(icon),
                                )
                            },
                            searchBarStyle = state.searchBarStyle,
                            onSetSearchBarStyle = { style ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.SearchBarStyleSetting(style),
                                )
                            },
                            themedIconsEnabled = state.themedIconsEnabled,
                            onThemedIconsToggle = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.THEMED_ICONS,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            deviceThemeEnabled = state.deviceThemeEnabled,
                            onDeviceThemeToggle = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.DEVICE_THEME,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            amoledThemeEnabled = state.amoledThemeEnabled,
                            onAmoledThemeToggle = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.AMOLED_THEME,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            accentColorMode = state.accentColorMode,
                            customAccentColorArgb = state.customAccentColorArgb,
                            onAccentColorModeChange = { mode ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.AccentColorModeSetting(mode),
                                )
                            },
                            onCustomAccentColorChange = { argb ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.CustomAccentColorSetting(argb),
                                )
                            },
                            hasWallpaperPermission = state.hasWallpaperPermission,
                            wallpaperAvailable = state.wallpaperAvailable,
                        )
                    }

                    SettingsDetailType.LAUNCH_OPTIONS -> {
                        LaunchOptionsSettings(
                            isDefaultAssistant = isDefaultAssistant,
                            assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
                            onSetDefaultAssistant = callbacks.onSetDefaultAssistant,
                            onSetDefaultLauncher = callbacks.onSetDefaultLauncher,
                            onToggleAssistantLaunchVoiceMode =
                                callbacks.onToggleAssistantLaunchVoiceMode,
                            onAddHomeScreenWidget = callbacks.onAddHomeScreenWidget,
                            onAddQuickSettingsTile = callbacks.onAddQuickSettingsTile,
                            onOpenEdgeGesture = {
                                onNavigateToDetail(SettingsDetailType.EDGE_GESTURE)
                            },
                            onOpenFloatingButton = {
                                onNavigateToDetail(SettingsDetailType.FLOATING_BUTTON)
                            },
                            modifier = Modifier,
                        )
                    }

                    SettingsDetailType.MORE_OPTIONS -> {
                        MoreOptionsSettings(
                            isToggleEnabled = { toggleKey ->
                                uiState.isAppSettingToggleEnabled(toggleKey)
                            },
                            notificationCountsEnabled = uiState.notificationCountsEnabled,
                            onApplySettingsCommand = callbacks.onApplySettingsCommand,
                            modifier = Modifier,
                        )
                    }

                    SettingsDetailType.PERMISSIONS -> {
                        PermissionsSettings(
                            onRequestUsagePermission = callbacks.onRequestUsagePermission,
                            onRequestContactPermission = callbacks.onRequestContactPermission,
                            onRequestFilePermission = callbacks.onRequestFilePermission,
                            onRequestCalendarPermission = callbacks.onRequestCalendarPermission,
                            onRequestCallPermission = callbacks.onRequestCallPermission,
                            modifier = Modifier,
                        )
                    }

                    SettingsDetailType.FEATURES_LIST -> {
                        FeaturesList(
                            scrollState = scrollState,
                            modifier =
                                Modifier.padding(
                                    bottom = DesignTokens.SectionTopPadding,
                                ),
                        )
                    }

                    SettingsDetailType.OPEN_SOURCE_LICENSES -> {
                        OpenSourceLicensesList(
                            selectedEntry = selectedOpenSourceLicense,
                            onSelectedEntryChange = { selectedOpenSourceLicense = it },
                            modifier =
                                Modifier.padding(
                                    bottom = DesignTokens.SectionTopPadding,
                                ),
                        )
                    }

                    SettingsDetailType.EXCLUDED_ITEMS,
                    SettingsDetailType.APP_MANAGEMENT,
                    SettingsDetailType.APP_SHORTCUTS,
                    SettingsDetailType.DEVICE_SETTINGS,
                    SettingsDetailType.CALENDAR_EVENTS,
                    SettingsDetailType.REMINDERS,
                    SettingsDetailType.BOOKMARKS,
                    SettingsDetailType.NOTES,
                    SettingsDetailType.NOTE_EDITOR,
                    SettingsDetailType.CALLS_TEXTS,
                    SettingsDetailType.FILES,
                    SettingsDetailType.TOOLS,
                    SettingsDetailType.AT_A_GLANCE,
                    SettingsDetailType.GEMINI_API_CONFIG,
                    SettingsDetailType.API_KEY_SETUP,
                    SettingsDetailType.UNIT_CONVERTER_INFO,
                    SettingsDetailType.DATE_CALCULATOR_INFO,
                    SettingsDetailType.CUSTOM_TOOL_EDITOR,
                    SettingsDetailType.TASKER_INTEGRATION,
                    SettingsDetailType.NICKNAMES,
                    SettingsDetailType.TRIGGERS,
                    SettingsDetailType.GESTURES,
                    SettingsDetailType.NOTIFICATION_HISTORY,
                    SettingsDetailType.EDGE_GESTURE,
                    SettingsDetailType.FLOATING_BUTTON,
                    -> Unit
                }
            }
        }

        if (detailType == SettingsDetailType.FEATURES_LIST && scrollState.value > 0) {
            AppFloatingActionButton(
                onClick = { coroutineScope.launch { scrollState.animateScrollTo(0) } },
                icon = MaterialSymbols.KeyboardArrowUp,
                contentDescription = stringResource(R.string.settings_scroll_to_top),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(DesignTokens.SpacingLarge),
                tone = AppFabTone.Tonal,
            )
        }
    }
    }
    }
}

@Composable
internal fun SettingsDetailHeader(
    title: String,
    onBack: () -> Unit,
    trailingContent: (@Composable (() -> Unit))? = null,
) {
    AppTopAppBar(
        title = title,
        onBack = onBack,
        actions = { trailingContent?.invoke() },
    )
}

@Composable
private fun MonoThemeWrapper(
    applyMono: Boolean,
    appThemeMode: AppThemeMode,
    fontScaleMultiplier: Float,
    useSystemFont: Boolean,
    content: @Composable () -> Unit,
) {
    if (applyMono) {
        QuickSearchTheme(
            fontScaleMultiplier = fontScaleMultiplier,
            useSystemFont = useSystemFont,
            appTheme = AppTheme.MONOCHROME,
            appThemeMode = appThemeMode,
        ) { content() }
    } else {
        content()
    }
}
