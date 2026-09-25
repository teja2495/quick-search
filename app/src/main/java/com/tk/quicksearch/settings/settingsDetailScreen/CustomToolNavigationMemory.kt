package com.tk.quicksearch.settings.settingsDetailScreen

object CustomToolNavigationMemory {
    private var pendingToolId: String? = null
    private var pendingAiBackedTool: AiBackedToolConfigId? = null

    fun setPendingToolId(toolId: String?) {
        pendingToolId = toolId
        pendingAiBackedTool = null
    }

    fun setPendingAiBackedTool(tool: AiBackedToolConfigId) {
        pendingAiBackedTool = tool
        pendingToolId = null
    }

    fun peekPendingToolId(): String? = pendingToolId
    fun peekPendingAiBackedTool(): AiBackedToolConfigId? = pendingAiBackedTool

    fun consumePendingToolId(): String? {
        val value = pendingToolId
        pendingToolId = null
        return value
    }

    fun consumePendingAiBackedTool(): AiBackedToolConfigId? {
        val value = pendingAiBackedTool
        pendingAiBackedTool = null
        return value
    }
}

enum class AiBackedToolConfigId {
    CURRENCY_CONVERTER,
    WORD_CLOCK,
    DICTIONARY,
    WEATHER,
}
