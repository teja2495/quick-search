package com.tk.quicksearch.search.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.*
import kotlinx.coroutines.delay

/**
 * Composable that displays an empty results message when no search results are found.
 */
@Composable
fun EmptyResultsMessage(
    query: String,
    enabledSections: List<SearchSection>,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        visible = false
        delay(400) // Small delay to prevent flickering during search
        visible = true
    }

    if (!visible) return

    val sectionLabels = enabledSections.mapNotNull { section ->
        when (section) {
            SearchSection.APPS -> stringResource(R.string.empty_state_section_apps)
            SearchSection.CONTACTS -> stringResource(R.string.empty_state_section_contacts)
            SearchSection.FILES -> stringResource(R.string.empty_state_section_files)
            SearchSection.SETTINGS -> stringResource(R.string.empty_state_section_settings)
        }
    }

    if (sectionLabels.isEmpty()) return

    val sectionsText = formatSectionList(sectionLabels)
    val message = stringResource(R.string.search_empty_state_no_results, sectionsText)
    val textColor = if (showWallpaperBackground) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopStart
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Formats a list of section labels into a readable string.
 * Examples:
 * - ["Apps"] -> "Apps"
 * - ["Apps", "Contacts"] -> "Apps and Contacts"
 * - ["Apps", "Contacts", "Files"] -> "Apps, Contacts, and Files"
 */
private fun formatSectionList(sectionLabels: List<String>): String {
    return when (sectionLabels.size) {
        1 -> sectionLabels.first()
        2 -> sectionLabels.joinToString(separator = " and ")
        else -> {
            val last = sectionLabels.last()
            sectionLabels.dropLast(1).joinToString(separator = ", ") + ", and " + last
        }
    }
}
