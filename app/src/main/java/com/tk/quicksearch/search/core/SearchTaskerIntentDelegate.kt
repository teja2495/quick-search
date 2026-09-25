package com.tk.quicksearch.search.core

import android.content.Context
import android.content.Intent
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.hasTriggerAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.tools.aiSearch.AiSearchHandler
import com.tk.quicksearch.tools.tasker.TaskerIntegration
import com.tk.quicksearch.tools.tasker.TaskerIntentTool
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class SearchTaskerIntentDelegate(
    private val scope: CoroutineScope,
    private val appContext: Context,
    private val userPreferences: UserAppPreferences,
    private val aliasHandler: () -> AliasHandler,
    private val aiSearchHandler: () -> AiSearchHandler,
    private val featureStateProvider: () -> SearchFeatureState,
    private val currentQueryProvider: () -> String,
    private val lockedTaskerIntentIdProvider: () -> String?,
    private val updateFeatureState: ((SearchFeatureState) -> SearchFeatureState) -> Unit,
    private val clearQuery: () -> Unit,
    private val showToast: (String) -> Unit,
) {
    fun executeCustomToolSearch(toolId: String?) {
        val tool = featureStateProvider().customTools.find { it.id == toolId } ?: return
        val query = currentQueryProvider().trim()
        if (query.isBlank()) return
        aiSearchHandler().requestCustomToolSearch(query, tool.prompt, tool.providerId, tool.modelId, tool.groundingEnabled, tool.thinkingEnabled, tool.advancedPayload?.takeIf { tool.advancedPayloadEnabled })
    }

    fun executeTaskerIntent(toolId: String?) {
        val tool = featureStateProvider().taskerIntentTools.find { it.id == toolId } ?: return
        val query = currentQueryProvider().trim()
        if (query.isBlank()) return
        if (runCatching { appContext.packageManager.getPackageInfo(TaskerIntegration.PACKAGE_NAME, 0) }.isFailure) {
            showToast(appContext.getString(R.string.tasker_not_installed))
            return
        }
        runCatching {
            appContext.sendBroadcast(Intent(tool.broadcastAction).apply {
                setPackage(TaskerIntegration.PACKAGE_NAME)
                putExtra(TaskerIntegration.QUERY_EXTRA, query)
                putExtra(TaskerIntegration.SOURCE_EXTRA, TaskerIntegration.SOURCE_VALUE)
            })
        }.onSuccess { clearQuery() }.onFailure { showToast(appContext.getString(R.string.tasker_broadcast_failed)) }
    }

    fun addTaskerIntentTool(alias: String, name: String, broadcastAction: String) = scope.launch(Dispatchers.IO) {
        val normalizedAlias = normalizeShortcutCodeInput(alias)
        val currentAliasHandler = aliasHandler()
        if (hasExactAliasConflict(normalizedAlias, currentAliasHandler.reloadFromPreferences().shortcutCodes) || hasTriggerAliasConflict(normalizedAlias, userPreferences.getAllTriggerWordsById().values)) {
            showToast(appContext.getString(R.string.tasker_alias_conflict))
            return@launch
        }
        val id = TaskerIntegration.TOOL_ID_PREFIX + UUID.randomUUID()
        val updated = userPreferences.getTaskerIntentTools() + TaskerIntentTool(id, name.trim(), broadcastAction.trim())
        userPreferences.setTaskerIntentTools(updated)
        userPreferences.setAliasCode(id, normalizedAlias)
        updateFeatureStateWithAliases(updated)
    }

    fun deleteTaskerIntentTool(id: String) = scope.launch(Dispatchers.IO) {
        val updated = userPreferences.getTaskerIntentTools().filterNot { it.id == id }
        userPreferences.setTaskerIntentTools(updated)
        userPreferences.clearAliasCode(id)
        updateFeatureStateWithAliases(updated)
        if (lockedTaskerIntentIdProvider() == id) clearQuery()
    }

    private fun updateFeatureStateWithAliases(updatedTools: List<TaskerIntentTool>) {
        val aliases = aliasHandler().reloadFromPreferences()
        updateFeatureState { it.copy(taskerIntentTools = updatedTools, shortcutCodes = aliases.shortcutCodes, shortcutEnabled = aliases.shortcutEnabled) }
    }
}
