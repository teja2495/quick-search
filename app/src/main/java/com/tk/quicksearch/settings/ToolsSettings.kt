package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
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
                                        enabled = isAvailable,
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
        }
    }
}
