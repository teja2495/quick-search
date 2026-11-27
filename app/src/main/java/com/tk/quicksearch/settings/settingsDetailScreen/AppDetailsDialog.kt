package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.search.apps.AppSortMetadata
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.PackageInfoCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.models.AppInfo
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
internal fun AppDetailsDialog(
    app: AppInfo,
    iconPackPackage: String?,
    onOpenAppInfo: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val iconResult =
        rememberAppIcon(
            packageName = app.packageName,
            iconPackPackage = iconPackPackage,
            userHandleId = app.userHandleId,
        )
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

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
                ) {
                    Box(modifier = Modifier.size(DesignTokens.Spacing40), contentAlignment = Alignment.Center) {
                        if (iconResult.bitmap != null) {
                            Image(
                                bitmap = iconResult.bitmap,
                                contentDescription = null,
                                modifier = Modifier.size(DesignTokens.Spacing40).clip(AppShapes.medium),
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
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    text = metadata.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            val detailGroups = listOf(
                listOf(
                    R.string.settings_app_info_version to metadata.versionName,
                    R.string.settings_app_info_version_code to metadata.versionCode,
                ),
                listOf(
                    R.string.settings_app_info_minimum_sdk to metadata.minimumSdk,
                    R.string.settings_app_info_target_sdk to metadata.targetSdk,
                ),
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                detailGroups.forEach { group ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                    ) {
                        group.forEach { (label, value) ->
                            AppDetailLine(
                                label = stringResource(label),
                                value = value,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
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
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_close))
            }
        },
        dismissButton = {
            TonalButton(
                text = stringResource(R.string.action_app_info),
                onClick = {
                    onDismiss()
                    onOpenAppInfo(app)
                },
            )
        },
    )
}

@Composable
private fun AppDetailLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
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
