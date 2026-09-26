package com.tk.quicksearch.search.searchScreen.searchRoute

import com.tk.quicksearch.search.data.ReminderRepository
import com.tk.quicksearch.search.searchScreen.ReminderSectionActions
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.other.OtherSearchItemAction
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.utils.FileUtils
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.launch

internal data class RouteUndoActions(
    val snackbarHostState: SnackbarHostState,
    val popupUndoSnackbar: @Composable BoxScope.() -> Unit,
    val onHideAppWithUndo: (AppInfo) -> Unit,
    val onExcludeContactWithUndo: (ContactInfo) -> Unit,
    val onExcludeFileWithUndo: (DeviceFile) -> Unit,
    val onExcludeFileExtensionWithUndo: (DeviceFile) -> Unit,
    val onExcludeSettingWithUndo: (DeviceSetting) -> Unit,
    val onDisableAppShortcut: (com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut) -> Unit,
    val onDisableAllAppShortcutsForApp: (com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut) -> Unit,
    val onExcludeCalendarEventWithUndo: (CalendarEventInfo) -> Unit,
    val reminderActions: ReminderSectionActions,
    val onDeleteNoteWithUndo: (NoteInfo) -> Unit,
    val onOtherSearchItemAction: (OtherSearchItemId, OtherSearchItemAction) -> Unit,
)

@Composable
internal fun rememberRouteUndoActions(
    viewModel: SearchViewModel,
    uiState: SearchUiState,
    overlaySnackbarHostState: SnackbarHostState?,
): RouteUndoActions {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val effectiveSnackbarHostState = overlaySnackbarHostState ?: snackbarHostState
    val snackbarScope = rememberCoroutineScope()
    // Mirrors the undo snackbar inside popups so it isn't hidden behind them.
    val popupUndoSnackbar: @Composable BoxScope.() -> Unit =
        remember(effectiveSnackbarHostState) {
            {
                ExcludeUndoSnackbarHost(
                    hostState = effectiveSnackbarHostState,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = DesignTokens.SpacingLarge,
                                end = DesignTokens.SpacingLarge,
                                bottom = DesignTokens.SpacingHuge,
                            ),
                )
            }
        }
    val undoLabel = stringResource(R.string.action_undo)

    // A new undo snackbar replaces the visible one right away instead of queueing behind it.
    val undoSnackbarJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val showUndoSnackbarVisuals: (UndoSnackbarVisuals, () -> Unit) -> Unit = { visuals, onUndo ->
        undoSnackbarJob.value?.cancel()
        undoSnackbarJob.value =
            snackbarScope.launch {
                val result = effectiveSnackbarHostState.showSnackbar(visuals)
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    onUndo()
                }
            }
    }

    val showUndoSnackbar: (String, () -> Unit) -> Unit = { message, onUndo ->
        showUndoSnackbarVisuals(UndoSnackbarVisuals(message = message, actionLabel = undoLabel), onUndo)
    }

    val showAppShortcutDisabledSnackbar: (() -> Unit) -> Unit = @Suppress("LocalContextGetResourceValueCall") { onUndo ->
        showUndoSnackbarVisuals(
            UndoSnackbarVisuals(
                message = context.getString(R.string.snackbar_app_shortcut_disabled_title),
                supportingText = context.getString(R.string.snackbar_app_shortcut_disabled_supporting),
                icon = null,
                actionLabel = undoLabel,
            ),
            onUndo,
        )
    }

    val onHideAppWithUndo: (AppInfo) -> Unit = @Suppress("LocalContextGetResourceValueCall") { app ->
        val isSearching = uiState.query.isNotBlank()
        viewModel.hideApp(app)
        val messageRes =
            if (isSearching) {
                R.string.toast_excluded_from_results
            } else {
                R.string.toast_excluded_from_suggestions
            }
        showUndoSnackbar(context.getString(messageRes, app.appName)) {
            if (isSearching) {
                viewModel.unhideAppFromResults(app)
            } else {
                viewModel.unhideAppFromSuggestions(app)
            }
        }
    }

    val onExcludeContactWithUndo: (ContactInfo) -> Unit = @Suppress("LocalContextGetResourceValueCall") { contact ->
        viewModel.excludeContact(contact)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, contact.displayName),
        ) {
            viewModel.removeExcludedContact(contact)
        }
    }

    val onExcludeFileWithUndo: (DeviceFile) -> Unit = @Suppress("LocalContextGetResourceValueCall") { file ->
        viewModel.excludeFile(file)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, file.displayName),
        ) {
            viewModel.removeExcludedFile(file)
        }
    }

    val onExcludeFileExtensionWithUndo: (DeviceFile) -> Unit = @Suppress("LocalContextGetResourceValueCall") { file ->
        val extension = FileUtils.getFileExtension(file.displayName)
        if (extension != null) {
            viewModel.excludeFileExtension(file)
            val extensionLabel = context.getString(R.string.file_extension_label, extension)
            showUndoSnackbar(
                context.getString(R.string.toast_excluded_from_results, extensionLabel),
            ) {
                viewModel.removeExcludedFileExtension(extension)
            }
        }
    }

    val onExcludeSettingWithUndo: (DeviceSetting) -> Unit = @Suppress("LocalContextGetResourceValueCall") { setting ->
        viewModel.excludeSetting(setting)
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, setting.title),
        ) {
            viewModel.removeExcludedSetting(setting)
        }
    }

    val onDisableAppShortcut: (com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut) -> Unit = @Suppress("LocalContextGetResourceValueCall") { shortcut ->
        viewModel.setAppShortcutEnabled(shortcut, false)
        showAppShortcutDisabledSnackbar {
            viewModel.setAppShortcutEnabled(shortcut, true)
        }
    }

    val onDisableAllAppShortcutsForApp: (com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut) -> Unit = @Suppress("LocalContextGetResourceValueCall") { shortcut ->
        // Disables the app's future shortcuts too. Undo restores each shortcut's previous state.
        viewModel.setAllAppShortcutsEnabled(shortcut.packageName, false)
        showAppShortcutDisabledSnackbar {
            viewModel.setAllAppShortcutsEnabled(shortcut.packageName, true)
        }
    }

    val onExcludeCalendarEventWithUndo: (CalendarEventInfo) -> Unit = @Suppress("LocalContextGetResourceValueCall") { event ->
        viewModel.excludeCalendarEvent(event)
        val label = event.title.ifBlank { context.getString(R.string.section_calendar) }
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, label),
        ) {
            viewModel.removeExcludedCalendarEvent(event)
        }
    }

    val reminderActions =
        remember(viewModel, context) {
            val reminderRepository = ReminderRepository(context)
            ReminderSectionActions(
                onPin = viewModel::pinReminder,
                onUnpin = viewModel::unpinReminder,
                onMovePinned = viewModel::movePinnedReminder,
                onMarkDone = { reminder -> reminderRepository.setDone(reminder.reminderId, true) },
                onDelete = { reminder ->
                    viewModel.unpinReminder(reminder)
                    reminderRepository.deleteReminder(reminder.reminderId)
                },
            )
        }
    val onDeleteNoteWithUndo: (NoteInfo) -> Unit = noteDelete@{ note ->
        val staged = viewModel.stageDeleteNote(note) ?: return@noteDelete
        val label = staged.title.ifBlank { context.getString(R.string.notes_untitled) }
        var wasUndone = false
        showUndoSnackbar(
            context.getString(R.string.toast_excluded_from_results, label),
        ) {
            wasUndone = true
            viewModel.undoDeleteNote(staged.noteId)
        }
        snackbarScope.launch {
            kotlinx.coroutines.delay(2_500L)
            if (!wasUndone) {
                viewModel.finalizeDeleteNote(staged.noteId)
            }
        }
    }

    val onOtherSearchItemAction: (OtherSearchItemId, OtherSearchItemAction) -> Unit = @Suppress("LocalContextGetResourceValueCall") { itemId, action ->
        when (action) {
            OtherSearchItemAction.TOGGLE_PIN -> viewModel.toggleOtherSearchItemPin(itemId)
            OtherSearchItemAction.HIDE -> {
                val wasPinned = OtherSearchItemRegistry.isPinned(itemId, uiState.pinnedNonAppItemOrder)
                viewModel.excludeOtherSearchItem(itemId)
                showUndoSnackbar(
                    context.getString(R.string.toast_excluded_from_results, context.getString(itemId.titleRes)),
                ) {
                    viewModel.removeExcludedOtherSearchItem(itemId)
                    if (wasPinned) viewModel.toggleOtherSearchItemPin(itemId)
                }
            }
        }
    }

    return RouteUndoActions(
        snackbarHostState = snackbarHostState,
        popupUndoSnackbar = popupUndoSnackbar,
        onHideAppWithUndo = onHideAppWithUndo,
        onExcludeContactWithUndo = onExcludeContactWithUndo,
        onExcludeFileWithUndo = onExcludeFileWithUndo,
        onExcludeFileExtensionWithUndo = onExcludeFileExtensionWithUndo,
        onExcludeSettingWithUndo = onExcludeSettingWithUndo,
        onDisableAppShortcut = onDisableAppShortcut,
        onDisableAllAppShortcutsForApp = onDisableAllAppShortcutsForApp,
        onExcludeCalendarEventWithUndo = onExcludeCalendarEventWithUndo,
        reminderActions = reminderActions,
        onDeleteNoteWithUndo = onDeleteNoteWithUndo,
        onOtherSearchItemAction = onOtherSearchItemAction,
    )
}
