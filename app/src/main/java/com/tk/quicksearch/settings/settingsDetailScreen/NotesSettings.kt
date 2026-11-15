package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.util.Locale
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.settings.shared.SettingsSegment
import com.tk.quicksearch.settings.shared.SettingsIconBadge
import com.tk.quicksearch.settings.shared.SettingsFloatingActionHeight
import com.tk.quicksearch.settings.shared.settingsFloatingActionShape
import com.tk.quicksearch.settings.shared.SettingsSegmentRow
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NotesSettingsBottomBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onNewNote: () -> Unit,
    multiSelectActive: Boolean,
    selectedNoteCount: Int,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (multiSelectActive) {
        val deleteEnabled = selectedNoteCount > 0
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .imePadding()
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingMedium,
                ),
        ) {
            ExtendedFloatingActionButton(
                onClick = { if (deleteEnabled) onDeleteSelected() },
                expanded = true,
                modifier = Modifier.fillMaxWidth().height(SettingsFloatingActionHeight)
                    .alpha(if (deleteEnabled) 1f else 0.38f),
                shape = settingsFloatingActionShape(interactionSource),
                interactionSource = interactionSource,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                text = { Text(text = stringResource(R.string.dialog_delete)) },
                icon = {
                    Icon(
                        imageVector = MaterialSymbols.Delete,
                        contentDescription = stringResource(R.string.notes_delete_note_desc),
                        modifier = Modifier.size(DesignTokens.IconSizeSmall),
                    )
                },
            )
        }
    } else {
        CalendarEventsBottomBar(
            query = query,
            onQueryChange = onQueryChange,
            onClear = onClear,
            onNewEvent = onNewNote,
            newItemLabelResId = R.string.notes_create_note_cta,
            modifier = modifier,
        )
    }
}

@Composable
fun NotesSettingsSection(
    searchQuery: String = "",
    onOpenNoteEditor: (Long?) -> Unit,
    multiSelectActive: Boolean,
    selectedNoteIds: Set<Long>,
    onEnterMultiSelect: (Long) -> Unit,
    onToggleNoteSelected: (Long) -> Unit,
    notesRefreshSignal: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locale = Locale.getDefault()
    val normalizedSearchQuery =
        remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
    val repository = remember(context) { NotesRepository(context) }
    var refreshToken by remember { mutableLongStateOf(0L) }
    val notes by produceState(initialValue = emptyList<NoteInfo>(), refreshToken, notesRefreshSignal) {
        value = withContext(Dispatchers.IO) { repository.getAllNotes() }
    }
    val quickNote = remember(notes) { notes.firstOrNull { repository.isQuickNote(it.noteId) } }
    val userNotes = remember(notes) { notes.filterNot { repository.isQuickNote(it.noteId) } }
    val filteredNotes =
        remember(userNotes, normalizedSearchQuery, locale) {
            if (normalizedSearchQuery.isBlank()) {
                userNotes
            } else {
                userNotes.filter { note ->
                    note.title.lowercase(locale).contains(normalizedSearchQuery) ||
                        note.markdownContent.lowercase(locale).contains(normalizedSearchQuery)
                }
            }
        }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshToken++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        quickNote?.let { note ->
            SettingsSegment(index = 0, count = 1, onClick = { onOpenNoteEditor(note.noteId) }) {
                SettingsSegmentRow(
                    title = stringResource(R.string.notes_quick_note_title),
                    leading = { SettingsIconBadge(icon = MaterialSymbols.EditNote) },
                )
            }
        }

        if (filteredNotes.isNotEmpty() || normalizedSearchQuery.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (filteredNotes.isEmpty()) {
                    SettingsSegment(index = 0, count = 1) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.widget_custom_buttons_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    NotesList(
                        notes = filteredNotes,
                        repository = repository,
                        selectedNoteIds = selectedNoteIds,
                        multiSelectActive = multiSelectActive,
                        onOpenNoteEditor = onOpenNoteEditor,
                        onEnterMultiSelect = onEnterMultiSelect,
                        onToggleNoteSelected = onToggleNoteSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesList(
    notes: List<NoteInfo>,
    repository: NotesRepository,
    selectedNoteIds: Set<Long>,
    multiSelectActive: Boolean,
    onOpenNoteEditor: (Long?) -> Unit,
    onEnterMultiSelect: (Long) -> Unit,
    onToggleNoteSelected: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(DesignTokens.ShapeXXLarge),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
    ) {
        itemsIndexed(
            items = notes,
            key = { _, note -> note.noteId },
        ) { index, note ->
            val selected = multiSelectActive && note.noteId in selectedNoteIds
            SettingsSegment(
                index = index,
                count = notes.size,
                containerColor = if (selected) {
                    AppColors.Accent.copy(alpha = 0.14f).compositeOver(AppColors.getSettingsCardContainerColor())
                } else {
                    AppColors.getSettingsCardContainerColor()
                },
            ) {
                NoteListRow(
                    note = note,
                    isPinned = repository.isPinned(note.noteId),
                    selectionMode = multiSelectActive,
                    isSelected = selectedNoteIds.contains(note.noteId),
                    onOpen = { onOpenNoteEditor(note.noteId) },
                    onLongPress = { onEnterMultiSelect(note.noteId) },
                    onToggleSelected = { onToggleNoteSelected(note.noteId) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteListRow(
    note: NoteInfo,
    isPinned: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelected: () -> Unit,
) {
    val contentModifier =
        if (selectionMode) {
            Modifier.combinedClickable(onClick = onToggleSelected, onLongClick = onLongPress)
        } else {
            Modifier.combinedClickable(onClick = onOpen, onLongClick = onLongPress)
        }
    androidx.compose.foundation.layout.Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(contentModifier)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelected() },
            )
        } else {
            SettingsIconBadge(icon = MaterialSymbols.EditNote)
        }
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
        ) {
            val preview = NotesTextUtils.firstLinesPreview(note.markdownContent)
            val subtitle =
                if (preview.isNotBlank()) {
                    preview
                } else {
                    stringResource(R.string.notes_empty_note_subtext)
                }
            Text(
                text = if (note.title.isBlank()) stringResource(R.string.notes_untitled) else note.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (isPinned) {
                Text(
                    text = stringResource(R.string.action_pin_app),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
