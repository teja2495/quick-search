package com.tk.quicksearch.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tk.quicksearch.model.AppInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent caching of app list to enable instant loading on app startup.
 * Uses SharedPreferences to store app data as JSON for efficient serialization.
 */
class AppCache(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Loads cached app list from SharedPreferences.
     * @return List of cached apps, or null if no cache exists or if cache is corrupted.
     */
    fun loadCachedApps(): List<AppInfo>? {
        return runCatching {
            val json = prefs.getString(KEY_APP_LIST, null) ?: return null
            JSONArray(json).toAppInfoList()
        }.onFailure { exception ->
            Log.e(TAG, "Failed to load cached apps", exception)
        }.getOrNull()
    }

    /**
     * Saves app list to SharedPreferences as JSON.
     * @param apps The list of apps to cache.
     * @return true if the save operation succeeded, false otherwise.
     */
    fun saveApps(apps: List<AppInfo>): Boolean {
        return runCatching {
            val json = apps.toJsonArray().toString()
            prefs.edit()
                .putString(KEY_APP_LIST, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
            true
        }.onFailure { exception ->
            Log.e(TAG, "Failed to save apps to cache", exception)
        }.getOrDefault(false)
    }
 
    /**
     * Returns the timestamp when the cache was last updated.
     * @return Timestamp in milliseconds, or 0L if cache has never been updated.
     */
    fun getLastUpdateTime(): Long {
        return prefs.getLong(KEY_LAST_UPDATE, 0L)
    }

    /**
     * Clears all cached data.
     */
    fun clearCache() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val TAG = "AppCache"
        private const val PREFS_NAME = "app_cache"
        private const val KEY_APP_LIST = "app_list"
        private const val KEY_LAST_UPDATE = "last_update"

        // JSON field names
        private const val FIELD_APP_NAME = "appName"
        private const val FIELD_PACKAGE_NAME = "packageName"
        private const val FIELD_LAST_USED_TIME = "lastUsedTime"
        private const val FIELD_TOTAL_TIME_IN_FOREGROUND = "totalTimeInForeground"
        private const val FIELD_LAUNCH_COUNT = "launchCount"
        private const val FIELD_IS_SYSTEM_APP = "isSystemApp"

        /**
         * Converts a JSONArray to a List<AppInfo>.
         */
        private fun JSONArray.toAppInfoList(): List<AppInfo> {
            return List(length()) { index ->
                val jsonObject = getJSONObject(index)
                AppInfo(
                    appName = jsonObject.getString(FIELD_APP_NAME),
                    packageName = jsonObject.getString(FIELD_PACKAGE_NAME),
                    lastUsedTime = jsonObject.getLong(FIELD_LAST_USED_TIME),
                    totalTimeInForeground = jsonObject.optLong(FIELD_TOTAL_TIME_IN_FOREGROUND, 0L),
                    launchCount = jsonObject.optInt(FIELD_LAUNCH_COUNT, 0),
                    isSystemApp = jsonObject.getBoolean(FIELD_IS_SYSTEM_APP)
                )
            }
        }

        /**
         * Converts a List<AppInfo> to a JSONArray.
         */
        private fun List<AppInfo>.toJsonArray(): JSONArray {
            return JSONArray().apply {
                forEach { app ->
                    put(
                        JSONObject().apply {
                            put(FIELD_APP_NAME, app.appName)
                            put(FIELD_PACKAGE_NAME, app.packageName)
                            put(FIELD_LAST_USED_TIME, app.lastUsedTime)
                            put(FIELD_TOTAL_TIME_IN_FOREGROUND, app.totalTimeInForeground)
                            put(FIELD_LAUNCH_COUNT, app.launchCount)
                            put(FIELD_IS_SYSTEM_APP, app.isSystemApp)
                        }
                    )
                }
            }
        }
    }
}


