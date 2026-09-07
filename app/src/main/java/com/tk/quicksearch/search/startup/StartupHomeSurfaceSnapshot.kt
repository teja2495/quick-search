package com.tk.quicksearch.search.startup

import android.content.Intent
import android.net.Uri
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultAction
import com.tk.quicksearch.search.appSettings.AppSettingResultSource
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.AppSettingsToggleKey
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchHistory.RecentSearchEntry
import com.tk.quicksearch.search.searchHistory.RecentSearchItem
import org.json.JSONArray
import org.json.JSONObject

/** A bounded, render-ready copy of the empty-query Home content. */
data class StartupHomeSurfaceSnapshot(
    val pinnedApps: List<AppInfo> = emptyList(),
    val recentApps: List<AppInfo> = emptyList(),
    val pinnedContacts: List<ContactInfo> = emptyList(),
    val pinnedFiles: List<DeviceFile> = emptyList(),
    val pinnedSettings: List<DeviceSetting> = emptyList(),
    val pinnedCalendarEvents: List<CalendarEventInfo> = emptyList(),
    val pinnedNotes: List<NoteInfo> = emptyList(),
    val pinnedAppShortcuts: List<StaticShortcut> = emptyList(),
    val recentItems: List<RecentSearchItem> = emptyList(),
) {
    fun bounded(): StartupHomeSurfaceSnapshot =
        copy(
            pinnedApps = pinnedApps.take(MAX_PINNED_ITEMS),
            recentApps = recentApps.take(MAX_APP_SUGGESTIONS),
            pinnedContacts = pinnedContacts.take(MAX_PINNED_ITEMS),
            pinnedFiles = pinnedFiles.take(MAX_PINNED_ITEMS),
            pinnedSettings = pinnedSettings.take(MAX_PINNED_ITEMS),
            pinnedCalendarEvents = pinnedCalendarEvents.take(MAX_PINNED_ITEMS),
            pinnedNotes = pinnedNotes.take(MAX_PINNED_ITEMS),
            pinnedAppShortcuts =
                pinnedAppShortcuts.take(MAX_PINNED_ITEMS).map { it.copy(iconBase64 = null) },
            recentItems = recentItems.take(MAX_RECENT_ITEMS),
        )

    companion object {
        private const val MAX_APP_SUGGESTIONS = 24
        private const val MAX_PINNED_ITEMS = 50
        private const val MAX_RECENT_ITEMS = 30
    }
}

internal object StartupHomeSurfaceSnapshotJson {
    private const val MAX_CACHED_NOTE_PREVIEW_CHARS = 500
    private const val TYPE = "type"
    private const val ENTRY = "entry"
    private const val PAYLOAD = "payload"

    fun toJson(snapshot: StartupHomeSurfaceSnapshot): JSONObject {
        val bounded = snapshot.bounded()
        return JSONObject().apply {
            put("pinnedApps", bounded.pinnedApps.toAppArray())
            put("recentApps", bounded.recentApps.toAppArray())
            put("pinnedContacts", bounded.pinnedContacts.toJsonArray(::contactToJson))
            put("pinnedFiles", bounded.pinnedFiles.toJsonArray(::fileToJson))
            put("pinnedSettings", bounded.pinnedSettings.toJsonArray(::settingToJson))
            put("pinnedCalendarEvents", bounded.pinnedCalendarEvents.toJsonArray(::calendarToJson))
            put("pinnedNotes", bounded.pinnedNotes.toJsonArray(::noteToJson))
            put("pinnedAppShortcuts", bounded.pinnedAppShortcuts.toJsonArray(::shortcutToJson))
            put("recentItems", bounded.recentItems.toJsonArray(::recentItemToJson))
        }
    }

    fun fromJson(json: JSONObject?): StartupHomeSurfaceSnapshot {
        if (json == null) return StartupHomeSurfaceSnapshot()
        return StartupHomeSurfaceSnapshot(
            pinnedApps = json.optJSONArray("pinnedApps").toAppList(),
            recentApps = json.optJSONArray("recentApps").toAppList(),
            pinnedContacts = json.optJSONArray("pinnedContacts").mapObjects(::contactFromJson),
            pinnedFiles = json.optJSONArray("pinnedFiles").mapObjects(::fileFromJson),
            pinnedSettings = json.optJSONArray("pinnedSettings").mapObjects(::settingFromJson),
            pinnedCalendarEvents =
                json.optJSONArray("pinnedCalendarEvents").mapObjects(::calendarFromJson),
            pinnedNotes = json.optJSONArray("pinnedNotes").mapObjects(::noteFromJson),
            pinnedAppShortcuts =
                json.optJSONArray("pinnedAppShortcuts").mapObjects(::shortcutFromJson),
            recentItems = json.optJSONArray("recentItems").mapObjects(::recentItemFromJson),
        ).bounded()
    }

    private fun List<AppInfo>.toAppArray(): JSONArray =
        toJsonArray { app ->
            JSONObject().apply {
                put("appName", app.appName)
                put("packageName", app.packageName)
                put("lastUsedTime", app.lastUsedTime)
                put("totalTimeInForeground", app.totalTimeInForeground)
                put("launchCount", app.launchCount)
                put("firstInstallTime", app.firstInstallTime)
                put("lastUpdateTime", app.lastUpdateTime)
                put("isSystemApp", app.isSystemApp)
                put("hasLaunchIntent", app.hasLaunchIntent)
                app.userHandleId?.let { put("userHandleId", it) }
                app.componentName?.let { put("componentName", it) }
            }
        }

    private fun JSONArray?.toAppList(): List<AppInfo> =
        mapObjects { app ->
            val packageName = app.stringOrNull("packageName") ?: return@mapObjects null
            AppInfo(
                appName = app.optString("appName").ifBlank { packageName },
                packageName = packageName,
                lastUsedTime = app.optLong("lastUsedTime"),
                totalTimeInForeground = app.optLong("totalTimeInForeground"),
                launchCount = app.optInt("launchCount"),
                firstInstallTime = app.optLong("firstInstallTime"),
                lastUpdateTime = app.optLong("lastUpdateTime", app.optLong("firstInstallTime")),
                isSystemApp = app.optBoolean("isSystemApp"),
                hasLaunchIntent = app.optBoolean("hasLaunchIntent", true),
                userHandleId = app.optInt("userHandleId", -1).takeIf { it >= 0 },
                componentName = app.stringOrNull("componentName"),
            )
        }

    private fun contactToJson(contact: ContactInfo) = JSONObject().apply {
        put("contactId", contact.contactId)
        put("lookupKey", contact.lookupKey)
        put("displayName", contact.displayName)
        put("phoneNumbers", contact.phoneNumbers.toStringArray())
        put("phoneNumberLabels", JSONObject(contact.phoneNumberLabels))
        contact.photoUri?.let { put("photoUri", it) }
        put("contactMethods", contact.contactMethods.toJsonArray(::contactMethodToJson))
    }

    private fun contactFromJson(json: JSONObject): ContactInfo? {
        val contactId = json.optLong("contactId", -1L).takeIf { it >= 0L } ?: return null
        return ContactInfo(
            contactId = contactId,
            lookupKey = json.optString("lookupKey"),
            displayName = json.optString("displayName"),
            phoneNumbers = json.optJSONArray("phoneNumbers").toStringList(),
            phoneNumberLabels = json.optJSONObject("phoneNumberLabels").toStringMap(),
            photoUri = json.stringOrNull("photoUri"),
            contactMethods = json.optJSONArray("contactMethods").mapObjects(::contactMethodFromJson),
        )
    }

    private fun contactMethodToJson(method: ContactMethod) = JSONObject().apply {
        put(TYPE, method.javaClass.simpleName)
        put("displayLabel", method.displayLabel)
        put("data", method.data)
        method.dataId?.let { put("dataId", it) }
        put("isPrimary", method.isPrimary)
        when (method) {
            is ContactMethod.VideoCall -> put("packageName", method.packageName)
            is ContactMethod.CustomApp -> {
                put("mimeType", method.mimeType)
                method.packageName?.let { put("packageName", it) }
            }
            else -> Unit
        }
    }

    private fun contactMethodFromJson(json: JSONObject): ContactMethod? {
        val label = json.optString("displayLabel")
        val data = json.optString("data")
        val dataId = json.optLong("dataId", -1L).takeIf { it >= 0L }
        val primary = json.optBoolean("isPrimary")
        return when (json.optString(TYPE)) {
            "Phone" -> ContactMethod.Phone(label, data, dataId, primary)
            "Sms" -> ContactMethod.Sms(label, data, dataId, primary)
            "WhatsAppCall" -> ContactMethod.WhatsAppCall(label, data, dataId, primary)
            "WhatsAppMessage" -> ContactMethod.WhatsAppMessage(label, data, dataId, primary)
            "WhatsAppVideoCall" -> ContactMethod.WhatsAppVideoCall(label, data, dataId, primary)
            "TelegramMessage" -> ContactMethod.TelegramMessage(label, data, dataId, primary)
            "TelegramCall" -> ContactMethod.TelegramCall(label, data, dataId, primary)
            "TelegramVideoCall" -> ContactMethod.TelegramVideoCall(label, data, dataId, primary)
            "SignalMessage" -> ContactMethod.SignalMessage(label, data, dataId, primary)
            "SignalCall" -> ContactMethod.SignalCall(label, data, dataId, primary)
            "SignalVideoCall" -> ContactMethod.SignalVideoCall(label, data, dataId, primary)
            "Email" -> ContactMethod.Email(label, data, dataId, primary)
            "VideoCall" -> ContactMethod.VideoCall(label, data, json.optString("packageName"), dataId, primary)
            "GoogleMeet" -> ContactMethod.GoogleMeet(label, data, dataId, primary)
            "CustomApp" -> ContactMethod.CustomApp(
                label,
                data,
                json.optString("mimeType"),
                json.stringOrNull("packageName"),
                dataId,
                primary,
            )
            "ViewInContactsApp" -> ContactMethod.ViewInContactsApp(label, data, dataId, primary)
            else -> null
        }
    }

    private fun fileToJson(file: DeviceFile) = JSONObject().apply {
        put("uri", file.uri.toString())
        put("displayName", file.displayName)
        file.mimeType?.let { put("mimeType", it) }
        put("lastModified", file.lastModified)
        put("isDirectory", file.isDirectory)
        file.relativePath?.let { put("relativePath", it) }
        file.volumeName?.let { put("volumeName", it) }
    }

    private fun fileFromJson(json: JSONObject): DeviceFile? =
        json.stringOrNull("uri")?.let { uri ->
            DeviceFile(
                uri = Uri.parse(uri),
                displayName = json.optString("displayName"),
                mimeType = json.stringOrNull("mimeType"),
                lastModified = json.optLong("lastModified"),
                isDirectory = json.optBoolean("isDirectory"),
                relativePath = json.stringOrNull("relativePath"),
                volumeName = json.stringOrNull("volumeName"),
            )
        }

    private fun settingToJson(setting: DeviceSetting) = JSONObject().apply {
        put("id", setting.id)
        put("title", setting.title)
        setting.description?.let { put("description", it) }
        put("keywords", setting.keywords.toStringArray())
        put("action", setting.action)
        setting.data?.let { put("data", it) }
        put("categories", setting.categories.toStringArray())
        put("extras", JSONObject(setting.extras))
        put("minSdk", setting.minSdk)
        put("maxSdk", setting.maxSdk)
    }

    private fun settingFromJson(json: JSONObject): DeviceSetting? {
        val id = json.stringOrNull("id") ?: return null
        val action = json.stringOrNull("action") ?: return null
        return DeviceSetting(
            id = id,
            title = json.optString("title"),
            description = json.stringOrNull("description"),
            keywords = json.optJSONArray("keywords").toStringList(),
            action = action,
            data = json.stringOrNull("data"),
            categories = json.optJSONArray("categories").toStringList(),
            extras = json.optJSONObject("extras").toValueMap(),
            minSdk = json.optInt("minSdk", 1),
            maxSdk = json.optInt("maxSdk", Int.MAX_VALUE),
        )
    }

    private fun calendarToJson(event: CalendarEventInfo) = JSONObject().apply {
        put("eventId", event.eventId)
        put("title", event.title)
        put("startMillis", event.startMillis)
        put("endMillis", event.endMillis)
        put("allDay", event.allDay)
        event.recurrenceRule?.let { put("recurrenceRule", it) }
    }

    private fun calendarFromJson(json: JSONObject): CalendarEventInfo? {
        val eventId = json.optLong("eventId", -1L).takeIf { it >= 0L } ?: return null
        return CalendarEventInfo(
            eventId = eventId,
            title = json.optString("title"),
            startMillis = json.optLong("startMillis"),
            endMillis = json.optLong("endMillis"),
            allDay = json.optBoolean("allDay"),
            recurrenceRule = json.stringOrNull("recurrenceRule"),
        )
    }

    private fun noteToJson(note: NoteInfo) = JSONObject().apply {
        put("noteId", note.noteId)
        put("title", note.title)
        // Home rows only need a preview; note detail navigation reloads the canonical Room row by ID.
        put("markdownContent", note.markdownContent.take(MAX_CACHED_NOTE_PREVIEW_CHARS))
        put("createdAtMillis", note.createdAtMillis)
        put("updatedAtMillis", note.updatedAtMillis)
    }

    private fun noteFromJson(json: JSONObject): NoteInfo? {
        val noteId = json.optLong("noteId", -1L).takeIf { it > 0L } ?: return null
        return NoteInfo(
            noteId = noteId,
            title = json.optString("title"),
            markdownContent = json.optString("markdownContent"),
            createdAtMillis = json.optLong("createdAtMillis"),
            updatedAtMillis = json.optLong("updatedAtMillis"),
        )
    }

    private fun shortcutToJson(shortcut: StaticShortcut) = JSONObject().apply {
        put("packageName", shortcut.packageName)
        put("appLabel", shortcut.appLabel)
        put("id", shortcut.id)
        shortcut.shortLabel?.let { put("shortLabel", it) }
        shortcut.longLabel?.let { put("longLabel", it) }
        shortcut.iconResId?.let { put("iconResId", it) }
        put("enabled", shortcut.enabled)
        put("intents", JSONArray().apply {
            shortcut.intents.forEach { intent ->
                runCatching { intent.toUri(Intent.URI_INTENT_SCHEME) }.getOrNull()?.let(::put)
            }
        })
    }

    private fun shortcutFromJson(json: JSONObject): StaticShortcut? {
        val packageName = json.stringOrNull("packageName") ?: return null
        val id = json.stringOrNull("id") ?: return null
        val intents =
            json.optJSONArray("intents").toStringList().mapNotNull { raw ->
                runCatching { Intent.parseUri(raw, Intent.URI_INTENT_SCHEME) }.getOrNull()
            }
        if (intents.isEmpty()) return null
        return StaticShortcut(
            packageName = packageName,
            appLabel = json.optString("appLabel", packageName),
            id = id,
            shortLabel = json.stringOrNull("shortLabel"),
            longLabel = json.stringOrNull("longLabel"),
            iconResId = json.optInt("iconResId", -1).takeIf { it >= 0 },
            iconBase64 = null,
            enabled = json.optBoolean("enabled", true),
            intents = intents,
        )
    }

    private fun appSettingToJson(setting: AppSettingResult) = JSONObject().apply {
        put("id", setting.id)
        put("title", setting.title)
        setting.description?.let { put("description", it) }
        put("keywords", setting.keywords.toStringArray())
        put("source", setting.source.name)
        put("action", setting.action.name)
        setting.destination?.let { put("destination", it.name) }
        setting.toggleKey?.let { put("toggleKey", it.name) }
    }

    private fun appSettingFromJson(json: JSONObject): AppSettingResult? {
        val id = json.stringOrNull("id") ?: return null
        val action = json.enumOrNull<AppSettingResultAction>("action") ?: return null
        val destination = json.enumOrNull<AppSettingsDestination>("destination")
        val toggleKey = json.enumOrNull<AppSettingsToggleKey>("toggleKey")
        return runCatching {
            AppSettingResult(
                id = id,
                title = json.optString("title"),
                description = json.stringOrNull("description"),
                keywords = json.optJSONArray("keywords").toStringList(),
                source = json.enumOrNull<AppSettingResultSource>("source") ?: AppSettingResultSource.APP,
                action = action,
                destination = destination,
                toggleKey = toggleKey,
            )
        }.getOrNull()
    }

    private fun recentItemToJson(item: RecentSearchItem) = JSONObject().apply {
        put(ENTRY, item.entry.toJsonString())
        when (item) {
            is RecentSearchItem.Query -> put(TYPE, "query")
            is RecentSearchItem.Contact -> { put(TYPE, "contact"); put(PAYLOAD, contactToJson(item.contact)) }
            is RecentSearchItem.File -> { put(TYPE, "file"); put(PAYLOAD, fileToJson(item.file)) }
            is RecentSearchItem.Setting -> { put(TYPE, "setting"); put(PAYLOAD, settingToJson(item.setting)) }
            is RecentSearchItem.AppShortcut -> { put(TYPE, "shortcut"); put(PAYLOAD, shortcutToJson(item.shortcut)) }
            is RecentSearchItem.AppSetting -> { put(TYPE, "appSetting"); put(PAYLOAD, appSettingToJson(item.setting)) }
            is RecentSearchItem.Note -> { put(TYPE, "note"); put(PAYLOAD, noteToJson(item.note)) }
        }
    }

    private fun recentItemFromJson(json: JSONObject): RecentSearchItem? {
        val entry = json.stringOrNull(ENTRY)?.let(RecentSearchEntry::fromRaw) ?: return null
        val payload = json.optJSONObject(PAYLOAD)
        return when (json.optString(TYPE)) {
            "query" -> (entry as? RecentSearchEntry.Query)?.let(RecentSearchItem::Query)
            "contact" -> (entry as? RecentSearchEntry.Contact)?.let { e -> payload?.let(::contactFromJson)?.let { RecentSearchItem.Contact(e, it) } }
            "file" -> (entry as? RecentSearchEntry.File)?.let { e -> payload?.let(::fileFromJson)?.let { RecentSearchItem.File(e, it) } }
            "setting" -> (entry as? RecentSearchEntry.Setting)?.let { e -> payload?.let(::settingFromJson)?.let { RecentSearchItem.Setting(e, it) } }
            "shortcut" -> (entry as? RecentSearchEntry.AppShortcut)?.let { e -> payload?.let(::shortcutFromJson)?.let { RecentSearchItem.AppShortcut(e, it) } }
            "appSetting" -> (entry as? RecentSearchEntry.AppSetting)?.let { e -> payload?.let(::appSettingFromJson)?.let { RecentSearchItem.AppSetting(e, it) } }
            "note" -> (entry as? RecentSearchEntry.Note)?.let { e -> payload?.let(::noteFromJson)?.let { RecentSearchItem.Note(e, it) } }
            else -> null
        }
    }

    private inline fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray =
        JSONArray().apply { forEach { put(transform(it)) } }

    private fun List<String>.toStringArray(): JSONArray = JSONArray().apply { forEach(::put) }

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T?): List<T> =
        buildList {
            val array = this@mapObjects ?: return@buildList
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let(transform)?.let(::add)
            }
        }

    private fun JSONArray?.toStringList(): List<String> =
        buildList {
            val array = this@toStringList ?: return@buildList
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }

    private fun JSONObject?.toStringMap(): Map<String, String> =
        buildMap {
            val json = this@toStringMap ?: return@buildMap
            json.keys().forEach { key -> json.stringOrNull(key)?.let { put(key, it) } }
        }

    private fun JSONObject?.toValueMap(): Map<String, Any> =
        buildMap {
            val json = this@toValueMap ?: return@buildMap
            json.keys().forEach { key ->
                when (val value = json.opt(key)) {
                    is String, is Boolean, is Int, is Long -> put(key, value)
                    is Number -> put(key, value.toLong())
                }
            }
        }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private inline fun <reified T : Enum<T>> JSONObject.enumOrNull(key: String): T? =
        stringOrNull(key)?.let { raw -> enumValues<T>().firstOrNull { it.name == raw } }
}
