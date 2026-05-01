package com.tk.quicksearch.settings.settingsDetailScreen

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppThemeMode
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.FeaturesList
import com.tk.quicksearch.settings.OpenSourceLicenseEntry
import com.tk.quicksearch.settings.OpenSourceLicensesList
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngines
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.settings.shared.isAppSettingToggleEnabled
import com.tk.quicksearch.settings.shared.settingsContentWidth
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
) {
    if (detailType.isLevel2()) return

    val context = LocalContext.current
    var selectedOpenSourceLicense by
        remember(detailType) { mutableStateOf<OpenSourceLicenseEntry?>(null) }
    val onBackAction: () -> Unit =
        if (detailType == SettingsDetailType.OPEN_SOURCE_LICENSES && selectedOpenSourceLicense != null) {
            { selectedOpenSourceLicense = null }
        } else {
            callbacks.onBack
        }
    BackHandler(onBack = onBackAction)
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty()
    val shouldForceMonochromeTheme = detailType == SettingsDetailType.FEATURES_LIST
    val effectiveAppTheme = if (shouldForceMonochromeTheme) AppTheme.MONOCHROME else state.appTheme
    val effectiveDeviceThemeEnabled = if (shouldForceMonochromeTheme) false else state.deviceThemeEnabled
    SettingsScreenBackground(
        appTheme = effectiveAppTheme,
        overlayThemeIntensity = state.overlayThemeIntensity,
        deviceThemeEnabled = effectiveDeviceThemeEnabled,
        modifier = modifier,
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
            )

            Column(
                modifier =
                    Modifier
                        .settingsContentWidth()
                        .fillMaxHeight()
                        .align(Alignment.CenterHorizontally)
                        .verticalScroll(scrollState)
                        .padding(
                            start = DesignTokens.ContentHorizontalPadding,
                            end = DesignTokens.ContentHorizontalPadding,
                            bottom = DesignTokens.SectionTopPadding,
                        ),
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
                            setAliasEnabled = callbacks.setAliasEnabled,
                            isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                            amazonDomain = state.amazonDomain,
                            onSetAmazonDomain = callbacks.onSetAmazonDomain,
                            onSetGeminiApiKey = callbacks.onSetGeminiApiKey,
                            geminiApiKeyLast4 = state.geminiApiKeyLast4,
                            isSavingGeminiApiKey = state.isSavingGeminiApiKey,
                            personalContext = state.personalContext,
                            onSetPersonalContext = callbacks.onSetPersonalContext,
                            geminiModel = state.geminiModel,
                            geminiGroundingEnabled = state.geminiGroundingEnabled,
                            availableGeminiModels = state.availableGeminiModels,
                            onSetGeminiModel = callbacks.onSetGeminiModel,
                            onSetGeminiGroundingEnabled = callbacks.onSetGeminiGroundingEnabled,
                            onRefreshAvailableGeminiModels = callbacks.onRefreshAvailableGeminiModels,
                            onOpenAiSearchConfigure = callbacks.onOpenAiSearchConfigure,
                            aiSearchAvailable = state.hasGeminiApiKey,
                            showTitle = false,
                            aiSearchSetupExpanded = aiSearchSetupExpanded,
                            onToggleAiSearchSetupExpanded = onToggleAiSearchSetupExpanded,
                            disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
                            onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
                            onToggleSearchEngineCompactMode = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.SEARCH_ENGINE_COMPACT_MODE,
                                        enabled = enabled,
                                    ),
                                )
                            },
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
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.settings_excluded_items_empty),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
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
                        )
                    }

                    SettingsDetailType.APPEARANCE -> {
                        val hasEnabledSearchEngines =
                            state.searchEngineOrder.any { target ->
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
                            backgroundSource = state.backgroundSource,
                            customImageUri = state.customImageUri,
                            onSetBackgroundSource = { source ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.BackgroundSourceSetting(source),
                                )
                            },
                            onPickCustomImage = callbacks.onPickCustomImage,
                            onRequestWallpaperPermission = callbacks.onRequestWallpaperPermission,
                            isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                            searchEngineCompactRowCount = state.searchEngineCompactRowCount,
                            hasEnabledSearchEngines = hasEnabledSearchEngines,
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
                            onSelectIconPack = { packageName ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.IconPackPackageSetting(packageName),
                                )
                            },
                            onSetMaskUnsupportedIconPackIcons =
                                    callbacks.onSetMaskUnsupportedIconPackIcons,
                            onRefreshIconPacks = {
                                callbacks.onRefreshIconPacks()
                                Toast
                                    .makeText(
                                        context,
                                        context.getString(R.string.settings_refreshing_icon_packs),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            },
                            onSearchIconPacks = callbacks.onSearchIconPacks,
                            appIconShape = state.appIconShape,
                            onSetAppIconShape = { shape ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.AppIconShapeSetting(shape),
                                )
                            },
                            launcherAppIcon = state.launcherAppIcon,
                            onSetLauncherAppIcon = { icon ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.LauncherAppIconSetting(icon),
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
                            wallpaperAccentEnabled = state.wallpaperAccentEnabled,
                            onWallpaperAccentToggle = { enabled ->
                                callbacks.onApplySettingsCommand(
                                    SettingsCommand.Toggle(
                                        key = com.tk.quicksearch.search.appSettings.AppSettingsToggleKey.WALLPAPER_ACCENT,
                                        enabled = enabled,
                                    ),
                                )
                            },
                            hasWallpaperPermission = state.hasWallpaperPermission,
                        )
                    }

                    SettingsDetailType.LAUNCH_OPTIONS -> {
                        LaunchOptionsSettings(
                            isDefaultAssistant = isDefaultAssistant,
                            assistantLaunchVoiceModeEnabled = assistantLaunchVoiceModeEnabled,
                            onSetDefaultAssistant = callbacks.onSetDefaultAssistant,
                            onToggleAssistantLaunchVoiceMode =
                                callbacks.onToggleAssistantLaunchVoiceMode,
                            onAddHomeScreenWidget = callbacks.onAddHomeScreenWidget,
                            onAddQuickSettingsTile = callbacks.onAddQuickSettingsTile,
                            modifier = Modifier,
                        )
                    }

                    SettingsDetailType.MORE_OPTIONS -> {
                        MoreOptionsSettings(
                            isToggleEnabled = { toggleKey ->
                                uiState.isAppSettingToggleEnabled(toggleKey)
                            },
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
                    SettingsDetailType.NOTES,
                    SettingsDetailType.NOTE_EDITOR,
                    SettingsDetailType.CALLS_TEXTS,
                    SettingsDetailType.FILES,
                    SettingsDetailType.TOOLS,
                    SettingsDetailType.GEMINI_API_CONFIG,
                    SettingsDetailType.UNIT_CONVERTER_INFO,
                    SettingsDetailType.DATE_CALCULATOR_INFO,
                    SettingsDetailType.CUSTOM_TOOL_EDITOR,
                    -> Unit
                }
            }
        }

        if (detailType == SettingsDetailType.FEATURES_LIST && scrollState.value > 0) {
            FloatingActionButton(
                onClick = { coroutineScope.launch { scrollState.animateScrollTo(0) } },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(DesignTokens.SpacingLarge)
                        .size(40.dp),
                containerColor = AppColors.Accent,
                contentColor = AppColors.OnAccent,
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.settings_scroll_to_top),
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
            }
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.ContentHorizontalPadding,
                    vertical = DesignTokens.HeaderVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_navigate_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(modifier = Modifier.width(DesignTokens.HeaderIconSpacing))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        trailingContent?.let {
            Spacer(modifier = Modifier.width(DesignTokens.HeaderIconSpacing))
            it()
        }
    }
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
