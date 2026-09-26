package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.searchEngines.*
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.search.searchScreen.TopMatchesSection
import com.tk.quicksearch.search.other.OtherSearchItemActionHandler
import com.tk.quicksearch.R

@Composable
internal fun ContentLayoutTopMatches(
    showTopMatchesSection: Boolean,
    displayedTopMatches: List<com.tk.quicksearch.search.searchScreen.TopMatchItem>,
    sectionParams: SectionRenderParams,
    effectiveShowWallpaperBackground: Boolean,
    state: SearchUiState,
    isPhysicalKeyboardConnected: Boolean,
    isLocalSearchRefreshing: Boolean,
    selectedTopMatchIndex: Int?,
    isReversed: Boolean,
    onOtherSearchItemAction: OtherSearchItemActionHandler,
    showTopMatches: Boolean,
    hasMoreResults: Boolean,
) {
        if (!showTopMatchesSection) return
        TopMatchesSection(
            matches = displayedTopMatches,
            params = sectionParams,
            showWallpaperBackground = effectiveShowWallpaperBackground,
            showTopResultIndicator =
                state.topResultIndicatorEnabled || isPhysicalKeyboardConnected,
            showHeader = !state.oneHandedMode || !isLocalSearchRefreshing,
            selectedMatchIndex = selectedTopMatchIndex,
            reverseOrder = isReversed,
            screenTimeState = state.screenTimeState,
            pinnedNonAppItemOrder = state.pinnedNonAppItemOrder,
            iconPackPackage = state.selectedIconPackPackage,
            onOtherSearchItemAction = onOtherSearchItemAction,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showTopMatches && hasMoreResults && !isReversed) {
            Text(
                text = stringResource(R.string.more_results_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.SpacingXSmall,
                    ),
            )
        }
    }
