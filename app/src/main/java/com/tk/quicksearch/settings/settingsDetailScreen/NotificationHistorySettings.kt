package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.search.apps.appLock.AppLockGate
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlin.math.abs
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryLauncher
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.util.getAppGridColumns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsNavigationRow
import com.tk.quicksearch.settings.shared.SettingsCardItem
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.foundation.layout.PaddingValues
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryAccess
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryEntry
import com.tk.quicksearch.search.notificationHistory.NotificationHistoryStore
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lists the notifications captured while notification listener access was granted, filtered by the
 * screen's search bar.
 */
@Composable
fun NotificationHistorySettingsSection(
    searchQuery: String = "",
    showAppFilterDialog: Boolean = false,
    onDismissAppFilterDialog: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val lifecycleOwner = LocalLifecycleOwner.current
    val listState = rememberLazyListState()

    val accessGranted by NotificationHistoryAccess.granted.collectAsState()
    // Null until the first database read completes, so the empty state doesn't flash.
    val loadedEntries by
        remember(context) { NotificationHistoryStore.entries(context) }
            .collectAsState(initial = null)
    val entries = loadedEntries.orEmpty()
    val hiddenPackages by NotificationHistoryStore.hiddenPackages.collectAsState()

    // Listener access is granted from a system screen, so re-check it every time we come back.
    DisposableEffect(lifecycleOwner, context) {
        NotificationHistoryAccess.refresh(context)
        NotificationHistoryStore.ensureLoaded(context)
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    NotificationHistoryAccess.refresh(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Hidden apps have no entries left, but must stay in the filter dialog so they can be re-enabled.
    val packageNames =
        remember(entries, hiddenPackages) {
            entries.mapTo(HashSet()) { it.packageName } +
                hiddenPackages +
                NotificationHistoryStore.DEFAULT_HIDDEN_PACKAGES
        }
    val appLabels by produceState(initialValue = emptyMap<String, String>(), packageNames) {
        value = withContext(Dispatchers.IO) { loadAppLabels(context, packageNames) }
    }

    val normalizedQuery = remember(searchQuery, locale) { searchQuery.trim().lowercase(locale) }
    val filteredEntries =
        remember(entries, appLabels, normalizedQuery, locale) {
            if (normalizedQuery.isBlank()) {
                entries
            } else {
                entries.filter { entry ->
                    val appLabel = appLabels[entry.packageName] ?: entry.packageName
                    listOf(appLabel, entry.title, entry.text).any { field ->
                        field.lowercase(locale).contains(normalizedQuery)
                    }
                }
            }
        }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val use24Hour = DateFormat.is24HourFormat(context)

    if (showAppFilterDialog) {
        NotificationHistoryAppFilterDialog(
            packageNames = packageNames,
            appLabels = appLabels,
            hiddenPackages = hiddenPackages,
            hasEntries = entries.isNotEmpty(),
            onToggle = { packageName, visible ->
                NotificationHistoryStore.setPackageHidden(context, packageName, hidden = !visible)
            },
            onClearAll = {
                NotificationHistoryStore.clear(context)
                onDismissAppFilterDialog()
            },
            onDismiss = onDismissAppFilterDialog,
        )
    }

    Column(modifier = modifier) {
        if (accessGranted == false) {
            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                SettingsNavigationRow(
                    item =
                        SettingsCardItem(
                            title = stringResource(R.string.permission_required_title),
                            description = stringResource(R.string.notification_history_permission_desc),
                            icon = Icons.Rounded.NotificationsActive,
                            actionOnPress = { NotificationHistoryAccess.openSettings(context) },
                        ),
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        ),
                )
            }
            return@Column
        }
        // Access state is unknown until the first check completes; avoid flashing either state.
        if (accessGranted == null || loadedEntries == null) return@Column

        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            stringResource(
                                if (entries.isEmpty()) {
                                    R.string.notification_history_empty
                                } else {
                                    R.string.widget_custom_buttons_no_results
                                },
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().heightIn(max = screenHeight),
                ) {
                    itemsIndexed(
                        items = filteredEntries,
                        // Unique per row (matches the database's unique index), so removals animate.
                        key = { _, entry -> "${entry.key}|${entry.title}|${entry.text}" },
                    ) { index, entry ->
                        val appLabel = appLabels[entry.packageName] ?: entry.packageName
                        Column(modifier = Modifier.animateItem()) {
                            SwipeToDeleteContainer(
                                onDelete = { NotificationHistoryStore.remove(context, entry) },
                            ) {
                                NotificationHistoryRow(
                                    entry = entry,
                                    appLabel = appLabel,
                                    timeLabel = formatNotificationTime(entry.postTime, use24Hour, locale),
                                    onClick = {
                                        AppLockGate.runAfterUnlock(context, entry.packageName, appLabel) {
                                            if (!NotificationHistoryLauncher.open(context, entry)) {
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.common_error_unable_to_open, appLabel),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                            }
                                        }
                                    },
                                    onHideApp = {
                                        NotificationHistoryStore.setPackageHidden(
                                            context,
                                            entry.packageName,
                                            hidden = true,
                                        )
                                    },
                                )
                            }
                            if (index < filteredEntries.lastIndex) {
                                HorizontalDivider(color = AppColors.SettingsDivider)
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val COLLAPSED_TEXT_MAX_LINES = 5

@Composable
private fun NotificationHistoryRow(
    entry: NotificationHistoryEntry,
    appLabel: String,
    timeLabel: String,
    onClick: () -> Unit,
    onHideApp: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var expanded by rememberSaveable(entry.key, entry.postTime) { mutableStateOf(false) }
    // Stays true once the text overflowed, so the chevron remains to collapse it again.
    var isTruncatable by remember(entry.text) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "notificationChevronRotation",
    )
    val haptics = LocalHapticFeedback.current

    Box {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        },
                    )
                    .padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            verticalAlignment = Alignment.Top,
        ) {
            NotificationAppIcon(packageName = entry.packageName)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = appLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    if (isTruncatable) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription =
                                stringResource(if (expanded) R.string.desc_collapse else R.string.desc_expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .size(20.dp)
                                    .rotate(chevronRotation)
                                    .clip(CircleShape)
                                    .clickable { expanded = !expanded },
                        )
                    }
                }

                if (entry.title.isNotBlank()) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (entry.text.isNotBlank()) {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_TEXT_MAX_LINES,
                        modifier = Modifier.animateContentSize(),
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { layout ->
                            if (layout.hasVisualOverflow) isTruncatable = true
                        },
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = AppColors.DialogBackground,
        ) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.notification_history_hide_app, appLabel)) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.VisibilityOff, contentDescription = null) },
                onClick = {
                    showMenu = false
                    onHideApp()
                },
            )
        }
    }
}

private const val SWIPE_DELETE_FRACTION = 0.5f

/**
 * Swiping either way shows a delete indicator on that side. Releasing past half the row's width
 * deletes it; releasing earlier springs the row back.
 */
@Composable
private fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val state =
        rememberSwipeToDismissBoxState(
            positionalThreshold = { totalDistance -> totalDistance * SWIPE_DELETE_FRACTION },
        )
    val isPastDeleteThreshold = state.targetValue != SwipeToDismissBoxValue.Settled

    LaunchedEffect(isPastDeleteThreshold) {
        if (isPastDeleteThreshold) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }

    val removeLabel = stringResource(R.string.action_remove)
    SwipeToDismissBox(
        state = state,
        onDismiss = { onDelete() },
        modifier =
            Modifier.semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction(removeLabel) {
                            onDelete()
                            true
                        },
                    )
            },
        backgroundContent = {
            // Only the strip the row has uncovered is tinted; the row itself has no background.
            val revealedPx = abs(runCatching { state.requireOffset() }.getOrDefault(0f))
            if (revealedPx > 0f) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment =
                        if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                            Alignment.CenterStart
                        } else {
                            Alignment.CenterEnd
                        },
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .width(with(density) { revealedPx.toDp() })
                                .clipToBounds()
                                .background(MaterialTheme.colorScheme.error),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                        )
                    }
                }
            }
        },
    ) {
        content()
    }
}

@Composable
private fun NotificationAppIcon(packageName: String) {
    val iconResult = rememberAppIcon(packageName = packageName)
    val bitmap = iconResult.bitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(DesignTokens.LargeIconSize),
            contentScale = ContentScale.Fit,
        )
    } else {
        Icon(
            imageVector = Icons.Rounded.Apps,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.LargeIconSize),
        )
    }
}

/** Grid of every app present in history; unchecking an app hides it and stops recording it. */
@Composable
private fun NotificationHistoryAppFilterDialog(
    packageNames: Set<String>,
    appLabels: Map<String, String>,
    hiddenPackages: Set<String>,
    hasEntries: Boolean,
    onToggle: (packageName: String, visible: Boolean) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = Locale.getDefault()
    val sortedPackages =
        remember(packageNames, appLabels, locale) {
            packageNames.sortedBy { (appLabels[it] ?: it).lowercase(locale) }
        }
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    if (showClearAllConfirmation) {
        AppAlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            title = { Text(text = stringResource(R.string.notification_history_clear_all_title)) },
            text = {
                Text(
                    text = stringResource(R.string.notification_history_clear_all_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllConfirmation = false
                        onClearAll()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.settings_action_clear_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmation = false }) {
                    Text(text = stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    AppAlertDialog(
        modifier = Modifier.fillMaxWidth(0.94f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.notification_history_app_filter_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                if (hasEntries) {
                    Surface(
                        onClick = { showClearAllConfirmation = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.settings_action_clear_all),
                            modifier =
                                Modifier
                                    .padding(
                                        horizontal = DesignTokens.SpacingMedium,
                                        vertical = DesignTokens.SpacingXSmall,
                                    ).size(20.dp),
                        )
                    }
                }
            }
        },
        text = {
            if (sortedPackages.isEmpty()) {
                Text(
                    text = stringResource(R.string.notification_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(getAppGridColumns()),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    items(items = sortedPackages, key = { it }) { packageName ->
                        val visible = packageName !in hiddenPackages
                        NotificationHistoryAppFilterItem(
                            packageName = packageName,
                            label = appLabels[packageName] ?: packageName,
                            checked = visible,
                            onClick = { onToggle(packageName, !visible) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_done))
            }
        },
    )
}

@Composable
private fun NotificationHistoryAppFilterItem(
    packageName: String,
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val iconResult = rememberAppIcon(packageName = packageName)
    val contentAlpha = if (checked) 1f else 0.4f
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(DesignTokens.SpacingMedium))
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() })
                .padding(vertical = DesignTokens.SpacingXSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            val bitmap = iconResult.bitmap
            val iconModifier = Modifier.size(48.dp).align(Alignment.Center).alpha(contentAlpha)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = iconModifier,
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = iconModifier,
                )
            }
            if (checked) {
                Box(
                    modifier =
                        Modifier.align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun loadAppLabels(
    context: Context,
    packageNames: Set<String>,
): Map<String, String> {
    val packageManager = context.packageManager
    return packageNames.associateWith { packageName ->
        runCatching {
            packageManager
                .getApplicationInfo(packageName, 0)
                .loadLabel(packageManager)
                .toString()
        }.getOrElse { packageName }
    }
}

private fun formatNotificationTime(
    postTime: Long,
    use24Hour: Boolean,
    locale: Locale,
): String {
    val timePattern = if (use24Hour) "HH:mm" else "h:mm a"
    val isToday =
        android.text.format.DateUtils.isToday(postTime)
    val pattern = if (isToday) timePattern else "MMM d, $timePattern"
    val formatter = java.text.SimpleDateFormat(pattern, locale)
    return runCatching { formatter.format(Date(postTime)) }.getOrElse { "" }
}
