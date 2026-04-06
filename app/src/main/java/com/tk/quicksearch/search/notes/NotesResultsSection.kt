package com.tk.quicksearch.search.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

@Composable
fun NotesResultsSection(
    notes: List<NoteInfo>,
    pinnedNoteIds: Set<Long>,
    onNoteClick: (NoteInfo) -> Unit,
    onTogglePin: (NoteInfo) -> Unit,
    onDelete: (NoteInfo) -> Unit,
    showAllResults: Boolean,
    showWallpaperBackground: Boolean,
    predictedTarget: PredictedSubmitTarget? = null,
    fillExpandedHeight: Boolean = false,
) {
    if (notes.isEmpty()) return

    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    val predictedNoteId = (predictedTarget as? PredictedSubmitTarget.Note)?.noteId

    ExpandableResultsCard(
        resultCount = notes.size,
        isExpanded = false,
        showAllResults = showAllResults,
        isTopPredicted = predictedNoteId != null && notes.any { it.noteId == predictedNoteId },
        showExpandControls = false,
        expandedCardMaxHeight = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
        hasScrollableContent = false,
        fillExpandedHeight = fillExpandedHeight,
        showWallpaperBackground = showWallpaperBackground,
        overlayCardColor = overlayCardColor,
    ) { contentModifier, _ ->
        val displayNotes =
            if (showAllResults) {
                notes
            } else {
                notes.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
            }
        Column(
            modifier = contentModifier.padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp),
        ) {
            displayNotes.forEachIndexed { index, note ->
                key(note.noteId) {
                    val isPredicted = predictedNoteId != null && predictedNoteId == note.noteId
                    NoteRow(
                        note = note,
                        isPinned = pinnedNoteIds.contains(note.noteId),
                        onClick = onNoteClick,
                        onTogglePin = onTogglePin,
                        onDelete = onDelete,
                        isPredicted = isPredicted,
                    )
                    if (index < displayNotes.lastIndex && !isPredicted) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color =
                                overlayDividerColor
                                    ?: if (showWallpaperBackground) {
                                        AppColors.WallpaperDivider
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: NoteInfo,
    isPinned: Boolean,
    onClick: (NoteInfo) -> Unit,
    onTogglePin: (NoteInfo) -> Unit,
    onDelete: (NoteInfo) -> Unit,
    isPredicted: Boolean,
) {
    var showMenu by remember { mutableStateOf(false) }
    val rowView = LocalView.current
    val title = note.title.ifBlank { stringResource(R.string.notes_untitled) }
    val preview = NotesMarkdownUtils.buildPreviewText(note.markdownContent)

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .topPredictedRowContainer(isTopPredicted = isPredicted)
                .combinedClickable(
                    onClick = {
                        hapticConfirm(rowView)()
                        onClick(note)
                    },
                    onLongClick = { showMenu = true },
                )
                .topPredictedRowContentPadding(isTopPredicted = isPredicted)
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 7.dp, top = 1.dp).size(24.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (preview.isNotBlank()) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            properties = PopupProperties(focusable = true),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (isPinned) {
                                R.string.action_unpin_app
                            } else {
                                R.string.action_pin_app
                            },
                        ),
                    )
                },
                onClick = {
                    showMenu = false
                    onTogglePin(note)
                },
                leadingIcon = {
                    Icon(Icons.Rounded.PushPin, contentDescription = null)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dialog_delete)) },
                onClick = {
                    showMenu = false
                    onDelete(note)
                },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
            )
        }
    }
}
