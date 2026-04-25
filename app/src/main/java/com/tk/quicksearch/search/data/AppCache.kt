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
        loadCachedAppsFromFile()?.let { return it }
        if (!prefs.contains(KEY_APP_LIST)) return null

        val migratedApps = runCatching {
            val json = prefs.getString(KEY_APP_LIST, null) ?: return null
            // Fast paths: empty or minimal content check without full parse
            if (json.length < 10 || json == "[]") return null
            JSONArray(json).toAppInfoList()
        }.onFailure { exception ->
            Log.e(TAG, "Failed to load legacy cached apps", exception)
        }.getOrNull()

        if (!migratedApps.isNullOrEmpty()) {
            saveAppsToFile(migratedApps)
        }
        return migratedApps
    }

    /**
     * @param apps The list of apps to cache.
     * @return true if the save operation succeeded, false otherwise.
     */
    fun saveApps(apps: List<AppInfo>): Boolean =
        runCatching {
            saveAppsToFile(apps)
            prefs
                .edit()
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
            true
        }.onFailure { exception ->
            Log.e(TAG, "Failed to save apps to cache", exception)
        }.getOrDefault(false)

    /**
     * @return Timestamp in milliseconds, or 0L if cache has never been updated.
     */
    fun getLastUpdateTime(): Long = prefs.getLong(KEY_LAST_UPDATE, 0L)

    fun clearCache() {
        prefs.edit().clear().apply()
        cacheFile.delete()
        tempCacheFile.delete()
    }

    private fun loadCachedAppsFromFile(): List<AppInfo>? {
        if (!cacheFile.exists()) return null

        return runCatching {
            DataInputStream(BufferedInputStream(cacheFile.inputStream())).use { input ->
                if (input.readInt() != CACHE_FILE_VERSION) return null
                val appCount = input.readInt()
                if (appCount <= 0) return null

                List(appCount) {
                    input.readAppInfo()
                }
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
        private const val CACHE_FILE_NAME = "app_cache_v1.bin"
        private const val CACHE_FILE_VERSION = 1

        // JSON field names
        private const val FIELD_APP_NAME = "appName"
        private const val FIELD_PACKAGE_NAME = "packageName"
        private const val FIELD_LAST_USED_TIME = "lastUsedTime"
        private const val FIELD_TOTAL_TIME_IN_FOREGROUND = "totalTimeInForeground"
        private const val FIELD_LAUNCH_COUNT = "launchCount"
        private const val FIELD_FIRST_INSTALL_TIME = "firstInstallTime"
        private const val FIELD_IS_SYSTEM_APP = "isSystemApp"
        private const val FIELD_USER_HANDLE_ID = "userHandleId"
        private const val FIELD_COMPONENT_NAME = "componentName"

        private fun JSONArray.toAppInfoList(): List<AppInfo> =
            List(length()) { index ->
                val jsonObject = getJSONObject(index)
                val userHandleId =
                    jsonObject.optInt(FIELD_USER_HANDLE_ID, -1).takeIf { it >= 0 }
                AppInfo(
                    appName = jsonObject.getString(FIELD_APP_NAME),
                    packageName = jsonObject.getString(FIELD_PACKAGE_NAME),
                    lastUsedTime = jsonObject.getLong(FIELD_LAST_USED_TIME),
                    totalTimeInForeground = jsonObject.optLong(FIELD_TOTAL_TIME_IN_FOREGROUND, 0L),
                    launchCount = jsonObject.optInt(FIELD_LAUNCH_COUNT, 0),
                    firstInstallTime = jsonObject.optLong(FIELD_FIRST_INSTALL_TIME, 0L),
                    isSystemApp = jsonObject.getBoolean(FIELD_IS_SYSTEM_APP),
                    userHandleId = userHandleId,
                    componentName = jsonObject.optString(FIELD_COMPONENT_NAME).takeIf { it.isNotBlank() },
                )
            }

        private fun DataInputStream.readAppInfo(): AppInfo =
            AppInfo(
                appName = readUTF(),
                packageName = readUTF(),
                lastUsedTime = readLong(),
                totalTimeInForeground = readLong(),
                launchCount = readInt(),
                firstInstallTime = readLong(),
                isSystemApp = readBoolean(),
                userHandleId = readNullableInt(),
                componentName = readNullableString(),
            )

        private fun DataOutputStream.writeAppInfo(app: AppInfo) {
            writeUTF(app.appName)
            writeUTF(app.packageName)
            writeLong(app.lastUsedTime)
            writeLong(app.totalTimeInForeground)
            writeInt(app.launchCount)
            writeLong(app.firstInstallTime)
            writeBoolean(app.isSystemApp)
            writeNullableInt(app.userHandleId)
            writeNullableString(app.componentName)
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
    }
}
