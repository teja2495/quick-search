package com.tk.quicksearch.search.core

import android.content.ComponentName
import android.content.Context
import android.app.ActivityManager
import android.content.pm.PackageManager

/** Applies launcher icon alias state based on the selected icon option. */
class LauncherIconManager(
    private val context: Context,
) {
    fun applySelection(
        selection: LauncherAppIcon,
        appTheme: AppTheme,
        isDarkMode: Boolean,
    ) {
        val resolved = resolveSelection(selection, appTheme, isDarkMode)
        val packageManager = context.packageManager
        val currentTaskRootComponent = getCurrentTaskRootComponentClassName()
        // Enable the new alias first, then disable the rest. If the currently-active alias is
        // disabled before the new one is enabled, Android kills the app even with DONT_KILL_APP.
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
            if (componentClass == currentTaskRootComponent) return@forEach
            runCatching {
                packageManager.setComponentEnabledSetting(
                    ComponentName(context, componentClass),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    private fun resolveSelection(
        selection: LauncherAppIcon,
        appTheme: AppTheme,
        isDarkMode: Boolean,
    ): LauncherAppIcon =
        when (selection) {
            LauncherAppIcon.AUTO ->
                when (appTheme) {
                    AppTheme.MONOCHROME ->
                        if (isDarkMode) LauncherAppIcon.MONOCHROME_DARK else LauncherAppIcon.MONOCHROME_LIGHT
                    AppTheme.FOREST ->
                        if (isDarkMode) LauncherAppIcon.FOREST_DARK else LauncherAppIcon.FOREST_LIGHT
                    AppTheme.AURORA ->
                        if (isDarkMode) LauncherAppIcon.AURORA_DARK else LauncherAppIcon.AURORA_LIGHT
                    AppTheme.SUNSET ->
                        if (isDarkMode) LauncherAppIcon.SUNSET_DARK else LauncherAppIcon.SUNSET_LIGHT
                }
            else -> selection
        }

    private fun getCurrentTaskRootComponentClassName(): String? {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        return activityManager.appTasks.firstOrNull()?.taskInfo?.baseIntent?.component?.className
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
