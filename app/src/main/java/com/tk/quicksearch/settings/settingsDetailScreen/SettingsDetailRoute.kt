package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.tile.requestAddQuickSearchTile
import com.tk.quicksearch.util.isDefaultDigitalAssistant

@Composable
fun SettingsDetailRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    detailType: SettingsDetailType,
    onNavigateToDetail: (SettingsDetailType) -> Unit = {},
    onRequestUsagePermission: () -> Unit = {},
    onRequestContactPermission: () -> Unit = {},
    onRequestFilePermission: () -> Unit = {},
    onRequestCallPermission: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val state = SettingsScreenState(
        suggestionExcludedApps = uiState.suggestionExcludedApps,
        resultExcludedApps = uiState.resultExcludedApps,
        excludedContacts = uiState.excludedContacts,
        excludedFiles = uiState.excludedFiles,
        excludedSettings = uiState.excludedSettings,
        excludedAppShortcuts = uiState.excludedAppShortcuts,
        searchEngineOrder = uiState.searchTargetsOrder,
        disabledSearchEngines = uiState.disabledSearchTargetIds,
        enabledFileTypes = uiState.enabledFileTypes,
        showFolders = uiState.showFolders,
        showSystemFiles = uiState.showSystemFiles,
        showHiddenFiles = uiState.showHiddenFiles,
        excludedFileExtensions = uiState.excludedFileExtensions,
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        shortcutCodes = uiState.shortcutCodes,
        shortcutEnabled = uiState.shortcutEnabled,
        messagingApp = uiState.messagingApp,
        isWhatsAppInstalled = uiState.isWhatsAppInstalled,
        isTelegramInstalled = uiState.isTelegramInstalled,
        showWallpaperBackground = uiState.showWallpaperBackground,
        wallpaperBackgroundAlpha = uiState.wallpaperBackgroundAlpha,
        wallpaperBlurRadius = uiState.wallpaperBlurRadius,
        selectedIconPackPackage = uiState.selectedIconPackPackage,
        availableIconPacks = uiState.availableIconPacks,
        showAllResults = uiState.showAllResults,
        directDialEnabled = uiState.directDialEnabled,
        sectionOrder = uiState.sectionOrder,
        disabledSections = uiState.disabledSections,
        isSearchEngineCompactMode = uiState.isSearchEngineCompactMode,
        amazonDomain = uiState.amazonDomain,
        calculatorEnabled = uiState.calculatorEnabled,
        webSuggestionsEnabled = uiState.webSuggestionsEnabled,
        webSuggestionsCount = uiState.webSuggestionsCount,
        recentQueriesEnabled = uiState.recentQueriesEnabled,
        recentQueriesCount = uiState.recentQueriesCount,
        hasGeminiApiKey = uiState.hasGeminiApiKey,
        geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
        personalContext = uiState.personalContext
    )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userPreferences = remember { UserAppPreferences(context) }
    var shouldShowShortcutHint by remember(detailType) {
        mutableStateOf(detailType == SettingsDetailType.SEARCH_ENGINES && userPreferences.shouldShowShortcutHintBanner())
    }
    var isDefaultAssistant by remember {
        mutableStateOf(context.isDefaultDigitalAssistant())
    }
    var directSearchSetupExpanded by remember(detailType) {
        mutableStateOf(
            if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                // Always start expanded in search engine settings screen
                true
            } else {
                true
            }
        )
    }

    DisposableEffect(lifecycleOwner, detailType) {
        val observer = LifecycleEventObserver { _, event ->
            if (detailType != SettingsDetailType.SEARCH_ENGINES) return@LifecycleEventObserver
                when (event) {
                Lifecycle.Event.ON_START -> {
                    userPreferences.resetShortcutHintBannerSessionDismissed()
                    shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                        isDefaultAssistant = context.isDefaultDigitalAssistant()
                }
                Lifecycle.Event.ON_RESUME -> {
                    shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
                        isDefaultAssistant = context.isDefaultDigitalAssistant()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onDismissShortcutHint = {
        userPreferences.incrementShortcutHintBannerDismissCount()
        userPreferences.setShortcutHintBannerSessionDismissed(true)
        shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
    }

    val onRequestAddQuickSettingsTile = { requestAddQuickSearchTile(context) }

    val callbacks = SettingsScreenCallbacks(
        onBack = onBack,
        onRemoveSuggestionExcludedApp = viewModel::unhideAppFromSuggestions,
        onRemoveResultExcludedApp = viewModel::unhideAppFromResults,
        onRemoveExcludedContact = viewModel::removeExcludedContact,
        onRemoveExcludedFile = viewModel::removeExcludedFile,
        onRemoveExcludedSetting = viewModel::removeExcludedSetting,
        onRemoveExcludedAppShortcut = viewModel::removeExcludedAppShortcut,
        onClearAllExclusions = viewModel::clearAllExclusions,
        onToggleSearchEngine = viewModel::setSearchTargetEnabled,
        onReorderSearchEngines = viewModel::reorderSearchTargets,
        onToggleFileType = viewModel::setFileTypeEnabled,
        onToggleFolders = viewModel::setShowFolders,
        onToggleSystemFiles = viewModel::setShowSystemFiles,
        onToggleHiddenFiles = viewModel::setShowHiddenFiles,
        onRemoveExcludedFileExtension = viewModel::removeExcludedFileExtension,
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        setShortcutCode = viewModel::setShortcutCode,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        onSetMessagingApp = viewModel::setMessagingApp,
        onToggleShowWallpaperBackground = viewModel::setShowWallpaperBackground,
        onWallpaperBackgroundAlphaChange = viewModel::setWallpaperBackgroundAlpha,
        onWallpaperBlurRadiusChange = viewModel::setWallpaperBlurRadius,
        onSelectIconPack = viewModel::setIconPackPackage,
        onSearchIconPacks = viewModel::searchIconPacks,
        onRefreshIconPacks = viewModel::refreshIconPacks,
        onToggleShowAllResults = viewModel::setShowAllResults,
        onToggleDirectDial = viewModel::setDirectDialEnabled,
        onToggleSection = { _, _ -> },
        onReorderSections = viewModel::reorderSections,
        onToggleSearchEngineCompactMode = viewModel::setSearchEngineCompactMode,
        onSetAmazonDomain = viewModel::setAmazonDomain,
        onToggleCalculator = viewModel::setCalculatorEnabled,
        onToggleWebSuggestions = viewModel::setWebSuggestionsEnabled,
        onWebSuggestionsCountChange = viewModel::setWebSuggestionsCount,
        onToggleRecentQueries = viewModel::setRecentQueriesEnabled,
        onRecentQueriesCountChange = viewModel::setRecentQueriesCount,
        onSetGeminiApiKey = viewModel::setGeminiApiKey,
        onSetPersonalContext = viewModel::setPersonalContext,
        onAddQuickSettingsTile = onRequestAddQuickSettingsTile,
        onSetDefaultAssistant = {
            try {
                val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to general settings if voice input settings not available
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_unable_to_open_settings),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onRefreshApps = viewModel::refreshApps,
        onRefreshContacts = viewModel::refreshContacts,
        onRefreshFiles = viewModel::refreshFiles,
        onRequestUsagePermission = onRequestUsagePermission,
        onRequestContactPermission = onRequestContactPermission,
        onRequestFilePermission = onRequestFilePermission,
        onRequestCallPermission = onRequestCallPermission
    )

    val onToggleDirectSearchSetupExpanded = {
        val newExpanded = !directSearchSetupExpanded
        directSearchSetupExpanded = newExpanded
        if (detailType == SettingsDetailType.SEARCH_ENGINES) {
            userPreferences.setDirectSearchSetupExpanded(newExpanded)
        }
    }

    SettingsDetailScreen(
        modifier = modifier,
        state = state,
        callbacks = callbacks,
        detailType = detailType,
        showShortcutHintBanner = shouldShowShortcutHint,
        onDismissShortcutHintBanner = onDismissShortcutHint,
        isDefaultAssistant = isDefaultAssistant,
        directSearchSetupExpanded = directSearchSetupExpanded,
        onToggleDirectSearchSetupExpanded = onToggleDirectSearchSetupExpanded,
        onNavigateToDetail = onNavigateToDetail
    )
}