package com.tk.quicksearch.search.common

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.widget.customButtons.CustomWidgetButtonAction
import com.tk.quicksearch.widget.customButtons.QuickSearchWidgetActionActivity

class AddToHomeHandler(private val context: Context) {

    private val shortcutManager: ShortcutManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.getSystemService(context, ShortcutManager::class.java)
        } else {
            null
        }
    }

    fun isSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            shortcutManager?.isRequestPinShortcutSupported == true
        } else {
            false
        }
    }

    fun addAppToHome(appInfo: AppInfo) {
        if (!isSupported()) {
            showUnsupportedMessage()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (intent == null) {
                showErrorMessage()
                return
            }

            val builder =
                    ShortcutInfo.Builder(context, "app_${appInfo.packageName}")
                            .setShortLabel(appInfo.appName)
                            .setLongLabel(appInfo.appName)
                            .setIntent(intent)

            loadAppIcon(appInfo.packageName)?.let { builder.setIcon(it) }

            shortcutManager?.requestPinShortcut(builder.build(), null)
        }
    }

    fun addAppShortcutToHome(shortcut: StaticShortcut) {
        if (!isSupported()) {
            showUnsupportedMessage()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder =
                    ShortcutInfo.Builder(context, "shortcut_${shortcut.id}")
                            .setShortLabel(shortcut.shortLabel ?: shortcut.appLabel)
                            .setLongLabel(shortcut.longLabel ?: shortcut.appLabel)
                            .setIntents(shortcut.intents.toTypedArray())

            val icon = loadShortcutIcon(shortcut) ?: loadAppIcon(shortcut.packageName)
            icon?.let { builder.setIcon(it) }

            shortcutManager?.requestPinShortcut(builder.build(), null)
        }
    }

    fun addContactToHome(contact: ContactInfo) {
        if (!isSupported()) {
            showUnsupportedMessage()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val contactAction =
                    CustomWidgetButtonAction.Contact(
                            contactId = contact.contactId,
                            lookupKey = contact.lookupKey,
                            displayName = contact.displayName,
                            photoUri = contact.photoUri,
                    )

            // Open QuickSearchWidgetActionActivity which shows the contact methods dialog,
            // matching the behavior of widget contact buttons.
            val intent =
                    Intent(context, QuickSearchWidgetActionActivity::class.java).apply {
                        action = Intent.ACTION_VIEW // Required: ShortcutInfo intents cannot have
                        // null action
                        putExtra(
                                QuickSearchWidgetActionActivity.EXTRA_CUSTOM_BUTTON_ACTION,
                                contactAction.toJson()
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

            val builder =
                    ShortcutInfo.Builder(context, "contact_${contact.contactId}")
                            .setShortLabel(contact.displayName)
                            .setLongLabel(contact.displayName)
                            .setIntent(intent)
                            .setIcon(loadContactIcon(contact))

            shortcutManager?.requestPinShortcut(builder.build(), null)
        }
    }

    fun addFileToHome(file: DeviceFile) {
        if (!isSupported()) {
            showUnsupportedMessage()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val fileAction =
                    CustomWidgetButtonAction.File(
                            uri = file.uri.toString(),
                            displayName = file.displayName,
                            mimeType = file.mimeType,
                            lastModified = file.lastModified,
                            isDirectory = file.isDirectory,
                            relativePath = file.relativePath,
                            volumeName = file.volumeName,
                    )
            val intent =
                    Intent(context, QuickSearchWidgetActionActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(
                                QuickSearchWidgetActionActivity.EXTRA_CUSTOM_BUTTON_ACTION,
                                fileAction.toJson()
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

            val icon =
                    if (file.isDirectory) createFileTypeIcon(isFolderIcon = true)
                    else createFileTypeIcon(isFolderIcon = false)

            val shortcutInfo =
                    ShortcutInfo.Builder(context, "file_${file.uri.hashCode()}")
                            .setShortLabel(file.displayName)
                            .setLongLabel(file.displayName)
                            .setIntent(intent)
                            .setIcon(icon)
                            .build()

            shortcutManager?.requestPinShortcut(shortcutInfo, null)
        }
    }

    fun addDeviceSettingToHome(setting: DeviceSetting) {
        if (!isSupported()) {
            showUnsupportedMessage()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(setting.action)

            val icon =
                    loadAppIcon("com.android.settings")
                            ?: Icon.createWithResource(context, R.drawable.ic_settings_shortcut)

            val shortcutInfo =
                    ShortcutInfo.Builder(context, "setting_${setting.id}")
                            .setShortLabel(setting.title)
                            .setLongLabel(setting.title)
                            .setIntent(intent)
                            .setIcon(icon)
                            .build()

            shortcutManager?.requestPinShortcut(shortcutInfo, null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadAppIcon(packageName: String): Icon? =
            runCatching {
                        val drawable = context.packageManager.getApplicationIcon(packageName)
                        Icon.createWithBitmap(drawable.toBitmap())
                    }
                    .getOrNull()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadShortcutIcon(shortcut: StaticShortcut): Icon? {
        // Try base64 encoded icon first (custom shortcuts)
        shortcut.iconBase64?.let { encoded ->
            val decoded = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
            val bitmap = decoded?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            if (bitmap != null) {
                return runCatching { Icon.createWithBitmap(bitmap) }.getOrNull()
            }
        }

        // Try resource icon from the shortcut's package
        val resId = shortcut.iconResId ?: return null
        val targetContext =
                runCatching { context.createPackageContext(shortcut.packageName, 0) }.getOrNull()
                        ?: return null

        return runCatching {
                    val drawable = targetContext.resources.getDrawable(resId, targetContext.theme)
                    Icon.createWithBitmap(drawable.toBitmap())
                }
                .getOrNull()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadContactIcon(contact: ContactInfo): Icon {
        val photoUri = contact.photoUri
        if (!photoUri.isNullOrBlank()) {
            val bitmap =
                    runCatching {
                                val uri = Uri.parse(photoUri)
                                context.contentResolver.openInputStream(uri)?.use {
                                    BitmapFactory.decodeStream(it)
                                }
                            }
                            .getOrNull()
            if (bitmap != null) {
                return Icon.createWithAdaptiveBitmap(bitmap)
            }
        }
        return Icon.createWithResource(context, R.drawable.ic_contact_person)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createFileTypeIcon(isFolderIcon: Boolean): Icon {
        @DrawableRes
        val resId = if (isFolderIcon) R.drawable.ic_shortcut_folder else R.drawable.ic_shortcut_file
        return Icon.createWithResource(context, resId)
    }

    private fun showUnsupportedMessage() {
        Toast.makeText(
                        context,
                        "Adding shortcuts to home screen is not supported on this device",
                        Toast.LENGTH_SHORT,
                )
                .show()
    }

    private fun showErrorMessage() {
        Toast.makeText(context, "Failed to create shortcut", Toast.LENGTH_SHORT).show()
    }
}
