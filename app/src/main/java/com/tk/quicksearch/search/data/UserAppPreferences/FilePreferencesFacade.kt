package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.data.preferences.FilePreferences
import com.tk.quicksearch.search.models.FileType

/**
 * Facade for file preference operations
 */
class FilePreferencesFacade(
    private val filePreferences: FilePreferences
) {
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

    fun getEnabledFileTypes(): Set<FileType> =
            filePreferences.getEnabledFileTypes()

    fun setEnabledFileTypes(enabled: Set<FileType>) =
            filePreferences.setEnabledFileTypes(enabled)

    fun clearEnabledFileTypes(): Set<FileType> =
            filePreferences.clearEnabledFileTypes()

    fun getShowFoldersInResults(): Boolean = filePreferences.getShowFoldersInResults()

    fun setShowFoldersInResults(show: Boolean) = filePreferences.setShowFoldersInResults(show)

    fun getShowSystemFiles(): Boolean = filePreferences.getShowSystemFiles()

    fun setShowSystemFiles(show: Boolean) = filePreferences.setShowSystemFiles(show)

    fun getShowHiddenFiles(): Boolean = filePreferences.getShowHiddenFiles()

    fun setShowHiddenFiles(show: Boolean) = filePreferences.setShowHiddenFiles(show)

    fun getFolderWhitelistPatterns(): Set<String> = filePreferences.getFolderWhitelistPatterns()

    fun setFolderWhitelistPatterns(patterns: Set<String>) =
            filePreferences.setFolderWhitelistPatterns(patterns)

    fun getFolderBlacklistPatterns(): Set<String> = filePreferences.getFolderBlacklistPatterns()

    fun setFolderBlacklistPatterns(patterns: Set<String>) =
            filePreferences.setFolderBlacklistPatterns(patterns)
}