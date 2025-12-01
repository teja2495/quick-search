package com.tk.quicksearch.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ROW_COUNT = 2
private const val SEARCH_ROW_COUNT = 1
private const val COLUMNS = 5

private object AppIconCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap?>()

    fun get(packageName: String): ImageBitmap? = cache[packageName]

    fun put(packageName: String, bitmap: ImageBitmap?) {
        if (bitmap == null) return
        cache[packageName] = bitmap
    }
}

@Composable
fun AppGridSection(
    apps: List<AppInfo>,
    isSearching: Boolean,
    hasAppResults: Boolean,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    pinnedPackageNames: Set<String>,
    showAppLabels: Boolean,
    modifier: Modifier = Modifier,
    rowCount: Int = ROW_COUNT,
    resultSectionTitle: @Composable (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (hasAppResults) {
            resultSectionTitle(stringResource(R.string.apps_section_title))
        }
        Crossfade(targetState = apps, label = "grid") { items ->
            if (items.isEmpty()) {
                Box {}
            } else {
                AppGrid(
                    apps = items,
                    onAppClick = onAppClick,
                    onAppInfoClick = onAppInfoClick,
                    onUninstallClick = onUninstallClick,
                    onHideApp = onHideApp,
                    onPinApp = onPinApp,
                    onUnpinApp = onUnpinApp,
                    pinnedPackageNames = pinnedPackageNames,
                    showAppLabels = showAppLabels,
                    rowCount = rowCount
                )
            }
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    pinnedPackageNames: Set<String>,
    showAppLabels: Boolean,
    rowCount: Int = ROW_COUNT
) {
    val rows = remember(apps) {
        apps.take(rowCount * COLUMNS).chunked(COLUMNS)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(rowCount) { rowIndex ->
            val rowApps = rows.getOrNull(rowIndex).orEmpty()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(COLUMNS) { columnIndex ->
                    val app = rowApps.getOrNull(columnIndex)
                    if (app != null) {
                        AppGridItem(
                            modifier = Modifier.weight(1f),
                            appInfo = app,
                            onClick = { onAppClick(app) },
                            onAppInfoClick = { onAppInfoClick(app) },
                            onUninstallClick = { onUninstallClick(app) },
                            onHideApp = { onHideApp(app) },
                            onPinApp = { onPinApp(app) },
                            onUnpinApp = { onUnpinApp(app) },
                            isPinned = pinnedPackageNames.contains(app.packageName),
                            showUninstall = !app.isSystemApp,
                            showAppLabel = showAppLabels
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AppGridItem(
    modifier: Modifier = Modifier,
    appInfo: AppInfo,
    onClick: () -> Unit,
    onAppInfoClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onHideApp: () -> Unit,
    onPinApp: () -> Unit,
    onUnpinApp: () -> Unit,
    isPinned: Boolean,
    showUninstall: Boolean,
    showAppLabel: Boolean
) {
    val context = LocalContext.current
    val packageName = appInfo.packageName
    val cachedIcon = remember(packageName) { AppIconCache.get(packageName) }
    val iconBitmap by produceState(initialValue = cachedIcon, key1 = packageName) {
        val existing = AppIconCache.get(packageName)
        if (existing != null) {
            value = existing
            return@produceState
        }

        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull()
        }

        if (bitmap != null) {
            AppIconCache.put(packageName, bitmap)
        }
        value = bitmap
    }

    var showOptions by remember { mutableStateOf(false) }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val placeholderLabel = remember(appInfo.appName) {
            appInfo.appName.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = { showOptions = true }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = iconBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = stringResource(
                                R.string.desc_launch_app,
                                appInfo.appName
                            ),
                            modifier = Modifier.size(52.dp)
                        )
                    } else {
                        Text(
                            text = placeholderLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (showAppLabel) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appInfo.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        DropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false)
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_app_info)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    onAppInfoClick()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_hide_app)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    onHideApp()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(
                            if (isPinned) R.string.action_unpin_app else R.string.action_pin_app
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null
                    )
                },
                onClick = {
                    showOptions = false
                    if (isPinned) {
                        onUnpinApp()
                    } else {
                        onPinApp()
                    }
                }
            )
            if (showUninstall) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.action_uninstall_app)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showOptions = false
                        onUninstallClick()
                    }
                )
            }
        }
    }
}

