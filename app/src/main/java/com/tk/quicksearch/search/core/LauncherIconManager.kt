package com.tk.quicksearch.search.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Applies launcher icon alias state based on the selected icon option. */
class LauncherIconManager(
    private val context: Context,
) {
    fun applySelection(
        selection: LauncherAppIcon,
    ) {
        val resolved = selection
        val packageManager = context.packageManager
        // Enable the new alias first, then disable the rest. Enabling before disabling ensures
        // Android does not kill the app when the previously-active alias is disabled.
        val targetComponent = ComponentName(context, aliasMap[resolved] ?: return)
        runCatching {
            packageManager.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        aliasMap.forEach { (option, componentClass) ->
            if (option == resolved) return@forEach
            runCatching {
                packageManager.setComponentEnabledSetting(
                    ComponentName(context, componentClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
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
