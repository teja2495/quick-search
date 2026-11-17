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
        private const val LAVENDER_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasLavenderLight"
        private const val LAVENDER_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasLavenderDark"
        private const val TEAL_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasTealLight"
        private const val TEAL_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasTealDark"
        private const val AMBER_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasAmberLight"
        private const val AMBER_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasAmberDark"
        private const val INDIGO_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasIndigoLight"
        private const val INDIGO_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasIndigoDark"
        private const val SLATE_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasSlateLight"
        private const val SLATE_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasSlateDark"
        private const val ROSE_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasRoseLight"
        private const val ROSE_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasRoseDark"
        private const val MOCHA_LIGHT_ALIAS = "com.tk.quicksearch.app.MainActivityAliasMochaLight"
        private const val MOCHA_DARK_ALIAS = "com.tk.quicksearch.app.MainActivityAliasMochaDark"

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
                LauncherAppIcon.LAVENDER_LIGHT to LAVENDER_LIGHT_ALIAS,
                LauncherAppIcon.LAVENDER_DARK to LAVENDER_DARK_ALIAS,
                LauncherAppIcon.TEAL_LIGHT to TEAL_LIGHT_ALIAS,
                LauncherAppIcon.TEAL_DARK to TEAL_DARK_ALIAS,
                LauncherAppIcon.AMBER_LIGHT to AMBER_LIGHT_ALIAS,
                LauncherAppIcon.AMBER_DARK to AMBER_DARK_ALIAS,
                LauncherAppIcon.INDIGO_LIGHT to INDIGO_LIGHT_ALIAS,
                LauncherAppIcon.INDIGO_DARK to INDIGO_DARK_ALIAS,
                LauncherAppIcon.SLATE_LIGHT to SLATE_LIGHT_ALIAS,
                LauncherAppIcon.SLATE_DARK to SLATE_DARK_ALIAS,
                LauncherAppIcon.ROSE_LIGHT to ROSE_LIGHT_ALIAS,
                LauncherAppIcon.ROSE_DARK to ROSE_DARK_ALIAS,
                LauncherAppIcon.MOCHA_LIGHT to MOCHA_LIGHT_ALIAS,
                LauncherAppIcon.MOCHA_DARK to MOCHA_DARK_ALIAS,
            )
    }
}
