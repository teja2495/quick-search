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
import org.json.JSONArray

/**
 * Manages persistent caching of app list to enable instant loading on app startup.
 * Uses a compact app-private file for startup reads, with the legacy SharedPreferences JSON
 * cache retained as a migration fallback.
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
        loadCachedAppsFromFile()?.let { return filterRemovedApps(it, removedAppKeys) }
        if (!prefs.contains(KEY_APP_LIST)) return null

        val migratedApps = runCatching {
            val json = prefs.getString(KEY_APP_LIST, null) ?: return null
            // Fast paths: empty or minimal content check without full parse
            if (json.length < 10 || json == "[]") return null
            JSONArray(json).toAppInfoList()
        }.onFailure { exception ->
            Log.e(TAG, "Failed to load legacy cached apps", exception)
        }.getOrNull()

        val filteredApps = migratedApps?.let { filterRemovedApps(it, removedAppKeys) }
        if (!filteredApps.isNullOrEmpty()) {
            saveAppsToFile(filteredApps)
        }
        return filteredApps
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
        prefs.edit().clear().apply()
        cacheFile.delete()
        tempCacheFile.delete()
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
                }
                apps
            }
        }.onFailure { exception ->
            Log.e(TAG, "Failed to load cached apps", exception)
        }.getOrNull()
    }

    private fun saveAppsToFile(apps: List<AppInfo>) {
        if (apps.isEmpty()) {
            cacheFile.delete()
            tempCacheFile.delete()
            return
        }

        DataOutputStream(BufferedOutputStream(tempCacheFile.outputStream())).use { output ->
            output.writeInt(CACHE_FILE_VERSION)
            output.writeInt(apps.size)
            apps.forEach { app -> output.writeAppInfo(app) }
        }

        if (!tempCacheFile.renameTo(cacheFile)) {
            tempCacheFile.copyTo(cacheFile, overwrite = true)
            tempCacheFile.delete()
        }
    }

    companion object {
        private const val TAG = "AppCache"
        private const val PREFS_NAME = "app_cache"
        private const val KEY_APP_LIST = "app_list"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_CATALOG_INVALIDATED = "catalog_invalidated"
        private const val KEY_REMOVED_APP_KEYS = "removed_app_keys"
        private const val CACHE_FILE_NAME = "app_cache_v1.bin"
        private const val CACHE_FILE_VERSION = 6

        // JSON field names
        private const val FIELD_APP_NAME = "appName"
        private const val FIELD_SEARCH_ALIASES = "searchAliases"
        private const val FIELD_PACKAGE_NAME = "packageName"
        private const val FIELD_LAST_USED_TIME = "lastUsedTime"
        private const val FIELD_TOTAL_TIME_IN_FOREGROUND = "totalTimeInForeground"
        private const val FIELD_LAUNCH_COUNT = "launchCount"
        private const val FIELD_FIRST_INSTALL_TIME = "firstInstallTime"
        private const val FIELD_LAST_UPDATE_TIME = "lastUpdateTime"
        private const val FIELD_IS_SYSTEM_APP = "isSystemApp"
        private const val FIELD_HAS_LAUNCH_INTENT = "hasLaunchIntent"
        private const val FIELD_USER_HANDLE_ID = "userHandleId"
        private const val FIELD_COMPONENT_NAME = "componentName"

        private fun JSONArray.toAppInfoList(): List<AppInfo> =
            List(length()) { index ->
                val jsonObject = getJSONObject(index)
                val userHandleId =
                    jsonObject.optInt(FIELD_USER_HANDLE_ID, -1).takeIf { it >= 0 }
                AppInfo(
                    appName = jsonObject.getString(FIELD_APP_NAME),
                    searchAliases =
                        jsonObject
                            .optJSONArray(FIELD_SEARCH_ALIASES)
                            ?.let { aliases ->
                                List(aliases.length()) { index -> aliases.optString(index) }
                                    .filter { it.isNotBlank() }
                            }
                            .orEmpty(),
                    packageName = jsonObject.getString(FIELD_PACKAGE_NAME),
                    lastUsedTime = jsonObject.getLong(FIELD_LAST_USED_TIME),
                    totalTimeInForeground = jsonObject.optLong(FIELD_TOTAL_TIME_IN_FOREGROUND, 0L),
                    launchCount = jsonObject.optInt(FIELD_LAUNCH_COUNT, 0),
                    firstInstallTime = jsonObject.optLong(FIELD_FIRST_INSTALL_TIME, 0L),
                    isSystemApp = jsonObject.getBoolean(FIELD_IS_SYSTEM_APP),
                    hasLaunchIntent = jsonObject.optBoolean(FIELD_HAS_LAUNCH_INTENT, true),
                    userHandleId = userHandleId,
                    componentName = jsonObject.optString(FIELD_COMPONENT_NAME).takeIf { it.isNotBlank() },
                    lastUpdateTime =
                        jsonObject.optLong(
                            FIELD_LAST_UPDATE_TIME,
                            jsonObject.optLong(FIELD_FIRST_INSTALL_TIME, 0L),
                        ),
                )
            }

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
