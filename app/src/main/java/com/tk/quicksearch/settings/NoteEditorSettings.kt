package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.notes.NotesMarkdownUtils
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.PhoneEmailLinkifiedText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NoteEditorSettingsSection(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { NotesRepository(context) }

    var activeNoteId by remember { mutableLongStateOf(-1L) }
    var titleInput by remember { mutableStateOf(TextFieldValue("")) }
    var bodyInput by remember { mutableStateOf(TextFieldValue("")) }

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

    fun persistNote() {
        val title = titleInput.text.trim()
        val body = bodyInput.text
        if (title.isBlank() && body.isBlank()) return

        if (activeNoteId > 0L) {
            repository.updateNote(activeNoteId, title, body)
        } else {
            val created = repository.createNote(title = title, markdownContent = body)
            activeNoteId = created.noteId
        }
    }

    val currentPersist by rememberUpdatedState(newValue = ::persistNote)
    DisposableEffect(Unit) {
        onDispose {
            currentPersist()
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = DesignTokens.CardHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                BasicTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = DesignTokens.SpacingSmall),
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

                BasicTextField(
                    value = bodyInput,
                    onValueChange = { bodyInput = it },
                    modifier = Modifier.fillMaxWidth(),
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
                )

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

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = DesignTokens.SpacingSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotesEditorToolbarIcon(
                        imageVector = Icons.Rounded.FormatBold,
                        onClick = {
                            bodyInput = bodyInput.wrapSelection(prefix = "**")
                        },
                    )
                    NotesEditorToolbarIcon(
                        imageVector = Icons.Rounded.FormatItalic,
                        onClick = {
                            bodyInput = bodyInput.wrapSelection(prefix = "*")
                        },
                    )
                    NotesEditorToolbarIcon(
                        imageVector = Icons.Rounded.Title,
                        onClick = {
                            bodyInput = bodyInput.wrapSelection(prefix = "# ", suffix = "")
                        },
                    )
                    NotesEditorToolbarIcon(
                        imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                        onClick = {
                            bodyInput = bodyInput.wrapSelection(prefix = "- ", suffix = "")
                        },
                    )
                    NotesEditorToolbarIcon(
                        imageVector = Icons.Rounded.FormatListNumbered,
                        onClick = {
                            bodyInput = bodyInput.wrapSelection(prefix = "1. ", suffix = "")
                        },
                    )
                    NotesEditorToolbarIcon(
                        imageVector = Icons.Rounded.Link,
                        onClick = {
                            bodyInput = bodyInput.wrapSelection(prefix = "[", suffix = "](https://)")
                        },
                    )
                }

                IconButton(onClick = { persistNote() }) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.dialog_save),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesEditorToolbarIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(imageVector = imageVector, contentDescription = null)
    }
}

private fun TextFieldValue.wrapSelection(prefix: String, suffix: String = prefix): TextFieldValue {
    val textValue = text
    val start = selection.start.coerceIn(0, textValue.length)
    val end = selection.end.coerceIn(0, textValue.length)
    val nextText =
        textValue.substring(0, start) +
            prefix +
            textValue.substring(start, end) +
            suffix +
            textValue.substring(end)
    val cursor = (end + prefix.length + suffix.length).coerceAtMost(nextText.length)
    return TextFieldValue(text = nextText, selection = TextRange(cursor))
}
