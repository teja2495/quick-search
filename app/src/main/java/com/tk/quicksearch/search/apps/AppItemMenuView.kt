package com.tk.quicksearch.search.apps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontFamily
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.core.LocalItemCustomizationRemover
import com.tk.quicksearch.search.appSettings.AppSettingsDestination
import com.tk.quicksearch.search.appSettings.LocalOpenAppSettingDestination
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppsRepository
import com.tk.quicksearch.search.data.TodayAppUsage
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.appShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.pinnedNotifications.PinnedNotifications
import com.tk.quicksearch.search.apps.speedBump.SpeedBump
import com.tk.quicksearch.search.apps.speedBump.SpeedBumpExplainerDialog
import com.tk.quicksearch.search.apps.appLock.AppLock
import com.tk.quicksearch.search.apps.appLock.LocalAppLockAuthenticator
import com.tk.quicksearch.search.apps.appLock.LocalAppLockCredentialAuthenticator
import com.tk.quicksearch.search.apps.swipeGestures.AppSwipeDirection
import com.tk.quicksearch.search.apps.swipeGestures.AppSwipeGestures
import com.tk.quicksearch.search.apps.swipeGestures.rememberAppSwipeActions
import com.tk.quicksearch.shared.ui.components.ItemMenuLongPressOption
import com.tk.quicksearch.shared.ui.components.ItemMenuPopup
import com.tk.quicksearch.shared.ui.components.ItemMenuRow
import com.tk.quicksearch.shared.ui.components.ItemMenuTile
import com.tk.quicksearch.shared.ui.components.itemMenuRemoveOption
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import com.tk.quicksearch.shared.util.cachedDefaultHomeAppStatus
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonAction
import com.tk.quicksearch.widgets.customButtonsWidget.CustomWidgetButtonIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ShortcutGridIconSize = 24.dp
private const val AppUnlockCredentialHoldMillis = 4_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItemDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isPinned: Boolean,
    showUninstall: Boolean,
    hasNickname: Boolean,
    hasTrigger: Boolean,
    shortcuts: List<StaticShortcut>,
    appInfo: AppInfo,
    iconPackPackage: String?,
    appIconShape: AppIconShape,
    onShortcutClick: (StaticShortcut) -> Unit,
    onDisableShortcut: (StaticShortcut) -> Unit = {},
    onAppInfoClick: () -> Unit,
    onHideApp: () -> Unit,
    onPinApp: () -> Unit,
    onUnpinApp: () -> Unit,
    onUninstallClick: () -> Unit,
    onNicknameClick: () -> Unit,
    onTriggerClick: () -> Unit,
    onAddToHome: () -> Unit,
    onOpenInSplitScreen: () -> Unit,
    /** False where swiping the item can't run the app's swipe gestures, such as list rows. */
    showSwipeGestures: Boolean = true,
) {
    val context = LocalContext.current
    val todayUsage by produceState<TodayAppUsage?>(
        initialValue = null,
        key1 = expanded,
        key2 = appInfo.packageName,
    ) {
        if (expanded) {
            value = withContext(Dispatchers.IO) {
                AppsRepository(context.applicationContext).getTodayAppUsage(appInfo.packageName)
            }
        }
    }
    val versionName by produceState<String?>(
        initialValue = null,
        key1 = expanded,
        key2 = appInfo.packageName,
    ) {
        if (expanded) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(appInfo.packageName, 0).versionName
                }.getOrNull()?.takeIf { it.isNotBlank() }
            }
        }
    }
    val isCurrentApp = appInfo.packageName == context.packageName
    val isLaunchableApp = appInfo.hasLaunchIntent
    val notificationAction =
        CustomWidgetButtonAction.App(
            packageName = appInfo.packageName,
            appName = appInfo.appName,
            userHandleId = appInfo.userHandleId,
        )
    val isPinnedToNotifications = PinnedNotifications.isPinned(context, notificationAction)
    val hasIconOverride = remember(appInfo.packageName, expanded) {
        UserAppPreferences(context).getAppIconOverride(appInfo.packageName)?.useSystemDefault == false
    }
    val showIconPicker = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var speedBumpEnabled by remember(appInfo.packageName, expanded) {
        mutableStateOf(SpeedBump.isEnabled(context, appInfo.packageName))
    }
    var appLockEnabled by remember(appInfo.packageName, expanded) {
        mutableStateOf(AppLock.isLocked(context, appInfo.packageName))
    }
    val isDefaultLauncher = context.cachedDefaultHomeAppStatus()
    val openAppSettingDestination = LocalOpenAppSettingDestination.current
    val authenticate = LocalAppLockAuthenticator.current
    val authenticateWithDeviceCredential = LocalAppLockCredentialAuthenticator.current
    // Non-null while the explainer is up; true when it followed the user first turning it on.
    var speedBumpExplainerJustEnabled by remember { mutableStateOf<Boolean?>(null) }
    val customizationRemover = LocalItemCustomizationRemover.current
    // Removing keeps the menu open, so the menu tracks it until it's next opened.
    var triggerRemoved by remember(expanded) { mutableStateOf(false) }
    var nicknameRemoved by remember(expanded) { mutableStateOf(false) }
    val triggerSet = hasTrigger && !triggerRemoved
    val nicknameSet = hasNickname && !nicknameRemoved
    val removeTriggerOption = itemMenuRemoveOption(
        customizationRemover?.takeIf { triggerSet }?.let { remover -> { triggerRemoved = true; remover.removeAppTrigger(appInfo) } },
    )
    val removeNicknameOption = itemMenuRemoveOption(
        customizationRemover?.takeIf { nicknameSet }?.let { remover -> { nicknameRemoved = true; remover.removeAppNickname(appInfo) } },
    )
    val isOtherLaunchableApp = !isCurrentApp && isLaunchableApp
    val actions = buildList {
        if (isOtherLaunchableApp) {
            add(ItemMenuTile(
                label = stringResource(if (isPinned) R.string.action_unpin_app else R.string.action_pin_app),
                icon = {
                    Icon(
                        painter = painterResource(if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin),
                        contentDescription = null,
                        tint = if (isPinned) AppColors.ItemMenuActiveIconTint else LocalContentColor.current,
                    )
                },
                onClick = { onDismiss(); if (isPinned) onUnpinApp() else onPinApp() },
            ))
        }
        if (!isCurrentApp) {
            add(ItemMenuTile(
                label = stringResource(R.string.action_add_trigger),
                icon = { Icon(imageVector = MaterialSymbols.Bolt, contentDescription = null, tint = if (triggerSet) AppColors.ItemMenuActiveIconTint else LocalContentColor.current) },
                onClick = { onDismiss(); onTriggerClick() },
                longPressOption = removeTriggerOption,
            ))
        }
        if (!isCurrentApp) {
            add(ItemMenuTile(
                label = stringResource(R.string.common_nickname),
                icon = { Icon(imageVector = MaterialSymbols.Edit, contentDescription = null, tint = if (nicknameSet) AppColors.ItemMenuActiveIconTint else LocalContentColor.current) },
                onClick = { onDismiss(); onNicknameClick() },
                longPressOption = removeNicknameOption,
            ))
        }
        add(ItemMenuTile(
            label = stringResource(R.string.action_exclude_generic),
            icon = { Icon(imageVector = MaterialSymbols.VisibilityOff, contentDescription = null) },
            onClick = { onDismiss(); onHideApp() },
        ))
    }

    val speedBumpAction =
        if (isOtherLaunchableApp) {
            ItemMenuRow(
                label = stringResource(R.string.speed_bump_title),
                icon = {
                    Icon(
                        imageVector = MaterialSymbols.Spa,
                        contentDescription = null,
                        tint = if (speedBumpEnabled) AppColors.ItemMenuActiveIconTint else LocalContentColor.current,
                    )
                },
                trailingText = stringResource(
                    if (speedBumpEnabled) R.string.app_menu_value_on else R.string.widget_icon_off,
                ),
                onClick = {
                    speedBumpEnabled = SpeedBump.toggle(context, appInfo.packageName)
                    if (!SpeedBump.hasSeenExplainer(context)) {
                        SpeedBump.markExplainerSeen(context)
                        onDismiss()
                        speedBumpExplainerJustEnabled = true
                    }
                    // Otherwise the menu stays open so the new state can be seen.
                },
                onLongClick = { onDismiss(); speedBumpExplainerJustEnabled = false },
            )
        } else {
            null
        }
    val lockPromptTitle = stringResource(
        if (appLockEnabled) R.string.app_lock_prompt_unlock else R.string.app_lock_prompt_lock,
        appInfo.appName,
    )
    val appLockAction =
        if (isDefaultLauncher && isOtherLaunchableApp) {
            ItemMenuRow(
                label = stringResource(
                    if (appLockEnabled) R.string.action_unlock_app else R.string.action_lock_app,
                ),
                icon = {
                    Icon(
                        imageVector = if (appLockEnabled) MaterialSymbols.LockOpen else MaterialSymbols.Lock,
                        contentDescription = null,
                        tint = if (appLockEnabled) AppColors.ItemMenuActiveIconTint else LocalContentColor.current,
                    )
                },
                onClick = {
                    val newLockState = !appLockEnabled
                    authenticate(lockPromptTitle) {
                        AppLock.setLocked(context, appInfo.packageName, newLockState)
                        appLockEnabled = newLockState
                    }
                },
                onLongHold =
                    if (appLockEnabled) {
                        {
                            authenticateWithDeviceCredential(lockPromptTitle) {
                                AppLock.setLocked(context, appInfo.packageName, false)
                                appLockEnabled = false
                            }
                        }
                    } else {
                        null
                    },
                longHoldDurationMillis = AppUnlockCredentialHoldMillis,
            )
        } else {
            null
        }

    val appearanceRows = buildList {
        if (!isDefaultLauncher) speedBumpAction?.let(::add)
        if (isLaunchableApp) {
            add(ItemMenuRow(
                label = stringResource(
                    if (isPinnedToNotifications) R.string.action_unpin_from_notifications
                    else R.string.action_pin_to_notifications,
                ),
                icon = {
                    if (isPinnedToNotifications) {
                        Icon(painter = painterResource(R.drawable.ic_unpin), contentDescription = null, tint = AppColors.ItemMenuActiveIconTint)
                    } else {
                        Icon(imageVector = MaterialSymbols.PinEnd, contentDescription = null)
                    }
                },
                onClick = { onDismiss(); PinnedNotifications.toggle(context, notificationAction) },
            ))
        }
    }

    val speedBumpAndLockButtons =
        if (isDefaultLauncher) {
            listOfNotNull(speedBumpAction, appLockAction)
        } else {
            emptyList()
        }

    val splitAndIconButtons = buildList {
        if (isLaunchableApp && appInfo.userHandleId == null) {
            add(ItemMenuRow(
                label = stringResource(R.string.action_open_in_split_screen),
                icon = { Icon(imageVector = MaterialSymbols.HorizontalSplit, contentDescription = null) },
                onClick = { onDismiss(); onOpenInSplitScreen() },
            ))
        }
        if (!isCurrentApp) {
            add(ItemMenuRow(
                label = stringResource(R.string.action_change_icon),
                icon = { Icon(imageVector = MaterialSymbols.Image, contentDescription = null, tint = if (hasIconOverride) AppColors.ItemMenuActiveIconTint else LocalContentColor.current) },
                onClick = { onDismiss(); showIconPicker.value = true },
            ))
        }
    }

    val launchRows = buildList {
        if (isLaunchableApp && !isDefaultLauncher) {
            add(ItemMenuRow(
                label = stringResource(R.string.action_add_to_home),
                icon = { Icon(imageVector = MaterialSymbols.Home, contentDescription = null) },
                onClick = { onDismiss(); onAddToHome() },
            ))
        }
        add(ItemMenuRow(
            label = stringResource(R.string.action_open_in_google_play),
            icon = { Icon(painter = painterResource(R.drawable.google_play), contentDescription = null, tint = Color.Unspecified) },
            onClick = { onDismiss(); openAppInGooglePlay(context, appInfo.packageName) },
        ))
    }

    val (swipeUpAction, swipeDownAction) = rememberAppSwipeActions(appInfo)
    var swipeInfoDirection by remember(appInfo.launchCountKey(), expanded) { mutableStateOf<AppSwipeDirection?>(null) }
    val swipeButtons = buildList {
        if (isLaunchableApp && showSwipeGestures) {
            listOf(
                Triple(AppSwipeDirection.UP, R.string.settings_gesture_swipe_up, swipeUpAction),
                Triple(AppSwipeDirection.DOWN, R.string.settings_gesture_swipe_down, swipeDownAction),
            ).forEach { (direction, labelRes, assignedAction) ->
                add(ItemMenuRow(
                    label = stringResource(labelRes),
                    icon = {
                        Icon(
                            imageVector =
                                if (direction == AppSwipeDirection.UP) MaterialSymbols.SwipeUp else MaterialSymbols.SwipeDown,
                            contentDescription = null,
                            tint = if (assignedAction != null) AppColors.ItemMenuActiveIconTint else LocalContentColor.current,
                        )
                    },
                    onClick = { onDismiss(); AppSwipeGestures.requestPicker(appInfo, direction) },
                    onLongClick = assignedAction?.let { { swipeInfoDirection = direction } },
                    anchoredContent = {
                        AppSwipeActionDropdown(
                            expanded = swipeInfoDirection == direction && assignedAction != null,
                            action = assignedAction,
                            iconPackPackage = iconPackPackage,
                            onDismiss = { swipeInfoDirection = null },
                            onClear = {
                                swipeInfoDirection = null
                                AppSwipeGestures.setAction(context, appInfo, direction, null)
                            },
                        )
                    },
                ))
            }
        }
    }

    val footerButtons = buildList {
        add(ItemMenuRow(
            label = stringResource(R.string.action_app_info),
            icon = { Icon(imageVector = MaterialSymbols.Info, contentDescription = null) },
            onClick = { onDismiss(); onAppInfoClick() },
        ))
        if (showUninstall) {
            add(ItemMenuRow(
                label = stringResource(R.string.action_uninstall_app),
                icon = { Icon(imageVector = MaterialSymbols.Delete, contentDescription = null) },
                onClick = { onDismiss(); onUninstallClick() },
                destructive = true,
            ))
        }
        openAppSettingDestination?.let { openDestination ->
            add(ItemMenuRow(
                label = stringResource(R.string.action_manage_all_apps),
                icon = { Icon(imageVector = MaterialSymbols.Settings, contentDescription = null) },
                onClick = { onDismiss(); openDestination(AppSettingsDestination.APP_MANAGEMENT) },
                iconOnly = true,
            ))
        }
    }

    val density = LocalDensity.current
    val shortcutIconSizePx = remember(density) {
        with(density) { ShortcutGridIconSize.roundToPx().coerceAtLeast(1) }
    }
    val iconResult = rememberAppIcon(
        packageName = appInfo.packageName,
        iconPackPackage = iconPackPackage,
        userHandleId = appInfo.userHandleId,
        forceCircularMask = appIconShape == AppIconShape.CIRCLE,
    )

    if (expanded) {
        val disableShortcutLabel = stringResource(R.string.action_disable_app_shortcut)
        val shortcutTiles = shortcuts.map { shortcut ->
            val displayName = shortcutDisplayName(shortcut)
            val iconBitmap = rememberShortcutIcon(shortcut, shortcutIconSizePx) ?: iconResult.bitmap
            ItemMenuTile(
                label = displayName,
                icon = {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = displayName,
                            modifier = Modifier.size(ShortcutGridIconSize),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = displayName.trim().take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                },
                onClick = { onShortcutClick(shortcut); onDismiss() },
                longPressOption = ItemMenuLongPressOption(
                    label = disableShortcutLabel,
                    icon = MaterialSymbols.Block,
                    // Keeps the app menu open; the disabled shortcut drops out of the grid.
                    onClick = { onDisableShortcut(shortcut) },
                ),
            )
        }
        ItemMenuPopup(
            onDismiss = onDismiss,
            leadingContent = {
                iconResult.bitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = appInfo.appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            },
            title = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        Text(
                            text = appInfo.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        versionName?.let { AppVersionPill(it) }
                    }
                    todayUsage
                        ?.takeIf { it.openedCount > 0 }
                        ?.let { usage ->
                            Text(
                                text = stringResource(
                                    R.string.app_menu_usage_today,
                                    formatUsageDuration(usage.foregroundTimeMillis),
                                    pluralStringResource(
                                        R.plurals.app_menu_opened_count,
                                        usage.openedCount,
                                        usage.openedCount,
                                    ),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                }
            },
            shortcutsTitle = stringResource(R.string.app_menu_section_shortcuts),
            shortcuts = shortcutTiles,
            actionsTitle = stringResource(R.string.app_menu_section_actions),
            actions = actions,
            buttonRows = listOf(speedBumpAndLockButtons, splitAndIconButtons, swipeButtons),
            rows = appearanceRows + launchRows,
            footer = footerButtons,
            caption = { AppPackageCaption(packageName = appInfo.packageName) },
        )
    }

    speedBumpExplainerJustEnabled?.let { justEnabled ->
        SpeedBumpExplainerDialog(
            justEnabledForAppName = appInfo.appName.takeIf { justEnabled },
            onDismiss = { speedBumpExplainerJustEnabled = null },
        )
    }

    if (showIconPicker.value) {
        AppIconOverrideDrawer(
            packageName = appInfo.packageName,
            appName = appInfo.appName,
            onDismiss = { showIconPicker.value = false },
        )
    }
}

/** The app's version in a small pill, shown beside the app name in the menu header. */
@Composable
private fun AppVersionPill(versionName: String) {
    Text(
        text = stringResource(R.string.app_menu_version, versionName),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = DesignTokens.SpacingSmall, vertical = 2.dp),
    )
}

/** The package name in small monospace, shown centered at the bottom of the menu. */
@Composable
private fun AppPackageCaption(packageName: String) {
    Text(
        text = packageName,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Small popup shown on long press of an assigned Swipe up / Swipe down button. */
@Composable
private fun AppSwipeActionDropdown(
    expanded: Boolean,
    action: CustomWidgetButtonAction?,
    iconPackPackage: String?,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    // Fills the anchoring button so the popup can match its width.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        DropdownMenu(
            expanded = expanded && action != null,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(maxWidth),
            offset = DpOffset(x = 0.dp, y = 8.dp),
            shape = RoundedCornerShape(24.dp),
            properties = PopupProperties(focusable = false),
            containerColor = if (LocalAppIsDarkTheme.current) Color.Black else Color.White,
        ) {
            if (action == null) return@DropdownMenu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_swipe_current_action),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CustomWidgetButtonIcon(
                        action = action,
                        iconSize = 24.dp,
                        iconPackPackage = iconPackPackage,
                    )
                    Text(
                        text = action.displayLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_remove)) },
                leadingIcon = { Icon(imageVector = MaterialSymbols.Close, contentDescription = null) },
                onClick = onClear,
            )
        }
    }
}

private fun openAppInGooglePlay(context: Context, packageName: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                .setPackage("com.android.vending")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun formatUsageDuration(durationMillis: Long): String {
    val totalMinutes = (durationMillis / 60_000L).toInt().coerceAtLeast(1)
    val halfHours = totalMinutes / 30
    val hours = halfHours / 2
    return when {
        totalMinutes >= 60 && halfHours % 2 == 1 ->
            stringResource(R.string.app_menu_usage_hours_decimal, "$hours.5")
        totalMinutes >= 60 -> pluralStringResource(R.plurals.app_menu_usage_hours, hours, hours)
        else -> pluralStringResource(R.plurals.app_menu_usage_minutes, totalMinutes, totalMinutes)
    }
}
