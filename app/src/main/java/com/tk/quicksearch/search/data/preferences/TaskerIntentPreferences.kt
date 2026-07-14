package com.tk.quicksearch.search.data.preferences

import android.content.Context
import com.tk.quicksearch.tools.tasker.TaskerIntentTool
import org.json.JSONArray
import org.json.JSONObject

class TaskerIntentPreferences(context: Context) : BasePreferences(context) {
    fun getTools(): List<TaskerIntentTool> {
        val stored = prefs.getString(KEY_TASKER_INTENTS, null)
        if (stored.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(stored)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val name = item.optString("name")
                    val action = item.optString("broadcastAction")
                    if (id.isBlank() || name.isBlank() || action.isBlank()) continue
                    add(TaskerIntentTool(id = id, name = name, broadcastAction = action))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun setTools(tools: List<TaskerIntentTool>) {
        val array = JSONArray()
        tools.forEach { tool ->
            array.put(
                JSONObject().apply {
                    put("id", tool.id)
                    put("name", tool.name)
                    put("broadcastAction", tool.broadcastAction)
                },
            )
        }
        prefs.edit().putString(KEY_TASKER_INTENTS, array.toString()).apply()
    }

    private companion object {
        const val KEY_TASKER_INTENTS = "tasker_intent_tools"
    }
}
