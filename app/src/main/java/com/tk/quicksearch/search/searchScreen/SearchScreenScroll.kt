package com.tk.quicksearch.search.searchScreen
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// ============================================================================
// Constants
// ============================================================================

private object ScrollBehaviorConstants {
    const val INITIAL_LAYOUT_DELAY_MS = 50L
    const val POLLING_INTERVAL_MS = 20L
    const val MAX_POLLING_ATTEMPTS = 10
    const val STABLE_COUNT_THRESHOLD = 2
    const val KEYBOARD_HIDE_SHOW_COOLDOWN_MS = 500L
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Waits for the scroll state's maxValue to stabilize, indicating content layout is complete.
 * Returns true if content was detected and stabilized, false otherwise.
 */
private suspend fun waitForContentStabilization(
    scrollState: ScrollState,
    initialDelayMs: Long = ScrollBehaviorConstants.INITIAL_LAYOUT_DELAY_MS,
    pollingIntervalMs: Long = ScrollBehaviorConstants.POLLING_INTERVAL_MS,
    maxAttempts: Int = ScrollBehaviorConstants.MAX_POLLING_ATTEMPTS,
    stableCountThreshold: Int = ScrollBehaviorConstants.STABLE_COUNT_THRESHOLD,
): Boolean {
    delay(initialDelayMs)

    var attempts = 0
    var lastMaxValue = 0
    var stableCount = 0

    while (attempts < maxAttempts) {
        delay(pollingIntervalMs)
        val currentMaxValue = scrollState.maxValue

        if (currentMaxValue > 0) {
            if (currentMaxValue == lastMaxValue) {
                stableCount++
                if (stableCount >= stableCountThreshold) {
                    return true
                }
            } else {
                stableCount = 0
                lastMaxValue = currentMaxValue
            }
        }
        attempts++
    }

    return scrollState.maxValue > 0
}

/**
 * Scrolls to the top of the scrollable content.
 */
private suspend fun scrollToTop(
    scrollState: ScrollState,
    reverseScrolling: Boolean,
) {
    val targetScroll =
        if (reverseScrolling) {
            scrollState.maxValue
        } else {
            0
        }
    if (targetScroll > 0 || !reverseScrolling) {
        scrollState.scrollTo(targetScroll)
    }
}

// ============================================================================
// Main Composable
// ============================================================================

/**
 * Handles scroll behavior for one-handed mode.
 *
 * When one-handed mode is active:
 * - Content is bottom-aligned via reverse scrolling in the layout
 * - Scrolls to top when a section is expanded (showing expanded content near search bar)
 *
 * The scroll behavior is triggered when content changes (query, results, permissions, etc.)
 * to ensure proper positioning after layout updates.
 */
@Composable
fun OneHandedModeScrollBehavior(
    scrollState: ScrollState,
    expandedSection: ExpandedSection,
    oneHandedMode: Boolean,
    reverseScrolling: Boolean,
    query: String,
    displayAppsSize: Int,
    contactResultsSize: Int,
    appShortcutResultsSize: Int,
    fileResultsSize: Int,
    pinnedContactsSize: Int,
    pinnedAppShortcutsSize: Int,
    pinnedFilesSize: Int,
    settingResultsSize: Int,
    pinnedSettingsSize: Int,
    hasUsagePermission: Boolean,
    errorMessage: String?,
) {
    LaunchedEffect(
        query,
        displayAppsSize,
        contactResultsSize,
        appShortcutResultsSize,
        fileResultsSize,
        pinnedContactsSize,
        pinnedAppShortcutsSize,
        pinnedFilesSize,
        settingResultsSize,
        pinnedSettingsSize,
        hasUsagePermission,
        errorMessage,
        expandedSection,
        oneHandedMode,
        reverseScrolling,
    ) {
        if (!oneHandedMode) return@LaunchedEffect

        // Wait for content to stabilize before scrolling
        waitForContentStabilization(scrollState)

        // Only auto-position when a section is expanded to bring it near the search bar
        if (expandedSection != ExpandedSection.NONE) {
            scrollToTop(scrollState, reverseScrolling)
        }
    }
}

/**
 * Handles keyboard visibility based on scroll position when overlay mode is off.
 *
 * When overlay mode is OFF:
 * - Scroll down: hide keyboard
 * - Reach top: show keyboard
 *
 * When one-handed mode is ON (reversed behavior):
 * - Scroll up: hide keyboard
 * - Reach bottom: show keyboard
 */
@Composable
fun ScrollBasedKeyboardBehavior(
    scrollState: ScrollState,
    overlayModeEnabled: Boolean,
    oneHandedMode: Boolean,
    reverseScrolling: Boolean,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    if (overlayModeEnabled) return
    
    val threshold = 100
    
    LaunchedEffect(scrollState, oneHandedMode, reverseScrolling) {
        var previousScrollValue = scrollState.value
        var lastHideTime = 0L
        
        snapshotFlow { scrollState.value }
            .distinctUntilChanged()
            .collect { currentScrollValue ->
                val maxScroll = scrollState.maxValue
                
                if (maxScroll == 0) {
                    previousScrollValue = currentScrollValue
                    return@collect
                }
                
                val scrollDelta = currentScrollValue - previousScrollValue
                val hasScrolled = kotlin.math.abs(scrollDelta) > 0
                
                if (!hasScrolled) {
                    previousScrollValue = currentScrollValue
                    return@collect
                }
                
                val now = System.currentTimeMillis()
                val isInCooldown = (now - lastHideTime) < ScrollBehaviorConstants.KEYBOARD_HIDE_SHOW_COOLDOWN_MS
                
                if (oneHandedMode) {
                    val isAtBottom = if (reverseScrolling) {
                        currentScrollValue <= threshold
                    } else {
                        currentScrollValue >= maxScroll - threshold
                    }
                    
                    val wasAtBottom = if (reverseScrolling) {
                        previousScrollValue <= threshold
                    } else {
                        previousScrollValue >= maxScroll - threshold
                    }
                    
                    val isScrollingUp = if (reverseScrolling) {
                        scrollDelta > 0
                    } else {
                        scrollDelta < 0
                    }
                    
                    if (isAtBottom && !wasAtBottom && !isInCooldown) {
                        keyboardController?.show()
                    } else if (isScrollingUp && !isAtBottom) {
                        keyboardController?.hide()
                        lastHideTime = now
                    }
                } else {
                    val isAtTop = if (reverseScrolling) {
                        currentScrollValue >= maxScroll - threshold
                    } else {
                        currentScrollValue <= threshold
                    }
                    
                    val wasAtTop = if (reverseScrolling) {
                        previousScrollValue >= maxScroll - threshold
                    } else {
                        previousScrollValue <= threshold
                    }
                    
                    val isScrollingDown = if (reverseScrolling) {
                        scrollDelta < 0
                    } else {
                        scrollDelta > 0
                    }
                    
                    if (isAtTop && !wasAtTop && !isInCooldown) {
                        keyboardController?.show()
                    } else if (isScrollingDown && !isAtTop) {
                        keyboardController?.hide()
                        lastHideTime = now
                    }
                }
                
                previousScrollValue = currentScrollValue
            }
    }
}
