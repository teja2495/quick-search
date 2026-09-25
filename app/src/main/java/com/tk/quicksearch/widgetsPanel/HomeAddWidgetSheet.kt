package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
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

/**
 * Widget picker opened from Home. The picked widget goes straight to Home, without being added to
 * the widgets panel first. Stays composed until the widget is added or the sheet is dismissed, so
 * the bind and configure results can land.
 */
@Composable
fun HomeAddWidgetSheet(onDismiss: () -> Unit) {
    val appContext = LocalContext.current.applicationContext
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    // Only allocates and releases ids; the Home widget stack does the hosting and listening.
    val appWidgetHost = remember(appContext) { AppWidgetHost(appContext, QUICK_SEARCH_WIDGET_HOST_ID) }
    val preferences = remember(appContext) { WidgetsPanelPreferences(appContext) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val requestAddWidget =
        rememberWidgetAddFlow(appWidgetHost) { appWidgetId, provider, columnSpan, rowSpan ->
            HomePinnedWidgetsStore.publish(
                preferences.addHomeWidget(
                    appWidgetId = appWidgetId,
                    provider = provider.provider,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan,
                ),
            )
            // New widgets land below the app grid, which can be off screen on a long Home.
            Toast.makeText(appContext, R.string.widget_pinned_to_home, Toast.LENGTH_SHORT).show()
            currentOnDismiss()
        }

    // Home isn't one full-screen box the picker can overlay, so give it a window of its own.
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
            WidgetPickerSheet(
                appWidgetManager = appWidgetManager,
                showQuickNote = false,
                onDismiss = onDismiss,
                onAddQuickNote = {},
                onSelectWidget = requestAddWidget,
            )
        }
    }
}
