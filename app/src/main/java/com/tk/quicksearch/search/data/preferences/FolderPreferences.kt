package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.search.folders.AppFolder
import org.json.JSONArray
import org.json.JSONObject

/**
 * Preferences for Pinned-tab app folders, stored as one JSON array.
 */
class FolderPreferences(
    context: Context,
) : BasePreferences(context) {
    fun getAppFolders(): List<AppFolder> {
        val stored = prefs.getString(BasePreferences.KEY_APP_FOLDERS, null)
        if (stored.isNullOrBlank()) return emptyList()
        val jsonArray = runCatching { JSONArray(stored) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val folderJson = jsonArray.optJSONObject(index) ?: continue
                val id = folderJson.optString(KEY_ID).takeIf(String::isNotBlank) ?: continue
                val membersJson = folderJson.optJSONArray(KEY_MEMBERS) ?: JSONArray()
                val memberKeys =
                    buildList {
                        for (memberIndex in 0 until membersJson.length()) {
                            membersJson.optString(memberIndex).takeIf(String::isNotBlank)?.let(::add)
                        }
                    }.distinct()
                add(
                    AppFolder(
                        id = id,
                        name = folderJson.optString(KEY_NAME),
                        memberKeys = memberKeys,
                    ),
                )
            }
        }.distinctBy { it.id }
    }

    fun setAppFolders(folders: List<AppFolder>) {
        if (folders.isEmpty()) {
            prefs.edit().remove(BasePreferences.KEY_APP_FOLDERS).apply()
            return
        }
        val jsonArray = JSONArray()
        folders.forEach { folder ->
            jsonArray.put(
                JSONObject()
                    .put(KEY_ID, folder.id)
                    .put(KEY_NAME, folder.name)
                    .put(KEY_MEMBERS, JSONArray(folder.memberKeys)),
            )
        }
        prefs.edit().putString(BasePreferences.KEY_APP_FOLDERS, jsonArray.toString()).apply()
    }

    private companion object {
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_MEMBERS = "members"
    }
}
