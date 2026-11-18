package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import com.tk.quicksearch.shared.ui.components.AppMessages
import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.customInfo.CustomInfoItem
import com.tk.quicksearch.customInfo.CustomInfoRepository
import com.tk.quicksearch.customInfo.CustomInfoScheduler
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.util.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle

internal class CustomInfoGlance(
    val items: List<CustomInfoItem>,
    val dismiss: (CustomInfoItem) -> Unit,
    val retry: (CustomInfoItem) -> Unit,
)

/** Latest Custom Info results, success or failure, until the user dismisses them or the next run replaces them. */
@Composable
internal fun rememberCustomInfoGlance(enabled: Boolean): CustomInfoGlance {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val repository = remember(appContext) { CustomInfoRepository(appContext) }
    val change by CustomInfoRepository.changes.collectAsState()
    val resumeKey = rememberResumeRefreshKey()
    var items by remember { mutableStateOf(emptyList<CustomInfoItem>()) }
    // Not keyed on [enabled]: typing a query hides the rows without reading them again.
    LaunchedEffect(change, resumeKey) {
        items =
            withContext(Dispatchers.IO) {
                repository.all().filter { item ->
                    item.enabled && item.showOnHome &&
                        (item.status == CustomInfoItem.ERROR ||
                            (item.status == CustomInfoItem.COMPLETE && item.answer.isNotBlank()))
                }
            }
    }
    return CustomInfoGlance(
        items = if (enabled) items else emptyList(),
        dismiss = { item ->
            CustomInfoScheduler.cancelNotification(appContext, item.id)
            repository.update(item.id) { it.copy(showOnHome = false) }
        },
        retry = { item -> CustomInfoScheduler.retry(appContext, item.id) },
    )
}

@Composable
internal fun CustomInfoRow(
    item: CustomInfoItem,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    if (item.status == CustomInfoItem.ERROR) {
        // A new run result (lastRunMillis) ends the spinner, whichever way it went.
        var retrying by remember(item.lastRunMillis) { mutableStateOf(false) }
        GlanceStatusRow(
            icon = {
                Icon(MaterialSymbols.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = item.title,
            subtitle = stringResource(R.string.direct_search_error_generic),
            onClick = {
                if (!retrying) {
                    retrying = true
                    onRetry()
                }
            },
            onDismiss = onDismiss,
            trailing = {
                if (retrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(DesignTokens.InlineProgressSize),
                        strokeWidth = DesignTokens.InlineProgressStrokeWidth,
                    )
                } else {
                    AppIconButton(
                        onClick = {
                            retrying = true
                            onRetry()
                        },
                        tooltip = stringResource(R.string.action_retry),
                        style = AppIconButtonStyle.Tonal,
                        modifier = Modifier.size(DesignTokens.ButtonHeightSmall),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Refresh,
                            contentDescription = stringResource(R.string.action_retry),
                            modifier = Modifier.size(DesignTokens.IconSizeSmall),
                        )
                    }
                }
            },
        )
    } else {
        val context = LocalContext.current
        val copyLabel = stringResource(R.string.calculator_copy_result)
        val copiedMessage = stringResource(R.string.home_copied)
        GlanceStatusRow(
            icon = {
                Icon(MaterialSymbols.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = item.title,
            onClick = {},
            // Android 13+ confirms copies itself; older versions get a toast.
            onLongClick = {
                val copied = copySensitiveText(context, copyLabel, item.answer, sensitive = false)
                if (copied && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    AppMessages.show(context, copiedMessage)
                }
            },
            onDismiss = onDismiss,
            belowText = {
                MarkdownText(
                    markdown = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }
}
