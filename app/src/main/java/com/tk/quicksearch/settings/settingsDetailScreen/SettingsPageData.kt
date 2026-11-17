package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.settings.appShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.appShortcutsSettings.queryAppShortcutSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal val LocalSettingsPageData = staticCompositionLocalOf<SettingsPageData?> { null }

/** Keeps one catalog snapshot per loader for the lifetime of the navigation host. */
internal class SettingsPageData {
    private data class CatalogSnapshot<T>(val apps: List<AppInfo>, val data: T)

    private val metadataLock = Mutex()
    private var metadata: CatalogSnapshot<Map<String, AppSortMetadata>>? = null
    private val shortcutSourcesLock = Mutex()
    private var shortcutSources: CatalogSnapshot<List<AppShortcutSource>>? = null

    suspend fun appMetadata(context: Context, apps: List<AppInfo>): Map<String, AppSortMetadata> =
        withContext(Dispatchers.IO) {
            metadataLock.withLock {
                metadata?.takeIf { it.apps == apps }?.data ?: run {
                    val jobContext = currentCoroutineContext()
                    apps.associate { app ->
                        jobContext.ensureActive()
                        app.launchCountKey() to loadAppSortMetadata(context, app)
                    }.also { metadata = CatalogSnapshot(apps, it) }
                }
            }
        }

    suspend fun appShortcutSources(context: Context, apps: List<AppInfo>): List<AppShortcutSource> =
        withContext(Dispatchers.IO) {
            shortcutSourcesLock.withLock {
                shortcutSources?.takeIf { it.apps == apps }?.data
                    ?: queryAppShortcutSources(context.packageManager, apps).also {
                        if (apps.isNotEmpty()) shortcutSources = CatalogSnapshot(apps, it)
                    }
            }
        }
}
