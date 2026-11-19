package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.search.apps.appLock.AppLockGate
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.settings.appShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState
import com.tk.quicksearch.settings.shared.settingsContentWidth
import com.tk.quicksearch.settings.appShortcutsSettings.AppShortcutsSettingsSection
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.tools.aiSearch.supportsThinkingControl
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
