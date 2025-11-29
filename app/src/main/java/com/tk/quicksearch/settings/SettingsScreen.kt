package com.tk.quicksearch.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
        onUnhideApp = viewModel::unhideApp,
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
        shortcutsEnabled = uiState.shortcutsEnabled,
        onToggleShortcutsEnabled = viewModel::setShortcutsEnabled,
        shortcutCodes = uiState.shortcutCodes,
        setShortcutCode = viewModel::setShortcutCode,
        shortcutEnabled = uiState.shortcutEnabled,
        setShortcutEnabled = viewModel::setShortcutEnabled,
        useWhatsAppForMessages = uiState.useWhatsAppForMessages,
        onToggleUseWhatsAppForMessages = viewModel::setUseWhatsAppForMessages
    )
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    hiddenApps: List<AppInfo>,
    onUnhideApp: (AppInfo) -> Unit,
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
    shortcutsEnabled: Boolean,
    onToggleShortcutsEnabled: (Boolean) -> Unit,
    shortcutCodes: Map<SearchEngine, String>,
    setShortcutCode: (SearchEngine, String) -> Unit,
    shortcutEnabled: Map<SearchEngine, Boolean>,
    setShortcutEnabled: (SearchEngine, Boolean) -> Unit,
    useWhatsAppForMessages: Boolean,
    onToggleUseWhatsAppForMessages: (Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        // Search Engine Section
        SearchEnginesSection(
            searchEngineOrder = searchEngineOrder,
            disabledSearchEngines = disabledSearchEngines,
            onToggleSearchEngine = onToggleSearchEngine,
            onReorderSearchEngines = onReorderSearchEngines
        )

        ShortcutsSection(
            shortcutsEnabled = shortcutsEnabled,
            onToggleShortcutsEnabled = onToggleShortcutsEnabled,
            shortcutCodes = shortcutCodes,
            setShortcutCode = setShortcutCode,
            shortcutEnabled = shortcutEnabled,
            setShortcutEnabled = setShortcutEnabled
        )

        // Apps Section
        AppLabelsSection(
            showAppLabels = showAppLabels,
            onToggleAppLabels = onToggleAppLabels
        )

        LayoutSection(
            keyboardAlignedLayout = keyboardAlignedLayout,
            onToggleKeyboardAlignedLayout = onToggleKeyboardAlignedLayout
        )

        HiddenAppsSection(
            hiddenApps = hiddenApps,
            onUnhideApp = onUnhideApp
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
    }
}


