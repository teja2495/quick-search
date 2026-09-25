package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.search.apps.appLock.AppLockGate
import com.tk.quicksearch.reminders.ReminderEditorRequests
import androidx.compose.runtime.collectAsState
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryAccess
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.data.NotesRepository
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.deviceSettings.NOTIFICATION_HISTORY_SETTING_ID
import com.tk.quicksearch.settings.settingsDetailScreen.GesturesSettingsSection
import com.tk.quicksearch.searchEngines.AliasHandler
import com.tk.quicksearch.settings.tasker.TaskerIntegrationScreen
import com.tk.quicksearch.tools.tasker.TaskerIntegration
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.shared.permissions.PermissionHelper
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.shared.SettingsCommand
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsCardItem
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.settings.shared.SettingsScreenState
import com.tk.quicksearch.settings.shared.SettingsManagementSearchBar
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutsSettingsSection
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.NoteDeleteConfirmationDialog
import com.tk.quicksearch.settings.NotesBulkDeleteConfirmationDialog
import com.tk.quicksearch.settings.settingsDetailScreen.CustomToolNavigationMemory
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.layout.ColumnScope
import android.content.Context

@Composable
internal fun ColumnScope.AppShortcutsDetailContent(
    state: SettingsScreenState,
    callbacks: SettingsScreenCallbacks,
    context: Context,
    isLoading: Boolean,
    searchQuery: String,
    collapseAllTrigger: Int,
    shortcutSources: List<AppShortcutSource>,
    searchTargets: List<SearchTarget>,
    focusShortcut: StaticShortcut?,
    focusPackageName: String?,
    onFocusHandled: () -> Unit,
) {

                AppShortcutsSettingsSection(
                    shortcuts = state.allAppShortcuts,
                    isLoading = isLoading,
                    disabledShortcutIds = state.disabledAppShortcutIds,
                    iconPackPackage = state.selectedIconPackPackage,
                    searchQuery = searchQuery,
                    collapseAllTrigger = collapseAllTrigger,
                    onShortcutEnabledChange = callbacks.onToggleAppShortcutEnabled,
                    onAllAppShortcutsEnabledChange = callbacks.onToggleAllAppShortcutsEnabled,
                    onShortcutNameClick = { shortcut ->
                        AppLockGate.runAfterUnlock(context, shortcut.packageName, shortcut.appLabel) {
                            callbacks.onLaunchAppShortcut(shortcut)
                        }
                    },
                    shortcutSources = shortcutSources,
                    onAddShortcutFromSource = callbacks.onAddAppShortcutFromSource,
                    onAddAppDeepLinkShortcut = callbacks.onAddAppDeepLinkShortcut,
                    searchTargets = searchTargets,
                    onAddQueryShortcut = callbacks.onAddSearchTargetQueryShortcut,
                    onUpdateCustomShortcut = callbacks.onUpdateCustomAppShortcut,
                    onDeleteCustomShortcut = callbacks.onDeleteCustomAppShortcut,
                    focusShortcut = focusShortcut,
                    focusPackageName = focusPackageName,
                    onFocusHandled = onFocusHandled,
                    modifier =
                        Modifier
                            .settingsContentWidth()
                            .fillMaxHeight()
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                            .padding(
                                start = DesignTokens.ContentHorizontalPadding,
                                end = DesignTokens.ContentHorizontalPadding,
                                bottom = 96.dp,
                            ),
                )
}
