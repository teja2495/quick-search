package com.tk.quicksearch.search.noted

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.settings.searchEngineSettings.dialogs.AddEditAliasDialog
import com.tk.quicksearch.settings.shared.AliasPill
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * More Options card for the Noted integration; renders nothing unless Noted is connected. The toggle
 * sends new reminders to Noted, lists Noted's reminders in Reminders settings, hides Quick Search's
 * Quick Note from search, and turns on the Quick Note and To Do task aliases shown under it.
 */
@Composable
internal fun NotedIntegrationSettingCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!remember(context) { NotedIntegration.ensureChecked(context) }) return
    val reminderPreferences = remember(context) { ReminderPreferences(context) }
    var enabled by remember { mutableStateOf(reminderPreferences.isUseNotedEnabled()) }
    var quickNoteAlias by remember { mutableStateOf(NotedQuickNoteAlias.get(context)) }
    var taskAlias by remember { mutableStateOf(NotedTaskAlias.get(context)) }
    var editingAlias by remember { mutableStateOf<NotedEndAlias?>(null) }

    SegmentedTile(index = 0, count = 1, modifier = modifier.fillMaxWidth()) {
        SettingsToggleRow(
            title = stringResource(R.string.noted_integration_title),
            checked = enabled,
            onCheckedChange = { checked ->
                enabled = checked
                reminderPreferences.setUseNotedEnabled(checked)
                // The Reminders list shows Noted tasks only while this is on; reload it.
                ReminderRepository.notifyChanged()
            },
            subtitleContent = {
                // Each pill trims its top touch inset, which leaves an 8dp gap between the stacked chips.
                Column {
                    NotedAliasPill(
                        alias = quickNoteAlias,
                        icon = MaterialSymbols.EditNote,
                        onEdit = { editingAlias = NotedQuickNoteAlias },
                        onClear = {
                            NotedQuickNoteAlias.set(context, "")
                            quickNoteAlias = ""
                        },
                    )
                    NotedAliasPill(
                        alias = taskAlias,
                        icon = MaterialSymbols.CheckCircle,
                        onEdit = { editingAlias = NotedTaskAlias },
                        onClear = {
                            NotedTaskAlias.set(context, "")
                            taskAlias = ""
                        },
                    )
                }
            },
            leadingIcon = MaterialSymbols.EditNote,
            isFirstItem = true,
            isLastItem = true,
        )
    }

    editingAlias?.let { target ->
        val isQuickNote = target === NotedQuickNoteAlias
        val currentAlias = if (isQuickNote) quickNoteAlias else taskAlias
        val preferences = remember(context) { UserAppPreferences(context) }
        val existingAliases = remember { preferences.getAllAliasWordsById() }
        val triggerWords = remember { preferences.getAllTriggerWordsById().values.toList() }
        val targetName =
            stringResource(if (isQuickNote) R.string.notes_quick_note_title else R.string.noted_task_alias_target)
        AddEditAliasDialog(
            currentCode = currentAlias,
            existingShortcuts = existingAliases,
            currentShortcutId = target.aliasTargetId,
            onSave = { code ->
                target.set(context, code)
                if (isQuickNote) quickNoteAlias = target.get(context) else taskAlias = target.get(context)
            },
            aliasTargetName = targetName,
            dialogTitle =
                stringResource(
                    if (currentAlias.isBlank()) {
                        R.string.dialog_add_alias_for_search_type_title
                    } else {
                        R.string.dialog_edit_alias_for_search_type_title
                    },
                    targetName,
                ),
            existingTriggerWords = triggerWords,
            infoText =
                stringResource(if (isQuickNote) R.string.noted_quick_note_alias_info else R.string.noted_task_alias_info),
            onDismiss = { editingAlias = null },
        )
    }
}

/**
 * An alias pill, styled like search engine aliases: the code with a clear button, or "Setup Alias".
 * [icon] tells the Quick Note and task pills apart.
 */
@Composable
private fun NotedAliasPill(
    alias: String,
    icon: ImageVector,
    onEdit: () -> Unit,
    onClear: () -> Unit,
) {
    // The pill keeps a 48dp touch target around its 32dp chip; let the invisible top inset overlap
    // the title's space so the visible chip sits right under the title.
    val modifier =
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            val inset = AliasPillTouchInset.roundToPx()
            layout(placeable.width, placeable.height - inset) { placeable.place(0, -inset) }
        }
    if (alias.isNotBlank()) {
        AliasPill(
            text = AnnotatedString(alias),
            leadingIcon = icon,
            onClick = onEdit,
            onClearClick = onClear,
            modifier = modifier,
        )
    } else {
        AliasPill(
            text = AnnotatedString(stringResource(R.string.settings_add_alias)),
            leadingIcon = icon,
            onClick = onEdit,
            modifier = modifier,
        )
    }
}

private val AliasPillTouchInset = (DesignTokens.MinTouchTarget - DesignTokens.ButtonHeightExtraSmall) / 2
