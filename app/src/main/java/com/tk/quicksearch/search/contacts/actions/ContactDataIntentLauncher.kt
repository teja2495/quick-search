package com.tk.quicksearch.search.contacts.actions

import android.app.Application
import android.content.Intent
import android.net.Uri

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
