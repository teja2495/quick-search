package com.tk.quicksearch.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.tk.quicksearch.R
import com.tk.quicksearch.model.SettingShortcut

/**
 * Provides a curated list of Settings shortcuts that can be surfaced in search.
 * Only shortcuts that resolve on the current device/SDK are returned.
 */
class SettingsShortcutRepository(private val context: Context) {

    private val packageManager = context.packageManager

    /**
     * Returns all supported and resolvable shortcuts for this device.
     */
    fun loadShortcuts(): List<SettingShortcut> {
        return allShortcuts()
            .filter { it.isSupported() }
            .filter { canResolve(it) }
    }

    /**
     * Builds an intent for the provided shortcut.
     */
    fun buildIntent(shortcut: SettingShortcut): Intent {
        return shortcut.toIntent(context)
    }

    private fun canResolve(shortcut: SettingShortcut): Boolean {
        val intent = shortcut.toIntent(context)
        return intent.resolveActivity(packageManager) != null
    }

    private fun allShortcuts(): List<SettingShortcut> {
        val packageUri = "package:{packageName}"
        val packageExtras = mapOf(
            Settings.EXTRA_APP_PACKAGE to "{packageName}",
            Intent.EXTRA_PACKAGE_NAME to "{packageName}"
        )

        return listOf(
            SettingShortcut(
                id = "system_settings",
                title = context.getString(R.string.settings_shortcut_system),
                description = "System & Device",
                keywords = listOf("android", "system", "main"),
                action = Settings.ACTION_SETTINGS
            ),
            SettingShortcut(
                id = "wifi",
                title = context.getString(R.string.settings_shortcut_wifi),
                description = "Network & Connectivity",
                keywords = listOf("wifi", "wi-fi", "internet", "network"),
                action = Settings.ACTION_WIFI_SETTINGS
            ),
            SettingShortcut(
                id = "bluetooth",
                title = context.getString(R.string.settings_shortcut_bluetooth),
                description = "Network & Connectivity",
                keywords = listOf("bt", "pair", "device"),
                action = Settings.ACTION_BLUETOOTH_SETTINGS
            ),
            SettingShortcut(
                id = "network",
                title = context.getString(R.string.settings_shortcut_network),
                description = "Network & Connectivity",
                keywords = listOf("network", "internet", "cellular", "carrier"),
                action = Settings.ACTION_WIRELESS_SETTINGS
            ),
            SettingShortcut(
                id = "mobile_data",
                title = context.getString(R.string.settings_shortcut_mobile_data),
                description = "Network & Connectivity",
                keywords = listOf("data", "roaming", "cellular"),
                action = Settings.ACTION_DATA_ROAMING_SETTINGS
            ),
            SettingShortcut(
                id = "airplane",
                title = context.getString(R.string.settings_shortcut_airplane),
                description = "Network & Connectivity",
                keywords = listOf("flight", "offline"),
                action = Settings.ACTION_AIRPLANE_MODE_SETTINGS
            ),
            SettingShortcut(
                id = "nfc",
                title = context.getString(R.string.settings_shortcut_nfc),
                description = "Network & Connectivity",
                keywords = listOf("tap", "pay", "contactless"),
                action = Settings.ACTION_NFC_SETTINGS
            ),
            SettingShortcut(
                id = "vpn",
                title = context.getString(R.string.settings_shortcut_vpn),
                description = "Network & Connectivity",
                keywords = listOf("vpn", "work", "tunnel"),
                action = Settings.ACTION_VPN_SETTINGS
            ),
            SettingShortcut(
                id = "location",
                title = context.getString(R.string.settings_shortcut_location),
                description = "System & Device",
                keywords = listOf("gps", "maps", "position"),
                action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
            ),
            SettingShortcut(
                id = "privacy",
                title = context.getString(R.string.settings_shortcut_privacy),
                description = "Security & Privacy",
                keywords = listOf("permissions", "data", "safety"),
                action = Settings.ACTION_PRIVACY_SETTINGS,
                minSdk = Build.VERSION_CODES.Q
            ),
            SettingShortcut(
                id = "security",
                title = context.getString(R.string.settings_shortcut_security),
                description = "Security & Privacy",
                keywords = listOf("lock", "screen", "device", "pin"),
                action = Settings.ACTION_SECURITY_SETTINGS
            ),
            SettingShortcut(
                id = "notifications",
                title = context.getString(R.string.settings_shortcut_notifications),
                description = "Notifications & Accessibility",
                keywords = listOf("alerts", "sounds", "banner"),
                action = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
                minSdk = Build.VERSION_CODES.M
            ),
            SettingShortcut(
                id = "app_notifications",
                title = context.getString(R.string.settings_shortcut_app_notifications),
                description = "Apps & Permissions",
                keywords = listOf("notifications", "this app", "alerts"),
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS,
                extras = packageExtras,
                minSdk = Build.VERSION_CODES.O
            ),
            SettingShortcut(
                id = "apps_list",
                title = context.getString(R.string.settings_shortcut_apps_list),
                description = "Apps & Permissions",
                keywords = listOf("manage", "permissions", "default"),
                action = Settings.ACTION_APPLICATION_SETTINGS
            ),
            SettingShortcut(
                id = "app_info",
                title = context.getString(R.string.settings_shortcut_app_info),
                description = "Apps & Permissions",
                keywords = listOf("details", "info", "manage"),
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                data = packageUri
            ),
            SettingShortcut(
                id = "display",
                title = context.getString(R.string.settings_shortcut_display),
                description = "System & Device",
                keywords = listOf("brightness", "dark", "font"),
                action = Settings.ACTION_DISPLAY_SETTINGS
            ),
            SettingShortcut(
                id = "sound",
                title = context.getString(R.string.settings_shortcut_sound),
                description = "System & Device",
                keywords = listOf("volume", "ringer", "vibration", "audio"),
                action = Settings.ACTION_SOUND_SETTINGS
            ),
            SettingShortcut(
                id = "storage",
                title = context.getString(R.string.settings_shortcut_storage),
                description = "System & Device",
                keywords = listOf("space", "files", "cleanup"),
                action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            ),
            SettingShortcut(
                id = "battery_saver",
                title = context.getString(R.string.settings_shortcut_battery_saver),
                description = "Battery & Performance",
                keywords = listOf("power", "low power", "battery"),
                action = Settings.ACTION_BATTERY_SAVER_SETTINGS,
                minSdk = Build.VERSION_CODES.LOLLIPOP
            ),
            SettingShortcut(
                id = "battery_optimization",
                title = context.getString(R.string.settings_shortcut_battery_optimization),
                description = "Battery & Performance",
                keywords = listOf("doze", "optimize", "ignore"),
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                minSdk = Build.VERSION_CODES.M
            ),
            SettingShortcut(
                id = "date_time",
                title = context.getString(R.string.settings_shortcut_date_time),
                description = "System & Device",
                keywords = listOf("clock", "timezone", "time zone"),
                action = Settings.ACTION_DATE_SETTINGS
            ),
            SettingShortcut(
                id = "language_input",
                title = context.getString(R.string.settings_shortcut_language_input),
                description = "Language & Input",
                keywords = listOf("keyboard", "ime", "typing"),
                action = Settings.ACTION_INPUT_METHOD_SETTINGS
            ),
            SettingShortcut(
                id = "locale",
                title = context.getString(R.string.settings_shortcut_locale),
                description = "Language & Input",
                keywords = listOf("language", "region", "locales"),
                action = Settings.ACTION_LOCALE_SETTINGS
            ),
            SettingShortcut(
                id = "developer_options",
                title = context.getString(R.string.settings_shortcut_developer_options),
                description = "Developer & Advanced",
                keywords = listOf("debug", "adb", "dev"),
                action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
            ),
            SettingShortcut(
                id = "about_phone",
                title = context.getString(R.string.settings_shortcut_about_phone),
                description = "System & Device",
                keywords = listOf("build", "version", "status"),
                action = Settings.ACTION_DEVICE_INFO_SETTINGS
            ),
            SettingShortcut(
                id = "accessibility",
                title = context.getString(R.string.settings_shortcut_accessibility),
                description = "Notifications & Accessibility",
                keywords = listOf("a11y", "talkback", "display size"),
                action = Settings.ACTION_ACCESSIBILITY_SETTINGS
            ),
            SettingShortcut(
                id = "overlay",
                title = context.getString(R.string.settings_shortcut_overlay_permission),
                description = "Apps & Permissions",
                keywords = listOf("draw over", "overlay", "bubble"),
                action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                data = packageUri,
                minSdk = Build.VERSION_CODES.M
            ),
            SettingShortcut(
                id = "write_settings",
                title = context.getString(R.string.settings_shortcut_write_settings),
                description = "Apps & Permissions",
                keywords = listOf("system settings", "modify", "write"),
                action = Settings.ACTION_MANAGE_WRITE_SETTINGS,
                data = packageUri,
                minSdk = Build.VERSION_CODES.M
            ),
            SettingShortcut(
                id = "all_files_access",
                title = context.getString(R.string.settings_shortcut_all_files),
                description = "Apps & Permissions",
                keywords = listOf("files", "storage", "manage"),
                action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                minSdk = Build.VERSION_CODES.R
            ),
            SettingShortcut(
                id = "unknown_sources",
                title = context.getString(R.string.settings_shortcut_unknown_sources),
                description = "Apps & Permissions",
                keywords = listOf("install", "apk", "sideload"),
                action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                data = packageUri,
                minSdk = Build.VERSION_CODES.O
            ),
            SettingShortcut(
                id = "notification_listener",
                title = context.getString(R.string.settings_shortcut_notification_listener),
                description = "Notifications & Accessibility",
                keywords = listOf("listener", "service", "notifications"),
                action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            ),
            SettingShortcut(
                id = "usage_access",
                title = context.getString(R.string.settings_shortcut_usage_access),
                description = "Apps & Permissions",
                keywords = listOf("usage", "stats", "permissions"),
                action = Settings.ACTION_USAGE_ACCESS_SETTINGS
            ),
            SettingShortcut(
                id = "home",
                title = context.getString(R.string.settings_shortcut_home),
                description = "Apps & Permissions",
                keywords = listOf("launcher", "default", "home app"),
                action = Settings.ACTION_HOME_SETTINGS
            ),
            SettingShortcut(
                id = "cast",
                title = context.getString(R.string.settings_shortcut_cast),
                description = "System & Device",
                keywords = listOf("chromecast", "display", "screen"),
                action = Settings.ACTION_CAST_SETTINGS
            ),
            SettingShortcut(
                id = "biometric",
                title = context.getString(R.string.settings_shortcut_biometric),
                description = "Security & Privacy",
                keywords = listOf("fingerprint", "face", "unlock"),
                action = Settings.ACTION_BIOMETRIC_ENROLL,
                minSdk = Build.VERSION_CODES.R
            ),
            SettingShortcut(
                id = "voice_input",
                title = context.getString(R.string.settings_shortcut_voice_input),
                description = "Language & Input",
                keywords = listOf("assistant", "speech", "voice"),
                action = Settings.ACTION_VOICE_INPUT_SETTINGS
            )
        )
    }
}

