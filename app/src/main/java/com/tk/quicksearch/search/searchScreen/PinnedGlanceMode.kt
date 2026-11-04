package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.tk.quicksearch.search.core.SearchUiState

/**
 * True while the overlay shows only pinned and At a Glance items (the "Pinned & At a Glance Items"
 * gesture action): no search bar, search engines, app suggestions or search history.
 */
val LocalPinnedGlanceOnly = staticCompositionLocalOf { false }

/**
 * Strips what pinned glance mode never shows; pinned items and At a Glance are left as they are.
 * One-handed mode is ignored: it pins content to the bottom by making the overlay fill its full
 * height, and this mode's card wraps its content instead.
 */
internal fun SearchUiState.forPinnedGlanceOnly(): SearchUiState =
    copy(
        recentApps = emptyList(),
        newOrUpdatedApps = emptyList(),
        mostUsedApps = emptyList(),
        appSuggestionsEnabled = false,
        showAllAppsButton = false,
        recentItems = emptyList(),
        recentQueriesEnabled = false,
        openKeyboardOnLaunch = false,
        oneHandedMode = false,
    )

/**
 * Stands in for the missing search field: keyboard gestures and one-handed overscroll still
 * request its focus, which would throw on an unattached [FocusRequester].
 */
@Composable
internal fun PinnedGlanceFocusTarget(focusRequester: FocusRequester) {
    Box(Modifier.focusRequester(focusRequester).focusable())
}
