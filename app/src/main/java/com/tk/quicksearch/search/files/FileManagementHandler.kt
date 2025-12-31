package com.tk.quicksearch.search.files

import com.tk.quicksearch.data.UserAppPreferences
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles file management operations like pinning, excluding, and nicknames.
 */
class FileManagementHandler(
    private val userPreferences: UserAppPreferences,
    private val scope: CoroutineScope,
    private val onStateChanged: () -> Unit,
    private val onUiStateUpdate: ((SearchUiState) -> SearchUiState) -> Unit
) {

    fun pinFile(deviceFile: DeviceFile) {
        val uriString = deviceFile.uri.toString()
        scope.launch(Dispatchers.IO) {
            userPreferences.pinFile(uriString)
            onStateChanged()
        }
    }

    fun unpinFile(deviceFile: DeviceFile) {
        val uriString = deviceFile.uri.toString()
        // Update UI immediately for responsive feedback
        onUiStateUpdate { state ->
            state.copy(
                pinnedFiles = state.pinnedFiles.filterNot { it.uri.toString() == uriString }
            )
        }
        scope.launch(Dispatchers.IO) {
            userPreferences.unpinFile(uriString)
            onStateChanged()
        }
    }

    fun excludeFile(deviceFile: DeviceFile) {
        val uriString = deviceFile.uri.toString()
        scope.launch(Dispatchers.IO) {
            userPreferences.excludeFile(uriString)
            if (userPreferences.getPinnedFileUris().contains(uriString)) {
                userPreferences.unpinFile(uriString)
            }

            onUiStateUpdate { state ->
                state.copy(
                    fileResults = state.fileResults.filterNot { it.uri.toString() == uriString },
                    pinnedFiles = state.pinnedFiles.filterNot { it.uri.toString() == uriString }
                )
            }
            onStateChanged()
        }
    }

    fun excludeFileExtension(deviceFile: DeviceFile) {
        val extension = FileUtils.getFileExtension(deviceFile.displayName)
        if (extension != null) {
            scope.launch(Dispatchers.IO) {
                userPreferences.addExcludedFileExtension(extension)

                onUiStateUpdate { state ->
                    state.copy(
                        fileResults = state.fileResults.filterNot {
                            FileUtils.isFileExtensionExcluded(it.displayName, userPreferences.getExcludedFileExtensions())
                        }
                    )
                }
                onStateChanged()
            }
        }
    }

    fun removeExcludedFileExtension(extension: String) {
        scope.launch(Dispatchers.IO) {
            userPreferences.removeExcludedFileExtension(extension)
            // Re-run search to include previously excluded files with this extension
            onStateChanged()
        }
    }

    fun setFileNickname(deviceFile: DeviceFile, nickname: String?) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setFileNickname(deviceFile.uri.toString(), nickname)
            onStateChanged()
        }
    }

    fun getFileNickname(uri: String): String? {
        return userPreferences.getFileNickname(uri)
    }

    fun removeExcludedFile(deviceFile: DeviceFile) {
        val uriString = deviceFile.uri.toString()
        scope.launch(Dispatchers.IO) {
            userPreferences.removeExcludedFile(uriString)
            onStateChanged()
        }
    }

    fun clearAllExcludedFiles() {
        scope.launch(Dispatchers.IO) {
            userPreferences.clearAllExcludedFiles()
            onStateChanged()
        }
    }
}
