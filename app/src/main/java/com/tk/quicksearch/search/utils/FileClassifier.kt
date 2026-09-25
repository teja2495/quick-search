
package com.tk.quicksearch.search.utils

import com.tk.quicksearch.search.models.DeviceFile
import java.util.Locale

/**
 * Centralised file-classification helpers shared across search components.
 *
 * Previously, [FileSearchAlgorithm] and [UnifiedSearchHandler] each kept a private copy of
 * [SYSTEM_EXCLUDED_EXTENSIONS] and the three `is*` functions. Any bug fix had to be applied in
 * two places. This object is the single source of truth.
 */
object FileClassifier {

    val SYSTEM_EXCLUDED_EXTENSIONS: Set<String> =
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

    /**
     * Returns `true` when the file should be treated as a system/internal file that is normally
     * hidden from end-users (dot-files, WhatsApp crypt databases, temp files, etc.).
     */
    fun isSystemFile(deviceFile: DeviceFile): Boolean {
        val name = deviceFile.displayName
        if (name.startsWith(".")) return true

        val extension =
            FileUtils.getFileExtension(name)?.lowercase(Locale.getDefault()) ?: return false

        if (extension.startsWith("crypt")) {
            return extension == "crypt" || extension.drop(5).all { it.isDigit() }
        }

        return extension in SYSTEM_EXCLUDED_EXTENSIONS
    }

    /**
     * Returns `true` when the directory name looks like an Android app data folder
     * (e.g. `com.example.myapp`).
     */
    fun isSystemFolder(deviceFile: DeviceFile): Boolean {
        if (!deviceFile.isDirectory) return false
        val name = deviceFile.displayName.lowercase(Locale.getDefault())
        return name.startsWith("com.")
    }

    /**
     * Returns `true` when the file or any ancestor directory segment is a trash folder
     * (`.Trash`, `.trash`, or `.trash-<uid>` style paths).
     */
    fun isInTrashFolder(deviceFile: DeviceFile): Boolean {
        if (deviceFile.displayName.equals(".Trash", ignoreCase = true)) return true

        val relativePath = deviceFile.relativePath ?: return false
        return relativePath
            .split('/')
            .asSequence()
            .filter { it.isNotBlank() }
            .map { it.lowercase(Locale.getDefault()) }
            .any { segment -> segment == ".trash" || segment.startsWith(".trash-") }
    }
}
