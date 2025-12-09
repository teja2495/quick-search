package com.tk.quicksearch.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tk.quicksearch.model.FileType
import com.tk.quicksearch.search.MessagingApp
import com.tk.quicksearch.search.SearchEngine
import com.tk.quicksearch.search.getDefaultShortcutCode
import com.tk.quicksearch.search.isValidAmazonDomain
import com.tk.quicksearch.search.isValidShortcutCode
import com.tk.quicksearch.search.normalizeShortcutCodeInput
import java.util.Locale

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps.
 * Manages preferences for apps, contacts, files, search engines, shortcuts, and UI settings.
 */
class UserAppPreferences(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val firstLaunchPrefs: SharedPreferences =
        appContext.getSharedPreferences(FIRST_LAUNCH_PREFS_NAME, Context.MODE_PRIVATE)
    
    // Encrypted SharedPreferences for sensitive data like API keys.
    // If encryption cannot be initialized, Gemini keys will not be persisted.
    private val encryptedPrefs: SharedPreferences? = run {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences; Gemini API key will not be stored", e)
            null
        }
    }

    init {
        migrateHiddenPackages()
    }

    // ============================================================================
    // App Preferences
    // ============================================================================

    fun getSuggestionHiddenPackages(): Set<String> = getStringSet(KEY_HIDDEN_SUGGESTIONS)

    fun getResultHiddenPackages(): Set<String> = getStringSet(KEY_HIDDEN_RESULTS)

    fun getPinnedPackages(): Set<String> = getStringSet(KEY_PINNED)

    fun hidePackageInSuggestions(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN_SUGGESTIONS) {
        it.add(packageName)
    }

    fun hidePackageInResults(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN_RESULTS) {
        it.add(packageName)
    }

    fun unhidePackageInSuggestions(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN_SUGGESTIONS) {
        it.remove(packageName)
    }

    fun unhidePackageInResults(packageName: String): Set<String> = updateStringSet(KEY_HIDDEN_RESULTS) {
        it.remove(packageName)
    }

    fun pinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.add(packageName)
    }

    fun unpinPackage(packageName: String): Set<String> = updateStringSet(KEY_PINNED) {
        it.remove(packageName)
    }

    fun clearAllHiddenAppsInSuggestions(): Set<String> = clearStringSet(KEY_HIDDEN_SUGGESTIONS)

    fun clearAllHiddenAppsInResults(): Set<String> = clearStringSet(KEY_HIDDEN_RESULTS)

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

    fun isDirectDialEnabled(): Boolean = getBooleanPref(KEY_DIRECT_DIAL_ENABLED, false)

    fun setDirectDialEnabled(enabled: Boolean) {
        setBooleanPref(KEY_DIRECT_DIAL_ENABLED, enabled)
    }

    fun hasSeenDirectDialChoice(): Boolean = getBooleanPref(KEY_DIRECT_DIAL_CHOICE_SHOWN, false)

    fun setHasSeenDirectDialChoice(seen: Boolean) {
        setBooleanPref(KEY_DIRECT_DIAL_CHOICE_SHOWN, seen)
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
    // Settings Preferences
    // ============================================================================

    fun getPinnedSettingIds(): Set<String> = getStringSet(KEY_PINNED_SETTINGS)

    fun getExcludedSettingIds(): Set<String> = getStringSet(KEY_EXCLUDED_SETTINGS)

    fun pinSetting(id: String): Set<String> = updateStringSet(KEY_PINNED_SETTINGS) {
        it.add(id)
    }

    fun unpinSetting(id: String): Set<String> = updateStringSet(KEY_PINNED_SETTINGS) {
        it.remove(id)
    }

    fun excludeSetting(id: String): Set<String> = updateStringSet(KEY_EXCLUDED_SETTINGS) {
        it.add(id)
    }

    fun removeExcludedSetting(id: String): Set<String> = updateStringSet(KEY_EXCLUDED_SETTINGS) {
        it.remove(id)
    }

    fun clearAllExcludedSettings(): Set<String> = clearStringSet(KEY_EXCLUDED_SETTINGS)

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

    fun getSettingNickname(id: String): String? {
        return prefs.getString("$KEY_NICKNAME_SETTING_PREFIX$id", null)
    }

    fun setSettingNickname(id: String, nickname: String?) {
        val key = "$KEY_NICKNAME_SETTING_PREFIX$id"
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
            if (key.startsWith(KEY_NICKNAME_CONTACT_PREFIX) && value is String) {
                val nickname = value.lowercase(Locale.getDefault())
                if (nickname.contains(normalizedQuery)) {
                    val contactIdStr = key.removePrefix(KEY_NICKNAME_CONTACT_PREFIX)
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
            if (key.startsWith(KEY_NICKNAME_FILE_PREFIX) && value is String) {
                val nickname = value.lowercase(Locale.getDefault())
                if (nickname.contains(normalizedQuery)) {
                    val fileUri = key.removePrefix(KEY_NICKNAME_FILE_PREFIX)
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
            if (key.startsWith(KEY_NICKNAME_SETTING_PREFIX) && value is String) {
                val nickname = value.lowercase(Locale.getDefault())
                if (nickname.contains(normalizedQuery)) {
                    val id = key.removePrefix(KEY_NICKNAME_SETTING_PREFIX)
                    matchingSettingIds.add(id)
                }
            }
        }

        return matchingSettingIds
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
        val storedCode = prefs.getString(key, defaultCode) ?: defaultCode
        val normalizedCode = normalizeShortcutCodeInput(storedCode)
        return if (isValidShortcutCode(normalizedCode)) {
            normalizedCode
        } else {
            defaultCode
        }
    }

    fun setShortcutCode(engine: SearchEngine, code: String) {
        val key = "$KEY_SHORTCUT_CODE_PREFIX${engine.name}"
        val normalizedCode = normalizeShortcutCodeInput(code)
        if (!isValidShortcutCode(normalizedCode)) {
            return
        }
        prefs.edit().putString(key, normalizedCode).apply()
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
    // Gemini API Preferences
    // ============================================================================

    fun getGeminiApiKey(): String? {
        val securePrefs = encryptedPrefs ?: run {
            Log.e(TAG, "EncryptedSharedPreferences unavailable; Gemini API key not loaded")
            return null
        }

        // First try to get from encrypted storage
        val encryptedKey = securePrefs.getString(KEY_GEMINI_API_KEY, null)
        if (!encryptedKey.isNullOrBlank()) return encryptedKey

        // Migration: If not in encrypted storage, check plain text storage and migrate
        val plainTextKey = prefs.getString(KEY_GEMINI_API_KEY, null)
        if (!plainTextKey.isNullOrBlank()) {
            // Migrate to encrypted storage
            securePrefs.edit().putString(KEY_GEMINI_API_KEY, plainTextKey).apply()
            // Remove from plain text storage
            prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
            return plainTextKey
        }
        
        return null
    }

    fun setGeminiApiKey(key: String?) {
        val securePrefs = encryptedPrefs ?: run {
            Log.e(TAG, "EncryptedSharedPreferences unavailable; Gemini API key not persisted")
            return
        }

        if (key.isNullOrBlank()) {
            // Remove from both encrypted and plain text (for migration safety)
            securePrefs.edit().remove(KEY_GEMINI_API_KEY).apply()
            prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
            return
        }

        val normalizedKey = key.trim()
        // Save to encrypted storage
        securePrefs.edit().putString(KEY_GEMINI_API_KEY, normalizedKey).apply()
        // Also remove from plain text storage if it exists (cleanup)
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
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

    fun isFirstLaunch(): Boolean {
        syncInstallTimeWithBackup()
        return getFirstLaunchFlag()
    }

    fun setFirstLaunchCompleted() {
        setFirstLaunchFlag(false)
        recordCurrentInstallTime()
    }

    fun shouldShowWallpaperBackground(): Boolean = getBooleanPref(KEY_SHOW_WALLPAPER_BACKGROUND, true)

    fun setShowWallpaperBackground(showWallpaper: Boolean) {
        setBooleanPref(KEY_SHOW_WALLPAPER_BACKGROUND, showWallpaper)
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

    private fun migrateHiddenPackages() {
        val legacyHidden = getStringSet(KEY_HIDDEN_LEGACY)
        val currentSuggestions = prefs.getStringSet(KEY_HIDDEN_SUGGESTIONS, null)
        val currentResults = prefs.getStringSet(KEY_HIDDEN_RESULTS, null)

        if (legacyHidden.isEmpty()) {
            // Nothing to migrate; ensure legacy key is cleaned up if present
            if (prefs.contains(KEY_HIDDEN_LEGACY)) {
                prefs.edit().remove(KEY_HIDDEN_LEGACY).apply()
            }
            return
        }

        val editor = prefs.edit()
        if (currentSuggestions.isNullOrEmpty()) {
            editor.putStringSet(KEY_HIDDEN_SUGGESTIONS, legacyHidden)
        }
        if (currentResults.isNullOrEmpty()) {
            editor.putStringSet(KEY_HIDDEN_RESULTS, legacyHidden)
        }
        editor.remove(KEY_HIDDEN_LEGACY).apply()
    }

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

    private fun syncInstallTimeWithBackup() {
        val currentInstallTime = getCurrentInstallTime() ?: return
        val storedInstallTime = prefs.getLong(KEY_INSTALL_TIME, -1L)

        if (storedInstallTime == -1L) {
            prefs.edit().putLong(KEY_INSTALL_TIME, currentInstallTime).apply()
            return
        }

        if (storedInstallTime != currentInstallTime) {
            // Restored from backup on a fresh install: treat as first launch again
            prefs.edit()
                .putLong(KEY_INSTALL_TIME, currentInstallTime)
                .apply()
            setFirstLaunchFlag(true)
        }
    }

    private fun recordCurrentInstallTime() {
        val currentInstallTime = getCurrentInstallTime() ?: return
        prefs.edit().putLong(KEY_INSTALL_TIME, currentInstallTime).apply()
    }

    private fun getCurrentInstallTime(): Long? {
        return try {
            val packageManager = appContext.packageManager
            val packageName = appContext.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).firstInstallTime
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read install time", e)
            null
        }
    }

    private fun getFirstLaunchFlag(): Boolean {
        // Prefer the dedicated non-backed-up prefs. If missing, infer a safe value.
        if (!firstLaunchPrefs.contains(KEY_FIRST_LAUNCH)) {
            val currentInstallTime = getCurrentInstallTime()
            val isFreshInstall = currentInstallTime != null &&
                System.currentTimeMillis() - currentInstallTime < FRESH_INSTALL_THRESHOLD_MS

            // If this looks like a fresh install, default to true even if legacy prefs say otherwise.
            val legacyValue = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
            val initialValue = if (isFreshInstall) true else legacyValue
            setFirstLaunchFlag(initialValue)
            return initialValue
        }

        return firstLaunchPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    private fun setFirstLaunchFlag(value: Boolean) {
        firstLaunchPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
        // Keep legacy location in sync for backward compatibility
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    }

    private companion object {
        private const val TAG = "UserAppPreferences"
        
        // SharedPreferences name
        private const val PREFS_NAME = "user_app_preferences"
        private const val FIRST_LAUNCH_PREFS_NAME = "first_launch_state"
        private const val ENCRYPTED_PREFS_NAME = "encrypted_user_preferences"

        // App preferences keys
        private const val KEY_HIDDEN_LEGACY = "hidden_packages"
        private const val KEY_HIDDEN_SUGGESTIONS = "hidden_packages_suggestions"
        private const val KEY_HIDDEN_RESULTS = "hidden_packages_results"
        private const val KEY_PINNED = "pinned_packages"

        // Contact preferences keys
        private const val KEY_PINNED_CONTACT_IDS = "pinned_contact_ids"
        private const val KEY_EXCLUDED_CONTACT_IDS = "excluded_contact_ids"
        private const val KEY_PREFERRED_PHONE_PREFIX = "preferred_phone_"
        private const val KEY_DIRECT_DIAL_ENABLED = "direct_dial_enabled"
        private const val KEY_DIRECT_DIAL_CHOICE_SHOWN = "direct_dial_choice_shown"

        // File preferences keys
        private const val KEY_PINNED_FILE_URIS = "pinned_file_uris"
        private const val KEY_EXCLUDED_FILE_URIS = "excluded_file_uris"
        private const val KEY_ENABLED_FILE_TYPES = "enabled_file_types"

        // Settings preferences keys
        private const val KEY_PINNED_SETTINGS = "pinned_settings"
        private const val KEY_EXCLUDED_SETTINGS = "excluded_settings"

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
        private const val KEY_INSTALL_TIME = "install_time"
        private const val KEY_SHOW_WALLPAPER_BACKGROUND = "show_wallpaper_background"

        // Fresh install detection window (10 minutes)
        private const val FRESH_INSTALL_THRESHOLD_MS = 10 * 60 * 1000L

        // Section preferences keys
        private const val KEY_SECTION_ORDER = "section_order"
        private const val KEY_DISABLED_SECTIONS = "disabled_sections"

        // Amazon domain preferences keys
        private const val KEY_AMAZON_DOMAIN = "amazon_domain"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"

        // Usage permission banner preferences keys
        private const val KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT = "usage_permission_banner_dismiss_count"
        private const val KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED = "usage_permission_banner_session_dismissed"

        // Nickname preferences keys
        private const val KEY_NICKNAME_APP_PREFIX = "nickname_app_"
        private const val KEY_NICKNAME_CONTACT_PREFIX = "nickname_contact_"
        private const val KEY_NICKNAME_FILE_PREFIX = "nickname_file_"
        private const val KEY_NICKNAME_SETTING_PREFIX = "nickname_setting_"
    }
}
