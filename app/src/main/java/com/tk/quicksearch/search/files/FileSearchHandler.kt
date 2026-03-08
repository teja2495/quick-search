package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.utils.SearchQueryContext

class FileSearchHandler(
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences,
) {
    companion object {
        const val FILE_SEARCH_RESULT_LIMIT = 25
        private const val FILE_SEARCH_PREFETCH_MULTIPLIER = 4
    }

    fun searchFiles(
        queryContext: SearchQueryContext,
        enabledFileTypes: Set<FileType>,
        showFolders: Boolean = true,
        showSystemFiles: Boolean = false,
        showHiddenFiles: Boolean = false,
        recentFileScores: Map<String, Int> = emptyMap(),
    ): List<DeviceFile> =
        searchFilesInternal(
            queryContext,
            enabledFileTypes,
            userPreferences.getExcludedFileUris(),
            userPreferences.getExcludedFileExtensions(),
            userPreferences.getFolderWhitelistPatterns(),
            userPreferences.getFolderBlacklistPatterns(),
            showFolders,
            showSystemFiles,
            showHiddenFiles,
            recentFileScores,
        )

    /**
     * Overload that accepts pre-fetched preference values to avoid repeated SharedPreferences
     * reads.
     */
    fun searchFiles(
        queryContext: SearchQueryContext,
        enabledFileTypes: Set<FileType>,
        excludedFileUris: Set<String>,
        excludedFileExtensions: Set<String>,
        folderWhitelistPatterns: Set<String>,
        folderBlacklistPatterns: Set<String>,
        showFolders: Boolean = true,
        showSystemFiles: Boolean = false,
        showHiddenFiles: Boolean = false,
        recentFileScores: Map<String, Int> = emptyMap(),
    ): List<DeviceFile> =
        searchFilesInternal(
            queryContext,
            enabledFileTypes,
            excludedFileUris,
            excludedFileExtensions,
            folderWhitelistPatterns,
            folderBlacklistPatterns,
            showFolders,
            showSystemFiles,
            showHiddenFiles,
            recentFileScores,
        )

    private fun searchFilesInternal(
        queryContext: SearchQueryContext,
        enabledFileTypes: Set<FileType>,
        excludedFileUris: Set<String>,
        excludedFileExtensions: Set<String>,
        folderWhitelistPatterns: Set<String>,
        folderBlacklistPatterns: Set<String>,
        showFolders: Boolean,
        showSystemFiles: Boolean,
        showHiddenFiles: Boolean,
        recentFileScores: Map<String, Int>,
    ): List<DeviceFile> {
        val whitespaceNormalized = queryContext.normalizedQuery
        if (whitespaceNormalized.isBlank() || !fileRepository.hasPermission()) return emptyList()
        if (whitespaceNormalized.length < 2) return emptyList()

        // Fetch more candidates than we need so that items dropped by the scorer
        // don't cause the final list to come up short.
        val prefetchLimit = FILE_SEARCH_RESULT_LIMIT * FILE_SEARCH_PREFETCH_MULTIPLIER
        val allFiles = fileRepository.searchFiles(whitespaceNormalized, prefetchLimit)

        // Pre-fetch nicknames for all candidates so search() can apply them during
        // ranking — avoids a second pass in UnifiedSearchHandler for display-name matches.
        val fileNicknames =
            allFiles.associate { file ->
                file.uri.toString() to userPreferences.getFileNickname(file.uri.toString())
            }

        return FileSearchAlgorithm.search(
            fullList = allFiles,
            queryContext = queryContext,
            enabledFileTypes = enabledFileTypes,
            excludedFileUris = excludedFileUris,
            excludedFileExtensions = excludedFileExtensions,
            folderWhitelistPatterns = folderWhitelistPatterns,
            folderBlacklistPatterns = folderBlacklistPatterns,
            showFolders = showFolders,
            showSystemFiles = showSystemFiles,
            showHiddenFiles = showHiddenFiles,
            fileNicknames = fileNicknames,
            recentFileScores = recentFileScores,
            resultLimit = FILE_SEARCH_RESULT_LIMIT,
        )
    }
}
