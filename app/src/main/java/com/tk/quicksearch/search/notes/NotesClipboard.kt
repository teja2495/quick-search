package com.tk.quicksearch.search.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.tk.quicksearch.R

/** Copies a note or snippet body to the clipboard. */
fun copyNoteContentToClipboard(
    context: Context,
    content: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(
        ClipData.newPlainText(context.getString(R.string.action_copy_content), content),
    )
    // Android 13+ shows its own clipboard confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.notes_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}
