package com.tk.quicksearch.search.bookmarks

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.tk.quicksearch.search.core.intentHelpers.IntentHelpers
import com.tk.quicksearch.search.data.bookmarks.BookmarkUrls
import com.tk.quicksearch.search.models.BookmarkInfo

/** Opens a bookmark in its chosen browser, or the system default browser when none is set. */
internal object BookmarkLauncher {
    /** Returns false when nothing could open the link. */
    fun open(
        context: Application,
        bookmark: BookmarkInfo,
        onShowToast: (Int, String?) -> Unit,
    ): Boolean {
        val browserPackage = bookmark.browserPackageName
        if (!browserPackage.isNullOrBlank()) {
            IntentHelpers.openBrowserUrl(context, bookmark.url, browserPackage, onShowToast)
            return true
        }
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            onShowToast(com.tk.quicksearch.R.string.common_error_unable_to_open, BookmarkUrls.displayHost(bookmark.url))
            false
        } catch (_: SecurityException) {
            onShowToast(com.tk.quicksearch.R.string.common_error_unable_to_open, BookmarkUrls.displayHost(bookmark.url))
            false
        }
    }
}
