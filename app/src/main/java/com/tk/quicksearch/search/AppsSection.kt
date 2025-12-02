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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.AppInfo

private const val ROW_COUNT = 2
private const val COLUMNS = 5

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
    val rows = remember(apps, rowCount) {
        apps.take(rowCount * COLUMNS).chunked(COLUMNS)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(rowCount) { rowIndex ->
            val rowApps = rows.getOrNull(rowIndex).orEmpty()
            AppGridRow(
                apps = rowApps,
                onAppClick = onAppClick,
                onAppInfoClick = onAppInfoClick,
                onUninstallClick = onUninstallClick,
                onHideApp = onHideApp,
                onPinApp = onPinApp,
                onUnpinApp = onUnpinApp,
                pinnedPackageNames = pinnedPackageNames,
                showAppLabels = showAppLabels
            )
        }
    }
}

@Composable
private fun AppGridRow(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onAppInfoClick: (AppInfo) -> Unit,
    onUninstallClick: (AppInfo) -> Unit,
    onHideApp: (AppInfo) -> Unit,
    onPinApp: (AppInfo) -> Unit,
    onUnpinApp: (AppInfo) -> Unit,
    pinnedPackageNames: Set<String>,
    showAppLabels: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(COLUMNS) { columnIndex ->
            val app = apps.getOrNull(columnIndex)
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
    val iconBitmap = rememberAppIcon(appInfo.packageName)
    var showOptions by remember { mutableStateOf(false) }
    val placeholderLabel = remember(appInfo.appName) {
        appInfo.appName.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AppIconSurface(
                iconBitmap = iconBitmap,
                placeholderLabel = placeholderLabel,
                appName = appInfo.appName,
                onClick = onClick,
                onLongClick = { showOptions = true }
            )
            if (showAppLabel) {
                AppLabelText(appInfo.appName)
            }
        }

        AppItemDropdownMenu(
            expanded = showOptions,
            onDismiss = { showOptions = false },
            isPinned = isPinned,
            showUninstall = showUninstall,
            onAppInfoClick = onAppInfoClick,
            onHideApp = onHideApp,
            onPinApp = onPinApp,
            onUnpinApp = onUnpinApp,
            onUninstallClick = onUninstallClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIconSurface(
    iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    placeholderLabel: String,
    appName: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = stringResource(
                        R.string.desc_launch_app,
                        appName
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
}

@Composable
private fun AppLabelText(appName: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = appName,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

