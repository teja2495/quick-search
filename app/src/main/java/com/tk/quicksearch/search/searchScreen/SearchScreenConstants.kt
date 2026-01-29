package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.shortcutKey
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.util.getAppGridColumns

/** Enum representing which section is currently expanded. */
enum class ExpandedSection {
    NONE,
    APP_SHORTCUTS,
    CONTACTS,
    FILES,
    SETTINGS,
}

/** Constants for search screen layout. */
internal object SearchScreenConstants {
    const val INITIAL_RESULT_COUNT = 1
    const val ROW_COUNT = 2
    const val SEARCH_ROW_COUNT = 1
    const val COLUMNS = 5
    val EXPANDED_CARD_MAX_HEIGHT = 600.dp
}

/** Data class holding all derived state calculations. */
internal data class DerivedState(
    val isSearching: Boolean,
    val hasPinnedContacts: Boolean,
    val hasPinnedFiles: Boolean,
    val hasPinnedSettings: Boolean,
    val hasPinnedAppShortcuts: Boolean,
    val visibleRowCount: Int,
    val visibleAppLimit: Int,
    val displayApps: List<AppInfo>,
    val pinnedPackageNames: Set<String>,
    val pinnedSettingIds: Set<String>,
    val pinnedAppShortcutIds: Set<String>,
    val hasAppResults: Boolean,
    val hasContactResults: Boolean,
    val hasFileResults: Boolean,
    val hasSettingResults: Boolean,
    val hasAppShortcutResults: Boolean,
    val pinnedContactIds: Set<Long>,
    val pinnedFileUris: Set<String>,
    val hasMultipleExpandableSections: Boolean,
    val orderedSections: List<SearchSection>,
    val shouldShowApps: Boolean,
    val shouldShowContacts: Boolean,
    val shouldShowFiles: Boolean,
    val shouldShowSettings: Boolean,
    val shouldShowAppShortcuts: Boolean,
)

/** Calculates all derived state from SearchUiState. */
@Composable
internal fun rememberDerivedState(state: SearchUiState): DerivedState {
    val isSearching = state.query.isNotBlank()
    val hasPinnedContacts = state.pinnedContacts.isNotEmpty() && state.hasContactPermission
    val hasPinnedFiles = state.pinnedFiles.isNotEmpty() && state.hasFilePermission
    val hasPinnedSettings = state.pinnedSettings.isNotEmpty()
    val hasPinnedAppShortcuts = state.pinnedAppShortcuts.isNotEmpty()
    val columns = getAppGridColumns()
    val visibleRowCount =
        if (isSearching ||
            hasPinnedContacts ||
            hasPinnedFiles ||
            hasPinnedSettings ||
            hasPinnedAppShortcuts ||
            (
                !state.query.isNotBlank() &&
                    state.recentQueriesEnabled &&
                    state.recentItems.isNotEmpty()
            )
        ) {
            SearchScreenConstants.SEARCH_ROW_COUNT
        } else {
            SearchScreenConstants.ROW_COUNT
        }
    // Load a large number of apps to ensure the grid is always filled
    val visibleAppLimit = visibleRowCount * columns

    val displayApps =
        remember(
            state.query,
            state.recentApps,
            state.searchResults,
            state.pinnedApps,
            visibleAppLimit,
        ) {
            if (!isSearching) {
                val pinnedPackages = state.pinnedApps.map { it.packageName }.toSet()
                (
                    state.pinnedApps +
                        state.recentApps.filterNot {
                            pinnedPackages.contains(it.packageName)
                        }
                ).take(visibleAppLimit)
            } else {
                state.searchResults.take(visibleAppLimit)
            }
        }

    val pinnedPackageNames =
        remember(state.pinnedApps) { state.pinnedApps.map { it.packageName }.toSet() }
    val hasAppResults = displayApps.isNotEmpty()
    val hasContactResults = state.contactResults.isNotEmpty()
    val hasFileResults = state.fileResults.isNotEmpty()
    val hasSettingResults = state.settingResults.isNotEmpty()
    val hasAppShortcutResults = state.appShortcutResults.isNotEmpty()
    val pinnedContactIds =
        remember(state.pinnedContacts) { state.pinnedContacts.map { it.contactId }.toSet() }
    val pinnedFileUris =
        remember(state.pinnedFiles) { state.pinnedFiles.map { it.uri.toString() }.toSet() }
    val pinnedSettingIds =
        remember(state.pinnedSettings) { state.pinnedSettings.map { it.id }.toSet() }
    val pinnedAppShortcutIds =
        remember(state.pinnedAppShortcuts) {
            state.pinnedAppShortcuts.map { shortcutKey(it) }.toSet()
        }
    val hasMultipleExpandableSections =
        listOf(hasContactResults, hasFileResults, hasSettingResults, hasAppShortcutResults)
            .count { it } > 1

    val orderedSections =
        remember(state.disabledSections) {
            ItemPriorityConfig.getSearchResultsPriority().filter { it !in state.disabledSections }
        }

    val shouldShowApps = SearchSection.APPS !in state.disabledSections && hasAppResults
    val shouldShowAppShortcuts =
        SearchSection.APP_SHORTCUTS !in state.disabledSections &&
            (hasAppShortcutResults || hasPinnedAppShortcuts)
    val shouldShowContacts =
        SearchSection.CONTACTS !in state.disabledSections &&
            (!state.hasContactPermission || hasContactResults || hasPinnedContacts)
    val shouldShowFiles =
        SearchSection.FILES !in state.disabledSections &&
            (!state.hasFilePermission || hasFileResults || hasPinnedFiles)
    val shouldShowSettings =
        SearchSection.SETTINGS !in state.disabledSections &&
            (hasSettingResults || hasPinnedSettings)

    return DerivedState(
        isSearching = isSearching,
        hasPinnedContacts = hasPinnedContacts,
        hasPinnedFiles = hasPinnedFiles,
        hasPinnedSettings = hasPinnedSettings,
        hasPinnedAppShortcuts = hasPinnedAppShortcuts,
        visibleRowCount = visibleRowCount,
        visibleAppLimit = visibleAppLimit,
        displayApps = displayApps,
        pinnedPackageNames = pinnedPackageNames,
        pinnedSettingIds = pinnedSettingIds,
        pinnedAppShortcutIds = pinnedAppShortcutIds,
        hasAppResults = hasAppResults,
        hasContactResults = hasContactResults,
        hasFileResults = hasFileResults,
        hasSettingResults = hasSettingResults,
        hasAppShortcutResults = hasAppShortcutResults,
        pinnedContactIds = pinnedContactIds,
        pinnedFileUris = pinnedFileUris,
        hasMultipleExpandableSections = hasMultipleExpandableSections,
        orderedSections = orderedSections,
        shouldShowApps = shouldShowApps,
        shouldShowContacts = shouldShowContacts,
        shouldShowFiles = shouldShowFiles,
        shouldShowSettings = shouldShowSettings,
        shouldShowAppShortcuts = shouldShowAppShortcuts,
    )
}
