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
): Modifier = componentsPredictedSubmitHighlight(isPredicted, shape)

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

@Composable
internal fun PersonalContextHintBanner(
    onOpenPersonalContext: () -> Unit,
    onOpenDirectSearchConfigure: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.PersonalContextHintBanner(
    onOpenPersonalContext = onOpenPersonalContext,
    onOpenDirectSearchConfigure = onOpenDirectSearchConfigure,
    onDismiss = onDismiss,
    modifier = modifier,
)

// Search Field
@Composable
internal fun PersistentSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    dismissKeyboardBeforeSettingsClick: Boolean = false,
    enabledTargets: List<com.tk.quicksearch.search.core.SearchTarget>,
    shortcutCodes: Map<String, String> = emptyMap(),
    shortcutEnabled: Map<String, Boolean> = emptyMap(),
    isSearchEngineAliasSuffixEnabled: Boolean = true,
    onSearchAction: () -> Unit,
    shouldUseNumberKeyboard: Boolean,
    detectedShortcutTarget: com.tk.quicksearch.search.core.SearchTarget? = null,
    detectedAliasSearchSection: com.tk.quicksearch.search.core.SearchSection? = null,
    activeToolType: SearchToolType? = null,
    isCalculatorMode: Boolean = false,
    placeholderText: String,
    showWelcomeAnimation: Boolean = false,
    opaqueBackground: Boolean = false,
    autoFocusOnStart: Boolean = false,
    onClearDetectedShortcut: () -> Unit = {},
    onWelcomeAnimationCompleted: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.PersistentSearchBar(
    query = query,
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
    activeToolType = activeToolType,
    isCalculatorMode = isCalculatorMode,
    placeholderText = placeholderText,
    showWelcomeAnimation = showWelcomeAnimation,
    opaqueBackground = opaqueBackground,
    autoFocusOnStart = autoFocusOnStart,
    onClearDetectedShortcut = onClearDetectedShortcut,
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
    modifier: Modifier = Modifier,
) = com.tk.quicksearch.search.searchScreen.components.NumberKeyboardOperatorPills(
    onOperatorClick = onOperatorClick,
    isOverlayPresentation = isOverlayPresentation,
    extendToScreenEdges = extendToScreenEdges,
    modifier = modifier,
)

// Empty State
@Composable
internal fun EmptyState() =
    com.tk.quicksearch.search.searchScreen.components.EmptyState()
