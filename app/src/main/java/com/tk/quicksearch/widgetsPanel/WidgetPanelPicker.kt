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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val WIDGET_PREVIEW_FALLBACK_SIZE_PX = 96
private val WIDGET_PANEL_GRID_ROW_HEIGHT_DP = 80f
private val WIDGET_PANEL_GRID_GAP_DP = 8f

private data class WidgetPickerApp(
    val packageName: String,
    val appLabel: String,
    val icon: Drawable?,
    val widgets: List<AppWidgetProviderInfo>,
)

@Composable
internal fun WidgetPickerSheet(
    appWidgetManager: AppWidgetManager,
    onDismiss: () -> Unit,
    onSelectWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    var query by rememberSaveable { mutableStateOf("") }
    val apps by produceState<List<WidgetPickerApp>?>(initialValue = null, appWidgetManager, packageManager) {
        value =
            withContext(Dispatchers.Default) {
                appWidgetManager.installedProviders
                    .groupBy { it.provider.packageName }
                    .map { (packageName, providers) ->
                        val appInfo =
                            runCatching { packageManager.getApplicationInfo(packageName, 0) }
                                .getOrNull()
                        WidgetPickerApp(
                            packageName = packageName,
                            appLabel =
                                appInfo
                                    ?.let { packageManager.getApplicationLabel(it).toString() }
                                    ?: packageName,
                            icon = appInfo?.let { runCatching { packageManager.getApplicationIcon(it) }.getOrNull() },
                            widgets =
                                providers.sortedBy {
                                    it.loadLabel(packageManager)?.toString().orEmpty().lowercase()
                                },
                        )
                    }
                    .sortedBy { it.appLabel.lowercase() }
            }
    }
    val filteredApps =
        remember(apps, query) {
            val loadedApps = apps.orEmpty()
            val normalizedQuery = query.trim().lowercase()
            if (normalizedQuery.isBlank()) {
                loadedApps
            } else {
                loadedApps.filter { app -> app.appLabel.lowercase().contains(normalizedQuery) }
            }
        }
    var expandedApps by rememberSaveable { mutableStateOf(setOf<String>()) }
    val blockSheetDragFromListScroll =
        remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset = if (available.y < 0f) available else Offset.Zero

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                ): Velocity = if (available.y < 0f) available else Velocity.Zero
            }
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
        )

        Surface(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = AppColors.getDialogContainerColor(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 720.dp)
                        .padding(top = DesignTokens.SpacingMedium)
                        .padding(horizontal = DesignTokens.ContentHorizontalPadding)
                        .padding(bottom = DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                )

                Text(
                    text = stringResource(R.string.widgets_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (apps == null) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.widgets_picker_loading),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    WidgetPickerSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .nestedScroll(blockSheetDragFromListScroll),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            WidgetPickerAppGroup(
                                app = app,
                                isExpanded = expandedApps.contains(app.packageName),
                                onToggleExpanded = {
                                    expandedApps =
                                        if (expandedApps.contains(app.packageName)) {
                                            expandedApps - app.packageName
                                        } else {
                                            expandedApps + app.packageName
                                        }
                                },
                                packageManager = packageManager,
                                onSelectWidget = onSelectWidget,
                            )
                        }
                        if (filteredApps.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.widgets_picker_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(DesignTokens.SpacingLarge),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = DesignTokens.ShapeXXLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.SpacingMedium,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (query.isBlank()) {
                        Text(
                            text = stringResource(R.string.common_search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DesignTokens.ExtraLargeCardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpanded)
                        .padding(DesignTokens.SpacingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                DrawableImage(
                    drawable = app.icon,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(DesignTokens.LargeIconSize)
                            .clip(CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.widgets_picker_widget_count,
                                app.widgets.size,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isExpanded) {
                app.widgets.forEachIndexed { index, provider ->
                    if (index > 0) {
                        HorizontalDivider(color = AppColors.SettingsDivider)
                    }
                    WidgetPickerRow(
                        provider = provider,
                        packageManager = packageManager,
                        onClick = { onSelectWidget(provider) },
                    )
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
                .clickable(onClick = onClick)
                .padding(DesignTokens.SpacingLarge),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 92.dp),
            shape = DesignTokens.ShapeMedium,
            color = Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    previewImage != null -> {
                        DrawableImage(
                            drawable = previewImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(DesignTokens.SpacingSmall),
                        )
                    }
                    previewLayoutRes != 0 -> {
                        WidgetRemoteViewsPreview(
                            packageName = provider.provider.packageName,
                            layoutRes = previewLayoutRes,
                            fallbackIcon = widgetIcon,
                            modifier = Modifier.fillMaxSize().padding(DesignTokens.SpacingSmall),
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
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = provider.loadLabel(packageManager)?.toString().orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                text =
                    stringResource(
                        R.string.widgets_picker_widget_grid_size,
                        initialGridSize.first,
                        initialGridSize.second,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                AssistChip(
                    onClick = onClick,
                    label = { Text(stringResource(R.string.common_action_add)) },
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
            val width =
                drawable.intrinsicWidth
                    .takeIf { it > 0 }
                    ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
            val height =
                drawable.intrinsicHeight
                    .takeIf { it > 0 }
                    ?: WIDGET_PREVIEW_FALLBACK_SIZE_PX
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
