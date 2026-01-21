package com.tk.quicksearch.search.core

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.searchEngines.buildSearchUrl
import com.tk.quicksearch.search.searchEngines.getDisplayNameResId
import com.tk.quicksearch.util.PackageConstants

/** Helper functions for creating and launching intents. */
object IntentHelpers {

    private const val EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY = "com.android.externalstorage.documents"

    /** Creates an intent with package URI and NEW_TASK flag. */
    private fun createPackageIntent(action: String, packageName: String): Intent {
        return Intent(action).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** Opens usage access settings for the app. */
    fun openUsageAccessSettings(context: Application) {
        val intent = createPackageIntent(Settings.ACTION_USAGE_ACCESS_SETTINGS, context.packageName)
        context.startActivity(intent)
    }

    /** Opens app settings for the app. */
    fun openAppSettings(context: Application) {
        val intent =
                createPackageIntent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        context.packageName
                )
        context.startActivity(intent)
    }

    /** Opens app info settings for a specific package. */
    fun openAppInfo(context: Application, packageName: String) {
        val intent = createPackageIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageName)
        context.startActivity(intent)
    }

    /** Opens all files access settings with fallback. */
    fun openAllFilesAccessSettings(context: Application) {
        val manageIntent =
                createPackageIntent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        context.packageName
                )
        runCatching { context.startActivity(manageIntent) }.onFailure {
            val fallback =
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            context.startActivity(fallback)
        }
    }

    /** Launches an app by package name. */
    fun launchApp(
            context: Application,
            appInfo: AppInfo,
            onShowToast: ((Int, String?) -> Unit)? = null
    ) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)

        if (launchIntent == null) {
            onShowToast?.invoke(R.string.error_launch_app, appInfo.appName)
            return
        }

        launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        context.startActivity(launchIntent)
    }

    /** Requests uninstall for an app. */
    fun requestUninstall(
            context: Application,
            appInfo: AppInfo,
            onShowToast: ((Int, String?) -> Unit)? = null
    ) {
        val packageName = appInfo.packageName
        if (packageName == context.packageName) {
            onShowToast?.invoke(R.string.error_uninstall_self, null)
            return
        }

        try {
            val intent = createPackageIntent(Intent.ACTION_DELETE, packageName)
            context.startActivity(intent)
        } catch (e: Exception) {
            onShowToast?.invoke(R.string.error_uninstall_app, appInfo.appName)
        }
    }

    /** Opens a search URL with the specified search engine. */
    fun openSearchUrl(
            context: Application,
            query: String,
            searchEngine: SearchEngine,
            amazonDomain: String? = null,
            onShowToast: ((Int, String?) -> Unit)? = null
    ) {
        // Handle apps with native integrations
        when (searchEngine) {
            SearchEngine.GEMINI -> {
                openGemini(context, query)
                return
            }
            SearchEngine.GOOGLE_PHOTOS -> {
                openGooglePhotos(context, query)
                return
            }
            SearchEngine.YOU_COM -> {
                openYouCom(context, query)
                return
            }
            SearchEngine.STARTPAGE -> {
                openStartpage(context, query)
                return
            }
            SearchEngine.SPOTIFY -> {
                openSpotify(context, query)
                return
            }
            else -> {} // Continue to web URL
        }

        val searchUrl = buildSearchUrl(query, searchEngine, amazonDomain)
        val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            onShowToast?.invoke(
                    R.string.error_open_search_engine,
                    context.getString(searchEngine.getDisplayNameResId())
            )
        } catch (exception: SecurityException) {
            onShowToast?.invoke(
                    R.string.error_open_search_engine,
                    context.getString(searchEngine.getDisplayNameResId())
            )
        }
    }

    fun openBrowserSearch(
            context: Application,
            query: String,
            browserPackageName: String,
            onShowToast: ((Int, String?) -> Unit)? = null
    ) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(browserPackageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: ActivityNotFoundException) {} catch (_: SecurityException) {}
            }
            onShowToast?.invoke(R.string.error_open_search_engine, null)
            return
        }

        val searchUrl = buildSearchUrl(trimmedQuery, SearchEngine.GOOGLE)
        val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
                    setPackage(browserPackageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            onShowToast?.invoke(R.string.error_open_search_engine, null)
        } catch (exception: SecurityException) {
            onShowToast?.invoke(R.string.error_open_search_engine, null)
        }
    }

    /**
     * Opens the Gemini app with the query using a share intent. If query is empty, just launches
     * the app.
     *
     * Uses ACTION_SEND intent to com.google.android.apps.bard which reliably pre-fills the query.
     * Assumes Gemini app is installed.
     */
    private fun openGemini(context: Application, query: String) {
        // If query is blank, just open the app
        if (query.isBlank()) {
            val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(
                            PackageConstants.GEMINI_PACKAGE_NAME
                    )
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: ActivityNotFoundException) {
                    Log.w("GeminiLaunch", "Failed to launch Gemini app: ${e.message}")
                } catch (e: SecurityException) {
                    Log.w("GeminiLaunch", "Security exception launching Gemini: ${e.message}")
                }
            }
            Log.e("GeminiLaunch", "Failed to open Gemini app")
            return
        }

        // If query is not blank, use share intent to pre-fill it
        val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, query)
                    setPackage(PackageConstants.GEMINI_PACKAGE_NAME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        if (shareIntent.resolveActivity(context.packageManager) != null) {
            try {
                context.startActivity(shareIntent)
                return
            } catch (e: ActivityNotFoundException) {
                Log.w("GeminiLaunch", "Share intent failed: ${e.message}")
            } catch (e: SecurityException) {
                Log.w("GeminiLaunch", "Share intent security exception: ${e.message}")
            }
        } else {
            Log.w("GeminiLaunch", "Gemini app not resolved - may not be installed")
        }

        // If share intent fails, just log it (no user-facing error)
        Log.e("GeminiLaunch", "Failed to open Gemini app with query")
    }

    /** Opens Google Photos app if installed, otherwise opens web URL. */
    private fun openGooglePhotos(context: Application, query: String) {
        // Check if app is installed
        val launchIntent =
                context.packageManager.getLaunchIntentForPackage(
                        PackageConstants.GOOGLE_PHOTOS_PACKAGE_NAME
                )

        if (launchIntent != null) {
            // App is installed
            if (query.isBlank()) {
                // Just open the app
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: Exception) {
                    Log.w("GooglePhotosLaunch", "Failed to launch Google Photos: ${e.message}")
                }
            } else {
                // Try to open with search (using web URL as Photos app doesn't have a direct search
                // intent)
                openWebUrl(context, buildSearchUrl(query, SearchEngine.GOOGLE_PHOTOS))
                return
            }
        }

        // Fallback to web URL
        openWebUrl(context, buildSearchUrl(query, SearchEngine.GOOGLE_PHOTOS))
    }

    /** Opens You.com app if installed, otherwise opens web URL. */
    private fun openYouCom(context: Application, query: String) {
        // Check if app is installed
        val launchIntent =
                context.packageManager.getLaunchIntentForPackage(
                        PackageConstants.YOU_COM_PACKAGE_NAME
                )

        if (launchIntent != null) {
            // App is installed
            if (query.isBlank()) {
                // Just open the app
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: Exception) {
                    Log.w("YouComLaunch", "Failed to launch You.com: ${e.message}")
                }
            } else {
                // Try to open with search (using web URL)
                openWebUrl(context, buildSearchUrl(query, SearchEngine.YOU_COM))
                return
            }
        }

        // Fallback to web URL
        openWebUrl(context, buildSearchUrl(query, SearchEngine.YOU_COM))
    }

    /** Opens Startpage app if installed, otherwise opens web URL. */
    private fun openStartpage(context: Application, query: String) {
        // Check if app is installed
        val launchIntent =
                context.packageManager.getLaunchIntentForPackage(
                        PackageConstants.STARTPAGE_PACKAGE_NAME
                )

        if (launchIntent != null) {
            // App is installed
            if (query.isBlank()) {
                // Just open the app
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: Exception) {
                    Log.w("StartpageLaunch", "Failed to launch Startpage: ${e.message}")
                }
            } else {
                // Try to open with search (using web URL)
                openWebUrl(context, buildSearchUrl(query, SearchEngine.STARTPAGE))
                return
            }
        }

        // Fallback to web URL
        openWebUrl(context, buildSearchUrl(query, SearchEngine.STARTPAGE))
    }

    /** Opens Spotify app if installed, otherwise opens web URL. */
    private fun openSpotify(context: Application, query: String) {
        // Check if app is installed
        val launchIntent =
                context.packageManager.getLaunchIntentForPackage(
                        PackageConstants.SPOTIFY_PACKAGE
                )

        if (launchIntent != null) {
            // App is installed
            if (query.isBlank()) {
                // Just open the app
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (e: Exception) {
                    Log.w("SpotifyLaunch", "Failed to launch Spotify: ${e.message}")
                }
            } else {
                // Try to open with search (using web URL)
                openWebUrl(context, buildSearchUrl(query, SearchEngine.SPOTIFY))
                return
            }
        }

        // Fallback to web URL
        openWebUrl(context, buildSearchUrl(query, SearchEngine.SPOTIFY))
    }

    /** Opens a web URL in a browser. */
    private fun openWebUrl(context: Application, url: String) {
        val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w("OpenWebUrl", "Failed to open URL: ${e.message}")
        } catch (e: SecurityException) {
            Log.w("OpenWebUrl", "Security exception opening URL: ${e.message}")
        }
    }

    /** Opens a file with appropriate app. */
    fun openFile(
            context: Application,
            deviceFile: DeviceFile,
            onShowToast: ((Int, String?) -> Unit)? = null
    ) {
        if (deviceFile.isDirectory) {
            openDirectory(context, deviceFile, onShowToast)
            return
        }

        // APK Handling: Open containing folder instead of launching the APK
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
            onShowToast?.invoke(R.string.error_open_file, deviceFile.displayName)
        } catch (exception: SecurityException) {
            onShowToast?.invoke(R.string.error_open_file, deviceFile.displayName)
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
            onShowToast: ((Int, String?) -> Unit)? = null
    ) {
        // relativePath typically ends with / and is the folder path
        val relativePath = deviceFile.relativePath?.trim()?.trimStart('/')?.trimEnd('/')

        if (relativePath.isNullOrBlank()) {
            // If we can't determine parent, try to fallback to normal open (which might fail or do
            // nothing desired)
            // or show error.
            onShowToast?.invoke(R.string.error_open_file, deviceFile.displayName)
            return
        }

        // Try to open via DocumentsContract
        val volumeId = toDocumentVolumeId(deviceFile.volumeName)
        if (volumeId != null) {
            val documentId = "$volumeId:$relativePath"
            val documentsUri =
                    DocumentsContract.buildDocumentUri(
                            EXTERNAL_STORAGE_DOCUMENTS_AUTHORITY,
                            documentId
                    )

            val documentsIntent =
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(documentsUri, DocumentsContract.Document.MIME_TYPE_DIR)
                        addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

            try {
                context.startActivity(documentsIntent)
                return
            } catch (_: Exception) {
                // Continue to fallback
            }
        }

        // Fallback: If we can't construct the document URI, we can't easily open the specific
        // folder
        // without a proper URI.
        // We could try to open the file URI as a directory, but that won't work for a file URI.
        // So we default to error toast if we couldn't open the folder.
        onShowToast?.invoke(R.string.error_open_file, deviceFile.displayName)
    }

    private fun openDirectory(
            context: Application,
            deviceFile: DeviceFile,
            onShowToast: ((Int, String?) -> Unit)? = null
    ) {
        val documentsUri = buildDocumentsDirectoryUri(deviceFile)
        if (documentsUri != null) {
            val documentsIntent =
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(documentsUri, DocumentsContract.Document.MIME_TYPE_DIR)
                        addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

            try {
                context.startActivity(documentsIntent)
                return
            } catch (_: ActivityNotFoundException) {} catch (_: SecurityException) {}
        }

        val primaryIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(deviceFile.uri, DocumentsContract.Document.MIME_TYPE_DIR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

        try {
            context.startActivity(primaryIntent)
            return
        } catch (_: ActivityNotFoundException) {} catch (_: SecurityException) {}

        val fallbackIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    data = deviceFile.uri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

        try {
            context.startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            onShowToast?.invoke(R.string.error_open_file, deviceFile.displayName)
        } catch (_: SecurityException) {
            onShowToast?.invoke(R.string.error_open_file, deviceFile.displayName)
        }
    }

    private fun buildDocumentsDirectoryUri(deviceFile: DeviceFile): Uri? {
        val relativePath = deviceFile.relativePath?.trim()?.trimStart('/')?.trimEnd('/')
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
}
