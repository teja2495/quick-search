package com.tk.quicksearch.widgetsPanel

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import com.tk.quicksearch.shared.ui.components.AppLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
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
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.theme.AppShapes

private val WidgetPickerStageHeight = 176.dp

/** Same accent tint as the drawer's search field, so tiles follow the app's accent and theme. */
@Composable
private fun widgetPickerTileColor() = AppColors.DrawerTileBackground

private const val WIDGET_PREVIEW_FALLBACK_SIZE_PX = 96
private const val MAX_WIDGET_PREVIEW_DIMENSION_PX = 1024
private const val MAX_WIDGET_PREVIEW_PIXELS = 1_048_576
private val WidgetPickerIconSize = 56.dp
private val WIDGET_PANEL_GRID_ROW_HEIGHT_DP = 80f
private val WIDGET_PANEL_GRID_GAP_DP = 8f

private data class WidgetPickerApp(
    val packageName: String,
    val profileId: Int,
    val appLabel: String,
    val icon: ImageBitmap?,
    val widgets: List<WidgetPickerProvider>,
) {
    val key: String = "$packageName:$profileId"
}

/** A provider with its label, resolved once while the list loads rather than on every draw. */
private data class WidgetPickerProvider(
    val info: AppWidgetProviderInfo,
    val label: String,
)

/** A row's preview, loaded and scaled to the row off the main thread. */
private data class WidgetPickerPreview(
    val image: ImageBitmap?,
    val icon: ImageBitmap?,
) {
    val byteCount: Int
        get() = (image?.let { it.width * it.height * 4 } ?: 0) + (icon?.let { it.width * it.height * 4 } ?: 0)
}

/**
 * Previews decoded while the picker is open, bounded by bytes, so scrolling back through an app
 * with hundreds of widgets doesn't decode each preview again. Lives only as long as the sheet.
 */
private class WidgetPickerPreviewCache {
    private val cache =
        object : LruCache<String, WidgetPickerPreview>(MAX_CACHED_PREVIEW_BYTES) {
            override fun sizeOf(key: String, value: WidgetPickerPreview): Int = value.byteCount.coerceAtLeast(1)
        }

    fun get(key: String): WidgetPickerPreview? = cache.get(key)

    fun put(key: String, preview: WidgetPickerPreview) {
        cache.put(key, preview)
    }

    fun clear() = cache.evictAll()
}

private const val MAX_CACHED_PREVIEW_BYTES = 24 * 1024 * 1024

@Composable
internal fun WidgetPickerSheet(
    appWidgetManager: AppWidgetManager,
    showQuickNote: Boolean,
    onDismiss: () -> Unit,
    onAddQuickNote: () -> Unit,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
    /** When set, the picker adds to a widget stack and lists only widgets that fit its size. */
    stackFit: WidgetStackFit? = null,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val density = LocalDensity.current
    val appIconSizePx = with(density) { DesignTokens.Spacing40.roundToPx() }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    val apps by produceState<List<WidgetPickerApp>?>(initialValue = null, appWidgetManager, packageManager, stackFit) {
        value =
            withContext(Dispatchers.IO) {
                appWidgetManager.installedProviders
                    .filter { provider -> stackFit == null || stackFit.fits(provider, density) }
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
                                    }
                                    // Kept as a bitmap at its drawn size, not the full drawable.
                                    ?.let { icon -> rasterize(icon, appIconSizePx, appIconSizePx) },
                            widgets =
                                providers
                                    .map { provider ->
                                        WidgetPickerProvider(
                                            info = provider,
                                            label =
                                                runCatching { provider.loadLabel(packageManager) }
                                                    .getOrNull()
                                                    .orEmpty(),
                                        )
                                    }.sortedBy { it.label.lowercase() },
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
    val previewCache = remember { WidgetPickerPreviewCache() }
    DisposableEffect(previewCache) { onDispose { previewCache.clear() } }
    AppPickerDrawer(
        title = stringResource(if (stackFit != null) R.string.widget_add_to_stack else R.string.widgets_picker_title),
        onDismiss = onDismiss,
        search = AppPickerDrawerSearch(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.common_search),
        ),
        header =
            if (stackFit != null) {
                {
                    Text(
                        text = stringResource(R.string.widget_stack_picker_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
    ) {
        if (apps == null) {
            WidgetPickerLoading()
        } else {
            // An app's widgets are their own lazy items, so expanding an app with hundreds of
            // widgets only composes (and loads previews for) the rows on screen.
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (showQuickNote) {
                    item(key = "quick_note", contentType = "quick_note") { QuickNotePickerRow(onClick = onAddQuickNote) }
                }
                filteredApps.forEachIndexed { appIndex, app ->
                    val isExpanded = app.key in expandedApps
                    item(key = "app:${app.key}", contentType = "app") {
                        WidgetPickerAppHeader(
                            app = app,
                            isExpanded = isExpanded,
                            onToggleExpanded = {
                                expandedApps = if (app.key in expandedApps) expandedApps - app.key else expandedApps + app.key
                            },
                            modifier =
                                Modifier
                                    .animateItem()
                                    .padding(top = if (appIndex > 0 || showQuickNote) DesignTokens.SpacingMedium else 0.dp),
                        )
                    }
                    if (isExpanded) {
                        itemsIndexed(
                            app.widgets,
                            key = { _, widget -> "widget:${app.key}:${widget.info.provider.flattenToShortString()}" },
                            contentType = { _, _ -> "widget" },
                        ) { index, widget ->
                            SegmentedTile(
                                index = index + 1,
                                count = app.widgets.size + 1,
                                onClick = { onSelectWidget(widget.info) },
                                containerColor = widgetPickerTileColor(),
                                modifier = Modifier.animateItem().padding(top = DesignTokens.SegmentedListGap),
                            ) {
                                WidgetPickerRow(
                                    pickerProvider = widget,
                                    previewCache = previewCache,
                                    onClick = { onSelectWidget(widget.info) },
                                )
                            }
                        }
                    }
                }
                if (filteredApps.isEmpty() && !showQuickNote) {
                    item {
                        AppPickerDrawerMessage(
                            text =
                                stringResource(
                                    if (stackFit != null && query.text.isBlank()) {
                                        R.string.widget_stack_picker_empty
                                    } else {
                                        R.string.widgets_picker_empty
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

/** Fills the drawer's list area so the indicator sits in its center. */
@Composable
private fun WidgetPickerLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AppLoadingIndicator()
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
private fun WidgetPickerAppHeader(
    app: WidgetPickerApp,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "widgetPickerChevronRotation",
    )
    SegmentedTile(
        index = 0,
        count = if (isExpanded) app.widgets.size + 1 else 1,
        onClick = onToggleExpanded,
        containerColor = widgetPickerTileColor(),
        modifier = modifier,
    ) {
        SegmentedTileRow(
            title = app.appLabel,
            description = stringResource(R.string.widgets_picker_widget_count, app.widgets.size),
            leading = {
                BitmapImage(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.Spacing40).clip(AppShapes.full),
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
}

@Composable
private fun WidgetPickerRow(
    pickerProvider: WidgetPickerProvider,
    previewCache: WidgetPickerPreviewCache,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val provider = pickerProvider.info
    val context = LocalContext.current
    val density = LocalDensity.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    // Previews are decoded and scaled to the stage off the main thread; the stage keeps its fixed
    // height while one loads, so rows do not shift.
    val stageWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val stageHeightPx = with(density) { WidgetPickerStageHeight.roundToPx() }
    val iconSizePx = with(density) { WidgetPickerIconSize.roundToPx() }
    val previewKey =
        remember(provider, stageWidthPx, stageHeightPx, iconSizePx) {
            "${provider.provider.flattenToShortString()}:${UserHandleUtils.getIdentifier(provider.profile)}:" +
                "$stageWidthPx:$stageHeightPx:$iconSizePx"
        }
    val preview by produceState(previewCache.get(previewKey), previewKey) {
        if (value != null) return@produceState
        value =
            withContext(Dispatchers.IO) {
                val previewImage =
                    runCatching { provider.loadPreviewImage(context, densityDpi) }.getOrNull()
                        ?: runCatching {
                            provider.loadPreviewImage(context, android.util.DisplayMetrics.DENSITY_DEVICE_STABLE)
                        }.getOrNull()
                        ?: runCatching {
                            provider.loadPreviewImage(context, android.util.DisplayMetrics.DENSITY_DEFAULT)
                        }.getOrNull()
                WidgetPickerPreview(
                    image = previewImage?.let { rasterize(it, stageWidthPx, stageHeightPx) },
                    icon =
                        runCatching { provider.loadIcon(context, densityDpi) }
                            .getOrNull()
                            ?.let { rasterize(it, iconSizePx, iconSizePx) },
                ).also { previewCache.put(previewKey, it) }
            }
    }
    val previewLayoutRes = remember(provider) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) provider.previewLayout else 0
    }
    val previewImage = preview?.image
    val widgetIcon = preview?.icon
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
                preview == null -> Unit
                previewImage != null -> {
                    BitmapImage(
                        bitmap = previewImage,
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
                    BitmapImage(
                        bitmap = widgetIcon,
                        contentDescription = null,
                        modifier = Modifier.size(WidgetPickerIconSize),
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
                    text = pickerProvider.label,
                    style = MaterialTheme.typography.bodyLarge,
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
            AppButton(onClick = onClick, icon = MaterialSymbols.Add) {
                Text(text = stringResource(R.string.common_action_add))
            }
        }
    }
}

@Composable
private fun WidgetRemoteViewsPreview(
    packageName: String,
    layoutRes: Int,
    fallbackIcon: ImageBitmap?,
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
                        setImageBitmap(fallbackIcon.asAndroidBitmap())
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
private fun BitmapImage(
    bitmap: ImageBitmap?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    if (bitmap == null) {
        Box(
            modifier = modifier.background(AppColors.AccentTintFaint),
        )
    } else {
        val painter = remember(bitmap) { BitmapPainter(bitmap) }
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * Draws [drawable] into a bitmap no larger than it is shown ([maxWidthPx] by [maxHeightPx]), and
 * never past the overall preview limits, so the picker holds pixels it draws rather than full-size
 * previews. Returns null if the drawable cannot be drawn.
 */
internal fun rasterize(
    drawable: Drawable,
    maxWidthPx: Int,
    maxHeightPx: Int,
): ImageBitmap? {
    val sourceWidth = drawable.intrinsicWidth.takeIf { it > 0 } ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
    val sourceHeight = drawable.intrinsicHeight.takeIf { it > 0 } ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
    val maxWidth = maxWidthPx.coerceIn(1, MAX_WIDGET_PREVIEW_DIMENSION_PX).toFloat()
    val maxHeight = maxHeightPx.coerceIn(1, MAX_WIDGET_PREVIEW_DIMENSION_PX).toFloat()
    val scale =
        minOf(
            1f,
            maxWidth / sourceWidth,
            maxHeight / sourceHeight,
            kotlin.math.sqrt(
                MAX_WIDGET_PREVIEW_PIXELS.toFloat() / (sourceWidth.toFloat() * sourceHeight.toFloat()),
            ).coerceAtMost(1f),
        )
    val width = (sourceWidth * scale).toInt().coerceAtLeast(1)
    val height = (sourceHeight * scale).toInt().coerceAtLeast(1)
    return runCatching { drawable.toBitmap(width = width, height = height).asImageBitmap() }.getOrNull()
}
