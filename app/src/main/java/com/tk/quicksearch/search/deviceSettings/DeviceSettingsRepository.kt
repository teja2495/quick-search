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
class DeviceSettingsRepository(
    private val context: Context,
) {
    private val packageManager = context.packageManager

    fun loadShortcuts(): List<DeviceSetting> =
        allShortcuts()
            .filter { it.isSupported() }
            .filter { canResolve(it) }

    fun buildIntent(shortcut: DeviceSetting): Intent = shortcut.toIntent(context)

    private fun canResolve(shortcut: DeviceSetting): Boolean {
        val intent = shortcut.toIntent(context)
        return intent.resolveActivity(packageManager) != null
    }

    private fun allShortcuts(): List<DeviceSetting> {
        val packageUri = "package:{packageName}"

        return buildList {
            // System & Device shortcuts
            addSystemDeviceShortcuts()

            // Network & Connectivity shortcuts
            addNetworkConnectivityShortcuts()

            // Security & Privacy shortcuts
            addSecurityPrivacyShortcuts()

            // Apps & Permissions shortcuts
            addAppsPermissionsShortcuts(packageUri)

            // Battery & Performance shortcuts
            addBatteryPerformanceShortcuts()

            // Language & Input shortcuts
            addLanguageInputShortcuts()

            // Notifications & Accessibility shortcuts
            addNotificationsAccessibilityShortcuts()

            // Developer & Advanced shortcuts
            addDeveloperShortcuts()
        }
    }

    private fun MutableList<DeviceSetting>.addSystemDeviceShortcuts() {
        add(
            createShortcut(
                id = "location",
                titleRes = R.string.settings_shortcut_location,
                descriptionRes = R.string.device_settings_category_system_device,
                keywords = listOf("gps", "maps", "position"),
                action = Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "display",
                titleRes = R.string.settings_shortcut_display,
                descriptionRes = R.string.device_settings_category_system_device,
                keywords = listOf("brightness", "dark", "font"),
                action = Settings.ACTION_DISPLAY_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "sound",
                titleRes = R.string.settings_shortcut_sound,
                descriptionRes = R.string.device_settings_category_system_device,
                keywords = listOf("volume", "ringer", "vibration", "audio"),
                action = Settings.ACTION_SOUND_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "storage",
                titleRes = R.string.settings_shortcut_storage,
                descriptionRes = R.string.device_settings_category_system_device,
                keywords = listOf("space", "files", "cleanup"),
                action = Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "date_time",
                titleRes = R.string.settings_shortcut_date_time,
                descriptionRes = R.string.device_settings_category_system_device,
                keywords = listOf("clock", "timezone", "time zone"),
                action = Settings.ACTION_DATE_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "about_phone",
                titleRes = R.string.settings_shortcut_about_phone,
                descriptionRes = R.string.device_settings_category_system_device,
                keywords = listOf("build", "version", "status"),
                action = Settings.ACTION_DEVICE_INFO_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "cast",
                titleRes = R.string.settings_shortcut_cast,
                descriptionRes = R.string.device_settings_category_system_device,
                keywords = listOf("chromecast", "display", "screen"),
                action = Settings.ACTION_CAST_SETTINGS,
            ),
        )
    }

    private fun MutableList<DeviceSetting>.addNetworkConnectivityShortcuts() {
        add(
            createShortcut(
                id = "wifi",
                titleRes = R.string.settings_shortcut_wifi,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("wifi", "wi-fi", "internet", "network"),
                action = Settings.ACTION_WIFI_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "bluetooth",
                titleRes = R.string.settings_shortcut_bluetooth,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("bt", "pair", "device"),
                action = Settings.ACTION_BLUETOOTH_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "network",
                titleRes = R.string.settings_shortcut_network,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("network", "internet", "cellular", "carrier"),
                action = Settings.ACTION_WIRELESS_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "mobile_data",
                titleRes = R.string.settings_shortcut_mobile_data,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("data", "roaming", "cellular"),
                action = Settings.ACTION_DATA_ROAMING_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "data_usage",
                titleRes = R.string.settings_shortcut_data_usage,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("data usage", "mobile data", "network usage"),
                action = Settings.ACTION_DATA_USAGE_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "airplane",
                titleRes = R.string.settings_shortcut_airplane,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("flight", "offline"),
                action = Settings.ACTION_AIRPLANE_MODE_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "hotspot",
                titleRes = R.string.settings_shortcut_hotspot,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("hotspot", "tethering", "wifi hotspot"),
                action = "android.settings.TETHER_SETTINGS",
            ),
        )

        add(
            createShortcut(
                id = "nfc",
                titleRes = R.string.settings_shortcut_nfc,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("tap", "pay", "contactless"),
                action = Settings.ACTION_NFC_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "vpn",
                titleRes = R.string.settings_shortcut_vpn,
                descriptionRes = R.string.device_settings_category_network_connectivity,
                keywords = listOf("vpn", "work", "tunnel"),
                action = Settings.ACTION_VPN_SETTINGS,
            ),
        )
    }

    private fun MutableList<DeviceSetting>.addSecurityPrivacyShortcuts() {
        add(
            createShortcut(
                id = "privacy",
                titleRes = R.string.settings_shortcut_privacy,
                descriptionRes = R.string.device_settings_category_security_privacy,
                keywords = listOf("permissions", "data", "safety"),
                action = Settings.ACTION_PRIVACY_SETTINGS,
                minSdk = Build.VERSION_CODES.Q,
            ),
        )

        add(
            createShortcut(
                id = "security",
                titleRes = R.string.settings_shortcut_security,
                descriptionRes = R.string.device_settings_category_security_privacy,
                keywords = listOf("lock", "screen", "device", "pin"),
                action = Settings.ACTION_SECURITY_SETTINGS,
            ),
        )
    }

    private fun MutableList<DeviceSetting>.addAppsPermissionsShortcuts(packageUri: String) {
        add(
            createShortcut(
                id = "apps_list",
                titleRes = R.string.section_apps,
                descriptionRes = R.string.device_settings_category_apps_permissions,
                keywords = listOf("manage", "permissions", "default"),
                action = Settings.ACTION_APPLICATION_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "overlay",
                titleRes = R.string.settings_shortcut_overlay_permission,
                descriptionRes = R.string.device_settings_category_apps_permissions,
                keywords = listOf("draw over", "overlay", "bubble"),
                action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                data = packageUri,
                minSdk = Build.VERSION_CODES.M,
            ),
        )

        add(
            createShortcut(
                id = "all_files_access",
                titleRes = R.string.settings_shortcut_all_files,
                descriptionRes = R.string.device_settings_category_apps_permissions,
                keywords = listOf("files", "storage", "manage"),
                action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION,
                minSdk = Build.VERSION_CODES.R,
            ),
        )

        add(
            createShortcut(
                id = "usage_access",
                titleRes = R.string.settings_shortcut_usage_access,
                descriptionRes = R.string.device_settings_category_apps_permissions,
                keywords = listOf("usage", "stats", "permissions"),
                action = Settings.ACTION_USAGE_ACCESS_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "home",
                titleRes = R.string.settings_shortcut_home,
                descriptionRes = R.string.device_settings_category_apps_permissions,
                keywords = listOf("launcher", "default", "home app"),
                action = Settings.ACTION_HOME_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "default_apps",
                titleRes = R.string.settings_shortcut_default_apps,
                descriptionRes = R.string.device_settings_category_apps_permissions,
                keywords = listOf("default apps", "defaults", "open by default"),
                action = Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
                minSdk = Build.VERSION_CODES.N,
            ),
        )
    }

    private fun MutableList<DeviceSetting>.addBatteryPerformanceShortcuts() {
        add(
            createShortcut(
                id = "battery_saver",
                titleRes = R.string.settings_shortcut_battery_saver,
                descriptionRes = R.string.device_settings_category_battery_performance,
                keywords = listOf("power", "low power", "battery"),
                action = Settings.ACTION_BATTERY_SAVER_SETTINGS,
                minSdk = Build.VERSION_CODES.LOLLIPOP,
            ),
        )

        add(
            createShortcut(
                id = "battery_optimization",
                titleRes = R.string.settings_shortcut_battery_optimization,
                descriptionRes = R.string.device_settings_category_battery_performance,
                keywords = listOf("doze", "optimize", "ignore"),
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                minSdk = Build.VERSION_CODES.M,
            ),
        )

        add(
            createShortcut(
                id = "battery_usage",
                titleRes = R.string.settings_shortcut_battery_usage,
                descriptionRes = R.string.device_settings_category_battery_performance,
                keywords = listOf("battery usage", "power usage", "consumption"),
                action = "android.settings.BATTERY_USAGE_SETTINGS",
                minSdk = Build.VERSION_CODES.P,
            ),
        )
    }

    private fun MutableList<DeviceSetting>.addLanguageInputShortcuts() {
        add(
            createShortcut(
                id = "language_input",
                titleRes = R.string.device_settings_category_language_input,
                descriptionRes = R.string.device_settings_category_language_input,
                keywords = listOf("keyboard", "ime", "typing"),
                action = Settings.ACTION_INPUT_METHOD_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "locale",
                titleRes = R.string.settings_shortcut_locale,
                descriptionRes = R.string.device_settings_category_language_input,
                keywords = listOf("language", "region", "locales"),
                action = Settings.ACTION_LOCALE_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "voice_input",
                titleRes = R.string.settings_shortcut_voice_input,
                descriptionRes = R.string.device_settings_category_language_input,
                keywords = listOf("assistant", "speech", "voice"),
                action = Settings.ACTION_VOICE_INPUT_SETTINGS,
            ),
        )
    }

    private fun MutableList<DeviceSetting>.addNotificationsAccessibilityShortcuts() {
        add(
            createShortcut(
                id = "notifications",
                titleRes = R.string.settings_shortcut_notifications,
                descriptionRes = R.string.device_settings_category_notifications_accessibility,
                keywords = listOf("alerts", "sounds", "banner"),
                action = Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
                minSdk = Build.VERSION_CODES.M,
            ),
        )

        add(
            createShortcut(
                id = "do_not_disturb",
                titleRes = R.string.settings_shortcut_do_not_disturb,
                descriptionRes = R.string.device_settings_category_notifications_accessibility,
                keywords = listOf("dnd", "focus", "zen mode"),
                action = "android.settings.ZEN_MODE_SETTINGS",
                minSdk = Build.VERSION_CODES.M,
            ),
        )

        add(
            createShortcut(
                id = "accessibility",
                titleRes = R.string.settings_shortcut_accessibility,
                descriptionRes = R.string.device_settings_category_notifications_accessibility,
                keywords = listOf("a11y", "talkback", "display size"),
                action = Settings.ACTION_ACCESSIBILITY_SETTINGS,
            ),
        )

        add(
            createShortcut(
                id = "notification_listener",
                titleRes = R.string.settings_shortcut_notification_listener,
                descriptionRes = R.string.device_settings_category_notifications_accessibility,
                keywords = listOf("listener", "service", "notifications"),
                action = Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
            ),
        )
    }

    private fun MutableList<DeviceSetting>.addDeveloperShortcuts() {
        add(
            createShortcut(
                id = "developer_options",
                titleRes = R.string.settings_shortcut_developer_options,
                descriptionRes = R.string.device_settings_category_developer_advanced,
                keywords = listOf("debug", "adb", "dev"),
                action = Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            ),
        )
    }

    private fun createShortcut(
        id: String,
        titleRes: Int,
        descriptionRes: Int,
        keywords: List<String>,
        action: String,
        data: String? = null,
        extras: Map<String, Any>? = null,
        minSdk: Int = Build.VERSION_CODES.BASE,
    ): DeviceSetting =
        DeviceSetting(
            id = id,
            title = context.getString(titleRes),
            description = context.getString(descriptionRes),
            keywords = keywords,
            action = action,
            data = data,
            extras = extras.orEmpty(),
            minSdk = minSdk,
        )
}
