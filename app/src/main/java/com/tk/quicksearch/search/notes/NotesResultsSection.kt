package com.tk.quicksearch.search.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TextSnippet
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PinEnd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.search.searchScreen.components.LocalSearchResultQuery
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.settings.shared.AliasPill
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.shared.util.hapticConfirm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NotesResultsSection(
    notes: List<NoteInfo>,
    pinnedNoteIds: Set<Long>,
    onNoteClick: (NoteInfo) -> Unit,
    onTogglePin: (NoteInfo) -> Unit,
    onMovePinned: (NoteInfo, Boolean) -> Unit = { _, _ -> },
    onDelete: (NoteInfo) -> Unit,
    onTriggerClick: (NoteInfo) -> Unit,
    getNoteTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    isExpanded: Boolean,
    showAllResults: Boolean,
    showExpandControls: Boolean,
    onExpandClick: () -> Unit,
    expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    showWallpaperBackground: Boolean,
    predictedTarget: PredictedSubmitTarget? = null,
    fillExpandedHeight: Boolean = false,
    showPinnedItemMenu: Boolean = false,
) {
    if (notes.isEmpty()) return

    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    val predictedNoteId = (predictedTarget as? PredictedSubmitTarget.Note)?.noteId
    val hasPredictedNote = predictedNoteId != null && notes.any { it.noteId == predictedNoteId }
    val displayAsExpanded = isExpanded || showAllResults
    val useCardLevelPrediction = hasPredictedNote && (!displayAsExpanded || notes.size == 1)
    val scrollState = rememberScrollState()

    ExpandableResultsCard(
        resultCount = notes.size,
        isExpanded = isExpanded,
        showAllResults = showAllResults,
        isTopPredicted = useCardLevelPrediction,
        showExpandControls = showExpandControls,
        expandedCardMaxHeight = expandedCardMaxHeight,
        hasScrollableContent = scrollState.maxValue > 0,
        fillExpandedHeight = fillExpandedHeight,
        showWallpaperBackground = showWallpaperBackground,
        overlayCardColor = overlayCardColor,
    ) { contentModifier, cardState ->
        val displayNotes =
            if (cardState.displayAsExpanded) {
                notes
            } else {
                notes.take(SearchScreenConstants.INITIAL_RESULT_COUNT)
            }
        Column(
            modifier =
                contentModifier.then(
                    if (isExpanded) {
                        Modifier.verticalScroll(scrollState)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier =
                    Modifier.padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp)
                        .padding(
                            bottom =
                                if (cardState.shouldFillExpandedHeight) {
                                    DesignTokens.SpacingSmall
                                } else {
                                    0.dp
                                },
                        ),
            ) {
                displayNotes.forEachIndexed { index, note ->
                    key(note.noteId) {
                        val isPredicted = predictedNoteId != null && predictedNoteId == note.noteId
                        val showPredictedOnRow = isPredicted && !useCardLevelPrediction
                        NoteRow(
                            note = note,
                            isPinned = pinnedNoteIds.contains(note.noteId),
                            onClick = onNoteClick,
                            onTogglePin = onTogglePin,
                            onMovePinned = onMovePinned,
                            onDelete = onDelete,
                            onTriggerClick = onTriggerClick,
                            hasTrigger = getNoteTrigger(note.noteId)?.word?.isNotBlank() == true,
                            isPredicted = showPredictedOnRow,
                            showPinnedItemMenu = showPinnedItemMenu,
                        )
                        if (index < displayNotes.lastIndex && !showPredictedOnRow) {
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

                if (cardState.shouldShowExpandButton) {
                    ExpandButton(
                        onClick = onExpandClick,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textResId = R.string.action_expand_more_notes,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NoteRow(
    note: NoteInfo,
    isPinned: Boolean,
    onClick: (NoteInfo) -> Unit,
    onTogglePin: (NoteInfo) -> Unit,
    onMovePinned: (NoteInfo, Boolean) -> Unit = { _, _ -> },
    onDelete: (NoteInfo) -> Unit,
    onTriggerClick: (NoteInfo) -> Unit,
    hasTrigger: Boolean,
    isPredicted: Boolean,
    showPinnedItemMenu: Boolean = false,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val rowView = LocalView.current
    val context = LocalContext.current
    val copyScope = rememberCoroutineScope()
    val query = LocalSearchResultQuery.current
    val title = note.title.ifBlank { stringResource(R.string.notes_untitled) }
    val notificationAction =
        CustomWidgetButtonAction.Note(
            noteId = note.noteId,
            title = title,
            markdownContent = note.markdownContent,
        )
    val isPinnedToNotifications = PinnedNotifications.isPinned(context, notificationAction)
    val matchingPreview =
        remember(note.markdownContent, query) {
            NotesTextUtils.matchCenteredPreview(note.markdownContent, query)
        }
    val preview =
        matchingPreview
            ?: remember(note.markdownContent) {
                NotesTextUtils.firstLinesPreview(note.markdownContent)
            }
    val subtitle =
        if (preview.isNotBlank()) {
            preview
        } else {
            stringResource(R.string.notes_empty_note_subtext)
        }

    Box(modifier = Modifier.fillMaxWidth()) {
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
                    .topPredictedRowContentPadding()
                    .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector =
                    if (note.isSnippet) {
                        Icons.AutoMirrored.Rounded.TextSnippet
                    } else {
                        Icons.Rounded.Description
                    },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 7.dp, top = 1.dp).size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = rememberQueryHighlightedText(title, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = rememberQueryHighlightedText(subtitle, query),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (matchingPreview != null) 5 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.isSnippet && note.keyword.isNotBlank()) {
                    AliasPill(
                        text = rememberQueryHighlightedText(note.keyword, query),
                        onClick = null,
                        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            if (note.isSnippet) {
                IconButton(
                    onClick = {
                        hapticConfirm(rowView)()
                        copyScope.launch {
                            // Cached home rows hold a truncated preview, so copy the stored body.
                            val content =
                                withContext(Dispatchers.IO) {
                                    NotesRepository(context).getNoteById(note.noteId)?.markdownContent
                                } ?: note.markdownContent
                            copyNoteContentToClipboard(context, content)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically).size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = stringResource(R.string.notes_copy_to_clipboard_desc),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                shape = RoundedCornerShape(24.dp),
                properties = PopupProperties(focusable = false),
                containerColor = AppColors.DialogBackground,
            ) {
                if (showPinnedItemMenu && isPinned) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_unpin_app)) },
                        onClick = {
                            showMenu = false
                            onTogglePin(note)
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_unpin),
                                contentDescription = null,
                            )
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_move_up)) },
                        onClick = {
                            showMenu = false
                            onMovePinned(note, true)
                        },
                        leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_move_down)) },
                        onClick = {
                            showMenu = false
                            onMovePinned(note, false)
                        },
                        leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                    )
                } else {
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
                        Icon(
                            painter =
                                painterResource(
                                    if (isPinned) {
                                        R.drawable.ic_unpin
                                    } else {
                                        R.drawable.ic_pin
                                    },
                                ),
                            contentDescription = null,
                        )
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (hasTrigger) {
                                    R.string.action_edit_trigger
                                } else {
                                    R.string.action_add_trigger
                                },
                            ),
                        )
                    },
                    onClick = {
                        showMenu = false
                        onTriggerClick(note)
                    },
                    leadingIcon = { Icon(Icons.Rounded.Bolt, contentDescription = null) },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(if (isPinnedToNotifications) R.string.action_unpin_from_notifications else R.string.action_pin_to_notifications)) },
                    onClick = {
                        showMenu = false
                        PinnedNotifications.toggle(context, notificationAction)
                    },
                    leadingIcon = {
                        if (isPinnedToNotifications) {
                            Icon(
                                painter = painterResource(R.drawable.ic_unpin),
                                contentDescription = null,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PinEnd,
                                contentDescription = null,
                            )
                        }
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dialog_delete)) },
                    onClick = {
                        showMenu = false
                        showDeleteConfirm = true
                    },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                )
                }
            }
        }

        if (showDeleteConfirm) {
            AppAlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(text = stringResource(R.string.notes_delete_confirm_title))
                },
                text = {
                    Text(text = stringResource(R.string.notes_delete_confirm_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete(note)
                        },
                    ) {
                        Text(text = stringResource(R.string.dialog_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(text = stringResource(R.string.dialog_cancel))
                    }
                },
            )
        }
    }
}
