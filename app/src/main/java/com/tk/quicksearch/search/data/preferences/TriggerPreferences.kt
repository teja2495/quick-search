package com.tk.quicksearch.search.data.preferences

import com.tk.quicksearch.search.utils.SearchTextNormalizer
import org.json.JSONObject
import android.content.Context
import com.tk.quicksearch.search.contacts.models.ContactCardAction

data class ResultTrigger(
    val word: String,
    val triggerAfterSpace: Boolean,
)

class TriggerPreferences(
    context: Context,
) : BasePreferences(context) {
    private val customizationStore = SearchCustomizationStore(context)

    /** Partitions every trigger category from one SharedPreferences snapshot. */
    fun getAllTriggerWordsById(): Map<String, String> {
        val snapshot = customizationStore.snapshot()
        return buildMap {
            snapshot.forEach { (key, value) ->
                val trigger = readTriggerValue(value) ?: return@forEach
                when {
                    key.startsWith(BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX) ->
                        put("shortcut:${key.removePrefix(BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX)}", trigger.word)
                    key.startsWith(BasePreferences.KEY_TRIGGER_APP_PREFIX) ->
                        put("app:${key.removePrefix(BasePreferences.KEY_TRIGGER_APP_PREFIX)}", trigger.word)
                    key.startsWith(BasePreferences.KEY_TRIGGER_CONTACT_ACTION_PREFIX) -> {
                        val id = key.removePrefix(BasePreferences.KEY_TRIGGER_CONTACT_ACTION_PREFIX)
                        val separatorIndex = id.indexOf(KEY_SEPARATOR)
                        if (separatorIndex > 0 && separatorIndex < id.lastIndex) {
                            val contactId = id.take(separatorIndex).toLongOrNull()
                            val action = ContactCardAction.fromSerializedString(id.drop(separatorIndex + 1))
                            if (contactId != null && action != null) {
                                put("contactAction:$contactId:${action.toSerializedString()}", trigger.word)
                            }
                        }
                    }
                    key.startsWith(BasePreferences.KEY_TRIGGER_CONTACT_PREFIX) ->
                        key.removePrefix(BasePreferences.KEY_TRIGGER_CONTACT_PREFIX).toLongOrNull()?.let {
                            put("contact:$it", trigger.word)
                        }
                    key.startsWith(BasePreferences.KEY_TRIGGER_FILE_PREFIX) ->
                        put("file:${key.removePrefix(BasePreferences.KEY_TRIGGER_FILE_PREFIX)}", trigger.word)
                    key.startsWith(BasePreferences.KEY_TRIGGER_SETTING_PREFIX) ->
                        put("setting:${key.removePrefix(BasePreferences.KEY_TRIGGER_SETTING_PREFIX)}", trigger.word)
                    key.startsWith(BasePreferences.KEY_TRIGGER_NOTE_PREFIX) ->
                        key.removePrefix(BasePreferences.KEY_TRIGGER_NOTE_PREFIX).toLongOrNull()?.let {
                            put("note:$it", trigger.word)
                        }
                }
            }
        }
    }

    private fun normalizeWord(word: String): String =
        SearchTextNormalizer.normalizeForSearch(word).trim().substringBefore(' ')

    private fun readTrigger(key: String): ResultTrigger? {
        val stored = customizationStore.getString(key) ?: return null
        return readTriggerValue(stored)
    }

    private fun readTriggerValue(stored: Any?): ResultTrigger? {
        if (stored !is String) return null
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
            customizationStore.putString(key, null)
            return
        }
        val json =
            JSONObject()
                .put(KEY_WORD, word)
                .put(KEY_AFTER_SPACE, trigger?.triggerAfterSpace == true)
        customizationStore.putString(key, json.toString())
    }

    fun getAllAppTriggers(): Map<String, ResultTrigger> =
        getAllTriggersByPrefix(
            prefix = BasePreferences.KEY_TRIGGER_APP_PREFIX,
            excludedPrefix = BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX,
        )

    fun getAllAppShortcutTriggers(): Map<String, ResultTrigger> =
        getAllTriggersByPrefix(BasePreferences.KEY_TRIGGER_APP_SHORTCUT_PREFIX)

    fun getAllContactTriggers(): Map<Long, ResultTrigger> =
        getAllTriggersByPrefix(
            prefix = BasePreferences.KEY_TRIGGER_CONTACT_PREFIX,
            excludedPrefix = BasePreferences.KEY_TRIGGER_CONTACT_ACTION_PREFIX,
        )
            .mapNotNull { (id, trigger) -> id.toLongOrNull()?.let { it to trigger } }
            .toMap()

    fun getAllContactActionTriggers(): Map<ContactActionTriggerKey, ResultTrigger> =
        getAllTriggersByPrefix(BasePreferences.KEY_TRIGGER_CONTACT_ACTION_PREFIX)
            .mapNotNull { (id, trigger) ->
                val separatorIndex = id.indexOf(KEY_SEPARATOR)
                if (separatorIndex <= 0 || separatorIndex == id.lastIndex) return@mapNotNull null
                val contactId = id.take(separatorIndex).toLongOrNull() ?: return@mapNotNull null
                val action =
                    ContactCardAction.fromSerializedString(id.drop(separatorIndex + 1))
                        ?: return@mapNotNull null
                ContactActionTriggerKey(contactId, action) to trigger
            }
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
        customizationStore.snapshot().mapNotNull { (key, value) ->
            if (!key.startsWith(prefix)) return@mapNotNull null
            if (excludedPrefix != null && key.startsWith(excludedPrefix)) return@mapNotNull null
            val trigger = readTriggerValue(value) ?: return@mapNotNull null
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

    fun getContactActionTrigger(
        contactId: Long,
        action: ContactCardAction,
    ): ResultTrigger? =
        readTrigger(contactActionTriggerKey(contactId, action))

    fun setContactActionTrigger(
        contactId: Long,
        action: ContactCardAction,
        trigger: ResultTrigger?,
    ) = writeTrigger(contactActionTriggerKey(contactId, action), trigger)

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
            excludedPrefix = BasePreferences.KEY_TRIGGER_CONTACT_ACTION_PREFIX,
            parseId = String::toLongOrNull,
        ) + findMatchingTriggerIds(
            query = query,
            prefix = BasePreferences.KEY_TRIGGER_CONTACT_ACTION_PREFIX,
            parseId = { id ->
                val separatorIndex = id.indexOf(KEY_SEPARATOR)
                if (separatorIndex <= 0) null else id.take(separatorIndex).toLongOrNull()
            },
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
        excludedPrefix: String? = null,
        parseId: (String) -> T?,
    ): Set<T> {
        val normalizedQuery = normalizeWord(query)
        if (normalizedQuery.isBlank()) return emptySet()

        return customizationStore.snapshot().mapNotNull { (key, value) ->
            if (!key.startsWith(prefix)) return@mapNotNull null
            if (excludedPrefix != null && key.startsWith(excludedPrefix)) return@mapNotNull null
            val trigger = readTriggerValue(value) ?: return@mapNotNull null
            if (normalizeWord(trigger.word) == normalizedQuery) {
                parseId(key.removePrefix(prefix))
            } else {
                null
            }
        }.toSet()
    }

    private fun contactActionTriggerKey(
        contactId: Long,
        action: ContactCardAction,
    ): String =
        "${BasePreferences.KEY_TRIGGER_CONTACT_ACTION_PREFIX}$contactId$KEY_SEPARATOR${action.toSerializedString()}"

    private companion object {
        const val KEY_SEPARATOR = "|"
        const val KEY_WORD = "word"
        const val KEY_AFTER_SPACE = "afterSpace"
    }
}

data class ContactActionTriggerKey(
    val contactId: Long,
    val action: ContactCardAction,
)
