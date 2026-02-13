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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.ui.theme.AppColors
import com.tk.quicksearch.ui.theme.DesignTokens
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape

private enum class AppSortOption(
    @StringRes val labelResId: Int,
) {
    NAME(R.string.settings_app_sort_name),
    APK_SIZE(R.string.settings_app_sort_apk_size),
    MOST_USED(R.string.settings_app_sort_most_used),
    LEAST_USED(R.string.settings_app_sort_least_used),
    INSTALLATION_DATE(R.string.settings_app_sort_installation_date),
    LAST_UPDATE(R.string.settings_app_sort_last_update),
    API_LEVEL(R.string.settings_app_sort_api_level),
}

@Composable
fun AppManagementSettingsSection(
    apps: List<AppInfo>,
    iconPackPackage: String?,
    onRequestAppUninstall: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onRefreshApps: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    if (apps.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_apps_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(vertical = DesignTokens.SpacingLarge),
        )
        return
    }

    var selectedSortOption by rememberSaveable { mutableStateOf(AppSortOption.NAME) }
    var isSortAscending by rememberSaveable { mutableStateOf(true) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var selectedAppKeys by rememberSaveable { mutableStateOf(setOf<String>()) }
    var uninstallQueueKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentUninstallKey by remember { mutableStateOf<String?>(null) }
    var waitingForUninstallReturn by remember { mutableStateOf(false) }
    var leftScreenForUninstall by remember { mutableStateOf(false) }
    var batchUninstallInProgress by remember { mutableStateOf(false) }
    var selectedAppForDetails by remember { mutableStateOf<AppInfo?>(null) }
    val listState = rememberLazyListState()
    val appByKey = remember(apps) { apps.associateBy { it.launchCountKey() } }
    val appSortMetadataByKey =
        remember(apps, context) {
            apps.associate { app ->
                app.launchCountKey() to loadAppSortMetadata(context, app)
            }
        }
    val selectedCount = selectedAppKeys.size

    val sortedApps =
        remember(apps, selectedSortOption, isSortAscending, appSortMetadataByKey) {
            val baseComparator =
                when (selectedSortOption) {
                    AppSortOption.NAME -> {
                        compareBy<AppInfo> { it.appName.lowercase(Locale.getDefault()) }
                    }

                    AppSortOption.APK_SIZE -> {
                        compareBy<AppInfo> { app -> appSortMetadataByKey[app.launchCountKey()]?.sizeBytes ?: 0L }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) }
                    }

                    AppSortOption.MOST_USED -> {
                        compareByDescending<AppInfo> { it.launchCount }
                            .thenByDescending { it.totalTimeInForeground }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) }
                    }

                    AppSortOption.LEAST_USED -> {
                        compareBy<AppInfo> { it.launchCount }
                            .thenBy { it.totalTimeInForeground }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) }
                    }

                    AppSortOption.INSTALLATION_DATE -> {
                        compareBy<AppInfo> { app ->
                            appSortMetadataByKey[app.launchCountKey()]?.installTimeMillis ?: app.firstInstallTime
                        }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) }
                    }

                    AppSortOption.LAST_UPDATE -> {
                        compareBy<AppInfo> { app -> appSortMetadataByKey[app.launchCountKey()]?.lastUpdateTimeMillis ?: 0L }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) }
                    }

                    AppSortOption.API_LEVEL -> {
                        compareBy<AppInfo> { app -> appSortMetadataByKey[app.launchCountKey()]?.targetSdk ?: 0 }
                            .thenBy { it.appName.lowercase(Locale.getDefault()) }
                    }
                }

            val comparator = if (isSortAscending) baseComparator else baseComparator.reversed()
            apps.sortedWith(comparator)
        }

    LaunchedEffect(selectedSortOption, isSortAscending) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(apps) {
        val currentKeys = apps.mapTo(mutableSetOf()) { it.launchCountKey() }
        selectedAppKeys = selectedAppKeys.intersect(currentKeys)
        uninstallQueueKeys = uninstallQueueKeys.filter { it in currentKeys }
        if (currentUninstallKey != null && currentUninstallKey !in currentKeys) {
            currentUninstallKey = null
        }
    }

    LaunchedEffect(uninstallQueueKeys, currentUninstallKey) {
        if (currentUninstallKey != null) return@LaunchedEffect
        val nextKey = uninstallQueueKeys.firstOrNull() ?: return@LaunchedEffect
        val nextApp = appByKey[nextKey] ?: return@LaunchedEffect
        currentUninstallKey = nextKey
        waitingForUninstallReturn = true
        leftScreenForUninstall = false
        onRequestAppUninstall(nextApp)
    }

    LaunchedEffect(uninstallQueueKeys, currentUninstallKey, waitingForUninstallReturn, batchUninstallInProgress) {
        if (!batchUninstallInProgress) return@LaunchedEffect
        if (uninstallQueueKeys.isNotEmpty()) return@LaunchedEffect
        if (currentUninstallKey != null) return@LaunchedEffect
        if (waitingForUninstallReturn) return@LaunchedEffect

        batchUninstallInProgress = false
        onRefreshApps(false)
    }

    DisposableEffect(lifecycleOwner, waitingForUninstallReturn, currentUninstallKey) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (!waitingForUninstallReturn) return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP -> {
                        leftScreenForUninstall = true
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        if (!leftScreenForUninstall) return@LifecycleEventObserver
                        val finishedKey = currentUninstallKey
                        if (finishedKey != null) {
                            uninstallQueueKeys = uninstallQueueKeys.drop(1)
                            selectedAppKeys = selectedAppKeys - finishedKey
                        }
                        currentUninstallKey = null
                        waitingForUninstallReturn = false
                        leftScreenForUninstall = false
                    }

                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                modifier = Modifier.offset(y = (-3).dp),
                onClick = { isSortAscending = !isSortAscending },
            ) {
                Icon(
                    imageVector =
                        if (isSortAscending) {
                            Icons.Rounded.ArrowUpward
                        } else {
                            Icons.Rounded.ArrowDownward
                        },
                    contentDescription =
                        if (isSortAscending) {
                            stringResource(R.string.settings_app_sort_ascending)
                        } else {
                            stringResource(R.string.settings_app_sort_descending)
                        },
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            ElevatedCard(
                modifier = Modifier.fillMaxSize(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    itemsIndexed(
                        items = sortedApps,
                        key = { _, app -> app.launchCountKey() },
                    ) { index, app ->
                        val appKey = app.launchCountKey()
                        val canUninstall = !app.isSystemApp && app.userHandleId == null

                        AppManagementRow(
                            app = app,
                            apkSizeBytes = appSortMetadataByKey[appKey]?.sizeBytes ?: 0L,
                            iconPackPackage = iconPackPackage,
                            isSelected = appKey in selectedAppKeys,
                            checkboxEnabled = canUninstall,
                            onSelectionChanged = { checked ->
                                selectedAppKeys =
                                    if (checked) {
                                        selectedAppKeys + appKey
                                    } else {
                                        selectedAppKeys - appKey
                                    }
                            },
                            onItemClick = { selectedAppForDetails = app },
                        )

                        if (index < sortedApps.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            if (selectedCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val queueKeys = sortedApps.map { it.launchCountKey() }.filter { it in selectedAppKeys }
                        uninstallQueueKeys = queueKeys
                        if (queueKeys.isNotEmpty()) {
                            waitingForUninstallReturn = false
                            leftScreenForUninstall = false
                            currentUninstallKey = null
                            batchUninstallInProgress = true
                        }
                    },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(
                            text =
                                stringResource(
                                    R.string.settings_app_uninstall_selected_format,
                                    selectedCount,
                                ),
                        )
                    },
                )
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
    apkSizeBytes: Long,
    iconPackPackage: String?,
    isSelected: Boolean,
    checkboxEnabled: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
    onItemClick: () -> Unit,
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
                text =
                    stringResource(
                        R.string.settings_app_apk_size_format,
                        apkSizeBytes.toDouble() / (1024.0 * 1024.0),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged,
            enabled = checkboxEnabled,
        )
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

private data class AppSortMetadata(
    val sizeBytes: Long,
    val installTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val targetSdk: Int,
)

private fun loadAppSortMetadata(
    context: Context,
    app: AppInfo,
): AppSortMetadata {
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

    val sourceDir = packageInfo?.applicationInfo?.sourceDir
    val appSizeBytes = sourceDir?.let { path -> runCatching { File(path).length() }.getOrDefault(0L) } ?: 0L

    return AppSortMetadata(
        sizeBytes = appSizeBytes,
        installTimeMillis = packageInfo?.firstInstallTime ?: app.firstInstallTime,
        lastUpdateTimeMillis = packageInfo?.lastUpdateTime ?: 0L,
        targetSdk = packageInfo?.applicationInfo?.targetSdkVersion ?: 0,
    )
}

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
