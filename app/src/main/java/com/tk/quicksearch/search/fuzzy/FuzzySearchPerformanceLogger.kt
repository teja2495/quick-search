package com.tk.quicksearch.search.fuzzy

import android.util.Log
import com.tk.quicksearch.BuildConfig
import com.tk.quicksearch.search.core.SearchSection

object FuzzySearchPerformanceLogger {
    private const val TAG = "FuzzySearch"

    fun <T> measure(
        section: SearchSection,
        query: String,
        candidateCount: Int,
        block: () -> T,
    ): T {
        if (!BuildConfig.DEBUG) return block()

        val startedAtNanos = System.nanoTime()
        return block().also {
            val elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000.0
            runCatching {
                Log.d(
                    TAG,
                    "section=$section queryLength=${query.length} candidates=$candidateCount elapsedMs=$elapsedMillis",
                )
            }
        }
    }
}
