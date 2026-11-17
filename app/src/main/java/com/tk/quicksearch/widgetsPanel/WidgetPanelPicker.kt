package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSearch
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerMessage
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.utils.UserHandleUtils
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppButton

private val WidgetPickerStageHeight = 176.dp

/** Tile tone that stays visible on the drawer's `surfaceContainerHigh` sheet. */
@Composable
private fun widgetPickerTileColor() = MaterialTheme.colorScheme.surfaceContainerHighest

private const val WIDGET_PREVIEW_FALLBACK_SIZE_PX = 96
private const val MAX_WIDGET_PREVIEW_DIMENSION_PX = 1024
private const val MAX_WIDGET_PREVIEW_PIXELS = 1_048_576
private val WIDGET_PANEL_GRID_ROW_HEIGHT_DP = 80f
private val WIDGET_PANEL_GRID_GAP_DP = 8f

private data class WidgetPickerApp(
    val packageName: String,
    val profileId: Int,
    val appLabel: String,
    val icon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>,
) {
    val key: String = "$packageName:$profileId"
}

@Composable
internal fun WidgetPickerSheet(
    appWidgetManager: AppWidgetManager,
    showQuickNote: Boolean,
    onDismiss: () -> Unit,
    onAddQuickNote: () -> Unit,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val apps by produceState<List<WidgetPickerApp>?>(initialValue = null, appWidgetManager, packageManager) {
        value =
            withContext(Dispatchers.Default) {
                appWidgetManager.installedProviders
                    .groupBy { it.provider.packageName to UserHandleUtils.getIdentifier(it.profile) }
                    .map { (appKey, providers) ->
                        val (packageName, profileId) = appKey
                        val appInfo =
                            runCatching { packageManager.getApplicationInfo(packageName, 0) }
                                .getOrNull()
                        val profile = providers.first().profile
                        WidgetPickerApp(
                            packageName = packageName,
                            profileId = profileId,
                            appLabel =
                                appInfo
                                    ?.let { packageManager.getApplicationLabel(it).toString() }
                                    ?: packageName,
                            // Badging makes a cloned/profile-specific app visibly distinct even
                            // when Android gives both copies the same app label and package name.
                            icon =
                                appInfo
                                    ?.let { runCatching { packageManager.getApplicationIcon(it) }.getOrNull() }
                                    ?.let { icon ->
                                        runCatching { packageManager.getUserBadgedIcon(icon, profile) }
                                            .getOrDefault(icon)
                                    },
                            widgets =
                                providers.sortedBy {
                                    it.loadLabel(packageManager).orEmpty().lowercase()
                                },
                        )
                    }
                    .sortedBy { it.appLabel.lowercase() }
            }
    }
    val filteredApps =
        remember(apps, query) {
            val loadedApps = apps.orEmpty()
            val normalizedQuery = query.text.trim().lowercase()
            if (normalizedQuery.isBlank()) {
                loadedApps
            } else {
                loadedApps.filter { app -> app.appLabel.lowercase().contains(normalizedQuery) }
            }
        }
    var expandedApps by rememberSaveable { mutableStateOf(setOf<String>()) }
    AppPickerDrawer(
        title = stringResource(R.string.widgets_picker_title),
        onDismiss = onDismiss,
        search = AppPickerDrawerSearch(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.common_search),
        ),
    ) {
        if (apps == null) {
            WidgetPickerLoading()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                if (showQuickNote) {
                    item(key = "quick_note") { QuickNotePickerRow(onClick = onAddQuickNote) }
                }
                items(filteredApps, key = { it.key }) { app ->
                    WidgetPickerAppGroup(
                        app = app,
                        isExpanded = app.key in expandedApps,
                        onToggleExpanded = {
                            expandedApps = if (app.key in expandedApps) expandedApps - app.key else expandedApps + app.key
                        },
                        packageManager = packageManager,
                        onSelectWidget = onSelectWidget,
                    )
                }
                if (filteredApps.isEmpty()) {
                    item { AppPickerDrawerMessage(text = stringResource(R.string.widgets_picker_empty)) }
                }
            }
        }
    }
}

/** Fills the drawer's list area so the message sits in its vertical center. */
@Composable
private fun WidgetPickerLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.widgets_picker_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickNotePickerRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedTile(
        index = 0,
        count = 1,
        modifier = modifier,
        onClick = onClick,
        containerColor = widgetPickerTileColor(),
    ) {
        SegmentedTileRow(
            title = stringResource(R.string.notes_quick_note_title),
            leading = { TonalIconBadge(icon = MaterialSymbols.EditNote) },
            trailing = {
                Icon(imageVector = MaterialSymbols.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
        )
    }
}

@Composable
private fun WidgetPickerAppGroup(
    app: WidgetPickerApp,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    packageManager: PackageManager,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "widgetPickerChevronRotation",
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap)) {
        SegmentedTile(
            index = 0,
            count = if (isExpanded) app.widgets.size + 1 else 1,
            onClick = onToggleExpanded,
            containerColor = widgetPickerTileColor(),
        ) {
            SegmentedTileRow(
                title = app.appLabel,
                description = stringResource(R.string.widgets_picker_widget_count, app.widgets.size),
                leading = {
                    DrawableImage(
                        drawable = app.icon,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.Spacing40).clip(CircleShape),
                    )
                },
                trailing = {
                    Icon(
                        imageVector = MaterialSymbols.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                },
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = AppMotion.defaultSpatial()) + fadeIn(),
            exit = shrinkVertically(animationSpec = AppMotion.defaultSpatial()) + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap)) {
                app.widgets.forEachIndexed { index, provider ->
                    SegmentedTile(
                        index = index + 1,
                        count = app.widgets.size + 1,
                        onClick = { onSelectWidget(provider) },
                        containerColor = widgetPickerTileColor(),
                    ) {
                        WidgetPickerRow(provider, packageManager, onClick = { onSelectWidget(provider) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerRow(
    provider: AppWidgetProviderInfo,
    packageManager: PackageManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val previewImage = remember(provider) {
        runCatching { provider.loadPreviewImage(context, densityDpi) }.getOrNull()
            ?: runCatching { provider.loadPreviewImage(context, android.util.DisplayMetrics.DENSITY_DEVICE_STABLE) }
                .getOrNull()
            ?: runCatching { provider.loadPreviewImage(context, android.util.DisplayMetrics.DENSITY_DEFAULT) }
                .getOrNull()
    }
    val previewLayoutRes = remember(provider) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.previewLayout else 0
    }
    val widgetIcon = remember(provider) {
        runCatching { provider.loadIcon(context, densityDpi) }.getOrNull()
    }
    val minWidth = with(density) { provider.minWidth.toDp() }
    val minHeight = with(density) { provider.minHeight.toDp() }
    val initialGridSize =
        remember(provider, minWidth, minHeight) {
            computeInitialSpan(
                targetCellWidth =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellWidth else 0,
                targetCellHeight =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.targetCellHeight else 0,
                minWidthDp = minWidth.value,
                minHeightDp = minHeight.value,
                // Mirrors panel sizing used in WidgetsPanelScreen.
                cellWidthDp = 80f,
                rowHeightDp = WIDGET_PANEL_GRID_ROW_HEIGHT_DP,
                gapDp = WIDGET_PANEL_GRID_GAP_DP,
            )
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(DesignTokens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(WidgetPickerStageHeight),
            contentAlignment = Alignment.Center,
        ) {
            when {
                previewImage != null -> {
                    DrawableImage(
                        drawable = previewImage,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(DesignTokens.SpacingLarge),
                    )
                }
                previewLayoutRes != 0 -> {
                    WidgetRemoteViewsPreview(
                        packageName = provider.provider.packageName,
                        layoutRes = previewLayoutRes,
                        fallbackIcon = widgetIcon,
                        modifier = Modifier.fillMaxSize().padding(DesignTokens.SpacingLarge),
                    )
                }
                widgetIcon != null -> {
                    DrawableImage(
                        drawable = widgetIcon,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.widgets_picker_preview_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(DesignTokens.SpacingSmall),
                    )
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = DesignTokens.SpacingSmall, bottom = DesignTokens.SpacingXSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
            ) {
                Text(
                    text = provider.loadLabel(packageManager).orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.widgets_picker_widget_grid_size,
                        initialGridSize.first,
                        initialGridSize.second,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppButton(
                onClick = onClick,
                shape = DesignTokens.ShapeFull,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
                Text(
                    text = stringResource(R.string.common_action_add),
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
        }
    }
}

@Composable
private fun WidgetRemoteViewsPreview(
    packageName: String,
    layoutRes: Int,
    fallbackIcon: Drawable?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val container =
                FrameLayout(ctx).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                }
            val inflated =
                runCatching {
                    RemoteViews(packageName, layoutRes).apply(ctx, container)
                }.getOrNull()
            if (inflated != null) {
                container.addView(
                    inflated,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            } else if (fallbackIcon != null) {
                val iconView =
                    ImageView(ctx).apply {
                        setImageDrawable(fallbackIcon)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                container.addView(
                    iconView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
            container
        },
    )
}

@Composable
private fun DrawableImage(
    drawable: Drawable?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (drawable == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
        return
    }
    val bitmap =
        remember(drawable) {
            val sourceWidth =
                drawable.intrinsicWidth
                    .takeIf { it > 0 }
                    ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
            val sourceHeight =
                drawable.intrinsicHeight
                    .takeIf { it > 0 }
                    ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
            val scale =
                minOf(
                    1f,
                    MAX_WIDGET_PREVIEW_DIMENSION_PX.toFloat() / sourceWidth,
                    MAX_WIDGET_PREVIEW_DIMENSION_PX.toFloat() / sourceHeight,
                    kotlin.math.sqrt(
                        MAX_WIDGET_PREVIEW_PIXELS.toFloat() /
                            (sourceWidth.toFloat() * sourceHeight.toFloat()),
                    ).coerceAtMost(1f),
                )
            val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
            val height = (sourceHeight * scale).toInt().coerceAtLeast(1)
            runCatching {
                drawable.toBitmap(width = width, height = height).asImageBitmap()
            }.getOrNull()
        }
    if (bitmap == null) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Image(
            painter = BitmapPainter(bitmap),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}
