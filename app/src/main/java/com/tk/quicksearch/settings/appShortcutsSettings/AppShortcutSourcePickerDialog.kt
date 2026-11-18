package com.tk.quicksearch.settings.appShortcutsSettings

import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private val ShortcutSourceIconSize = 44.dp

@Composable
fun AppShortcutSourcePickerDialog(
    sources: List<AppShortcutSource>,
    onDismiss: () -> Unit,
    onSourceSelected: (AppShortcutSource) -> Unit,
) {
    AppPickerDrawer(
        title = stringResource(R.string.settings_app_shortcuts_add_dialog_title),
        onDismiss = onDismiss,
    ) {
        if (sources.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_app_shortcuts_create_not_supported),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                items(sources, key = { "${it.packageName}/${it.className}:${it.sourceType}" }) { source ->
                    ShortcutSourceGridItem(
                        source = source,
                        onClick = { onSourceSelected(source) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutSourceGridItem(
    source: AppShortcutSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(AppShapes.medium)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = DesignTokens.SpacingSmall, horizontal = DesignTokens.SpacingXSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        val icon = rememberShortcutSourceIcon(source)
        val iconBitmap = icon?.bitmap
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size(ShortcutSourceIconSize),
                contentScale = ContentScale.Fit,
            )
        } else if (icon == null) {
            Box(modifier = Modifier.size(ShortcutSourceIconSize))
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
