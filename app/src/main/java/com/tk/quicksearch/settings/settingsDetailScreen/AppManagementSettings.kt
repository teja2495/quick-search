package com.tk.quicksearch.settings.settingsDetailScreen

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.pm.PackageInfoCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape

private enum class AppSortOption(
    @StringRes val labelResId: Int,
) {
    ALPHABETICAL(R.string.settings_app_sort_alphabetical),
    MOST_USED(R.string.settings_app_sort_most_used),
    LEAST_USED(R.string.settings_app_sort_least_used),
    RECENTLY_USED(R.string.settings_app_sort_recently_used),
}

@Composable
fun AppManagementSettingsSection(
    apps: List<AppInfo>,
    iconPackPackage: String?,
    onRequestAppUninstall: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apps.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_apps_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = DesignTokens.SpacingLarge),
        )
        return
    }

    var selectedSortOption by rememberSaveable { mutableStateOf(AppSortOption.ALPHABETICAL) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var selectedAppForDetails by remember { mutableStateOf<AppInfo?>(null) }
    val listState = rememberLazyListState()

    val sortedApps =
        remember(apps, selectedSortOption) {
            when (selectedSortOption) {
                AppSortOption.ALPHABETICAL -> {
                    apps.sortedBy { it.appName.lowercase(Locale.getDefault()) }
                }

                AppSortOption.MOST_USED -> {
                    apps.sortedWith(
                        compareByDescending<AppInfo> { it.launchCount }
                            .thenByDescending { it.totalTimeInForeground }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) },
                    )
                }

                AppSortOption.LEAST_USED -> {
                    apps.sortedWith(
                        compareBy<AppInfo> { it.launchCount }
                            .thenBy { it.totalTimeInForeground }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) },
                    )
                }

                AppSortOption.RECENTLY_USED -> {
                    apps.sortedWith(
                        compareByDescending<AppInfo> { it.lastUsedTime }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) },
                    )
                }
            }
        }

    LaunchedEffect(selectedSortOption) {
        listState.scrollToItem(0)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Row(
                    modifier =
                        Modifier
                            .clickable { isSortMenuExpanded = true }
                            .padding(start = 4.dp, end = 4.dp, top = 0.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.settings_app_sort_by_format,
                                stringResource(selectedSortOption.labelResId),
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                DropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false },
                    shape = RoundedCornerShape(24.dp),
                    properties = PopupProperties(focusable = false),
                    containerColor = AppColors.DialogBackground,
                ) {
                    AppSortOption.entries.forEachIndexed { index, option ->
                        if (index > 0) {
                            HorizontalDivider()
                        }
                        DropdownMenuItem(
                            text = { Text(text = stringResource(option.labelResId)) },
                            onClick = {
                                selectedSortOption = option
                                isSortMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(
                    items = sortedApps,
                    key = { _, app -> app.launchCountKey() },
                ) { index, app ->
                    AppManagementRow(
                        app = app,
                        iconPackPackage = iconPackPackage,
                        onItemClick = { selectedAppForDetails = app },
                        onDeleteClick = { onRequestAppUninstall(app) },
                    )

                    if (index < sortedApps.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    selectedAppForDetails?.let { app ->
        AppDetailsDialog(
            app = app,
            onOpenAppInfo = onOpenAppInfo,
            onDismiss = { selectedAppForDetails = null },
        )
    }
}

@Composable
private fun AppManagementRow(
    app: AppInfo,
    iconPackPackage: String?,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val iconResult =
        rememberAppIcon(
            packageName = app.packageName,
            iconPackPackage = iconPackPackage,
            userHandleId = app.userHandleId,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onItemClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Box(modifier = Modifier.size(DesignTokens.IconSize), contentAlignment = Alignment.Center) {
            if (iconResult.bitmap != null) {
                Image(
                    bitmap = iconResult.bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.IconSize),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = app.appName.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.action_uninstall_app),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun AppDetailsDialog(
    app: AppInfo,
    onOpenAppInfo: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val notAvailableText = stringResource(R.string.settings_app_info_not_available)
    val dateTimeFormatter =
        remember {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        }
    val metadata =
        remember(app.launchCountKey(), context, notAvailableText, dateTimeFormatter) {
            loadAppMetadata(
                context = context,
                app = app,
                fallback = notAvailableText,
                formatter = dateTimeFormatter,
            )
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = app.appName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
                AppDetailLine(
                    label = stringResource(R.string.settings_app_info_package_name),
                    value = metadata.packageName,
                )
                AppDetailLine(
                    label = stringResource(R.string.settings_app_info_version),
                    value = metadata.versionName,
                )
                AppDetailLine(
                    label = stringResource(R.string.settings_app_info_version_code),
                    value = metadata.versionCode,
                )
                AppDetailLine(
                    label = stringResource(R.string.settings_app_info_target_sdk),
                    value = metadata.targetSdk,
                )
                AppDetailLine(
                    label = stringResource(R.string.settings_app_info_minimum_sdk),
                    value = metadata.minimumSdk,
                )
                AppDetailLine(
                    label = stringResource(R.string.settings_app_info_install_date),
                    value = metadata.installDate,
                )
                AppDetailLine(
                    label = stringResource(R.string.settings_app_info_last_update),
                    value = metadata.lastUpdate,
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_close))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    onDismiss()
                    onOpenAppInfo(app)
                },
            ) {
                Text(text = stringResource(R.string.action_app_info))
            }
        },
    )
}

@Composable
private fun AppDetailLine(
    label: String,
    value: String,
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatDateTime(
    timestampMillis: Long,
    formatter: DateFormat,
    fallback: String,
): String {
    if (timestampMillis <= 0L) return fallback
    return runCatching { formatter.format(Date(timestampMillis)) }.getOrDefault(fallback)
}

private data class AppMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    val targetSdk: String,
    val minimumSdk: String,
    val installDate: String,
    val lastUpdate: String,
)

private fun loadAppMetadata(
    context: Context,
    app: AppInfo,
    fallback: String,
    formatter: DateFormat,
): AppMetadata {
    val packageInfo =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    app.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(app.packageName, 0)
            }
        }.getOrNull()

    val applicationInfo = packageInfo?.applicationInfo

    return AppMetadata(
        packageName = app.packageName,
        versionName = packageInfo?.versionName?.takeIf { it.isNotBlank() } ?: fallback,
        versionCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it).toString() } ?: fallback,
        targetSdk = applicationInfo?.targetSdkVersion?.takeIf { it > 0 }?.toString() ?: fallback,
        minimumSdk =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                applicationInfo?.minSdkVersion?.takeIf { it > 0 }?.toString() ?: fallback
            } else {
                fallback
            },
        installDate =
            formatDateTime(
                timestampMillis = packageInfo?.firstInstallTime ?: app.firstInstallTime,
                formatter = formatter,
                fallback = fallback,
            ),
        lastUpdate =
            formatDateTime(
                timestampMillis = packageInfo?.lastUpdateTime ?: 0L,
                formatter = formatter,
                fallback = fallback,
            ),
    )
}
