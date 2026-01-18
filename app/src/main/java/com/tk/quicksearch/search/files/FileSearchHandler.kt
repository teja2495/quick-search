package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.data.FileSearchRepository
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.models.FileTypeUtils
import java.util.Locale

data class FileSearchResults(
    val pinned: List<DeviceFile>,
    val excluded: List<DeviceFile>,
    val results: List<DeviceFile>
)

class FileSearchHandler(
    private val fileRepository: FileSearchRepository,
    private val userPreferences: UserAppPreferences
) {

    fun getFileState(
        query: String,
        enabledFileTypes: Set<FileType>,
        isFilesSectionEnabled: Boolean,
        currentResults: List<DeviceFile>
    ): FileSearchResults {
        // Cache preference reads to avoid repeated SharedPreferences lookups
        val pinnedUris = userPreferences.getPinnedFileUris()
        val excludedUris = userPreferences.getExcludedFileUris()
        val excludedExtensions = userPreferences.getExcludedFileExtensions()

        val pinned = fileRepository.getFilesByUris(pinnedUris)
            .filter { file ->
                val fileType = FileTypeUtils.getFileType(file)
                fileType in enabledFileTypes && !excludedUris.contains(file.uri.toString())
            }
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }

        val excluded = fileRepository.getFilesByUris(excludedUris)
            .sortedBy { it.displayName.lowercase(Locale.getDefault()) }

        val results = if (query.isNotBlank() && isFilesSectionEnabled) {
            searchFilesInternal(query, enabledFileTypes, excludedUris, excludedExtensions)
        } else {
            emptyList()
        }

        return FileSearchResults(pinned, excluded, results)
    }

    fun searchFiles(query: String, enabledFileTypes: Set<FileType>): List<DeviceFile> {
        return searchFilesInternal(
            query,
            enabledFileTypes,
            userPreferences.getExcludedFileUris(),
            userPreferences.getExcludedFileExtensions()
        )
    }

    private fun searchFilesInternal(
        query: String,
        enabledFileTypes: Set<FileType>,
        excludedFileUris: Set<String>,
        excludedFileExtensions: Set<String>
    ): List<DeviceFile> {
        if (query.isBlank() || !fileRepository.hasPermission()) return emptyList()

        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2) return emptyList()

        // Get files from repository
        val allFiles = fileRepository.searchFiles(trimmedQuery, Int.MAX_VALUE)

        // Apply filters
        val filteredFiles = allFiles.filter { file ->
            val fileType = FileTypeUtils.getFileType(file)
            fileType in enabledFileTypes &&
            !excludedFileUris.contains(file.uri.toString()) &&
            !FileUtils.isFileExtensionExcluded(file.displayName, excludedFileExtensions) &&
            !isSystemFolder(file) &&
            !isApkFile(file) &&
            !isSystemFile(file)
        }

        // Rank and return results
        return rankFiles(filteredFiles, trimmedQuery)
    }

    private fun rankFiles(files: List<DeviceFile>, query: String): List<DeviceFile> {
        if (files.isEmpty()) return emptyList()

        val normalizedQuery = query.lowercase(Locale.getDefault())
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Pre-fetch all file nicknames in a single call to reduce SharedPreferences reads
        val distinctFiles = files.distinctBy { it.uri.toString() }
        val fileNicknames = distinctFiles.associate { file ->
            file.uri.toString() to userPreferences.getFileNickname(file.uri.toString())
        }

        return distinctFiles
            .mapNotNull { file ->
                val uriString = file.uri.toString()
                val nickname = fileNicknames[uriString]
                val priority = SearchRankingUtils.calculateMatchPriorityWithNickname(
                    file.displayName, nickname, normalizedQuery, queryTokens
                )
                if (SearchRankingUtils.isOtherMatch(priority)) null else file to priority
            }
            .sortedWith(
                compareBy<Pair<DeviceFile, Int>> { it.second }
                    .thenBy { it.first.displayName.lowercase(Locale.getDefault()) }
            )
            .map { it.first }
    }

    private fun isApkFile(deviceFile: DeviceFile): Boolean {
        val mime = deviceFile.mimeType?.lowercase(Locale.getDefault())
        if (mime == "application/vnd.android.package-archive") {
            return true
        }
        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.endsWith(".apk")
    }

    private fun isSystemFile(deviceFile: DeviceFile): Boolean {
        val name = deviceFile.displayName
        // Check for hidden files (starting with dot)
        if (name.startsWith(".")) return true

        val extension = FileUtils.getFileExtension(name)?.lowercase(Locale.getDefault()) ?: return false

        val systemExcludedExtensions = setOf(
            "tmp", "temp", "cache", "log", "bak", "backup", "old", "orig",
            "swp", "swo", "part", "crdownload", "download", "tmpfile"
        )

        // WhatsApp/Signal backup files (crypt, crypt12, crypt14, etc.)
        if (extension.startsWith("crypt")) {
            // Check if it's "crypt" or "crypt" followed by numbers
            return extension == "crypt" || extension.drop(5).all { it.isDigit() }
        }

        return extension in systemExcludedExtensions
    }

    private fun isSystemFolder(deviceFile: DeviceFile): Boolean {
        if (!deviceFile.isDirectory) return false

        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.startsWith("com.")
    }
}