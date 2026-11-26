package com.tk.quicksearch.search.noted

import com.tk.quicksearch.searchEngines.AliasValidator.splitAliasCodes
import com.tk.quicksearch.shared.ui.components.AppMessages
import android.content.Context
import android.os.SystemClock
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.AliasPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * An alias typed at the end of a query that sends the text before it to Noted. Set under More Options >
 * Noted Integration, and works only while that toggle is on. Unlike other end aliases, it fires as
 * soon as its last letter is typed, even when "trigger after space" is on.
 */
internal abstract class NotedEndAlias(
    /** The alias's target id in [AliasPreferences]; it is stored and backed up like other aliases. */
    val aliasTargetId: String,
    /** Used until the user saves or clears an alias; a cleared alias stays cleared. */
    private val defaultAlias: String,
) {
    @Volatile
    private var alias: String? = null

    @Volatile
    private var loaded = false

    protected abstract val addedMessage: Int
    protected abstract val failedMessage: Int

    /** Hands [text] to Noted. Returns whether Noted took it. */
    protected abstract suspend fun send(repository: NotedRepository, text: String): Boolean

    /**
     * The saved alias, [defaultAlias] when never set, or "" when cleared. Read once and then served
     * from memory: every keystroke checks it.
     */
    fun get(context: Context): String {
        if (!loaded) {
            alias = AliasPreferences(context).getAliasCode(aliasTargetId) ?: defaultAlias
            loaded = true
        }
        return alias.orEmpty()
    }

    /**
     * The alias keyed by [aliasTargetId] while the Noted Integration toggle is on, for alias and
     * trigger conflict checks; empty while it is off, since the alias does nothing then.
     */
    fun activeAliasById(context: Context): Map<String, String> {
        if (!NotedIntegration.isEnabled(context)) return emptyMap()
        val code = get(context)
        return if (code.isEmpty()) emptyMap() else mapOf(aliasTargetId to code)
    }

    fun set(context: Context, code: String) {
        val preferences = AliasPreferences(context)
        preferences.setAliasCode(aliasTargetId, code)
        // Read back the normalized code the preferences stored.
        alias = preferences.getAliasCode(aliasTargetId).orEmpty()
        loaded = true
    }

    /**
     * When [query] ends with the alias, sends the text before it to Noted and shows a toast with the
     * result. [onAdded] runs on the main thread once Noted took it; on failure the query stays, so
     * nothing typed is lost.
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
            val added = send(NotedRepository(appContext), text)
            AppMessages.show(appContext, if (added) addedMessage else failedMessage)
            if (!added) return@launch
            // Leave the search bar a frame or so to see the typed query applied before clearing it.
            delay(MIN_CLEAR_DELAY_MS - (SystemClock.uptimeMillis() - startedAt))
            onAdded()
        }
    }

    /** The text before the alias when [query] ends with it after other text, or null. Keeps line breaks. */
    private fun textBeforeAlias(context: Context, query: String): String? {
        val aliases = splitAliasCodes(get(context))
        if (aliases.isEmpty()) return null
        val trimmed = query.trimEnd()
        val lastWordStart = trimmed.indexOfLast { it.isWhitespace() } + 1
        if (lastWordStart == 0 || trimmed.substring(lastWordStart).lowercase(Locale.getDefault()) !in aliases) return null
        return trimmed.substring(0, lastWordStart).trim().takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val MIN_CLEAR_DELAY_MS = 100L
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        /** Every Noted end alias, in the order their pills show. */
        val all: List<NotedEndAlias> get() = listOf(NotedQuickNoteAlias, NotedTaskAlias)

        /** [activeAliasById] of every Noted alias. */
        fun activeAliasesById(context: Context): Map<String, String> =
            all.fold(emptyMap()) { acc, alias -> acc + alias.activeAliasById(context) }

        /** Runs [sendIfTriggered] for each alias; at most one matches the query's last word. */
        fun sendIfTriggered(context: Context, query: String, onAdded: () -> Unit) {
            all.forEach { it.sendIfTriggered(context, query, onAdded) }
        }
    }
}

/** "nn": puts the query at the top of Noted's Quick Note. */
internal object NotedQuickNoteAlias : NotedEndAlias(aliasTargetId = "noted_quick_note", defaultAlias = "nn") {
    override val addedMessage = R.string.noted_quick_note_added
    override val failedMessage = R.string.noted_quick_note_failed

    override suspend fun send(repository: NotedRepository, text: String) = repository.addToQuickNote(text)
}

/** "ntt": adds the query as an unscheduled task to Noted's To Do list. */
internal object NotedTaskAlias : NotedEndAlias(aliasTargetId = "noted_task", defaultAlias = "ntt") {
    override val addedMessage = R.string.noted_task_added
    override val failedMessage = R.string.noted_task_failed

    override suspend fun send(repository: NotedRepository, text: String) =
        repository.createTask(text, dueAtMillis = null, isDueTimeSet = false)
}
