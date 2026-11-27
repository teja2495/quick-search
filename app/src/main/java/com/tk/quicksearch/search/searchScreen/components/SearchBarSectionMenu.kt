package com.tk.quicksearch.search.searchScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchSectionRegistry
import com.tk.quicksearch.search.core.SearchSectionUiMetadataRegistry
import com.tk.quicksearch.shared.ui.components.ItemMenuTile
import com.tk.quicksearch.shared.ui.components.ItemMenuTileButton
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Material picker sheet for the searchable sections opened from the search bar. */
@Composable
internal fun SearchBarSectionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSectionSelected: (SearchSection) -> Unit,
) {
    if (!expanded) return
    AppPickerDrawer(
        title = stringResource(R.string.search_section_menu_title),
        onDismiss = onDismiss,
        // Opening the sheet from the search bar closes the keyboard; following its inset would
        // leave a keyboard-sized gap below the tiles.
        avoidKeyboard = false,
    ) { dismiss ->
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(bottom = DesignTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            sectionMenuRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
                    row.forEach { section ->
                        val metadata = SearchSectionUiMetadataRegistry.metadataFor(section)
                        ItemMenuTileButton(
                            tile = ItemMenuTile(
                                label = stringResource(metadata.sectionLabelRes),
                                icon = {
                                    Icon(
                                        imageVector = metadata.searchBarIcon,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    dismiss()
                                    onSectionSelected(section)
                                },
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(SectionMenuColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private const val SectionMenuColumns = 3
private val sectionMenuRows = SearchSectionRegistry.orderedSections.chunked(SectionMenuColumns)
