package com.tk.quicksearch.search.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchUiState

/**
 * Enum representing which section is currently expanded.
 */
enum class ExpandedSection {
    NONE,
    CONTACTS,
    FILES,
    SETTINGS
}

/**
 * Constants for search screen layout.
 */
private object SearchScreenConstants {
    const val INITIAL_RESULT_COUNT = 1
    const val ROW_COUNT = 2
    const val SEARCH_ROW_COUNT = 1
    const val COLUMNS = 5
}

/**
 * Data class holding all derived state calculations.
 */
internal data class DerivedState(
    val isSearching: Boolean,
    val hasPinnedContacts: Boolean,
    val hasPinnedFiles: Boolean,
    val hasPinnedSettings: Boolean,
    val visibleRowCount: Int,
    val visibleAppLimit: Int,
    val displayApps: List<AppInfo>,
    val pinnedPackageNames: Set<String>,
    val pinnedSettingIds: Set<String>,
    val hasAppResults: Boolean,
    val hasContactResults: Boolean,
    val hasFileResults: Boolean,
    val hasSettingResults: Boolean,
    val pinnedContactIds: Set<Long>,
    val pinnedFileUris: Set<String>,
    val autoExpandFiles: Boolean,
    val autoExpandContacts: Boolean,
    val autoExpandSettings: Boolean,
    val hasMultipleExpandableSections: Boolean,
    val orderedSections: List<SearchSection>,
    val shouldShowApps: Boolean,
    val shouldShowContacts: Boolean,
    val shouldShowFiles: Boolean,
    val shouldShowSettings: Boolean
)

/**
 * Calculates all derived state from SearchUiState.
 */
@Composable
internal fun rememberDerivedState(
    state: SearchUiState
): DerivedState {
    val isSearching = state.query.isNotBlank()
    val hasPinnedContacts = state.pinnedContacts.isNotEmpty() && state.hasContactPermission
    val hasPinnedFiles = state.pinnedFiles.isNotEmpty() && state.hasFilePermission
    val hasPinnedSettings = state.pinnedSettings.isNotEmpty()
    val visibleRowCount = if (isSearching || hasPinnedContacts || hasPinnedFiles || hasPinnedSettings) {
        SearchScreenConstants.SEARCH_ROW_COUNT
    } else {
        SearchScreenConstants.ROW_COUNT
    }
    val visibleAppLimit = visibleRowCount * SearchScreenConstants.COLUMNS

    val displayApps = remember(
        state.query,
        state.recentApps,
        state.searchResults,
        state.pinnedApps,
        visibleAppLimit
    ) {
        if (!isSearching) {
            val pinnedPackages = state.pinnedApps.map { it.packageName }.toSet()
            (state.pinnedApps + state.recentApps.filterNot { pinnedPackages.contains(it.packageName) })
                .take(visibleAppLimit)
        } else {
            state.searchResults.take(visibleAppLimit)
        }
    }

    val pinnedPackageNames = remember(state.pinnedApps) {
        state.pinnedApps.map { it.packageName }.toSet()
    }
    val hasAppResults = displayApps.isNotEmpty()
    val hasContactResults = state.contactResults.isNotEmpty()
    val hasFileResults = state.fileResults.isNotEmpty()
    val hasSettingResults = state.settingResults.isNotEmpty()
    val pinnedContactIds = remember(state.pinnedContacts) {
        state.pinnedContacts.map { it.contactId }.toSet()
    }
    val pinnedFileUris = remember(state.pinnedFiles) {
        state.pinnedFiles.map { it.uri.toString() }.toSet()
    }
    val pinnedSettingIds = remember(state.pinnedSettings) {
        state.pinnedSettings.map { it.id }.toSet()
    }
    val autoExpandFiles = (hasFileResults && !hasContactResults) || state.showAllResults
    val autoExpandContacts = (hasContactResults && !hasFileResults) || state.showAllResults
    val autoExpandSettings = (!isSearching && hasSettingResults && !hasContactResults && !hasFileResults) || state.showAllResults
    val hasMultipleExpandableSections = listOf(hasContactResults, hasFileResults, hasSettingResults).count { it } > 1

    val orderedSections = remember(state.sectionOrder, state.disabledSections) {
        state.sectionOrder.filter { it !in state.disabledSections }
    }

    val shouldShowApps = SearchSection.APPS !in state.disabledSections && hasAppResults
    val shouldShowContacts = SearchSection.CONTACTS !in state.disabledSections &&
        (!state.hasContactPermission || hasContactResults || hasPinnedContacts)
    val shouldShowFiles = SearchSection.FILES !in state.disabledSections &&
        (!state.hasFilePermission || hasFileResults || hasPinnedFiles)
    val shouldShowSettings = SearchSection.SETTINGS !in state.disabledSections &&
        (hasSettingResults || hasPinnedSettings)

    return DerivedState(
        isSearching = isSearching,
        hasPinnedContacts = hasPinnedContacts,
        hasPinnedFiles = hasPinnedFiles,
        hasPinnedSettings = hasPinnedSettings,
        visibleRowCount = visibleRowCount,
        visibleAppLimit = visibleAppLimit,
        displayApps = displayApps,
        pinnedPackageNames = pinnedPackageNames,
        pinnedSettingIds = pinnedSettingIds,
        hasAppResults = hasAppResults,
        hasContactResults = hasContactResults,
        hasFileResults = hasFileResults,
        hasSettingResults = hasSettingResults,
        pinnedContactIds = pinnedContactIds,
        pinnedFileUris = pinnedFileUris,
        autoExpandFiles = autoExpandFiles,
        autoExpandContacts = autoExpandContacts,
        autoExpandSettings = autoExpandSettings,
        hasMultipleExpandableSections = hasMultipleExpandableSections,
        orderedSections = orderedSections,
        shouldShowApps = shouldShowApps,
        shouldShowContacts = shouldShowContacts,
        shouldShowFiles = shouldShowFiles,
        shouldShowSettings = shouldShowSettings
    )
}
