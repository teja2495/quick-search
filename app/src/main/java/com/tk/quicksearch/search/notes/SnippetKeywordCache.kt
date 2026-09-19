package com.tk.quicksearch.search.notes

import android.content.Context
import com.tk.quicksearch.search.models.NoteInfo
import java.util.concurrent.Executors

/**
 * In-memory keyword -> body map for the accessibility service.
 *
 * The service matches keywords on every keystroke from [android.accessibilityservice
 * .AccessibilityService.onAccessibilityEvent], which runs on the main thread, so the notes store is
 * never touched there. Writers publish through [publish]; the service warms the cache with [load].
 */
object SnippetKeywordCache {
    @Volatile
    private var keywords: Map<String, String> = emptyMap()

    private val loader = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "snippet-keyword-cache").apply { isDaemon = true }
    }

    fun current(): Map<String, String> = keywords

    /** Publishes the snippet keywords from an already-loaded note list. */
    fun publish(notes: List<NoteInfo>) {
        keywords =
            notes
                .asSequence()
                .filter { it.isSnippet && it.keyword.isNotBlank() && it.markdownContent.isNotBlank() }
                .associate { SnippetExpander.normalizeKeyword(it.keyword) to it.markdownContent }
    }

    /** Warms the cache off the main thread. Safe to call repeatedly. */
    fun load(context: Context) {
        val appContext = context.applicationContext
        loader.execute {
            runCatching {
                publish(
                    com.tk.quicksearch.search.data
                        .NotesRepository(appContext)
                        .getAllNotes(),
                )
            }
        }
    }

    fun clear() {
        keywords = emptyMap()
    }
}
