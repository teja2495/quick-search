package com.tk.quicksearch.search.noted

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.AliasPreferences
import com.tk.quicksearch.search.data.preferences.SearchEnginePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The Quick Note alias: typing the query followed by the alias puts the query at the top of Noted's
 * Quick Note. Set under More Options > Noted Integration, and works only while that toggle is on.
 * Like other end aliases, it waits for a trailing space when "trigger after space" is on.
 */
internal object NotedQuickNoteAlias {
    /** The alias's target id in [AliasPreferences]; it is stored and backed up like other aliases. */
    const val ALIAS_TARGET_ID = "noted_quick_note"

    private const val MIN_CLEAR_DELAY_MS = 100L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var alias: String? = null

    @Volatile
    private var loaded = false

    /** The saved alias, or "" when none. Read once and then served from memory: every keystroke checks it. */
    fun get(context: Context): String {
        if (!loaded) {
            alias = AliasPreferences(context).getAliasCode(ALIAS_TARGET_ID).orEmpty()
            loaded = true
        }
        return alias.orEmpty()
    }

    fun set(context: Context, code: String) {
        val preferences = AliasPreferences(context)
        preferences.setAliasCode(ALIAS_TARGET_ID, code)
        // Read back the normalized code the preferences stored.
        alias = preferences.getAliasCode(ALIAS_TARGET_ID).orEmpty()
        loaded = true
    }

    /**
     * When [query] ends with the alias, sends the text before it to Noted's Quick Note and shows a
     * toast with the result. [onAdded] runs on the main thread once Noted took it; on failure the query
     * stays, so nothing typed is lost.
     *
     * Call it after the query itself was applied: the search bar ignores a shorter query while its
     * last keystroke is unacknowledged, so an immediate clear would be dropped as a stale echo.
     */
    fun sendIfTriggered(context: Context, query: String, onAdded: () -> Unit) {
        val text = textBeforeAlias(context, query) ?: return
        if (!NotedIntegration.isEnabled(context)) return
        val appContext = context.applicationContext
        val startedAt = SystemClock.uptimeMillis()
        scope.launch {
            val added = NotedRepository(appContext).addToQuickNote(text)
            val message = if (added) R.string.noted_quick_note_added else R.string.noted_quick_note_failed
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            if (!added) return@launch
            // Leave the search bar a frame or so to see the typed query applied before clearing it.
            delay(MIN_CLEAR_DELAY_MS - (SystemClock.uptimeMillis() - startedAt))
            onAdded()
        }
    }

    /** The text before the alias when [query] ends with it after other text, or null. Keeps line breaks. */
    private fun textBeforeAlias(context: Context, query: String): String? {
        val alias = get(context).lowercase(Locale.getDefault())
        if (alias.isEmpty()) return null
        val trimmed = query.trimEnd()
        val lastWordStart = trimmed.indexOfLast { it.isWhitespace() } + 1
        if (lastWordStart == 0 || trimmed.substring(lastWordStart).lowercase(Locale.getDefault()) != alias) return null
        val hasTrailingSpace = trimmed.length < query.length
        if (!hasTrailingSpace && SearchEnginePreferences(context).isAliasTriggerAfterSpaceEnabled()) return null
        return trimmed.substring(0, lastWordStart).trim().takeIf { it.isNotEmpty() }
    }
}
