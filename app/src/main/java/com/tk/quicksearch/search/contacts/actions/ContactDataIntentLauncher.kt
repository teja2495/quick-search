package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

internal fun launchContactDataIntent(
    context: Application,
    dataId: Long?,
    packageName: String? = null,
    mimeType: String? = null,
    configureIntent: Intent.() -> Unit = {},
): Boolean {
    if (dataId == null) return false

    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            val dataUri = Uri.parse("content://com.android.contacts/data/$dataId")
            if (mimeType != null) {
                setDataAndType(dataUri, mimeType)
            } else {
                data = dataUri
            }
            packageName?.let(::setPackage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            configureIntent()
        }

    if (intent.resolveActivity(context.packageManager) == null) {
        return false
    }
    context.startActivity(intent)
    return true
}

/**
 * Whether the contacts data row [dataId] still exists with [mimeType]. Sync adapters such as
 * WhatsApp can rewrite their rows, leaving cached IDs pointing at nothing; apps handed such an
 * ID close silently. Returns true when the check itself can't run, so launches aren't blocked.
 */
internal fun contactDataRowExists(
    context: Application,
    dataId: Long,
    mimeType: String,
): Boolean =
    try {
        context.contentResolver
            .query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(ContactsContract.Data._ID),
                "${ContactsContract.Data._ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(dataId.toString(), mimeType),
                null,
            )?.use { it.moveToFirst() } ?: true
    } catch (e: Exception) {
        Log.w("ContactDataIntent", "Could not verify contact data row $dataId", e)
        true
    }
