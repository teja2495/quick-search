package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.NicknamePreferences

/**
 * Facade for nickname preference operations
 */
class NicknamePreferencesFacade(
    private val nicknamePreferences: NicknamePreferences
) {
    fun getAllAppNicknames(): Map<String, String> = nicknamePreferences.getAllAppNicknames()

    fun getAppNickname(packageName: String): String? =
            nicknamePreferences.getAppNickname(packageName)

    fun setAppNickname(
            packageName: String,
            nickname: String?,
    ) = nicknamePreferences.setAppNickname(packageName, nickname)

    fun getAllAppShortcutNicknames(): Map<String, String> =
            nicknamePreferences.getAllAppShortcutNicknames()

    fun getAppShortcutNickname(shortcutId: String): String? =
            nicknamePreferences.getAppShortcutNickname(shortcutId)

    fun setAppShortcutNickname(
            shortcutId: String,
            nickname: String?,
    ) = nicknamePreferences.setAppShortcutNickname(shortcutId, nickname)

    fun getContactNickname(contactId: Long): String? =
            nicknamePreferences.getContactNickname(contactId)

    fun setContactNickname(
            contactId: Long,
            nickname: String?,
    ) = nicknamePreferences.setContactNickname(contactId, nickname)

    fun getFileNickname(uri: String): String? = nicknamePreferences.getFileNickname(uri)

    fun setFileNickname(
            uri: String,
            nickname: String?,
    ) = nicknamePreferences.setFileNickname(uri, nickname)

    fun getSettingNickname(id: String): String? = nicknamePreferences.getSettingNickname(id)

    fun setSettingNickname(
            id: String,
            nickname: String?,
    ) = nicknamePreferences.setSettingNickname(id, nickname)

    /** Finds contact IDs that have nicknames matching the query. */
    fun findContactsWithMatchingNickname(query: String): Set<Long> =
            nicknamePreferences.findContactsWithMatchingNickname(query)

    /** Finds file URIs that have nicknames matching the query. */
    fun findFilesWithMatchingNickname(query: String): Set<String> =
            nicknamePreferences.findFilesWithMatchingNickname(query)

    /** Finds settings that have nicknames matching the query. */
    fun findSettingsWithMatchingNickname(query: String): Set<String> =
            nicknamePreferences.findSettingsWithMatchingNickname(query)
}