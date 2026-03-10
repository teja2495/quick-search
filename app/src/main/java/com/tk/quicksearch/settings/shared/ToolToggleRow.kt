package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.settings.searchEnginesScreen.AliasDisplayType
import com.tk.quicksearch.settings.searchEnginesScreen.AliasCodeDisplay
import com.tk.quicksearch.shared.ui.theme.DesignTokens

data class ToolToggleCardModel(
    val title: String,
    val subtitle: String? = null,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val leadingIcon: ImageVector? = null,
    val aliasCode: String? = null,
    val onAliasCodeChange: ((String) -> Unit)? = null,
    val existingShortcuts: Map<String, String> = emptyMap(),
    val aliasFeatureId: String? = null,
)

@Composable
fun ToolToggleRows(
    tools: List<ToolToggleCardModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        tools.forEach { tool ->
            ToolToggleRow(tool = tool)
        }
    }
}

@Composable
fun ToolToggleRow(
    tool: ToolToggleCardModel,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            SettingsToggleRow(
                title = tool.title,
                subtitle = if (tool.aliasCode == null) tool.subtitle else null,
                subtitleContent =
                    if (tool.aliasCode != null) {
                        {
                            AliasCodeDisplay(
                                shortcutCode = tool.aliasCode,
                                isEnabled = true,
                                onCodeChange = tool.onAliasCodeChange,
                                engineName = tool.title,
                                existingShortcuts = tool.existingShortcuts,
                                currentShortcutId = tool.aliasFeatureId,
                                validateCode = { input -> isValidGeneralAliasCode(input) },
                                validateConflict = { input, existing ->
                                    !hasExactAliasConflict(input, existing)
                                },
                                conflictErrorMessage = stringResource(R.string.dialog_edit_alias_error_prefix),
                                aliasDisplayType = AliasDisplayType.TOOL,
                            )
                        }
                    } else {
                        null
                    },
                checked = tool.checked,
                onCheckedChange = tool.onCheckedChange,
                leadingIcon = tool.leadingIcon,
                isFirstItem = true,
                isLastItem = true,
                showDivider = false,
            )
        }
    }
}
