package com.tk.quicksearch.util

/**
 * Utility functions for file operations.
 */
object FileUtils {

    /**
     * Extracts the file extension from a display name.
     * Returns null if no extension is found.
     *
     * @param displayName The file display name
     * @return The file extension without the dot, or null if none found
     */
    fun getFileExtension(displayName: String): String? {
        val lastDotIndex = displayName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < displayName.length - 1) {
            displayName.substring(lastDotIndex + 1).lowercase()
        } else {
            null
        }
    }

    /**
     * Checks if a file extension is in the excluded extensions set.
     *
     * @param displayName The file display name
     * @param excludedExtensions Set of excluded extensions (without dots)
     * @return true if the file's extension is excluded
     */
    fun isFileExtensionExcluded(displayName: String, excludedExtensions: Set<String>): Boolean {
        val extension = getFileExtension(displayName)
        return extension != null && excludedExtensions.contains(extension)
    }
}
