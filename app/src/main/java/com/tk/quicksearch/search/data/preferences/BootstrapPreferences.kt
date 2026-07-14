package com.tk.quicksearch.search.data.preferences

import android.content.Context

/**
 * Tiny preference file containing only values that are safe to read before the first frame.
 *
 * Legacy values are retained for one release so downgrades and older backup files continue to
 * work. The process cache also prevents attachBaseContext() and onCreate() from opening the file
 * twice during a cold launch.
 */
object BootstrapPreferences {
    private const val PREFS_NAME = "bootstrap_preferences"
    private const val KEY_SCHEMA_VERSION = "schema_version"
    private const val KEY_APP_LANGUAGE_TAG = "app_language_tag"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_DEFAULT_HOME_HINT = "default_home_hint"
    private const val SCHEMA_VERSION = 1

    @Volatile
    private var cachedLanguageLoaded = false

    @Volatile
    private var cachedLanguageTag: String? = null

    fun getAppLanguageTag(context: Context): String? {
        if (cachedLanguageLoaded) return cachedLanguageTag
        return synchronized(this) {
            if (cachedLanguageLoaded) return@synchronized cachedLanguageTag
            val appContext = context.applicationContext
            val bootstrap = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val value =
                if (bootstrap.contains(KEY_APP_LANGUAGE_TAG)) {
                    bootstrap.getString(KEY_APP_LANGUAGE_TAG, null)
                } else {
                    // This is the only migration allowed on the pre-onCreate path: locale is
                    // needed to construct correctly localized resources.
                    val legacy =
                        appContext
                            .getSharedPreferences(BasePreferences.PREFS_NAME, Context.MODE_PRIVATE)
                            .getString(BasePreferences.KEY_APP_LANGUAGE_TAG, null)
                    bootstrap.edit()
                        .putString(KEY_APP_LANGUAGE_TAG, legacy)
                        .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
                        .commit()
                    legacy
                }
            cachedLanguageTag = value?.takeIf { it.isNotBlank() }
            cachedLanguageLoaded = true
            cachedLanguageTag
        }
    }

    fun setAppLanguageTag(context: Context, languageTag: String?) {
        val normalized = languageTag?.takeIf { it.isNotBlank() }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE_TAG, normalized)
            .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            .commit()
        cachedLanguageTag = normalized
        cachedLanguageLoaded = true
    }

    fun isFirstLaunch(context: Context): Boolean {
        val appContext = context.applicationContext
        val bootstrap = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (bootstrap.contains(KEY_FIRST_LAUNCH)) {
            return bootstrap.getBoolean(KEY_FIRST_LAUNCH, true)
        }
        // The dedicated first-launch file is intentionally not restored from backup and is tiny.
        // Do not consult PackageManager or the central preference file before first draw.
        val launchState =
            appContext.getSharedPreferences(BasePreferences.FIRST_LAUNCH_PREFS_NAME, Context.MODE_PRIVATE)
        val value =
            if (launchState.contains(BasePreferences.KEY_FIRST_LAUNCH)) {
                launchState.getBoolean(BasePreferences.KEY_FIRST_LAUNCH, true)
            } else {
                true
            }
        bootstrap.edit()
            .putBoolean(KEY_FIRST_LAUNCH, value)
            .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            .commit()
        return value
    }

    fun setFirstLaunch(context: Context, isFirstLaunch: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch)
            .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            .apply()
    }

    fun getDefaultHomeHint(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEFAULT_HOME_HINT, false)

    fun setDefaultHomeHint(context: Context, isDefaultHome: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEFAULT_HOME_HINT, isDefaultHome)
            .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            .apply()
    }

    internal fun clearProcessCacheForTest() {
        cachedLanguageTag = null
        cachedLanguageLoaded = false
    }
}
