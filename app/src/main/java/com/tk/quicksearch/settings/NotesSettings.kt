package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NotesMarkdownUtils
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.PhoneEmailLinkifiedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NotesSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { NotesRepository(context) }
    val listState = rememberLazyListState()
    var activeNoteId by remember { mutableLongStateOf(-1L) }
    var titleInput by remember { mutableStateOf(TextFieldValue("")) }
    var bodyInput by remember { mutableStateOf(TextFieldValue("")) }
    var refreshToken by remember { mutableLongStateOf(0L) }
    val notes by produceState(initialValue = emptyList<NoteInfo>(), refreshToken) {
        value = withContext(Dispatchers.IO) { repository.getAllNotes() }
    }

    LaunchedEffect(Unit) {
        val pendingId = NotesNavigationMemory.consumePendingNoteId()
        if (pendingId != null) {
            val note = withContext(Dispatchers.IO) { repository.getNoteById(pendingId) }
            if (note != null) {
                activeNoteId = note.noteId
                titleInput = TextFieldValue(note.title)
                bodyInput = TextFieldValue(note.markdownContent)
            }
        }
    }

    fun openEditor(note: NoteInfo?) {
        if (note == null) {
            activeNoteId = -1L
            titleInput = TextFieldValue("")
            bodyInput = TextFieldValue("")
        } else {
            activeNoteId = note.noteId
            titleInput = TextFieldValue(note.title)
            bodyInput = TextFieldValue(note.markdownContent)
        }
    }

    fun saveNote() {
        val title = titleInput.text.trim()
        val body = bodyInput.text
        if (title.isBlank() && body.isBlank()) {
            activeNoteId = -1L
            return
        }
        if (activeNoteId > 0L) {
            repository.updateNote(activeNoteId, title, body)
        } else {
            val created = repository.createNote(title = title, markdownContent = body)
            activeNoteId = created.noteId
        }
        refreshToken++
    }

    if (activeNoteId > 0L || titleInput.text.isNotBlank() || bodyInput.text.isNotBlank()) {
        NotesEditorSurface(
            titleInput = titleInput,
            bodyInput = bodyInput,
            onTitleChange = { titleInput = it },
            onBodyChange = { bodyInput = it },
            onBack = {
                saveNote()
                activeNoteId = -1L
                refreshToken++
            },
            onSave = { saveNote() },
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier) {
        FilledTonalButton(
            onClick = { openEditor(note = null) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.notes_create_note_cta),
                modifier = Modifier.padding(start = DesignTokens.SpacingSmall),
            )
        }

        SettingsCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = DesignTokens.SpacingMedium),
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
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                ) {
                    items(notes, key = { it.noteId }) { note ->
                        NoteListRow(
                            note = note,
                            isPinned = repository.isPinned(note.noteId),
                            onClick = { openEditor(note) },
                        )
                        HorizontalDivider(color = AppColors.SettingsDivider)
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteListRow(
    note: NoteInfo,
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall),
    ) {
        Text(
            text = if (note.title.isBlank()) stringResource(R.string.notes_untitled) else note.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = NotesMarkdownUtils.buildPreviewText(note.markdownContent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
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

@Composable
private fun NotesEditorSurface(
    titleInput: TextFieldValue,
    bodyInput: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    onBodyChange: (TextFieldValue) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = null,
                    )
                }
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                    )
                }
            }

            BasicTextField(
                value = titleInput,
                onValueChange = onTitleChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.CardHorizontalPadding),
                textStyle =
                    MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                singleLine = true,
                decorationBox = { inner ->
                    if (titleInput.text.isBlank()) {
                        Text(
                            text = stringResource(R.string.notes_title_hint),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )

            NotesEditorToolbar(
                bodyInput = bodyInput,
                onBodyChange = onBodyChange,
            )

            BasicTextField(
                value = bodyInput,
                onValueChange = onBodyChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.CardHorizontalPadding),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                decorationBox = { inner ->
                    if (bodyInput.text.isBlank()) {
                        Text(
                            text = stringResource(R.string.notes_body_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )

            Text(
                text = stringResource(R.string.notes_preview_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = DesignTokens.CardHorizontalPadding),
            )

            val context = LocalContext.current
            PhoneEmailLinkifiedText(
                text = NotesMarkdownUtils.toSearchablePlainText(bodyInput.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                linkColor = MaterialTheme.colorScheme.primary,
                onPhoneNumberClick = { number ->
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")),
                        )
                    }
                },
                onEmailClick = { email ->
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NotesEditorToolbar(
    bodyInput: TextFieldValue,
    onBodyChange: (TextFieldValue) -> Unit,
) {
    fun applyWrapper(prefix: String, suffix: String = prefix) {
        val selected = bodyInput.selection
        val text = bodyInput.text
        val start = selected.start.coerceIn(0, text.length)
        val end = selected.end.coerceIn(0, text.length)
        val next =
            text.substring(0, start) +
                prefix +
                text.substring(start, end) +
                suffix +
                text.substring(end)
        val cursor = (end + prefix.length + suffix.length).coerceAtMost(next.length)
        onBodyChange(
            TextFieldValue(
                text = next,
                selection = androidx.compose.ui.text.TextRange(cursor),
            ),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { applyWrapper("**") }) {
            Icon(imageVector = Icons.Rounded.FormatBold, contentDescription = null)
        }
        IconButton(onClick = { applyWrapper("*") }) {
            Icon(imageVector = Icons.Rounded.FormatItalic, contentDescription = null)
        }
        IconButton(onClick = { applyWrapper("# ", "") }) {
            Icon(imageVector = Icons.Rounded.Title, contentDescription = null)
        }
        IconButton(onClick = { applyWrapper("- ", "") }) {
            Icon(imageVector = Icons.Rounded.FormatListBulleted, contentDescription = null)
        }
        IconButton(onClick = { applyWrapper("1. ", "") }) {
            Icon(imageVector = Icons.Rounded.FormatListNumbered, contentDescription = null)
        }
        IconButton(onClick = { applyWrapper("[", "](https://)") }) {
            Icon(imageVector = Icons.Rounded.Link, contentDescription = null)
        }
    }
}
