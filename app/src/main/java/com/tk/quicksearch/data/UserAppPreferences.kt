package com.tk.quicksearch.data

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.search.SearchEngine

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps.
 */
class UserAppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHiddenPackages(): Set<String> = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()

    fun getPinnedPackages(): Set<String> = prefs.getStringSet(KEY_PINNED, emptySet()).orEmpty().toSet()

    fun shouldShowAppLabels(): Boolean = prefs.getBoolean(KEY_SHOW_APP_LABELS, true)

    fun setShowAppLabels(showLabels: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_APP_LABELS, showLabels).apply()
    }

    fun hidePackage(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN) {
        it.add(packageName)
    }

    fun unhidePackage(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN) {
        it.remove(packageName)
    }

    fun pinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.add(packageName)
    }

    fun unpinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.remove(packageName)
    }

    fun getDisabledSearchEngines(): Set<String> = prefs.getStringSet(KEY_DISABLED_SEARCH_ENGINES, emptySet()).orEmpty().toSet()

    fun setDisabledSearchEngines(disabled: Set<String>) {
        prefs.edit().putStringSet(KEY_DISABLED_SEARCH_ENGINES, disabled).apply()
    }

    fun getSearchEngineOrder(): List<String> {
        val orderString = prefs.getString(KEY_SEARCH_ENGINE_ORDER, null)
        return if (orderString.isNullOrBlank()) {
            emptyList()
        } else {
            orderString.split(",").filter { it.isNotBlank() }
        }
    }

    fun setSearchEngineOrder(order: List<String>) {
        prefs.edit().putString(KEY_SEARCH_ENGINE_ORDER, order.joinToString(",")).apply()
    }

    fun getPreferredPhoneNumber(contactId: Long): String? {
        return prefs.getString("$KEY_PREFERRED_PHONE_PREFIX$contactId", null)
    }

    fun setPreferredPhoneNumber(contactId: Long, phoneNumber: String) {
        prefs.edit().putString("$KEY_PREFERRED_PHONE_PREFIX$contactId", phoneNumber).apply()
    }

    fun getEnabledFileTypes(): Set<FileType> {
        val enabledNames = prefs.getStringSet(KEY_ENABLED_FILE_TYPES, null)
        return if (enabledNames == null) {
            // Default: all file types enabled except OTHER
            FileType.values().filter { it != FileType.OTHER }.toSet()
        } else {
            val migratedNames = enabledNames.map { name ->
                // Migrate old IMAGES or VIDEOS to PHOTOS_AND_VIDEOS
                when (name) {
                    "IMAGES", "VIDEOS" -> "PHOTOS_AND_VIDEOS"
                    else -> name
                }
            }.toSet()
            val result = migratedNames.mapNotNull { name ->
                FileType.values().find { it.name == name }
            }.toSet()
            // If migration occurred, save the migrated preferences
            if (migratedNames != enabledNames) {
                setEnabledFileTypes(result)
            }
            result
        }
    }

    fun setEnabledFileTypes(enabled: Set<FileType>) {
        prefs.edit().putStringSet(KEY_ENABLED_FILE_TYPES, enabled.map { it.name }.toSet()).apply()
    }

    fun isKeyboardAlignedLayout(): Boolean = prefs.getBoolean(KEY_KEYBOARD_ALIGNED_LAYOUT, false)

    fun setKeyboardAlignedLayout(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_KEYBOARD_ALIGNED_LAYOUT, enabled).apply()
    }

    fun areShortcutsEnabled(): Boolean = prefs.getBoolean(KEY_SHORTCUTS_ENABLED, true)

    fun setShortcutsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHORTCUTS_ENABLED, enabled).apply()
    }

    fun getShortcutCode(engine: SearchEngine): String {
        val key = "$KEY_SHORTCUT_CODE_PREFIX${engine.name}"
        val defaultCode = getDefaultShortcutCode(engine)
        return prefs.getString(key, defaultCode) ?: defaultCode
    }

    fun setShortcutCode(engine: SearchEngine, code: String) {
        val key = "$KEY_SHORTCUT_CODE_PREFIX${engine.name}"
        prefs.edit().putString(key, code.lowercase()).apply()
    }

    fun isShortcutEnabled(engine: SearchEngine): Boolean {
        val key = "$KEY_SHORTCUT_ENABLED_PREFIX${engine.name}"
        return prefs.getBoolean(key, true)
    }

    fun setShortcutEnabled(engine: SearchEngine, enabled: Boolean) {
        val key = "$KEY_SHORTCUT_ENABLED_PREFIX${engine.name}"
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun useWhatsAppForMessages(): Boolean {
        return prefs.getBoolean(KEY_USE_WHATSAPP_FOR_MESSAGES, false)
    }

    fun setUseWhatsAppForMessages(useWhatsApp: Boolean) {
        prefs.edit().putBoolean(KEY_USE_WHATSAPP_FOR_MESSAGES, useWhatsApp).apply()
    }

    fun getAllShortcutCodes(): Map<SearchEngine, String> {
        return SearchEngine.values().associateWith { engine ->
            getShortcutCode(engine)
        }
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun shouldShowSectionTitles(): Boolean = prefs.getBoolean(KEY_SHOW_SECTION_TITLES, true)

    fun setShowSectionTitles(showTitles: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SECTION_TITLES, showTitles).apply()
    }

    private fun getDefaultShortcutCode(engine: SearchEngine): String {
        return when (engine) {
            SearchEngine.GOOGLE -> "ggl"
            SearchEngine.CHATGPT -> "cgpt"
            SearchEngine.PERPLEXITY -> "ppx"
            SearchEngine.GROK -> "grk"
            SearchEngine.GOOGLE_MAPS -> "mps"
            SearchEngine.GOOGLE_PLAY -> "gplay"
            SearchEngine.REDDIT -> "rdt"
            SearchEngine.YOUTUBE -> "ytb"
            SearchEngine.AMAZON -> "amz"
            SearchEngine.AI_MODE -> "gai"
        }
    }

    private inline fun updateStringSet(
        key: String,
        block: (MutableSet<String>) -> Unit
    ): Set<String> {
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        block(current)
        val snapshot = current.toSet()
        prefs.edit().putStringSet(key, snapshot).apply()
        return snapshot
    }

    private companion object {
        private const val PREFS_NAME = "user_app_preferences"
        private const val KEY_HIDDEN = "hidden_packages"
        private const val KEY_PINNED = "pinned_packages"
        private const val KEY_SHOW_APP_LABELS = "show_app_labels"
        private const val KEY_DISABLED_SEARCH_ENGINES = "disabled_search_engines"
        private const val KEY_SEARCH_ENGINE_ORDER = "search_engine_order"
        private const val KEY_PREFERRED_PHONE_PREFIX = "preferred_phone_"
        private const val KEY_ENABLED_FILE_TYPES = "enabled_file_types"
        private const val KEY_KEYBOARD_ALIGNED_LAYOUT = "keyboard_aligned_layout"
        private const val KEY_SHORTCUTS_ENABLED = "shortcuts_enabled"
        private const val KEY_SHORTCUT_CODE_PREFIX = "shortcut_code_"
        private const val KEY_SHORTCUT_ENABLED_PREFIX = "shortcut_enabled_"
        private const val KEY_USE_WHATSAPP_FOR_MESSAGES = "use_whatsapp_for_messages"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SHOW_SECTION_TITLES = "show_section_titles"
    }
}


