package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

data class ToolToggleCardModel(
    val title: String,
    val subtitle: String? = null,
    val isBeta: Boolean = false,
    val tagLabel: String? = null,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val leadingIcon: ImageVector? = null,
    val aliasCode: String? = null,
    val onAliasCodeChange: ((String) -> Unit)? = null,
    val existingShortcuts: Map<String, String> = emptyMap(),
    val aliasFeatureId: String? = null,
    val onRowClick: (() -> Unit)? = null,
)

@Composable
fun ToolToggleRows(
    tools: List<ToolToggleCardModel>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = AppColors.getCardElevation(false),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column {
            tools.forEachIndexed { index, tool ->
                ToolToggleRow(tool = tool)
                if (index != tools.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun ToolToggleRow(
    tool: ToolToggleCardModel,
    modifier: Modifier = Modifier,
) {
    SettingsNavigationToggleRow(
        title = tool.title,
        checked = tool.checked,
        onCheckedChange = tool.onCheckedChange,
        modifier = modifier,
        subtitle = tool.subtitle,
        leadingIcon = tool.leadingIcon,
        isBeta = tool.isBeta,
        tagLabel = tool.tagLabel,
        onRowClick = tool.onRowClick,
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
                        modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                    )
                }
            } else {
                null
            },
    )
}
