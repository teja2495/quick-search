package com.tk.quicksearch.search.files

import android.net.Uri
import com.tk.quicksearch.search.core.FileManagementConfig
import com.tk.quicksearch.search.core.GenericManagementHandler
import com.tk.quicksearch.search.core.ManagementHandler
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.utils.FileUtils
import kotlinx.coroutines.CoroutineScope

/**
 * Handles file management operations like pinning, excluding, and nicknames.
 */
class FileManagementHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    private val onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit,
) : ManagementHandler<DeviceFile> by GenericManagementHandler(
        FileManagementConfig(),
        userPreferences,
        scope,
        onStateChanged,
        onUiStateUpdate,
    ) {
    // File-specific methods that don't use BaseManagementHandler
    fun excludeFileExtension(deviceFile: DeviceFile): Set<String> {
        val extension = FileUtils.getFileExtension(deviceFile.displayName)
        return if (extension != null) {
            val updatedExtensions = userPreferences.addExcludedFileExtension(extension)
            onUiStateUpdate { state ->
                state.copy(
                    fileResults =
                        state.fileResults.filterNot {
                            FileUtils.isFileExtensionExcluded(it.displayName, updatedExtensions)
                        },
                )
            }
            updatedExtensions
        } else {
            userPreferences.getExcludedFileExtensions()
        }
    }

    fun removeExcludedFileExtension(extension: String): Set<String> {
        val updatedExtensions = userPreferences.removeExcludedFileExtension(extension)
        // Re-run search to include previously excluded files with this extension
        onStateChanged()
        return updatedExtensions
    }

    // Convenience methods that delegate to the interface
    fun pinFile(deviceFile: DeviceFile) = pinItem(deviceFile)

    fun unpinFile(deviceFile: DeviceFile) = unpinItem(deviceFile)

    fun excludeFile(deviceFile: DeviceFile) = excludeItem(deviceFile)

    fun removeExcludedFile(deviceFile: DeviceFile) = removeExcludedItem(deviceFile)

    fun setFileNickname(
        deviceFile: DeviceFile,
        nickname: String?,
    ) = setItemNickname(deviceFile, nickname)

    fun getFileNickname(uri: String): String? = getItemNickname(DeviceFile(Uri.parse(uri), "", null, 0L, false))

    fun clearAllExcludedFiles() = clearAllExcludedItems()
}
