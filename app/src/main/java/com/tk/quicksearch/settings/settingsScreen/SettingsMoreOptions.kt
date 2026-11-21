package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.ui.semantics.Role
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import com.tk.quicksearch.shared.ui.components.AppTopAppBar
import com.tk.quicksearch.settings.aboutSettings.preloadFeaturesMarkdown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.BuildConfig
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.FeedbackUtils
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.ConnectedButtonRowScope
import com.tk.quicksearch.shared.ui.components.ConnectedButtonRow

/**
 * More options section with rating, feedback, GitHub, and features.
 */
@Composable
fun SettingsMoreOptions(
    modifier: Modifier = Modifier,
    onOpenReleaseNotes: () -> Unit = {},
    onOpenFeaturesList: () -> Unit = {},
    onOpenOssLicenses: () -> Unit = {},
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) { preloadFeaturesMarkdown(context) }

    val onSendFeedback = {
        FeedbackUtils.launchFeedbackEmail(context, null)
    }

    val onRateApp = {
        val packageName = context.packageName
        try {
            // Try to open Google Play Store app
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("market://details?id=$packageName")
                    setPackage("com.android.vending")
                }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to web browser if Play Store app is not available
            try {
                val intent =
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Handle case where browser is not available
            }
        }
    }

    val onOpenGitHub = {
        val url = "https://github.com/teja2495/quick-search"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where browser is not available
        }
    }

    val onOpenFeatures = {
        onOpenFeaturesList()
    }

    val onOpenDeveloperGooglePlay: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/developer?id=Teja+Karlapudi"),
                ),
            )
        }
    }

    val onOpenDeveloperGitHub: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/teja2495/")))
        }
    }

    val feedbackItems =
        listOf(
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_send_title),
                description = stringResource(R.string.settings_feedback_send_desc),
                icon = MaterialSymbols.Email,
                actionOnPress = onSendFeedback,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_rate_title),
                description = stringResource(R.string.settings_feedback_rate_desc),
                iconResId = R.drawable.google_play,
                actionOnPress = onRateApp,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_github_title),
                description = stringResource(R.string.settings_feedback_github_desc),
                iconResId = R.drawable.ic_github,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                actionOnPress = onOpenGitHub,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_release_notes_title),
                description = stringResource(R.string.settings_release_notes_desc),
                icon = MaterialSymbols.RocketLaunch,
                actionOnPress = onOpenReleaseNotes,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_all_quick_search_features),
                description = stringResource(R.string.settings_all_quick_search_features_desc),
                icon = MaterialSymbols.FormatListBulleted,
                actionOnPress = onOpenFeatures,
            ),
        )

    // Feedback rows, then More apps, then the licenses link close the group.
    val count = feedbackItems.size + 2
    SegmentedGroup(modifier = modifier) {
        feedbackItems.forEachIndexed { index, item ->
            SettingsSegmentNavigationRow(item = item, index = index, count = count)
        }

        SegmentedTile(index = count - 2, count = count) {
            // Title row, then a connected Google Play / GitHub pair in the Backup button style.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                ) {
                    TonalIconBadge(icon = MaterialSymbols.Apps)
                    Text(
                        text = stringResource(R.string.settings_more_apps_from_developer_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                ConnectedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                    height = BackupButtonHeight,
                    pressMorph = false,
                ) {
                    backupButton(
                        text = stringResource(R.string.search_engine_google_play),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.google_play),
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        onClick = onOpenDeveloperGooglePlay,
                    )
                    backupButton(
                        text = stringResource(R.string.settings_github_label),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        onClick = onOpenDeveloperGitHub,
                    )
                }
            }
        }

        SegmentedTile(index = count - 1, count = count, onClick = onOpenOssLicenses) {
            Text(
                text = stringResource(R.string.settings_open_source_licenses_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = DesignTokens.SpacingLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
            )
        }
    }
}

/** Backup tile: title and description over a connected Import / Export button pair. */
@Composable
internal fun BackupRestoreRow(
    index: Int,
    count: Int,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    SegmentedTile(index = index, count = count) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.SpacingLarge,
                        vertical = DesignTokens.SpacingLarge,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                TonalIconBadge(icon = MaterialSymbols.History)
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.TextColumnSpacing)) {
                    Text(
                        text = stringResource(R.string.settings_backup_restore_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.settings_backup_restore_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ConnectedButtonRow(
                modifier = Modifier.fillMaxWidth(),
                height = BackupButtonHeight,
                pressMorph = false,
            ) {
                backupButton(
                    text = stringResource(R.string.settings_backup_import_button),
                    icon = { BackupButtonIcon(MaterialSymbols.Download) },
                    onClick = onImportClick,
                )
                backupButton(
                    text = stringResource(R.string.settings_backup_export_button),
                    icon = { BackupButtonIcon(MaterialSymbols.Upload) },
                    onClick = onExportClick,
                )
            }
        }
    }
}

@Composable
private fun BackupButtonIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
    )
}

/** A button of a backup/links row: [icon] then a single-line [text]. */
private fun ConnectedButtonRowScope.backupButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    button(onClick = onClick) {
        icon()
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
        Text(text = text, maxLines = 1)
    }
}

private val BackupButtonHeight = 48.dp

/**
 * Header component for the settings screen.
 */
@Composable
fun SettingsHeader(onBack: () -> Unit) {
    AppTopAppBar(
        title = stringResource(R.string.settings_gesture_settings),
        onBack = onBack,
    )
}

/**
 * App identity footer at the bottom of the settings screen: icon, name, version, and developer link.
 */
@Composable
fun SettingsVersionDisplay(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = getAppVersionName() ?: BuildConfig.VERSION_NAME
    val appName = stringResource(R.string.app_name)
    val versionLabel = stringResource(R.string.app_menu_version, versionName)
    val developerName = stringResource(R.string.settings_feedback_developer_name)
    val madeByLabel = stringResource(R.string.settings_feedback_developer_made_by)
    val appIcon =
        remember(context) {
            runCatching {
                circularAppIconBitmap(context.packageManager.getApplicationIcon(context.packageName))
                    .asImageBitmap()
            }.getOrNull()
        }

    val versionTapInteractionSource = remember { MutableInteractionSource() }
    val identityModifier =
        Modifier.combinedClickable(
            interactionSource = versionTapInteractionSource,
            indication = null,
            onClick = {},
            onLongClick = {
                FeedbackUtils.launchFeedbackEmailWithCrashLog(context)
            },
        )
    val onDeveloperClick = {
        val url = "https://teja2495.github.io/teja-karlapudi-links/"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
        }
    }
    // Bento: the icon tile spans both rows; name/version and the credit stack to its right.
    val outer = DesignTokens.SegmentedListOuterCorner
    val inner = DesignTokens.SegmentedListInnerCorner
    val gap = DesignTokens.SegmentedListGap
    val tileColor = AppColors.getSettingsCardContainerColor()
    val iconShape = AppShapes.connected(start = outer, end = inner)
    val nameShape = AppShapes.corners(topStart = inner, topEnd = outer, bottomEnd = inner, bottomStart = inner)
    val creditShape = AppShapes.corners(topStart = inner, topEnd = inner, bottomEnd = outer, bottomStart = inner)
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        // The icon and name tiles both allow long-press feedback.
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(iconShape)
                    .background(tileColor, iconShape)
                    .then(identityModifier),
        ) {
            VersionFooterCookie()
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(VersionFooterIconSize).clip(AppShapes.full),
                )
            }
        }
        // Icon tile takes a third of the width so all three tiles have about the same area.
        Column(
            modifier = Modifier.weight(2f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            // App name leads; the version sits beneath it as a tonal pill.
            Column(
                verticalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(nameShape)
                        .background(tileColor, nameShape)
                        .then(identityModifier)
                        .padding(horizontal = DesignTokens.SpacingXLarge, vertical = DesignTokens.SpacingLarge),
            ) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXSmall))
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier =
                        Modifier
                            .clip(AppShapes.full)
                            .background(MaterialTheme.colorScheme.secondaryContainer, AppShapes.full)
                            .padding(horizontal = DesignTokens.SpacingSmall + 2.dp, vertical = 2.dp),
                )
            }
            // Developer credit, with a round link button so the tile reads as tappable.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(creditShape)
                        .background(tileColor, creditShape)
                        .clickable(role = Role.Button, onClick = onDeveloperClick)
                        .padding(start = DesignTokens.SpacingXLarge, end = DesignTokens.SpacingLarge),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = madeByLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = developerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(DesignTokens.Spacing40)
                            .background(MaterialTheme.colorScheme.secondaryContainer, AppShapes.cookie4),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(DesignTokens.ButtonIconSizeSmall),
                    )
                }
            }
        }
    }
}

/**
 * Cookie behind the app icon. It turns slowly while at least half of it is on screen and pauses
 * where it is when scrolled away, so it costs no frames while hidden.
 */
@Composable
private fun VersionFooterCookie() {
    var isVisible by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isVisible) {
        if (!isVisible) return@LaunchedEffect
        while (true) {
            // Continue from the paused angle so resuming never jumps.
            val start = rotation.value % 360f
            rotation.snapTo(start)
            rotation.animateTo(
                targetValue = start + 360f,
                animationSpec = tween(durationMillis = VersionFooterCookieRotationMillis, easing = LinearEasing),
            )
        }
    }
    Box(
        modifier =
            Modifier
                .size(VersionFooterCookieSize)
                .onVisibilityChanged(minFractionVisible = 0.5f) { visible -> isVisible = visible }
                .graphicsLayer { rotationZ = rotation.value }
                .background(MaterialTheme.colorScheme.secondaryContainer, AppShapes.cookie9),
    )
}

private val VersionFooterIconSize = 44.dp
private val VersionFooterCookieSize = 88.dp

// Looping period, not a transition duration, so it stays local (see DESIGN_SYSTEM.md motion).
private const val VersionFooterCookieRotationMillis = 16_000
