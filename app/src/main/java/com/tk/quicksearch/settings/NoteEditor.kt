package com.tk.quicksearch.settings.settingsDetailScreen

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.searchScreen.LockScreenAccessibilityService
import com.tk.quicksearch.shared.permissions.SnippetIntroDialog
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.search.notes.copyNoteContentToClipboard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NOTE_EDITOR_BACK_SWIPE_THRESHOLD_PX = 140f
private const val NOTE_EDITOR_TITLE_FIELD = "title"
private const val NOTE_EDITOR_BODY_FIELD = "body"
private const val NOTE_EDITOR_KEYWORD_FIELD = "keyword"

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun applyNoteLinkHighlighting(
    value: TextFieldValue,
    linkColor: Color,
): TextFieldValue =
    value.copy(
        annotatedString =
            NotesTextUtils.buildLinkHighlightedAnnotatedString(value.text, linkColor),
    )

private fun launchNoteLink(
    context: Context,
    url: String,
) {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
    val intent =
        when {
            uri.scheme.equals("tel", ignoreCase = true) -> Intent(Intent.ACTION_DIAL, uri)
            uri.scheme.equals("mailto", ignoreCase = true) -> Intent(Intent.ACTION_SENDTO, uri)
            else -> Intent(Intent.ACTION_VIEW, uri)
        }
    runCatching { context.startActivity(intent) }
}

@Composable
private fun LinkStyledNoteTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    cursorBrush: Brush,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    onTextLayout: (TextLayoutResult) -> Unit,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
    focusRequester: FocusRequester,
    density: Density,
    context: Context,
    readOnly: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var linkMenu by remember { mutableStateOf<Pair<String, DpOffset>?>(null) }

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .pointerInput(value.text, textLayoutResult) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                focusRequester.requestFocus()
                                val charOffset = layout.getOffsetForPosition(tapOffset)
                                val url =
                                    NotesTextUtils.linkUrlAtCharOffset(
                                        value.annotatedString,
                                        charOffset,
                                    )
                                if (url != null) {
                                    linkMenu =
                                        url to
                                            DpOffset(
                                                x = with(density) { tapOffset.x.toDp() },
                                                y = with(density) { tapOffset.y.toDp() },
                                            )
                                } else {
                                    onValueChange(
                                        value.copy(selection = TextRange(charOffset, charOffset)),
                                    )
                                }
                            },
                        )
                    },
            textStyle = textStyle,
            cursorBrush = cursorBrush,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = { result ->
                textLayoutResult = result
                onTextLayout(result)
            },
            decorationBox = decorationBox,
        )

        linkMenu?.let { (url, offset) ->
            DropdownMenu(
                expanded = true,
                onDismissRequest = { linkMenu = null },
                offset = offset,
            ) {
                val labelRes =
                    when {
                        url.startsWith("tel:", ignoreCase = true) ->
                            R.string.contact_method_call_label
                        url.startsWith("mailto:", ignoreCase = true) ->
                            R.string.notes_editor_action_send_email
                        else -> R.string.notes_editor_action_open_link
                    }
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        linkMenu = null
                        launchNoteLink(context, url)
                    },
                )
            }
        }
    }
}

@Composable
fun NoteEditor(
    onNavigateToNotes: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onDeleteToolbarState: (canDelete: Boolean, onConfirmedDelete: () -> Unit) -> Unit = { _, _ -> },
    onSnippetModeResolved: (isSnippet: Boolean) -> Unit = {},
    hideTopBar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val linkColor = MaterialTheme.colorScheme.primary
    val repository = remember(context) { NotesRepository(context) }
    val uiPreferences = remember(context) { UiPreferences(context) }

    var activeNoteId by rememberSaveable { mutableStateOf(-1L) }
    var titleInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var bodyInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var keywordInput by rememberSaveable { mutableStateOf("") }
    var isSnippet by rememberSaveable { mutableStateOf(false) }
    var keywordTaken by remember { mutableStateOf(false) }
    var contentBaselineTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var contentBaselineBody by rememberSaveable { mutableStateOf<String?>(null) }
    var contentBaselineKeyword by rememberSaveable { mutableStateOf<String?>(null) }
    var isNewNoteEntry by rememberSaveable { mutableStateOf(false) }
    var isQuickNote by rememberSaveable { mutableStateOf(false) }
    var editorInitialized by rememberSaveable { mutableStateOf(false) }
    var focusedEditorField by rememberSaveable { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember { mutableStateOf(false) }
    var showSnippetIntro by rememberSaveable { mutableStateOf(false) }
    var snippetIntroChecked by rememberSaveable { mutableStateOf(false) }
    val persistOnLeave = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (editorInitialized) {
            onSnippetModeResolved(isSnippet)
            return@LaunchedEffect
        }
        val pendingId = NotesNavigationMemory.consumePendingNoteId()
        val pendingIsSnippet = NotesNavigationMemory.consumePendingIsSnippet()
        if (pendingId != null) {
            isNewNoteEntry = false
            val note = withContext(Dispatchers.IO) { repository.getNoteById(pendingId) }
            if (note != null) {
                activeNoteId = note.noteId
                isQuickNote = repository.isQuickNote(note.noteId)
                isSnippet = note.isSnippet
                keywordInput = note.keyword
                titleInput =
                    applyNoteLinkHighlighting(
                        TextFieldValue(
                            text = note.title,
                            selection = TextRange(note.title.length),
                        ),
                        linkColor,
                    )
                bodyInput =
                    applyNoteLinkHighlighting(
                        TextFieldValue(
                            text = note.markdownContent,
                            selection = TextRange(note.markdownContent.length),
                        ),
                        linkColor,
                    )
            } else {
                isQuickNote = false
            }
        } else {
            isNewNoteEntry = true
            isQuickNote = false
            isSnippet = pendingIsSnippet
        }
        contentBaselineTitle = titleInput.text
        contentBaselineBody = bodyInput.text
        contentBaselineKeyword = keywordInput
        editorInitialized = true
        onSnippetModeResolved(isSnippet)
    }

    // Gated on editorInitialized because isSnippet is only resolved by the effect above.
    LaunchedEffect(editorInitialized) {
        if (!editorInitialized || snippetIntroChecked) return@LaunchedEffect
        snippetIntroChecked = true
        if (isNewNoteEntry && isSnippet && !uiPreferences.hasSeenSnippetIntro()) {
            showSnippetIntro = true
        }
    }

    LaunchedEffect(isSnippet, keywordInput, activeNoteId) {
        if (!isSnippet || keywordInput.isBlank()) {
            keywordTaken = false
            return@LaunchedEffect
        }
        val excludeId = activeNoteId
        keywordTaken =
            withContext(Dispatchers.IO) { repository.isSnippetKeywordTaken(keywordInput, excludeId) }
    }

    val hasEdits =
        if (contentBaselineTitle != null && contentBaselineBody != null) {
            titleInput.text != contentBaselineTitle ||
                bodyInput.text != contentBaselineBody ||
                keywordInput != contentBaselineKeyword
        } else {
            false
        }

    val noteScrollState = rememberScrollState()
    val bodyBringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollBodyToCaretScope = rememberCoroutineScope()
    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val keywordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(editorInitialized, contentBaselineTitle, contentBaselineBody, hideTopBar, showSnippetIntro) {
        if (!editorInitialized || initialFocusRequested) return@LaunchedEffect
        // Otherwise the keyboard pops up behind the intro dialog and shifts the layout under it.
        if (showSnippetIntro) return@LaunchedEffect
        if (contentBaselineTitle == null || contentBaselineBody == null) return@LaunchedEffect
        delay(50)
        when (focusedEditorField) {
            NOTE_EDITOR_TITLE_FIELD -> titleFocusRequester.requestFocus()
            NOTE_EDITOR_BODY_FIELD -> bodyFocusRequester.requestFocus()
            NOTE_EDITOR_KEYWORD_FIELD ->
                if (isSnippet) keywordFocusRequester.requestFocus() else bodyFocusRequester.requestFocus()
            null ->
                when {
                    hideTopBar -> bodyFocusRequester.requestFocus()
                    isNewNoteEntry -> titleFocusRequester.requestFocus()
                    else -> return@LaunchedEffect
                }
        }
        initialFocusRequested = true
        keyboardController?.show()
    }

    fun persistNote() {
        val title = titleInput.text.trim()
        val body = bodyInput.text
        val keyword = keywordInput.trim()
        if (title.isBlank() && body.isBlank() && (!isSnippet || keyword.isBlank())) return

        // A keyword already used by another snippet is never saved; the stored keyword is kept instead.
        val savableKeyword =
            if (isSnippet && !repository.isSnippetKeywordTaken(keyword, activeNoteId)) keyword else null
        if (activeNoteId <= 0L && title.isBlank() && body.isBlank() && savableKeyword.isNullOrBlank()) return
        if (activeNoteId > 0L) {
            repository.updateNote(activeNoteId, title, body, savableKeyword)
        } else {
            val created =
                repository.createNote(
                    title = title,
                    markdownContent = body,
                    isSnippet = isSnippet,
                    keyword = savableKeyword.orEmpty(),
                )
            activeNoteId = created.noteId
        }
    }

    LaunchedEffect(hasEdits, titleInput.text, bodyInput.text, keywordInput) {
        if (!hasEdits) return@LaunchedEffect
        delay(450)
        persistNote()
        contentBaselineTitle = titleInput.text
        contentBaselineBody = bodyInput.text
        contentBaselineKeyword = keywordInput
    }

    val currentPersist by rememberUpdatedState(newValue = ::persistNote)
    DisposableEffect(Unit) {
        onDispose {
            if (persistOnLeave.value && context.findActivity()?.isChangingConfigurations != true) {
                currentPersist()
            }
        }
    }

    LaunchedEffect(activeNoteId, isQuickNote) {
        val id = activeNoteId
        if (id > 0L && !isQuickNote) {
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

    val noteEditorSwipeModifier =
        Modifier.pointerInput(onNavigateToSearch) {
            var totalHorizontalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalHorizontalDrag = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    totalHorizontalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalHorizontalDrag <= -NOTE_EDITOR_BACK_SWIPE_THRESHOLD_PX) {
                        onNavigateToSearch()
                    }
                    totalHorizontalDrag = 0f
                },
                onDragCancel = { totalHorizontalDrag = 0f },
            )
        }

    if (showSnippetIntro) {
        fun acknowledgeSnippetIntro() {
            uiPreferences.setHasSeenSnippetIntro(true)
            showSnippetIntro = false
        }
        SnippetIntroDialog(
            isAccessibilityEnabled = LockScreenAccessibilityService.isEnabled(context),
            onGrantPermission = {
                acknowledgeSnippetIntro()
                runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            },
            onContinueWithout = { acknowledgeSnippetIntro() },
        )
    }

    Column(
        modifier =
            modifier
                .then(noteEditorSwipeModifier)
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding(),
    ) {
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = if (hideTopBar) DesignTokens.SpacingLarge else 0.dp)
                    .padding(bottom = DesignTokens.CardBottomPadding),
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
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = DesignTokens.SpacingLarge),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinkStyledNoteTextField(
                            value = titleInput,
                            onValueChange = {
                                if (!isQuickNote) {
                                    titleInput = applyNoteLinkHighlighting(it, linkColor)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            textStyle =
                                MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            maxLines = 1,
                            minLines = 1,
                            focusRequester = titleFocusRequester,
                            density = density,
                            context = context,
                            readOnly = isQuickNote,
                            onFocusChanged = { isFocused ->
                                if (isFocused) focusedEditorField = NOTE_EDITOR_TITLE_FIELD
                            },
                            onTextLayout = {},
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
                        if (isSnippet) {
                            IconButton(
                                onClick = { copyNoteContentToClipboard(context, bodyInput.text) },
                                enabled = bodyInput.text.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(R.string.notes_copy_to_clipboard_desc),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (isSnippet) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = DesignTokens.SpacingSmall),
                            color = AppColors.SettingsDivider,
                        )
                        SnippetKeywordField(
                            value = keywordInput,
                            onValueChange = { keywordInput = it },
                            isTaken = keywordTaken,
                            focusRequester = keywordFocusRequester,
                            onFocused = { focusedEditorField = NOTE_EDITOR_KEYWORD_FIELD },
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = DesignTokens.SpacingSmall),
                        color = AppColors.SettingsDivider,
                    )

                    LinkStyledNoteTextField(
                        value = bodyInput,
                        onValueChange = { newValue ->
                            bodyInput = applyNoteLinkHighlighting(newValue, linkColor)
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
                        singleLine = false,
                        maxLines = Int.MAX_VALUE,
                        minLines = 1,
                        focusRequester = bodyFocusRequester,
                        density = density,
                        context = context,
                        readOnly = false,
                        onFocusChanged = { isFocused ->
                            if (isFocused) focusedEditorField = NOTE_EDITOR_BODY_FIELD
                        },
                        decorationBox = { inner ->
                            if (bodyInput.text.isBlank()) {
                                Text(
                                    text =
                                        stringResource(
                                            if (isSnippet) {
                                                R.string.notes_snippet_body_hint
                                            } else {
                                                R.string.notes_body_hint
                                            },
                                        ),
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
    }
}

@Composable
private fun SnippetKeywordField(
    value: String,
    onValueChange: (String) -> Unit,
    isTaken: Boolean,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.notes_snippet_keyword_prefix),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = value,
                // Keywords are a single word, so whitespace is dropped as it is typed or pasted.
                onValueChange = { onValueChange(it.filterNot(Char::isWhitespace)) },
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { if (it.isFocused) onFocused() },
                textStyle =
                    MaterialTheme.typography.titleMedium.copy(
                        color =
                            if (isTaken) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.notes_snippet_keyword_hint),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        )
                    }
                    inner()
                },
            )
        }
        if (isTaken) {
            Text(
                text = stringResource(R.string.notes_snippet_keyword_taken),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
