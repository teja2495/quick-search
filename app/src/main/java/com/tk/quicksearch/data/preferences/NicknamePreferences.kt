package com.tk.quicksearch.data.preferences

import android.content.Context

import java.util.Locale

/**
 * Preferences for managing nicknames for apps, contacts, files, and settings.
 */
class NicknamePreferences(context: Context) : BasePreferences(context) {

    // ============================================================================
    // Nickname Preferences
    // ============================================================================

    fun getAppNickname(packageName: String): String? {
        return prefs.getString("${NicknamePreferences.KEY_NICKNAME_APP_PREFIX}$packageName", null)
    }

    fun setAppNickname(packageName: String, nickname: String?) {
        val key = "${NicknamePreferences.KEY_NICKNAME_APP_PREFIX}$packageName"
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
            if (key.startsWith(NicknamePreferences.KEY_NICKNAME_APP_PREFIX) && value is String) {
                val packageName = key.removePrefix(NicknamePreferences.KEY_NICKNAME_APP_PREFIX)
                nicknames[packageName] = value
            }
        }
        return nicknames
    }

    fun getContactNickname(contactId: Long): String? {
        return prefs.getString("${NicknamePreferences.KEY_NICKNAME_CONTACT_PREFIX}$contactId", null)
    }

    fun setContactNickname(contactId: Long, nickname: String?) {
        val key = "${NicknamePreferences.KEY_NICKNAME_CONTACT_PREFIX}$contactId"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    fun getFileNickname(uri: String): String? {
        return prefs.getString("${NicknamePreferences.KEY_NICKNAME_FILE_PREFIX}$uri", null)
    }

    fun setFileNickname(uri: String, nickname: String?) {
        val key = "${NicknamePreferences.KEY_NICKNAME_FILE_PREFIX}$uri"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    fun getSettingNickname(id: String): String? {
        return prefs.getString("${NicknamePreferences.KEY_NICKNAME_SETTING_PREFIX}$id", null)
    }

    fun setSettingNickname(id: String, nickname: String?) {
        val key = "${NicknamePreferences.KEY_NICKNAME_SETTING_PREFIX}$id"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    /**
     * Finds contact IDs that have nicknames matching the query.
     */
    fun findContactsWithMatchingNickname(query: String): Set<Long> {
        val normalizedQuery = query.lowercase(Locale.getDefault()).trim()
        if (normalizedQuery.isBlank()) return emptySet()

        val matchingContactIds = mutableSetOf<Long>()
        val allPrefs = prefs.all

        for ((key, value) in allPrefs) {
            if (key.startsWith(NicknamePreferences.KEY_NICKNAME_CONTACT_PREFIX) && value is String) {
                val nickname = value.lowercase(Locale.getDefault())
                if (nickname.contains(normalizedQuery)) {
                    val contactIdStr = key.removePrefix(NicknamePreferences.KEY_NICKNAME_CONTACT_PREFIX)
                    contactIdStr.toLongOrNull()?.let { matchingContactIds.add(it) }
                }
            }
        }

        return matchingContactIds
    }

    /**
     * Finds file URIs that have nicknames matching the query.
     */
    fun findFilesWithMatchingNickname(query: String): Set<String> {
        val normalizedQuery = query.lowercase(Locale.getDefault()).trim()
        if (normalizedQuery.isBlank()) return emptySet()

        val matchingFileUris = mutableSetOf<String>()
        val allPrefs = prefs.all

        for ((key, value) in allPrefs) {
            if (key.startsWith(NicknamePreferences.KEY_NICKNAME_FILE_PREFIX) && value is String) {
                val nickname = value.lowercase(Locale.getDefault())
                if (nickname.contains(normalizedQuery)) {
                    val fileUri = key.removePrefix(NicknamePreferences.KEY_NICKNAME_FILE_PREFIX)
                    matchingFileUris.add(fileUri)
                }
            }
        }

        return matchingFileUris
    }

    /**
     * Finds settings that have nicknames matching the query.
     */
    fun findSettingsWithMatchingNickname(query: String): Set<String> {
        val normalizedQuery = query.lowercase(Locale.getDefault()).trim()
        if (normalizedQuery.isBlank()) return emptySet()

        val matchingSettingIds = mutableSetOf<String>()
        val allPrefs = prefs.all

        for ((key, value) in allPrefs) {
            if (key.startsWith(NicknamePreferences.KEY_NICKNAME_SETTING_PREFIX) && value is String) {
                val nickname = value.lowercase(Locale.getDefault())
                if (nickname.contains(normalizedQuery)) {
                    val id = key.removePrefix(NicknamePreferences.KEY_NICKNAME_SETTING_PREFIX)
                    matchingSettingIds.add(id)
                }
            }
        }

        return matchingSettingIds
    }

    companion object {
        // Nickname preferences keys
        const val KEY_NICKNAME_APP_PREFIX = "nickname_app_"
        const val KEY_NICKNAME_CONTACT_PREFIX = "nickname_contact_"
        const val KEY_NICKNAME_FILE_PREFIX = "nickname_file_"
        const val KEY_NICKNAME_SETTING_PREFIX = "nickname_setting_"
    }
}
