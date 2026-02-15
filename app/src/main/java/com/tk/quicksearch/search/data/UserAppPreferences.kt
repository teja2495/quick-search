package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.data.preferences.*
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.recentSearches.RecentSearchesPreferences

/**
 * Stores user-driven overrides for the app grid such as hidden or pinned apps. Manages preferences
 * for apps, contacts, files, search engines, shortcuts, and UI settings. This class now delegates
 * to specialized preference classes for better organization.
 */
class UserAppPreferences(
        private val context: Context,
) {
    private val sharedPrefs by lazy {
        context.getSharedPreferences(
                com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                android.content.Context.MODE_PRIVATE,
        )
    }

    // Feature-specific preference managers - lazy to avoid blocking construction
    private val appPreferences by lazy { AppPreferences(context) }
    private val contactPreferences by lazy { ContactPreferences(context) }
    private val filePreferences by lazy { FilePreferences(context) }
    private val settingsPreferences by lazy { SettingsPreferences(context) }
    private val appShortcutPreferences by lazy { AppShortcutPreferences(context) }
    private val nicknamePreferences by lazy { NicknamePreferences(context) }
    private val searchEnginePreferences by lazy { SearchEnginePreferences(context) }
    private val shortcutPreferences by lazy { ShortcutPreferences(context) }
    private val geminiPreferences by lazy { GeminiPreferences(context) }
    val uiPreferences by lazy { UiPreferences(context) }
    private val amazonPreferences by lazy { AmazonPreferences(context) }
    private val recentSearchesPreferences by lazy { RecentSearchesPreferences(context) }

    /** Minimal preferences needed for first frame render - only layout-affecting values. */
    data class CriticalPreferences(
            val oneHandedMode: Boolean,
    )

    /**
     * Optimized: Loads only critical preference using direct access. Uses the underlying
     * SharedPreferences directly for minimal overhead.
     */
    fun getCriticalPreferences(): CriticalPreferences {
        // Direct access to avoid lazy initialization overhead
        val prefs =
                context.getSharedPreferences(
                        com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE,
                )
        val oneHandedMode =
                prefs.getBoolean(
                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                .KEY_ONE_HANDED_MODE,
                        false,
                )
        return CriticalPreferences(oneHandedMode = oneHandedMode)
    }

    /**
     * Data class to hold all preferences needed during app startup for performance optimization.
     */
    data class StartupPreferences(
            val enabledFileTypes: Set<FileType>,
            val showFolders: Boolean,
            val showSystemFiles: Boolean,
            val showHiddenFiles: Boolean,
            val excludedFileExtensions: Set<String>,
            val oneHandedMode: Boolean,
            val overlayModeEnabled: Boolean,
            val directDialEnabled: Boolean,
            val hasSeenDirectDialChoice: Boolean,
            val hasSeenSearchEngineOnboarding: Boolean,
            val showWallpaperBackground: Boolean,
            val wallpaperBackgroundAlpha: Float,
            val wallpaperBlurRadius: Float,
            val overlayGradientTheme: OverlayGradientTheme,
            val overlayThemeIntensity: Float,
            val amazonDomain: String?,
            val pinnedPackages: Set<String>,
            val suggestionHiddenPackages: Set<String>,
            val resultHiddenPackages: Set<String>,
            val recentSearchesEnabled: Boolean,
            val appSuggestionsEnabled: Boolean,
    )

    /**
     * Consolidated startup configuration that includes all data needed for app initialization. This
     * enables loading everything in a single batch operation for maximum performance.
     */
    data class StartupConfig(
            // Critical preferences (needed immediately for layout)
            val oneHandedMode: Boolean,
            // Cached apps metadata
            val cachedAppsLastUpdate: Long,
            // Full startup preferences (loaded in background)
            val startupPreferences: StartupPreferences,
    )

    /**
     * Loads all preferences needed during startup in a single batch operation. This reduces the
     * number of SharedPreferences reads for better startup performance.
     */

    /**
     * Optimized: Loads all preferences needed during startup in a single batch operation. Uses
     * SharedPreferences.getAll() to minimize disk I/O operations.
     */
    fun getStartupPreferences(): StartupPreferences {
        // Batch read all preferences at once
        val prefs =
                context.getSharedPreferences(
                        com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE,
                )
        val allPrefs = prefs.all

        // Parse values from the batch read
        val enabledFileTypesNames =
                allPrefs[
                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                .KEY_ENABLED_FILE_TYPES,
                ] as?
                        Set<String>
        val enabledFileTypes =
                if (enabledFileTypesNames == null) {
                    // Default: all file types enabled except OTHER
                    com.tk.quicksearch.search.models.FileType.values()
                            .filter { it != com.tk.quicksearch.search.models.FileType.OTHER }
                            .toSet()
                } else {
                    com.tk.quicksearch.search.data.preferences.PreferenceUtils
                            .migrateAndGetFileTypes(enabledFileTypesNames)
                }

        return StartupPreferences(
                enabledFileTypes = enabledFileTypes,
                showFolders =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_SHOW_FOLDERS_IN_RESULTS,
                        ] as?
                                Boolean
                                ?: false,
                showSystemFiles =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_SHOW_SYSTEM_FILES,
                        ] as?
                                Boolean
                                ?: false,
                showHiddenFiles =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_SHOW_HIDDEN_FILES,
                        ] as?
                                Boolean
                                ?: false,
                excludedFileExtensions =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_EXCLUDED_FILE_EXTENSIONS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                oneHandedMode =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_ONE_HANDED_MODE,
                        ] as?
                                Boolean
                                ?: false,
                overlayModeEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_OVERLAY_MODE_ENABLED,
                        ] as?
                                Boolean
                                ?: false,
                directDialEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_DIRECT_DIAL_ENABLED,
                        ] as?
                                Boolean
                                ?: false,
                hasSeenDirectDialChoice =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_DIRECT_DIAL_CHOICE_SHOWN,
                        ] as?
                                Boolean
                                ?: false,
                hasSeenSearchEngineOnboarding =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_SEARCH_ENGINE_ONBOARDING_SEEN,
                        ] as?
                                Boolean
                                ?: false,
                showWallpaperBackground =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_SHOW_WALLPAPER_BACKGROUND,
                        ] as?
                                Boolean
                                ?: true,
                wallpaperBackgroundAlpha =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_WALLPAPER_BACKGROUND_ALPHA,
                        ] as?
                                Float
                                ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .DEFAULT_WALLPAPER_BACKGROUND_ALPHA,
                wallpaperBlurRadius =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_WALLPAPER_BLUR_RADIUS,
                        ] as?
                                Float
                                ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .DEFAULT_WALLPAPER_BLUR_RADIUS,
                overlayGradientTheme =
                        (
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_OVERLAY_GRADIENT_THEME,
                                ] as?
                                        String
                                )
                                ?.let { value ->
                                    runCatching { OverlayGradientTheme.valueOf(value) }.getOrNull()
                                }
                                ?: OverlayGradientTheme.MONOCHROME,
                overlayThemeIntensity =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_OVERLAY_THEME_INTENSITY,
                        ] as?
                                Float
                                ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .DEFAULT_OVERLAY_THEME_INTENSITY,
                amazonDomain =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_AMAZON_DOMAIN,
                        ] as?
                                String,
                pinnedPackages =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_PINNED,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                suggestionHiddenPackages =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_HIDDEN_SUGGESTIONS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                resultHiddenPackages =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.BasePreferences
                                        .KEY_HIDDEN_RESULTS,
                        ] as?
                                Set<String>
                                ?: emptySet(),
                recentSearchesEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_RECENT_QUERIES_ENABLED,
                        ] as?
                                Boolean
                                ?: true,
                appSuggestionsEnabled =
                        allPrefs[
                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                        .KEY_APP_SUGGESTIONS_ENABLED,
                        ] as?
                                Boolean
                                ?: true,
        )
    }

    /**
     * Loads all startup configuration in a single atomic operation for maximum performance. This
     * consolidates critical preferences, cached apps data, and startup preferences into one batch
     * read operation, minimizing disk I/O during app launch.
     */
    fun loadStartupConfig(): StartupConfig {
        // Get user preferences in one batch read
        val prefs =
                context.getSharedPreferences(
                        com.tk.quicksearch.search.data.preferences.BasePreferences.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE,
                )
        val allPrefs = prefs.all

        // Get app cache metadata in separate read (different SharedPreferences file)
        val appCachePrefs =
                context.getSharedPreferences(
                        "app_cache",
                        android.content.Context.MODE_PRIVATE,
                )
        val cachedAppsLastUpdate = appCachePrefs.getLong("last_update", 0L)

        // Extract critical preference directly from batch read
        val oneHandedMode =
                allPrefs[
                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                .KEY_ONE_HANDED_MODE,
                ] as?
                        Boolean
                        ?: false

        // Build startup preferences from the same batch read
        val enabledFileTypesNames =
                allPrefs[
                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                .KEY_ENABLED_FILE_TYPES,
                ] as?
                        Set<String>
        val enabledFileTypes =
                if (enabledFileTypesNames == null) {
                    // Default: all file types enabled except OTHER
                    com.tk.quicksearch.search.models.FileType.values()
                            .filter { it != com.tk.quicksearch.search.models.FileType.OTHER }
                            .toSet()
                } else {
                    com.tk.quicksearch.search.data.preferences.PreferenceUtils
                            .migrateAndGetFileTypes(enabledFileTypesNames)
                }

        val startupPreferences =
                StartupPreferences(
                        enabledFileTypes = enabledFileTypes,
                        showFolders =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SHOW_FOLDERS_IN_RESULTS,
                                ] as?
                                        Boolean
                                        ?: false,
                        showSystemFiles =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SHOW_SYSTEM_FILES,
                                ] as?
                                        Boolean
                                        ?: false,
                        showHiddenFiles =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SHOW_HIDDEN_FILES,
                                ] as?
                                        Boolean
                                        ?: false,
                        excludedFileExtensions =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_EXCLUDED_FILE_EXTENSIONS,
                                ] as?
                                        Set<String>
                                        ?: emptySet(),
                        oneHandedMode = oneHandedMode,
                        overlayModeEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_OVERLAY_MODE_ENABLED,
                                ] as?
                                        Boolean
                                        ?: false,
                        directDialEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_DIRECT_DIAL_ENABLED,
                                ] as?
                                        Boolean
                                        ?: true,
                        hasSeenDirectDialChoice =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_DIRECT_DIAL_CHOICE_SHOWN,
                                ] as?
                                        Boolean
                                        ?: false,
                        hasSeenSearchEngineOnboarding =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_SEARCH_ENGINE_ONBOARDING_SEEN,
                                ] as?
                                        Boolean
                                        ?: false,
                        showWallpaperBackground =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_SHOW_WALLPAPER_BACKGROUND,
                                ] as?
                                        Boolean
                                        ?: true,
                        wallpaperBackgroundAlpha =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_WALLPAPER_BACKGROUND_ALPHA,
                                ] as?
                                        Float
                                        ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .DEFAULT_WALLPAPER_BACKGROUND_ALPHA,
                        wallpaperBlurRadius =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_WALLPAPER_BLUR_RADIUS,
                                ] as?
                                        Float
                                        ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .DEFAULT_WALLPAPER_BLUR_RADIUS,
                        overlayGradientTheme =
                                (
                                        allPrefs[
                                                com.tk.quicksearch.search.data.preferences.UiPreferences
                                                        .KEY_OVERLAY_GRADIENT_THEME,
                                        ] as?
                                                String
                                        )
                                        ?.let { value ->
                                            runCatching { OverlayGradientTheme.valueOf(value) }
                                                    .getOrNull()
                                        }
                                        ?: OverlayGradientTheme.MONOCHROME,
                        overlayThemeIntensity =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_OVERLAY_THEME_INTENSITY,
                                ] as?
                                        Float
                                        ?: com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .DEFAULT_OVERLAY_THEME_INTENSITY,
                        amazonDomain =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_AMAZON_DOMAIN,
                                ] as?
                                        String,
                        pinnedPackages =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_PINNED,
                                ] as?
                                        Set<String>
                                        ?: emptySet(),
                        suggestionHiddenPackages =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_HIDDEN_SUGGESTIONS,
                                ] as?
                                        Set<String>
                                        ?: emptySet(),
                        resultHiddenPackages =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                                .KEY_HIDDEN_RESULTS,
                                ] as?
                                        Set<String>
                                        ?: emptySet(),
                        recentSearchesEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_RECENT_QUERIES_ENABLED,
                                ] as?
                                        Boolean
                                        ?: true,
                        appSuggestionsEnabled =
                                allPrefs[
                                        com.tk.quicksearch.search.data.preferences.UiPreferences
                                                .KEY_APP_SUGGESTIONS_ENABLED,
                                ] as?
                                        Boolean
                                        ?: true,
                )

        return StartupConfig(
                oneHandedMode = oneHandedMode,
                cachedAppsLastUpdate = cachedAppsLastUpdate,
                startupPreferences = startupPreferences,
        )
    }

    // ============================================================================
    // App Preferences
    // ============================================================================

    fun getSuggestionHiddenPackages(): Set<String> = appPreferences.getSuggestionHiddenPackages()

    fun getResultHiddenPackages(): Set<String> = appPreferences.getResultHiddenPackages()

    fun getPinnedPackages(): Set<String> = appPreferences.getPinnedPackages()

    fun hidePackageInSuggestions(packageName: String): Set<String> =
            appPreferences.hidePackageInSuggestions(packageName)

    fun hidePackageInResults(packageName: String): Set<String> =
            appPreferences.hidePackageInResults(packageName)

    fun unhidePackageInSuggestions(packageName: String): Set<String> =
            appPreferences.unhidePackageInSuggestions(packageName)

    fun unhidePackageInResults(packageName: String): Set<String> =
            appPreferences.unhidePackageInResults(packageName)

    fun pinPackage(packageName: String): Set<String> = appPreferences.pinPackage(packageName)

    fun unpinPackage(packageName: String): Set<String> = appPreferences.unpinPackage(packageName)

    fun clearAllHiddenAppsInSuggestions(): Set<String> =
            appPreferences.clearAllHiddenAppsInSuggestions()

    fun clearAllHiddenAppsInResults(): Set<String> = appPreferences.clearAllHiddenAppsInResults()

    fun getAppLaunchCount(packageName: String): Int = appPreferences.getAppLaunchCount(packageName)

    fun getAppLaunchCount(packageName: String, userHandleId: Int?): Int =
        appPreferences.getAppLaunchCount(packageName, userHandleId)

    fun incrementAppLaunchCount(packageName: String) =
        appPreferences.incrementAppLaunchCount(packageName)

    fun incrementAppLaunchCount(packageName: String, userHandleId: Int?) =
        appPreferences.incrementAppLaunchCount(packageName, userHandleId)

    fun getAllAppLaunchCounts(): Map<String, Int> = appPreferences.getAllAppLaunchCounts()

    fun getRecentAppLaunches(): List<String> = appPreferences.getRecentAppLaunches()

    fun setRecentAppLaunches(packageNames: List<String>): List<String> =
            appPreferences.setRecentAppLaunches(packageNames)

    fun addRecentAppLaunch(packageName: String): List<String> =
            appPreferences.addRecentAppLaunch(packageName)

    // ============================================================================
    // Contact Preferences
    // ============================================================================

    fun getPinnedContactIds(): Set<Long> = contactPreferences.getPinnedContactIds()

    fun getExcludedContactIds(): Set<Long> = contactPreferences.getExcludedContactIds()

    fun pinContact(contactId: Long): Set<Long> = contactPreferences.pinContact(contactId)

    fun unpinContact(contactId: Long): Set<Long> = contactPreferences.unpinContact(contactId)

    fun excludeContact(contactId: Long): Set<Long> = contactPreferences.excludeContact(contactId)

    fun removeExcludedContact(contactId: Long): Set<Long> =
            contactPreferences.removeExcludedContact(contactId)

    fun clearAllExcludedContacts(): Set<Long> = contactPreferences.clearAllExcludedContacts()

    fun getPreferredPhoneNumber(contactId: Long): String? =
            contactPreferences.getPreferredPhoneNumber(contactId)

    fun setPreferredPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) = contactPreferences.setPreferredPhoneNumber(contactId, phoneNumber)

    fun getLastShownPhoneNumber(contactId: Long): String? =
            contactPreferences.getLastShownPhoneNumber(contactId)

    fun setLastShownPhoneNumber(
            contactId: Long,
            phoneNumber: String,
    ) = contactPreferences.setLastShownPhoneNumber(contactId, phoneNumber)

    fun isDirectDialEnabled(): Boolean = contactPreferences.isDirectDialEnabled()

    fun setDirectDialEnabled(enabled: Boolean) = contactPreferences.setDirectDialEnabled(enabled)

    fun hasSeenDirectDialChoice(): Boolean = contactPreferences.hasSeenDirectDialChoice()

    fun setHasSeenDirectDialChoice(seen: Boolean) =
            contactPreferences.setHasSeenDirectDialChoice(seen)

    fun isDirectDialManuallyDisabled(): Boolean =
            sharedPrefs.getBoolean(
                    com.tk.quicksearch.search.data.preferences.BasePreferences
                            .KEY_DIRECT_DIAL_MANUALLY_DISABLED,
                    false,
            )

    fun setDirectDialManuallyDisabled(disabled: Boolean) {
        sharedPrefs
                .edit()
                .putBoolean(
                        com.tk.quicksearch.search.data.preferences.BasePreferences
                                .KEY_DIRECT_DIAL_MANUALLY_DISABLED,
                        disabled,
                ).apply()
    }

    // ============================================================================
    // File Preferences
    // ============================================================================

    fun getPinnedFileUris(): Set<String> = filePreferences.getPinnedFileUris()

    fun getExcludedFileUris(): Set<String> = filePreferences.getExcludedFileUris()

    fun pinFile(uri: String): Set<String> = filePreferences.pinFile(uri)

    fun unpinFile(uri: String): Set<String> = filePreferences.unpinFile(uri)

    fun excludeFile(uri: String): Set<String> = filePreferences.excludeFile(uri)

    fun removeExcludedFile(uri: String): Set<String> = filePreferences.removeExcludedFile(uri)

    fun clearAllExcludedFiles(): Set<String> = filePreferences.clearAllExcludedFiles()

    fun getExcludedFileExtensions(): Set<String> = filePreferences.getExcludedFileExtensions()

    fun addExcludedFileExtension(extension: String): Set<String> =
            filePreferences.addExcludedFileExtension(extension)

    fun removeExcludedFileExtension(extension: String): Set<String> =
            filePreferences.removeExcludedFileExtension(extension)

    fun clearAllExcludedFileExtensions(): Set<String> =
            filePreferences.clearAllExcludedFileExtensions()

    fun getEnabledFileTypes(): Set<com.tk.quicksearch.search.models.FileType> =
            filePreferences.getEnabledFileTypes()

    fun setEnabledFileTypes(enabled: Set<com.tk.quicksearch.search.models.FileType>) =
            filePreferences.setEnabledFileTypes(enabled)

    fun clearEnabledFileTypes(): Set<com.tk.quicksearch.search.models.FileType> =
            filePreferences.clearEnabledFileTypes()

    fun getShowFoldersInResults(): Boolean = filePreferences.getShowFoldersInResults()

    fun setShowFoldersInResults(show: Boolean) = filePreferences.setShowFoldersInResults(show)

    fun getShowSystemFiles(): Boolean = filePreferences.getShowSystemFiles()

    fun setShowSystemFiles(show: Boolean) = filePreferences.setShowSystemFiles(show)

    fun getShowHiddenFiles(): Boolean = filePreferences.getShowHiddenFiles()

    fun setShowHiddenFiles(show: Boolean) = filePreferences.setShowHiddenFiles(show)

    // ============================================================================
    // Settings Preferences
    // ============================================================================

    fun getPinnedSettingIds(): Set<String> = settingsPreferences.getPinnedSettingIds()

    fun getExcludedSettingIds(): Set<String> = settingsPreferences.getExcludedSettingIds()

    fun pinSetting(id: String): Set<String> = settingsPreferences.pinSetting(id)

    fun unpinSetting(id: String): Set<String> = settingsPreferences.unpinSetting(id)

    fun excludeSetting(id: String): Set<String> = settingsPreferences.excludeSetting(id)

    fun removeExcludedSetting(id: String): Set<String> =
            settingsPreferences.removeExcludedSetting(id)

    fun clearAllExcludedSettings(): Set<String> = settingsPreferences.clearAllExcludedSettings()

    // ============================================================================
    // App Shortcut Preferences
    // ============================================================================

    fun getPinnedAppShortcutIds(): Set<String> = appShortcutPreferences.getPinnedAppShortcutIds()

    fun getExcludedAppShortcutIds(): Set<String> =
            appShortcutPreferences.getExcludedAppShortcutIds()

    fun getDisabledAppShortcutIds(): Set<String> =
            appShortcutPreferences.getDisabledAppShortcutIds()

    fun pinAppShortcut(id: String): Set<String> = appShortcutPreferences.pinAppShortcut(id)

    fun unpinAppShortcut(id: String): Set<String> = appShortcutPreferences.unpinAppShortcut(id)

    fun excludeAppShortcut(id: String): Set<String> = appShortcutPreferences.excludeAppShortcut(id)

    fun removeExcludedAppShortcut(id: String): Set<String> =
            appShortcutPreferences.removeExcludedAppShortcut(id)

    fun clearAllExcludedAppShortcuts(): Set<String> =
            appShortcutPreferences.clearAllExcludedAppShortcuts()

    fun setAppShortcutEnabled(
            id: String,
            enabled: Boolean,
    ): Set<String> = appShortcutPreferences.setAppShortcutEnabled(id, enabled)

    // ============================================================================
    // Nickname Preferences
    // ============================================================================

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

    // ============================================================================
    // Search Engine Preferences
    // ============================================================================

    fun hasDisabledSearchEnginesPreference(): Boolean =
            searchEnginePreferences.hasDisabledSearchEnginesPreference()

    fun getDisabledSearchEngines(): Set<String> = searchEnginePreferences.getDisabledSearchEngines()

    fun setDisabledSearchEngines(disabled: Set<String>) =
            searchEnginePreferences.setDisabledSearchEngines(disabled)

    fun getSearchEngineOrder(): List<String> = searchEnginePreferences.getSearchEngineOrder()

    fun setSearchEngineOrder(order: List<String>) =
            searchEnginePreferences.setSearchEngineOrder(order)

    fun isSearchEngineCompactMode(): Boolean = searchEnginePreferences.isSearchEngineCompactMode()

    fun setSearchEngineCompactMode(enabled: Boolean) =
            searchEnginePreferences.setSearchEngineCompactMode(enabled)

    fun hasSeenSearchEngineOnboarding(): Boolean =
            searchEnginePreferences.hasSeenSearchEngineOnboarding()

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) =
            searchEnginePreferences.setHasSeenSearchEngineOnboarding(seen)

    fun getCustomSearchEngines(): List<CustomSearchEngine> =
            searchEnginePreferences.getCustomSearchEngines()

    fun setCustomSearchEngines(engines: List<CustomSearchEngine>) =
            searchEnginePreferences.setCustomSearchEngines(engines)

    // ============================================================================
    // Shortcut Preferences
    // ============================================================================

    fun areShortcutsEnabled(): Boolean = shortcutPreferences.areShortcutsEnabled()

    fun setShortcutsEnabled(enabled: Boolean) = shortcutPreferences.setShortcutsEnabled(enabled)

    fun getShortcutCode(engine: SearchEngine): String = shortcutPreferences.getShortcutCode(engine)

    fun setShortcutCode(
            engine: SearchEngine,
            code: String,
    ) = shortcutPreferences.setShortcutCode(engine, code)

    fun getShortcutCode(targetId: String): String? = shortcutPreferences.getShortcutCode(targetId)

    fun setShortcutCode(
            targetId: String,
            code: String,
    ) = shortcutPreferences.setShortcutCode(targetId, code)

    fun isShortcutEnabled(engine: SearchEngine): Boolean =
            shortcutPreferences.isShortcutEnabled(engine)

    fun setShortcutEnabled(
            engine: SearchEngine,
            enabled: Boolean,
    ) = shortcutPreferences.setShortcutEnabled(engine, enabled)

    fun getAllShortcutCodes(): Map<SearchEngine, String> = shortcutPreferences.getAllShortcutCodes()

    // ============================================================================
    // Amazon Domain Preferences
    // ============================================================================

    fun getAmazonDomain(): String? = amazonPreferences.getAmazonDomain()

    fun setAmazonDomain(domain: String?) = amazonPreferences.setAmazonDomain(domain)

    // ============================================================================
    // Gemini API Preferences
    // ============================================================================

    fun getGeminiApiKey(): String? = geminiPreferences.getGeminiApiKey()

    fun setGeminiApiKey(key: String?) = geminiPreferences.setGeminiApiKey(key)

    fun getPersonalContext(): String? = geminiPreferences.getPersonalContext()

    fun setPersonalContext(context: String?) = geminiPreferences.setPersonalContext(context)

    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isOneHandedMode(): Boolean = uiPreferences.isOneHandedMode()

    fun setOneHandedMode(enabled: Boolean) = uiPreferences.setOneHandedMode(enabled)

    fun isOverlayModeEnabled(): Boolean = uiPreferences.isOverlayModeEnabled()

    fun setOverlayModeEnabled(enabled: Boolean) = uiPreferences.setOverlayModeEnabled(enabled)

    fun getMessagingApp(): MessagingApp = uiPreferences.getMessagingApp()

    fun setMessagingApp(app: MessagingApp) = uiPreferences.setMessagingApp(app)

    fun isFirstLaunch(): Boolean = uiPreferences.isFirstLaunch()

    fun setFirstLaunchCompleted() = uiPreferences.setFirstLaunchCompleted()

    fun shouldShowWallpaperBackground(): Boolean = uiPreferences.shouldShowWallpaperBackground()

    fun setShowWallpaperBackground(showWallpaper: Boolean) =
            uiPreferences.setShowWallpaperBackground(showWallpaper)

    fun getWallpaperBackgroundAlpha(): Float = uiPreferences.getWallpaperBackgroundAlpha()

    fun setWallpaperBackgroundAlpha(alpha: Float) = uiPreferences.setWallpaperBackgroundAlpha(alpha)

    fun getWallpaperBlurRadius(): Float = uiPreferences.getWallpaperBlurRadius()

    fun setWallpaperBlurRadius(radius: Float) = uiPreferences.setWallpaperBlurRadius(radius)

    fun getOverlayGradientTheme(): OverlayGradientTheme = uiPreferences.getOverlayGradientTheme()

    fun setOverlayGradientTheme(theme: OverlayGradientTheme) =
            uiPreferences.setOverlayGradientTheme(theme)

    fun getOverlayThemeIntensity(): Float = uiPreferences.getOverlayThemeIntensity()

    fun setOverlayThemeIntensity(intensity: Float) = uiPreferences.setOverlayThemeIntensity(intensity)

    fun getSelectedIconPackPackage(): String? = uiPreferences.getSelectedIconPackPackage()

    fun setSelectedIconPackPackage(packageName: String?) =
            uiPreferences.setSelectedIconPackPackage(packageName)

    fun isDirectSearchSetupExpanded(): Boolean = uiPreferences.isDirectSearchSetupExpanded()

    fun setDirectSearchSetupExpanded(expanded: Boolean) =
            uiPreferences.setDirectSearchSetupExpanded(expanded)

    fun isDisabledSearchEnginesExpanded(): Boolean = uiPreferences.isDisabledSearchEnginesExpanded()

    fun setDisabledSearchEnginesExpanded(expanded: Boolean) =
            uiPreferences.setDisabledSearchEnginesExpanded(expanded)

    fun hasSeenSearchBarWelcome(): Boolean = uiPreferences.hasSeenSearchBarWelcome()

    fun setHasSeenSearchBarWelcome(seen: Boolean) = uiPreferences.setHasSeenSearchBarWelcome(seen)

    fun shouldForceSearchBarWelcomeOnNextOpen(): Boolean =
            uiPreferences.shouldForceSearchBarWelcomeOnNextOpen()

    fun setForceSearchBarWelcomeOnNextOpen(force: Boolean) =
            uiPreferences.setForceSearchBarWelcomeOnNextOpen(force)

    fun consumeForceSearchBarWelcomeOnNextOpen(): Boolean {
        val shouldForce = uiPreferences.shouldForceSearchBarWelcomeOnNextOpen()
        if (shouldForce) {
            uiPreferences.setForceSearchBarWelcomeOnNextOpen(false)
        }
        return shouldForce
    }

    fun hasSeenContactActionHint(): Boolean = uiPreferences.hasSeenContactActionHint()

    fun setHasSeenContactActionHint(seen: Boolean) = uiPreferences.setHasSeenContactActionHint(seen)

    fun hasSeenPersonalContextHint(): Boolean = uiPreferences.hasSeenPersonalContextHint()

    fun setHasSeenPersonalContextHint(seen: Boolean) =
            uiPreferences.setHasSeenPersonalContextHint(seen)

    fun hasSeenOverlayCloseTip(): Boolean = uiPreferences.hasSeenOverlayCloseTip()

    fun setHasSeenOverlayCloseTip(seen: Boolean) = uiPreferences.setHasSeenOverlayCloseTip(seen)

    fun hasSeenOverlayAssistantTip(): Boolean = uiPreferences.hasSeenOverlayAssistantTip()

    fun setHasSeenOverlayAssistantTip(seen: Boolean) =
            uiPreferences.setHasSeenOverlayAssistantTip(seen)

    fun getLastOverlayKeyboardOpenHeightDp(): Float? =
            uiPreferences.getLastOverlayKeyboardOpenHeightDp()

    fun setLastOverlayKeyboardOpenHeightDp(heightDp: Float) =
            uiPreferences.setLastOverlayKeyboardOpenHeightDp(heightDp)

    fun getLastSeenVersionName(): String? = uiPreferences.getLastSeenVersionName()

    fun setLastSeenVersionName(versionName: String?) =
            uiPreferences.setLastSeenVersionName(versionName)

    fun getUsagePermissionBannerDismissCount(): Int =
            uiPreferences.getUsagePermissionBannerDismissCount()

    fun incrementUsagePermissionBannerDismissCount() =
            uiPreferences.incrementUsagePermissionBannerDismissCount()

    fun isUsagePermissionBannerSessionDismissed(): Boolean =
            uiPreferences.isUsagePermissionBannerSessionDismissed()

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) =
            uiPreferences.setUsagePermissionBannerSessionDismissed(dismissed)

    fun resetUsagePermissionBannerSessionDismissed() =
            uiPreferences.resetUsagePermissionBannerSessionDismissed()

    fun shouldShowUsagePermissionBanner(): Boolean = uiPreferences.shouldShowUsagePermissionBanner()

    // ============================================================================
    // Web Search Suggestions Preferences
    // ============================================================================

    fun areAppSuggestionsEnabled(): Boolean = uiPreferences.areAppSuggestionsEnabled()

    fun setAppSuggestionsEnabled(enabled: Boolean) = uiPreferences.setAppSuggestionsEnabled(enabled)

    fun areWebSuggestionsEnabled(): Boolean = uiPreferences.areWebSuggestionsEnabled()

    fun getWebSuggestionsCount(): Int = uiPreferences.getWebSuggestionsCount()

    fun setWebSuggestionsCount(count: Int) {
        uiPreferences.setWebSuggestionsCount(count)
    }

    fun setWebSuggestionsEnabled(enabled: Boolean) = uiPreferences.setWebSuggestionsEnabled(enabled)

    // ============================================================================
    // Calculator Preferences
    // ============================================================================

    fun isCalculatorEnabled(): Boolean = uiPreferences.isCalculatorEnabled()

    fun setCalculatorEnabled(enabled: Boolean) = uiPreferences.setCalculatorEnabled(enabled)

    // ============================================================================
    // Recent Queries Preferences
    // ============================================================================

    fun getRecentItems(): List<com.tk.quicksearch.search.recentSearches.RecentSearchEntry> =
            recentSearchesPreferences.getRecentItems()

    fun addRecentItem(entry: com.tk.quicksearch.search.recentSearches.RecentSearchEntry) =
            recentSearchesPreferences.addRecentItem(entry)

    fun clearRecentQueries() = recentSearchesPreferences.clearRecentQueries()

    fun deleteRecentItem(entry: com.tk.quicksearch.search.recentSearches.RecentSearchEntry) =
            recentSearchesPreferences.deleteRecentItem(entry)

    fun areRecentQueriesEnabled(): Boolean = recentSearchesPreferences.areRecentQueriesEnabled()

    fun setRecentQueriesEnabled(enabled: Boolean) =
            recentSearchesPreferences.setRecentQueriesEnabled(enabled)

    // ============================================================================
    // Usage Permission Banner Preferences
    // ============================================================================

    fun getShortcutHintBannerDismissCount(): Int = uiPreferences.getShortcutHintBannerDismissCount()

    fun incrementShortcutHintBannerDismissCount() =
            uiPreferences.incrementShortcutHintBannerDismissCount()

    fun isShortcutHintBannerSessionDismissed(): Boolean =
            uiPreferences.isShortcutHintBannerSessionDismissed()

    fun setShortcutHintBannerSessionDismissed(dismissed: Boolean) =
            uiPreferences.setShortcutHintBannerSessionDismissed(dismissed)

    fun resetShortcutHintBannerSessionDismissed() =
            uiPreferences.resetShortcutHintBannerSessionDismissed()

    fun shouldShowShortcutHintBanner(): Boolean = uiPreferences.shouldShowShortcutHintBanner()

    fun shouldShowDefaultEngineHintBanner(): Boolean =
            uiPreferences.shouldShowDefaultEngineHintBanner()

    fun setDefaultEngineHintBannerDismissed(dismissed: Boolean) =
            uiPreferences.setDefaultEngineHintBannerDismissed(dismissed)

    // ============================================================================
    // Section Preferences
    // ============================================================================

    fun getDisabledSections(): Set<String> = uiPreferences.getDisabledSections()

    fun setDisabledSections(disabled: Set<String>) = uiPreferences.setDisabledSections(disabled)

    // ============================================================================
    // In-App Review Preferences
    // ============================================================================

    fun getFirstAppOpenTime(): Long = uiPreferences.getFirstAppOpenTime()

    fun recordFirstAppOpenTime() = uiPreferences.recordFirstAppOpenTime()

    fun getLastReviewPromptTime(): Long = uiPreferences.getLastReviewPromptTime()

    fun recordReviewPromptTime() = uiPreferences.recordReviewPromptTime()

    fun getReviewPromptedCount(): Int = uiPreferences.getReviewPromptedCount()

    fun incrementReviewPromptedCount() = uiPreferences.incrementReviewPromptedCount()

    fun getAppOpenCount(): Int = uiPreferences.getAppOpenCount()

    fun incrementAppOpenCount() = uiPreferences.incrementAppOpenCount()

    fun recordAppOpenCountAtPrompt() = uiPreferences.recordAppOpenCountAtPrompt()

    fun shouldShowReviewPrompt(): Boolean = uiPreferences.shouldShowReviewPrompt()

    // ============================================================================
    // In-App Update Session Tracking
    // ============================================================================

    fun hasShownUpdateCheckThisSession(): Boolean = uiPreferences.hasShownUpdateCheckThisSession()

    fun setUpdateCheckShownThisSession() = uiPreferences.setUpdateCheckShownThisSession()

    fun resetUpdateCheckSession() = uiPreferences.resetUpdateCheckSession()
}
