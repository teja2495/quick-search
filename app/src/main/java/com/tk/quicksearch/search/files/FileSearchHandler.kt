package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.utils.SearchTextNormalizer

class FileSearchHandler(
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences,
) {
    companion object {
        const val FILE_SEARCH_RESULT_LIMIT = 25
        private const val FILE_SEARCH_PREFETCH_MULTIPLIER = 4
    }

    fun searchFiles(
        query: String,
        enabledFileTypes: Set<FileType>,
        showFolders: Boolean = true,
        showSystemFiles: Boolean = false,
        showHiddenFiles: Boolean = false,
    ): List<DeviceFile> =
        searchFilesInternal(
            query,
            enabledFileTypes,
            userPreferences.getExcludedFileUris(),
            userPreferences.getExcludedFileExtensions(),
            userPreferences.getFolderWhitelistPatterns(),
            userPreferences.getFolderBlacklistPatterns(),
            showFolders,
            showSystemFiles,
            showHiddenFiles,
        )

    private fun searchFilesInternal(
        query: String,
        enabledFileTypes: Set<FileType>,
        excludedFileUris: Set<String>,
        excludedFileExtensions: Set<String>,
        folderWhitelistPatterns: Set<String>,
        folderBlacklistPatterns: Set<String>,
        showFolders: Boolean,
        showSystemFiles: Boolean,
        showHiddenFiles: Boolean,
    ): List<DeviceFile> {
        if (query.isBlank() || !fileRepository.hasPermission()) return emptyList()

        val normalizedQuery = SearchTextNormalizer.normalizeQueryWhitespace(query)
        if (normalizedQuery.length < 2) return emptyList()

        val prefetchLimit = FILE_SEARCH_RESULT_LIMIT * FILE_SEARCH_PREFETCH_MULTIPLIER
        val allFiles =
            fileRepository.searchFiles(normalizedQuery, prefetchLimit)

        return FileSearchAlgorithm.filterCandidates(
            fullList = allFiles,
            query = normalizedQuery,
            enabledFileTypes = enabledFileTypes,
            excludedFileUris = excludedFileUris,
            excludedFileExtensions = excludedFileExtensions,
            folderWhitelistPatterns = folderWhitelistPatterns,
            folderBlacklistPatterns = folderBlacklistPatterns,
            showFolders = showFolders,
            showSystemFiles = showSystemFiles,
            showHiddenFiles = showHiddenFiles,
        )
    }
}
