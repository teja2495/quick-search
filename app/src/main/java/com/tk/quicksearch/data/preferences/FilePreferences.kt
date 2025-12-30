package com.tk.quicksearch.data.preferences

import android.content.Context

import com.tk.quicksearch.model.FileType

/**
 * Preferences for file-related settings such as pinned/excluded files and file types.
 */
class FilePreferences(context: Context) : BasePreferences(context) {

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

    fun getExcludedFileExtensions(): Set<String> = getStringSet(KEY_EXCLUDED_FILE_EXTENSIONS)

    fun addExcludedFileExtension(extension: String): Set<String> = updateStringSet(KEY_EXCLUDED_FILE_EXTENSIONS) {
        it.add(extension.lowercase().removePrefix("."))
    }

    fun removeExcludedFileExtension(extension: String): Set<String> = updateStringSet(KEY_EXCLUDED_FILE_EXTENSIONS) {
        it.remove(extension.lowercase().removePrefix("."))
    }

    fun clearAllExcludedFileExtensions(): Set<String> = clearStringSet(KEY_EXCLUDED_FILE_EXTENSIONS)

    fun getEnabledFileTypes(): Set<FileType> {
        val enabledNames = prefs.getStringSet(KEY_ENABLED_FILE_TYPES, null)
        return if (enabledNames == null) {
            // Default: all file types enabled except OTHER
            FileType.values().filter { it != FileType.OTHER }.toSet()
        } else {
            val migratedTypes = migrateAndGetFileTypes(enabledNames)
            // If migration occurred, save the migrated preferences
            val currentNames = enabledNames.map { name ->
                when (name) {
                    "IMAGES", "VIDEOS" -> "PHOTOS_AND_VIDEOS"
                    else -> name
                }
            }.toSet()
            if (currentNames != enabledNames) {
                setEnabledFileTypes(migratedTypes)
            }
            migratedTypes
        }
    }

    fun setEnabledFileTypes(enabled: Set<FileType>) {
        prefs.edit().putStringSet(KEY_ENABLED_FILE_TYPES, enabled.map { it.name }.toSet()).apply()
    }
}
