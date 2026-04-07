package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NoteEditor(
    onNavigateToNotes: () -> Unit,
    onDeleteToolbarState: (canDelete: Boolean, onConfirmedDelete: () -> Unit) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { NotesRepository(context) }

    var activeNoteId by remember { mutableLongStateOf(-1L) }
    var titleInput by remember { mutableStateOf(TextFieldValue("")) }
    var bodyInput by remember { mutableStateOf(TextFieldValue("")) }
    var contentBaseline by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isNewNoteEntry by remember { mutableStateOf(false) }
    val persistOnLeave = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val pendingId = NotesNavigationMemory.consumePendingNoteId()
        if (pendingId != null) {
            isNewNoteEntry = false
            val note = withContext(Dispatchers.IO) { repository.getNoteById(pendingId) }
            if (note != null) {
                activeNoteId = note.noteId
                titleInput = TextFieldValue(note.title)
                bodyInput = TextFieldValue(note.markdownContent)
            }
        } else {
            isNewNoteEntry = true
        }
        contentBaseline = titleInput.text to bodyInput.text
    }

    val hasEdits =
        contentBaseline?.let { (t, b) ->
            titleInput.text != t || bodyInput.text != b
        } == true

    val showActionButtons = isNewNoteEntry || hasEdits

    val noteScrollState = rememberScrollState()
    val bodyBringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollBodyToCaretScope = rememberCoroutineScope()

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
            if (persistOnLeave.value) {
                currentPersist()
            }
        }
    }

    LaunchedEffect(activeNoteId) {
        val id = activeNoteId
        if (id > 0L) {
            onDeleteToolbarState(true) {
                repository.stageDelete(id)
                repository.finalizeDelete(id)
                persistOnLeave.value = false
                onNavigateToNotes()
            }
        } else {
            onDeleteToolbarState(false) {}
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            onDeleteToolbarState(false) {}
        }
    }

    fun onSaveClick() {
        persistNote()
        persistOnLeave.value = false
        onNavigateToNotes()
    }

    fun onCancelClick() {
        persistOnLeave.value = false
        onNavigateToNotes()
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
    ) {
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (showActionButtons) {
                            Modifier
                        } else {
                            Modifier.padding(bottom = DesignTokens.CardBottomPadding)
                        },
                    ),
            shape = MaterialTheme.shapes.extraLarge,
            color = AppColors.getSettingsCardContainerColor(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(noteScrollState)
                            .padding(horizontal = DesignTokens.CardHorizontalPadding)
                            .padding(bottom = DesignTokens.CardBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    BasicTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = DesignTokens.SpacingLarge),
                        textStyle =
                            MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (titleInput.text.isBlank()) {
                                Text(
                                    text = stringResource(R.string.notes_title_hint),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.55f,
                                        ),
                                )
                            }
                            inner()
                        },
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = DesignTokens.SpacingSmall),
                        color = AppColors.SettingsDivider,
                    )

                    BasicTextField(
                        value = bodyInput,
                        onValueChange = { newValue ->
                            val previousNewlines = bodyInput.text.count { it == '\n' }
                            val nextNewlines = newValue.text.count { it == '\n' }
                            bodyInput = newValue
                            if (nextNewlines > previousNewlines) {
                                scrollBodyToCaretScope.launch {
                                    withFrameNanos { }
                                    noteScrollState.animateScrollTo(Int.MAX_VALUE)
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(bodyBringIntoViewRequester),
                        onTextLayout = { layoutResult ->
                            val offset =
                                bodyInput.selection.start.coerceIn(
                                    0,
                                    bodyInput.text.length,
                                )
                            val cursorRect = layoutResult.getCursorRect(offset)
                            scrollBodyToCaretScope.launch {
                                bodyBringIntoViewRequester.bringIntoView(cursorRect)
                            }
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
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.55f,
                                        ),
                                )
                            }
                            inner()
                        },
                    )
                }
                NoteEditorScrollIndicator(
                    scrollState = noteScrollState,
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = DesignTokens.SpacingXSmall)
                            .width(DesignTokens.SpacingXXSmall),
                )
            }
        }

        if (showActionButtons) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = DesignTokens.SpacingMedium),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                OutlinedButton(
                    onClick = ::onCancelClick,
                    modifier = Modifier.weight(1f).heightIn(min = DesignTokens.Spacing48),
                    shape = DesignTokens.ShapeXLarge,
                ) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
                Button(
                    onClick = ::onSaveClick,
                    modifier = Modifier.weight(1f).heightIn(min = DesignTokens.Spacing48),
                    enabled = titleInput.text.trim().isNotEmpty(),
                    shape = DesignTokens.ShapeXLarge,
                ) {
                    Text(text = stringResource(R.string.dialog_save))
                }
            }
        }
    }
}

@Composable
private fun NoteEditorScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var indicatorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(scrollState.value, scrollState.isScrollInProgress, scrollState.maxValue) {
        if (scrollState.maxValue <= 0) {
            indicatorVisible = false
            return@LaunchedEffect
        }
        indicatorVisible = true
        if (!scrollState.isScrollInProgress) {
            delay(700)
            indicatorVisible = false
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (indicatorVisible && scrollState.maxValue > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "noteScrollIndicator",
    )
    if (alpha <= 0.01f) return
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val thumbColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    Canvas(
        modifier =
            modifier.graphicsLayer {
                this.alpha = alpha
            },
    ) {
        val w = size.width
        val h = size.height
        val vp = scrollState.viewportSize
        val maxS = scrollState.maxValue
        if (maxS <= 0 || vp <= 0 || h <= 0f) return@Canvas
        val minThumbPx = with(density) { 24.dp.toPx() }
        val contentExtent = maxS + vp
        val thumbH = (vp.toFloat() / contentExtent * h).coerceIn(minThumbPx, h)
        val travel = (h - thumbH).coerceAtLeast(0f)
        val t = scrollState.value.toFloat() / maxS.toFloat()
        val thumbY = t * travel
        val radius = CornerRadius(w / 2f, w / 2f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, thumbY),
            size = Size(w, thumbH),
            cornerRadius = radius,
        )
    }
}
