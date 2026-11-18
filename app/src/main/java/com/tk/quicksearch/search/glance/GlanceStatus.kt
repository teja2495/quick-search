package com.tk.quicksearch.search.glance

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.search.data.preferences.GlanceItem

/** A device status sampled when Home resumes or the At a Glance widget refreshes. */
internal class GlanceStatusDefinition(
    val preferenceKey: String,
    @StringRes val settingsTitle: Int,
    val settingsDescription: (Context) -> String,
    @StringRes val rowTitle: Int,
    val icon: () -> ImageVector,
    val read: (Context, GlanceStatusSurface) -> GlanceStatusValue?,
    val enabledByDefault: Boolean = true,
    val isError: Boolean = false,
    val open: ((Context) -> Unit)? = null,
    val dismissFromWidget: ((Context) -> Unit)? = null,
)

internal enum class GlanceStatusSurface {
    HOME,
    WIDGET,
}

internal class GlanceStatusValue(
    val subtitle: String? = null,
    val dismissFromHome: (() -> Unit)? = null,
)

internal val glanceStatusItems: List<GlanceItem> by lazy {
    GlanceItem.entries.filter { it.status != null }
}

internal fun glanceStatusForKey(key: String?): GlanceStatusDefinition? =
    glanceStatusItems.firstOrNull { it.key == key }?.status
