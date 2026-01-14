package com.tk.quicksearch.search.deviceSettings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.tk.quicksearch.R

/**
 * Provides a curated list of Settings shortcuts that can be surfaced in search.
 * Only shortcuts that resolve on the current device/SDK are returned.
 */
class DeviceSettingsRepository(private val context: Context) {

    private val packageManager = context.packageManager

    fun loadShortcuts(): List<DeviceSetting> {
        return allShortcuts()
            .filter { it.isSupported() }
            .filter { canResolve(it) }
    }

    fun buildIntent(shortcut: DeviceSetting): Intent {
        return shortcut.toIntent(context)
    }

    private fun canResolve(shortcut: DeviceSetting): Boolean {
        val intent = shortcut.toIntent(context)
        return intent.resolveActivity(packageManager) != null
    }

    private fun allShortcuts(): List<DeviceSetting> {
        val packageUri = "package:{packageName}"
        val packageExtras = mapOf(
            Settings.EXTRA_APP_PACKAGE to "{packageName}",
            Intent.EXTRA_PACKAGE_NAME to "{packageName}"
        )

        return buildList {
            // System & Device shortcuts
            addSystemDeviceShortcuts()

            // Network & Connectivity shortcuts
            addNetworkConnectivityShortcuts()

            // Security & Privacy shortcuts
            addSecurityPrivacyShortcuts()

            // Apps & Permissions shortcuts
            addAppsPermissionsShortcuts(packageUri, packageExtras)

            // Battery & Performance shortcuts
            addBatteryPerformanceShortcuts()

            // Language & Input shortcuts
            addLanguageInputShortcuts()

            // Notifications & Accessibility shortcuts
            addNotificationsAccessibilityShortcuts(packageUri)

            // Developer & Advanced shortcuts
            addDeveloperShortcuts()
        }
    }

    private fun MutableList<DeviceSetting>.addSystemDeviceShortcuts() {
        add(createShortcut(
            id = "system_settings",
            titleRes = R.string.settings_shortcut_system,
            description = "System & Device",
            keywords = listOf("android", "system", "main"),
            action = Settings.ACTION_SETTINGS
        ))

        add(createShortcut(
            id = "location",
            titleRes = R.string.settings_shortcut_location,
            description = "System & Device",
            keywords = listOf("gps", "maps", "position"),
            action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
        ))

        add(createShortcut(
            id = "display",
            titleRes = R.string.settings_shortcut_display,
            description = "System & Device",
            keywords = listOf("brightness", "dark", "font"),
            action = Settings.ACTION_DISPLAY_SETTINGS
        ))

        add(createShortcut(
            id = "sound",
            titleRes = R.string.settings_shortcut_sound,
            description = "System & Device",
            keywords = listOf("volume", "ringer", "vibration", "audio"),
            action = Settings.ACTION_SOUND_SETTINGS
        ))

        add(createShortcut(
            id = "storage",
            titleRes = R.string.settings_shortcut_storage,
            description = "System & Device",
            keywords = listOf("space", "files", "cleanup"),
            action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS
        ))

        add(createShortcut(
            id = "date_time",
            titleRes = R.string.settings_shortcut_date_time,
            description = "System & Device",
            keywords = listOf("clock", "timezone", "time zone"),
            action = Settings.ACTION_DATE_SETTINGS
        ))

        add(createShortcut(
            id = "about_phone",
            titleRes = R.string.settings_shortcut_about_phone,
            description = "System & Device",
            keywords = listOf("build", "version", "status"),
            action = Settings.ACTION_DEVICE_INFO_SETTINGS
        ))

        add(createShortcut(
            id = "cast",
            titleRes = R.string.settings_shortcut_cast,
            description = "System & Device",
            keywords = listOf("chromecast", "display", "screen"),
            action = Settings.ACTION_CAST_SETTINGS
        ))
    }

    private fun MutableList<DeviceSetting>.addNetworkConnectivityShortcuts() {
        add(createShortcut(
            id = "wifi",
            titleRes = R.string.settings_shortcut_wifi,
            description = "Network & Connectivity",
            keywords = listOf("wifi", "wi-fi", "internet", "network"),
            action = Settings.ACTION_WIFI_SETTINGS
        ))

        add(createShortcut(
            id = "bluetooth",
            titleRes = R.string.settings_shortcut_bluetooth,
            description = "Network & Connectivity",
            keywords = listOf("bt", "pair", "device"),
            action = Settings.ACTION_BLUETOOTH_SETTINGS
        ))

        add(createShortcut(
            id = "network",
            titleRes = R.string.settings_shortcut_network,
            description = "Network & Connectivity",
            keywords = listOf("network", "internet", "cellular", "carrier"),
            action = Settings.ACTION_WIRELESS_SETTINGS
        ))

        add(createShortcut(
            id = "mobile_data",
            titleRes = R.string.settings_shortcut_mobile_data,
            description = "Network & Connectivity",
            keywords = listOf("data", "roaming", "cellular"),
            action = Settings.ACTION_DATA_ROAMING_SETTINGS
        ))

        add(createShortcut(
            id = "airplane",
            titleRes = R.string.settings_shortcut_airplane,
            description = "Network & Connectivity",
            keywords = listOf("flight", "offline"),
            action = Settings.ACTION_AIRPLANE_MODE_SETTINGS
        ))

        add(createShortcut(
            id = "nfc",
            titleRes = R.string.settings_shortcut_nfc,
            description = "Network & Connectivity",
            keywords = listOf("tap", "pay", "contactless"),
            action = Settings.ACTION_NFC_SETTINGS
        ))

        add(createShortcut(
            id = "vpn",
            titleRes = R.string.settings_shortcut_vpn,
            description = "Network & Connectivity",
            keywords = listOf("vpn", "work", "tunnel"),
            action = Settings.ACTION_VPN_SETTINGS
        ))
    }

    private fun MutableList<DeviceSetting>.addSecurityPrivacyShortcuts() {
        add(createShortcut(
            id = "privacy",
            titleRes = R.string.settings_shortcut_privacy,
            description = "Security & Privacy",
            keywords = listOf("permissions", "data", "safety"),
            action = Settings.ACTION_PRIVACY_SETTINGS,
            minSdk = Build.VERSION_CODES.Q
        ))

        add(createShortcut(
            id = "security",
            titleRes = R.string.settings_shortcut_security,
            description = "Security & Privacy",
            keywords = listOf("lock", "screen", "device", "pin"),
            action = Settings.ACTION_SECURITY_SETTINGS
        ))

        add(createShortcut(
            id = "biometric",
            titleRes = R.string.settings_shortcut_biometric,
            description = "Security & Privacy",
            keywords = listOf("fingerprint", "face", "unlock"),
            action = Settings.ACTION_BIOMETRIC_ENROLL,
            minSdk = Build.VERSION_CODES.R
        ))
    }

    private fun MutableList<DeviceSetting>.addAppsPermissionsShortcuts(
        packageUri: String,
        packageExtras: Map<String, String>
    ) {
        add(createShortcut(
            id = "app_notifications",
            titleRes = R.string.settings_shortcut_app_notifications,
            description = "Apps & Permissions",
            keywords = listOf("notifications", "this app", "alerts"),
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            extras = packageExtras,
            minSdk = Build.VERSION_CODES.O
        ))

        add(createShortcut(
            id = "apps_list",
            titleRes = R.string.settings_shortcut_apps_list,
            description = "Apps & Permissions",
            keywords = listOf("manage", "permissions", "default"),
            action = Settings.ACTION_APPLICATION_SETTINGS
        ))

        add(createShortcut(
            id = "app_info",
            titleRes = R.string.settings_shortcut_app_info,
            description = "Apps & Permissions",
            keywords = listOf("details", "info", "manage"),
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            data = packageUri
        ))

        add(createShortcut(
            id = "overlay",
            titleRes = R.string.settings_shortcut_overlay_permission,
            description = "Apps & Permissions",
            keywords = listOf("draw over", "overlay", "bubble"),
            action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            data = packageUri,
            minSdk = Build.VERSION_CODES.M
        ))

        add(createShortcut(
            id = "write_settings",
            titleRes = R.string.settings_shortcut_write_settings,
            description = "Apps & Permissions",
            keywords = listOf("system settings", "modify", "write"),
            action = Settings.ACTION_MANAGE_WRITE_SETTINGS,
            data = packageUri,
            minSdk = Build.VERSION_CODES.M
        ))

        add(createShortcut(
            id = "all_files_access",
            titleRes = R.string.settings_shortcut_all_files,
            description = "Apps & Permissions",
            keywords = listOf("files", "storage", "manage"),
            action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
            minSdk = Build.VERSION_CODES.R
        ))

        add(createShortcut(
            id = "unknown_sources",
            titleRes = R.string.settings_shortcut_unknown_sources,
            description = "Apps & Permissions",
            keywords = listOf("install", "apk", "sideload"),
            action = Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            data = packageUri,
            minSdk = Build.VERSION_CODES.O
        ))

        add(createShortcut(
            id = "usage_access",
            titleRes = R.string.settings_shortcut_usage_access,
            description = "Apps & Permissions",
            keywords = listOf("usage", "stats", "permissions"),
            action = Settings.ACTION_USAGE_ACCESS_SETTINGS
        ))

        add(createShortcut(
            id = "home",
            titleRes = R.string.settings_shortcut_home,
            description = "Apps & Permissions",
            keywords = listOf("launcher", "default", "home app"),
            action = Settings.ACTION_HOME_SETTINGS
        ))
    }

    private fun MutableList<DeviceSetting>.addBatteryPerformanceShortcuts() {
        add(createShortcut(
            id = "battery_saver",
            titleRes = R.string.settings_shortcut_battery_saver,
            description = "Battery & Performance",
            keywords = listOf("power", "low power", "battery"),
            action = Settings.ACTION_BATTERY_SAVER_SETTINGS,
            minSdk = Build.VERSION_CODES.LOLLIPOP
        ))

        add(createShortcut(
            id = "battery_optimization",
            titleRes = R.string.settings_shortcut_battery_optimization,
            description = "Battery & Performance",
            keywords = listOf("doze", "optimize", "ignore"),
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            minSdk = Build.VERSION_CODES.M
        ))
    }

    private fun MutableList<DeviceSetting>.addLanguageInputShortcuts() {
        add(createShortcut(
            id = "language_input",
            titleRes = R.string.settings_shortcut_language_input,
            description = "Language & Input",
            keywords = listOf("keyboard", "ime", "typing"),
            action = Settings.ACTION_INPUT_METHOD_SETTINGS
        ))

        add(createShortcut(
            id = "locale",
            titleRes = R.string.settings_shortcut_locale,
            description = "Language & Input",
            keywords = listOf("language", "region", "locales"),
            action = Settings.ACTION_LOCALE_SETTINGS
        ))

        add(createShortcut(
            id = "voice_input",
            titleRes = R.string.settings_shortcut_voice_input,
            description = "Language & Input",
            keywords = listOf("assistant", "speech", "voice"),
            action = Settings.ACTION_VOICE_INPUT_SETTINGS
        ))
    }

    private fun MutableList<DeviceSetting>.addNotificationsAccessibilityShortcuts(packageUri: String) {
        add(createShortcut(
            id = "notifications",
            titleRes = R.string.settings_shortcut_notifications,
            description = "Notifications & Accessibility",
            keywords = listOf("alerts", "sounds", "banner"),
            action = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
            minSdk = Build.VERSION_CODES.M
        ))

        add(createShortcut(
            id = "accessibility",
            titleRes = R.string.settings_shortcut_accessibility,
            description = "Notifications & Accessibility",
            keywords = listOf("a11y", "talkback", "display size"),
            action = Settings.ACTION_ACCESSIBILITY_SETTINGS
        ))

        add(createShortcut(
            id = "notification_listener",
            titleRes = R.string.settings_shortcut_notification_listener,
            description = "Notifications & Accessibility",
            keywords = listOf("listener", "service", "notifications"),
            action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        ))
    }

    private fun MutableList<DeviceSetting>.addDeveloperShortcuts() {
        add(createShortcut(
            id = "developer_options",
            titleRes = R.string.settings_shortcut_developer_options,
            description = "Developer & Advanced",
            keywords = listOf("debug", "adb", "dev"),
            action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
        ))
    }

    private fun createShortcut(
        id: String,
        titleRes: Int,
        description: String,
        keywords: List<String>,
        action: String,
        data: String? = null,
        extras: Map<String, Any>? = null,
        minSdk: Int = Build.VERSION_CODES.BASE
    ): DeviceSetting {
        return DeviceSetting(
            id = id,
            title = context.getString(titleRes),
            description = description,
            keywords = keywords,
            action = action,
            data = data,
            extras = extras.orEmpty(),
            minSdk = minSdk
        )
    }
}