package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.tools.directSearch.GeminiTextModel
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Main feature for configuring search engines.
 * Allows reordering, enabling/disabling, and managing aliases.
 */
@Composable
fun SearchEngines(
    searchEngineOrder: List<SearchTarget>,
    disabledSearchEngines: Set<String>,
    disabledSearchEnginesExpanded: Boolean = true,
    onToggleDisabledSearchEnginesExpanded: (() -> Unit)? = null,
    onToggleSearchEngine: (SearchTarget, Boolean) -> Unit,
    onReorderSearchEngines: (List<SearchTarget>) -> Unit,
    shortcutCodes: Map<String, String> = emptyMap(),
    setAliasCode: ((SearchTarget, String) -> Unit)? = null,
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    setAliasEnabled: ((SearchTarget, Boolean) -> Unit)? = null,
    isSearchEngineCompactMode: Boolean = true,
    onToggleSearchEngineCompactMode: ((Boolean) -> Unit)? = null,
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    onSetGeminiApiKey: ((String?) -> Unit)? = null,
    geminiApiKeyLast4: String? = null,
    personalContext: String = "",
    onSetPersonalContext: ((String?) -> Unit)? = null,
    geminiModel: String,
    geminiGroundingEnabled: Boolean,
    availableGeminiModels: List<GeminiTextModel>,
    onSetGeminiModel: ((String?) -> Unit)? = null,
    onSetGeminiGroundingEnabled: ((Boolean) -> Unit)? = null,
    onRefreshAvailableGeminiModels: (() -> Unit)? = null,
    onOpenDirectSearchConfigure: (() -> Unit)? = null,
    showTitle: Boolean = true,
    directSearchAvailable: Boolean = false,
    directSearchSetupExpanded: Boolean = true,
    onToggleDirectSearchSetupExpanded: (() -> Unit)? = null,
    showAddSearchEngineButton: Boolean = true,
    onAddCustomSearchEngine: ((String, String, String) -> Unit)? = null,
    onUpdateCustomSearchEngine: ((String, String, String, String?) -> Unit)? = null,
    onDeleteCustomSearchEngine: ((String) -> Unit)? = null,
    showDirectSearchAtTop: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hasEnabledEngines =
        searchEngineOrder.any { searchTarget ->
            searchTarget.getId() !in disabledSearchEngines &&
                (directSearchAvailable ||
                    searchTarget !is SearchTarget.Engine ||
                    searchTarget.engine != SearchEngine.DIRECT_SEARCH)
        }

    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_search_engines_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding),
        )

        if (hasEnabledEngines) {
            Text(
                text = stringResource(R.string.settings_search_engines_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = DesignTokens.SectionDescriptionBottomPadding),
            )
        }
    }

    // Show Direct Search card at top or bottom based on the showDirectSearchAtTop parameter

    if (showDirectSearchAtTop && onSetGeminiApiKey != null) {
        DirectSearchSetupCard(
            directSearchEnabled = directSearchAvailable,
            onSetGeminiApiKey = onSetGeminiApiKey,
            geminiApiKeyLast4 = geminiApiKeyLast4,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
            isExpanded = directSearchSetupExpanded,
            onToggleExpanded = onToggleDirectSearchSetupExpanded,
        )
        Spacer(modifier = Modifier.height(6.dp))
    }

    val enginesToDisplay =
        if (directSearchAvailable) {
            searchEngineOrder
        } else {
            searchEngineOrder.filterNot {
                it is SearchTarget.Engine && it.engine == SearchEngine.DIRECT_SEARCH
            }
        }
    SearchEngineListCard(
        searchEngineOrder = enginesToDisplay,
        disabledSearchEngines = disabledSearchEngines,
        disabledSearchEnginesExpanded = disabledSearchEnginesExpanded,
        onToggleDisabledSearchEnginesExpanded = onToggleDisabledSearchEnginesExpanded,
        onToggleSearchEngine = onToggleSearchEngine,
        onReorderSearchEngines = onReorderSearchEngines,
        shortcutCodes = shortcutCodes,
        setAliasCode = setAliasCode,
        shortcutEnabled = shortcutEnabled,
        setAliasEnabled = setAliasEnabled,
        isSearchEngineCompactMode = isSearchEngineCompactMode,
        amazonDomain = amazonDomain,
        onSetAmazonDomain = onSetAmazonDomain,
        showAddSearchEngineButton = showAddSearchEngineButton,
        onAddCustomSearchEngine = onAddCustomSearchEngine,
        onUpdateCustomSearchEngine = onUpdateCustomSearchEngine,
        onDeleteCustomSearchEngine = onDeleteCustomSearchEngine,
    )

    // Show Direct Search card at bottom if not shown at top
    if (!showDirectSearchAtTop && onSetGeminiApiKey != null) {
        Spacer(modifier = Modifier.height(16.dp))
        DirectSearchSetupCard(
            directSearchEnabled = directSearchAvailable,
            onSetGeminiApiKey = onSetGeminiApiKey,
            geminiApiKeyLast4 = geminiApiKeyLast4,
            onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
            isExpanded = directSearchSetupExpanded,
            onToggleExpanded = onToggleDirectSearchSetupExpanded,
        )
    }
}
