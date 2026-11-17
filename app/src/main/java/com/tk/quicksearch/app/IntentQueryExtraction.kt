package com.tk.quicksearch.app

import android.app.SearchManager
import android.content.Intent

/** The search query an incoming intent carries (assist, search, share, process-text), if any. */
internal fun extractQueryFromIntent(intent: Intent?): String? {
    if (intent == null) return null

    val queryFromCommonExtras =
        listOf(
                SearchManager.QUERY,
                Intent.EXTRA_TEXT,
                "query",
                "q",
                "text",
            )
            .asSequence()
            // Callers put the query under any of these keys with any type, so read it untyped.
            .mapNotNull { key ->
                @Suppress("DEPRECATION")
                intent.extras?.get(key)?.toString()?.trim()
            }
            .firstOrNull { it.isNotBlank() }

    return when (intent.action) {
        Intent.ACTION_PROCESS_TEXT ->
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()?.trim()
                ?.takeIf { it.isNotBlank() }
        Intent.ACTION_SEND ->
            if (intent.type == "text/plain")
                intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotBlank() }
            else null
        Intent.ACTION_SEARCH,
        Intent.ACTION_WEB_SEARCH,
        Intent.ACTION_VIEW,
        Intent.ACTION_MAIN,
        -> queryFromCommonExtras
        else -> null
    }
}
