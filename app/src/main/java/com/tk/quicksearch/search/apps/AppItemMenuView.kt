package com.tk.quicksearch.search.apps

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.shared.ui.components.AppBottomPopup
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private data class AppMenuItem(
    val textResId: Int,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
)

private val ShortcutGridIconSize = 32.dp
private val ActionGridIconSize = DesignTokens.IconSize

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
    onAppInfoClick: () -> Unit,
    onHideApp: () -> Unit,
    onPinApp: () -> Unit,
    onUnpinApp: () -> Unit,
    onUninstallClick: () -> Unit,
    onNicknameClick: () -> Unit,
    onTriggerClick: () -> Unit,
    onAddToHome: () -> Unit,
) {
    val context = LocalContext.current
    val isCurrentApp = appInfo.packageName == context.packageName
    val isLaunchableApp = appInfo.hasLaunchIntent
    val menuItems = buildList {
        if (!isCurrentApp && isLaunchableApp) {
            add(AppMenuItem(
                textResId = if (isPinned) R.string.action_unpin_app else R.string.action_pin_app,
                icon = {
                    Icon(
                        painter = painterResource(if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin),
                        contentDescription = null,
                    )
                },
                onClick = { onDismiss(); if (isPinned) onUnpinApp() else onPinApp() },
            ))
        }
        if (!isCurrentApp) {
            add(AppMenuItem(
                textResId = if (hasNickname) R.string.action_edit_nickname else R.string.common_nickname,
                icon = { Icon(imageVector = Icons.Rounded.Edit, contentDescription = null) },
                onClick = { onDismiss(); onNicknameClick() },
            ))
            add(AppMenuItem(
                textResId = if (hasTrigger) R.string.action_edit_trigger else R.string.action_add_trigger,
                icon = { Icon(imageVector = Icons.Rounded.Bolt, contentDescription = null) },
                onClick = { onDismiss(); onTriggerClick() },
            ))
        }
        if (isLaunchableApp) {
            add(AppMenuItem(
                textResId = R.string.action_add_to_home,
                icon = { Icon(imageVector = Icons.Rounded.Home, contentDescription = null) },
                onClick = { onDismiss(); onAddToHome() },
            ))
        }
        add(AppMenuItem(
            textResId = R.string.action_app_info,
            icon = { Icon(imageVector = Icons.Rounded.Info, contentDescription = null) },
            onClick = { onDismiss(); onAppInfoClick() },
        ))
        add(AppMenuItem(
            textResId = R.string.action_exclude_generic,
            icon = { Icon(imageVector = Icons.Rounded.VisibilityOff, contentDescription = null) },
            onClick = { onDismiss(); onHideApp() },
        ))
        if (showUninstall) {
            add(AppMenuItem(
                textResId = R.string.action_uninstall_app,
                icon = { Icon(imageVector = Icons.Rounded.Delete, contentDescription = null) },
                onClick = { onDismiss(); onUninstallClick() },
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
        AppBottomPopup(
            onDismiss = onDismiss,
            leadingContent = {
                iconResult.bitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = appInfo.appName,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                }
            },
            title = {
                Column {
                    Text(
                        text = appInfo.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (shortcuts.isNotEmpty()) {
                        Text(
                            text = "${shortcuts.size} shortcuts • ${menuItems.size} actions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        ) {
            if (shortcuts.isNotEmpty()) {
                // Shortcuts section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    Text(
                        text = stringResource(R.string.app_menu_section_shortcuts),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    shortcuts.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                        ) {
                            row.forEach { shortcut ->
                                val displayName = shortcutDisplayName(shortcut)
                                val iconBitmap = rememberShortcutIcon(shortcut, shortcutIconSizePx)
                                AppMenuGridButton(
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
                                            Box(
                                                modifier = Modifier.size(ShortcutGridIconSize),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = displayName.trim().take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                )
                                            }
                                        }
                                    },
                                    onClick = { onShortcutClick(shortcut); onDismiss() },
                                    enableMarquee = true,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            // Actions section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                Text(
                    text = stringResource(R.string.app_menu_section_actions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val actionItems = if (showUninstall) menuItems.dropLast(1) else menuItems
                actionItems.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                    ) {
                        row.forEach { item ->
                            AppMenuGridButton(
                                label = stringResource(item.textResId),
                                icon = { item.icon() },
                                onClick = item.onClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // Uninstall
            if (showUninstall) {
                Button(
                    onClick = { onDismiss(); onUninstallClick() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(ActionGridIconSize),
                    )
                    Spacer(Modifier.width(DesignTokens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.action_uninstall_app),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppMenuGridButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enableMarquee: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val borderColor = AppColors.OnboardingBubbleBorder
    Surface(
        onClick = onClick,
        modifier = modifier.border(
            width = DesignTokens.BorderWidth,
            color = borderColor,
            shape = DesignTokens.ShapeSmall,
        ),
        shape = DesignTokens.ShapeSmall,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.DialogText,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = if (enableMarquee) Modifier.basicMarquee() else Modifier,
            )
        }
    }
}
