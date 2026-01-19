package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.SettingsSpacing
import com.tk.quicksearch.search.directSearch.SearchEngineToggleCard
import com.tk.quicksearch.settings.searchEnginesScreen.SearchEngineListCard
import com.tk.quicksearch.settings.searchEnginesScreen.ShortcutsSection
import com.tk.quicksearch.search.core.*

/**
 * Main section for configuring search engines.
 * Allows reordering, enabling/disabling, and managing shortcuts.
 */
@Composable
fun SearchEnginesSection(
    searchEngineOrder: List<SearchTarget>,
    disabledSearchEngines: Set<String>,
    onToggleSearchEngine: (SearchTarget, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchTarget>) -> Unit,
    shortcutCodes: Map<String, String> = emptyMap(),
    setShortcutCode: ((SearchTarget, String) -> Unit)? = null,
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    setShortcutEnabled: ((SearchTarget, Boolean) -> Unit)? = null,
    isSearchEngineCompactMode: Boolean = true,
    onToggleSearchEngineCompactMode: ((Boolean) -> Unit)? = null,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    onSetGeminiApiKey: ((String?) -> Unit)? = null,
    geminiApiKeyLast4: String? = null,
    personalContext: String = "",
    onSetPersonalContext: ((String?) -> Unit)? = null,
    showTitle: Boolean = true,
    directSearchAvailable: Boolean = false,
    showShortcutHintBanner: Boolean = false,
    onDismissShortcutHintBanner: (() -> Unit)? = null,
    directSearchSetupExpanded: Boolean = true,
    onToggleDirectSearchSetupExpanded: (() -> Unit)? = null,
    showRequestSearchEngine: Boolean = true,
    showDirectSearchAtTop: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_search_engines_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = SettingsSpacing.sectionTitleBottomPadding)
        )

        Text(
            text = stringResource(R.string.settings_search_engines_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = SettingsSpacing.sectionDescriptionBottomPadding)
        )
    }

    // Show Direct Search card at top or bottom based on the showDirectSearchAtTop parameter

    if (showDirectSearchAtTop && onSetGeminiApiKey != null) {
        SearchEngineToggleCard(
            directSearchEnabled = directSearchAvailable,
            onSetGeminiApiKey = onSetGeminiApiKey,
            geminiApiKeyLast4 = geminiApiKeyLast4,
            personalContext = personalContext,
            onSetPersonalContext = onSetPersonalContext,
            isExpanded = directSearchSetupExpanded,
            onToggleExpanded = onToggleDirectSearchSetupExpanded
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

    if (showShortcutHintBanner && onDismissShortcutHintBanner != null) {
        ShortcutHintBanner(
            onDismiss = onDismissShortcutHintBanner,
            modifier = Modifier.padding(bottom = 18.dp)
        )
    }

    val enginesToDisplay = if (directSearchAvailable) {
        searchEngineOrder
    } else {
        searchEngineOrder.filterNot {
            it is SearchTarget.Engine && it.engine == SearchEngine.DIRECT_SEARCH
        }
    }
    SearchEngineListCard(
        searchEngineOrder = enginesToDisplay,
        disabledSearchEngines = disabledSearchEngines,
        onToggleSearchEngine = onToggleSearchEngine,
        onReorderSearchEngines = onReorderSearchEngines,
        shortcutCodes = shortcutCodes,
        setShortcutCode = setShortcutCode,
        shortcutEnabled = shortcutEnabled,
        setShortcutEnabled = setShortcutEnabled,
        isSearchEngineCompactMode = isSearchEngineCompactMode,
        amazonDomain = amazonDomain,
        onSetAmazonDomain = onSetAmazonDomain,
        showRequestSearchEngine = showRequestSearchEngine
    )

    // Show Direct Search card at bottom if not shown at top
    if (!showDirectSearchAtTop && onSetGeminiApiKey != null) {
        Spacer(modifier = Modifier.height(16.dp))
        SearchEngineToggleCard(
            directSearchEnabled = directSearchAvailable,
            onSetGeminiApiKey = onSetGeminiApiKey,
            geminiApiKeyLast4 = geminiApiKeyLast4,
            personalContext = personalContext,
            onSetPersonalContext = onSetPersonalContext,
            isExpanded = directSearchSetupExpanded,
            onToggleExpanded = onToggleDirectSearchSetupExpanded
        )
    }

}

@Composable
private fun ShortcutHintBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_shortcuts_hint_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.desc_close),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
