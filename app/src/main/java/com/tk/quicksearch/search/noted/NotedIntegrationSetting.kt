package com.tk.quicksearch.search.noted

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.data.preferences.ReminderPreferences
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.settings.searchEngineSettings.dialogs.AddEditAliasDialog
import com.tk.quicksearch.settings.shared.AliasPill
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * More Options card for the Noted integration; renders nothing unless Noted is connected. The toggle
 * sends new reminders to Noted, lists Noted's reminders in Reminders settings, hides Quick Search's
 * Quick Note from search, and turns on the Quick Note alias shown under it.
 */
@Composable
internal fun NotedIntegrationSettingCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (!remember(context) { NotedIntegration.ensureChecked(context) }) return
    val reminderPreferences = remember(context) { ReminderPreferences(context) }
    var enabled by remember { mutableStateOf(reminderPreferences.isUseNotedEnabled()) }
    var alias by remember { mutableStateOf(NotedQuickNoteAlias.get(context)) }
    var showAliasDialog by remember { mutableStateOf(false) }

    SettingsCard(modifier = modifier.fillMaxWidth()) {
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
                NotedQuickNoteAliasPill(
                    alias = alias,
                    onEdit = { showAliasDialog = true },
                    onClear = {
                        NotedQuickNoteAlias.set(context, "")
                        alias = ""
                    },
                )
            },
            leadingIcon = MaterialSymbols.EditNote,
            isFirstItem = true,
            isLastItem = true,
        )
    }

    if (showAliasDialog) {
        val preferences = remember(context) { UserAppPreferences(context) }
        val existingAliases = remember { preferences.getAllAliasWordsById() }
        val triggerWords = remember { preferences.getAllTriggerWordsById().values.toList() }
        val quickNoteName = stringResource(R.string.notes_quick_note_title)
        AddEditAliasDialog(
            currentCode = alias,
            existingShortcuts = existingAliases,
            currentShortcutId = NotedQuickNoteAlias.ALIAS_TARGET_ID,
            onSave = { code ->
                NotedQuickNoteAlias.set(context, code)
                alias = NotedQuickNoteAlias.get(context)
            },
            aliasTargetName = quickNoteName,
            dialogTitle =
                stringResource(
                    if (alias.isBlank()) {
                        R.string.dialog_add_alias_for_search_type_title
                    } else {
                        R.string.dialog_edit_alias_for_search_type_title
                    },
                    quickNoteName,
                ),
            existingTriggerWords = triggerWords,
            infoText = stringResource(R.string.noted_quick_note_alias_info),
            onDismiss = { showAliasDialog = false },
        )
    }
}

/** The alias pill, styled like search engine aliases: the code with a clear button, or "Setup Alias". */
@Composable
private fun NotedQuickNoteAliasPill(
    alias: String,
    onEdit: () -> Unit,
    onClear: () -> Unit,
) {
    val modifier = Modifier.padding(top = DesignTokens.SpacingXSmall)
    if (alias.isNotBlank()) {
        AliasPill(
            text = AnnotatedString(alias),
            textColor = AppColors.LinkColor,
            leadingIcon = MaterialSymbols.Bolt,
            onClick = onEdit,
            onClearClick = onClear,
            modifier = modifier,
        )
    } else {
        AliasPill(
            text = AnnotatedString(stringResource(R.string.settings_add_alias)),
            textColor = AppColors.LinkColor,
            showBackground = false,
            leadingIcon = MaterialSymbols.Bolt,
            onClick = onEdit,
            modifier = modifier,
        )
    }
}
