package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.models.FileType

/** Preferences for file-related settings such as pinned/excluded files and file types. */
class FilePreferences(
    context: Context,
) : BasePreferences(context) {
    // ============================================================================
    // File Preferences
    // ============================================================================

    fun getPinnedFileUris(): Set<String> = getPinnedStringItems(BasePreferences.KEY_PINNED_FILE_URIS)

    fun getPinnedFileOrder(): List<String> = getStringListPref(BasePreferences.KEY_PINNED_FILE_ORDER)

    fun setPinnedFileOrder(order: List<String>): List<String> =
        order.distinct().also { setStringListPref(BasePreferences.KEY_PINNED_FILE_ORDER, it) }

    fun getExcludedFileUris(): Set<String> = getExcludedStringItems(BasePreferences.KEY_EXCLUDED_FILE_URIS)

    fun pinFile(uri: String): Set<String> =
        pinStringItem(BasePreferences.KEY_PINNED_FILE_URIS, uri).also {
            if (uri !in getPinnedFileOrder()) {
                setPinnedFileOrder(getPinnedFileOrder() + uri)
            }
        }

    fun unpinFile(uri: String): Set<String> =
        unpinStringItem(BasePreferences.KEY_PINNED_FILE_URIS, uri).also {
            setPinnedFileOrder(getPinnedFileOrder().filterNot { it == uri })
        }

    fun excludeFile(uri: String): Set<String> = excludeStringItem(BasePreferences.KEY_EXCLUDED_FILE_URIS, uri)

    fun removeExcludedFile(uri: String): Set<String> = removeExcludedStringItem(BasePreferences.KEY_EXCLUDED_FILE_URIS, uri)

    fun clearAllExcludedFiles(): Set<String> = clearAllExcludedStringItems(BasePreferences.KEY_EXCLUDED_FILE_URIS)

    fun getExcludedFileExtensions(): Set<String> = getStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS)

    fun addExcludedFileExtension(extension: String): Set<String> =
        updateStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS) {
            it.add(extension.lowercase().removePrefix("."))
        }

    fun removeExcludedFileExtension(extension: String): Set<String> =
        updateStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS) {
            it.remove(extension.lowercase().removePrefix("."))
        }

    fun clearAllExcludedFileExtensions(): Set<String> = clearStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS)

    fun getShowFoldersInResults(): Boolean = prefs.getBoolean(BasePreferences.KEY_SHOW_FOLDERS_IN_RESULTS, false)

    fun setShowFoldersInResults(show: Boolean) = prefs.edit().putBoolean(BasePreferences.KEY_SHOW_FOLDERS_IN_RESULTS, show).apply()

    fun areFilePreviewsEnabled(): Boolean = prefs.getBoolean(BasePreferences.KEY_FILE_PREVIEWS_ENABLED, true)

    fun setFilePreviewsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(BasePreferences.KEY_FILE_PREVIEWS_ENABLED, enabled).apply()

    fun getShowSystemFiles(): Boolean =
        prefs.getBoolean(BasePreferences.KEY_SHOW_SYSTEM_FILES, false) ||
            prefs.getBoolean(BasePreferences.KEY_SHOW_HIDDEN_FILES, false)

    fun setShowSystemFiles(show: Boolean) =
        prefs
            .edit()
            .putBoolean(BasePreferences.KEY_SHOW_SYSTEM_FILES, show)
            .remove(BasePreferences.KEY_SHOW_HIDDEN_FILES)
            .apply()

    fun getFolderWhitelistPatterns(): Set<String> = getStringSet(BasePreferences.KEY_FOLDER_WHITELIST_PATTERNS)

    fun setFolderWhitelistPatterns(patterns: Set<String>) =
        prefs
            .edit()
            .putStringSet(
                BasePreferences.KEY_FOLDER_WHITELIST_PATTERNS,
                patterns,
            ).apply()

    fun getFolderBlacklistPatterns(): Set<String> = getStringSet(BasePreferences.KEY_FOLDER_BLACKLIST_PATTERNS)

    fun setFolderBlacklistPatterns(patterns: Set<String>) =
        prefs
            .edit()
            .putStringSet(
                BasePreferences.KEY_FOLDER_BLACKLIST_PATTERNS,
                patterns,
            ).apply()

    fun getEnabledFileTypes(): Set<FileType> {
        val key = BasePreferences.KEY_ENABLED_FILE_TYPES
        return if (!prefs.contains(key)) {
            // Default: all file types enabled
            FileType.values().toSet()
        } else {
            val enabledNames = prefs.getStringSet(key, emptySet()).orEmpty()
            enabledNames
                .mapNotNull { name -> FileType.values().find { it.name == name } }
                .toSet()
        }
    }

    fun setEnabledFileTypes(enabled: Set<FileType>) {
        prefs
            .edit()
            .putStringSet(
                BasePreferences.KEY_ENABLED_FILE_TYPES,
                enabled.map { it.name }.toSet(),
            ).apply()
    }

    fun clearEnabledFileTypes(): Set<FileType> {
        clearStringSet(BasePreferences.KEY_ENABLED_FILE_TYPES)
        // Return the default enabled file types
        return FileType.values().toSet()
    }
}
