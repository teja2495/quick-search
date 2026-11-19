package com.tk.quicksearch.search.notes

import com.tk.quicksearch.settings.notesSettings.NoteDeleteConfirmationDialog
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonSegment
import com.tk.quicksearch.search.searchScreen.shared.segmentedRowsActive
import com.tk.quicksearch.search.searchScreen.shared.resultRowsContainerPadding
import com.tk.quicksearch.search.searchScreen.shared.resultRowsContainerVerticalPadding
import com.tk.quicksearch.search.searchScreen.shared.ResultSegment
import com.tk.quicksearch.search.searchScreen.shared.ResultRowDivider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.PredictedSubmitTarget
import com.tk.quicksearch.search.searchScreen.SearchScreenConstants
import com.tk.quicksearch.search.searchScreen.components.ExpandButton
import com.tk.quicksearch.search.searchScreen.components.ExpandButtonDivider
import com.tk.quicksearch.search.searchScreen.components.ExpandableResultsCard
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.resultRowVerticalPadding
import com.tk.quicksearch.search.searchScreen.components.LocalSearchResultQuery
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.shared.util.hapticConfirm

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
        segmented = true,
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
            val segmentCount = displayNotes.size + if (cardState.shouldShowExpandButton) 1 else 0
            val segmented = segmentedRowsActive()
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = resultRowsContainerPadding(DesignTokens.SpacingMedium),
                        vertical = resultRowsContainerVerticalPadding(4.dp),
                    )
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
                        ResultSegment(index = index, count = segmentCount) {
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
                        }
                        if (index < displayNotes.lastIndex && !showPredictedOnRow) {
                            ResultRowDivider(showWallpaperBackground, color = overlayDividerColor)
                        }
                    }
                }

                if (cardState.shouldShowExpandButton && segmented) {
                    ExpandButtonSegment(index = segmentCount - 1, count = segmentCount, onClick = onExpandClick, textResId = R.string.action_expand_more_notes)
                } else if (cardState.shouldShowExpandButton) {
                    ExpandButtonDivider(showWallpaperBackground)
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
                        role = Role.Button,
                        onClick = {
                            hapticConfirm(rowView)()
                            onClick(note)
                        },
                        onLongClickLabel = stringResource(R.string.desc_show_options),
                        onLongClick = { showMenu = true },
                    )
                    .padding(vertical = resultRowVerticalPadding(DesignTokens.SpacingMedium)),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = MaterialSymbols.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = DesignTokens.ResultIconStartInset, top = 1.dp).size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
            ) {
                Text(
                    text = rememberQueryHighlightedText(title, query),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = rememberQueryHighlightedText(subtitle, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (matchingPreview != null) 5 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            AppDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                if (showPinnedItemMenu && isPinned) {
                    AppDropdownMenuItem(
                        text = stringResource(R.string.action_unpin_app),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_unpin),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showMenu = false
                            onTogglePin(note)
                        },
                    )
                    AppDropdownMenuDivider()
                    AppDropdownMenuItem(
                        text = stringResource(R.string.action_move_up),
                        icon = MaterialSymbols.ArrowUpward,
                        onClick = {
                            showMenu = false
                            onMovePinned(note, true)
                        },
                    )
                    AppDropdownMenuDivider()
                    AppDropdownMenuItem(
                        text = stringResource(R.string.action_move_down),
                        icon = MaterialSymbols.ArrowDownward,
                        onClick = {
                            showMenu = false
                            onMovePinned(note, false)
                        },
                    )
                } else {
                AppDropdownMenuItem(
                    text = stringResource(if (isPinned) { R.string.action_unpin_app } else { R.string.action_pin_app }),
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
                    onClick = {
                        showMenu = false
                        onTogglePin(note)
                    },
                )
                AppDropdownMenuDivider()
                AppDropdownMenuItem(
                    text = stringResource(if (hasTrigger) { R.string.action_edit_trigger } else { R.string.action_add_trigger }),
                    icon = MaterialSymbols.Bolt,
                    onClick = {
                        showMenu = false
                        onTriggerClick(note)
                    },
                )
                AppDropdownMenuDivider()
                AppDropdownMenuItem(
                    text = stringResource(if (isPinnedToNotifications) R.string.action_unpin_from_notifications else R.string.action_pin_to_notifications),
                    leadingIcon = {
                        if (isPinnedToNotifications) {
                            Icon(
                                painter = painterResource(R.drawable.ic_unpin),
                                contentDescription = null,
                            )
                        } else {
                            Icon(
                                imageVector = MaterialSymbols.PinEnd,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = {
                        showMenu = false
                        PinnedNotifications.toggle(context, notificationAction)
                    },
                )
                AppDropdownMenuDivider()
                AppDropdownMenuItem(
                    text = stringResource(R.string.dialog_delete),
                    icon = MaterialSymbols.DeleteOutline,
                    onClick = {
                        showMenu = false
                        showDeleteConfirm = true
                    },
                )
                }
            }
        }

        if (showDeleteConfirm) {
            NoteDeleteConfirmationDialog(
                onConfirm = {
                    showDeleteConfirm = false
                    onDelete(note)
                },
                onDismiss = { showDeleteConfirm = false },
            )
        }
    }
}
