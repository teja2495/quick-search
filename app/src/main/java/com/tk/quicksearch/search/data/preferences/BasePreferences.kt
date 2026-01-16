package com.tk.quicksearch.search.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tk.quicksearch.search.models.FileType

/** Utility class containing common SharedPreferences operations. */
object PreferenceUtils {

    // ============================================================================
    // Generic CRUD Operations for Set-based Preferences
    // ============================================================================

    fun getStringSet(prefs: SharedPreferences, key: String): Set<String> {
        return prefs.getStringSet(key, emptySet()).orEmpty()
    }

    fun getLongSet(prefs: SharedPreferences, key: String): Set<Long> {
        return prefs.getStringSet(key, emptySet())
                .orEmpty()
                .mapNotNull { it.toLongOrNull() }
                .toSet()
    }

    fun getBooleanPref(prefs: SharedPreferences, key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun setBooleanPref(prefs: SharedPreferences, key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getStringListPref(prefs: SharedPreferences, key: String): List<String> {
        val orderString = prefs.getString(key, null)
        return if (orderString.isNullOrBlank()) {
            emptyList()
        } else {
            orderString.split(",").filter { it.isNotBlank() }
        }
    }

    fun setStringListPref(prefs: SharedPreferences, key: String, order: List<String>) {
        prefs.edit().putString(key, order.joinToString(",")).apply()
    }

    fun updateStringSet(
            prefs: SharedPreferences,
            key: String,
            block: (MutableSet<String>) -> Unit
    ): Set<String> {
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        block(current)
        val snapshot = current.toSet()
        prefs.edit().putStringSet(key, snapshot).apply()
        return snapshot
    }

    fun updateLongSet(
            prefs: SharedPreferences,
            key: String,
            block: (MutableSet<String>) -> Unit
    ): Set<Long> {
        val current = prefs.getStringSet(key, emptySet()).orEmpty().toMutableSet()
        block(current)
        val snapshot = current.toSet()
        prefs.edit().putStringSet(key, snapshot).apply()
        return snapshot.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun clearStringSet(prefs: SharedPreferences, key: String): Set<String> {
        prefs.edit().putStringSet(key, emptySet()).apply()
        return emptySet()
    }

    fun clearLongSet(prefs: SharedPreferences, key: String): Set<Long> {
        prefs.edit().putStringSet(key, emptySet()).apply()
        return emptySet()
    }

    // ============================================================================
    // Generic CRUD Operations for Pinned/Excluded Items
    // ============================================================================

    fun getPinnedStringItems(prefs: SharedPreferences, key: String): Set<String> =
            getStringSet(prefs, key)

    fun pinStringItem(prefs: SharedPreferences, key: String, itemId: String): Set<String> =
            updateStringSet(prefs, key) { it.add(itemId) }

    fun unpinStringItem(prefs: SharedPreferences, key: String, itemId: String): Set<String> =
            updateStringSet(prefs, key) { it.remove(itemId) }

    fun getExcludedStringItems(prefs: SharedPreferences, key: String): Set<String> =
            getStringSet(prefs, key)

    fun excludeStringItem(prefs: SharedPreferences, key: String, itemId: String): Set<String> =
            updateStringSet(prefs, key) { it.add(itemId) }

    fun removeExcludedStringItem(
            prefs: SharedPreferences,
            key: String,
            itemId: String
    ): Set<String> = updateStringSet(prefs, key) { it.remove(itemId) }

    fun clearAllExcludedStringItems(prefs: SharedPreferences, key: String): Set<String> =
            clearStringSet(prefs, key)

    fun getPinnedLongItems(prefs: SharedPreferences, key: String): Set<Long> =
            getLongSet(prefs, key)

    fun pinLongItem(prefs: SharedPreferences, key: String, itemId: Long): Set<Long> =
            updateLongSet(prefs, key) { it.add(itemId.toString()) }

    fun unpinLongItem(prefs: SharedPreferences, key: String, itemId: Long): Set<Long> =
            updateLongSet(prefs, key) { it.remove(itemId.toString()) }

    fun getExcludedLongItems(prefs: SharedPreferences, key: String): Set<Long> =
            getLongSet(prefs, key)

    fun excludeLongItem(prefs: SharedPreferences, key: String, itemId: Long): Set<Long> =
            updateLongSet(prefs, key) { it.add(itemId.toString()) }

    fun removeExcludedLongItem(prefs: SharedPreferences, key: String, itemId: Long): Set<Long> =
            updateLongSet(prefs, key) { it.remove(itemId.toString()) }

    fun clearAllExcludedLongItems(prefs: SharedPreferences, key: String): Set<Long> =
            clearLongSet(prefs, key)

    // ============================================================================
    // File Type Migration Utility
    // ============================================================================

    fun migrateAndGetFileTypes(enabledNames: Set<String>): Set<FileType> {
        val migratedNames =
                enabledNames
                        .map { name ->
                            // Migrate old IMAGES or VIDEOS to PHOTOS_AND_VIDEOS
                            when (name) {
                                "IMAGES", "VIDEOS" -> "PHOTOS_AND_VIDEOS"
                                else -> name
                            }
                        }
                        .toSet()

        val result =
                migratedNames
                        .mapNotNull { name -> FileType.values().find { it.name == name } }
                        .toSet()

        return result
    }
}

/** Base class containing shared utilities and constants for all preference classes. */
abstract class BasePreferences(protected val context: Context) {

    protected val appContext = context.applicationContext

    protected val prefs: SharedPreferences =
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    protected val firstLaunchPrefs: SharedPreferences =
            appContext.getSharedPreferences(FIRST_LAUNCH_PREFS_NAME, Context.MODE_PRIVATE)

    // Encrypted SharedPreferences for sensitive data like API keys.
    // If encryption cannot be initialized, sensitive data will not be persisted.
    protected val encryptedPrefs: SharedPreferences? = run {
        try {
            val masterKey =
                    MasterKey.Builder(appContext)
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
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
            null
        }
    }

    // ============================================================================
    // Shared Utility Functions (delegated to PreferenceUtils)
    // ============================================================================

    protected fun getStringSet(key: String): Set<String> = PreferenceUtils.getStringSet(prefs, key)

    protected fun getLongSet(key: String): Set<Long> = PreferenceUtils.getLongSet(prefs, key)

    protected fun getBooleanPref(key: String, defaultValue: Boolean): Boolean =
            PreferenceUtils.getBooleanPref(prefs, key, defaultValue)

    protected fun setBooleanPref(key: String, value: Boolean) =
            PreferenceUtils.setBooleanPref(prefs, key, value)

    protected fun getStringListPref(key: String): List<String> =
            PreferenceUtils.getStringListPref(prefs, key)

    protected fun setStringListPref(key: String, order: List<String>) =
            PreferenceUtils.setStringListPref(prefs, key, order)

    protected fun updateStringSet(key: String, block: (MutableSet<String>) -> Unit): Set<String> =
            PreferenceUtils.updateStringSet(prefs, key, block)

    protected fun updateLongSet(key: String, block: (MutableSet<String>) -> Unit): Set<Long> =
            PreferenceUtils.updateLongSet(prefs, key, block)

    protected fun clearStringSet(key: String): Set<String> =
            PreferenceUtils.clearStringSet(prefs, key)

    protected fun clearLongSet(key: String): Set<Long> = PreferenceUtils.clearLongSet(prefs, key)

    // ============================================================================
    // Common Preference Operations for Management
    // ============================================================================

    // ============================================================================
    // Generic CRUD Operations for Common Patterns (delegated to PreferenceUtils)
    // ============================================================================

    /** Generic getter for pinned items using string keys. */
    protected fun getPinnedStringItems(key: String): Set<String> =
            PreferenceUtils.getPinnedStringItems(prefs, key)

    /** Generic pin operation for string-based items. */
    protected fun pinStringItem(key: String, itemId: String): Set<String> =
            PreferenceUtils.pinStringItem(prefs, key, itemId)

    /** Generic unpin operation for string-based items. */
    protected fun unpinStringItem(key: String, itemId: String): Set<String> =
            PreferenceUtils.unpinStringItem(prefs, key, itemId)

    /** Generic getter for excluded items using string keys. */
    protected fun getExcludedStringItems(key: String): Set<String> =
            PreferenceUtils.getExcludedStringItems(prefs, key)

    /** Generic exclude operation for string-based items. */
    protected fun excludeStringItem(key: String, itemId: String): Set<String> =
            PreferenceUtils.excludeStringItem(prefs, key, itemId)

    /** Generic remove excluded operation for string-based items. */
    protected fun removeExcludedStringItem(key: String, itemId: String): Set<String> =
            PreferenceUtils.removeExcludedStringItem(prefs, key, itemId)

    /** Generic clear all excluded items operation for string keys. */
    protected fun clearAllExcludedStringItems(key: String): Set<String> =
            PreferenceUtils.clearAllExcludedStringItems(prefs, key)

    /** Generic getter for pinned items using long keys. */
    protected fun getPinnedLongItems(key: String): Set<Long> =
            PreferenceUtils.getPinnedLongItems(prefs, key)

    /** Generic pin operation for long-based items. */
    protected fun pinLongItem(key: String, itemId: Long): Set<Long> =
            PreferenceUtils.pinLongItem(prefs, key, itemId)

    /** Generic unpin operation for long-based items. */
    protected fun unpinLongItem(key: String, itemId: Long): Set<Long> =
            PreferenceUtils.unpinLongItem(prefs, key, itemId)

    /** Generic getter for excluded items using long keys. */
    protected fun getExcludedLongItems(key: String): Set<Long> =
            PreferenceUtils.getExcludedLongItems(prefs, key)

    /** Generic exclude operation for long-based items. */
    protected fun excludeLongItem(key: String, itemId: Long): Set<Long> =
            PreferenceUtils.excludeLongItem(prefs, key, itemId)

    /** Generic remove excluded operation for long-based items. */
    protected fun removeExcludedLongItem(key: String, itemId: Long): Set<Long> =
            PreferenceUtils.removeExcludedLongItem(prefs, key, itemId)

    /** Generic clear all excluded items operation for long keys. */
    protected fun clearAllExcludedLongItems(key: String): Set<Long> =
            PreferenceUtils.clearAllExcludedLongItems(prefs, key)

    protected fun migrateAndGetFileTypes(enabledNames: Set<String>): Set<FileType> =
            PreferenceUtils.migrateAndGetFileTypes(enabledNames)

    protected fun getCurrentInstallTime(): Long? {
        return try {
            val packageManager = appContext.packageManager
            val packageName = appContext.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                        .firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).firstInstallTime
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read install time", e)
            null
        }
    }

    protected fun getFirstLaunchFlag(): Boolean {
        // Prefer the dedicated non-backed-up prefs. If missing, infer a safe value.
        if (!firstLaunchPrefs.contains(KEY_FIRST_LAUNCH)) {
            val currentInstallTime = getCurrentInstallTime()
            val isFreshInstall =
                    currentInstallTime != null &&
                            System.currentTimeMillis() - currentInstallTime <
                                    FRESH_INSTALL_THRESHOLD_MS

            // If this looks like a fresh install, default to true even if legacy prefs say
            // otherwise.
            val legacyValue = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
            val initialValue = if (isFreshInstall) true else legacyValue
            setFirstLaunchFlag(initialValue)
            return initialValue
        }

        return firstLaunchPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    protected fun setFirstLaunchFlag(value: Boolean) {
        firstLaunchPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
        // Keep legacy location in sync for backward compatibility
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()
    }

    protected fun syncInstallTimeWithBackup() {
        val currentInstallTime = getCurrentInstallTime() ?: return
        val storedInstallTime = prefs.getLong(KEY_INSTALL_TIME, -1L)

        if (storedInstallTime == -1L) {
            prefs.edit().putLong(KEY_INSTALL_TIME, currentInstallTime).apply()
            return
        }

        if (storedInstallTime != currentInstallTime) {
            // Restored from backup on a fresh install: treat as first launch again
            prefs.edit().putLong(KEY_INSTALL_TIME, currentInstallTime).apply()
            setFirstLaunchFlag(true)
        }
    }

    protected fun recordCurrentInstallTime() {
        val currentInstallTime = getCurrentInstallTime() ?: return
        prefs.edit().putLong(KEY_INSTALL_TIME, currentInstallTime).apply()
    }

    companion object {
        private const val TAG = "BasePreferences"

        // SharedPreferences names
        const val PREFS_NAME = "user_app_preferences"
        const val FIRST_LAUNCH_PREFS_NAME = "first_launch_state"
        const val ENCRYPTED_PREFS_NAME = "encrypted_user_preferences"

        // App preferences keys
        const val KEY_HIDDEN_LEGACY = "hidden_packages"
        const val KEY_HIDDEN_SUGGESTIONS = "hidden_packages_suggestions"
        const val KEY_HIDDEN_RESULTS = "hidden_packages_results"
        const val KEY_PINNED = "pinned_packages"

        // Contact preferences keys
        const val KEY_PINNED_CONTACT_IDS = "pinned_contact_ids"
        const val KEY_EXCLUDED_CONTACT_IDS = "excluded_contact_ids"
        const val KEY_PREFERRED_PHONE_PREFIX = "preferred_phone_"
        const val KEY_LAST_SHOWN_PHONE_PREFIX = "last_shown_phone_"
        const val KEY_DIRECT_DIAL_ENABLED = "direct_dial_enabled"
        const val KEY_DIRECT_DIAL_CHOICE_SHOWN = "direct_dial_choice_shown"
        const val KEY_DIRECT_DIAL_MANUALLY_DISABLED = "direct_dial_manually_disabled"

        // File preferences keys
        const val KEY_PINNED_FILE_URIS = "pinned_file_uris"
        const val KEY_EXCLUDED_FILE_URIS = "excluded_file_uris"
        const val KEY_EXCLUDED_FILE_EXTENSIONS = "excluded_file_extensions"
        const val KEY_ENABLED_FILE_TYPES = "enabled_file_types"

        // Settings preferences keys
        const val KEY_PINNED_SETTINGS = "pinned_settings"
        const val KEY_EXCLUDED_SETTINGS = "excluded_settings"

        // App shortcut preferences keys
        const val KEY_PINNED_APP_SHORTCUTS = "pinned_app_shortcuts"
        const val KEY_EXCLUDED_APP_SHORTCUTS = "excluded_app_shortcuts"

        // Search engine preferences keys
        const val KEY_DISABLED_SEARCH_ENGINES = "disabled_search_engines"
        const val KEY_SEARCH_ENGINE_ORDER = "search_engine_order"
        const val KEY_SEARCH_ENGINE_COMPACT_MODE = "search_engine_compact_mode"
        const val KEY_SEARCH_ENGINE_ONBOARDING_SEEN = "search_engine_onboarding_seen"

        // Shortcut preferences keys
        const val KEY_SHORTCUTS_ENABLED = "shortcuts_enabled"
        const val KEY_SHORTCUT_CODE_PREFIX = "shortcut_code_"
        const val KEY_SHORTCUT_ENABLED_PREFIX = "shortcut_enabled_"

        // UI preferences keys
        const val KEY_KEYBOARD_ALIGNED_LAYOUT = "keyboard_aligned_layout"
        const val KEY_USE_WHATSAPP_FOR_MESSAGES =
                "use_whatsapp_for_messages" // Deprecated, kept for migration
        const val KEY_MESSAGING_APP = "messaging_app"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_INSTALL_TIME = "install_time"
        const val KEY_SHOW_WALLPAPER_BACKGROUND = "show_wallpaper_background"
        const val KEY_CLEAR_QUERY_AFTER_SEARCH_ENGINE = "clear_query_after_search_engine"
        const val KEY_SHOW_ALL_RESULTS = "show_all_results"
        const val KEY_SELECTED_ICON_PACK = "selected_icon_pack"
        const val KEY_SORT_APPS_BY_USAGE = "sort_apps_by_usage"
        const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        const val KEY_DIRECT_SEARCH_SETUP_EXPANDED = "direct_search_setup_expanded"
        const val KEY_HAS_SEEN_SEARCH_BAR_WELCOME = "has_seen_search_bar_welcome"

        // Fresh install detection window (10 minutes)
        const val FRESH_INSTALL_THRESHOLD_MS = 10 * 60 * 1000L

        // Section preferences keys
        const val KEY_SECTION_ORDER = "section_order"
        const val KEY_DISABLED_SECTIONS = "disabled_sections"

        // Amazon domain preferences keys
        const val KEY_AMAZON_DOMAIN = "amazon_domain"
        const val KEY_GEMINI_API_KEY = "gemini_api_key"
        const val KEY_GEMINI_PERSONAL_CONTEXT = "gemini_personal_context"

        // Usage permission banner preferences keys
        const val KEY_USAGE_PERMISSION_BANNER_DISMISS_COUNT =
                "usage_permission_banner_dismiss_count"
        const val KEY_USAGE_PERMISSION_BANNER_SESSION_DISMISSED =
                "usage_permission_banner_session_dismissed"

        // Shortcut hint banner preferences keys
        const val KEY_SHORTCUT_HINT_BANNER_DISMISS_COUNT = "shortcut_hint_banner_dismiss_count"
        const val KEY_SHORTCUT_HINT_BANNER_SESSION_DISMISSED =
                "shortcut_hint_banner_session_dismissed"

        // Web search suggestions preferences keys
        const val KEY_WEB_SUGGESTIONS_ENABLED = "web_suggestions_enabled"
        const val KEY_WEB_SUGGESTIONS_COUNT = "web_suggestions_count"

        // Recent queries preferences keys
        const val KEY_RECENT_QUERIES = "recent_queries"
        const val KEY_RECENT_QUERIES_ENABLED = "recent_queries_enabled"
        const val KEY_RECENT_QUERIES_COUNT = "recent_queries_count"

        // Calculator preferences keys
        const val KEY_CALCULATOR_ENABLED = "calculator_enabled"

        // In-app review preferences keys
        const val KEY_FIRST_APP_OPEN_TIME = "first_app_open_time"
        const val KEY_LAST_REVIEW_PROMPT_TIME = "last_review_prompt_time"
        const val KEY_REVIEW_PROMPTED_COUNT = "review_prompted_count"
        const val KEY_APP_OPEN_COUNT = "app_open_count"
        const val KEY_APP_OPEN_COUNT_AT_LAST_PROMPT = "app_open_count_at_last_prompt"

        // In-app update session tracking keys
        const val KEY_UPDATE_CHECK_SHOWN_THIS_SESSION = "update_check_shown_this_session"

        // Nickname preferences keys
        const val KEY_NICKNAME_APP_PREFIX = "nickname_app_"
        const val KEY_NICKNAME_CONTACT_PREFIX = "nickname_contact_"
        const val KEY_NICKNAME_FILE_PREFIX = "nickname_file_"
        const val KEY_NICKNAME_SETTING_PREFIX = "nickname_setting_"

        // Contact card action preferences keys
        const val KEY_CONTACT_PRIMARY_ACTION_PREFIX = "contact_primary_action_"
        const val KEY_CONTACT_SECONDARY_ACTION_PREFIX = "contact_secondary_action_"
    }
}
