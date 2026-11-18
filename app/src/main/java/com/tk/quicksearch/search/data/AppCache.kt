package com.tk.quicksearch.search.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tk.quicksearch.search.models.AppInfo
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/**
 * Manages persistent caching of app list to enable instant loading on app startup.
 * Uses a compact app-private file for startup reads.
 */
class AppCache(
    context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val cacheFile = File(context.filesDir, CACHE_FILE_NAME)
    private val tempCacheFile = File(context.filesDir, "$CACHE_FILE_NAME.tmp")

    /**
     * @return List of cached apps, or null if no cache exists or if cache is corrupted.
     */
    fun loadCachedApps(): List<AppInfo>? {
        val removedAppKeys = getRemovedAppKeys()
        return loadCachedAppsFromFile()?.let { filterRemovedApps(it, removedAppKeys) }
    }

    /**
     * @param apps The list of apps to cache.
     * @return true if the save operation succeeded, false otherwise.
     */
    fun saveApps(
        apps: List<AppInfo>,
        catalogReconciled: Boolean = true,
    ): Boolean =
        runCatching {
            saveAppsToFile(apps)
            if (catalogReconciled) {
                prefs
                    .edit()
                    .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                    .putBoolean(KEY_CATALOG_INVALIDATED, false)
                    .remove(KEY_REMOVED_APP_KEYS)
                    .apply()
            }
            true
        }.onFailure { exception ->
            Log.e(TAG, "Failed to save apps to cache", exception)
        }.getOrDefault(false)

    /**
     * @return Timestamp in milliseconds, or 0L if cache has never been updated.
     */
    fun getLastUpdateTime(): Long = prefs.getLong(KEY_LAST_UPDATE, 0L)

    fun isCatalogInvalidated(): Boolean = prefs.getBoolean(KEY_CATALOG_INVALIDATED, false)

    internal fun recordCatalogChange(
        change: AppCatalogChange,
        currentUserHandleId: Int,
        commitSynchronously: Boolean = false,
    ) {
        AppCatalogLoadCoalescer.onCatalogChanged()
        val appKey = change.appKey(currentUserHandleId)
        val removedAppKeys = getRemovedAppKeys().toMutableSet()
        if (appKey != null) {
            if (change.isRemoval) {
                removedAppKeys.add(appKey)
            } else {
                removedAppKeys.remove(appKey)
            }
        }

        val editor =
            prefs
                .edit()
                .putBoolean(KEY_CATALOG_INVALIDATED, true)
                .putStringSet(KEY_REMOVED_APP_KEYS, removedAppKeys)
        if (commitSynchronously) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    internal fun recordUnavailableApps(apps: List<AppInfo>) {
        if (apps.isEmpty()) return
        AppCatalogLoadCoalescer.onCatalogChanged()

        val removedAppKeys = getRemovedAppKeys().toMutableSet()
        removedAppKeys.addAll(apps.map { it.launchCountKey() })
        prefs
            .edit()
            .putBoolean(KEY_CATALOG_INVALIDATED, true)
            .putStringSet(KEY_REMOVED_APP_KEYS, removedAppKeys)
            .apply()
    }

    internal fun removedAppKeys(): Set<String> = getRemovedAppKeys()

    fun clearCatalogInvalidation() {
        prefs.edit().putBoolean(KEY_CATALOG_INVALIDATED, false).apply()
    }

    private fun getRemovedAppKeys(): Set<String> =
        prefs.getStringSet(KEY_REMOVED_APP_KEYS, emptySet()).orEmpty().toSet()

    fun clearCache() {
        AppCatalogLoadCoalescer.onCatalogChanged()
        prefs.edit().clear().apply()
        synchronized(FILE_LOCK) {
            cacheFile.delete()
            tempCacheFile.delete()
            lastPersistedApps = null
        }
    }

    /**
     * Whether [apps] equals what the cache file currently holds, without re-reading the file when
     * this process already knows its content.
     */
    internal fun matchesPersistedApps(apps: List<AppInfo>): Boolean {
        val persisted = lastPersistedApps ?: run {
            loadCachedAppsFromFile()
            lastPersistedApps
        }
        return persisted == apps
    }

    private fun loadCachedAppsFromFile(): List<AppInfo>? {
        if (!cacheFile.exists()) return null

        return runCatching {
            DataInputStream(BufferedInputStream(cacheFile.inputStream())).use { input ->
                val version = input.readInt()
                if (version !in 1..CACHE_FILE_VERSION) return null
                val appCount = input.readInt()
                if (appCount <= 0) return null

                val apps = List(appCount) {
                    input.readAppInfo(version)
                }
                // A catalog written before aliases were available can still render immediately,
                // but must be reconciled in the background before it is used for app search.
                if (version < CACHE_FILE_VERSION) {
                    prefs.edit().putBoolean(KEY_CATALOG_INVALIDATED, true).apply()
                } else {
                    lastPersistedApps = apps
                }
                apps
            }
        }.onFailure { exception ->
            Log.e(TAG, "Failed to load cached apps", exception)
        }.getOrNull()
    }

    // Each surface's ViewModel owns an AppCache over the same file, so writes are serialized
    // process-wide to keep two writers from interleaving on the shared temp file.
    private fun saveAppsToFile(apps: List<AppInfo>) =
        synchronized(FILE_LOCK) {
            if (apps.isEmpty()) {
                cacheFile.delete()
                tempCacheFile.delete()
                lastPersistedApps = null
                return@synchronized
            }

            lastPersistedApps = null
            DataOutputStream(BufferedOutputStream(tempCacheFile.outputStream())).use { output ->
                output.writeInt(CACHE_FILE_VERSION)
                output.writeInt(apps.size)
                apps.forEach { app -> output.writeAppInfo(app) }
            }

            if (!tempCacheFile.renameTo(cacheFile)) {
                tempCacheFile.copyTo(cacheFile, overwrite = true)
                tempCacheFile.delete()
            }
            lastPersistedApps = apps
        }

    companion object {
        private const val TAG = "AppCache"
        private val FILE_LOCK = Any()

        /** Content of the cache file as last read or written by this process (current version only). */
        @Volatile
        private var lastPersistedApps: List<AppInfo>? = null
        private const val PREFS_NAME = "app_cache"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_CATALOG_INVALIDATED = "catalog_invalidated"
        private const val KEY_REMOVED_APP_KEYS = "removed_app_keys"
        private const val CACHE_FILE_NAME = "app_cache_v1.bin"
        private const val CACHE_FILE_VERSION = 7

        private fun DataInputStream.readAppInfo(version: Int): AppInfo {
            val appName = readUTF()
            val packageName = readUTF()
            val lastUsedTime = readLong()
            val totalTimeInForeground = readLong()
            val launchCount = readInt()
            val firstInstallTime = readLong()
            val isSystemApp = readBoolean()
            val hasLaunchIntent = if (version >= 3) readBoolean() else true
            val userHandleId = readNullableInt()
            val componentName = readNullableString()
            val lastUpdateTime = if (version >= 2) readLong() else firstInstallTime
            val searchAliases = if (version >= 4) readStringList() else emptyList()
            val isArchived = if (version >= 6) readBoolean() else false
            val isSuspended = if (version >= 7) readBoolean() else false
            return AppInfo(
                appName = appName,
                searchAliases = searchAliases,
                packageName = packageName,
                lastUsedTime = lastUsedTime,
                totalTimeInForeground = totalTimeInForeground,
                launchCount = launchCount,
                firstInstallTime = firstInstallTime,
                isSystemApp = isSystemApp,
                hasLaunchIntent = hasLaunchIntent,
                userHandleId = userHandleId,
                componentName = componentName,
                lastUpdateTime = lastUpdateTime,
                isArchived = isArchived,
                isSuspended = isSuspended,
            )
        }

        private fun DataOutputStream.writeAppInfo(app: AppInfo) {
            writeUTF(app.appName)
            writeUTF(app.packageName)
            writeLong(app.lastUsedTime)
            writeLong(app.totalTimeInForeground)
            writeInt(app.launchCount)
            writeLong(app.firstInstallTime)
            writeBoolean(app.isSystemApp)
            writeBoolean(app.hasLaunchIntent)
            writeNullableInt(app.userHandleId)
            writeNullableString(app.componentName)
            writeLong(app.lastUpdateTime)
            writeStringList(app.searchAliases)
            writeBoolean(app.isArchived)
            writeBoolean(app.isSuspended)
        }

        private fun DataInputStream.readNullableInt(): Int? =
            if (readBoolean()) readInt() else null

        private fun DataOutputStream.writeNullableInt(value: Int?) {
            writeBoolean(value != null)
            if (value != null) writeInt(value)
        }

        private fun DataInputStream.readNullableString(): String? =
            if (readBoolean()) readUTF() else null

        private fun DataOutputStream.writeNullableString(value: String?) {
            writeBoolean(value != null)
            if (value != null) writeUTF(value)
        }

        private fun DataInputStream.readStringList(): List<String> {
            val size = readInt()
            require(size in 0..MAX_SEARCH_ALIASES) { "Invalid search alias count: $size" }
            return List(size) { readUTF() }
        }

        private fun DataOutputStream.writeStringList(values: List<String>) {
            val boundedValues = values.take(MAX_SEARCH_ALIASES)
            writeInt(boundedValues.size)
            boundedValues.forEach(::writeUTF)
        }

        private const val MAX_SEARCH_ALIASES = 2
    }
}
