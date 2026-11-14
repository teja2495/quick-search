package com.tk.quicksearch.search.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Applies launcher icon alias state based on the selected icon option. */
class LauncherIconManager(
    private val context: Context,
) {
    fun applySelection(
        selection: LauncherAppIcon,
    ) {
        // Alternate icons are adaptive-only; below API 26 they don't resolve, so keep the default.
        val resolved =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) selection else LauncherAppIcon.DEFAULT
        val packageManager = context.packageManager
        // Enable the new alias first, then disable the rest. Enabling before disabling ensures
        // Android does not kill the app when the previously-active alias is disabled.
        val targetClass = aliasMap[resolved] ?: return
        setAliasEnabled(packageManager, targetClass, enabled = true)
        aliasMap.forEach { (option, componentClass) ->
            if (option == resolved) return@forEach
            setAliasEnabled(packageManager, componentClass, enabled = false)
        }
    }

    /**
     * Writes the alias state only when it differs from the effective state. This runs on every
     * startup, and each real write sends PACKAGE_CHANGED, which can make launchers and gesture
     * apps (e.g. Good Lock HomeUp) drop shortcuts that point at our launcher alias.
     */
    private fun setAliasEnabled(
        packageManager: PackageManager,
        componentClass: String,
        enabled: Boolean,
    ) {
        val component = ComponentName(context, componentClass)
        runCatching {
            val current =
                when (packageManager.getComponentEnabledSetting(component)) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> componentClass == DEFAULT_ALIAS
                    else -> false
                }
            if (current == enabled) return@runCatching
            packageManager.setComponentEnabledSetting(
                component,
                if (enabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
    }

    companion object {
        private const val DEFAULT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasDefault"
        private const val MONOCHROME_LIGHT_ALIAS =
            "com.tk.quicksearch.app.MainActivityAliasMonochromeLight"
        private const val MONOCHROME_DARK_ALIAS =
            "com.tk.quicksearch.app.MainActivityAliasMonochromeDark"
        private const val FOREST_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasForestLight"
        private const val FOREST_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasForestDark"
        private const val AURORA_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasAuroraLight"
        private const val AURORA_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasAuroraDark"
        private const val SUNSET_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasSunsetLight"
        private const val SUNSET_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasSunsetDark"

        private val aliasMap: Map<LauncherAppIcon, String> =
            mapOf(
                LauncherAppIcon.DEFAULT to DEFAULT_ALIAS,
                LauncherAppIcon.MONOCHROME_LIGHT to MONOCHROME_LIGHT_ALIAS,
                LauncherAppIcon.MONOCHROME_DARK to MONOCHROME_DARK_ALIAS,
                LauncherAppIcon.FOREST_LIGHT to FOREST_LIGHT_ALIAS,
                LauncherAppIcon.FOREST_DARK to FOREST_DARK_ALIAS,
                LauncherAppIcon.AURORA_LIGHT to AURORA_LIGHT_ALIAS,
                LauncherAppIcon.AURORA_DARK to AURORA_DARK_ALIAS,
                LauncherAppIcon.SUNSET_LIGHT to SUNSET_LIGHT_ALIAS,
                LauncherAppIcon.SUNSET_DARK to SUNSET_DARK_ALIAS,
            )
    }
}
