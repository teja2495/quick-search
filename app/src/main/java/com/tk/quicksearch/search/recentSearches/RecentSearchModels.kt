package com.tk.quicksearch.search.recentSearches

import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import org.json.JSONObject

sealed class RecentSearchEntry {
    abstract val stableKey: String

    data class Query(val query: String) : RecentSearchEntry() {
        val trimmedQuery = query.trim()
        override val stableKey: String = "query:$trimmedQuery"
    }

    data class Contact(val contactId: Long) : RecentSearchEntry() {
        override val stableKey: String = "contact:$contactId"
    }

    data class File(val uri: String) : RecentSearchEntry() {
        override val stableKey: String = "file:$uri"
    }

    data class Setting(val id: String) : RecentSearchEntry() {
        override val stableKey: String = "setting:$id"
    }

    data class AppShortcut(val shortcutKey: String) : RecentSearchEntry() {
        override val stableKey: String = "app_shortcut:$shortcutKey"
    }

    fun toJsonString(): String {
        val json = JSONObject()
        when (this) {
            is Query -> {
                json.put(FIELD_TYPE, TYPE_QUERY)
                json.put(FIELD_QUERY, trimmedQuery)
            }
            is Contact -> {
                json.put(FIELD_TYPE, TYPE_CONTACT)
                json.put(FIELD_CONTACT_ID, contactId)
            }
            is File -> {
                json.put(FIELD_TYPE, TYPE_FILE)
                json.put(FIELD_FILE_URI, uri)
            }
            is Setting -> {
                json.put(FIELD_TYPE, TYPE_SETTING)
                json.put(FIELD_SETTING_ID, id)
            }
            is AppShortcut -> {
                json.put(FIELD_TYPE, TYPE_APP_SHORTCUT)
                json.put(FIELD_SHORTCUT_KEY, shortcutKey)
            }
        }
        return json.toString()
    }

    companion object {
        private const val FIELD_TYPE = "type"
        private const val FIELD_QUERY = "query"
        private const val FIELD_CONTACT_ID = "contactId"
        private const val FIELD_FILE_URI = "fileUri"
        private const val FIELD_SETTING_ID = "settingId"
        private const val FIELD_SHORTCUT_KEY = "shortcutKey"

        private const val TYPE_QUERY = "query"
        private const val TYPE_CONTACT = "contact"
        private const val TYPE_FILE = "file"
        private const val TYPE_SETTING = "setting"
        private const val TYPE_APP_SHORTCUT = "app_shortcut"

        fun fromRaw(raw: String): RecentSearchEntry? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            val parsed =
                runCatching {
                    val json = JSONObject(trimmed)
                    when (json.optString(FIELD_TYPE)) {
                        TYPE_QUERY -> json.optString(FIELD_QUERY)
                            .takeIf { it.isNotBlank() }
                            ?.let { Query(it) }
                        TYPE_CONTACT -> json.optLong(FIELD_CONTACT_ID, -1L)
                            .takeIf { it >= 0L }
                            ?.let { Contact(it) }
                        TYPE_FILE -> json.optString(FIELD_FILE_URI)
                            .takeIf { it.isNotBlank() }
                            ?.let { File(it) }
                        TYPE_SETTING -> json.optString(FIELD_SETTING_ID)
                            .takeIf { it.isNotBlank() }
                            ?.let { Setting(it) }
                        TYPE_APP_SHORTCUT -> json.optString(FIELD_SHORTCUT_KEY)
                            .takeIf { it.isNotBlank() }
                            ?.let { AppShortcut(it) }
                        else -> null
                    }
                }.getOrNull()

            return parsed ?: Query(trimmed)
        }
    }
}

sealed class RecentSearchItem(open val entry: RecentSearchEntry) {
    data class Query(val value: String) :
        RecentSearchItem(RecentSearchEntry.Query(value))

    data class Contact(
        override val entry: RecentSearchEntry.Contact,
        val contact: ContactInfo
    ) : RecentSearchItem(entry)

    data class File(
        override val entry: RecentSearchEntry.File,
        val file: DeviceFile
    ) : RecentSearchItem(entry)

    data class Setting(
        override val entry: RecentSearchEntry.Setting,
        val setting: DeviceSetting
    ) : RecentSearchItem(entry)

    data class AppShortcut(
        override val entry: RecentSearchEntry.AppShortcut,
        val shortcut: StaticShortcut
    ) : RecentSearchItem(entry)
}
