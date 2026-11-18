package com.tk.quicksearch.settings.tasker

import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.shared.ui.components.AppIconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.tasker.TaskerIntentTool
import com.tk.quicksearch.tools.tasker.TaskerIntegration
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppButton

@Composable
fun TaskerIntegrationScreen(
    tools: List<TaskerIntentTool>,
    existingAliases: Map<String, String>,
    onAdd: (String, String, String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var alias by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var action by rememberSaveable { mutableStateOf("") }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val normalizedAlias = normalizeShortcutCodeInput(alias)
    val aliasConflict = hasExactAliasConflict(alias, existingAliases)
    val actionValid = action.isNotBlank() && action.none(Char::isWhitespace)
    val canAdd = isValidGeneralAliasCode(alias) && !aliasConflict && name.isNotBlank() && actionValid
    val taskerIcon = rememberAppIcon(TaskerIntegration.PACKAGE_NAME).bitmap

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        SegmentedTile(index = 0, count = 1) {
            SegmentedTileContent {
                Text(
                    stringResource(R.string.tasker_add_intent_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(R.string.tasker_add_intent_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.tasker_intent_name_label),
                    singleLine = true,
                )
                AppOutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.settings_custom_tool_alias_label),
                    supportingText = if (aliasConflict) {
                        { Text(stringResource(R.string.tasker_alias_conflict)) }
                    } else {
                        null
                    },
                    isError = aliasConflict,
                    singleLine = true,
                )
                AppOutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.tasker_broadcast_action_label),
                    placeholder = { Text(stringResource(R.string.tasker_broadcast_action_placeholder)) },
                    singleLine = true,
                )
                AppButton(
                    onClick = {
                        onAdd(normalizedAlias, name.trim(), action.trim())
                        alias = ""
                        name = ""
                        action = ""
                    },
                    enabled = canAdd,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.tasker_add_intent_button))
                }
            }
        }

        if (tools.isNotEmpty()) {
            Spacer(Modifier.height(DesignTokens.SpacingSmall))
        }
        SegmentedGroup {
            tools.forEachIndexed { index, tool ->
                SegmentedTile(index = index, count = tools.size) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(
                            start = DesignTokens.SpacingLarge,
                            end = DesignTokens.SpacingXSmall,
                            top = DesignTokens.SpacingLarge,
                            bottom = DesignTokens.SpacingLarge,
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                    ) {
                        if (taskerIcon != null) {
                            Box(modifier = Modifier.size(TaskerLeadingSlotSize), contentAlignment = Alignment.Center) {
                                Image(
                                    bitmap = taskerIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(DesignTokens.IconSize),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        } else {
                            TonalIconBadge(icon = MaterialSymbols.Bolt)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
                            Text(
                                tool.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                stringResource(R.string.tasker_saved_alias, existingAliases[tool.id].orEmpty()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                tool.broadcastAction,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppIconButton(onClick = { pendingDeleteId = tool.id }, tooltip = stringResource(R.string.tasker_delete_intent)) {
                            Icon(
                                MaterialSymbols.DeleteOutline,
                                contentDescription = stringResource(R.string.tasker_delete_intent),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    tools.firstOrNull { it.id == pendingDeleteId }?.let { tool ->
        AppAlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.dialog_delete_item_title, tool.name)) },
            confirmLabel = stringResource(R.string.dialog_delete),
            onConfirm = {
                pendingDeleteId = null
                onDelete(tool.id)
            },
            dismissLabel = stringResource(R.string.dialog_cancel),
            destructive = true,
        )
    }
}

private val TaskerLeadingSlotSize = 40.dp
