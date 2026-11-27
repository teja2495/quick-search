package com.tk.quicksearch.settings.settingsScreen

import android.content.Context
import android.net.Uri
import com.tk.quicksearch.R
import com.tk.quicksearch.customInfo.CustomInfoRepository
import com.tk.quicksearch.customInfo.CustomInfoScheduler
import com.tk.quicksearch.reminders.ReminderScheduler
import com.tk.quicksearch.search.apps.IconPackManager
import com.tk.quicksearch.settings.autoBackup.AutoBackupStore
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.BootstrapPreferences
import com.tk.quicksearch.search.data.preferences.CustomLlmProviderPreferences
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.notes.NotesRoomStore
import com.tk.quicksearch.search.data.bookmarks.BookmarkBackupCodec
import com.tk.quicksearch.search.data.bookmarks.BookmarkRepository
import com.tk.quicksearch.widgets.utils.requestNoteWidgetsRefresh
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.CustomLlmProviderConfig
import java.io.File
import java.io.IOException
import java.io.Writer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes and restores backup-eligible SharedPreferences into a portable JSON file.
 */
object SettingsBackupManager {
    /** Entries are serialized by name. Preference matchers run in declaration order. */
    enum class ExportItem(
        internal val option: BackupSectionOption? = null,
        internal val defaultSelected: Boolean = true,
        internal val matchPreference: ((file: String, key: String) -> Boolean)? = null,
        internal val restoreLegacyPreferences: Boolean = true,
        internal val payload: BackupSectionPayload? = null,
    ) {
        SETTINGS,
        PINNED_ITEMS(
            option = BackupSectionOption(
                R.string.settings_backup_export_option_pinned_items_title,
                { MaterialSymbols.PushPin },
                { source ->
                    listOf(
                        BasePreferences.KEY_PINNED,
                        BasePreferences.KEY_PINNED_CONTACT_IDS,
                        BasePreferences.KEY_PINNED_FILE_URIS,
                        BasePreferences.KEY_PINNED_SETTINGS,
                        BasePreferences.KEY_PINNED_CALENDAR_EVENT_IDS,
                        BasePreferences.KEY_PINNED_REMINDER_IDS,
                        BasePreferences.KEY_PINNED_APP_SHORTCUTS,
                    ).any { key -> source.preferences.getStringSet(key, emptySet()).orEmpty().isNotEmpty() }
                },
            ),
            matchPreference = ::isPinnedItemKey,
        ),
        SHORTCUTS(
            matchPreference = { file, key -> isShortcutKey(file, key) || file == "app_shortcut_cache" },
        ),
        NOTES(
            option = BackupSectionOption(
                R.string.section_notes,
                { MaterialSymbols.Description },
                { NotesRoomStore(it.context).hasAny() },
            ),
            matchPreference = ::isNotesKey,
        ),
        SEARCH_ENGINES(matchPreference = ::isSearchEngineKey),
        REMINDERS(
            option = BackupSectionOption(
                R.string.section_reminders,
                { MaterialSymbols.NotificationsActive },
                {
                    val reminders = it.preferences.getString(BasePreferences.KEY_REMINDERS_DATA, null).orEmpty()
                    reminders.isNotBlank() && reminders != "[]"
                },
            ),
            matchPreference = ::isRemindersKey,
        ),
        BOOKMARKS(
            option = BackupSectionOption(
                R.string.section_bookmarks,
                { MaterialSymbols.Bookmark },
                { BookmarkRepository(it.context).hasBookmarks() },
            ),
            payload = BackupSectionPayload(
                field = "bookmarks",
                export = { BookmarkBackupCodec.serialize(BookmarkRepository(it).getAll()) },
                restore = { context, array ->
                    BookmarkRepository(context).replaceFromBackup(BookmarkBackupCodec.parse(array))
                },
            ),
        ),
        SEARCH_HISTORY(
            option = BackupSectionOption(
                R.string.recent_queries_toggle_title,
                { MaterialSymbols.History },
                { it.preferences.getBoolean(BasePreferences.KEY_RECENT_QUERIES_ENABLED, true) },
            ),
            matchPreference = ::isSearchHistoryKey,
            restoreLegacyPreferences = false,
        ),
        API_KEYS(
            option = BackupSectionOption(
                R.string.settings_backup_export_option_api_keys,
                { MaterialSymbols.Key },
                { UserAppPreferences(it.context).hasAnyLlmApiKey() },
            ),
            defaultSelected = false,
        ),
        ;

        companion object {
            internal val selectableItems = entries.filter { it.option != null }
            internal val requiredItems = entries.filter { it.option == null }.toSet()
            internal val defaultSelection = entries.filter { it.defaultSelected }.toSet()
            private val preferenceSections = entries.filter { it.matchPreference != null }
            internal val payloadSections = entries.filter { it.payload != null }

            internal fun forPreference(file: String, key: String): ExportItem? =
                preferenceSections.firstOrNull { it.matchPreference?.invoke(file, key) == true }
                    ?: SETTINGS.takeIf { file == BasePreferences.PREFS_NAME }
        }
    }

    data class ExportOptions(
        val selectedItems: Set<ExportItem> = ExportItem.defaultSelection,
    ) {
        fun includes(item: ExportItem): Boolean = item in selectedItems
    }

    private const val FORMAT_VERSION = 2

    private const val FIELD_FORMAT_VERSION = "formatVersion"
    private const val FIELD_EXPORTED_AT_EPOCH_MS = "exportedAtEpochMs"
    private const val FIELD_PREFERENCES = "preferences"
    private const val FIELD_LLM_API_KEYS = "llmApiKeys"
    // Stored inside FIELD_LLM_API_KEYS alongside the LLM provider keys.
    private const val TAVILY_API_KEY_FIELD = "tavily"
    private const val FIELD_LLM_PERSONAL_CONTEXTS = "llmPersonalContexts"
    private const val FIELD_CUSTOM_LLM_PROVIDERS = "customLlmProviders"
    private const val FIELD_SELECTED_EXPORT_ITEMS = "selectedExportItems"
    private const val FIELD_NOTES = "notes"
    // The icon pack lives in session prefs, which aren't exported, so it travels as its own field.
    private const val FIELD_SELECTED_ICON_PACK = "selectedIconPack"
    private const val FIELD_TYPE = "type"
    private const val FIELD_VALUE = "value"

    private const val TYPE_STRING = "string"
    private const val TYPE_BOOLEAN = "boolean"
    private const val TYPE_INT = "int"
    private const val TYPE_LONG = "long"
    private const val TYPE_FLOAT = "float"
    private const val TYPE_STRING_SET = "string_set"

    private const val STARTUP_SURFACE_PREFS_NAME = "startup_surface_store"

    private val excludedPreferenceFiles =
        setOf(
            "bootstrap_preferences",
            "search_customization_index",
            "ai_startup_state",
            "startup_preferences_snapshot",
            "notes_store_state",
            "managed_asset_index",
            BasePreferences.FIRST_LAUNCH_PREFS_NAME,
            BasePreferences.TIMING_PREFS_NAME,
            STARTUP_SURFACE_PREFS_NAME,
            "app_cache",
            BasePreferences.ENCRYPTED_PREFS_NAME,
            // API keys are exported in plain text only when the user opts in, via their own section.
            BasePreferences.SECURE_PREFS_NAME,
            "app_launch_counts",
            // The folder grant belongs to this device, so the schedule never travels in a backup.
            AutoBackupStore.PREFS_NAME,
        )

    private val excludedUserPreferenceKeys =
        setOf(
            BasePreferences.KEY_FIRST_LAUNCH,
            BasePreferences.KEY_INSTALL_TIME,
            // Permission prompts are per device, so a restore should still ask on the first reminder.
            BasePreferences.KEY_REMINDER_PERMISSIONS_REQUESTED,
            BasePreferences.KEY_NOTES_DATA,
            "custom_calendar_events_data",
            "custom_calendar_event_id_counter",
            "excluded_contact_ids",
            "excluded_file_uris",
            "excluded_file_extensions",
            "excluded_app_shortcuts",
            "excluded_settings",
            "search_engine_onboarding_seen",
            "has_seen_search_bar_welcome",
            "has_seen_contact_action_hint",
            "recent_app_launches",
            "last_seen_version",
            "direct_search_setup_expanded",
            "disabled_search_engines_expanded",
            "usage_permission_banner_dismiss_count",
            "usage_permission_banner_session_dismissed",
            "shortcut_hint_banner_dismiss_count",
            "shortcut_hint_banner_session_dismissed",
            "default_engine_hint_banner_dismissed",
            "last_overlay_keyboard_open_height_dp",
            "update_check_shown_this_session",
        ) + AiSearchLlmProviderRegistry.definitions.map { it.personalContextPreference }

    fun exportToUri(
        context: Context,
        outputUri: Uri,
        options: ExportOptions = ExportOptions(),
    ) {
        val includeApiKeys = options.includes(ExportItem.API_KEYS)
        val userPreferences = UserAppPreferences(context)
        val notes = if (options.includes(ExportItem.NOTES)) NotesRepository(context).getAllNotes() else null
        val outputStream =
            context.contentResolver.openOutputStream(outputUri)
                ?: throw IOException("Unable to open output stream")

        // Written field by field so the whole backup is never held as one JSON tree and string.
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            var firstField = true
            fun field(name: String, writeValue: () -> Unit) {
                writer.write(if (firstField) "{\n" else ",\n")
                firstField = false
                writer.write(JSONObject.quote(name))
                writer.write(": ")
                writeValue()
            }
            field(FIELD_FORMAT_VERSION) { writer.write(FORMAT_VERSION.toString()) }
            field(FIELD_EXPORTED_AT_EPOCH_MS) { writer.write(System.currentTimeMillis().toString()) }
            field(FIELD_PREFERENCES) { writePreferencesJson(writer, context, options, notes) }
            field(FIELD_SELECTED_EXPORT_ITEMS) {
                writer.write(JSONArray(options.selectedItems.map { it.name }.sorted()).toString())
            }
            field(FIELD_LLM_PERSONAL_CONTEXTS) { writer.write(serializePersonalContexts(userPreferences).toString()) }
            if (includeApiKeys) {
                field(FIELD_LLM_API_KEYS) { writer.write(serializeApiKeys(userPreferences).toString()) }
                field(FIELD_CUSTOM_LLM_PROVIDERS) {
                    writer.write(serializeCustomLlmProviders(CustomLlmProviderPreferences(context).getProviders()).toString())
                }
            }
            if (notes != null) {
                field(FIELD_NOTES) { writer.write(serializeNotes(notes).toString()) }
            }
            ExportItem.payloadSections.forEach { section ->
                if (options.includes(section)) {
                    section.payload?.let { payload ->
                        field(payload.field) { writer.write(payload.export(context).toString()) }
                    }
                }
            }
            if (options.includes(ExportItem.SETTINGS)) {
                userPreferences.getSelectedIconPackPackage()?.let { iconPack ->
                    field(FIELD_SELECTED_ICON_PACK) { writer.write(JSONObject.quote(iconPack)) }
                }
            }
            writer.write("\n}\n")
        }
    }

    fun importFromUri(
        context: Context,
        inputUri: Uri,
    ) {
        val inputStream =
            context.contentResolver.openInputStream(inputUri)
                ?: throw IOException("Unable to open input stream")
        val text = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (text.isBlank()) {
            throw IllegalArgumentException("Backup file is empty")
        }

        val root = JSONObject(text)
        val preferencesJson = root.optJSONObject(FIELD_PREFERENCES)
            ?: throw IllegalArgumentException("Invalid backup file format")
        val selectedExportItems = parseSelectedExportItems(root)

        val importsReminders = selectedExportItems == null || ExportItem.REMINDERS in selectedExportItems
        // Alarms are keyed by reminder id, so cancel the current ones before the data is replaced.
        if (importsReminders) {
            ReminderRepository(context).getAllReminders().forEach { reminder ->
                ReminderScheduler.cancel(context, reminder.reminderId)
                ReminderScheduler.cancelNotification(context, reminder.reminderId)
            }
        }

        val importedNames = preferencesJson.keys().asSequence().toSet()
        val preferenceNames =
            if (selectedExportItems != null) {
                importedNames.filterNot { it in excludedPreferenceFiles }.toSet()
            } else {
                collectEligiblePreferenceNames(context, importedNames)
            }
        // Custom Info alarms are keyed by item id too, so clear them before the items are replaced.
        val importsCustomInfo = CustomInfoRepository.PREFS_NAME in preferenceNames
        if (importsCustomInfo) {
            CustomInfoRepository(context).all().forEach { CustomInfoScheduler.cancel(context, it.id) }
        }

        var importedCustomEngines = false
        preferenceNames.forEach { prefName ->
            val sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val prefEntries = preferencesJson.optJSONObject(prefName)
            if (prefEntries == null && selectedExportItems != null) return@forEach

            val editor =
                sharedPreferences
                    .edit()
                    .apply {
                        if (selectedExportItems == null) {
                            clear()
                        }
                    }

            if (prefEntries != null) {
                prefEntries.keys().forEach { key ->
                    if (shouldExcludeKey(prefName, key)) return@forEach
                    if (!shouldRestorePreference(prefName, key, selectedExportItems)) return@forEach
                    val valueObject = prefEntries.optJSONObject(key) ?: return@forEach
                    if (prefName == BasePreferences.PREFS_NAME && key == BasePreferences.KEY_CUSTOM_SEARCH_ENGINES) {
                        importedCustomEngines = true
                    }
                    when (valueObject.optString(FIELD_TYPE)) {
                        TYPE_STRING -> editor.putString(key, valueObject.optString(FIELD_VALUE))
                        TYPE_BOOLEAN -> editor.putBoolean(key, valueObject.optBoolean(FIELD_VALUE))
                        TYPE_INT -> editor.putInt(key, valueObject.optInt(FIELD_VALUE))
                        TYPE_LONG -> editor.putLong(key, valueObject.optLong(FIELD_VALUE))
                        TYPE_FLOAT -> editor.putFloat(
                            key,
                            valueObject.optDouble(FIELD_VALUE).toFloat(),
                        )
                        TYPE_STRING_SET -> {
                            val jsonArray = valueObject.optJSONArray(FIELD_VALUE) ?: JSONArray()
                            val stringSet =
                                buildSet {
                                    for (index in 0 until jsonArray.length()) {
                                        add(jsonArray.optString(index))
                                    }
                                }
                            editor.putStringSet(key, stringSet)
                        }
                    }
                }
            }

            if (!editor.commit()) {
                throw IOException("Failed to import $prefName")
            }
        }

        SettingsBackupStorageBridge.afterPreferencesImport(
            context = context,
            fullRestore = selectedExportItems == null,
            importedCustomEngines = importedCustomEngines,
        )

        if (selectedExportItems == null || ExportItem.SETTINGS in selectedExportItems) {
            val importedLanguage = UserAppPreferences(context).getAppLanguageTag()
            BootstrapPreferences.setAppLanguageTag(context, importedLanguage)
            importSelectedIconPack(context, root)
        }

        importAiProviders(
            context = context,
            root = root,
            includeCustomProviders =
                selectedExportItems == null || ExportItem.API_KEYS in selectedExportItems,
        )

        if (selectedExportItems == null || ExportItem.NOTES in selectedExportItems) {
            val notesStore = NotesRoomStore(context)
            val notes =
                root.optJSONArray(FIELD_NOTES)?.let(::parseNotes)
                    // A full restore replaces everything, notes included; a partial one without
                    // notes in it leaves the current notes alone.
                    ?: if (selectedExportItems == null) emptyList() else null
            if (notes != null) {
                notesStore.replaceAll(notes)
                requestNoteWidgetsRefresh(context)
            }
        }

        // Payloads are restored only when present. Older backups may predate a section.
        ExportItem.payloadSections.forEach { section ->
            if (selectedExportItems == null || section in selectedExportItems) {
                section.payload?.let { payload ->
                    root.optJSONArray(payload.field)?.let { payload.restore(context, it) }
                }
            }
        }

        if (importsReminders) {
            ReminderScheduler.rescheduleAll(context)
        }
        if (importsCustomInfo) {
            CustomInfoScheduler.rescheduleAll(context)
            CustomInfoRepository.notifyChanged()
        }
    }

    /** Applies the backed-up icon pack only when it is installed here; otherwise the current one stays. */
    private fun importSelectedIconPack(
        context: Context,
        root: JSONObject,
    ) {
        val iconPackPackage = root.optString(FIELD_SELECTED_ICON_PACK).takeIf { it.isNotBlank() } ?: return
        val isInstalled =
            IconPackManager.findInstalledIconPacks(context).any { it.packageName == iconPackPackage }
        if (isInstalled) {
            UserAppPreferences(context).setSelectedIconPackPackage(iconPackPackage)
        }
    }

    private fun parseSelectedExportItems(root: JSONObject): Set<ExportItem>? {
        val itemsArray = root.optJSONArray(FIELD_SELECTED_EXPORT_ITEMS) ?: return null
        if (itemsArray.length() == 0) return emptySet()
        return buildSet {
            for (index in 0 until itemsArray.length()) {
                val itemName = itemsArray.optString(index)
                runCatching { ExportItem.valueOf(itemName) }.getOrNull()
                    ?.let { add(it) }
            }
        }
    }

    /** Writes the preferences section one entry at a time instead of building it in memory. */
    private fun writePreferencesJson(
        writer: Writer,
        context: Context,
        options: ExportOptions,
        notes: List<NoteInfo>?,
    ) {
        val sections =
            collectEligiblePreferenceNames(context)
                .sorted()
                .asSequence()
                .map { prefName -> prefName to preferenceEntriesForExport(context, prefName, options, notes) }
        SettingsBackupStorageBridge.writeSections(writer, sections)
    }

    private fun preferenceEntriesForExport(
        context: Context,
        prefName: String,
        options: ExportOptions,
        notes: List<NoteInfo>?,
    ): Sequence<Pair<String, JSONObject>> =
        sequence {
            val stored = context.getSharedPreferences(prefName, Context.MODE_PRIVATE).all
            val entries =
                if (prefName == BasePreferences.PREFS_NAME) {
                    SettingsBackupStorageBridge.exportableUserPreferences(context, stored)
                } else {
                    stored
                }
            // Notes live in Room. Keep the legacy JSON copy in the backup so builds that read
            // notes only from the preferences section restore them too.
            val legacyNotes =
                if (prefName == BasePreferences.PREFS_NAME && notes != null) {
                    serializePreferenceValue(NotesRoomStore.serializeLegacySnapshot(notes))
                } else {
                    null
                }
            entries.keys
                .filterNot { shouldExcludeKey(prefName, it) }
                .filter { shouldIncludeKeyForExport(prefName, it, options) }
                .sorted()
                .forEach { key ->
                    serializePreferenceValue(entries[key])?.let { yield(key to it) }
                }
            legacyNotes?.let { yield(BasePreferences.KEY_NOTES_DATA to it) }
        }

    private fun collectEligiblePreferenceNames(
        context: Context,
        additionalNames: Set<String> = emptySet(),
    ): Set<String> {
        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val discoveredNames =
            sharedPrefsDir.listFiles()
                ?.asSequence()
                ?.mapNotNull { file ->
                    val name = file.name
                    if (name.endsWith(".xml")) {
                        name.removeSuffix(".xml")
                    } else {
                        null
                    }
                }?.toSet()
                ?: emptySet()

        return (discoveredNames + additionalNames + BasePreferences.PREFS_NAME)
            .filterNot { it in excludedPreferenceFiles }
            .toSet()
    }

    private fun shouldExcludeKey(
        prefName: String,
        key: String,
    ): Boolean {
        if (prefName != BasePreferences.PREFS_NAME) return false
        return key in excludedUserPreferenceKeys ||
            key.startsWith("feature_flag_")
    }

    internal fun shouldIncludeKeyForExport(prefName: String, key: String, options: ExportOptions): Boolean =
        ExportItem.forPreference(prefName, key)?.let(options::includes) == true

    internal fun shouldRestorePreference(
        prefName: String,
        key: String,
        selectedItems: Set<ExportItem>?,
    ): Boolean {
        val section = ExportItem.forPreference(prefName, key)
        return if (selectedItems == null) {
            section?.restoreLegacyPreferences != false
        } else {
            section in selectedItems
        }
    }

    private fun serializePreferenceValue(value: Any?): JSONObject? =
        when (value) {
            is String ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_STRING)
                    .put(FIELD_VALUE, value)
            is Boolean ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_BOOLEAN)
                    .put(FIELD_VALUE, value)
            is Int ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_INT)
                    .put(FIELD_VALUE, value)
            is Long ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_LONG)
                    .put(FIELD_VALUE, value)
            is Float ->
                JSONObject()
                    .put(FIELD_TYPE, TYPE_FLOAT)
                    .put(FIELD_VALUE, value.toDouble())
            is Set<*> -> {
                val stringValues =
                    value
                        .mapNotNull { it as? String }
                        .let(::sanitizeStringSetForExport)
                JSONObject()
                    .put(FIELD_TYPE, TYPE_STRING_SET)
                    .put(FIELD_VALUE, JSONArray(stringValues))
            }
            else -> {
                null
            }
        }

    private fun sanitizeStringSetForExport(values: List<String>): List<String> {
        return values
    }

    private fun importAiProviders(
        context: Context,
        root: JSONObject,
        includeCustomProviders: Boolean,
    ) {
        val userPreferences = UserAppPreferences(context)

        root.optJSONObject(FIELD_LLM_API_KEYS)?.let { keys ->
            AiSearchLlmProviderId.entries.forEach { providerId ->
                keys.optString(providerId.storageValue, "").takeIf { it.isNotBlank() }?.let {
                    userPreferences.setLlmApiKey(providerId, it)
                }
            }
            keys.optString(TAVILY_API_KEY_FIELD, "").takeIf { it.isNotBlank() }?.let {
                userPreferences.setTavilyApiKey(it)
            }
        }
        root.optJSONObject(FIELD_LLM_PERSONAL_CONTEXTS)?.let { contexts ->
            AiSearchLlmProviderId.entries.forEach { providerId ->
                if (contexts.has(providerId.storageValue)) {
                    userPreferences.setLlmPersonalContext(providerId, contexts.optString(providerId.storageValue))
                }
            }
        }
        // Custom providers travel with API keys: skipped when the backup excluded keys.
        if (includeCustomProviders) {
            root.optJSONArray(FIELD_CUSTOM_LLM_PROVIDERS)?.let { array ->
                CustomLlmProviderPreferences(context).importProviders(parseCustomLlmProviders(array))
            }
        }

        userPreferences.refreshConfiguredAiProviderHint()
    }

    private fun serializeApiKeys(userPreferences: UserAppPreferences): JSONObject =
        JSONObject().apply {
            AiSearchLlmProviderId.entries.forEach { providerId ->
                userPreferences.getLlmApiKey(providerId)?.takeIf { it.isNotBlank() }?.let {
                    put(providerId.storageValue, it)
                }
            }
            userPreferences.getTavilyApiKey()?.takeIf { it.isNotBlank() }?.let {
                put(TAVILY_API_KEY_FIELD, it)
            }
        }

    private fun serializePersonalContexts(userPreferences: UserAppPreferences): JSONObject =
        JSONObject().apply {
            AiSearchLlmProviderId.entries.forEach { providerId ->
                userPreferences.getLlmPersonalContext(providerId)?.takeIf { it.isNotBlank() }?.let {
                    put(providerId.storageValue, it)
                }
            }
        }

    private fun serializeCustomLlmProviders(providers: List<CustomLlmProviderConfig>): JSONArray =
        JSONArray().apply {
            providers.forEach { provider ->
                put(
                    JSONObject()
                        .put("id", provider.id)
                        .put("baseUrl", provider.baseUrl)
                        .put("modelId", provider.modelId)
                        .put("groundingEnabled", provider.groundingEnabled)
                        .put("advancedPayload", provider.advancedPayload.orEmpty())
                        .put("advancedPayloadEnabled", provider.advancedPayloadEnabled)
                        .apply {
                            if (provider.apiKey.isNotBlank()) put("apiKey", provider.apiKey)
                        },
                )
            }
        }

    private fun parseCustomLlmProviders(array: JSONArray): List<CustomLlmProviderConfig> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val baseUrl = item.optString("baseUrl").takeIf { it.isNotBlank() } ?: continue
                val modelId = item.optString("modelId").orEmpty()
                add(
                    CustomLlmProviderConfig(
                        id = id,
                        baseUrl = baseUrl,
                        apiKey = item.optString("apiKey").orEmpty(),
                        modelId = modelId,
                        groundingEnabled = item.optBoolean("groundingEnabled", true),
                        advancedPayload = item.optString("advancedPayload").takeIf { it.isNotBlank() },
                        advancedPayloadEnabled = item.optBoolean("advancedPayloadEnabled", false),
                    ),
                )
            }
        }

    private fun serializeNotes(notes: List<NoteInfo>): JSONArray =
        JSONArray().apply {
            notes.forEach { note ->
                put(
                    JSONObject()
                        .put("noteId", note.noteId)
                        .put("title", note.title)
                        .put("markdown", note.markdownContent)
                        .put("createdAtMillis", note.createdAtMillis)
                        .put("updatedAtMillis", note.updatedAtMillis),
                )
            }
        }

    private fun parseNotes(array: JSONArray): List<NoteInfo> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optLong("noteId", -1L)
                if (id <= 0L) continue
                val createdAt = item.optLong("createdAtMillis", 0L)
                add(
                    NoteInfo(
                        noteId = id,
                        title = item.optString("title").orEmpty(),
                        markdownContent = item.optString("markdown").orEmpty(),
                        createdAtMillis = createdAt,
                        updatedAtMillis = item.optLong("updatedAtMillis", createdAt),
                    ),
                )
            }
        }
}
