package com.tk.quicksearch.tools.tasker

data class TaskerIntentTool(
    val id: String,
    val name: String,
    val broadcastAction: String,
)

object TaskerIntegration {
    const val PACKAGE_NAME = "net.dinglisch.android.taskerm"
    const val TOOL_ID_PREFIX = "tasker_intent:"
    const val QUERY_EXTRA = "query"
    const val SOURCE_EXTRA = "source"
    const val SOURCE_VALUE = "quick_search"
}
