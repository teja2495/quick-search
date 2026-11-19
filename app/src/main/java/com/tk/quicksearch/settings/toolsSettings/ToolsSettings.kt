package com.tk.quicksearch.settings.toolsSettings

import com.tk.quicksearch.settings.aiSettings.AiApiKeyRequiredDialog
import com.tk.quicksearch.shared.ui.components.AppExtendedFloatingActionButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsSegmentNavigationRow
import com.tk.quicksearch.settings.shared.ToolToggleCardModel
import com.tk.quicksearch.settings.shared.ToolToggleRows
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.tasker.TaskerIntegration

@Composable
internal fun ToolsSettingsSection(
        toolStates: Map<String, ToolSettingUiState>,
        hasApiKey: Boolean,
        existingShortcuts: Map<String, String>,
        onToolAliasChange: (String, String) -> Unit,
        onToolToggle: (String, Boolean) -> Unit,
        onToolInfoClick: (String) -> Unit,
        onToolConfigureClick: (String) -> Unit = {},
        onNavigateToLlmApiSetup: () -> Unit = {},
        showTaskerIntegration: Boolean = false,
        onNavigateToTaskerIntegration: () -> Unit = {},
        customTools: List<CustomTool> = emptyList(),
        disabledCustomToolIds: Set<String> = emptySet(),
        customToolAliases: Map<String, String> = emptyMap(),
        onCustomToolToggle: (String, Boolean) -> Unit = { _, _ -> },
        onCustomToolAliasChange: (String, String) -> Unit = { _, _ -> },
        onCustomToolClick: (String) -> Unit = {},
        onCreateNewTool: () -> Unit = {},
        modifier: Modifier = Modifier,
        scrollState: androidx.compose.foundation.ScrollState =
                androidx.compose.foundation.rememberScrollState(),
) {
    val taskerIcon = rememberAppIcon(TaskerIntegration.PACKAGE_NAME).bitmap
    var showApiKeyRequiredDialog by remember { mutableStateOf(false) }

    val builtInToolRows =
            ToolSettingsRegistry.definitions.map { definition ->
                val toolState =
                        toolStates[definition.id]
                                ?: ToolSettingUiState(
                                        enabled = false,
                                        aliasCode = "",
                                )
                val isAvailable = !definition.requiresLlmApiKey || hasApiKey
                val subtitleResId =
                        if (isAvailable) {
                            definition.defaultDescriptionResId
                        } else {
                            definition.requiresLlmApiKeyDescriptionResId
                                    ?: definition.defaultDescriptionResId
                        }

                // Without an API key the row still looks normal; any press opens the key dialog.
                val showKeyDialog = { showApiKeyRequiredDialog = true }
                ToolToggleCardModel(
                        title = stringResource(definition.titleResId),
                        subtitle = stringResource(subtitleResId),
                        enabled = true,
                        checked = toolState.enabled && isAvailable,
                        onCheckedChange = { enabled ->
                            if (isAvailable) onToolToggle(definition.id, enabled) else showKeyDialog()
                        },
                        leadingIcon = definition.icon,
                        aliasCode =
                                if (isAvailable && definition.aliasFeatureId != null) {
                                    toolState.aliasCode
                                } else {
                                    null
                                },
                        onAliasCodeChange = { alias ->
                            if (definition.aliasFeatureId != null) {
                                onToolAliasChange(definition.id, alias)
                            }
                        },
                        existingShortcuts = existingShortcuts,
                        aliasFeatureId = definition.aliasFeatureId,
                        onRowClick =
                                if (definition.aiBackedModelConfigurable ||
                                                definition.infoDestination != null
                                ) {
                                    {
                                        if (!isAvailable) {
                                            showKeyDialog()
                                        } else if (definition.aiBackedModelConfigurable) {
                                            onToolConfigureClick(definition.id)
                                        } else {
                                            onToolInfoClick(definition.id)
                                        }
                                    }
                                } else {
                                    null
                                },
                )
            }
    val customToolRows =
            customTools.map { tool ->
                ToolToggleCardModel(
                        title = tool.name,
                        subtitle = stringResource(R.string.settings_edit_label),
                        enabled = true,
                        checked = tool.id !in disabledCustomToolIds,
                        onCheckedChange = { enabled ->
                            onCustomToolToggle(tool.id, enabled)
                        },
                        leadingIcon = MaterialSymbols.Construction,
                        aliasCode = customToolAliases[tool.id].orEmpty(),
                        onAliasCodeChange = { code ->
                            onCustomToolAliasChange(tool.id, code)
                        },
                        existingShortcuts = existingShortcuts,
                        aliasFeatureId = tool.id,
                        allowAliasClear = false,
                        onRowClick = { onCustomToolClick(tool.id) },
                )
            }

    // The page doesn't scroll: the tools card takes the height left above the Create button and
    // scrolls its rows inside, shrinking to fit when they don't fill it.
    Column(
            modifier =
                    modifier.fillMaxSize()
                            .padding(
                                    start = DesignTokens.SpacingSmall,
                                    end = DesignTokens.SpacingSmall,
                                    bottom = DesignTokens.SpacingSmall,
                            ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        if (showTaskerIntegration) {
            SettingsSegmentNavigationRow(
                item = SettingsCardItem(
                    title = stringResource(R.string.tasker_integration_title),
                    description = stringResource(R.string.tasker_integration_description),
                    icon = MaterialSymbols.Bolt,
                    iconBitmap = taskerIcon,
                    actionOnPress = onNavigateToTaskerIntegration,
                ),
                index = 0,
                count = 1,
            )
        }

        ToolToggleRows(
                tools = builtInToolRows + customToolRows,
                modifier = Modifier.weight(1f, fill = false),
                scrollState = scrollState,
        )

        // Matches the new-item button in Calendar Events settings.
        AppExtendedFloatingActionButton(
                onClick = {
                    if (hasApiKey) onCreateNewTool() else showApiKeyRequiredDialog = true
                },
                text = stringResource(R.string.settings_create_new_tool_button),
                icon = MaterialSymbols.Add,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = DesignTokens.SpacingSmall),
        )
    }

    if (showApiKeyRequiredDialog) {
        AiApiKeyRequiredDialog(
                onDismiss = { showApiKeyRequiredDialog = false },
                onSetupKey = onNavigateToLlmApiSetup,
        )
    }
}
