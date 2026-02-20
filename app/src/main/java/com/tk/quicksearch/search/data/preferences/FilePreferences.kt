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

    fun getExcludedFileUris(): Set<String> = getExcludedStringItems(BasePreferences.KEY_EXCLUDED_FILE_URIS)

    fun pinFile(uri: String): Set<String> = pinStringItem(BasePreferences.KEY_PINNED_FILE_URIS, uri)

    fun unpinFile(uri: String): Set<String> = unpinStringItem(BasePreferences.KEY_PINNED_FILE_URIS, uri)

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

    fun getShowSystemFiles(): Boolean = prefs.getBoolean(BasePreferences.KEY_SHOW_SYSTEM_FILES, false)

    fun setShowSystemFiles(show: Boolean) = prefs.edit().putBoolean(BasePreferences.KEY_SHOW_SYSTEM_FILES, show).apply()

    fun getShowHiddenFiles(): Boolean = prefs.getBoolean(BasePreferences.KEY_SHOW_HIDDEN_FILES, false)

    fun setShowHiddenFiles(show: Boolean) = prefs.edit().putBoolean(BasePreferences.KEY_SHOW_HIDDEN_FILES, show).apply()

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
            // Default: all file types enabled except OTHER
            FileType.values().filter { it != FileType.OTHER }.toSet()
        } else {
            val enabledNames = prefs.getStringSet(key, emptySet()).orEmpty()
            val migratedTypes = migrateAndGetFileTypes(enabledNames)
            // If migration occurred, save the migrated preferences
            val currentNames =
                enabledNames
                    .map { name ->
                        when (name) {
                            "PHOTOS_AND_VIDEOS" -> {
                                listOf("PICTURES", "VIDEOS")
                            }

                            "IMAGES" -> {
                                listOf("PICTURES")
                            }

                            "VIDEOS" -> {
                                listOf("VIDEOS")
                            }

                            else -> {
                                listOf(name)
                            }
                        }
                    }.flatten()
                    .toSet()
            if (currentNames != enabledNames) {
                setEnabledFileTypes(migratedTypes)
            }
            migratedTypes
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
        // Return the default enabled file types (all except OTHER)
        return FileType.values().filter { it != FileType.OTHER }.toSet()
    }
}
