package com.tk.quicksearch.search.startup

import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.models.AppInfo
import org.json.JSONArray
import org.json.JSONObject

private const val SNAPSHOT_VERSION = 1

/**
 * Small persisted snapshot used to pre-hydrate the first visible search surface on cold start.
 */
data class StartupSurfaceSnapshot(
    val version: Int = SNAPSHOT_VERSION,
    val createdAtMillis: Long,
    val backgroundSource: BackgroundSource,
    val showWallpaperBackground: Boolean,
    val wallpaperBackgroundAlpha: Float,
    val wallpaperBlurRadius: Float,
    val appTheme: AppTheme,
    val overlayThemeIntensity: Float,
    val customImageUri: String?,
    val startupBackgroundPreviewPath: String?,
    val oneHandedMode: Boolean,
    val bottomSearchBarEnabled: Boolean,
    val topResultIndicatorEnabled: Boolean,
    val openKeyboardOnLaunch: Boolean,
    val fontScaleMultiplier: Float,
    val useSystemFont: Boolean = false,
    val showAppLabels: Boolean,
    val appSuggestionsEnabled: Boolean,
    val phoneAppGridColumns: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS,
    val suggestedApps: List<AppInfo>,
)

internal object StartupSurfaceSnapshotJson {
    private const val KEY_VERSION = "version"
    private const val KEY_CREATED_AT = "createdAtMillis"
    private const val KEY_BACKGROUND_SOURCE = "backgroundSource"
    private const val KEY_SHOW_WALLPAPER = "showWallpaperBackground"
    private const val KEY_WALLPAPER_ALPHA = "wallpaperBackgroundAlpha"
    private const val KEY_WALLPAPER_BLUR = "wallpaperBlurRadius"
    private const val KEY_APP_THEME = "appTheme"
    private const val LEGACY_KEY_APP_THEME = "overlayGradientTheme"
    private const val KEY_THEME_INTENSITY = "overlayThemeIntensity"
    private const val KEY_CUSTOM_IMAGE_URI = "customImageUri"
    private const val KEY_PREVIEW_PATH = "startupBackgroundPreviewPath"
    private const val KEY_ONE_HANDED = "oneHandedMode"
    private const val KEY_BOTTOM_SEARCH_BAR = "bottomSearchBarEnabled"
    private const val KEY_TOP_RESULT_INDICATOR = "topResultIndicatorEnabled"
    private const val KEY_OPEN_KEYBOARD_ON_LAUNCH = "openKeyboardOnLaunch"
    private const val KEY_FONT_SCALE = "fontScaleMultiplier"
    private const val KEY_USE_SYSTEM_FONT = "useSystemFont"
    private const val KEY_SHOW_APP_LABELS = "showAppLabels"
    private const val KEY_APP_SUGGESTIONS = "appSuggestionsEnabled"
    private const val KEY_PHONE_APP_GRID_COLUMNS = "phoneAppGridColumns"
    private const val KEY_SUGGESTED_APPS = "suggestedApps"

    private const val KEY_APP_NAME = "appName"
    private const val KEY_PACKAGE_NAME = "packageName"
    private const val KEY_LAST_USED_TIME = "lastUsedTime"
    private const val KEY_TOTAL_TIME_IN_FOREGROUND = "totalTimeInForeground"
    private const val KEY_LAUNCH_COUNT = "launchCount"
    private const val KEY_FIRST_INSTALL_TIME = "firstInstallTime"
    private const val KEY_IS_SYSTEM_APP = "isSystemApp"
    private const val KEY_USER_HANDLE_ID = "userHandleId"
    private const val KEY_COMPONENT_NAME = "componentName"

    fun toJson(snapshot: StartupSurfaceSnapshot): String {
        val root =
            JSONObject().apply {
                put(KEY_VERSION, snapshot.version)
                put(KEY_CREATED_AT, snapshot.createdAtMillis)
                put(KEY_BACKGROUND_SOURCE, snapshot.backgroundSource.name)
                put(KEY_SHOW_WALLPAPER, snapshot.showWallpaperBackground)
                put(KEY_WALLPAPER_ALPHA, snapshot.wallpaperBackgroundAlpha.toDouble())
                put(KEY_WALLPAPER_BLUR, snapshot.wallpaperBlurRadius.toDouble())
                put(KEY_APP_THEME, snapshot.appTheme.name)
                put(KEY_THEME_INTENSITY, snapshot.overlayThemeIntensity.toDouble())
                put(KEY_ONE_HANDED, snapshot.oneHandedMode)
                put(KEY_BOTTOM_SEARCH_BAR, snapshot.bottomSearchBarEnabled)
                put(KEY_TOP_RESULT_INDICATOR, snapshot.topResultIndicatorEnabled)
                put(KEY_OPEN_KEYBOARD_ON_LAUNCH, snapshot.openKeyboardOnLaunch)
                put(KEY_FONT_SCALE, snapshot.fontScaleMultiplier.toDouble())
                put(KEY_USE_SYSTEM_FONT, snapshot.useSystemFont)
                put(KEY_SHOW_APP_LABELS, snapshot.showAppLabels)
                put(KEY_APP_SUGGESTIONS, snapshot.appSuggestionsEnabled)
                put(KEY_PHONE_APP_GRID_COLUMNS, snapshot.phoneAppGridColumns)
                if (!snapshot.customImageUri.isNullOrBlank()) {
                    put(KEY_CUSTOM_IMAGE_URI, snapshot.customImageUri)
                }
                if (!snapshot.startupBackgroundPreviewPath.isNullOrBlank()) {
                    put(KEY_PREVIEW_PATH, snapshot.startupBackgroundPreviewPath)
                }
                put(KEY_SUGGESTED_APPS, JSONArray().apply {
                    snapshot.suggestedApps.forEach { app ->
                        put(
                            JSONObject().apply {
                                put(KEY_APP_NAME, app.appName)
                                put(KEY_PACKAGE_NAME, app.packageName)
                                put(KEY_LAST_USED_TIME, app.lastUsedTime)
                                put(KEY_TOTAL_TIME_IN_FOREGROUND, app.totalTimeInForeground)
                                put(KEY_LAUNCH_COUNT, app.launchCount)
                                put(KEY_FIRST_INSTALL_TIME, app.firstInstallTime)
                                put(KEY_IS_SYSTEM_APP, app.isSystemApp)
                                app.userHandleId?.let { put(KEY_USER_HANDLE_ID, it) }
                                app.componentName?.let { put(KEY_COMPONENT_NAME, it) }
                            },
                        )
                    }
                })
            }
        return root.toString()
    }

    fun fromJson(raw: String?): StartupSurfaceSnapshot? {
        if (raw.isNullOrBlank()) return null

        return runCatching {
            val root = JSONObject(raw)
            val version = root.optInt(KEY_VERSION, SNAPSHOT_VERSION)
            if (version != SNAPSHOT_VERSION) return null

            val backgroundSource =
                root.optString(KEY_BACKGROUND_SOURCE)
                    .takeIf { it.isNotBlank() }
                    ?.let { runCatching { BackgroundSource.valueOf(it) }.getOrNull() }
                    ?: BackgroundSource.THEME

            val appThemeRaw =
                root.optString(KEY_APP_THEME).takeIf { it.isNotBlank() }
                    ?: root.optString(LEGACY_KEY_APP_THEME).takeIf { it.isNotBlank() }
            val appTheme =
                appThemeRaw?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                    ?: AppTheme.MONOCHROME

            val suggestedApps =
                root.optJSONArray(KEY_SUGGESTED_APPS)
                    ?.toAppInfoList()
                    .orEmpty()

            StartupSurfaceSnapshot(
                version = version,
                createdAtMillis = root.optLong(KEY_CREATED_AT, 0L),
                backgroundSource = backgroundSource,
                showWallpaperBackground = root.optBoolean(KEY_SHOW_WALLPAPER, backgroundSource != BackgroundSource.THEME),
                wallpaperBackgroundAlpha = root.optDouble(KEY_WALLPAPER_ALPHA, 0.5).toFloat(),
                wallpaperBlurRadius = root.optDouble(KEY_WALLPAPER_BLUR, 20.0).toFloat(),
                appTheme = appTheme,
                overlayThemeIntensity = root.optDouble(KEY_THEME_INTENSITY, 0.5).toFloat(),
                customImageUri = root.optString(KEY_CUSTOM_IMAGE_URI).takeIf { it.isNotBlank() },
                startupBackgroundPreviewPath = root.optString(KEY_PREVIEW_PATH).takeIf { it.isNotBlank() },
                oneHandedMode = root.optBoolean(KEY_ONE_HANDED, false),
                bottomSearchBarEnabled = root.optBoolean(KEY_BOTTOM_SEARCH_BAR, false),
                topResultIndicatorEnabled = root.optBoolean(KEY_TOP_RESULT_INDICATOR, true),
                openKeyboardOnLaunch = root.optBoolean(KEY_OPEN_KEYBOARD_ON_LAUNCH, true),
                fontScaleMultiplier = root.optDouble(KEY_FONT_SCALE, 1.0).toFloat(),
                useSystemFont = root.optBoolean(KEY_USE_SYSTEM_FONT, false),
                showAppLabels = root.optBoolean(KEY_SHOW_APP_LABELS, true),
                appSuggestionsEnabled = root.optBoolean(KEY_APP_SUGGESTIONS, true),
                phoneAppGridColumns = root.optInt(KEY_PHONE_APP_GRID_COLUMNS, com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_PHONE_APP_GRID_COLUMNS),
                suggestedApps = suggestedApps,
            )
        }.getOrNull()
    }

    private fun JSONArray.toAppInfoList(): List<AppInfo> =
        buildList {
            for (index in 0 until length()) {
                val app = optJSONObject(index) ?: continue
                val packageName = app.optString(KEY_PACKAGE_NAME).takeIf { it.isNotBlank() } ?: continue
                val appName = app.optString(KEY_APP_NAME).ifBlank { packageName }
                add(
                    AppInfo(
                        appName = appName,
                        packageName = packageName,
                        lastUsedTime = app.optLong(KEY_LAST_USED_TIME, 0L),
                        totalTimeInForeground = app.optLong(KEY_TOTAL_TIME_IN_FOREGROUND, 0L),
                        launchCount = app.optInt(KEY_LAUNCH_COUNT, 0),
                        firstInstallTime = app.optLong(KEY_FIRST_INSTALL_TIME, 0L),
                        isSystemApp = app.optBoolean(KEY_IS_SYSTEM_APP, false),
                        userHandleId = app.optInt(KEY_USER_HANDLE_ID, -1).takeIf { it >= 0 },
                        componentName = app.optString(KEY_COMPONENT_NAME).takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
}
