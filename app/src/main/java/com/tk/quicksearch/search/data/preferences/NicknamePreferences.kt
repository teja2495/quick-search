package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.search.utils.NicknameUtils
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.concurrent.ConcurrentHashMap

/**
 * Preferences for managing nicknames for apps, app shortcuts, contacts, files, and settings. Uses
 * in-memory caching to avoid expensive SharedPreferences iteration on every search.
 */
class NicknamePreferences(
    context: Context,
) : BasePreferences(context) {
    private val customizationStore = SearchCustomizationStore(context)
    private val appShortcutNicknameCache = ConcurrentHashMap<String, String>()
    private val contactNicknameCache = ConcurrentHashMap<Long, String>()
    private val fileNicknameCache = ConcurrentHashMap<String, String>()
    private val settingNicknameCache = ConcurrentHashMap<String, String>()
    private val calendarEventNicknameCache = ConcurrentHashMap<Long, String>()

    init {
        loadNicknameCaches()
    }

    fun reloadCaches() {
        AppNicknameCache.invalidate()
        appShortcutNicknameCache.clear()
        contactNicknameCache.clear()
        fileNicknameCache.clear()
        settingNicknameCache.clear()
        calendarEventNicknameCache.clear()
        loadNicknameCaches()
    }

    private fun loadNicknameCaches() {
        val allPrefs = customizationStore.snapshot()
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

                key.startsWith(BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX) -> {
                    val shortcutId = key.removePrefix(BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX)
                    appShortcutNicknameCache[shortcutId] = value
                }

                key.startsWith(BasePreferences.KEY_NICKNAME_SETTING_PREFIX) -> {
                    val id = key.removePrefix(BasePreferences.KEY_NICKNAME_SETTING_PREFIX)
                    settingNicknameCache[id] = value
                }

                key.startsWith(BasePreferences.KEY_NICKNAME_CALENDAR_EVENT_PREFIX) -> {
                    val eventId = key.removePrefix(BasePreferences.KEY_NICKNAME_CALENDAR_EVENT_PREFIX)
                    eventId.toLongOrNull()?.let { calendarEventNicknameCache[it] = value }
                }
            }
        }
    }

    // ============================================================================
    // Nickname Preferences
    // ============================================================================

    // App nicknames are keyed by AppInfo.launchCountKey(): the package name, plus ":<userId>" for
    // cloned and work-profile apps.
    // Rows read this during composition, so it is served from a process-wide map rather than
    // two SharedPreferences lookups per app.
    fun getAppNickname(appKey: String): String? = AppNicknameCache.get(customizationStore)[appKey]

    fun setAppNickname(
        appKey: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_APP_PREFIX}$appKey"
        customizationStore.putString(key, NicknameUtils.normalizeInput(nickname))
        // apply() has already updated the in-memory preferences, so the next read reloads the write.
        AppNicknameCache.invalidate()
    }

    /** Every nickname, trigger and alias entry, for detecting whether any of them changed. */
    fun getSearchCustomizationSnapshot(): Map<String, *> = customizationStore.snapshot()

    fun getAllAppNicknames(): Map<String, String> = readAppNicknames(customizationStore.snapshot())

    fun getAppShortcutNickname(shortcutId: String): String? = appShortcutNicknameCache[shortcutId]

    fun setAppShortcutNickname(
        shortcutId: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX}$shortcutId"
        val normalized = NicknameUtils.normalizeInput(nickname)
        customizationStore.putString(key, normalized)
        if (normalized == null) {
            appShortcutNicknameCache.remove(shortcutId)
        } else {
            appShortcutNicknameCache[shortcutId] = normalized
        }
    }

    fun getAllAppShortcutNicknames(): Map<String, String> = appShortcutNicknameCache.toMap()

    fun getAllContactNicknames(): Map<Long, String> = contactNicknameCache.toMap()

    fun getContactNickname(contactId: Long): String? = contactNicknameCache[contactId]

    fun setContactNickname(
        contactId: Long,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_CONTACT_PREFIX}$contactId"
        val normalized = NicknameUtils.normalizeInput(nickname)
        customizationStore.putString(key, normalized)
        if (normalized == null) {
            contactNicknameCache.remove(contactId)
        } else {
            contactNicknameCache[contactId] = normalized
        }
    }

    fun getAllFileNicknames(): Map<String, String> = fileNicknameCache.toMap()

    fun getFileNickname(uri: String): String? = fileNicknameCache[uri]

    fun setFileNickname(
        uri: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_FILE_PREFIX}$uri"
        val normalized = NicknameUtils.normalizeInput(nickname)
        customizationStore.putString(key, normalized)
        if (normalized == null) {
            fileNicknameCache.remove(uri)
        } else {
            fileNicknameCache[uri] = normalized
        }
    }

    fun getAllSettingNicknames(): Map<String, String> = settingNicknameCache.toMap()

    fun getSettingNickname(id: String): String? = settingNicknameCache[id]

    fun setSettingNickname(
        id: String,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_SETTING_PREFIX}$id"
        val normalized = NicknameUtils.normalizeInput(nickname)
        customizationStore.putString(key, normalized)
        if (normalized == null) {
            settingNicknameCache.remove(id)
        } else {
            settingNicknameCache[id] = normalized
        }
    }

    fun getAllCalendarEventNicknames(): Map<Long, String> = calendarEventNicknameCache.toMap()

    fun getCalendarEventNickname(eventId: Long): String? = calendarEventNicknameCache[eventId]

    fun setCalendarEventNickname(
        eventId: Long,
        nickname: String?,
    ) {
        val key = "${BasePreferences.KEY_NICKNAME_CALENDAR_EVENT_PREFIX}$eventId"
        val normalized = NicknameUtils.normalizeInput(nickname)
        customizationStore.putString(key, normalized)
        if (normalized == null) {
            calendarEventNicknameCache.remove(eventId)
        } else {
            calendarEventNicknameCache[eventId] = normalized
        }
    }

    /**
     * Finds contact IDs that have nicknames matching the query. Uses in-memory cache for O(1)
     * lookup instead of O(n) SharedPreferences iteration.
     */
    fun findContactsWithMatchingNickname(query: String): Set<Long> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query).trim()
        if (normalizedQuery.isBlank()) return emptySet()
        val compactQuery = SearchTextNormalizer.removeSearchWhitespace(normalizedQuery)

        val matchingContactIds = mutableSetOf<Long>()
        for ((contactId, nickname) in contactNicknameCache) {
            if (nicknameMatchesQuery(nickname, normalizedQuery, compactQuery)) {
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
        val compactQuery = SearchTextNormalizer.removeSearchWhitespace(normalizedQuery)

        val matchingFileUris = mutableSetOf<String>()
        for ((uri, nickname) in fileNicknameCache) {
            if (nicknameMatchesQuery(nickname, normalizedQuery, compactQuery)) {
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
        val compactQuery = SearchTextNormalizer.removeSearchWhitespace(normalizedQuery)

        val matchingSettingIds = mutableSetOf<String>()
        for ((id, nickname) in settingNicknameCache) {
            if (nicknameMatchesQuery(nickname, normalizedQuery, compactQuery)) {
                matchingSettingIds.add(id)
            }
        }

        return matchingSettingIds
    }

    /** Finds calendar event IDs that have nicknames matching the query. */
    fun findCalendarEventsWithMatchingNickname(query: String): Set<Long> {
        val normalizedQuery = SearchTextNormalizer.normalizeForSearch(query).trim()
        if (normalizedQuery.isBlank()) return emptySet()
        val compactQuery = SearchTextNormalizer.removeSearchWhitespace(normalizedQuery)

        val matchingEventIds = mutableSetOf<Long>()
        for ((eventId, nickname) in calendarEventNicknameCache) {
            if (nicknameMatchesQuery(nickname, normalizedQuery, compactQuery)) {
                matchingEventIds.add(eventId)
            }
        }

        return matchingEventIds
    }

    private fun nicknameMatchesQuery(
        nickname: String,
        normalizedQuery: String,
        compactQuery: String,
    ): Boolean =
        normalizedAliases(nickname).any { alias ->
            alias.normalized.contains(normalizedQuery) || alias.compact.contains(compactQuery)
        }

    private class NormalizedAlias(
        val normalized: String,
        val compact: String,
    )

    /**
     * Process-wide app nickname map. Any write invalidates it: [setAppNickname] directly, and other
     * writers (backup restore, clears) through a listener on the store, so the next read reloads
     * from preferences instead of serving a stale value.
     */
    private object AppNicknameCache {
        private val lock = Any()

        @Volatile
        private var nicknames: Map<String, String>? = null

        @Volatile
        private var source: Any? = null

        // Held strongly here: SharedPreferences keeps its listeners only weakly.
        private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

        fun get(store: SearchCustomizationStore): Map<String, String> {
            val current = nicknames
            if (current != null && source === store.preferencesIdentity) return current
            return synchronized(lock) {
                val identity = store.preferencesIdentity
                nicknames?.takeIf { source === identity }
                    ?: readAppNicknames(store.snapshot()).also { loaded ->
                        if (source !== identity) {
                            // Only differs from the first instance when the preferences object was
                            // replaced (a new Application in tests); listen to the live one.
                            val changeListener =
                                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                                    if (key == null || isAppNicknameKey(key)) invalidate()
                                }
                            listener = changeListener
                            store.registerChangeListener(changeListener)
                            source = identity
                        }
                        nicknames = loaded
                    }
            }
        }

        fun invalidate() {
            synchronized(lock) { nicknames = null }
        }
    }

    private companion object {
        private const val MAX_NORMALIZED_ALIAS_CACHE_SIZE = 2_048
        private val normalizedAliasCache = ConcurrentHashMap<String, List<NormalizedAlias>>()

        fun isAppNicknameKey(key: String): Boolean =
            key.startsWith(BasePreferences.KEY_NICKNAME_APP_PREFIX) &&
                !key.startsWith(BasePreferences.KEY_NICKNAME_APP_SHORTCUT_PREFIX)

        fun readAppNicknames(allPrefs: Map<String, *>): Map<String, String> {
            val nicknames = mutableMapOf<String, String>()
            for ((key, value) in allPrefs) {
                if (isAppNicknameKey(key) && value is String) {
                    nicknames[key.removePrefix(BasePreferences.KEY_NICKNAME_APP_PREFIX)] = value
                }
            }
            return nicknames
        }

        /** Normalizing is pure, so each saved nickname is normalized once instead of per query. */
        fun normalizedAliases(nickname: String): List<NormalizedAlias> =
            normalizedAliasCache[nickname] ?: run {
                val aliases = if (NicknameUtils.hasMultiple(nickname)) NicknameUtils.split(nickname) else listOf(nickname)
                val normalized =
                    aliases.map { alias ->
                        val normalizedAlias = SearchTextNormalizer.normalizeForSearch(alias)
                        NormalizedAlias(normalizedAlias, SearchTextNormalizer.removeSearchWhitespace(normalizedAlias))
                    }
                if (normalizedAliasCache.size >= MAX_NORMALIZED_ALIAS_CACHE_SIZE) normalizedAliasCache.clear()
                normalizedAliasCache[nickname] = normalized
                normalized
            }
    }
}
