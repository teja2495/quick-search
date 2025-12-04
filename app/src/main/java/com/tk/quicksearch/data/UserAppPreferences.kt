package com.tk.quicksearch.data

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.search.MessagingApp
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.getDefaultShortcutCode
import com.tk.quicksearch.search.isValidAmazonDomain
import com.tk.quicksearch.ui.theme.ThemeMode

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps.
 * Manages preferences for apps, contacts, files, search engines, shortcuts, and UI settings.
 */
class UserAppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ============================================================================
    // App Preferences
    // ============================================================================

    fun getHiddenPackages(): Set<String> = getStringSet(KEY_HIDDEN)

    fun getPinnedPackages(): Set<String> = getStringSet(KEY_PINNED)

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

    fun clearAllHiddenApps(): Set<String> = clearStringSet(KEY_HIDDEN)

    fun shouldShowAppLabels(): Boolean = getBooleanPref(KEY_SHOW_APP_LABELS, true)

    fun setShowAppLabels(showLabels: Boolean) {
        setBooleanPref(KEY_SHOW_APP_LABELS, showLabels)
    }

    // ============================================================================
    // Contact Preferences
    // ============================================================================

    fun getPinnedContactIds(): Set<Long> = getLongSet(KEY_PINNED_CONTACT_IDS)

    fun getExcludedContactIds(): Set<Long> = getLongSet(KEY_EXCLUDED_CONTACT_IDS)

    fun pinContact(contactId: Long): Set<Long> = updateLongSet(KEY_PINNED_CONTACT_IDS) {
        it.add(contactId.toString())
    }

    fun unpinContact(contactId: Long): Set<Long> = updateLongSet(KEY_PINNED_CONTACT_IDS) {
        it.remove(contactId.toString())
    }

    fun excludeContact(contactId: Long): Set<Long> = updateLongSet(KEY_EXCLUDED_CONTACT_IDS) {
        it.add(contactId.toString())
    }

    fun removeExcludedContact(contactId: Long): Set<Long> = updateLongSet(KEY_EXCLUDED_CONTACT_IDS) {
        it.remove(contactId.toString())
    }

    fun clearAllExcludedContacts(): Set<Long> = clearLongSet(KEY_EXCLUDED_CONTACT_IDS)

    fun getPreferredPhoneNumber(contactId: Long): String? {
        return prefs.getString("$KEY_PREFERRED_PHONE_PREFIX$contactId", null)
    }

    fun setPreferredPhoneNumber(contactId: Long, phoneNumber: String) {
        prefs.edit().putString("$KEY_PREFERRED_PHONE_PREFIX$contactId", phoneNumber).apply()
    }

    // ============================================================================
    // File Preferences
    // ============================================================================

    fun getPinnedFileUris(): Set<String> = getStringSet(KEY_PINNED_FILE_URIS)

    fun getExcludedFileUris(): Set<String> = getStringSet(KEY_EXCLUDED_FILE_URIS)

    fun pinFile(uri: String): Set<String> = updateStringSet(KEY_PINNED_FILE_URIS) {
        it.add(uri)
    }

    fun unpinFile(uri: String): Set<String> = updateStringSet(KEY_PINNED_FILE_URIS) {
        it.remove(uri)
    }

    fun excludeFile(uri: String): Set<String> = updateStringSet(KEY_EXCLUDED_FILE_URIS) {
        it.add(uri)
    }

    fun removeExcludedFile(uri: String): Set<String> = updateStringSet(KEY_EXCLUDED_FILE_URIS) {
        it.remove(uri)
    }

    fun clearAllExcludedFiles(): Set<String> = clearStringSet(KEY_EXCLUDED_FILE_URIS)

    // ============================================================================
    // Nickname Preferences
    // ============================================================================

    fun getAppNickname(packageName: String): String? {
        return prefs.getString("$KEY_NICKNAME_APP_PREFIX$packageName", null)
    }

    fun setAppNickname(packageName: String, nickname: String?) {
        val key = "$KEY_NICKNAME_APP_PREFIX$packageName"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    fun getContactNickname(contactId: Long): String? {
        return prefs.getString("$KEY_NICKNAME_CONTACT_PREFIX$contactId", null)
    }

    fun setContactNickname(contactId: Long, nickname: String?) {
        val key = "$KEY_NICKNAME_CONTACT_PREFIX$contactId"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    fun getFileNickname(uri: String): String? {
        return prefs.getString("$KEY_NICKNAME_FILE_PREFIX$uri", null)
    }

    fun setFileNickname(uri: String, nickname: String?) {
        val key = "$KEY_NICKNAME_FILE_PREFIX$uri"
        if (nickname.isNullOrBlank()) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, nickname.trim()).apply()
        }
    }

    fun getEnabledFileTypes(): Set<FileType> {
        val enabledNames = prefs.getStringSet(KEY_ENABLED_FILE_TYPES, null)
        return if (enabledNames == null) {
            // Default: all file types enabled except OTHER
            FileType.values().filter { it != FileType.OTHER }.toSet()
        } else {
            migrateAndGetFileTypes(enabledNames)
        }
    }

    fun setEnabledFileTypes(enabled: Set<FileType>) {
        prefs.edit().putStringSet(KEY_ENABLED_FILE_TYPES, enabled.map { it.name }.toSet()).apply()
    }

    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun getDisabledSearchEngines(): Set<String> = getStringSet(KEY_DISABLED_SEARCH_ENGINES)

    fun setDisabledSearchEngines(disabled: Set<String>) {
        prefs.edit().putStringSet(KEY_DISABLED_SEARCH_ENGINES, disabled).apply()
    }

    fun getSearchEngineOrder(): List<String> = getStringListPref(KEY_SEARCH_ENGINE_ORDER)

    fun setSearchEngineOrder(order: List<String>) {
        setStringListPref(KEY_SEARCH_ENGINE_ORDER, order)
    }

    fun isSearchEngineSectionEnabled(): Boolean = getBooleanPref(KEY_SEARCH_ENGINE_SECTION_ENABLED, true)

    fun setSearchEngineSectionEnabled(enabled: Boolean) {
        setBooleanPref(KEY_SEARCH_ENGINE_SECTION_ENABLED, enabled)
    }

    // ============================================================================
    // Shortcut Preferences
    // ============================================================================

    fun areShortcutsEnabled(): Boolean = getBooleanPref(KEY_SHORTCUTS_ENABLED, true)

    fun setShortcutsEnabled(enabled: Boolean) {
        setBooleanPref(KEY_SHORTCUTS_ENABLED, enabled)
    }

    fun getShortcutCode(engine: SearchEngine): String {
        val key = "$KEY_SHORTCUT_CODE_PREFIX${engine.name}"
        val defaultCode = getDefaultShortcutCode(engine)
        // Code is already filtered on write, so no need to filter again
        return prefs.getString(key, defaultCode) ?: defaultCode
    }

    fun setShortcutCode(engine: SearchEngine, code: String) {
        val key = "$KEY_SHORTCUT_CODE_PREFIX${engine.name}"
        val filteredCode = code.lowercase().filter { char -> char.isLetterOrDigit() && char != ' ' }
        prefs.edit().putString(key, filteredCode).apply()
    }

    fun isShortcutEnabled(engine: SearchEngine): Boolean {
        val key = "$KEY_SHORTCUT_ENABLED_PREFIX${engine.name}"
        return getBooleanPref(key, true)
    }

    fun setShortcutEnabled(engine: SearchEngine, enabled: Boolean) {
        val key = "$KEY_SHORTCUT_ENABLED_PREFIX${engine.name}"
        setBooleanPref(key, enabled)
    }

    fun getAllShortcutCodes(): Map<SearchEngine, String> {
        return SearchEngine.values().associateWith { getShortcutCode(it) }
    }

    // ============================================================================
    // Amazon Domain Preferences
    // ============================================================================

    fun getAmazonDomain(): String? {
        return prefs.getString(KEY_AMAZON_DOMAIN, null)
    }

    fun setAmazonDomain(domain: String?) {
        if (domain.isNullOrBlank()) {
            prefs.edit().remove(KEY_AMAZON_DOMAIN).apply()
        } else {
            // Normalize domain (remove protocol, www, trailing slashes)
            val normalizedDomain = domain.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .removeSuffix("/")
            
            // Validate domain before saving
            if (isValidAmazonDomain(normalizedDomain)) {
                prefs.edit().putString(KEY_AMAZON_DOMAIN, normalizedDomain).apply()
            } else {
                // Invalid domain - don't save, just remove the existing one
                prefs.edit().remove(KEY_AMAZON_DOMAIN).apply()
            }
        }
    }

    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isKeyboardAlignedLayout(): Boolean = getBooleanPref(KEY_KEYBOARD_ALIGNED_LAYOUT, false)

    fun setKeyboardAlignedLayout(enabled: Boolean) {
        setBooleanPref(KEY_KEYBOARD_ALIGNED_LAYOUT, enabled)
    }

    fun getMessagingApp(): MessagingApp {
        // Migrate from old boolean preference if it exists
        val oldKeyExists = prefs.contains(KEY_USE_WHATSAPP_FOR_MESSAGES)
        if (oldKeyExists) {
            val useWhatsApp = getBooleanPref(KEY_USE_WHATSAPP_FOR_MESSAGES, false)
            val migratedApp = if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.MESSAGES
            // Save migrated value and remove old key
            setMessagingApp(migratedApp)
            prefs.edit().remove(KEY_USE_WHATSAPP_FOR_MESSAGES).apply()
            return migratedApp
        }
        
        // Read new enum preference
        val appName = prefs.getString(KEY_MESSAGING_APP, null)
        return if (appName != null) {
            try {
                MessagingApp.valueOf(appName)
            } catch (e: IllegalArgumentException) {
                MessagingApp.MESSAGES
            }
        } else {
            MessagingApp.MESSAGES
        }
    }

    fun setMessagingApp(app: MessagingApp) {
        prefs.edit().putString(KEY_MESSAGING_APP, app.name).apply()
    }
    
    @Deprecated("Use getMessagingApp() instead", ReplaceWith("getMessagingApp()"))
    fun useWhatsAppForMessages(): Boolean {
        return getMessagingApp() == MessagingApp.WHATSAPP
    }
    
    @Deprecated("Use setMessagingApp() instead", ReplaceWith("setMessagingApp(MessagingApp.WHATSAPP)"))
    fun setUseWhatsAppForMessages(useWhatsApp: Boolean) {
        setMessagingApp(if (useWhatsApp) MessagingApp.WHATSAPP else MessagingApp.MESSAGES)
    }

    fun isFirstLaunch(): Boolean = getBooleanPref(KEY_FIRST_LAUNCH, true)

    fun setFirstLaunchCompleted() {
        setBooleanPref(KEY_FIRST_LAUNCH, false)
    }

    fun shouldShowSectionTitles(): Boolean = getBooleanPref(KEY_SHOW_SECTION_TITLES, true)

    fun setShowSectionTitles(showTitles: Boolean) {
        setBooleanPref(KEY_SHOW_SECTION_TITLES, showTitles)
    }

    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.value) ?: ThemeMode.SYSTEM.value
    }

    fun setThemeMode(themeMode: String) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode).apply()
    }

    fun getUsagePermissionBannerDismissCount(): Int {
        return prefs.getInt(KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, 0)
    }

    fun incrementUsagePermissionBannerDismissCount() {
        val currentCount = getUsagePermissionBannerDismissCount()
        prefs.edit().putInt(KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT, currentCount + 1).apply()
    }

    fun isUsagePermissionBannerSessionDismissed(): Boolean {
        return prefs.getBoolean(KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, false)
    }

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, dismissed).apply()
    }

    fun resetUsagePermissionBannerSessionDismissed() {
        prefs.edit().putBoolean(KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED, false).apply()
    }

    fun shouldShowUsagePermissionBanner(): Boolean {
        // Show banner if: total dismiss count < 2 AND session not dismissed
        return getUsagePermissionBannerDismissCount() < 2 && !isUsagePermissionBannerSessionDismissed()
    }

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getSectionOrder(): List<String> = getStringListPref(KEY_SECTION_ORDER)

    fun setSectionOrder(order: List<String>) {
        setStringListPref(KEY_SECTION_ORDER, order)
    }

    fun getDisabledSections(): Set<String> = getStringSet(KEY_DISABLED_SECTIONS)

    fun setDisabledSections(disabled: Set<String>) {
        prefs.edit().putStringSet(KEY_DISABLED_SECTIONS, disabled).apply()
    }

    // ============================================================================
    // Private Helper Functions
    // ============================================================================

    private fun getStringSet(key: String): Set<String> {
        return prefs.getStringSet(key, emptySet()).orEmpty().toSet()
    }

    private fun getLongSet(key: String): Set<Long> {
        return prefs.getStringSet(key, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    private fun getBooleanPref(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    private fun setBooleanPref(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun getStringListPref(key: String): List<String> {
        val orderString = prefs.getString(key, null)
        return if (orderString.isNullOrBlank()) {
            emptyList()
        } else {
            orderString.split(",").filter { it.isNotBlank() }
        }
    }

    private fun setStringListPref(key: String, order: List<String>) {
        prefs.edit().putString(key, order.joinToString(",")).apply()
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

    private inline fun updateLongSet(
        key: String,
        block: (MutableSet<String>) -> Unit
    ): Set<Long> {
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        block(current)
        val snapshot = current.toSet()
        prefs.edit().putStringSet(key, snapshot).apply()
        return snapshot.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun clearStringSet(key: String): Set<String> {
        prefs.edit().putStringSet(key, emptySet()).apply()
        return emptySet()
    }

    private fun clearLongSet(key: String): Set<Long> {
        prefs.edit().putStringSet(key, emptySet()).apply()
        return emptySet()
    }

    private fun migrateAndGetFileTypes(enabledNames: Set<String>): Set<FileType> {
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

        return result
    }

    private fun getDefaultShortcutCode(engine: SearchEngine): String {
        return engine.getDefaultShortcutCode()
    }

    private companion object {
        // SharedPreferences name
        private const val PREFS_NAME = "user_app_preferences"

        // App preferences keys
        private const val KEY_HIDDEN = "hidden_packages"
        private const val KEY_PINNED = "pinned_packages"
        private const val KEY_SHOW_APP_LABELS = "show_app_labels"

        // Contact preferences keys
        private const val KEY_PINNED_CONTACT_IDS = "pinned_contact_ids"
        private const val KEY_EXCLUDED_CONTACT_IDS = "excluded_contact_ids"
        private const val KEY_PREFERRED_PHONE_PREFIX = "preferred_phone_"

        // File preferences keys
        private const val KEY_PINNED_FILE_URIS = "pinned_file_uris"
        private const val KEY_EXCLUDED_FILE_URIS = "excluded_file_uris"
        private const val KEY_ENABLED_FILE_TYPES = "enabled_file_types"

        // Search engine preferences keys
        private const val KEY_DISABLED_SEARCH_ENGINES = "disabled_search_engines"
        private const val KEY_SEARCH_ENGINE_ORDER = "search_engine_order"
        private const val KEY_SEARCH_ENGINE_SECTION_ENABLED = "search_engine_section_enabled"

        // Shortcut preferences keys
        private const val KEY_SHORTCUTS_ENABLED = "shortcuts_enabled"
        private const val KEY_SHORTCUT_CODE_PREFIX = "shortcut_code_"
        private const val KEY_SHORTCUT_ENABLED_PREFIX = "shortcut_enabled_"

        // UI preferences keys
        private const val KEY_KEYBOARD_ALIGNED_LAYOUT = "keyboard_aligned_layout"
        private const val KEY_USE_WHATSAPP_FOR_MESSAGES = "use_whatsapp_for_messages" // Deprecated, kept for migration
        private const val KEY_MESSAGING_APP = "messaging_app"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_SHOW_SECTION_TITLES = "show_section_titles"
        private const val KEY_THEME_MODE = "theme_mode"

        // Section preferences keys
        private const val KEY_SECTION_ORDER = "section_order"
        private const val KEY_DISABLED_SECTIONS = "disabled_sections"

        // Amazon domain preferences keys
        private const val KEY_AMAZON_DOMAIN = "amazon_domain"

        // Usage permission banner preferences keys
        private const val KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT = "usage_permission_banner_dismiss_count"
        private const val KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED = "usage_permission_banner_session_dismissed"

        // Nickname preferences keys
        private const val KEY_NICKNAME_APP_PREFIX = "nickname_app_"
        private const val KEY_NICKNAME_CONTACT_PREFIX = "nickname_contact_"
        private const val KEY_NICKNAME_FILE_PREFIX = "nickname_file_"
    }
}
