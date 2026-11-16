package com.tk.quicksearch.settings.settingsScreen

import android.content.ActivityNotFoundException
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenCallbacks as SharedSettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.settingsRoute.SettingsScreenState as SharedSettingsScreenState
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import com.tk.quicksearch.settings.preloadFeaturesMarkdown
import com.tk.quicksearch.settings.shared.SettingsCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.tk.quicksearch.BuildConfig
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.dialogs.ReleaseNotesDrawer
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.featureFlags.FeatureFlag
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.shared.util.FeedbackUtils
import com.tk.quicksearch.shared.util.isDefaultHomeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    SettingsSegmentGroup(modifier = modifier) {
        feedbackItems.forEachIndexed { index, item ->
            SettingsSegmentNavigationRow(item = item, index = index, count = count)
        }

        SettingsSegment(index = count - 2, count = count) {
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
                    SettingsIconBadge(icon = MaterialSymbols.Apps)
                    Text(
                        text = stringResource(R.string.settings_more_apps_from_developer_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
                ) {
                    BackupButton(
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
                        shape = BackupButtonStartShape,
                        modifier = Modifier.weight(1f),
                    )
                    BackupButton(
                        text = stringResource(R.string.settings_github_label),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        onClick = onOpenDeveloperGitHub,
                        shape = BackupButtonEndShape,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SettingsSegment(index = count - 1, count = count, onClick = onOpenOssLicenses) {
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
    SettingsSegment(index = index, count = count) {
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
                SettingsIconBadge(icon = MaterialSymbols.History)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
            ) {
                BackupButton(
                    text = stringResource(R.string.settings_backup_import_button),
                    icon = { BackupButtonIcon(MaterialSymbols.Download) },
                    onClick = onImportClick,
                    shape = BackupButtonStartShape,
                    modifier = Modifier.weight(1f),
                )
                BackupButton(
                    text = stringResource(R.string.settings_backup_export_button),
                    icon = { BackupButtonIcon(MaterialSymbols.Upload) },
                    onClick = onExportClick,
                    shape = BackupButtonEndShape,
                    modifier = Modifier.weight(1f),
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

@Composable
private fun BackupButton(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.height(BackupButtonHeight),
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = AppColors.Accent.copy(alpha = BackupButtonTintAlpha),
                contentColor = AppColors.Accent,
            ),
        contentPadding = PaddingValues(horizontal = DesignTokens.SpacingLarge),
    ) {
        icon()
        Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
        Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

private val BackupButtonHeight = 48.dp
private const val BackupButtonTintAlpha = 0.14f
private val BackupButtonStartShape =
    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)
private val BackupButtonEndShape =
    RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)

/**
 * Header component for the settings screen.
 */
@Composable
fun SettingsHeader(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.ContentHorizontalPadding,
                    vertical = DesignTokens.HeaderVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = MaterialSymbols.ArrowBack,
                contentDescription = stringResource(R.string.desc_navigate_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.width(DesignTokens.HeaderIconSpacing))
        Text(
            text = stringResource(R.string.settings_gesture_settings),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * App identity footer at the bottom of the settings screen: icon, name, version, and developer link.
 */
@Composable
fun SettingsVersionDisplay(
    modifier: Modifier = Modifier,
    onFeatureFlagsChanged: () -> Unit = {},
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
                context.packageManager
                    .getApplicationIcon(context.packageName)
                    .toBitmap(width = 192, height = 192)
                    .asImageBitmap()
            }.getOrNull()
        }

    var versionTapCount by remember { mutableIntStateOf(0) }
    val versionTapInteractionSource = remember { MutableInteractionSource() }
    val hasNewFeaturesAvailable = FeatureFlag.entries.isNotEmpty()
    val identityModifier =
        Modifier.combinedClickable(
            interactionSource = versionTapInteractionSource,
            indication = null,
            onClick = {
                versionTapCount += 1
                if (versionTapCount < 5) {
                    return@combinedClickable
                }
                versionTapCount = 0

                if (FeatureFlags.isAnyEnabled()) {
                    FeatureFlags.setAll(context, enabled = false)
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.settings_beta_features_disabled),
                            Toast.LENGTH_SHORT,
                        ).show()
                    onFeatureFlagsChanged()
                    return@combinedClickable
                }

                if (!hasNewFeaturesAvailable) {
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.settings_beta_features_unavailable),
                            Toast.LENGTH_SHORT,
                        ).show()
                    return@combinedClickable
                }

                FeatureFlags.setAll(context, enabled = true)
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.settings_beta_features_enabled),
                        Toast.LENGTH_SHORT,
                    ).show()
                onFeatureFlagsChanged()
            },
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
    val iconShape = RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
    val nameShape = RoundedCornerShape(topStart = inner, topEnd = outer, bottomStart = inner, bottomEnd = inner)
    val creditShape = RoundedCornerShape(topStart = inner, topEnd = inner, bottomStart = inner, bottomEnd = outer)
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        // Icon and name tiles keep the hidden tap / long-press gestures.
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
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(VersionFooterIconSize),
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(DesignTokens.SpacingXSmall))
                Text(
                    text = versionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
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
                        .clickable(onClick = onDeveloperClick)
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
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                ) {
                    Icon(
                        imageVector = MaterialSymbols.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

private val VersionFooterIconSize = 56.dp
