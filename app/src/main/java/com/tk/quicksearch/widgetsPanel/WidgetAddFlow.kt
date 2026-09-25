package com.tk.quicksearch.widgetsPanel

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Host id shared by every surface that hosts Quick Search widgets (the panel and Home). */
internal const val QUICK_SEARCH_WIDGET_HOST_ID = 8291

private data class PendingWidgetRequest(
    val appWidgetId: Int,
    val provider: AppWidgetProviderInfo,
)

/**
 * Binds a picked widget provider (asking for bind permission when needed), runs its configure
 * activity if it has one, and reports the ready widget through [onWidgetAdded]. Cancelled or failed
 * requests release their allocated widget id. Keep the caller composed until the widget is added:
 * the activity-result launchers live in this composition.
 */
@Composable
internal fun rememberWidgetAddFlow(
    appWidgetHost: AppWidgetHost,
    onWidgetAdded: (appWidgetId: Int, provider: AppWidgetProviderInfo, columnSpan: Int, rowSpan: Int) -> Unit,
): (AppWidgetProviderInfo) -> Unit {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val configuration = LocalConfiguration.current
    val appWidgetManager = remember(appContext) { AppWidgetManager.getInstance(appContext) }
    val widgetOptionsFactory =
        remember(configuration) {
            WidgetOptionsFactory(
                screenWidthDp = configuration.screenWidthDp,
                density = context.resources.displayMetrics.density,
                orientation = configuration.orientation,
            )
        }
    val currentOnWidgetAdded by rememberUpdatedState(onWidgetAdded)
    var pendingRequest by remember { mutableStateOf<PendingWidgetRequest?>(null) }

    fun finalizeAddWidget(request: PendingWidgetRequest) {
        val (columnSpan, rowSpan) = initialSpanFor(request.provider)
        appWidgetManager.updateAppWidgetOptions(
            request.appWidgetId,
            widgetOptionsFactory.create(columnSpan, rowSpan),
        )
        pendingRequest = null
        currentOnWidgetAdded(request.appWidgetId, request.provider, columnSpan, rowSpan)
    }

    val configureLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result: ActivityResult ->
            val request = pendingRequest ?: return@rememberLauncherForActivityResult
            if (
                result.resultCode == Activity.RESULT_OK ||
                isWidgetConfigurationOptional(request.provider)
            ) {
                finalizeAddWidget(request)
            } else {
                appWidgetHost.deleteAppWidgetId(request.appWidgetId)
                pendingRequest = null
            }
        }

    fun launchConfigureIfNeeded(request: PendingWidgetRequest) {
        val configure = request.provider.configure
        if (configure == null) {
            finalizeAddWidget(request)
            return
        }
        val (columnSpan, rowSpan) = initialSpanFor(request.provider)
        val intent =
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(configure)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, request.appWidgetId)
                .putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,
                    widgetOptionsFactory.create(columnSpan, rowSpan),
                )
        pendingRequest = request
        val launchFailed =
            runCatching { configureLauncher.launch(intent) }
                .exceptionOrNull()
                ?.let { it is SecurityException || it is ActivityNotFoundException }
                ?: false

        if (launchFailed) {
            // Some widgets expose configure components that are not exported to third-party launchers.
            finalizeAddWidget(request)
        }
    }

    val bindLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result: ActivityResult ->
            val request = pendingRequest ?: return@rememberLauncherForActivityResult
            if (result.resultCode == Activity.RESULT_OK) {
                launchConfigureIfNeeded(request)
            } else {
                appWidgetHost.deleteAppWidgetId(request.appWidgetId)
                pendingRequest = null
            }
        }

    return { provider ->
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        val request = PendingWidgetRequest(appWidgetId, provider)
        val (columnSpan, rowSpan) = initialSpanFor(provider)
        val widgetOptions = widgetOptionsFactory.create(columnSpan, rowSpan)
        val canBind =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    appWidgetManager.bindAppWidgetIdIfAllowed(
                        appWidgetId,
                        provider.profile,
                        provider.provider,
                        widgetOptions,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider.provider)
                }
            }.getOrDefault(false)

        if (canBind) {
            launchConfigureIfNeeded(request)
        } else {
            pendingRequest = request
            val intent =
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, widgetOptions)
            val launchFailed =
                runCatching { bindLauncher.launch(intent) }
                    .exceptionOrNull()
                    ?.let { it is SecurityException || it is ActivityNotFoundException }
                    ?: false
            if (launchFailed) {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                pendingRequest = null
            }
        }
    }
}
