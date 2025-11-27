package com.tk.quicksearch.data

import android.content.Context
import android.content.SharedPreferences
import com.tk.quicksearch.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages persistent caching of app list to enable instant loading on app startup.
 */
class AppCache(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Loads cached app list from SharedPreferences.
     * Returns null if no cache exists or if cache is corrupted.
     */
    suspend fun loadCachedApps(): List<AppInfo>? = withContext(Dispatchers.IO) {
        runCatching {
            val json = prefs.getString(KEY_APP_LIST, null) ?: return@withContext null
            val jsonArray = JSONArray(json)
            
            List(jsonArray.length()) { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                AppInfo(
                    appName = jsonObject.getString("appName"),
                    packageName = jsonObject.getString("packageName"),
                    lastUsedTime = jsonObject.getLong("lastUsedTime"),
                    isSystemApp = jsonObject.getBoolean("isSystemApp")
                )
            }
        }.getOrNull()
    }

    /**
     * Saves app list to SharedPreferences as JSON.
     */
    suspend fun saveApps(apps: List<AppInfo>) = withContext(Dispatchers.IO) {
        runCatching {
            val jsonArray = JSONArray()
            apps.forEach { app ->
                val jsonObject = JSONObject().apply {
                    put("appName", app.appName)
                    put("packageName", app.packageName)
                    put("lastUsedTime", app.lastUsedTime)
                    put("isSystemApp", app.isSystemApp)
                }
                jsonArray.put(jsonObject)
            }
            
            prefs.edit()
                .putString(KEY_APP_LIST, jsonArray.toString())
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
        }
    }

    /**
     * Returns the timestamp when the cache was last updated.
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
        private const val PREFS_NAME = "app_cache"
        private const val KEY_APP_LIST = "app_list"
        private const val KEY_LAST_UPDATE = "last_update"
    }
}


