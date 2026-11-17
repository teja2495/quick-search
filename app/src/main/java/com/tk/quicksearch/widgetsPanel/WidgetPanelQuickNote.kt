package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.search.searchScreen.LocalFrostedCardSource
import com.tk.quicksearch.search.searchScreen.frostedCardBackground
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.max

private const val QUICK_NOTE_SAVE_DELAY_MS = 450L
private const val QUICK_NOTE_MINIMUM_CONTAINER_ALPHA = 0.78f
private val QuickNoteHeight = 164.dp
private val QuickNoteFocusedHeight = 280.dp

@Composable
internal fun CompactQuickNoteWidget(
    modifier: Modifier = Modifier,
    fitContentHeight: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (x: Float, y: Float) -> Unit = { _, _ -> },
    onDragEnd: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember(context) { NotesRepository(context) }
    val linkColor = AppColors.LinkColor
    var bodyInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var baselineBody by rememberSaveable { mutableStateOf<String?>(null) }
    var quickNoteId by rememberSaveable { mutableStateOf(-1L) }
    var isFocused by rememberSaveable { mutableStateOf(false) }
    val quickNoteHeight by animateDpAsState(
        targetValue = if (isFocused) QuickNoteFocusedHeight else QuickNoteHeight,
        label = "quickNoteHeight",
    )
    val surfaceModifier =
        if (fitContentHeight) {
            modifier
                .fillMaxWidth()
                .heightIn(min = QuickNoteHeight)
        } else {
            modifier.height(quickNoteHeight)
        }

    LaunchedEffect(Unit) {
        val note = withContext(Dispatchers.IO) { repository.getOrCreateQuickNote() }
        quickNoteId = note.noteId
        bodyInput =
            TextFieldValue(
                annotatedString =
                    NotesTextUtils.buildLinkHighlightedAnnotatedString(
                        note.markdownContent,
                        linkColor,
                    ),
                selection = TextRange(note.markdownContent.length),
            )
        baselineBody = note.markdownContent
    }

    LaunchedEffect(quickNoteId, bodyInput.text, baselineBody) {
        val id = quickNoteId
        val baseline = baselineBody ?: return@LaunchedEffect
        if (id <= 0L || bodyInput.text == baseline) return@LaunchedEffect
        delay(QUICK_NOTE_SAVE_DELAY_MS)
        withContext(Dispatchers.IO) {
            repository.updateNote(
                noteId = id,
                title = context.getString(R.string.notes_quick_note_title),
                markdownContent = bodyInput.text,
            )
        }
        baselineBody = bodyInput.text
    }

    val currentBody by rememberUpdatedState(bodyInput.text)
    val currentBaseline by rememberUpdatedState(baselineBody)
    DisposableEffect(quickNoteId) {
        onDispose {
            val id = quickNoteId
            val baseline = currentBaseline
            if (id > 0L && baseline != null && currentBody != baseline) {
                repository.updateNote(
                    noteId = id,
                    title = context.getString(R.string.notes_quick_note_title),
                    markdownContent = currentBody,
                )
            }
        }
    }

    // Over a frosted wallpaper the note matches search result cards: frost behind a lighter scrim.
    val isFrosted = LocalFrostedCardSource.current?.isActive == true
    Surface(
        modifier = surfaceModifier.frostedCardBackground(AppShapes.widgetPanelCard),
        shape = AppShapes.widgetPanelCard,
        color =
            if (isFrosted) {
                SearchResultCardDefaults.containerColor(
                    showWallpaperBackground = true,
                    overlayContainerColor = null,
                )
            } else {
                AppColors.getSettingsCardContainerColor().let { containerColor ->
                    containerColor.copy(alpha = max(containerColor.alpha, QUICK_NOTE_MINIMUM_CONTAINER_ALPHA))
                }
            },
    ) {
        Column(
            modifier =
                (if (fitContentHeight) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
                    .padding(DesignTokens.SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            Modifier.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { onDragStart() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        onDrag(dragAmount.x, dragAmount.y)
                                    },
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragEnd,
                                )
                            },
                        ),
            ) {
                Text(
                    text = stringResource(R.string.notes_quick_note_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            BasicTextField(
                value = bodyInput,
                onValueChange = {
                    bodyInput =
                        it.copy(
                            annotatedString =
                                NotesTextUtils.updateLinkHighlightedAnnotatedString(
                                    bodyInput.annotatedString,
                                    it.text,
                                    linkColor,
                                ),
                        )
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (fitContentHeight) {
                                Modifier.verticalScroll(rememberScrollState())
                            } else {
                                Modifier
                            },
                        )
                        .onFocusChanged {
                            isFocused = it.isFocused
                            onFocusChanged(it.isFocused)
                        },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (bodyInput.text.isBlank()) {
                        Text(
                            text = stringResource(R.string.notes_body_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            // Dimmed: the card is darker than a standard surface over the wallpaper,
                            // so full onSurfaceVariant reads almost as bright as typed text.
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AppColors.ContentSecondaryAlpha),
                        )
                    }
                    inner()
                },
            )
        }
    }
}
