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
        // Update UI immediately (optimistic)
        onUiStateUpdate { state ->
            if (state.pinnedFiles.any { it.uri.toString() == uriString }) {
                state
            } else {
                state.copy(pinnedFiles = state.pinnedFiles + deviceFile)
            }
        }
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

    fun excludeFileExtension(deviceFile: DeviceFile): Set<String> {
        val extension = FileUtils.getFileExtension(deviceFile.displayName)
        return if (extension != null) {
            val updatedExtensions = userPreferences.addExcludedFileExtension(extension)
            scope.launch(Dispatchers.IO) {
                onUiStateUpdate { state ->
                    state.copy(
                        fileResults = state.fileResults.filterNot {
                            FileUtils.isFileExtensionExcluded(it.displayName, userPreferences.getExcludedFileExtensions())
                        }
                    )
                }
                onStateChanged()
            }
            updatedExtensions
        } else {
            userPreferences.getExcludedFileExtensions()
        }
    }

    fun removeExcludedFileExtension(extension: String): Set<String> {
        val updatedExtensions = userPreferences.removeExcludedFileExtension(extension)
        scope.launch(Dispatchers.IO) {
            // Re-run search to include previously excluded files with this extension
            onStateChanged()
        }
        return updatedExtensions
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

        // Update UI immediately (optimistic)
        onUiStateUpdate { state ->
            state.copy(
                excludedFiles = state.excludedFiles.filterNot { it.uri.toString() == uriString }
            )
        }

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
