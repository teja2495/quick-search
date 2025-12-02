package com.tk.quicksearch.search

import androidx.compose.animation.core.spring
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
    const val SPRING_DAMPING_RATIO = 0.9f
    const val SPRING_STIFFNESS = 500f
}

private val scrollAnimationSpec = spring<Float>(
    dampingRatio = ScrollBehaviorConstants.SPRING_DAMPING_RATIO,
    stiffness = ScrollBehaviorConstants.SPRING_STIFFNESS
)

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
 * Scrolls to the bottom of the scrollable content.
 */
private suspend fun scrollToBottom(scrollState: ScrollState) {
    val targetScroll = scrollState.maxValue
    if (targetScroll > 0) {
        scrollState.animateScrollTo(
            value = targetScroll,
            animationSpec = scrollAnimationSpec
        )
    }
}

/**
 * Scrolls to the top of the scrollable content.
 */
private suspend fun scrollToTop(scrollState: ScrollState) {
    scrollState.animateScrollTo(
        value = 0,
        animationSpec = scrollAnimationSpec
    )
}

// ============================================================================
// Main Composable
// ============================================================================

/**
 * Handles scroll behavior for keyboard-aligned layout.
 * 
 * When keyboard-aligned layout is active:
 * - Scrolls to bottom when no section is expanded (showing bottom-aligned content)
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
    query: String,
    displayAppsSize: Int,
    contactResultsSize: Int,
    fileResultsSize: Int,
    pinnedContactsSize: Int,
    pinnedFilesSize: Int,
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
        hasUsagePermission,
        errorMessage,
        expandedSection,
        keyboardAlignedLayout
    ) {
        if (!keyboardAlignedLayout) return@LaunchedEffect
        
        // Wait for content to stabilize before scrolling
        waitForContentStabilization(scrollState)
        
        // Determine scroll direction based on expansion state
        if (expandedSection == ExpandedSection.NONE) {
            scrollToBottom(scrollState)
        } else {
            scrollToTop(scrollState)
        }
    }
}
