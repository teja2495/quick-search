package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.data.appShortcutRepository.allAppShortcutsKey
import com.tk.quicksearch.search.data.assets.ManagedAssetStore

/**
 * Preferences for app shortcut settings such as pinned and excluded shortcuts.
 */
class AppShortcutPreferences(
    context: Context,
) : BasePreferences(context) {
    private val assetStore = ManagedAssetStore(context)

    fun getPinnedAppShortcutIds(): Set<String> = getPinnedStringItems(BasePreferences.KEY_PINNED_APP_SHORTCUTS)

    fun getPinnedAppShortcutOrder(): List<String> = getStringListPref(BasePreferences.KEY_PINNED_APP_SHORTCUT_ORDER)

    fun setPinnedAppShortcutOrder(order: List<String>): List<String> =
        order.distinct().also { setStringListPref(BasePreferences.KEY_PINNED_APP_SHORTCUT_ORDER, it) }

    fun getExcludedAppShortcutIds(): Set<String> = getExcludedStringItems(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS)

    fun getDisabledAppShortcutIds(): Set<String> = getStringSet(BasePreferences.KEY_DISABLED_APP_SHORTCUTS)

    fun pinAppShortcut(id: String): Set<String> =
        pinStringItem(BasePreferences.KEY_PINNED_APP_SHORTCUTS, id).also {
            if (id !in getPinnedAppShortcutOrder()) {
                setPinnedAppShortcutOrder(getPinnedAppShortcutOrder() + id)
            }
        }

    fun unpinAppShortcut(id: String): Set<String> =
        unpinStringItem(BasePreferences.KEY_PINNED_APP_SHORTCUTS, id).also {
            setPinnedAppShortcutOrder(getPinnedAppShortcutOrder().filterNot { orderedId -> orderedId == id })
        }

    fun excludeAppShortcut(id: String): Set<String> = excludeStringItem(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS, id)

    fun removeExcludedAppShortcut(id: String): Set<String> = removeExcludedStringItem(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS, id)

    fun clearAllExcludedAppShortcuts(): Set<String> = clearAllExcludedStringItems(BasePreferences.KEY_EXCLUDED_APP_SHORTCUTS)

    fun setAppShortcutEnabled(
        id: String,
        enabled: Boolean,
    ): Set<String> =
        updateStringSet(BasePreferences.KEY_DISABLED_APP_SHORTCUTS) { disabledIds ->
            if (enabled) {
                disabledIds.remove(id)
            } else {
                disabledIds.add(id)
            }
        }

    fun setAppShortcutsEnabled(
        ids: Collection<String>,
        enabled: Boolean,
    ): Set<String> =
        updateStringSet(BasePreferences.KEY_DISABLED_APP_SHORTCUTS) { disabledIds ->
            if (enabled) {
                disabledIds.removeAll(ids.toSet())
            } else {
                disabledIds.addAll(ids)
            }
        }

    /**
     * Toggles only the app-wide marker. Individual choices are never touched, so enabling brings
     * back each shortcut's previous state.
     */
    fun setAllAppShortcutsEnabled(
        packageName: String,
        enabled: Boolean,
    ): Set<String> =
        updateStringSet(BasePreferences.KEY_DISABLED_APP_SHORTCUTS) { disabledIds ->
            if (enabled) {
                disabledIds.remove(allAppShortcutsKey(packageName))
            } else {
                disabledIds.add(allAppShortcutsKey(packageName))
            }
        }

    /**
     * Enables [id] even when its whole app is disabled. The app-wide marker would otherwise keep
     * hiding it, so it is replaced by individual entries for [otherAppShortcutIds].
     */
    fun enableAppShortcutOverridingApp(
        packageName: String,
        id: String,
        otherAppShortcutIds: Collection<String>,
    ): Set<String> =
        updateStringSet(BasePreferences.KEY_DISABLED_APP_SHORTCUTS) { disabledIds ->
            if (disabledIds.remove(allAppShortcutsKey(packageName))) {
                disabledIds.addAll(otherAppShortcutIds)
            }
            disabledIds.remove(id)
        }

    /**
     * Icons live in [ManagedAssetStore]. A copy in preferences comes from an older build or a
     * restored backup, so it wins and is moved to the asset store.
     */
    fun getAppShortcutIconOverride(id: String): String? {
        val pending = prefs.getString(iconOverrideKey(id), null)?.takeIf { it.isNotBlank() }
        if (pending != null) {
            synchronized(iconOverrideLock) {
                moveToAssetStore(mapOf(id to pending))
                cachedIconOverrides = cachedIconOverrides?.plus(id to pending)
            }
            return pending
        }
        return assetStore.getBase64(assetId(id))
    }

    /**
     * All icon overrides, keyed by shortcut key. Reading them means one file read and encode per
     * icon, so the map is kept in memory for the process and updated by every writer.
     */
    fun getAllAppShortcutIconOverrides(): Map<String, String> {
        cachedIconOverrides?.let { return it }
        synchronized(iconOverrideLock) {
            cachedIconOverrides?.let { return it }
            val prefix = BasePreferences.KEY_APP_SHORTCUT_ICON_OVERRIDE_PREFIX
            val pending = mutableMapOf<String, String>()
            for ((key, value) in prefs.all) {
                if (!key.startsWith(prefix)) continue
                if (value !is String || value.isBlank()) continue
                pending[key.removePrefix(prefix)] = value
            }
            val loaded = assetStore.getAllBase64(ManagedAssetStore.SHORTCUT_ICON_PREFIX) + pending
            moveToAssetStore(pending)
            cachedIconOverrides = loaded
            return loaded
        }
    }

    fun setAppShortcutIconOverride(
        id: String,
        iconBase64: String?,
    ) {
        val key = iconOverrideKey(id)
        synchronized(iconOverrideLock) {
            // commit() so a follow-up read (e.g. refreshAppShortcutsState) sees the new value;
            // apply() is async and can race with immediate UI refresh.
            if (iconBase64.isNullOrBlank()) {
                assetStore.remove(assetId(id))
                if (prefs.contains(key)) prefs.edit().remove(key).commit()
                cachedIconOverrides = cachedIconOverrides?.minus(id)
            } else {
                if (!assetStore.putBase64(assetId(id), iconBase64)) return
                if (prefs.contains(key)) prefs.edit().remove(key).commit()
                cachedIconOverrides = cachedIconOverrides?.plus(id to iconBase64)
            }
        }
    }

    /** Moves preference copies into the asset store; one that can't be stored stays where it is. */
    private fun moveToAssetStore(icons: Map<String, String>) {
        if (icons.isEmpty()) return
        val moved = icons.filter { (id, icon) -> assetStore.putBase64(assetId(id), icon) }.keys
        if (moved.isNotEmpty()) prefs.edit().apply { moved.forEach { remove(iconOverrideKey(it)) } }.commit()
    }

    private fun assetId(id: String): String = "${ManagedAssetStore.SHORTCUT_ICON_PREFIX}$id"

    private fun iconOverrideKey(id: String): String =
        "${BasePreferences.KEY_APP_SHORTCUT_ICON_OVERRIDE_PREFIX}$id"

    companion object {
        private val iconOverrideLock = Any()

        @Volatile
        private var cachedIconOverrides: Map<String, String>? = null

        /** Drops the in-memory overrides after their stored copy was replaced, e.g. by a restore. */
        fun invalidateIconOverrideCache() {
            synchronized(iconOverrideLock) { cachedIconOverrides = null }
        }
    }
}
