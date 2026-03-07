package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.search.utils.DefaultSearchMatcher
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.search.utils.SearchTextNormalizer
import java.util.Locale

object FileSearchAlgorithm {
    private val SYSTEM_EXCLUDED_EXTENSIONS =
        setOf(
            "tmp",
            "temp",
            "cache",
            "log",
            "bak",
            "backup",
            "old",
            "orig",
            "swp",
            "swo",
            "part",
            "crdownload",
            "download",
            "tmpfile",
        )

    fun search(
        fullList: List<DeviceFile>,
        query: String,
        enabledFileTypes: Set<FileType>,
        excludedFileUris: Set<String>,
        excludedFileExtensions: Set<String>,
        folderWhitelistPatterns: Set<String>,
        folderBlacklistPatterns: Set<String>,
        showFolders: Boolean,
        showSystemFiles: Boolean,
        showHiddenFiles: Boolean,
        fileNicknames: Map<String, String?>,
        resultLimit: Int = 25,
    ): List<DeviceFile> {
        val normalizedQuery = SearchTextNormalizer.normalizeQueryWhitespace(query)
        val filteredFiles =
            filterCandidates(
                fullList = fullList,
                query = query,
                enabledFileTypes = enabledFileTypes,
                excludedFileUris = excludedFileUris,
                excludedFileExtensions = excludedFileExtensions,
                folderWhitelistPatterns = folderWhitelistPatterns,
                folderBlacklistPatterns = folderBlacklistPatterns,
                showFolders = showFolders,
                showSystemFiles = showSystemFiles,
                showHiddenFiles = showHiddenFiles,
            )

        return rankFiles(filteredFiles, normalizedQuery, fileNicknames).take(resultLimit)
    }

    fun filterCandidates(
        fullList: List<DeviceFile>,
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
        if (query.isBlank()) return emptyList()

        val normalizedQuery = SearchTextNormalizer.normalizeQueryWhitespace(query)
        if (normalizedQuery.length < 2) return emptyList()

        val pathMatcher =
            FolderPathPatternMatcher.createPathMatcher(
                whitelistPatterns = folderWhitelistPatterns,
                blacklistPatterns = folderBlacklistPatterns,
            )

        return fullList.filter { file ->
            val fileType = FileTypeUtils.getFileType(file)
            val fileTypeMatches = fileType in enabledFileTypes

            if (file.isDirectory && !showFolders) return@filter false

            val isApk = isApkFile(file)
            if (isApk && FileType.APKS !in enabledFileTypes) return@filter false

            val isSystem = isSystemFolder(file) || isSystemFile(file)
            if (isSystem && !showSystemFiles) return@filter false

            val isHidden = file.displayName.startsWith(".")
            if (isHidden && !showHiddenFiles) return@filter false

            if (!showHiddenFiles && isInTrashFolder(file)) return@filter false

            fileTypeMatches &&
                !excludedFileUris.contains(file.uri.toString()) &&
                pathMatcher(file) &&
                !FileUtils.isFileExtensionExcluded(
                    file.displayName,
                    excludedFileExtensions,
                )
        }
    }

    private fun rankFiles(
        files: List<DeviceFile>,
        query: String,
        fileNicknames: Map<String, String?>,
    ): List<DeviceFile> {
        if (files.isEmpty()) return emptyList()

        val queryContext = SearchQueryContext.fromRawQuery(query)

        return files
            .distinctBy { it.uri.toString() }
            .mapNotNull { file ->
                val uriString = file.uri.toString()
                val nickname = fileNicknames[uriString]
                val priority = FileSearchPolicy.matchPriority(file.displayName, nickname, queryContext)
                if (!DefaultSearchMatcher.isMatch(priority)) {
                    null
                } else {
                    file to priority
                }
            }.sortedWith(
                compareBy<Pair<DeviceFile, Int>> { it.second }.thenBy {
                    it.first.displayName.lowercase(Locale.getDefault())
                },
            ).map { it.first }
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
        if (name.startsWith(".")) return true

        val extension =
            FileUtils.getFileExtension(name)?.lowercase(Locale.getDefault())
                ?: return false

        if (extension.startsWith("crypt")) {
            return extension == "crypt" || extension.drop(5).all { it.isDigit() }
        }

        return extension in SYSTEM_EXCLUDED_EXTENSIONS
    }

    private fun isSystemFolder(deviceFile: DeviceFile): Boolean {
        if (!deviceFile.isDirectory) return false
        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.startsWith("com.")
    }

    private fun isInTrashFolder(deviceFile: DeviceFile): Boolean {
        if (deviceFile.displayName.equals(".Trash", ignoreCase = true)) return true

        val relativePath = deviceFile.relativePath ?: return false
        return relativePath
            .split('/')
            .asSequence()
            .filter { it.isNotBlank() }
            .map { it.lowercase(Locale.getDefault()) }
            .any { segment ->
                segment == ".trash" || segment.startsWith(".trash-")
            }
    }
}
