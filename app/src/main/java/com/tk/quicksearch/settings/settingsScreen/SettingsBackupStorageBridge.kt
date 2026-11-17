package com.tk.quicksearch.settings.settingsScreen

import android.content.Context
import com.tk.quicksearch.search.data.assets.ManagedAssetStore
import com.tk.quicksearch.search.data.preferences.AppShortcutPreferences
import com.tk.quicksearch.search.data.preferences.BasePreferences
import com.tk.quicksearch.search.data.preferences.SearchCustomizationStore
import java.io.Writer
import org.json.JSONArray
import org.json.JSONObject

/**
 * Keeps the backup format stable while some values live outside the central preference file:
 * shortcut icons and engine favicons in [ManagedAssetStore], and nicknames, triggers and alias
 * codes in [SearchCustomizationStore]. Backups still carry them as central preference keys.
 */
internal object SettingsBackupStorageBridge {
    private const val FAVICON_FIELD = "faviconBase64"

    /** The central preference entries as a backup carries them, with the moved values folded back in. */
    fun exportableUserPreferences(
        context: Context,
        stored: Map<String, *>,
    ): Map<String, Any?> {
        val entries = HashMap<String, Any?>(stored)
        SearchCustomizationStore(context).snapshot().forEach { (key, value) ->
            if (value is String) entries[key] = value
        }
        val assetStore = ManagedAssetStore(context)
        // A copy still in preferences hasn't been moved yet and is the newer one.
        assetStore.getAllBase64(ManagedAssetStore.SHORTCUT_ICON_PREFIX).forEach { (id, icon) ->
            entries.putIfAbsent("${BasePreferences.KEY_APP_SHORTCUT_ICON_OVERRIDE_PREFIX}$id", icon)
        }
        (entries[BasePreferences.KEY_CUSTOM_SEARCH_ENGINES] as? String)?.let { enginesJson ->
            entries[BasePreferences.KEY_CUSTOM_SEARCH_ENGINES] =
                inlineFavicons(enginesJson) { engineId ->
                    assetStore.getBase64("${ManagedAssetStore.SEARCH_ENGINE_ICON_PREFIX}$engineId")
                }
        }
        return entries
    }

    /**
     * Moves restored values to where the app reads them. The restored copies in preferences win
     * on the next read, so only stored assets the backup could leave stale are dropped here.
     */
    fun afterPreferencesImport(
        context: Context,
        fullRestore: Boolean,
        importedCustomEngines: Boolean,
    ) {
        SearchCustomizationStore.syncAfterImport(context, fullRestore)
        val assetStore = ManagedAssetStore(context)
        // Backups always carry every icon and favicon inline, so drop the ones they replace;
        // otherwise an icon removed in the backup would survive the restore.
        if (fullRestore) assetStore.removeAll(ManagedAssetStore.SHORTCUT_ICON_PREFIX)
        if (fullRestore || importedCustomEngines) {
            assetStore.removeAll(ManagedAssetStore.SEARCH_ENGINE_ICON_PREFIX)
        }
        AppShortcutPreferences.invalidateIconOverrideCache()
    }

    /** Fills each engine's blank favicon from [faviconFor]; returns [enginesJson] as is if it can't be read. */
    fun inlineFavicons(
        enginesJson: String,
        faviconFor: (engineId: String) -> String?,
    ): String {
        val engines = runCatching { JSONArray(enginesJson) }.getOrNull() ?: return enginesJson
        var changed = false
        for (index in 0 until engines.length()) {
            val engine = engines.optJSONObject(index) ?: continue
            if (engine.optString(FAVICON_FIELD).isNotBlank()) continue
            val engineId = engine.optString("id").takeIf { it.isNotBlank() } ?: continue
            val favicon = faviconFor(engineId) ?: continue
            engine.put(FAVICON_FIELD, favicon)
            changed = true
        }
        return if (changed) engines.toString() else enginesJson
    }

    /**
     * Writes `{"section": {"key": value, ...}, ...}` one entry at a time, leaving out sections
     * without entries.
     */
    fun writeSections(
        writer: Writer,
        sections: Sequence<Pair<String, Sequence<Pair<String, JSONObject>>>>,
    ) {
        writer.write("{")
        var firstSection = true
        sections.forEach { (sectionName, entries) ->
            var firstEntry = true
            entries.forEach { (key, value) ->
                if (firstEntry) {
                    writer.write(if (firstSection) "\n" else ",\n")
                    firstSection = false
                    writer.write(JSONObject.quote(sectionName))
                    writer.write(": {\n")
                } else {
                    writer.write(",\n")
                }
                firstEntry = false
                writer.write(JSONObject.quote(key))
                writer.write(": ")
                writer.write(value.toString())
            }
            if (!firstEntry) writer.write("\n}")
        }
        writer.write(if (firstSection) "}" else "\n}")
    }
}
