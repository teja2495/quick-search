package com.tk.quicksearch.search.files

import com.tk.quicksearch.search.models.DeviceFile
import java.util.Locale

// Matches DeviceFile paths using limited patterns.
// Only the "*/path/*" form is supported, which matches any path containing the inner segment.
object FolderPathPatternMatcher {
    private val multiSlashRegex = "/+".toRegex()

    fun normalizePathPatterns(patterns: Set<String>): Set<String> =
        patterns
            .asSequence()
            .mapNotNull(::normalizePattern)
            .toSet()

    fun createPathMatcher(
        whitelistPatterns: Set<String>,
        blacklistPatterns: Set<String>,
    ): (DeviceFile) -> Boolean {
        val normalizedWhitelist = normalizePathPatterns(whitelistPatterns)
        val normalizedBlacklist = normalizePathPatterns(blacklistPatterns)

        return { file ->
            val candidatePath = buildCandidatePath(file)
            val matchesWhitelist =
                normalizedWhitelist.isEmpty() ||
                    normalizedWhitelist.any { pattern ->
                        matchesPattern(candidatePath, pattern)
                    }
            val matchesBlacklist =
                normalizedBlacklist.any { pattern ->
                    matchesPattern(candidatePath, pattern)
                }
            matchesWhitelist && !matchesBlacklist
        }
    }

    private fun normalizePattern(rawPattern: String): String? {
        val trimmed = rawPattern.trim()
        if (trimmed.isBlank()) return null
        val normalizedInput = trimmed.replace('\\', '/')
        val collapsed = normalizedInput.replace(multiSlashRegex, "/")
        val withoutLeadingSlash = collapsed.removePrefix("/")
        val normalized = withoutLeadingSlash.trim()
        if (normalized.isBlank()) return null
        return normalized.lowercase(Locale.getDefault())
    }

    private fun buildCandidatePath(file: DeviceFile): String {
        val relativeSegments =
            file.relativePath
                .orEmpty()
                .replace('\\', '/')
                .split('/')
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toList()
        val fileName = file.displayName.trim()
        if (fileName.isEmpty()) return ""

        val segments = relativeSegments + fileName
        return segments.joinToString("/").lowercase(Locale.getDefault())
    }

    private fun matchesPattern(path: String, pattern: String): Boolean {
        if (pattern.startsWith("*/") && pattern.endsWith("/*") && pattern.length > 4) {
            val core = pattern.removePrefix("*/").removeSuffix("/*")
            return core.isNotBlank() && path.contains(core)
        }
        return false
    }
}
