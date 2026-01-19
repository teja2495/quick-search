package com.tk.quicksearch.settings.settingsScreen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.util.FeedbackUtils
import com.tk.quicksearch.util.hapticToggle
import kotlinx.coroutines.launch
import com.tk.quicksearch.settings.settingsDetailScreens.SettingsDetailType


@Composable
fun SettingsScreen(
        modifier: Modifier = Modifier,
        state: SettingsScreenState,
        callbacks: SettingsScreenCallbacks,
        hasUsagePermission: Boolean,
        hasContactPermission: Boolean,
        hasFilePermission: Boolean,
        hasCallPermission: Boolean,
        shouldShowBanner: Boolean,
        onRequestUsagePermission: () -> Unit,
        onRequestContactPermission: () -> Unit,
        onRequestFilePermission: () -> Unit,
        onRequestCallPermission: () -> Unit,
        onDismissBanner: () -> Unit,
        onNavigateToDetail: (SettingsDetailType) -> Unit,
        scrollState: androidx.compose.foundation.ScrollState =
                androidx.compose.foundation.rememberScrollState()
) {
    BackHandler(onBack = callbacks.onBack)
    val coroutineScope = rememberCoroutineScope()

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .safeDrawingPadding()
    ) {
        SettingsHeader(onBack = callbacks.onBack)

        // Scrollable Content
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(horizontal = SettingsSpacing.contentHorizontalPadding)
        ) {
            // Usage Permission Banner (at the top)
            // Show banner only if usage access permission is missing and user hasn't dismissed it
            // twice
            if (!hasUsagePermission && shouldShowBanner) {
                UsagePermissionBanner(
                        onRequestPermission = onRequestUsagePermission,
                        onDismiss = onDismissBanner,
                        onCardClick = {
                            onNavigateToDetail(SettingsDetailType.PERMISSIONS)
                        },
                        modifier = Modifier.padding(bottom = SettingsSpacing.sectionTopPadding)
                )
            }

            // Search Results and Search Engines Card
            val navigationItems = listOf(
                    SettingsCardItem(
                            title = stringResource(R.string.settings_appearance_title),
                            description = stringResource(R.string.settings_appearance_desc),
                            icon = Icons.Rounded.Palette,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.APPEARANCE)
                            }
                    ),
                    SettingsCardItem(
                            title = stringResource(R.string.settings_search_results_title),
                            description = stringResource(R.string.settings_search_results_desc),
                            icon = Icons.Rounded.Search,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.SEARCH_RESULTS)
                            }
                    ),
                    SettingsCardItem(
                            title = stringResource(R.string.settings_search_engines_title),
                            description = stringResource(R.string.settings_search_engines_desc),
                            icon = Icons.AutoMirrored.Rounded.ManageSearch,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.SEARCH_ENGINES)
                            }
                    ),
                    SettingsCardItem(
                            title = stringResource(R.string.settings_calls_texts_title),
                            description = stringResource(R.string.settings_calls_texts_desc),
                            icon = Icons.Rounded.Phone,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.CALLS_TEXTS)
                            }
                    ),
                    SettingsCardItem(
                            title = stringResource(R.string.settings_file_types_title),
                            description = stringResource(R.string.settings_files_desc),
                            icon = Icons.Rounded.Folder,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.FILES)
                            }
                    ),
                    SettingsCardItem(
                            title = stringResource(R.string.settings_launch_options_title),
                            description = stringResource(R.string.settings_launch_options_desc),
                            icon = Icons.Rounded.RocketLaunch,
                            actionOnPress = {
                                onNavigateToDetail(SettingsDetailType.LAUNCH_OPTIONS)
                            }
                    )
            )

            ElevatedCard(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SettingsSpacing.sectionTopPadding),
                    shape = MaterialTheme.shapes.extraLarge
            ) {
                Column {
                    navigationItems.forEachIndexed { index, item ->
                        SettingsCardItemRow(
                                item = item,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                        )

                        if (index < navigationItems.lastIndex) {
                            HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }


            // More Options Section
            SettingsMoreOptions(
                onNavigateToPermissions = {
                    onNavigateToDetail(SettingsDetailType.PERMISSIONS)
                }
            )

            // App Version
            SettingsVersionDisplay(modifier = Modifier.padding(top = 40.dp, bottom = 60.dp))
        }
    }
}

/**
 * Single navigation card that matches the Search Engines section style.
 */
@Composable
fun NavigationSectionCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        SettingsCardItemRow(
            item = SettingsCardItem(
                title = title,
                description = description,
                actionOnPress = onClick
            ),
            contentPadding = contentPadding
        )
    }
}






/**
 * Generic toggle card component.
 */
@Composable
private fun ToggleCard(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val view = LocalView.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(!enabled) }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { newValue ->
                    hapticToggle(view)()
                    onToggle(newValue)
                }
            )
        }
    }
}

/**
 * More options section with navigation options for permissions, rating, feedback, GitHub, and features.
 */
@Composable
fun SettingsMoreOptions(
    modifier: Modifier = Modifier,
    onNavigateToPermissions: () -> Unit = {}
) {
    val context = LocalContext.current

    val onSendFeedback = {
        FeedbackUtils.launchFeedbackEmail(context, null)
    }

    val onRateApp = {
        val packageName = context.packageName
        try {
            // Try to open Google Play Store app
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to web browser if Play Store app is not available
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
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
        val url = "https://github.com/teja2495/quick-search/blob/main/FEATURES.md"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle case where browser is not available
        }
    }

    val feedbackItems = listOf(
        SettingsCardItem(
            title = stringResource(R.string.settings_permissions_title),
            description = stringResource(R.string.settings_permissions_desc),
            icon = Icons.Rounded.AdminPanelSettings,
            actionOnPress = onNavigateToPermissions
        ),
        SettingsCardItem(
            title = stringResource(R.string.settings_feedback_rate_title),
            description = stringResource(R.string.settings_feedback_rate_desc),
            iconResId = R.drawable.google_play,
            actionOnPress = onRateApp
        ),
        SettingsCardItem(
            title = stringResource(R.string.settings_feedback_send_title),
            description = stringResource(R.string.settings_feedback_send_desc),
            icon = Icons.Rounded.Email,
            actionOnPress = onSendFeedback
        ),
        SettingsCardItem(
            title = stringResource(R.string.settings_feedback_github_title),
            description = stringResource(R.string.settings_feedback_github_desc),
            iconResId = R.drawable.ic_github,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            actionOnPress = onOpenGitHub
        ),
        SettingsCardItem(
            title = stringResource(R.string.settings_all_quick_search_features),
            description = stringResource(R.string.settings_all_quick_search_features_desc),
            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
            actionOnPress = onOpenFeatures
        )
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            feedbackItems.forEachIndexed { index, item ->
                SettingsCardItemRow(
                    item = item,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                )

                if (index < feedbackItems.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
fun UsagePermissionBanner(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content area - clickable for scrolling to permissions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onCardClick)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Banner only shows for usage access permission
                Text(
                    text = stringResource(R.string.settings_usage_permission_banner_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Close button - separate from content area to prevent click conflicts
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.desc_close),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

