package com.tk.quicksearch.search.files

import android.os.CancellationSignal
import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
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
        private const val FUZZY_FILE_CANDIDATE_LIMIT = 350
    }

    fun searchFiles(
        queryContext: SearchQueryContext,
        enabledFileTypes: Set<FileType>,
        showFolders: Boolean = true,
        showSystemFiles: Boolean = false,
        recentFileScores: Map<String, Int> = emptyMap(),
        fileOpenCounts: Map<String, Int> = emptyMap(),
        secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal =
            userPreferences.getSecondaryRankingSignal(),
        includeFuzzyCandidates: Boolean = false,
        resultLimit: Int = FILE_SEARCH_RESULT_LIMIT,
        fuzzyCandidateLimit: Int = FUZZY_FILE_CANDIDATE_LIMIT,
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
            recentFileScores,
            fileOpenCounts,
            secondaryRankingSignal,
            includeFuzzyCandidates,
            resultLimit,
            fuzzyCandidateLimit,
            extensionQuery = null,
            cancellationSignal = null,
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
        recentFileScores: Map<String, Int> = emptyMap(),
        fileOpenCounts: Map<String, Int> = emptyMap(),
        secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal =
            userPreferences.getSecondaryRankingSignal(),
        includeFuzzyCandidates: Boolean = false,
        resultLimit: Int = FILE_SEARCH_RESULT_LIMIT,
        fuzzyCandidateLimit: Int = FUZZY_FILE_CANDIDATE_LIMIT,
        extensionQuery: FileExtensionQuery? = null,
        cancellationSignal: CancellationSignal? = null,
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
            recentFileScores,
            fileOpenCounts,
            secondaryRankingSignal,
            includeFuzzyCandidates,
            resultLimit,
            fuzzyCandidateLimit,
            extensionQuery,
            cancellationSignal,
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
        recentFileScores: Map<String, Int>,
        fileOpenCounts: Map<String, Int>,
        secondaryRankingSignal: com.tk.quicksearch.search.models.SecondaryRankingSignal,
        includeFuzzyCandidates: Boolean,
        resultLimit: Int,
        fuzzyCandidateLimit: Int,
        extensionQuery: FileExtensionQuery?,
        cancellationSignal: CancellationSignal?,
    ): List<DeviceFile> {
        // With an extension query, [queryContext] holds only the name part, which may be blank.
        val whitespaceNormalized = queryContext.normalizedQuery
        if (!fileRepository.hasPermission()) return emptyList()
        if (extensionQuery == null && whitespaceNormalized.length < 2) return emptyList()

        // Fetch more candidates than we need so that items dropped by the scorer
        // don't cause the final list to come up short.
        val prefetchLimit = resultLimit * FILE_SEARCH_PREFETCH_MULTIPLIER
        val exactCandidates =
            fileRepository.searchFiles(
                whitespaceNormalized,
                prefetchLimit,
                extensionQuery?.extension,
                cancellationSignal,
            )
        if (extensionQuery != null && whitespaceNormalized.isBlank()) {
            // Extension-only query: newest files first (repository order), recently opened ahead.
            return FileSearchAlgorithm.filterCandidates(
                fullList = exactCandidates,
                query = whitespaceNormalized,
                enabledFileTypes = enabledFileTypes,
                excludedFileUris = excludedFileUris,
                excludedFileExtensions = excludedFileExtensions,
                folderWhitelistPatterns = folderWhitelistPatterns,
                folderBlacklistPatterns = folderBlacklistPatterns,
                showFolders = showFolders,
                showSystemFiles = showSystemFiles,
                extensionQuery = extensionQuery,
            ).sortedByDescending { recentFileScores[it.uri.toString()] ?: 0 }
                .take(resultLimit)
        }
        val allFiles =
            if (includeFuzzyCandidates) {
                (exactCandidates + fileRepository.getRecentFilesSnapshot(fuzzyCandidateLimit, cancellationSignal))
                    .distinctBy { it.uri.toString() }
            } else {
                exactCandidates
            }

        if (includeFuzzyCandidates) {
            // Ranking (including nicknames) happens once in UnifiedResultRanker.
            return FileSearchAlgorithm.filterCandidates(
                fullList = allFiles,
                query = whitespaceNormalized,
                enabledFileTypes = enabledFileTypes,
                excludedFileUris = excludedFileUris,
                excludedFileExtensions = excludedFileExtensions,
                folderWhitelistPatterns = folderWhitelistPatterns,
                folderBlacklistPatterns = folderBlacklistPatterns,
                showFolders = showFolders,
                showSystemFiles = showSystemFiles,
                extensionQuery = extensionQuery,
            )
        }

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
            fileNicknames = fileNicknames,
            recentFileScores = recentFileScores,
            fileOpenCounts = fileOpenCounts,
            secondaryRankingSignal = secondaryRankingSignal,
            resultLimit = resultLimit,
            extensionQuery = extensionQuery,
        )
    }
}
