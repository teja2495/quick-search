package com.tk.quicksearch.search.startup

import android.content.Context

class StartupSurfaceStore(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSnapshot(): StartupSurfaceSnapshot? =
        StartupSurfaceSnapshotJson.fromJson(prefs.getString(KEY_SNAPSHOT_JSON, null))

    fun saveSnapshot(snapshot: StartupSurfaceSnapshot) {
        prefs.edit().putString(KEY_SNAPSHOT_JSON, StartupSurfaceSnapshotJson.toJson(snapshot)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_SNAPSHOT_JSON).apply()
    }

    companion object {
        private const val PREFS_NAME = "startup_surface_store"
        private const val KEY_SNAPSHOT_JSON = "startup_surface_snapshot_json"
    }
}
