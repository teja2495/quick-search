package com.tk.quicksearch.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.search.SearchSection
import com.tk.quicksearch.search.SearchViewModel
import com.tk.quicksearch.ui.theme.ThemeMode

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Handle section toggle with permission check
    val onToggleSection = rememberSectionToggleHandler(viewModel, uiState.disabledSections)
    
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
        useWhatsAppForMessages = uiState.useWhatsAppForMessages,
        showSectionTitles = uiState.showSectionTitles,
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
        onToggleUseWhatsAppForMessages = viewModel::setUseWhatsAppForMessages,
        onToggleShowSectionTitles = viewModel::setShowSectionTitles,
        onToggleSection = onToggleSection,
        onReorderSections = viewModel::reorderSections,
        onToggleSearchEngineSectionEnabled = viewModel::setSearchEngineSectionEnabled,
        onToggleShortcutsEnabled = viewModel::setShortcutsEnabled,
        onSetAmazonDomain = viewModel::setAmazonDomain
    )
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val userPreferences = remember { UserAppPreferences(context) }
    var currentThemeMode by remember { mutableStateOf(ThemeMode.fromString(userPreferences.getThemeMode())) }
    
    SettingsScreen(
        modifier = modifier,
        state = state,
        callbacks = callbacks,
        currentThemeMode = currentThemeMode,
        onThemeModeChange = { themeMode ->
            userPreferences.setThemeMode(themeMode.value)
            currentThemeMode = themeMode
            onThemeModeChange(themeMode)
        }
    )
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    BackHandler(onBack = callbacks.onBack)
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        SettingsHeader(onBack = callbacks.onBack)

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = SettingsSpacing.contentHorizontalPadding)
        ) {
            // Search Sections Section
            SectionSettingsSection(
                sectionOrder = state.sectionOrder,
                disabledSections = state.disabledSections,
                onToggleSection = callbacks.onToggleSection,
                onReorderSections = callbacks.onReorderSections
            )

            // Appearance Section
            AppLabelsSection(
                showAppLabels = state.showAppLabels,
                onToggleAppLabels = callbacks.onToggleAppLabels,
                keyboardAlignedLayout = state.keyboardAlignedLayout,
                onToggleKeyboardAlignedLayout = callbacks.onToggleKeyboardAlignedLayout,
                showSectionTitles = state.showSectionTitles,
                onToggleShowSectionTitles = callbacks.onToggleShowSectionTitles,
                appsSectionEnabled = SearchSection.APPS !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Theme Section
            ThemeSection(
                themeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Contacts Section
            MessagingSection(
                useWhatsAppForMessages = state.useWhatsAppForMessages,
                onToggleUseWhatsAppForMessages = callbacks.onToggleUseWhatsAppForMessages,
                contactsSectionEnabled = SearchSection.CONTACTS !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

            // Files Section
            FileTypesSection(
                enabledFileTypes = state.enabledFileTypes,
                onToggleFileType = callbacks.onToggleFileType,
                filesSectionEnabled = SearchSection.FILES !in state.disabledSections,
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )

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
                modifier = Modifier.padding(top = SettingsSpacing.sectionTopPadding)
            )
            
            // Excluded Items Section (at the bottom)
            ExcludedItemsSection(
                hiddenApps = state.hiddenApps,
                excludedContacts = state.excludedContacts,
                excludedFiles = state.excludedFiles,
                onRemoveExcludedApp = callbacks.onRemoveExcludedApp,
                onRemoveExcludedContact = callbacks.onRemoveExcludedContact,
                onRemoveExcludedFile = callbacks.onRemoveExcludedFile,
                onClearAll = callbacks.onClearAllExclusions
            )

            // App Version
            SettingsVersionDisplay()
        }
    }
}

/**
 * Header component for the settings screen.
 */
@Composable
private fun SettingsHeader(onBack: () -> Unit) {
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
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Displays the app version at the bottom of the settings screen.
 */
@Composable
private fun SettingsVersionDisplay() {
    Spacer(modifier = Modifier.height(32.dp))
    val versionName = getAppVersionName()
    Text(
        text = stringResource(R.string.settings_app_version, versionName ?: "Unknown"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = SettingsSpacing.versionBottomPadding,
                top = SettingsSpacing.versionTopPadding
            ),
        textAlign = TextAlign.Center
    )
}


