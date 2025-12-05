package com.tk.quicksearch.settings

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
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
        hiddenApps = uiState.hiddenApps,
        excludedContacts = uiState.excludedContacts,
        excludedFiles = uiState.excludedFiles,
        showAppLabels = uiState.showAppLabels,
        searchEngineOrder = uiState.searchEngineOrder,
        disabledSearchEngines = uiState.disabledSearchEngines,
        enabledFileTypes = uiState.enabledFileTypes,
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        shortcutCodes = uiState.shortcutCodes,
        shortcutEnabled = uiState.shortcutEnabled,
        messagingApp = uiState.messagingApp,
        showSectionTitles = uiState.showSectionTitles,
        showWallpaperBackground = uiState.showWallpaperBackground,
        sectionOrder = uiState.sectionOrder,
        disabledSections = uiState.disabledSections,
        searchEngineSectionEnabled = uiState.searchEngineSectionEnabled,
        shortcutsEnabled = uiState.shortcutsEnabled,
        amazonDomain = uiState.amazonDomain
    )
    
    val callbacks = SettingsScreenCallbacks(
        onBack = onBack,
        onRemoveExcludedApp = viewModel::unhideApp,
        onRemoveExcludedContact = viewModel::removeExcludedContact,
        onRemoveExcludedFile = viewModel::removeExcludedFile,
        onClearAllExclusions = viewModel::clearAllExclusions,
        onToggleAppLabels = viewModel::setShowAppLabels,
        onToggleSearchEngine = viewModel::setSearchEngineEnabled,
        onReorderSearchEngines = viewModel::reorderSearchEngines,
        onToggleFileType = viewModel::setFileTypeEnabled,
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        setShortcutCode = viewModel::setShortcutCode,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        onSetMessagingApp = viewModel::setMessagingApp,
        onToggleShowSectionTitles = viewModel::setShowSectionTitles,
        onToggleShowWallpaperBackground = viewModel::setShowWallpaperBackground,
        onToggleSection = { _, _ -> },
        onReorderSections = viewModel::reorderSections,
        onToggleSearchEngineSectionEnabled = viewModel::setSearchEngineSectionEnabled,
        onToggleShortcutsEnabled = viewModel::setShortcutsEnabled,
        onSetAmazonDomain = viewModel::setAmazonDomain
    )
    
    SettingsDetailScreen(
        modifier = modifier,
        state = state,
        callbacks = callbacks,
        detailType = detailType
    )
}

@Composable
private fun SettingsDetailScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    detailType: SettingsDetailType
) {
    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
    var showClearAllConfirmation by remember { mutableStateOf(false) }
    
    // Navigate back to settings when all excluded items are cleared
    val hasExcludedItems = state.hiddenApps.isNotEmpty() || 
                           state.excludedContacts.isNotEmpty() || 
                           state.excludedFiles.isNotEmpty()
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
                },
                onBack = callbacks.onBack
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
                            onToggleSearchEngineSectionEnabled = callbacks.onToggleSearchEngineSectionEnabled,
                            shortcutsEnabled = state.shortcutsEnabled,
                            onToggleShortcutsEnabled = callbacks.onToggleShortcutsEnabled,
                            amazonDomain = state.amazonDomain,
                            onSetAmazonDomain = callbacks.onSetAmazonDomain,
                            showTitle = false
                        )
                    }
                    SettingsDetailType.EXCLUDED_ITEMS -> {
                        // Excluded Items Section
                        ExcludedItemsSection(
                            hiddenApps = state.hiddenApps,
                            excludedContacts = state.excludedContacts,
                            excludedFiles = state.excludedFiles,
                            onRemoveExcludedApp = callbacks.onRemoveExcludedApp,
                            onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                            onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                            onClearAll = callbacks.onClearAllExclusions,
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
    onBack: () -> Unit
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Enum to represent different types of settings detail screens.
 */
enum class SettingsDetailType {
    SEARCH_ENGINES,
    EXCLUDED_ITEMS
}

