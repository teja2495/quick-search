package com.tk.quicksearch.search

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.ContactsContract
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import com.tk.quicksearch.model.ContactInfo
import com.tk.quicksearch.model.DeviceFile
import com.tk.quicksearch.search.buildSearchUrl

/**
 * Helper functions for creating and launching intents.
 * Centralizes intent creation logic to reduce duplication.
 */
object IntentHelpers {
    
    /**
     * Creates an intent with package URI and NEW_TASK flag.
     */
    private fun createPackageIntent(action: String, packageName: String): Intent {
        return Intent(action).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    
    /**
     * Opens usage access settings for the app.
     */
    fun openUsageAccessSettings(context: Application) {
        val intent = createPackageIntent(Settings.ACTION_USAGE_ACCESS_SETTINGS, context.packageName)
        context.startActivity(intent)
    }
    
    /**
     * Opens app settings for the app.
     */
    fun openAppSettings(context: Application) {
        val intent = createPackageIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, context.packageName)
        context.startActivity(intent)
    }
    
    /**
     * Opens app info settings for a specific package.
     */
    fun openAppInfo(context: Application, packageName: String) {
        val intent = createPackageIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageName)
        context.startActivity(intent)
    }
    
    /**
     * Opens all files access settings with fallback.
     */
    fun openAllFilesAccessSettings(context: Application) {
        val manageIntent = createPackageIntent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            context.packageName
        )
        runCatching {
            context.startActivity(manageIntent)
        }.onFailure {
            val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }
    
    /**
     * Launches an app by package name.
     */
    fun launchApp(context: Application, appInfo: AppInfo) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
        
        if (launchIntent == null) {
            Toast.makeText(
                context,
                context.getString(R.string.error_launch_app, appInfo.appName),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(launchIntent)
    }
    
    /**
     * Requests uninstall for an app.
     */
    fun requestUninstall(context: Application, appInfo: AppInfo) {
        val packageName = appInfo.packageName
        if (packageName == context.packageName) {
            Toast.makeText(
                context,
                context.getString(R.string.error_uninstall_self),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        try {
            val intent = createPackageIntent(Intent.ACTION_DELETE, packageName)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to uninstall ${appInfo.appName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Opens a search URL with the specified search engine.
     */
    fun openSearchUrl(context: Application, query: String, searchEngine: SearchEngine, amazonDomain: String? = null) {
        val searchUrl = buildSearchUrl(query, searchEngine, amazonDomain)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Opens a contact's details.
     */
    fun openContact(context: Application, contactInfo: ContactInfo) {
        val lookupUri = ContactsContract.Contacts.getLookupUri(contactInfo.contactId, contactInfo.lookupKey)
        if (lookupUri == null) {
            Toast.makeText(
                context,
                context.getString(R.string.error_open_contact),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, lookupUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Initiates a phone call.
     */
    fun performCall(context: Application, number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Opens SMS app for a phone number.
     */
    fun performSms(context: Application, number: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    
    /**
     * Opens WhatsApp chat for a phone number with fallback.
     */
    fun openWhatsAppChat(context: Application, phoneNumber: String) {
        if (phoneNumber.isBlank()) return
        
        val uri = Uri.parse("https://wa.me/${Uri.encode(phoneNumber)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
            } catch (inner: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.error_missing_phone_number),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    /**
     * Opens a file with appropriate app.
     */
    fun openFile(context: Application, deviceFile: DeviceFile) {
        val isApk = isApkFile(deviceFile)
        val mimeType = if (isApk) {
            "application/vnd.android.package-archive"
        } else {
            deviceFile.mimeType ?: "*/*"
        }
        
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(deviceFile.uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        try {
            context.startActivity(viewIntent)
        } catch (exception: ActivityNotFoundException) {
            if (isApk) {
                val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(deviceFile.uri, mimeType)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                try {
                    context.startActivity(installIntent)
                    return
                } catch (_: Exception) {
                    // Fall through to generic error toast
                }
            }
            showFileOpenError(context, deviceFile.displayName)
        } catch (exception: SecurityException) {
            showFileOpenError(context, deviceFile.displayName)
        }
    }
    
    private fun showFileOpenError(context: Application, fileName: String) {
        Toast.makeText(
            context,
            context.getString(R.string.error_open_file, fileName),
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * Best-effort detection of APK files.
     */
    private fun isApkFile(deviceFile: DeviceFile): Boolean {
        val mime = deviceFile.mimeType?.lowercase(java.util.Locale.getDefault())
        if (mime == "application/vnd.android.package-archive") {
            return true
        }
        val name = deviceFile.displayName.lowercase(java.util.Locale.getDefault())
        return name.endsWith(".apk")
    }
}
