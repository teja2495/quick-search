package com.tk.quicksearch.settings.shared

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.ui.theme.DesignTokens

private data class ShortcutSource(
    val label: String,
    val launchIntent: Intent,
    val icon: androidx.compose.ui.graphics.ImageBitmap?,
)

private val ShortcutSourceIconSize = 44.dp

@Composable
fun AppShortcutSourcePickerDialog(
    onDismiss: () -> Unit,
    onSourceSelected: (Intent) -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val sources =
        remember {
            queryShortcutSources(packageManager).map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val className = resolveInfo.activityInfo.name
                val intent =
                    Intent(Intent.ACTION_CREATE_SHORTCUT).setClassName(packageName, className)
                val label =
                    resolveInfo.activityInfo.nonLocalizedLabel
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                        ?: resolveInfo.activityInfo.applicationInfo.nonLocalizedLabel
                            ?.toString()
                            ?.takeIf { it.isNotBlank() }
                        ?: formatPackageNameAsLabel(packageName)
                val icon =
                    runCatching { resolveInfo.loadIcon(packageManager) }
                        .getOrNull()
                        ?.toBitmap(width = 96, height = 96)
                        ?.asImageBitmap()
                ShortcutSource(label = label, launchIntent = intent, icon = icon)
            }
        }

    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.94f),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_app_shortcuts_add_dialog_title)) },
        text = {
            if (sources.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_app_shortcuts_create_not_supported),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val sourceRows = sources.chunked(4)
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
                ) {
                    sourceRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                        ) {
                            rowItems.forEach { source ->
                                ShortcutSourceGridItem(
                                    source = source,
                                    onClick = { onSourceSelected(source.launchIntent) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(4 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun ShortcutSourceGridItem(
    source: ShortcutSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clickable(onClick = onClick)
                .padding(vertical = DesignTokens.SpacingSmall, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        if (source.icon != null) {
            Image(
                bitmap = source.icon,
                contentDescription = source.label,
                modifier = Modifier.size(ShortcutSourceIconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(ShortcutSourceIconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = source.label.take(1).uppercase(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = source.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
    }
}

private fun queryShortcutSources(packageManager: PackageManager): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
    val results =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    return results
        .filter { it.activityInfo?.exported == true }
        .sortedBy { info ->
            val packageName = info.activityInfo.packageName
            (
                info.activityInfo.nonLocalizedLabel
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: info.activityInfo.applicationInfo.nonLocalizedLabel
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                ?: formatPackageNameAsLabel(packageName)
            ).lowercase()
        }
}

private fun formatPackageNameAsLabel(packageName: String): String =
    packageName
        .substringAfterLast(".")
        .replaceFirstChar { it.titlecase() }
