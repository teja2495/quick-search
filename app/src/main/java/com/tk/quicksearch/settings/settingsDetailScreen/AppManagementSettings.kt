package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.components.AppFabTone
import com.tk.quicksearch.shared.ui.components.AppExtendedFloatingActionButton
import com.tk.quicksearch.search.apps.appLock.AppLockGate
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuItem
import com.tk.quicksearch.shared.ui.components.AppDropdownMenuDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.settings.appShortcutsSettings.shortcutMatchPriority
import com.tk.quicksearch.shared.ui.components.AppDropdownMenu
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import java.util.Locale
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle

private enum class AppSortOption(
    @StringRes val labelResId: Int,
    val requiresUsagePermission: Boolean = false,
) {
    NAME(R.string.settings_app_sort_name),
    APK_SIZE(R.string.settings_app_sort_apk_size),
    MOST_USED(R.string.common_most_used, requiresUsagePermission = true),
    LEAST_USED(R.string.settings_app_sort_least_used, requiresUsagePermission = true),
    INSTALLATION_DATE(R.string.settings_app_info_install_date),
    LAST_UPDATE(R.string.settings_app_info_last_update),
    API_LEVEL(R.string.settings_app_sort_api_level),
}

@Composable
fun AppManagementSettingsSection(
    apps: List<AppInfo>,
    hasUsagePermission: Boolean,
    iconPackPackage: String?,
    searchQuery: String = "",
    onRequestAppUninstall: (AppInfo) -> Unit,
    onOpenAppInfo: (AppInfo) -> Unit,
    onRefreshApps: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val normalizedSearchQuery =
        remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
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
    var sortButtonWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    var selectedAppKeys by rememberSaveable { mutableStateOf(setOf<String>()) }
    var uninstallQueueKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentUninstallKey by remember { mutableStateOf<String?>(null) }
    var waitingForUninstallReturn by remember { mutableStateOf(false) }
    var leftScreenForUninstall by remember { mutableStateOf(false) }
    var batchUninstallInProgress by remember { mutableStateOf(false) }
    var selectedAppForDetails by remember { mutableStateOf<AppInfo?>(null) }
    val listState = rememberLazyListState()
    val appByKey = remember(apps) { apps.associateBy { it.launchCountKey() } }
    val availableSortOptions =
        remember(hasUsagePermission) {
            AppSortOption.entries.filter { !it.requiresUsagePermission || hasUsagePermission }
        }

    LaunchedEffect(availableSortOptions, selectedSortOption) {
        if (selectedSortOption !in availableSortOptions) {
            selectedSortOption = AppSortOption.NAME
        }
    }
    val pageData = LocalSettingsPageData.current ?: remember { SettingsPageData() }
    var appSortMetadataByKey by remember(apps) { mutableStateOf<Map<String, AppSortMetadata>?>(null) }
    LaunchedEffect(pageData, context, apps) {
        appSortMetadataByKey = pageData.appMetadata(context.applicationContext, apps)
    }

    val selectedCount = selectedAppKeys.size
    var sortedApps by remember(apps) { mutableStateOf<List<AppInfo>?>(null) }
    val metadataForSort =
        when (selectedSortOption) {
            AppSortOption.APK_SIZE, AppSortOption.INSTALLATION_DATE, AppSortOption.LAST_UPDATE, AppSortOption.API_LEVEL ->
                appSortMetadataByKey
            else -> emptyMap()
        }
    LaunchedEffect(apps, normalizedSearchQuery, locale, selectedSortOption, isSortAscending, metadataForSort) {
        val metadata = metadataForSort ?: return@LaunchedEffect
        sortedApps = withContext(Dispatchers.Default) {
            sortManagementApps(apps, normalizedSearchQuery, locale, selectedSortOption, isSortAscending, metadata)
        }
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

    BackHandler(enabled = selectedCount > 0) {
        selectedAppKeys = emptySet()
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
            modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.SpacingMedium),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.onSizeChanged { sortButtonWidthPx = it.width }) {
                TonalButton(
                    text = stringResource(
                        R.string.settings_app_sort_by_format,
                        stringResource(selectedSortOption.labelResId),
                    ),
                    onClick = { isSortMenuExpanded = true },
                    icon = MaterialSymbols.ExpandMore,
                )
                AppDropdownMenu(
                    expanded = isSortMenuExpanded,
                    onDismissRequest = { isSortMenuExpanded = false },
                    modifier = Modifier.width(with(density) { sortButtonWidthPx.toDp() }),
                    offset = DpOffset(0.dp, DesignTokens.SpacingSmall),
                ) {
                    availableSortOptions.forEachIndexed { index, option ->
                        if (index > 0) AppDropdownMenuDivider()
                        AppDropdownMenuItem(
                            text = stringResource(option.labelResId),
                            icon = if (option == selectedSortOption) MaterialSymbols.Check else null,
                            iconTint = AppColors.Accent,
                            onClick = {
                                selectedSortOption = option
                                isSortMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            AppIconButton(
                onClick = { isSortAscending = !isSortAscending },
                tooltip =
                    stringResource(
                        if (isSortAscending) R.string.settings_app_sort_ascending else R.string.settings_app_sort_descending,
                    ),
                style = AppIconButtonStyle.Tonal,
            ) {
                Icon(
                    imageVector =
                        if (isSortAscending) {
                            MaterialSymbols.ArrowUpward
                        } else {
                            MaterialSymbols.ArrowDownward
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

        val displayedApps = sortedApps
        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (displayedApps != null) Box(modifier = Modifier.fillMaxWidth()) {
                if (displayedApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_apps_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
                        contentPadding = PaddingValues(
                            bottom = if (selectedCount > 0) {
                                DesignTokens.FloatingActionHeight + DesignTokens.Spacing48
                            } else DesignTokens.SpacingSmall,
                        ),
                    ) {
                        itemsIndexed(
                            items = displayedApps,
                            key = { _, app -> app.launchCountKey() },
                        ) { index, app ->
                            val appKey = app.launchCountKey()
                            val canUninstall = !app.isSystemApp

                            SegmentedTile(
                                index = index,
                                count = displayedApps.size,
                                onClick = { selectedAppForDetails = app },
                                containerColor = if (appKey in selectedAppKeys) {
                                    AppColors.AccentTintTonal
                                        .compositeOver(AppColors.getSettingsCardContainerColor())
                                } else AppColors.getSettingsCardContainerColor(),
                            ) {
                                AppManagementRow(
                                    app = app,
                                    apkSizeBytes = appSortMetadataByKey?.get(appKey)?.sizeBytes,
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
                                )
                            }
                        }
                    }
                }
            }

            if (selectedCount > 0) {
                AppExtendedFloatingActionButton(
                    onClick = {
                        val queuedApps = sortedApps.orEmpty().filter { it.launchCountKey() in selectedAppKeys }
                        val startUninstallQueue = {
                            val queueKeys = queuedApps.map { it.launchCountKey() }
                            uninstallQueueKeys = queueKeys
                            if (queueKeys.isNotEmpty()) {
                                waitingForUninstallReturn = false
                                leftScreenForUninstall = false
                                currentUninstallKey = null
                                batchUninstallInProgress = true
                            }
                        }
                        // One prompt up front covers every locked app in the batch, so the queue
                        // never stalls on a cancelled prompt halfway through.
                        val lockedApps = queuedApps.filter { AppLockGate.isProtected(context, it.packageName) }
                        if (lockedApps.isEmpty()) {
                            startUninstallQueue()
                        } else {
                            AppLockGate.runAfterUnlock(
                                context,
                                lockedApps.first().packageName,
                                lockedApps.joinToString { it.appName },
                                action = startUninstallQueue,
                            )
                        }
                    },
                    text =
                        stringResource(
                            R.string.settings_app_uninstall_selected_format,
                            selectedCount,
                        ),
                    icon = MaterialSymbols.DeleteOutline,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(DesignTokens.SpacingLarge),
                    tone = AppFabTone.Error,
                )
            }
        }
    }

    selectedAppForDetails?.let { app ->
        AppDetailsDialog(
            app = app,
            iconPackPackage = iconPackPackage,
            onOpenAppInfo = { detailsApp ->
                AppLockGate.runAfterUnlock(context, detailsApp.packageName, detailsApp.appName) {
                    onOpenAppInfo(detailsApp)
                }
            },
            onDismiss = { selectedAppForDetails = null },
        )
    }
}

@Composable
private fun AppManagementRow(
    app: AppInfo,
    apkSizeBytes: Long?,
    iconPackPackage: String?,
    isSelected: Boolean,
    checkboxEnabled: Boolean,
    onSelectionChanged: (Boolean) -> Unit,
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
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        Box(modifier = Modifier.size(DesignTokens.Spacing40), contentAlignment = Alignment.Center) {
            if (iconResult.bitmap != null) {
                Image(
                    bitmap = iconResult.bitmap,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.Spacing40),
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall / 2),
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    apkSizeBytes?.let {
                        stringResource(R.string.settings_app_apk_size_format, it.toDouble() / (1024.0 * 1024.0))
                    }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged,
            enabled = checkboxEnabled,
            modifier = Modifier.semantics { contentDescription = app.appName },
        )
    }
}

private fun sortManagementApps(
    apps: List<AppInfo>,
    normalizedSearchQuery: String,
    locale: Locale,
    selectedSortOption: AppSortOption,
    isSortAscending: Boolean,
    appSortMetadataByKey: Map<String, AppSortMetadata>,
): List<AppInfo> {
    val nameKeys = mutableMapOf<String, String>()
    fun nameKey(app: AppInfo): String = nameKeys.getOrPut(app.appName) { app.appName.lowercase(locale) }

    val filteredApps =
        if (normalizedSearchQuery.isBlank()) {
            apps
        } else {
            apps
                .mapNotNull { app ->
                    val priority =
                        shortcutMatchPriority(
                            name = app.appName,
                            query = normalizedSearchQuery,
                            locale = locale,
                        ) ?: return@mapNotNull null
                    app to priority
                }.sortedWith(
                    compareBy<Pair<AppInfo, com.tk.quicksearch.settings.appShortcutsSettings.ShortcutSearchMatchPriority>> { it.second.ordinal }
                        .thenBy { nameKey(it.first) },
                )
                .map { it.first }
        }

    val baseComparator =
        when (selectedSortOption) {
            AppSortOption.NAME -> {
                compareBy<AppInfo> { nameKey(it) }
            }

            AppSortOption.APK_SIZE -> {
                compareBy<AppInfo> { app -> appSortMetadataByKey[app.launchCountKey()]?.sizeBytes ?: 0L }
                    .thenBy { nameKey(it) }
            }

            AppSortOption.MOST_USED -> {
                compareByDescending<AppInfo> { it.launchCount }
                    .thenByDescending { it.totalTimeInForeground }
                    .thenBy { nameKey(it) }
            }

            AppSortOption.LEAST_USED -> {
                compareBy<AppInfo> { it.launchCount }
                    .thenBy { it.totalTimeInForeground }
                    .thenBy { nameKey(it) }
            }

            AppSortOption.INSTALLATION_DATE -> {
                compareBy<AppInfo> { app ->
                    appSortMetadataByKey[app.launchCountKey()]?.installTimeMillis ?: app.firstInstallTime
                }
                    .thenBy { nameKey(it) }
            }

            AppSortOption.LAST_UPDATE -> {
                compareBy<AppInfo> { app -> appSortMetadataByKey[app.launchCountKey()]?.lastUpdateTimeMillis ?: 0L }
                    .thenBy { nameKey(it) }
            }

            AppSortOption.API_LEVEL -> {
                compareBy<AppInfo> { app -> appSortMetadataByKey[app.launchCountKey()]?.targetSdk ?: 0 }
                    .thenBy { nameKey(it) }
            }
        }

    val comparator = if (isSortAscending) baseComparator else baseComparator.reversed()
    return filteredApps.sortedWith(comparator)
}
