package com.tk.quicksearch.settings

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.SearchViewModel

@Composable
fun SettingsDetailRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    detailType: SettingsDetailType
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val state = SettingsScreenState(
        suggestionExcludedApps = uiState.suggestionExcludedApps,
        resultExcludedApps = uiState.resultExcludedApps,
        excludedContacts = uiState.excludedContacts,
        excludedFiles = uiState.excludedFiles,
        excludedSettings = uiState.excludedSettings,
        searchEngineOrder = uiState.searchEngineOrder,
        disabledSearchEngines = uiState.disabledSearchEngines,
        enabledFileTypes = uiState.enabledFileTypes,
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        shortcutCodes = uiState.shortcutCodes,
        shortcutEnabled = uiState.shortcutEnabled,
        messagingApp = uiState.messagingApp,
        isWhatsAppInstalled = uiState.isWhatsAppInstalled,
        isTelegramInstalled = uiState.isTelegramInstalled,
        showWallpaperBackground = uiState.showWallpaperBackground,
        clearQueryAfterSearchEngine = uiState.clearQueryAfterSearchEngine,
        showAllResults = uiState.showAllResults,
        sortAppsByUsageEnabled = uiState.sortAppsByUsageEnabled,
        directDialEnabled = uiState.directDialEnabled,
        sectionOrder = uiState.sectionOrder,
        disabledSections = uiState.disabledSections,
        searchEngineSectionEnabled = uiState.searchEngineSectionEnabled,
        amazonDomain = uiState.amazonDomain,
        hasGeminiApiKey = uiState.hasGeminiApiKey,
        geminiApiKeyLast4 = uiState.geminiApiKeyLast4,
        personalContext = uiState.personalContext
    )
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userPreferences = remember { UserAppPreferences(context) }
    var shouldShowShortcutHint by remember(detailType) {
        mutableStateOf(
            if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                userPreferences.shouldShowShortcutHintBanner()
            } else {
                false
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
                }
                Lifecycle.Event.ON_RESUME -> {
                    shouldShowShortcutHint = userPreferences.shouldShowShortcutHintBanner()
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
    
    val callbacks = SettingsScreenCallbacks(
        onBack = onBack,
        onRemoveSuggestionExcludedApp = viewModel::unhideAppFromSuggestions,
        onRemoveResultExcludedApp = viewModel::unhideAppFromResults,
        onRemoveExcludedContact = viewModel::removeExcludedContact,
        onRemoveExcludedFile = viewModel::removeExcludedFile,
        onRemoveExcludedSetting = viewModel::removeExcludedSetting,
        onClearAllExclusions = viewModel::clearAllExclusions,
        onToggleSearchEngine = viewModel::setSearchEngineEnabled,
        onReorderSearchEngines = viewModel::reorderSearchEngines,
        onToggleFileType = viewModel::setFileTypeEnabled,
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        setShortcutCode = viewModel::setShortcutCode,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        onSetMessagingApp = viewModel::setMessagingApp,
        onToggleShowWallpaperBackground = viewModel::setShowWallpaperBackground,
        onToggleClearQueryAfterSearchEngine = viewModel::setClearQueryAfterSearchEngine,
        onToggleShowAllResults = viewModel::setShowAllResults,
        onToggleSortAppsByUsage = viewModel::setSortAppsByUsageEnabled,
        onToggleDirectDial = viewModel::setDirectDialEnabled,
        onToggleSection = { _, _ -> },
        onReorderSections = viewModel::reorderSections,
        onToggleSearchEngineSectionEnabled = viewModel::setSearchEngineSectionEnabled,
        onSetAmazonDomain = viewModel::setAmazonDomain,
        onSetGeminiApiKey = viewModel::setGeminiApiKey,
        onSetPersonalContext = viewModel::setPersonalContext,
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
                        "Unable to open settings",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )
    
    SettingsDetailScreen(
        modifier = modifier,
        state = state,
        callbacks = callbacks,
        detailType = detailType,
        showShortcutHintBanner = shouldShowShortcutHint,
        onDismissShortcutHintBanner = onDismissShortcutHint
    )
}

@Composable
private fun SettingsDetailScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType,
    showShortcutHintBanner: Boolean = false,
    onDismissShortcutHintBanner: () -> Unit = {}
) {
    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    
    // Navigate back to settings when all excluded items are cleared
    val hasExcludedItems = state.suggestionExcludedApps.isNotEmpty() || 
                           state.resultExcludedApps.isNotEmpty() ||
                           state.excludedContacts.isNotEmpty() || 
                           state.excludedFiles.isNotEmpty() ||
                           state.excludedSettings.isNotEmpty()
    LaunchedEffect(hasExcludedItems) {
        if (detailType == SettingsDetailType.EXCLUDED_ITEMS && !hasExcludedItems) {
            callbacks.onBack()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsDetailHeader(
                title = when (detailType) {
                    SettingsDetailType.SEARCH_ENGINES -> stringResource(R.string.settings_search_engines_title)
                    SettingsDetailType.EXCLUDED_ITEMS -> stringResource(R.string.settings_excluded_items_title)
                    SettingsDetailType.ADDITIONAL_SETTINGS -> stringResource(R.string.settings_additional_settings_title)
                },
                onBack = callbacks.onBack,
                trailingContent = if (detailType == SettingsDetailType.SEARCH_ENGINES) {
                    {
                        Switch(
                            checked = state.searchEngineSectionEnabled,
                            onCheckedChange = callbacks.onToggleSearchEngineSectionEnabled
                        )
                    }
                } else {
                    null
                }
            )

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        start = SettingsSpacing.contentHorizontalPadding,
                        end = SettingsSpacing.contentHorizontalPadding,
                        bottom = SettingsSpacing.sectionTopPadding
                    )
            ) {
                when (detailType) {
                    SettingsDetailType.SEARCH_ENGINES -> {
                        // Search Engine Section (includes shortcuts)
                        SearchEnginesSection(
                            searchEngineOrder = state.searchEngineOrder,
                            disabledSearchEngines = state.disabledSearchEngines,
                            onToggleSearchEngine = callbacks.onToggleSearchEngine,
                            onReorderSearchEngines = callbacks.onReorderSearchEngines,
                            shortcutCodes = state.shortcutCodes,
                            setShortcutCode = callbacks.setShortcutCode,
                            shortcutEnabled = state.shortcutEnabled,
                            setShortcutEnabled = callbacks.setShortcutEnabled,
                            searchEngineSectionEnabled = state.searchEngineSectionEnabled,
                            amazonDomain = state.amazonDomain,
                            onSetAmazonDomain = callbacks.onSetAmazonDomain,
                            onSetGeminiApiKey = callbacks.onSetGeminiApiKey,
                            geminiApiKeyLast4 = state.geminiApiKeyLast4,
                            personalContext = state.personalContext,
                            onSetPersonalContext = callbacks.onSetPersonalContext,
                        DirectSearchAvailable = state.hasGeminiApiKey,
                        showTitle = false,
                        showShortcutHintBanner = showShortcutHintBanner,
                        onDismissShortcutHintBanner = onDismissShortcutHintBanner
                        )
                    }
                    SettingsDetailType.EXCLUDED_ITEMS -> {
                        // Excluded Items Section
                        ExcludedItemsSection(
                            suggestionExcludedApps = state.suggestionExcludedApps,
                            resultExcludedApps = state.resultExcludedApps,
                            excludedContacts = state.excludedContacts,
                            excludedFiles = state.excludedFiles,
                            excludedSettings = state.excludedSettings,
                            onRemoveSuggestionExcludedApp = callbacks.onRemoveSuggestionExcludedApp,
                            onRemoveResultExcludedApp = callbacks.onRemoveResultExcludedApp,
                            onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                            onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                            onRemoveExcludedSetting = callbacks.onRemoveExcludedSetting,
                            onClearAll = callbacks.onClearAllExclusions,
                            showTitle = false
                        )
                    }
                    SettingsDetailType.ADDITIONAL_SETTINGS -> {
                        // Additional Settings Section
                        AdditionalSettingsSection(
                            clearQueryAfterSearchEngine = state.clearQueryAfterSearchEngine,
                            onToggleClearQueryAfterSearchEngine = callbacks.onToggleClearQueryAfterSearchEngine,
                            showAllResults = state.showAllResults,
                            onToggleShowAllResults = callbacks.onToggleShowAllResults,
                            sortAppsByUsageEnabled = state.sortAppsByUsageEnabled,
                            onToggleSortAppsByUsage = callbacks.onToggleSortAppsByUsage,
                            onSetDefaultAssistant = callbacks.onSetDefaultAssistant,
                            showTitle = false
                        )
                    }
                }
            }
        }
        
        // Floating Action Button for Clear All (only shown for Excluded Items)
        if (detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            FloatingActionButton(
                onClick = { showClearAllConfirmation = true },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.settings_action_clear_all),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Clear All confirmation dialog
        if (showClearAllConfirmation && detailType == SettingsDetailType.EXCLUDED_ITEMS) {
            ClearAllConfirmationDialog(
                onConfirm = {
                    callbacks.onClearAllExclusions()
                    showClearAllConfirmation = false
                },
                onDismiss = { showClearAllConfirmation = false }
            )
        }
    }
}

/**
 * Header component for the settings detail screen.
 */
@Composable
private fun SettingsDetailHeader(
    title: String,
    onBack: () -> Unit,
    trailingContent: (@Composable (() -> Unit))? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.headerHorizontalPadding,
                vertical = SettingsSpacing.headerVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_navigate_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(SettingsSpacing.headerIconSpacing))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        trailingContent?.let {
            Spacer(modifier = Modifier.width(SettingsSpacing.headerIconSpacing))
            it()
        }
    }
}

/**
 * Enum to represent different types of settings detail screens.
 */
enum class SettingsDetailType {
    SEARCH_ENGINES,
    EXCLUDED_ITEMS,
    ADDITIONAL_SETTINGS
}

