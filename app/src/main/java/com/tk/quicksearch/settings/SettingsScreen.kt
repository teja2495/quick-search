package com.tk.quicksearch.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.SearchViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: SearchViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        modifier = modifier,
        onBack = onBack,
        hiddenApps = uiState.hiddenApps,
        excludedContacts = uiState.excludedContacts,
        excludedFiles = uiState.excludedFiles,
        onRemoveExcludedApp = viewModel::unhideApp,
        onRemoveExcludedContact = viewModel::removeExcludedContact,
        onRemoveExcludedFile = viewModel::removeExcludedFile,
        onClearAllExclusions = viewModel::clearAllExclusions,
        showAppLabels = uiState.showAppLabels,
        onToggleAppLabels = viewModel::setShowAppLabels,
        searchEngineOrder = uiState.searchEngineOrder,
        disabledSearchEngines = uiState.disabledSearchEngines,
        onToggleSearchEngine = viewModel::setSearchEngineEnabled,
        onReorderSearchEngines = viewModel::reorderSearchEngines,
        enabledFileTypes = uiState.enabledFileTypes,
        onToggleFileType = viewModel::setFileTypeEnabled,
        keyboardAlignedLayout = uiState.keyboardAlignedLayout,
        onToggleKeyboardAlignedLayout = viewModel::setKeyboardAlignedLayout,
        shortcutCodes = uiState.shortcutCodes,
        setShortcutCode = viewModel::setShortcutCode,
        shortcutEnabled = uiState.shortcutEnabled,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        useWhatsAppForMessages = uiState.useWhatsAppForMessages,
        onToggleUseWhatsAppForMessages = viewModel::setUseWhatsAppForMessages,
        showSectionTitles = uiState.showSectionTitles,
        onToggleShowSectionTitles = viewModel::setShowSectionTitles
    )
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    hiddenApps: List<AppInfo>,
    excludedContacts: List<com.tk.quicksearch.model.ContactInfo>,
    excludedFiles: List<com.tk.quicksearch.model.DeviceFile>,
    onRemoveExcludedApp: (AppInfo) -> Unit,
    onRemoveExcludedContact: (com.tk.quicksearch.model.ContactInfo) -> Unit,
    onRemoveExcludedFile: (com.tk.quicksearch.model.DeviceFile) -> Unit,
    onClearAllExclusions: () -> Unit,
    showAppLabels: Boolean,
    onToggleAppLabels: (Boolean) -> Unit,
    searchEngineOrder: List<SearchEngine>,
    disabledSearchEngines: Set<SearchEngine>,
    onToggleSearchEngine: (SearchEngine, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchEngine>) -> Unit,
    enabledFileTypes: Set<FileType>,
    onToggleFileType: (FileType, Boolean) -> Unit,
    keyboardAlignedLayout: Boolean,
    onToggleKeyboardAlignedLayout: (Boolean) -> Unit,
    shortcutCodes: Map<SearchEngine, String>,
    setShortcutCode: (SearchEngine, String) -> Unit,
    shortcutEnabled: Map<SearchEngine, Boolean>,
    setShortcutEnabled: (SearchEngine, Boolean) -> Unit,
    useWhatsAppForMessages: Boolean,
    onToggleUseWhatsAppForMessages: (Boolean) -> Unit,
    showSectionTitles: Boolean,
    onToggleShowSectionTitles: (Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Fixed Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.desc_navigate_back),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            // Appearance Section
            AppLabelsSection(
                showAppLabels = showAppLabels,
                onToggleAppLabels = onToggleAppLabels,
                keyboardAlignedLayout = keyboardAlignedLayout,
                onToggleKeyboardAlignedLayout = onToggleKeyboardAlignedLayout,
                showSectionTitles = showSectionTitles,
                onToggleShowSectionTitles = onToggleShowSectionTitles
            )

            // Contacts Section
            MessagingSection(
                useWhatsAppForMessages = useWhatsAppForMessages,
                onToggleUseWhatsAppForMessages = onToggleUseWhatsAppForMessages
            )

            // Files Section
            FileTypesSection(
                enabledFileTypes = enabledFileTypes,
                onToggleFileType = onToggleFileType
            )

            // Search Engine Section (includes shortcuts)
            SearchEnginesSection(
                searchEngineOrder = searchEngineOrder,
                disabledSearchEngines = disabledSearchEngines,
                onToggleSearchEngine = onToggleSearchEngine,
                onReorderSearchEngines = onReorderSearchEngines,
                shortcutCodes = shortcutCodes,
                setShortcutCode = setShortcutCode,
                shortcutEnabled = shortcutEnabled,
                setShortcutEnabled = setShortcutEnabled
            )
            
            // Excluded Items Section (at the bottom)
            ExcludedItemsSection(
                hiddenApps = hiddenApps,
                excludedContacts = excludedContacts,
                excludedFiles = excludedFiles,
                onRemoveExcludedApp = onRemoveExcludedApp,
                onRemoveExcludedContact = onRemoveExcludedContact,
                onRemoveExcludedFile = onRemoveExcludedFile,
                onClearAll = onClearAllExclusions
            )

            // App Version
            Spacer(modifier = Modifier.height(32.dp))
            val context = LocalContext.current
            val versionName = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                null
            }
            Text(
                text = stringResource(R.string.settings_app_version, versionName ?: "Unknown"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 100.dp, top = 45.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}


