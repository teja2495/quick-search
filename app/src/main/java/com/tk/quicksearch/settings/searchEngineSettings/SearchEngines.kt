package com.tk.quicksearch.settings.searchEngineSettings

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNestedCheckbox
import com.tk.quicksearch.shared.ui.components.SettingsRevealVisibility
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.shared.ui.components.SegmentedTileToggleHorizontalPadding
import com.tk.quicksearch.searchEngines.SearchEngine
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.searchEngines.isInAppBrowserPackage
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
    amazonDomain: String? = null,
    onSetAmazonDomain: ((String?) -> Unit)? = null,
    onOpenAiSearchConfigure: (() -> Unit)? = null,
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onToggleSearchEngineAliasSuffixEnabled: ((Boolean) -> Unit)? = null,
    isAliasTriggerAfterSpaceEnabled: Boolean = true,
    onToggleAliasTriggerAfterSpaceEnabled: ((Boolean) -> Unit)? = null,
    showTitle: Boolean = true,
    aiSearchAvailable: Boolean = false,
    showAddSearchEngineButton: Boolean = true,
    onAddCustomSearchEngine: ((String, String, String, String?) -> Unit)? = null,
    onUpdateCustomSearchEngine: ((String, String, String, String?, String?) -> Unit)? = null,
    onDeleteCustomSearchEngine: ((String) -> Unit)? = null,
    showAiSearchAtTop: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hasEnabledEngines =
        searchEngineOrder.any { searchTarget ->
            searchTarget.getId() !in disabledSearchEngines &&
                (aiSearchAvailable ||
                    searchTarget !is SearchTarget.Engine ||
                    searchTarget.engine != SearchEngine.DIRECT_SEARCH)
        }

    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_app_shortcuts_filter_search_engines),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding).semantics { heading() },
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

    if (showAiSearchAtTop && onToggleSearchEngineAliasSuffixEnabled != null) {
        SearchEngineAliasSuffixCard(
            enabled = isSearchEngineAliasSuffixEnabled,
            onToggle = onToggleSearchEngineAliasSuffixEnabled,
            triggerAfterSpaceEnabled = isAliasTriggerAfterSpaceEnabled,
            onToggleTriggerAfterSpace = onToggleAliasTriggerAfterSpaceEnabled,
        )
        Spacer(modifier = Modifier.height(DesignTokens.SpacingSmall))
    }

    if (!aiSearchAvailable && onOpenAiSearchConfigure != null) {
        Box(modifier = Modifier.padding(bottom = DesignTokens.SectionTopPadding)) {
            SettingsSegmentNavigationRow(
                item = SettingsCardItem(
                    title = stringResource(R.string.settings_direct_search_setup_nav_title),
                    description = stringResource(R.string.settings_direct_search_setup_nav_desc),
                    iconResId = R.drawable.direct_search,
                    actionOnPress = onOpenAiSearchConfigure,
                ),
                index = 0,
                count = 1,
            )
        }
    }

    val enginesToDisplay =
        (if (aiSearchAvailable) {
            searchEngineOrder
        } else {
            searchEngineOrder.filterNot {
                it is SearchTarget.Engine && it.engine == SearchEngine.DIRECT_SEARCH
            }
        }).filterNot {
            it is SearchTarget.Browser && isInAppBrowserPackage(it.app.packageName)
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
        isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
        amazonDomain = amazonDomain,
        onSetAmazonDomain = onSetAmazonDomain,
        showAddSearchEngineButton = showAddSearchEngineButton,
        onAddCustomSearchEngine = onAddCustomSearchEngine,
        onUpdateCustomSearchEngine = onUpdateCustomSearchEngine,
        onDeleteCustomSearchEngine = onDeleteCustomSearchEngine,
    )

    if (!showAiSearchAtTop && onToggleSearchEngineAliasSuffixEnabled != null) {
        Spacer(modifier = Modifier.height(DesignTokens.SpacingLarge))
        SearchEngineAliasSuffixCard(
            enabled = isSearchEngineAliasSuffixEnabled,
            onToggle = onToggleSearchEngineAliasSuffixEnabled,
            triggerAfterSpaceEnabled = isAliasTriggerAfterSpaceEnabled,
            onToggleTriggerAfterSpace = onToggleAliasTriggerAfterSpaceEnabled,
        )
    }
}

@Composable
private fun SearchEngineAliasSuffixCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    triggerAfterSpaceEnabled: Boolean = true,
    onToggleTriggerAfterSpace: ((Boolean) -> Unit)? = null,
) {
    // One tile: the switch row, with its trigger-after-space option nested below while it is on.
    SegmentedTile(
        index = 0,
        count = 1,
        modifier = Modifier.padding(bottom = DesignTokens.SpacingMedium),
    ) {
        Column {
            SettingsToggleRow(
                title = stringResource(R.string.settings_search_engine_alias_suffix_title),
                subtitle = stringResource(R.string.settings_search_engine_alias_suffix_desc),
                checked = enabled,
                onCheckedChange = onToggle,
                showDivider = false,
            )
            SettingsRevealVisibility(visible = enabled && onToggleTriggerAfterSpace != null) {
                SettingsNestedCheckbox(
                    label = stringResource(R.string.alias_trigger_after_space),
                    checked = triggerAfterSpaceEnabled,
                    onCheckedChange = { onToggleTriggerAfterSpace?.invoke(it) },
                    modifier =
                        Modifier
                            .padding(
                                start = SegmentedTileToggleHorizontalPadding,
                                end = SegmentedTileToggleHorizontalPadding,
                                bottom = SegmentedTileContentPadding,
                            ),
                )
            }
        }
    }
}
