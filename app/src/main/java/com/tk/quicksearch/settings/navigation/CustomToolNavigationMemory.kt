package com.tk.quicksearch.settings.settingsDetailScreen

object CustomToolNavigationMemory {
    private var pendingToolId: String? = null

    fun setPendingToolId(toolId: String?) {
        pendingToolId = toolId
    }

    fun consumePendingToolId(): String? {
        val value = pendingToolId
        pendingToolId = null
        return value
    }
}
