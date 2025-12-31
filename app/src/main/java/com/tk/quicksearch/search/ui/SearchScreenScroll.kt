package com.tk.quicksearch.search.ui
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

// ============================================================================
// Constants
// ============================================================================

private object ScrollBehaviorConstants {
    const val INITIAL_LAYOUT_DELAY_MS = 50L
    const val POLLING_INTERVAL_MS = 20L
    const val MAX_POLLING_ATTEMPTS = 10
    const val STABLE_COUNT_THRESHOLD = 2
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
    stableCountThreshold: Int = ScrollBehaviorConstants.STABLE_COUNT_THRESHOLD
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
private suspend fun scrollToTop(scrollState: ScrollState, reverseScrolling: Boolean) {
    val targetScroll = if (reverseScrolling) {
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
 * Handles scroll behavior for keyboard-aligned layout.
 * 
 * When keyboard-aligned layout is active:
 * - Content is bottom-aligned via reverse scrolling in the layout
 * - Scrolls to top when a section is expanded (showing expanded content near search bar)
 * 
 * The scroll behavior is triggered when content changes (query, results, permissions, etc.)
 * to ensure proper positioning after layout updates.
 */
@Composable
fun KeyboardAlignedScrollBehavior(
    scrollState: ScrollState,
    expandedSection: ExpandedSection,
    keyboardAlignedLayout: Boolean,
    reverseScrolling: Boolean,
    query: String,
    displayAppsSize: Int,
    contactResultsSize: Int,
    fileResultsSize: Int,
    pinnedContactsSize: Int,
    pinnedFilesSize: Int,
    settingResultsSize: Int,
    pinnedSettingsSize: Int,
    hasUsagePermission: Boolean,
    errorMessage: String?
) {
    LaunchedEffect(
        query,
        displayAppsSize,
        contactResultsSize,
        fileResultsSize,
        pinnedContactsSize,
        pinnedFilesSize,
        settingResultsSize,
        pinnedSettingsSize,
        hasUsagePermission,
        errorMessage,
        expandedSection,
        keyboardAlignedLayout,
        reverseScrolling
    ) {
        if (!keyboardAlignedLayout) return@LaunchedEffect
        
        // Wait for content to stabilize before scrolling
        waitForContentStabilization(scrollState)
        
        // Only auto-position when a section is expanded to bring it near the search bar
        if (expandedSection != ExpandedSection.NONE) {
            scrollToTop(scrollState, reverseScrolling)
        }
    }
}
