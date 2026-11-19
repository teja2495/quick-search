package com.tk.quicksearch.settings.notesSettings

import com.tk.quicksearch.shared.ui.components.AppIconButton
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction

/** Top-bar state the note editor publishes to its host screen; a null delete action hides it. */
data class NoteEditorToolbarState(
    val onConfirmedDelete: (() -> Unit)? = null,
    val isNotificationNote: Boolean = false,
)

internal fun noteEditorTitleRes(toolbar: NoteEditorToolbarState): Int =
    if (toolbar.isNotificationNote) {
        R.string.notes_editor_notification_note_title
    } else {
        R.string.notes_editor_title
    }

internal fun noteEditorHeaderActions(
    toolbar: NoteEditorToolbarState,
    onDeleteClick: () -> Unit,
): (@Composable () -> Unit)? {
    if (toolbar.onConfirmedDelete == null) return null
    return {
        AppIconButton(onClick = onDeleteClick, tooltip = stringResource(R.string.notes_delete_note_desc)) {
            Icon(
                imageVector = MaterialSymbols.DeleteOutline,
                contentDescription = stringResource(R.string.notes_delete_note_desc),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

internal fun noteNotificationAction(
    context: Context,
    noteId: Long,
    title: String,
    markdownContent: String,
): CustomWidgetButtonAction.Note =
    CustomWidgetButtonAction.Note(
        noteId = noteId,
        title = title.ifBlank { context.getString(R.string.notes_untitled) },
        markdownContent = markdownContent,
    )

internal fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
