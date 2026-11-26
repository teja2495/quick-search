package com.tk.quicksearch.widgetsPanel

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.PreserveHostKeyboardStateEffect
import com.tk.quicksearch.shared.ui.components.ReportOpenPopupEffect

/**
 * Widget picker opened from Home. The picked widget goes straight to Home, without being added to
 * the widgets panel first, or onto the Home stack [stackTarget] names (its owner id and size).
 * Stays composed until the widget is added or the sheet is dismissed, so the bind and configure
 * results can land.
 */
@Composable
fun HomeAddWidgetSheet(onDismiss: () -> Unit) {
    HomeAddWidgetSheet(onDismiss = onDismiss, stackTarget = null)
}

@Composable
internal fun HomeAddWidgetSheet(
    onDismiss: () -> Unit,
    stackTarget: Pair<Int, WidgetStackFit>?,
) {
    val appContext = LocalContext.current.applicationContext
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    // Only allocates and releases ids; the Home widget stack does the hosting and listening.
    val appWidgetHost = remember(appContext) { AppWidgetHost(appContext, QUICK_SEARCH_WIDGET_HOST_ID) }
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentStackTarget by rememberUpdatedState(stackTarget)
    val requestAddWidget =
        rememberWidgetAddFlow(appWidgetHost) { appWidgetId, provider, columnSpan, rowSpan ->
            val ownerId = currentStackTarget?.first
            if (ownerId != null && preferences.getWidgets().any { it.isOwner(ownerId, onHome = true) }) {
                HomePinnedWidgetsStore.publish(
                    preferences.addToStack(ownerId, appWidgetId, provider.provider, onHome = true),
                )
            } else {
                HomePinnedWidgetsStore.publish(
                    preferences.addHomeWidget(
                        appWidgetId = appWidgetId,
                        provider = provider.provider,
                        columnSpan = columnSpan,
                        rowSpan = rowSpan,
                    ),
                )
                // New widgets land below the app grid, which can be off screen on a long Home.
                AppMessages.show(appContext, R.string.widget_pinned_to_home)
            }
            currentOnDismiss()
        }

    HomeSheetDialog(onDismiss = onDismiss) {
        WidgetPickerSheet(
            appWidgetManager = appWidgetManager,
            showQuickNote = false,
            stackFit = stackTarget?.second,
            onDismiss = onDismiss,
            onAddQuickNote = {},
            onSelectWidget = requestAddWidget,
        )
    }
}

/**
 * Reorder drawer for the Home stack owned by [ownerId]. Reordering hands the stack to its new first
 * page, so [onOwnerChange] reports the id to keep the drawer on, or null once it should close.
 */
@Composable
internal fun HomeStackReorderSheet(
    ownerId: Int,
    widgets: List<PanelWidgetInfo>,
    host: WidgetPanelHost,
    onOwnerChange: (Int?) -> Unit,
) {
    val appContext = LocalContext.current.applicationContext
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }
    val owner = widgets.firstOrNull { it.appWidgetId == ownerId }?.takeIf { it.stack.isNotEmpty() }
    val currentOnOwnerChange by rememberUpdatedState(onOwnerChange)
    // The stack was removed or broken up while the drawer was open.
    LaunchedEffect(owner == null) {
        if (owner == null) currentOnOwnerChange(null)
    }
    if (owner == null) return

    HomeSheetDialog(onDismiss = { onOwnerChange(null) }) {
        WidgetStackReorderSheet(
            pageIds = owner.pageIds,
            appWidgetHost = host,
            onReorder = { ordered ->
                HomePinnedWidgetsStore.publish(preferences.reorderStack(owner.appWidgetId, ordered, onHome = true))
                onOwnerChange(ordered.first())
            },
            onRemove = { pageId ->
                val next = preferences.removeStackPage(owner.appWidgetId, pageId, onHome = true)
                // A Home widget saved before Home kept its own instances can share its id with a
                // panel widget; that id stays bound while the panel shows it.
                if (!isBuiltInPanelPage(pageId) && !next.referencesWidgetId(pageId)) host.deleteAppWidgetId(pageId)
                HomePinnedWidgetsStore.publish(next)
                // Removing the first page hands the stack to the next one; a stack left with one
                // page is a plain widget, which closes the drawer.
                onOwnerChange(
                    owner.pageIds.firstOrNull { it != pageId }
                        ?.let { remaining -> next.filter { it.isOnHome }.stackOwnerOf(remaining) }
                        ?.takeIf { it.stack.isNotEmpty() }
                        ?.appWidgetId,
                )
            },
            onDismiss = { onOwnerChange(null) },
        )
    }
}

/** Home isn't one full-screen box a sheet can overlay, so its sheets get a window of their own. */
@Composable
private fun HomeSheetDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    PreserveHostKeyboardStateEffect()
    ReportOpenPopupEffect()
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setDimAmount(0f)
            // Match the widgets panel: the keyboard covers the sheet instead of resizing it.
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            content()
        }
    }
}
