package com.tk.quicksearch.search.data.preferences

import com.tk.quicksearch.search.utils.SearchTextNormalizer
import org.json.JSONObject
import android.content.Context

data class ResultTrigger(
    val word: String,
    val triggerAfterSpace: Boolean,
)

class TriggerPreferences(
    context: Context,
) : BasePreferences(context) {
    private fun normalizeWord(word: String): String =
        SearchTextNormalizer.normalizeForSearch(word).trim().substringBefore(' ')

    private fun readTrigger(key: String): ResultTrigger? {
        val stored = prefs.getString(key, null) ?: return null
        return runCatching {
            val json = JSONObject(stored)
            val word = json.optString(KEY_WORD).trim()
            if (word.isBlank()) {
                null
            } else {
                ResultTrigger(
                    word = word,
                    triggerAfterSpace = json.optBoolean(KEY_AFTER_SPACE, false),
                )
            }
        }.getOrNull()
    }

    private fun writeTrigger(
        key: String,
        trigger: ResultTrigger?,
    ) {
        val word = trigger?.word?.let(::normalizeWord).orEmpty()
        if (word.isBlank()) {
            prefs.edit().remove(key).apply()
            return
        }
        val json =
            JSONObject()
                .put(KEY_WORD, word)
                .put(KEY_AFTER_SPACE, trigger?.triggerAfterSpace == true)
        prefs.edit().putString(key, json.toString()).apply()
    }

    fun getAllAppTriggers(): Map<String, ResultTrigger> =
        getAllTriggersByPrefix(
            prefix = BasePreferences.KEY_TRIGGER_APP_PREFIX,
            excludedPrefix = BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX,
        )

    fun getAllAppShortcutTriggers(): Map<String, ResultTrigger> =
        getAllTriggersByPrefix(BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX)

    fun getAllContactTriggers(): Map<Long, ResultTrigger> =
        getAllTriggersByPrefix(BasePreferences.KEY_TRIGGER_CONTACT_PREFIX)
            .mapNotNull { (id, trigger) -> id.toLongOrNull()?.let { it to trigger } }
            .toMap()

    fun getAllFileTriggers(): Map<String, ResultTrigger> =
        getAllTriggersByPrefix(BasePreferences.KEY_TRIGGER_FILE_PREFIX)

    fun getAllSettingTriggers(): Map<String, ResultTrigger> =
        getAllTriggersByPrefix(BasePreferences.KEY_TRIGGER_SETTING_PREFIX)

    fun getAllNoteTriggers(): Map<Long, ResultTrigger> =
        getAllTriggersByPrefix(BasePreferences.KEY_TRIGGER_NOTE_PREFIX)
            .mapNotNull { (id, trigger) -> id.toLongOrNull()?.let { it to trigger } }
            .toMap()

    private fun getAllTriggersByPrefix(
        prefix: String,
        excludedPrefix: String? = null,
    ): Map<String, ResultTrigger> =
        prefs.all.mapNotNull { (key, _) ->
            if (!key.startsWith(prefix)) return@mapNotNull null
            if (excludedPrefix != null && key.startsWith(excludedPrefix)) return@mapNotNull null
            val trigger = readTrigger(key) ?: return@mapNotNull null
            key.removePrefix(prefix) to trigger
        }.toMap()

    fun getAppTrigger(packageName: String): ResultTrigger? =
        readTrigger("${BasePreferences.KEY_TRIGGER_APP_PREFIX}$packageName")

    fun setAppTrigger(packageName: String, trigger: ResultTrigger?) =
        writeTrigger("${BasePreferences.KEY_TRIGGER_APP_PREFIX}$packageName", trigger)

    fun getAppShortcutTrigger(shortcutId: String): ResultTrigger? =
        readTrigger("${BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX}$shortcutId")

    fun setAppShortcutTrigger(shortcutId: String, trigger: ResultTrigger?) =
        writeTrigger("${BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX}$shortcutId", trigger)

    fun getContactTrigger(contactId: Long): ResultTrigger? =
        readTrigger("${BasePreferences.KEY_TRIGGER_CONTACT_PREFIX}$contactId")

    fun setContactTrigger(contactId: Long, trigger: ResultTrigger?) =
        writeTrigger("${BasePreferences.KEY_TRIGGER_CONTACT_PREFIX}$contactId", trigger)

    fun getFileTrigger(uri: String): ResultTrigger? =
        readTrigger("${BasePreferences.KEY_TRIGGER_FILE_PREFIX}$uri")

    fun setFileTrigger(uri: String, trigger: ResultTrigger?) =
        writeTrigger("${BasePreferences.KEY_TRIGGER_FILE_PREFIX}$uri", trigger)

    fun getSettingTrigger(id: String): ResultTrigger? =
        readTrigger("${BasePreferences.KEY_TRIGGER_SETTING_PREFIX}$id")

    fun setSettingTrigger(id: String, trigger: ResultTrigger?) =
        writeTrigger("${BasePreferences.KEY_TRIGGER_SETTING_PREFIX}$id", trigger)

    fun getNoteTrigger(noteId: Long): ResultTrigger? =
        readTrigger("${BasePreferences.KEY_TRIGGER_NOTE_PREFIX}$noteId")

    fun setNoteTrigger(noteId: Long, trigger: ResultTrigger?) =
        writeTrigger("${BasePreferences.KEY_TRIGGER_NOTE_PREFIX}$noteId", trigger)

    fun findContactsWithMatchingTrigger(query: String): Set<Long> =
        findMatchingTriggerIds(
            query = query,
            prefix = BasePreferences.KEY_TRIGGER_CONTACT_PREFIX,
            parseId = String::toLongOrNull,
        )

    fun findFilesWithMatchingTrigger(query: String): Set<String> =
        findMatchingTriggerIds(
            query = query,
            prefix = BasePreferences.KEY_TRIGGER_FILE_PREFIX,
            parseId = { it },
        )

    fun findSettingsWithMatchingTrigger(query: String): Set<String> =
        findMatchingTriggerIds(
            query = query,
            prefix = BasePreferences.KEY_TRIGGER_SETTING_PREFIX,
            parseId = { it },
        )

    fun findNotesWithMatchingTrigger(query: String): Set<Long> =
        findMatchingTriggerIds(
            query = query,
            prefix = BasePreferences.KEY_TRIGGER_NOTE_PREFIX,
            parseId = String::toLongOrNull,
        )

    private fun <T : Any> findMatchingTriggerIds(
        query: String,
        prefix: String,
        parseId: (String) -> T?,
    ): Set<T> {
        val normalizedQuery = normalizeWord(query)
        if (normalizedQuery.isBlank()) return emptySet()

        return prefs.all.mapNotNull { (key, _) ->
            if (!key.startsWith(prefix)) return@mapNotNull null
            val trigger = readTrigger(key) ?: return@mapNotNull null
            if (normalizeWord(trigger.word) == normalizedQuery) {
                parseId(key.removePrefix(prefix))
            } else {
                null
            }
        }.toSet()
    }

    private companion object {
        const val KEY_WORD = "word"
        const val KEY_AFTER_SPACE = "afterSpace"
    }
}
