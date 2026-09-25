package com.tk.quicksearch.app.navigation

import android.content.Context
import android.os.Environment
import java.io.File

internal fun findQuicksearchFilesOnDevice(context: Context): List<File> {
    val searchDirs = listOfNotNull(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
        context.getExternalFilesDir(null),
    )
    return searchDirs
        .filter { it.exists() && it.canRead() }
        .flatMap { dir ->
            dir.walkTopDown()
                .maxDepth(3)
                .filter { it.isFile && it.extension == "quicksearch" }
                .toList()
        }
        .distinctBy { it.absolutePath }
}

internal fun hasFilesPermission(context: Context): Boolean =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
