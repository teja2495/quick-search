package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.inlineSearch.SearchToolType

/**
 * [PersistentSearchBar] plus the standalone settings button: when the mic takes the bar's trailing
 * slot, the settings icon moves to its own button beside the bar.
 */
@Composable
internal fun SearchBarWithSettingsButton(
    query: String,
    selectRetainedQuery: Boolean,
    onSelectRetainedQueryHandled: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    showSettingsIcon: Boolean = true,
    onVoiceClick: () -> Unit = {},
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<com.tk.quicksearch.search.core.SearchTarget>,
    shortcutCodes: Map<String, String> = emptyMap(),
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    triggerWords: Collection<String> = emptyList(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onSearchAction: () -> Boolean,
    onMoveTopResultSelectionUp: (() -> Boolean)? = null,
    onMoveTopResultSelectionDown: (() -> Boolean)? = null,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: com.tk.quicksearch.search.core.SearchTarget? = null,
    detectedAliasSearchSection: com.tk.quicksearch.search.core.SearchSection? = null,
    isCurrencyConverterAliasMode: Boolean = false,
    isWorldClockAliasMode: Boolean = false,
    isDictionaryAliasMode: Boolean = false,
    isWeatherAliasMode: Boolean = false,
    detectedCustomToolId: String? = null,
    detectedTaskerIntentId: String? = null,
    activeToolType: SearchToolType? = null,
    isCalculatorMode: Boolean = false,
    placeholderText: String,
    showWelcomeAnimation: Boolean = false,
    showWallpaperBackground: Boolean = false,
    opaqueBackground: Boolean = false,
    forceRestingOutline: Boolean = false,
    autoFocusOnStart: Boolean = false,
    releaseFocusOnLeave: Boolean = false,
    restoreKeyboardOnEnter: Boolean = false,
    onRestoreKeyboardHandled: () -> Unit = {},
    startupSurfaceReady: Boolean = true,
    onClearDetectedShortcut: () -> Unit = {},
    onSectionSelected: (com.tk.quicksearch.search.core.SearchSection) -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    onPressWhileKeyboardClosed: () -> Unit = {},
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    transparentBackground: Boolean = false,
    cornerRadius: androidx.compose.ui.unit.Dp = com.tk.quicksearch.shared.ui.theme.DesignTokens.Spacing28,
    searchBarStyle: com.tk.quicksearch.search.core.SearchBarStyle = com.tk.quicksearch.search.core.SearchBarStyle(),
    modifier: Modifier = Modifier,
) {
    // With the mic in the bar's trailing slot, the settings icon moves to its own button beside it.
    val showSettingsOutside = showSettingsIcon && searchBarStyle.showMicIcon
    val isAliasActive =
        isSearchBarAliasActive(
            detectedShortcutTarget = detectedShortcutTarget,
            detectedAliasSearchSection = detectedAliasSearchSection,
            activeToolType = activeToolType,
            isCurrencyConverterAliasMode = isCurrencyConverterAliasMode,
            isWorldClockAliasMode = isWorldClockAliasMode,
            isDictionaryAliasMode = isDictionaryAliasMode,
            isWeatherAliasMode = isWeatherAliasMode,
            detectedCustomToolId = detectedCustomToolId,
            detectedTaskerIntentId = detectedTaskerIntentId,
        )
    val searchBar: @Composable (Modifier) -> Unit = { barModifier ->
        PersistentSearchBar(
            query = query,
            selectRetainedQuery = selectRetainedQuery,
            onSelectRetainedQueryHandled = onSelectRetainedQueryHandled,
            onQueryChange = onQueryChange,
            onClearQuery = onClearQuery,
            onSettingsClick = onSettingsClick,
            showSettingsIcon = showSettingsIcon && !showSettingsOutside,
            onVoiceClick = onVoiceClick,
            dismissKeyboardBeforeSettingsClick = dismissKeyboardBeforeSettingsClick,
            enabledTargets = enabledTargets,
            shortcutCodes = shortcutCodes,
            shortcutEnabled = shortcutEnabled,
            triggerWords = triggerWords,
            isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
            onSearchAction = onSearchAction,
            onMoveTopResultSelectionUp = onMoveTopResultSelectionUp,
            onMoveTopResultSelectionDown = onMoveTopResultSelectionDown,
            shouldUseNumberKeyboard = shouldUseNumberKeyboard,
            detectedShortcutTarget = detectedShortcutTarget,
            detectedAliasSearchSection = detectedAliasSearchSection,
            isCurrencyConverterAliasMode = isCurrencyConverterAliasMode,
            isWorldClockAliasMode = isWorldClockAliasMode,
            isDictionaryAliasMode = isDictionaryAliasMode,
            isWeatherAliasMode = isWeatherAliasMode,
            detectedCustomToolId = detectedCustomToolId,
            detectedTaskerIntentId = detectedTaskerIntentId,
            activeToolType = activeToolType,
            isCalculatorMode = isCalculatorMode,
            placeholderText = placeholderText,
            showWelcomeAnimation = showWelcomeAnimation,
            showWallpaperBackground = showWallpaperBackground,
            opaqueBackground = opaqueBackground,
            forceRestingOutline = forceRestingOutline,
            autoFocusOnStart = autoFocusOnStart,
            releaseFocusOnLeave = releaseFocusOnLeave,
            restoreKeyboardOnEnter = restoreKeyboardOnEnter,
            onRestoreKeyboardHandled = onRestoreKeyboardHandled,
            startupSurfaceReady = startupSurfaceReady,
            onClearDetectedShortcut = onClearDetectedShortcut,
            onSectionSelected = onSectionSelected,
            onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
            onPressWhileKeyboardClosed = onPressWhileKeyboardClosed,
            focusRequester = focusRequester,
            transparentBackground = transparentBackground,
            cornerRadius = cornerRadius,
            searchBarStyle = searchBarStyle,
            modifier = barModifier,
        )
    }
    if (!showSettingsOutside) {
        searchBar(modifier)
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        searchBar(Modifier.weight(1f))
        // Mid-query the bar's clear action is the only control needed, so the button steps aside
        // and the bar grows into its space until the query is cleared.
        // Without clipping, so the button's light-theme shadow isn't cut off at the row's edges.
        AnimatedVisibility(
            visible = query.isEmpty() && !isAliasActive,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start, clip = false),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start, clip = false),
        ) {
            SearchBarSettingsButton(
                searchBarStyle = searchBarStyle,
                cornerRadius = cornerRadius,
                showWallpaperBackground = showWallpaperBackground,
                transparentBackground = transparentBackground,
                forceRestingOutline = forceRestingOutline,
                dismissKeyboardBeforeClick = dismissKeyboardBeforeSettingsClick,
                onClick = onSettingsClick,
                modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
            )
        }
    }
}
