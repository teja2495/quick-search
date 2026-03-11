package com.tk.quicksearch.settings.settingsDetailScreen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.FeaturesList
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngines
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun SettingsDetailLevel1Screen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    hasUsagePermission: Boolean,
    isDefaultAssistant: Boolean,
    assistantLaunchVoiceModeEnabled: Boolean,
    directSearchSetupExpanded: Boolean = true,
    onToggleDirectSearchSetupExpanded: (() -> Unit)? = null,
    disabledSearchEnginesExpanded: Boolean = true,
    onToggleDisabledSearchEnginesExpanded: (() -> Unit)? = null,
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
) {
    if (detailType.isLevel2()) return

    val context = LocalContext.current
    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
    val hasExcludedItems =
        state.suggestionExcludedApps.isNotEmpty() ||
            state.resultExcludedApps.isNotEmpty() ||
            state.excludedContacts.isNotEmpty() ||
            state.excludedFiles.isNotEmpty() ||
            state.excludedFileExtensions.isNotEmpty() ||
            state.excludedSettings.isNotEmpty() ||
            state.excludedAppShortcuts.isNotEmpty()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsDetailHeader(
                title = stringResource(detailType.titleResId()),
                onBack = callbacks.onBack,
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
                            personalContext = state.personalContext,
                            onSetPersonalContext = callbacks.onSetPersonalContext,
                            geminiModel = state.geminiModel,
                            geminiGroundingEnabled = state.geminiGroundingEnabled,
                            availableGeminiModels = state.availableGeminiModels,
                            onSetGeminiModel = callbacks.onSetGeminiModel,
                            onSetGeminiGroundingEnabled = callbacks.onSetGeminiGroundingEnabled,
                            onRefreshAvailableGeminiModels = callbacks.onRefreshAvailableGeminiModels,
                            onOpenDirectSearchConfigure = callbacks.onOpenDirectSearchConfigure,
                            directSearchAvailable = state.hasGeminiApiKey,
                            showTitle = false,
                            directSearchSetupExpanded = directSearchSetupExpanded,
                            onToggleDirectSearchSetupExpanded = onToggleDirectSearchSetupExpanded,
                            disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
                            onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
                            onToggleSearchEngineCompactMode = callbacks.onToggleSearchEngineCompactMode,
                            isSearchEngineAliasSuffixEnabled = state.isSearchEngineAliasSuffixEnabled,
                            onToggleSearchEngineAliasSuffixEnabled =
                                callbacks.onToggleSearchEngineAliasSuffixEnabled,
                            showDirectSearchAtTop = true,
                        )
                    }

                    SettingsDetailType.SEARCH_RESULTS -> {
                        SearchResultsSettingsSection(
                            state = state,
                            callbacks = callbacks,
                            hasUsagePermission = hasUsagePermission,
                            hasContactPermission = PermissionUtils.hasContactsPermission(context),
                            hasFilePermission = PermissionHelper.checkFilesPermission(context),
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
                        )
                    }

                    SettingsDetailType.APPEARANCE -> {
                        val hasEnabledSearchEngines =
                            state.searchEngineOrder.any { target ->
                                target.getId() !in state.disabledSearchEngines
                            }

                        AppearanceSettingsSection(
                            oneHandedMode = state.oneHandedMode,
                            onToggleOneHandedMode = callbacks.onToggleOneHandedMode,
                            bottomSearchBarEnabled = state.bottomSearchBarEnabled,
                            onToggleBottomSearchBar = callbacks.onToggleBottomSearchBar,
                            wallpaperBackgroundAlpha = state.wallpaperBackgroundAlpha,
                            wallpaperBlurRadius = state.wallpaperBlurRadius,
                            onWallpaperBackgroundAlphaChange = callbacks.onWallpaperBackgroundAlphaChange,
                            onWallpaperBlurRadiusChange = callbacks.onWallpaperBlurRadiusChange,
                            overlayGradientTheme = state.overlayGradientTheme,
                            overlayThemeIntensity = state.overlayThemeIntensity,
                            fontScaleMultiplier = state.fontScaleMultiplier,
                            onSetOverlayGradientTheme = callbacks.onSetOverlayGradientTheme,
                            onOverlayThemeIntensityChange = callbacks.onOverlayThemeIntensityChange,
                            onFontScaleMultiplierChange = callbacks.onFontScaleMultiplierChange,
                            backgroundSource = state.backgroundSource,
                            customImageUri = state.customImageUri,
                            onSetBackgroundSource = callbacks.onSetBackgroundSource,
                            onPickCustomImage = callbacks.onPickCustomImage,
                            onRequestWallpaperPermission = callbacks.onRequestWallpaperPermission,
                            isSearchEngineCompactMode = state.isSearchEngineCompactMode,
                            searchEngineCompactRowCount = state.searchEngineCompactRowCount,
                            hasEnabledSearchEngines = hasEnabledSearchEngines,
                            onToggleSearchEngineCompactMode = callbacks.onToggleSearchEngineCompactMode,
                            onSetSearchEngineCompactRowCount = callbacks.onSetSearchEngineCompactRowCount,
                            selectedIconPackPackage = state.selectedIconPackPackage,
                            availableIconPacks = state.availableIconPacks,
                            showAppLabels = state.showAppLabels,
                            onToggleAppLabels = callbacks.onToggleAppLabels,
                            onSelectIconPack = callbacks.onSelectIconPack,
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

                    SettingsDetailType.PERMISSIONS -> {
                        PermissionsSettings(
                            onRequestUsagePermission = callbacks.onRequestUsagePermission,
                            onRequestContactPermission = callbacks.onRequestContactPermission,
                            onRequestFilePermission = callbacks.onRequestFilePermission,
                            onRequestCallPermission = callbacks.onRequestCallPermission,
                            modifier = Modifier,
                        )
                    }

                    SettingsDetailType.FEATURES_LIST -> {
                        FeaturesList(
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
                    SettingsDetailType.CALLS_TEXTS,
                    SettingsDetailType.FILES,
                    SettingsDetailType.DIRECT_SEARCH_CONFIGURE,
                    SettingsDetailType.TOOLS,
                    -> Unit
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

internal fun SettingsDetailType.titleResId(): Int =
    when (this) {
        SettingsDetailType.SEARCH_ENGINES -> R.string.settings_search_engines_title
        SettingsDetailType.EXCLUDED_ITEMS -> R.string.settings_excluded_items_title
        SettingsDetailType.SEARCH_RESULTS -> R.string.settings_search_results_title
        SettingsDetailType.APP_MANAGEMENT -> R.string.settings_manage_apps_title
        SettingsDetailType.APP_SHORTCUTS -> R.string.section_app_shortcuts
        SettingsDetailType.DEVICE_SETTINGS -> R.string.section_settings
        SettingsDetailType.APPEARANCE -> R.string.settings_appearance_title
        SettingsDetailType.CALLS_TEXTS -> R.string.settings_calls_texts_title
        SettingsDetailType.FILES -> R.string.settings_file_types_title
        SettingsDetailType.LAUNCH_OPTIONS -> R.string.settings_launch_options_title
        SettingsDetailType.PERMISSIONS -> R.string.settings_permissions_title
        SettingsDetailType.DIRECT_SEARCH_CONFIGURE -> R.string.settings_direct_search_configure_title
        SettingsDetailType.TOOLS -> R.string.settings_tools_title
        SettingsDetailType.FEATURES_LIST -> R.string.settings_all_quick_search_features
    }

internal fun SettingsDetailType.isLevel2(): Boolean =
    this == SettingsDetailType.APP_MANAGEMENT ||
        this == SettingsDetailType.APP_SHORTCUTS ||
        this == SettingsDetailType.EXCLUDED_ITEMS ||
        this == SettingsDetailType.DEVICE_SETTINGS ||
        this == SettingsDetailType.CALLS_TEXTS ||
        this == SettingsDetailType.FILES ||
        this == SettingsDetailType.DIRECT_SEARCH_CONFIGURE ||
        this == SettingsDetailType.TOOLS

internal fun SettingsDetailType.level(): Int = if (isLevel2()) 2 else 1

/**
 * Enum to represent different types of settings detail screens.
 */
enum class SettingsDetailType {
    SEARCH_ENGINES,
    EXCLUDED_ITEMS,
    SEARCH_RESULTS,
    APP_MANAGEMENT,
    APP_SHORTCUTS,
    DEVICE_SETTINGS,
    APPEARANCE,
    CALLS_TEXTS,
    FILES,
    LAUNCH_OPTIONS,
    PERMISSIONS,
    DIRECT_SEARCH_CONFIGURE,
    TOOLS,
    FEATURES_LIST,
}
