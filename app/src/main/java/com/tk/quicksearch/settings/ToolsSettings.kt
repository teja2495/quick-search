package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.ToolToggleCardModel
import com.tk.quicksearch.settings.shared.ToolToggleRows
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun ToolsSettingsSection(
        toolStates: Map<ToolSettingId, ToolSettingUiState>,
        hasGeminiApiKey: Boolean,
        existingShortcuts: Map<String, String>,
        onToolAliasChange: (ToolSettingId, String) -> Unit,
        onToolToggle: (ToolSettingId, Boolean) -> Unit,
        onToolInfoClick: (ToolSettingId) -> Unit,
        onNavigateToGeminiApiSetup: () -> Unit = {},
        customTools: List<CustomTool> = emptyList(),
        disabledCustomToolIds: Set<String> = emptySet(),
        customToolAliases: Map<String, String> = emptyMap(),
        onCustomToolToggle: (String, Boolean) -> Unit = { _, _ -> },
        onCustomToolAliasChange: (String, String) -> Unit = { _, _ -> },
        onCustomToolClick: (String) -> Unit = {},
        onCreateNewTool: () -> Unit = {},
        modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = DesignTokens.SpacingSmall),
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = DesignTokens.SpacingSmall),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            if (!hasGeminiApiKey) {
                SettingsCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsNavigationRow(
                            item =
                                    SettingsCardItem(
                                            title = stringResource(R.string.settings_tools_gemini_api_title),
                                            description =
                                                    stringResource(
                                                            R.string.settings_tools_gemini_api_desc
                                                    ),
                                            iconResId = R.drawable.direct_search,
                                            actionOnPress = onNavigateToGeminiApiSetup,
                                    ),
                            contentPadding =
                                    PaddingValues(
                                            horizontal = DesignTokens.CardHorizontalPadding,
                                            vertical = DesignTokens.CardVerticalPadding,
                                    ),
                    )
                }
            }
            ToolToggleRows(
                    tools =
                            ToolSettingsRegistry.definitions.map { definition ->
                                val toolState =
                                        toolStates[definition.id]
                                                ?: ToolSettingUiState(
                                                        enabled = false,
                                                        aliasCode = "",
                                                )
                                val isAvailable =
                                        !definition.requiresGeminiApiKey || hasGeminiApiKey
                                val subtitleResId =
                                        if (isAvailable) {
                                            definition.defaultDescriptionResId
                                        } else {
                                            definition.requiresGeminiDescriptionResId
                                                    ?: definition.defaultDescriptionResId
                                        }

                                ToolToggleCardModel(
                                        title = stringResource(definition.titleResId),
                                        subtitle = stringResource(subtitleResId),
                                        enabled = true,
                                        checked = toolState.enabled && isAvailable,
                                        onCheckedChange = { enabled ->
                                            onToolToggle(definition.id, enabled)
                                        },
                                        leadingIcon = definition.icon,
                                        aliasCode =
                                                if (isAvailable) {
                                                    toolState.aliasCode
                                                } else {
                                                    null
                                                },
                                        onAliasCodeChange = { alias ->
                                            onToolAliasChange(definition.id, alias)
                                        },
                                        existingShortcuts = existingShortcuts,
                                        aliasFeatureId = definition.aliasFeatureId,
                                        onRowClick =
                                                if (definition.infoDestination != null) {
                                                    { onToolInfoClick(definition.id) }
                                                } else {
                                                    null
                                                },
                                )
                            },
            )

            if (customTools.isNotEmpty()) {
                ToolToggleRows(
                        tools =
                                customTools.map { tool ->
                                    val isEnabled = tool.id !in disabledCustomToolIds
                                    val aliasCode = customToolAliases[tool.id].orEmpty()
                                    ToolToggleCardModel(
                                            title = tool.name,
                                            subtitle = stringResource(R.string.settings_edit_label),
                                            enabled = true,
                                            checked = isEnabled,
                                            onCheckedChange = { enabled ->
                                                onCustomToolToggle(tool.id, enabled)
                                            },
                                            leadingIcon = Icons.Rounded.Construction,
                                            aliasCode = aliasCode,
                                            onAliasCodeChange = { code ->
                                                onCustomToolAliasChange(tool.id, code)
                                            },
                                            existingShortcuts = existingShortcuts,
                                            aliasFeatureId = tool.id,
                                            allowAliasClear = false,
                                            onRowClick = { onCustomToolClick(tool.id) },
                                    )
                                },
                )
            }

            Box(
                    modifier =
                            Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center,
            ) {
                Button(onClick = onCreateNewTool) {
                    Text(text = stringResource(R.string.settings_create_new_tool_button))
                }
            }
        }
    }
}
