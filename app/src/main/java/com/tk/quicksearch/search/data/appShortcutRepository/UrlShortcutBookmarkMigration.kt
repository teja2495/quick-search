package com.tk.quicksearch.search.data.appShortcutRepository

import android.content.Context
import android.content.Intent
import com.tk.quicksearch.search.data.bookmarks.BookmarkRepository
import com.tk.quicksearch.search.data.bookmarks.BookmarkUrls
import com.tk.quicksearch.search.data.bookmarks.ImportedBookmark
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity

/**
 * Websites used to be saved as per-browser URL shortcuts. Moves any that remain (including ones
 * restored from an older backup) into Bookmarks, keeping the browser each one opened in.
 *
 * TODO: Remove once most users have updated past the release that added Bookmarks.
 */
internal object UrlShortcutBookmarkMigration {
    private val lock = Any()

    private data class UrlShortcut(val name: String, val url: String, val browserPackageName: String)

    /** Returns the custom shortcuts that remain after the move. Cheap when there is nothing to move. */
    fun migrate(
        context: Context,
        shortcutCache: AppShortcutCache,
        customShortcuts: List<StaticShortcut>,
    ): List<StaticShortcut> {
        if (customShortcuts.none { urlShortcutOf(it) != null }) return customShortcuts
        synchronized(lock) {
            val current = shortcutCache.loadCustomShortcuts().orEmpty()
            val toMove = current.mapNotNull { shortcut -> urlShortcutOf(shortcut)?.let { shortcut to it } }
            if (toMove.isEmpty()) return current

            val bookmarkRepository = BookmarkRepository(context)
            toMove
                .groupBy { (_, urlShortcut) -> urlShortcut.browserPackageName }
                .forEach { (browserPackageName, entries) ->
                    bookmarkRepository.import(
                        items = entries.map { (_, urlShortcut) -> ImportedBookmark(urlShortcut.name, urlShortcut.url) },
                        browserPackageName = browserPackageName,
                    )
                }
            val movedKeys = toMove.mapTo(HashSet()) { (shortcut, _) -> shortcutKey(shortcut) }
            val remaining = current.filterNot { shortcutKey(it) in movedKeys }
            // If saving fails the shortcuts stay, and the next load retries; import skips duplicates.
            shortcutCache.saveCustomShortcuts(remaining)
            return remaining
        }
    }

    private fun urlShortcutOf(shortcut: StaticShortcut): UrlShortcut? {
        if (!isUserCreatedShortcut(shortcut)) return null
        val intent =
            shortcut.intents.firstOrNull {
                it.action == SearchTargetQueryShortcutActivity.ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT
            } ?: return null
        if (intent.getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_TARGET_TYPE) !=
            SearchTargetQueryShortcutActivity.TARGET_TYPE_BROWSER
        ) {
            return null
        }
        val browserPackageName =
            intent.getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_BROWSER_PACKAGE)
                ?.takeIf { it.isNotBlank() } ?: return null
        val query = intent.getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_QUERY)?.trim().orEmpty()
        val mode = modeOf(intent)
        // Auto-mode shortcuts searched for plain text and opened anything that looked like a link.
        if (mode == SearchTargetShortcutMode.FORCE_SEARCH) return null
        val url = BookmarkUrls.normalize(query) ?: return null
        return UrlShortcut(name = shortcutDisplayName(shortcut), url = url, browserPackageName = browserPackageName)
    }

    private fun modeOf(intent: Intent): SearchTargetShortcutMode =
        intent.getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_BROWSER_SHORTCUT_MODE)
            ?.let { runCatching { SearchTargetShortcutMode.valueOf(it) }.getOrNull() }
            ?: SearchTargetShortcutMode.AUTO
}
