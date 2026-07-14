package com.tk.quicksearch.search.data.preferences

import android.content.Context
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

    fun getAppShortcutIconOverride(id: String): String? =
        assetStore.getBase64("${ManagedAssetStore.SHORTCUT_ICON_PREFIX}$id")
            ?: prefs.getString(iconOverrideKey(id), null)?.takeIf { it.isNotBlank() }?.also {
                assetStore.putBase64("${ManagedAssetStore.SHORTCUT_ICON_PREFIX}$id", it)
            }

    fun getAllAppShortcutIconOverrides(): Map<String, String> {
        val prefix = BasePreferences.KEY_APP_SHORTCUT_ICON_OVERRIDE_PREFIX
        val out = mutableMapOf<String, String>()
        for ((key, value) in prefs.all) {
            if (!key.startsWith(prefix)) continue
            if (value !is String || value.isBlank()) continue
            out[key.removePrefix(prefix)] = value
        }
        return out + assetStore.getAllBase64(ManagedAssetStore.SHORTCUT_ICON_PREFIX)
    }

    fun setAppShortcutIconOverride(
        id: String,
        iconBase64: String?,
    ) {
        val key = iconOverrideKey(id)
        val assetId = "${ManagedAssetStore.SHORTCUT_ICON_PREFIX}$id"
        // commit() so a follow-up read (e.g. refreshAppShortcutsState) sees the new value;
        // apply() is async and can race with immediate UI refresh.
        if (iconBase64.isNullOrBlank()) {
            assetStore.remove(assetId)
            prefs.edit().remove(key).commit()
        } else {
            if (!assetStore.putBase64(assetId, iconBase64)) return
            prefs.edit().putString(key, iconBase64).commit()
        }
    }

    private fun iconOverrideKey(id: String): String =
        "${BasePreferences.KEY_APP_SHORTCUT_ICON_OVERRIDE_PREFIX}$id"
}
