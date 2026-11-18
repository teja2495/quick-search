package com.tk.quicksearch.widgets.noteWidget

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.notes.NotesRoomStore
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.settings.shared.SettingsIconBadge
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetConfigScaffold
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

private const val PICKER_PREVIEW_LINES = 2
private const val SELECTED_TINT_ALPHA = 0.14f

/** The note with [noteId], or null while it loads or when there is none. */
@Composable
internal fun rememberWidgetNote(noteId: Long?): NoteInfo? {
    val context = LocalContext.current
    val note by produceState<NoteInfo?>(initialValue = null, noteId) {
        value = noteId?.let { id -> withContext(Dispatchers.IO) { NotesRoomStore(context).getById(id) } }
    }
    return note
}

/** Full-screen list of the user's notes with a search bar, shown when a Note widget is placed. */
@Composable
fun NoteWidgetPickerScreen(
    selectedNoteId: Long?,
    onSelect: (NoteInfo) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val notes by produceState<List<NoteInfo>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { NotesRepository(context).getAllNotes() }
    }
    var query by rememberSaveable { mutableStateOf("") }
    val locale = Locale.getDefault()
    val filteredNotes =
        remember(notes, query, locale) {
            val normalizedQuery = query.trim().lowercase(locale)
            notes.orEmpty().filter { note ->
                normalizedQuery.isEmpty() ||
                    note.title.lowercase(locale).contains(normalizedQuery) ||
                    note.markdownContent.lowercase(locale).contains(normalizedQuery)
            }
        }

    WidgetConfigScaffold(
        title = stringResource(R.string.widget_note_picker_title),
        onCancel = onClose,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
                    .padding(
                        start = WidgetConfigConstants.HORIZONTAL_PADDING,
                        end = WidgetConfigConstants.HORIZONTAL_PADDING,
                        bottom = DesignTokens.SpacingLarge,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            SettingsManagementSearchBar(
                query = query,
                onQueryChange = { query = it },
                onClear = { query = "" },
                applyDefaultPadding = false,
                applyImePadding = false,
                placeholder = stringResource(R.string.search_hint_notes),
            )
            when {
                notes == null -> Unit
                filteredNotes.isEmpty() ->
                    Text(
                        text = stringResource(R.string.widget_custom_buttons_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                else ->
                    // fill = false lets a short list wrap instead of stretching.
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).clip(AppShapes.extraLarge),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
                    ) {
                        itemsIndexed(items = filteredNotes, key = { _, note -> note.noteId }) { index, note ->
                            val selected = note.noteId == selectedNoteId
                            SegmentedTile(
                                index = index,
                                count = filteredNotes.size,
                                onClick = { onSelect(note) },
                                containerColor =
                                    if (selected) {
                                        AppColors.Accent
                                            .copy(alpha = SELECTED_TINT_ALPHA)
                                            .compositeOver(AppColors.getSettingsCardContainerColor())
                                    } else {
                                        AppColors.getSettingsCardContainerColor()
                                    },
                            ) {
                                NoteRow(
                                    note = note,
                                    trailingIcon = MaterialSymbols.Check.takeIf { selected },
                                    trailingIconDescription = stringResource(R.string.desc_selected),
                                )
                            }
                        }
                    }
            }
        }
    }
}

/** The widget's current note in the configure screen; tapping it reopens the picker. */
@Composable
fun NoteWidgetSelectedNoteSection(
    noteId: Long?,
    onChangeNote: () -> Unit,
) {
    val note = rememberWidgetNote(noteId) ?: return
    SegmentedTile(index = 0, count = 1, onClick = onChangeNote) {
        NoteRow(
            note = note,
            trailingIcon = MaterialSymbols.ChevronRight,
            trailingIconDescription = null,
        )
    }
}

@Composable
private fun NoteRow(
    note: NoteInfo,
    trailingIcon: ImageVector?,
    trailingIconDescription: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        TonalIconBadge(icon = MaterialSymbols.EditNote)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Text(
                text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    NotesTextUtils
                        .firstLinesPreview(note.markdownContent, PICKER_PREVIEW_LINES)
                        .ifBlank { stringResource(R.string.notes_empty_note_subtext) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = PICKER_PREVIEW_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingIcon?.let {
            Icon(
                imageVector = it,
                contentDescription = trailingIconDescription,
                tint =
                    if (it == MaterialSymbols.Check) {
                        AppColors.Accent
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
    }
}
