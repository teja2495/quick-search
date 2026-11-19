package com.tk.quicksearch.settings.notesSettings

import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.tk.quicksearch.search.notes.NoteLinkOutputTransformation
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.notes.NotesTextUtils
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

private const val NOTE_EDITOR_BACK_SWIPE_THRESHOLD_PX = 140f
private const val NOTE_EDITOR_TITLE_FIELD = "title"
private const val NOTE_EDITOR_BODY_FIELD = "body"

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

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
    state: TextFieldState,
    linkHighlighter: NoteLinkOutputTransformation,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    cursorBrush: Brush,
    lineLimits: TextFieldLineLimits,
    onTextLayout: (TextLayoutResult) -> Unit,
    decorator: TextFieldDecorator,
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
            state = state,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .pointerInput(state, textLayoutResult) {
                        detectTapGestures(
                            onTap = { tapOffset ->
                                val layout = textLayoutResult ?: return@detectTapGestures
                                focusRequester.requestFocus()
                                val charOffset = layout.getOffsetForPosition(tapOffset)
                                val url = linkHighlighter.linkUrlAt(state.text.toString(), charOffset)
                                if (url != null) {
                                    linkMenu =
                                        url to
                                            DpOffset(
                                                x = with(density) { tapOffset.x.toDp() },
                                                y = with(density) { tapOffset.y.toDp() },
                                            )
                                } else {
                                    state.edit { selection = TextRange(charOffset) }
                                }
                            },
                        )
                    },
            textStyle = textStyle,
            cursorBrush = cursorBrush,
            readOnly = readOnly,
            lineLimits = lineLimits,
            outputTransformation = linkHighlighter,
            onTextLayout = { getResult ->
                getResult()?.let { result ->
                    textLayoutResult = result
                    onTextLayout(result)
                }
            },
            decorator = decorator,
        )

        linkMenu?.let { (url, offset) ->
            AppDropdownMenu(
                expanded = true,
                onDismissRequest = { linkMenu = null },
                offset = offset,
                focusable = true,
            ) {
                val labelRes =
                    when {
                        url.startsWith("tel:", ignoreCase = true) ->
                            R.string.contact_method_call_label
                        url.startsWith("mailto:", ignoreCase = true) ->
                            R.string.notes_editor_action_send_email
                        else -> R.string.notes_editor_action_open_link
                    }
                AppDropdownMenuItem(
                    text = stringResource(labelRes),
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
    onToolbarState: (NoteEditorToolbarState) -> Unit = {},
    hideTopBar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val linkColor = MaterialTheme.colorScheme.primary
    val repository = remember(context) { NotesRepository(context) }

    var activeNoteId by rememberSaveable { mutableStateOf(-1L) }
    val titleState = rememberTextFieldState()
    val bodyState = rememberTextFieldState()
    val titleText = titleState.text.toString()
    val bodyText = bodyState.text.toString()
    val titleLinkHighlighter = remember(linkColor) { NoteLinkOutputTransformation(linkColor) }
    val bodyLinkHighlighter = remember(linkColor) { NoteLinkOutputTransformation(linkColor) }
    var contentBaselineTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var contentBaselineBody by rememberSaveable { mutableStateOf<String?>(null) }
    var isNewNoteEntry by rememberSaveable { mutableStateOf(false) }
    var isQuickNote by rememberSaveable { mutableStateOf(false) }
    var notificationPinRequested by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var isPinnedToNotification by rememberSaveable { mutableStateOf(false) }
    var editorInitialized by rememberSaveable { mutableStateOf(false) }
    var focusedEditorField by rememberSaveable { mutableStateOf<String?>(null) }
    var initialFocusRequested by remember { mutableStateOf(false) }
    val persistOnLeave = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (editorInitialized) return@LaunchedEffect
        val pendingId = NotesNavigationMemory.consumePendingNoteId()
        val pinToNotificationRequest = NotesNavigationMemory.consumePinToNotificationRequest()
        if (pendingId != null) {
            isNewNoteEntry = false
            val note = withContext(Dispatchers.IO) { repository.getNoteById(pendingId) }
            if (note != null) {
                activeNoteId = note.noteId
                isQuickNote = repository.isQuickNote(note.noteId)
                isPinnedToNotification =
                    PinnedNotifications.isPinned(
                        context,
                        noteNotificationAction(context, note.noteId, note.title, note.markdownContent),
                    )
                titleState.setTextAndPlaceCursorAtEnd(note.title)
                bodyState.setTextAndPlaceCursorAtEnd(note.markdownContent)
            } else {
                isQuickNote = false
            }
        } else {
            isNewNoteEntry = true
            isQuickNote = false
            notificationPinRequested = pinToNotificationRequest
        }
        contentBaselineTitle = titleState.text.toString()
        contentBaselineBody = bodyState.text.toString()
        editorInitialized = true
    }

    val hasEdits =
        if (contentBaselineTitle != null && contentBaselineBody != null) {
            titleText != contentBaselineTitle || bodyText != contentBaselineBody
        } else {
            false
        }

    val noteScrollState = rememberScrollState()
    val bodyBringIntoViewRequester = remember { BringIntoViewRequester() }
    val scrollBodyToCaretScope = rememberCoroutineScope()
    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(editorInitialized, contentBaselineTitle, contentBaselineBody, hideTopBar) {
        if (!editorInitialized || initialFocusRequested) return@LaunchedEffect
        if (contentBaselineTitle == null || contentBaselineBody == null) return@LaunchedEffect
        delay(50)
        when (focusedEditorField) {
            NOTE_EDITOR_TITLE_FIELD -> titleFocusRequester.requestFocus()
            NOTE_EDITOR_BODY_FIELD -> bodyFocusRequester.requestFocus()
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

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) notificationPinRequested = false
        }
    LaunchedEffect(notificationPinRequested) {
        if (!notificationPinRequested || notificationPermissionRequested) return@LaunchedEffect
        if (!canPostNotifications(context)) {
            notificationPermissionRequested = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Autosave writes on IO while the leave-save writes on Main; one lock and this id keep them from
    // both creating the same new note.
    val persistLock = remember { Any() }
    val persistedNoteId = remember { AtomicLong(activeNoteId) }

    /** Writes the note and returns its id, or -1 when there is nothing to save. Blocking. */
    fun writeNote(title: String, body: String): Long {
        if (title.isBlank() && body.isBlank()) return -1L
        synchronized(persistLock) {
            val id = persistedNoteId.get().takeIf { it > 0L } ?: activeNoteId
            if (id > 0L) {
                repository.updateNote(id, title, body)
                persistedNoteId.set(id)
                return id
            }
            val created = repository.createNote(title = title, markdownContent = body)
            persistedNoteId.set(created.noteId)
            return created.noteId
        }
    }

    /** Applies a finished [writeNote] to the editor state and posts a requested notification pin. */
    fun onNotePersisted(noteId: Long, title: String, body: String) {
        if (noteId <= 0L) return
        activeNoteId = noteId
        if (notificationPinRequested && canPostNotifications(context)) {
            PinnedNotifications.pin(
                context,
                noteNotificationAction(context, noteId, title, body),
                PinnedNotifications.PinMode.SEPARATELY,
            )
            notificationPinRequested = false
            isPinnedToNotification = true
        }
    }

    fun persistNote() {
        val title = titleState.text.toString().trim()
        val body = bodyState.text.toString()
        onNotePersisted(writeNote(title, body), title, body)
    }

    LaunchedEffect(hasEdits, titleText, bodyText) {
        if (!hasEdits) return@LaunchedEffect
        delay(450)
        val title = titleText.trim()
        // A newer keystroke restarts this effect; let a started write finish so its new note id is
        // kept, and the next save updates that note instead of creating another.
        withContext(NonCancellable) {
            val noteId = withContext(Dispatchers.IO) { writeNote(title, bodyText) }
            onNotePersisted(noteId, title, bodyText)
        }
        contentBaselineTitle = titleText
        contentBaselineBody = bodyText
    }

    val currentPersist by rememberUpdatedState(newValue = ::persistNote)
    DisposableEffect(Unit) {
        onDispose {
            if (persistOnLeave.value && context.findActivity()?.isChangingConfigurations != true) {
                currentPersist()
            }
        }
    }

    LaunchedEffect(activeNoteId, isQuickNote, notificationPinRequested, isPinnedToNotification) {
        val id = activeNoteId
        onToolbarState(
            NoteEditorToolbarState(
                onConfirmedDelete =
                    if (id > 0L && !isQuickNote) {
                        {
                            repository.stageDelete(id)
                            repository.finalizeDelete(id)
                            persistOnLeave.value = false
                            onNavigateToNotes()
                        }
                    } else {
                        null
                    },
                isNotificationNote = notificationPinRequested || isPinnedToNotification,
            ),
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            onToolbarState(NoteEditorToolbarState())
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
            shape = AppShapes.extraLarge,
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
                    LinkStyledNoteTextField(
                        state = titleState,
                        linkHighlighter = titleLinkHighlighter,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = DesignTokens.SpacingLarge),
                        textStyle =
                            MaterialTheme.typography.headlineSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        focusRequester = titleFocusRequester,
                        density = density,
                        context = context,
                        readOnly = isQuickNote,
                        onFocusChanged = { isFocused ->
                            if (isFocused) focusedEditorField = NOTE_EDITOR_TITLE_FIELD
                        },
                        onTextLayout = {},
                        decorator = { inner ->
                            if (titleText.isBlank()) {
                                Text(
                                    text = stringResource(R.string.notes_title_hint),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = AppColors.ContentMutedAlpha,
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

                    LinkStyledNoteTextField(
                        state = bodyState,
                        linkHighlighter = bodyLinkHighlighter,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(bodyBringIntoViewRequester),
                        onTextLayout = { layoutResult ->
                            val offset =
                                bodyState.selection.start.coerceIn(
                                    0,
                                    bodyState.text.length,
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
                        lineLimits = TextFieldLineLimits.MultiLine(),
                        focusRequester = bodyFocusRequester,
                        density = density,
                        context = context,
                        readOnly = false,
                        onFocusChanged = { isFocused ->
                            if (isFocused) focusedEditorField = NOTE_EDITOR_BODY_FIELD
                        },
                        decorator = { inner ->
                            if (bodyText.isBlank()) {
                                Text(
                                    text = stringResource(R.string.notes_body_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = AppColors.ContentMutedAlpha,
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
                            .width(DesignTokens.SpacingXSmall),
                )
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
        animationSpec = tween(durationMillis = AppMotion.DurationShort, easing = AppMotion.EmphasizedEasing),
        label = "noteScrollIndicator",
    )
    if (alpha <= 0.01f) return
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = AppColors.TintStrongAlpha)
    val thumbColor = MaterialTheme.colorScheme.outline.copy(alpha = AppColors.ContentMutedAlpha)
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
