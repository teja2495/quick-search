package com.tk.quicksearch.search.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.ui.AppsSectionParams
import com.tk.quicksearch.search.ui.ContactsSectionParams
import com.tk.quicksearch.search.ui.FilesSectionParams
import com.tk.quicksearch.search.ui.SectionRenderingState
import com.tk.quicksearch.search.ui.SettingsSectionParams

/**
 * Composable extension functions for conditional rendering based on visibility states.
 * Provides clean, type-safe alternatives to manual if-else visibility logic.
 */

/**
 * Shows content only when the screen is in a content state.
 */
@Composable
fun ContentVisibility(
    screenState: ScreenVisibilityState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = screenState is ScreenVisibilityState.Content,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Shows content only when the screen is loading.
 */
@Composable
fun LoadingVisibility(
    screenState: ScreenVisibilityState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = screenState is ScreenVisibilityState.Loading || screenState is ScreenVisibilityState.Initializing,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Shows content only when there's an error.
 */
@Composable
fun ErrorVisibility(
    screenState: ScreenVisibilityState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = screenState is ScreenVisibilityState.Error,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Shows content only when the screen is empty.
 */
@Composable
fun EmptyVisibility(
    screenState: ScreenVisibilityState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = screenState is ScreenVisibilityState.Empty,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Shows content only when permissions are missing.
 */
@Composable
fun NoPermissionsVisibility(
    screenState: ScreenVisibilityState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = screenState is ScreenVisibilityState.NoPermissions,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

// Section-specific visibility helpers

/**
 * Shows apps section content based on its visibility state.
 */
@Composable
fun AppsSectionVisibility(
    sectionState: AppsSectionVisibility,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = {},
    noResultsContent: @Composable () -> Unit = {},
    resultsContent: @Composable (hasPinned: Boolean) -> Unit
) {
    when (sectionState) {
        is AppsSectionVisibility.Hidden -> {
            // Nothing to show
        }
        is AppsSectionVisibility.Loading -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                loadingContent()
            }
        }
        is AppsSectionVisibility.NoResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                noResultsContent()
            }
        }
        is AppsSectionVisibility.ShowingResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                resultsContent(sectionState.hasPinned)
            }
        }
    }
}

/**
 * Shows contacts section content based on its visibility state.
 */
@Composable
fun ContactsSectionVisibility(
    sectionState: ContactsSectionVisibility,
    modifier: Modifier = Modifier,
    noPermissionContent: @Composable () -> Unit = {},
    noResultsContent: @Composable () -> Unit = {},
    resultsContent: @Composable (hasPinned: Boolean) -> Unit
) {
    when (sectionState) {
        is ContactsSectionVisibility.Hidden -> {
            // Nothing to show
        }
        is ContactsSectionVisibility.NoPermission -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                noPermissionContent()
            }
        }
        is ContactsSectionVisibility.NoResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                noResultsContent()
            }
        }
        is ContactsSectionVisibility.ShowingResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                resultsContent(sectionState.hasPinned)
            }
        }
    }
}

/**
 * Shows files section content based on its visibility state.
 */
@Composable
fun FilesSectionVisibility(
    sectionState: FilesSectionVisibility,
    modifier: Modifier = Modifier,
    noPermissionContent: @Composable () -> Unit = {},
    noResultsContent: @Composable () -> Unit = {},
    resultsContent: @Composable (hasPinned: Boolean) -> Unit
) {
    when (sectionState) {
        is FilesSectionVisibility.Hidden -> {
            // Nothing to show
        }
        is FilesSectionVisibility.NoPermission -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                noPermissionContent()
            }
        }
        is FilesSectionVisibility.NoResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                noResultsContent()
            }
        }
        is FilesSectionVisibility.ShowingResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                resultsContent(sectionState.hasPinned)
            }
        }
    }
}

/**
 * Shows settings section content based on its visibility state.
 */
@Composable
fun SettingsSectionVisibility(
    sectionState: SettingsSectionVisibility,
    modifier: Modifier = Modifier,
    noResultsContent: @Composable () -> Unit = {},
    resultsContent: @Composable (hasPinned: Boolean) -> Unit
) {
    when (sectionState) {
        is SettingsSectionVisibility.Hidden -> {
            // Nothing to show
        }
        is SettingsSectionVisibility.NoResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                noResultsContent()
            }
        }
        is SettingsSectionVisibility.ShowingResults -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                resultsContent(sectionState.hasPinned)
            }
        }
    }
}

/**
 * Shows search engines based on their visibility state.
 */
@Composable
fun SearchEnginesVisibility(
    enginesState: SearchEnginesVisibility,
    modifier: Modifier = Modifier,
    hiddenContent: @Composable () -> Unit = {},
    compactContent: @Composable () -> Unit = {},
    fullContent: @Composable () -> Unit = {},
    shortcutContent: @Composable (SearchEngine) -> Unit = {}
) {
    when (enginesState) {
        is SearchEnginesVisibility.Hidden -> {
            hiddenContent()
        }
        is SearchEnginesVisibility.Compact -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                compactContent()
            }
        }
        is SearchEnginesVisibility.Full -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                fullContent()
            }
        }
        is SearchEnginesVisibility.ShortcutDetected -> {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = modifier
            ) {
                shortcutContent(enginesState.engine)
            }
        }
    }
}

// Utility functions for common visibility patterns

/**
 * Shows content only when condition is true, with smooth animation.
 */
@Composable
fun VisibleIf(
    condition: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = condition,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Shows content only when condition is false, with smooth animation.
 */
@Composable
fun GoneIf(
    condition: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = !condition,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        content()
    }
}

// ============================================================================
// Visibility State Helpers
// ============================================================================

/**
 * Checks if any section will show content based on the new visibility states.
 * This replaces the old hasAnySearchContent logic.
 */
fun hasAnySectionContent(state: SearchUiState): Boolean {
    return when (state.appsSectionState) {
        is AppsSectionVisibility.ShowingResults -> true
        else -> false
    } || when (state.contactsSectionState) {
        is ContactsSectionVisibility.ShowingResults -> true
        else -> false
    } || when (state.filesSectionState) {
        is FilesSectionVisibility.ShowingResults -> true
        else -> false
    } || when (state.settingsSectionState) {
        is SettingsSectionVisibility.ShowingResults -> true
        else -> false
    }
}

// ============================================================================
// Section Content Composables - Use new visibility states
// ============================================================================

// TODO: Implement full section rendering using new visibility states
// This would replace the old SearchResultsSections approach