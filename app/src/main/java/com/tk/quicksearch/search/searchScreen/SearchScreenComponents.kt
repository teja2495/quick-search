package com.tk.quicksearch.search.searchScreen

// Re-export all components from their respective files for backward compatibility

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.shared.ui.theme.DesignTokens

// Import the modifier functions
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitHighlight as componentsPredictedSubmitHighlight
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitCardBorder as componentsPredictedSubmitCardBorder

// Modifiers
internal fun Modifier.predictedSubmitHighlight(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
    opaqueCardTopResultBorder: Boolean = false,
): Modifier = componentsPredictedSubmitHighlight(isPredicted, shape, opaqueCardTopResultBorder)

internal fun Modifier.predictedSubmitCardBorder(
    isPredicted: Boolean,
    shape: Shape = DesignTokens.CardShape,
): Modifier = componentsPredictedSubmitCardBorder(isPredicted, shape)

// Cards
@Composable
internal fun PermissionDisabledCard(
    title: String,
    message: String,
    actionLabel: String,
    onActionClick: () -> Unit,
) = com.tk.quicksearch.search.searchScreen.components.PermissionDisabledCard(
    title = title,
    message = message,
    actionLabel = actionLabel,
    onActionClick = onActionClick,
)

@Composable
internal fun UsagePermissionCard(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
) = com.tk.quicksearch.search.searchScreen.components.UsagePermissionCard(
    modifier = modifier,
    onRequestPermission = onRequestPermission,
    onDismiss = onDismiss,
)

// Banners
@Composable
internal fun InfoBanner(message: String) =
    com.tk.quicksearch.search.searchScreen.components.InfoBanner(message)

// Search Field
@Composable
internal fun PersistentSearchBar(
    query: String,
    selectRetainedQuery: Boolean,
    onSelectRetainedQueryHandled: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<com.tk.quicksearch.search.core.SearchTarget>,
    shortcutCodes: Map<String, String> = emptyMap(),
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onSearchAction: () -> Boolean,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: com.tk.quicksearch.search.core.SearchTarget? = null,
    detectedAliasSearchSection: com.tk.quicksearch.search.core.SearchSection? = null,
    isCurrencyConverterAliasMode: Boolean = false,
    isWordClockAliasMode: Boolean = false,
    isDictionaryAliasMode: Boolean = false,
    activeToolType: SearchToolType? = null,
    isCalculatorMode: Boolean = false,
    placeholderText: String,
    showWelcomeAnimation: Boolean = false,
    showWallpaperBackground: Boolean = false,
    opaqueBackground: Boolean = false,
    forceRestingOutline: Boolean = false,
    autoFocusOnStart: Boolean = false,
    onClearDetectedShortcut: () -> Unit = {},
    onSectionSelected: (com.tk.quicksearch.search.core.SearchSection) -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.PersistentSearchBar(
    query = query,
    selectRetainedQuery = selectRetainedQuery,
    onSelectRetainedQueryHandled = onSelectRetainedQueryHandled,
    onQueryChange = onQueryChange,
    onClearQuery = onClearQuery,
    onSettingsClick = onSettingsClick,
    dismissKeyboardBeforeSettingsClick = dismissKeyboardBeforeSettingsClick,
    enabledTargets = enabledTargets,
    shortcutCodes = shortcutCodes,
    shortcutEnabled = shortcutEnabled,
    isSearchEngineAliasSuffixEnabled = isSearchEngineAliasSuffixEnabled,
    onSearchAction = onSearchAction,
    shouldUseNumberKeyboard = shouldUseNumberKeyboard,
    detectedShortcutTarget = detectedShortcutTarget,
    detectedAliasSearchSection = detectedAliasSearchSection,
    isCurrencyConverterAliasMode = isCurrencyConverterAliasMode,
    isWordClockAliasMode = isWordClockAliasMode,
    isDictionaryAliasMode = isDictionaryAliasMode,
    activeToolType = activeToolType,
    isCalculatorMode = isCalculatorMode,
    placeholderText = placeholderText,
    showWelcomeAnimation = showWelcomeAnimation,
    showWallpaperBackground = showWallpaperBackground,
    opaqueBackground = opaqueBackground,
    forceRestingOutline = forceRestingOutline,
    autoFocusOnStart = autoFocusOnStart,
    onClearDetectedShortcut = onClearDetectedShortcut,
    onSectionSelected = onSectionSelected,
    onWelcomeAnimationCompleted = onWelcomeAnimationCompleted,
    modifier = modifier,
)

// Pills
@Composable
internal fun KeyboardSwitchPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.KeyboardSwitchPill(
    text = text,
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun OpenKeyboardAction(
    text: String,
    onClick: () -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.OpenKeyboardAction(
    text = text,
    onClick = onClick,
    showWallpaperBackground = showWallpaperBackground,
    modifier = modifier,
)

@Composable
internal fun OverlayExpandPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.OverlayExpandPill(
    text = text,
    onClick = onClick,
    modifier = modifier,
)

@Composable
internal fun NumberKeyboardOperatorPills(
    onOperatorClick: (String) -> Unit,
    isOverlayPresentation: Boolean = false,
    extendToScreenEdges: Boolean = true,
    showWallpaperBackground: Boolean = false,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.NumberKeyboardOperatorPills(
    onOperatorClick = onOperatorClick,
    isOverlayPresentation = isOverlayPresentation,
    extendToScreenEdges = extendToScreenEdges,
    showWallpaperBackground = showWallpaperBackground,
    modifier = modifier,
)

// Empty State
@Composable
internal fun EmptyState() =
    com.tk.quicksearch.search.searchScreen.components.EmptyState()
