package com.tk.quicksearch.search.core

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.DeviceFile

/** File and directory handling intents. */
internal object FileIntents {
    private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

    /** Opens the folder containing the file, or the folder itself if it is a directory. */
    fun openContainingFolder(
        context: Application,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        if (deviceFile.isDirectory) {
            openDirectory(context, deviceFile, onShowToast)
        } else {
            openParentDirectory(context, deviceFile, onShowToast)
        }
    }

    /** Opens a file with appropriate app. */
    fun openFile(
        context: Application,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        if (deviceFile.isDirectory) {
            openDirectory(context, deviceFile, onShowToast)
            return
        }

        // APK Handling: Open containing folder using existing folder-opening logic
        if (isApk(deviceFile)) {
            openParentDirectory(context, deviceFile, onShowToast)
            return
        }

        val mimeType = deviceFile.mimeType ?: "*/*"

        val viewIntent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(deviceFile.uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        try {
            context.startActivity(viewIntent)
        } catch (exception: ActivityNotFoundException) {
            onShowToast?.invoke(R.string.common_error_unable_to_open, deviceFile.displayName)
        } catch (exception: SecurityException) {
            onShowToast?.invoke(R.string.common_error_unable_to_open, deviceFile.displayName)
        }
    }

    private fun isApk(deviceFile: DeviceFile): Boolean {
        val mime = deviceFile.mimeType?.lowercase(java.util.Locale.getDefault())
        if (mime == "application/vnd.android.package-archive") return true
        return deviceFile.displayName.lowercase(java.util.Locale.getDefault()).endsWith(".apk")
    }

    private fun openParentDirectory(
        context: Application,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val folderRelativePath =
            deviceFile.relativePath
                ?.trim()
                ?.trimStart('/')
                ?.trimEnd('/')
        if (folderRelativePath.isNullOrBlank()) {
            onShowToast?.invoke(R.string.common_error_unable_to_open, deviceFile.displayName)
            return
        }

        val folderName =
            folderRelativePath.substringAfterLast('/').takeIf { it.isNotBlank() }
                ?: run {
                    onShowToast?.invoke(R.string.common_error_unable_to_open, deviceFile.displayName)
                    return
                }

        val folderDeviceFile =
            DeviceFile(
                uri =
                    DocumentsContract.buildDocumentUri(
                        EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
                        "${toDocumentVolumeId(deviceFile.volumeName) ?: "primary"}:$folderRelativePath",
                    ),
                displayName = folderName,
                mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
                lastModified = deviceFile.lastModified,
                isDirectory = true,
                relativePath = folderRelativePath,
                volumeName = deviceFile.volumeName,
            )

        openDirectory(context, folderDeviceFile, onShowToast)
    }

    private fun openDirectory(
        context: Application,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val folderPath = buildFolderPath(deviceFile)

        // Try Samsung My Files first (if available)
        if (folderPath != null && trySamsungMyFiles(context, folderPath)) {
            return
        }

        // Fallback to original directory opening logic
        fallbackDirectoryOpening(context, deviceFile, onShowToast)
    }

    private fun trySamsungMyFiles(
        context: Application,
        folderPath: String,
    ): Boolean {
        val samsungIntent =
            Intent("samsung.myfiles.intent.action.LAUNCH_MY_FILES").apply {
                setComponent(
                    ComponentName(
                        "com.sec.android.app.myfiles",
                        "com.sec.android.app.myfiles.ui.MultiInstanceLaunchActivity",
                    ),
                )
                putExtra("samsung.myfiles.intent.extra.START_PATH", folderPath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        return try {
            context.startActivity(samsungIntent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun fallbackDirectoryOpening(
        context: Application,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        val documentsUri = buildDocumentsDirectoryUri(deviceFile)
        if (documentsUri != null) {
            val documentsIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(documentsUri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }

            try {
                context.startActivity(documentsIntent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }

        val primaryIntent =
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(deviceFile.uri, DocumentsContract.Document.MIME_TYPE_DIR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        try {
            context.startActivity(primaryIntent)
            return
        } catch (_: ActivityNotFoundException) {
        } catch (_: SecurityException) {
        }

        val fallbackIntent =
            Intent(Intent.ACTION_VIEW).apply {
                data = deviceFile.uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

        try {
            context.startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            onShowToast?.invoke(R.string.common_error_unable_to_open, deviceFile.displayName)
        } catch (_: SecurityException) {
            onShowToast?.invoke(R.string.common_error_unable_to_open, deviceFile.displayName)
        }
    }

    private fun buildDocumentsDirectoryUri(deviceFile: DeviceFile): Uri? {
        val relativePath =
            deviceFile.relativePath
                ?.trim()
                ?.trimStart('/')
                ?.trimEnd('/')
        val displayName = deviceFile.displayName.trim().trim('/')
        if (displayName.isBlank()) return null

        val basePath =
            when {
                relativePath.isNullOrBlank() -> displayName
                relativePath.endsWith(displayName, ignoreCase = true) -> relativePath
                else -> "$relativePath/$displayName"
            }

        val volumeId = toDocumentVolumeId(deviceFile.volumeName) ?: return null
        val documentId = "$volumeId:$basePath"
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY, documentId)
    }

    private fun toDocumentVolumeId(volumeName: String?): String? {
        if (volumeName.isNullOrBlank()) return "primary"
        return when (volumeName) {
            MediaStore.VOLUME_EXTERNAL_PRIMARY, "external_primary", "external" -> "primary"
            else -> volumeName
        }
    }

    private fun buildFolderPath(deviceFile: DeviceFile): String? {
        val relativePath =
            deviceFile.relativePath
                ?.trim()
                ?.trimStart('/')
                ?.trimEnd('/')
        val displayName = deviceFile.displayName.trim().trim('/')

        if (displayName.isBlank()) return null

        val basePath =
            when {
                relativePath.isNullOrBlank() -> displayName
                relativePath.endsWith(displayName, ignoreCase = true) -> relativePath
                else -> "$relativePath/$displayName"
            }

        // Convert to absolute path format expected by Samsung My Files
        return "/storage/emulated/0/$basePath"
    }
}