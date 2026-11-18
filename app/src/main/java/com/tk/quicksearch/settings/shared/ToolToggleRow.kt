package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.settings.searchEngineSettings.AliasDisplayType
import com.tk.quicksearch.settings.searchEngineSettings.AliasCodeDisplay
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile

data class ToolToggleCardModel(
    val title: String,
    val subtitle: String? = null,
    val isBeta: Boolean = false,
    val tagLabel: String? = null,
    val enabled: Boolean = true,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val leadingIcon: ImageVector? = null,
    val aliasCode: String? = null,
    val onAliasCodeChange: ((String) -> Unit)? = null,
    val existingShortcuts: Map<String, String> = emptyMap(),
    val aliasFeatureId: String? = null,
    val onRowClick: (() -> Unit)? = null,
    val allowAliasClear: Boolean = true,
)

@Composable
fun ToolToggleRows(
    tools: List<ToolToggleCardModel>,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
) {
    // With a [scrollState], the tiles scroll inside the group; give [modifier] a bounded height.
    // The group is clipped to the outer corners so tiles scrolling past its edge stay rounded.
    SegmentedGroup(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(AppShapes.segmentGroup)
                .then(if (scrollState != null) Modifier.verticalScroll(scrollState) else Modifier),
    ) {
        tools.forEachIndexed { index, tool ->
            SegmentedTile(index = index, count = tools.size) {
                ToolToggleRow(tool = tool)
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
        enabled = tool.enabled,
        modifier = modifier,
        subtitle = tool.subtitle,
        leadingIcon = tool.leadingIcon,
        isBeta = tool.isBeta,
        tagLabel = tool.tagLabel,
        onRowClick = tool.onRowClick,
        footerContent =
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
                        allowClearAction = tool.allowAliasClear,
                        allowEmptyAlias = tool.allowAliasClear,
                    )
                }
            } else {
                null
            },
    )
}
