package com.tk.quicksearch.search.noted

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tk.quicksearch.search.models.NotedItem

/** Opens a Noted item in the Noted app via `noted://<kind>/<id>`. */
internal object NotedLauncher {
    /** Returns false when Noted could not be opened. */
    fun open(
        context: Context,
        item: NotedItem,
    ): Boolean = open(context, item.kind, item.id)

    fun open(
        context: Context,
        kind: NotedItem.Kind,
        id: String,
    ): Boolean {
        if (!NotedIntegration.ensureChecked(context)) return false
        val uri = Uri.parse("${NotedIntegration.DEEP_LINK_SCHEME}://${kind.raw}/$id")
        val intent =
            Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(NotedIntegration.packageName ?: return false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
