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

    fun getPinnedFileUris(): Set<String> = getPinnedStringItems(BasePreferences.KEY_PINNED_FILE_URIS)

    fun getExcludedFileUris(): Set<String> = getExcludedStringItems(BasePreferences.KEY_EXCLUDED_FILE_URIS)

    fun pinFile(uri: String): Set<String> = pinStringItem(BasePreferences.KEY_PINNED_FILE_URIS, uri)

    fun unpinFile(uri: String): Set<String> = unpinStringItem(BasePreferences.KEY_PINNED_FILE_URIS, uri)

    fun excludeFile(uri: String): Set<String> = excludeStringItem(BasePreferences.KEY_EXCLUDED_FILE_URIS, uri)

    fun removeExcludedFile(uri: String): Set<String> = removeExcludedStringItem(BasePreferences.KEY_EXCLUDED_FILE_URIS, uri)

    fun clearAllExcludedFiles(): Set<String> = clearAllExcludedStringItems(BasePreferences.KEY_EXCLUDED_FILE_URIS)

    fun getExcludedFileExtensions(): Set<String> = getStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS)

    fun addExcludedFileExtension(extension: String): Set<String> = updateStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS) {
        it.add(extension.lowercase().removePrefix("."))
    }

    fun removeExcludedFileExtension(extension: String): Set<String> = updateStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS) {
        it.remove(extension.lowercase().removePrefix("."))
    }

    fun clearAllExcludedFileExtensions(): Set<String> = clearStringSet(BasePreferences.KEY_EXCLUDED_FILE_EXTENSIONS)

    fun getEnabledFileTypes(): Set<FileType> {
        val enabledNames = prefs.getStringSet(BasePreferences.KEY_ENABLED_FILE_TYPES, null)
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
        prefs.edit().putStringSet(BasePreferences.KEY_ENABLED_FILE_TYPES, enabled.map { it.name }.toSet()).apply()
    }

}
