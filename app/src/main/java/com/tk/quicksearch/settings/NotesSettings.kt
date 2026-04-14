package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import java.util.Locale
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val NotesSettingsBarCornerShape = RoundedCornerShape(28.dp)

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
    val searchFocusRequester = remember { FocusRequester() }
    var isSearchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(multiSelectActive) {
        if (multiSelectActive) {
            isSearchExpanded = false
        }
    }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) {
            searchFocusRequester.requestFocus()
        }
    }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (multiSelectActive) {
            val deleteEnabled = selectedNoteCount > 0
            ExtendedFloatingActionButton(
                onClick = { if (deleteEnabled) onDeleteSelected() },
                expanded = true,
                modifier = Modifier.fillMaxWidth().alpha(if (deleteEnabled) 1f else 0.38f),
                shape = NotesSettingsBarCornerShape,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                text = { Text(text = stringResource(R.string.dialog_delete)) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.notes_delete_note_desc),
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        } else if (isSearchExpanded) {
            SettingsManagementSearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onClear = onClear,
                modifier = Modifier.weight(1f),
                applyDefaultPadding = false,
                applyImePadding = false,
                fillMaxWidth = false,
                focusRequester = searchFocusRequester,
            )
            FloatingActionButton(
                onClick = onNewNote,
                modifier = Modifier.size(48.dp),
                shape = NotesSettingsBarCornerShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.notes_create_note_cta),
                    modifier = Modifier.size(24.dp),
                )
            }
        } else {
            FloatingActionButton(
                onClick = { isSearchExpanded = true },
                modifier = Modifier.size(48.dp),
                shape = NotesSettingsBarCornerShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.desc_search_icon),
                    modifier = Modifier.size(22.dp),
                )
            }
            ExtendedFloatingActionButton(
                onClick = onNewNote,
                expanded = true,
                modifier = Modifier.weight(1f),
                shape = NotesSettingsBarCornerShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                text = { Text(text = stringResource(R.string.notes_create_note_cta)) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
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
    val filteredNotes =
        remember(notes, normalizedSearchQuery, locale) {
            if (normalizedSearchQuery.isBlank()) {
                notes
            } else {
                notes.filter { note ->
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

    Box(modifier = modifier) {
        SettingsCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.notes_empty_state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (filteredNotes.isEmpty()) {
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    itemsIndexed(
                        items = filteredNotes,
                        key = { _, note -> note.noteId },
                    ) { index, note ->
                        NoteListRow(
                            note = note,
                            isPinned = repository.isPinned(note.noteId),
                            selectionMode = multiSelectActive,
                            isSelected = selectedNoteIds.contains(note.noteId),
                            onOpen = { onOpenNoteEditor(note.noteId) },
                            onLongPress = { onEnterMultiSelect(note.noteId) },
                            onToggleSelected = { onToggleNoteSelected(note.noteId) },
                        )
                        if (index < filteredNotes.lastIndex) {
                            HorizontalDivider(color = AppColors.SettingsDivider)
                        }
                    }
                }
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
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelected() },
            )
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
                style = MaterialTheme.typography.bodySmall,
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
