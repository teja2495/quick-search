package com.tk.quicksearch.search.searchScreen.searchScreenLayout

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.search.data.preferences.AppGlanceItemSettings
import com.tk.quicksearch.search.data.preferences.GlanceItem
import com.tk.quicksearch.search.glance.GlanceStatusSurface
import com.tk.quicksearch.search.glance.GlanceStatusValue
import com.tk.quicksearch.search.glance.glanceStatusItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberSampledGlanceStatusRows(enabled: Boolean): List<AtAGlanceItem> {
    val context = LocalContext.current
    val settings = remember(context) { AppGlanceItemSettings(context) }
    val refreshKey = rememberResumeRefreshKey()
    var statuses by remember { mutableStateOf(emptyMap<GlanceItem, GlanceStatusValue>()) }
    // Search visibility changes should not trigger device reads.
    LaunchedEffect(context, refreshKey) {
        statuses = withContext(Dispatchers.IO) {
            buildMap {
                for (item in glanceStatusItems) {
                    if (!settings.isShown(item)) continue
                    item.status?.read?.invoke(context, GlanceStatusSurface.HOME)?.let { put(item, it) }
                }
            }
        }
    }

    if (!enabled) return emptyList()
    return statuses.map { (item, value) ->
        val status = checkNotNull(item.status)
        AtAGlanceItem(key = item.key.replace('_', '-')) {
            GlanceStatusRow(
                icon = {
                    Icon(
                        imageVector = status.icon(),
                        contentDescription = null,
                        tint = if (status.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                },
                title = stringResource(status.rowTitle),
                subtitle = value.subtitle,
                onClick = status.open?.let { open -> { open(context) } },
                onDismiss = value.dismissFromHome?.let { dismiss ->
                    {
                        dismiss()
                        statuses = statuses - item
                    }
                },
            )
        }
    }
}
