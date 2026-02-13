package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.concurrent.ConcurrentHashMap

/**
 * Preferences for managing nicknames for apps, app shortcuts, contacts, files, and settings. Uses
 * in-memory caching to avoid expensive SharedPreferences iteration on every search.
 */
class NicknamePreferences(
    context: Context,
) : BasePreferences(context) {
    private val contactNicknameCache = ConcurrentHashMap<Long, String>()
    private val fileNicknameCache = ConcurrentHashMap<String, String>()
    private val settingNicknameCache = ConcurrentHashMap<String, String>()

    init {
        loadNicknameCaches()
    }

    private fun loadNicknameCaches() {
        val allPrefs = prefs.all
        for ((key, value) in allPrefs) {
            if (value !is String) continue

            when {
                key.startsWith(BasePreferences.KEY_NICKNAME_CONTACT_PREFIX) -> {
                    val contactIdStr = key.removePrefix(BasePreferences.KEY_NICKNAME_CONTACT_PREFIX)
                    contactIdStr.toLongOrNull()?.let { contactNicknameCache[it] = value }
                }

                key.startsWith(BasePreferences.KEY_NICKNAME_FILE_PREFIX) -> {
                    val fileUri = key.removePrefix(BasePreferences.KEY_NICKNAME_FILE_PREFIX)
                    fileNicknameCache[fileUri] = value
                }

                key.startsWith(BasePreferences.KEY_NICKNAME_SETTING_PREFIX) -> {
                    val id = key.removePrefix(BasePreferences.KEY_NICKNAME_SETTING_PREFIX)
                    settingNicknameCache[id] = value
                }
            }
        }
    }

    // ============================================================================
    // Nickname Preferences
    // ============================================================================

    fun getAppNickname(packageName: String): String? = prefs.getString("${BasePreferences.KEY_NICKNAME_APP_PREFIX}$packageName", null)

    fun setAppNickname(
        packageName: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_APP_PREFIX}$packageName"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    fun getAllAppNicknames(): Map<String, String> {
        val allPrefs = prefs.all
        val nicknames = mutableMapOf<String, String>()
        for ((key, value) in allPrefs) {
            if (key.startsWith(BasePreferences.KEY_NICKNAME_APP_PREFIX) && value is String) {
                val packageName = key.removePrefix(BasePreferences.KEY_NICKNAME_APP_PREFIX)
                nicknames[packageName] = value
            }
        }
        return nicknames
    }

    fun getAppShortcutNickname(shortcutId: String): String? =
        prefs.getString(
            "${BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX}$shortcutId",
            null,
        )

    fun setAppShortcutNickname(
        shortcutId: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX}$shortcutId"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    fun getAllAppShortcutNicknames(): Map<String, String> {
        val allPrefs = prefs.all
        val nicknames = mutableMapOf<String, String>()
        for ((key, value) in allPrefs) {
            if (key.startsWith(BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX) && value is String) {
                val shortcutId = key.removePrefix(BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX)
                nicknames[shortcutId] = value
            }
        }
        return nicknames
    }

    fun getContactNickname(contactId: Long): String? = contactNicknameCache[contactId]

    fun setContactNickname(
        contactId: Long,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_CONTACT_PREFIX}$contactId"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
            contactNicknameCache.remove(contactId)
        } else {
            val trimmed = nickname.trim()
            prefs.edit().putString(key, trimmed).apply()
            contactNicknameCache[contactId] = trimmed
        }
    }

    fun getFileNickname(uri: String): String? = fileNicknameCache[uri]

    fun setFileNickname(
        uri: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_FILE_PREFIX}$uri"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
            fileNicknameCache.remove(uri)
        } else {
            val trimmed = nickname.trim()
            prefs.edit().putString(key, trimmed).apply()
            fileNicknameCache[uri] = trimmed
        }
    }

    fun getSettingNickname(id: String): String? = settingNicknameCache[id]

    fun setSettingNickname(
        id: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_SETTING_PREFIX}$id"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
            settingNicknameCache.remove(id)
        } else {
            val trimmed = nickname.trim()
            prefs.edit().putString(key, trimmed).apply()
            settingNicknameCache[id] = trimmed
        }
    }

    /**
     * Finds contact IDs that have nicknames matching the query. Uses in-memory cache for O(1)
     * lookup instead of O(n) SharedPreferences iteration.
     */
    fun findContactsWithMatchingNickname(query: String): Set<Long> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query).trim()
        if (normalizedQuery.isBlank()) return emptySet()

        val matchingContactIds = mutableSetOf<Long>()
        for ((contactId, nickname) in contactNicknameCache) {
            if (SearchTextNormalizer.normalizeForSearch(nickname).contains(normalizedQuery)) {
                matchingContactIds.add(contactId)
            }
        }

        return matchingContactIds
    }

    /**
     * Finds file URIs that have nicknames matching the query. Uses in-memory cache for O(1) lookup
     * instead of O(n) SharedPreferences iteration.
     */
    fun findFilesWithMatchingNickname(query: String): Set<String> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query).trim()
        if (normalizedQuery.isBlank()) return emptySet()

        val matchingFileUris = mutableSetOf<String>()
        for ((uri, nickname) in fileNicknameCache) {
            if (SearchTextNormalizer.normalizeForSearch(nickname).contains(normalizedQuery)) {
                matchingFileUris.add(uri)
            }
        }

        return matchingFileUris
    }

    /**
     * Finds settings that have nicknames matching the query. Uses in-memory cache for O(1) lookup
     * instead of O(n) SharedPreferences iteration.
     */
    fun findSettingsWithMatchingNickname(query: String): Set<String> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query).trim()
        if (normalizedQuery.isBlank()) return emptySet()

        val matchingSettingIds = mutableSetOf<String>()
        for ((id, nickname) in settingNicknameCache) {
            if (SearchTextNormalizer.normalizeForSearch(nickname).contains(normalizedQuery)) {
                matchingSettingIds.add(id)
            }
        }

        return matchingSettingIds
    }
}
