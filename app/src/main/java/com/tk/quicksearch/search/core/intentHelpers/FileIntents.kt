package com.tk.quicksearch.search.core.intentHelpers

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
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

    fun shareFile(
        context: Context,
        deviceFile: DeviceFile,
        onShowToast: ((Int, String?) -> Unit)? = null,
    ) {
        if (deviceFile.isDirectory) return

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = deviceFile.mimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, deviceFile.uri)
                clipData = ClipData.newUri(context.contentResolver, deviceFile.displayName, deviceFile.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        val chooserIntent = Intent.createChooser(shareIntent, null).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            context.startActivity(chooserIntent)
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
                        "${resolveDocumentVolumeId(context, deviceFile.volumeName)}:$folderRelativePath",
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
        val folderPath = buildFolderPath(context, deviceFile)
        // Try Samsung My Files first (if available). My Files silently shows its home screen when
        // handed a path it cannot resolve, so skip it whenever the path is provably absent. That
        // check is only trustworthy with all-files access; without it scoped storage reports false
        // for valid directories, so My Files is still attempted in that case.
        if (folderPath != null &&
            !isKnownMissingDirectory(folderPath) &&
            trySamsungMyFiles(context, folderPath)
        ) {
            return
        }

        // Fallback to original directory opening logic
        fallbackDirectoryOpening(context, deviceFile, onShowToast)
    }

    private fun trySamsungMyFiles(
        context: Application,
        folderPath: String,
    ): Boolean {
        val packageManager = context.packageManager
        try {
            packageManager.getPackageInfo("com.sec.android.app.myfiles", 0)
        } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
            return false
        }

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
            // Launched, not necessarily honored: My Files silently ignores an unusable START_PATH
            // and shows its home screen instead, which does not throw here.
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
        val documentsUri = buildDocumentsDirectoryUri(context, deviceFile)
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

    private fun buildDocumentsDirectoryUri(
        context: Context,
        deviceFile: DeviceFile,
    ): Uri? {
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

        val volumeId = resolveDocumentVolumeId(context, deviceFile.volumeName)
        val documentId = "$volumeId:$basePath"
        return DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY, documentId)
    }

    private fun isPrimaryVolume(volumeName: String?): Boolean {
        if (volumeName.isNullOrBlank()) return true
        return volumeName == MediaStore.VOLUME_EXTERNAL_PRIMARY ||
            volumeName == "external_primary" ||
            volumeName == "external"
    }

    /**
     * Document id volume for [volumeName]. MediaStore lowercases removable volume uuids, while
     * ExternalStorageProvider keeps the volume's own casing, so the uuid is looked up rather than
     * passed through.
     */
    private fun resolveDocumentVolumeId(
        context: Context,
        volumeName: String?,
    ): String {
        if (isPrimaryVolume(volumeName)) return "primary"
        // Keep the volume's own casing when it is known; adopted storage uses lowercase guids and
        // only the FAT-style fallback guess needs uppercasing.
        findStorageVolume(context, volumeName)?.uuid?.let { return it }
        return volumeName?.uppercase(java.util.Locale.US) ?: "primary"
    }

    private fun findStorageVolume(
        context: Context,
        volumeName: String?,
    ) = runCatching {
        context.getSystemService(StorageManager::class.java)
            ?.storageVolumes
            ?.firstOrNull { it.uuid?.equals(volumeName, ignoreCase = true) == true }
    }.getOrNull()

    private fun buildFolderPath(
        context: Context,
        deviceFile: DeviceFile,
    ): String? {
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

        // Files on removable volumes do not live under the primary storage root, so resolve the
        // mount point for this file's volume instead of assuming /storage/emulated/0.
        val volumeRoot = resolveVolumeRoot(context, deviceFile.volumeName) ?: return null

        return "$volumeRoot/$basePath"
    }

    /** Mount point for [volumeName], e.g. /storage/emulated/0 or /storage/8268-171A. */
    private fun resolveVolumeRoot(
        context: Context,
        volumeName: String?,
    ): String? {
        if (isPrimaryVolume(volumeName)) {
            return Environment.getExternalStorageDirectory().absolutePath
        }

        // MediaStore reports the volume uuid lowercased; the mount point keeps the original case.
        val storageVolume = findStorageVolume(context, volumeName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storageVolume?.directory?.absolutePath?.let { return it }
        }

        // Same casing rule as resolveDocumentVolumeId: trust a known uuid, uppercase only a guess.
        storageVolume?.uuid?.let { return "/storage/$it" }
        val uuid = volumeName ?: return null
        return "/storage/${uuid.uppercase(java.util.Locale.US)}"
    }

    /** Whether [java.io.File] checks on shared storage can be trusted. */
    private fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    /** True only when the path is provably absent; false when we cannot tell. */
    private fun isKnownMissingDirectory(path: String): Boolean =
        hasAllFilesAccess() && !java.io.File(path).isDirectory
}
