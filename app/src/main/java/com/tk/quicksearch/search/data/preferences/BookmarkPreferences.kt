package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.searchEngines.IN_APP_BROWSER_PACKAGE

class BookmarkPreferences(
    context: Context,
) : BasePreferences(context) {
    /** Browser that new bookmarks open in; the in-app browser until the user picks another. */
    fun getDefaultBrowserPackage(): String =
        prefs.getString(BasePreferences.KEY_BOOKMARK_DEFAULT_BROWSER, null)?.takeIf { it.isNotBlank() }
            ?: IN_APP_BROWSER_PACKAGE

    fun setDefaultBrowserPackage(packageName: String) {
        prefs.edit().putString(BasePreferences.KEY_BOOKMARK_DEFAULT_BROWSER, packageName).apply()
    }
}
