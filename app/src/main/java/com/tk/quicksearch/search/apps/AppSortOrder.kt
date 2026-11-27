package com.tk.quicksearch.search.apps

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.AppInfo
import java.io.File

/** Sort orders shared by Manage Apps and the All Apps drawer. */
internal enum class AppSortOption(
    @StringRes val labelResId: Int,
    val requiresUsagePermission: Boolean = false,
) {
    NAME(R.string.settings_app_sort_name),
    APK_SIZE(R.string.settings_app_sort_apk_size),
    MOST_USED(R.string.common_most_used, requiresUsagePermission = true),
    LEAST_USED(R.string.settings_app_sort_least_used, requiresUsagePermission = true),
    INSTALLATION_DATE(R.string.settings_app_info_install_date),
    LAST_UPDATE(R.string.settings_app_info_last_update),
    API_LEVEL(R.string.settings_app_sort_api_level),
}

/** Whether this order reads [AppSortMetadata], which costs a package lookup per app. */
internal val AppSortOption.needsMetadata: Boolean
    get() =
        when (this) {
            AppSortOption.APK_SIZE, AppSortOption.INSTALLATION_DATE, AppSortOption.LAST_UPDATE, AppSortOption.API_LEVEL -> true
            AppSortOption.NAME, AppSortOption.MOST_USED, AppSortOption.LEAST_USED -> false
        }

internal fun availableAppSortOptions(hasUsagePermission: Boolean): List<AppSortOption> =
    AppSortOption.entries.filter { !it.requiresUsagePermission || hasUsagePermission }

/** Ascending comparator for [option]; ties fall back to [nameKey]. */
internal fun appSortComparator(
    option: AppSortOption,
    metadataByKey: Map<String, AppSortMetadata>,
    nameKey: (AppInfo) -> String,
): Comparator<AppInfo> {
    fun meta(app: AppInfo) = metadataByKey[app.launchCountKey()]
    val primary =
        when (option) {
            AppSortOption.NAME -> return compareBy(nameKey)
            AppSortOption.APK_SIZE -> compareBy<AppInfo> { meta(it)?.sizeBytes ?: 0L }
            AppSortOption.MOST_USED ->
                compareByDescending<AppInfo> { it.launchCount }.thenByDescending { it.totalTimeInForeground }
            AppSortOption.LEAST_USED -> compareBy<AppInfo> { it.launchCount }.thenBy { it.totalTimeInForeground }
            AppSortOption.INSTALLATION_DATE -> compareBy<AppInfo> { meta(it)?.installTimeMillis ?: it.firstInstallTime }
            AppSortOption.LAST_UPDATE -> compareBy<AppInfo> { meta(it)?.lastUpdateTimeMillis ?: 0L }
            AppSortOption.API_LEVEL -> compareBy<AppInfo> { meta(it)?.targetSdk ?: 0 }
        }
    return primary.thenBy(nameKey)
}

internal data class AppSortMetadata(
    val sizeBytes: Long,
    val installTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val targetSdk: Int,
)

internal fun loadAppSortMetadata(
    context: Context,
    app: AppInfo,
): AppSortMetadata {
    val packageInfo =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    app.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(app.packageName, 0)
            }
        }.getOrNull()

    val sourceDir = packageInfo?.applicationInfo?.sourceDir
    val appSizeBytes = sourceDir?.let { path -> runCatching { File(path).length() }.getOrDefault(0L) } ?: 0L

    return AppSortMetadata(
        sizeBytes = appSizeBytes,
        installTimeMillis = packageInfo?.firstInstallTime ?: app.firstInstallTime,
        lastUpdateTimeMillis = packageInfo?.lastUpdateTime ?: 0L,
        targetSdk = packageInfo?.applicationInfo?.targetSdkVersion ?: 0,
    )
}

