package com.tk.quicksearch.settings.settingsScreen

import android.content.ActivityNotFoundException
import com.tk.quicksearch.settings.shared.SettingsScreenCallbacks as SharedSettingsScreenCallbacks
import com.tk.quicksearch.settings.shared.SettingsScreenState as SharedSettingsScreenState
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Upload
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.ButtonDefaults
import com.tk.quicksearch.settings.shared.SettingsCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.searchScreen.dialogs.ReleaseNotesDrawer
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.featureFlags.FeatureFlag
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.permissions.LockScreenAccessibilityDisclosureDialog
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.AppLanguageManager
import com.tk.quicksearch.shared.util.AppLanguageOption
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
        Unit
    }

    val onOpenDeveloperGitHub: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/teja2495/")))
        }
        Unit
    }

    val feedbackItems =
        listOf(
            SettingsCardItem(
                title = stringResource(R.string.settings_feedback_send_title),
                description = stringResource(R.string.settings_feedback_send_desc),
                icon = Icons.Rounded.Email,
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
                icon = Icons.Rounded.RocketLaunch,
                actionOnPress = onOpenReleaseNotes,
            ),
            SettingsCardItem(
                title = stringResource(R.string.settings_all_quick_search_features),
                description = stringResource(R.string.settings_all_quick_search_features_desc),
                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                actionOnPress = onOpenFeatures,
            ),
        )

    SettingsCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            feedbackItems.forEachIndexed { index, item ->
                SettingsNavigationRow(
                    item = item,
                    contentPadding =
                        PaddingValues(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
                )

                if (index < feedbackItems.lastIndex) {
                    HorizontalDivider(
                        color = AppColors.SettingsDivider,
                    )
                }
            }

            HorizontalDivider(
                color = AppColors.SettingsDivider,
            )

            MoreAppsFromDeveloperRow(
                onGooglePlayClick = onOpenDeveloperGooglePlay,
                onGitHubClick = onOpenDeveloperGitHub,
            )

            HorizontalDivider(
                color = AppColors.SettingsDivider,
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenOssLicenses)
                        .padding(
                            horizontal = DesignTokens.SpacingXXLarge,
                            vertical = DesignTokens.SpacingLarge,
                        ),
            ) {
                Text(
                    text = stringResource(R.string.settings_open_source_licenses_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MoreAppsFromDeveloperRow(
    onGooglePlayClick: () -> Unit,
    onGitHubClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.SpacingXXLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.settings_more_apps_from_developer_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onGooglePlayClick) {
            Icon(
                painter = painterResource(R.drawable.google_play),
                contentDescription = "Google Play",
                tint = androidx.compose.ui.graphics.Color.Unspecified,
            )
        }

        IconButton(onClick = onGitHubClick) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = "GitHub",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun AppLanguagePickerDialog(
    selectedLanguageTag: String?,
    languageOptions: List<AppLanguageOption>,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f),
        title = {
            Text(text = stringResource(R.string.settings_app_language_picker_title))
        },
        text = {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(languageOptions) { option ->
                    val isSelected = option.languageTag == selectedLanguageTag
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    onLanguageSelected(option.languageTag)
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onLanguageSelected(option.languageTag)
                            },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            option.supportingLabel?.let { supportingLabel ->
                                Text(
                                    text = supportingLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
            }
        },
    )
}

@Composable
internal fun BackupRestoreRow(
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.SpacingXXLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Text(
            text = stringResource(R.string.settings_backup_restore_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_backup_restore_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Text(text = stringResource(R.string.settings_backup_import_button))
            }
            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                Text(text = stringResource(R.string.settings_backup_export_button))
            }
        }
    }
}

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
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.desc_navigate_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.width(DesignTokens.HeaderIconSpacing))
        Text(
            text = stringResource(R.string.settings_gesture_settings),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Displays the app version and developer info in a card at the bottom of the settings screen.
 */
@Composable
fun SettingsVersionDisplay(
    modifier: Modifier = Modifier,
    onFeatureFlagsChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionName = getAppVersionName() ?: "1.2.2"
    val developerName = stringResource(R.string.settings_feedback_developer_name)
    val developerDesc = stringResource(R.string.settings_feedback_developer_desc, developerName)

    val annotatedDeveloperDesc =
        buildAnnotatedString {
            val parts = developerDesc.split(developerName)
            if (parts.size > 1) {
                append(parts[0])
                withStyle(
                    style =
                        SpanStyle(
                            color = AppColors.LinkColor,
                            fontWeight = FontWeight.Medium,
                        ),
                ) {
                    append(developerName)
                }
                append(parts[1])
            } else {
                append(developerDesc)
            }
        }

    var versionTapCount by remember { mutableIntStateOf(0) }
    val versionTapInteractionSource = remember { MutableInteractionSource() }
    val hasNewFeaturesAvailable = FeatureFlag.entries.isNotEmpty()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.SpacingXXLarge,
                    vertical = DesignTokens.SpacingXLarge,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.settings_feedback_developer_title, stringResource(R.string.app_name), versionName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
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
                ),
        )

        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.height(DesignTokens.SpacingXSmall))

        Text(
            text = annotatedDeveloperDesc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.clickable {
                    val url = "https://teja2495.github.io/teja-karlapudi-links/"
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                    }
                },
        )
    }
}
