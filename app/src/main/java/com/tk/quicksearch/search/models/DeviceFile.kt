package com.tk.quicksearch.search.models

import android.net.Uri

/**
 * File metadata returned from device-wide searches via MediaStore.
 */
data class DeviceFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String?,
    val lastModified: Long,
    val isDirectory: Boolean,
    val relativePath: String? = null,
    val volumeName: String? = null,
)
