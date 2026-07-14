package com.tk.quicksearch.settings.tasker

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.searchEngines.AliasValidator.hasExactAliasConflict
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import com.tk.quicksearch.tools.tasker.TaskerIntentTool
import com.tk.quicksearch.tools.tasker.TaskerIntegration

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
    val normalizedAlias = normalizeShortcutCodeInput(alias)
    val aliasConflict = hasExactAliasConflict(alias, existingAliases)
    val actionValid = action.isNotBlank() && action.none(Char::isWhitespace)
    val canAdd = isValidGeneralAliasCode(alias) && !aliasConflict && name.isNotBlank() && actionValid
    val taskerIcon = rememberAppIcon(TaskerIntegration.PACKAGE_NAME).bitmap

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                Text(
                    stringResource(R.string.tasker_add_intent_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.tasker_add_intent_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.tasker_intent_name_label)) },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                )
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_custom_tool_alias_label)) },
                    supportingText = if (aliasConflict) {
                        { Text(stringResource(R.string.tasker_alias_conflict)) }
                    } else {
                        null
                    },
                    isError = aliasConflict,
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                )
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.tasker_broadcast_action_label)) },
                    placeholder = { Text("com.example.MY_TASK") },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                )
                Button(
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

        tools.forEach { tool ->
            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    if (taskerIcon != null) {
                        Image(
                            bitmap = taskerIcon,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
                        Text(
                            tool.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.tasker_saved_alias, existingAliases[tool.id].orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            tool.broadcastAction,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(tool.id) }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.tasker_delete_intent),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
