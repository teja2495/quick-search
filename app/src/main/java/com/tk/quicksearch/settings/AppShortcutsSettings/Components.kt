package com.tk.quicksearch.settings.AppShortcutsSettings

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import java.util.Locale

@Composable
fun ShortcutSourceRow(
    source: AppShortcutSource,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        if (source.icon != null) {
            Image(
                bitmap = source.icon,
                contentDescription = source.label,
                modifier = Modifier.size(DesignTokens.IconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(DesignTokens.IconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = source.label.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = source.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.settings_app_shortcuts_add_button),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun SearchTargetShortcutSourceRow(
    source: SearchTargetShortcutSource,
    onClick: () -> Unit,
) {
    val actionLabelResId =
        when (source.kind) {
            SearchTargetShortcutKind.QUERY -> R.string.settings_app_shortcuts_add_query_shortcut
            SearchTargetShortcutKind.URL -> R.string.settings_app_shortcuts_add_url_shortcut
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        SearchTargetIcon(
            target = source.target,
            iconSize = DesignTokens.IconSize,
            style = IconRenderStyle.ADVANCED,
        )

        Text(
            text = stringResource(actionLabelResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(actionLabelResId),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun AddSearchTargetShortcutDialog(
    targetLabel: String,
    shortcutKind: SearchTargetShortcutKind,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var shortcutName by remember(targetLabel, shortcutKind) {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var shortcutValue by remember(targetLabel, shortcutKind) {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }

    val trimmedName = shortcutName.text.trim()
    val trimmedValue = shortcutValue.text.trim()
    val canSave = trimmedName.isNotBlank() && trimmedValue.isNotBlank()
    val titleResId =
        when (shortcutKind) {
            SearchTargetShortcutKind.QUERY -> R.string.settings_app_shortcuts_add_query_dialog_title
            SearchTargetShortcutKind.URL -> R.string.settings_app_shortcuts_add_url_dialog_title
        }
    val valueLabelResId =
        when (shortcutKind) {
            SearchTargetShortcutKind.QUERY -> R.string.settings_app_shortcuts_query_label
            SearchTargetShortcutKind.URL -> R.string.settings_app_shortcuts_url_label
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleResId, targetLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = shortcutName,
                    onValueChange = { shortcutName = it },
                    singleLine = true,
                    maxLines = 1,
                    label = { Text(stringResource(R.string.settings_app_shortcuts_shortcut_name_label)) },
                )
                OutlinedTextField(
                    value = shortcutValue,
                    onValueChange = { shortcutValue = it },
                    singleLine = false,
                    maxLines = 3,
                    label = { Text(stringResource(valueLabelResId)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmedName, trimmedValue) },
                enabled = canSave,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
fun EditCustomShortcutDialog(
    shortcut: StaticShortcut,
    iconPackPackage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var shortcutName by remember(shortcut) {
        mutableStateOf(
            TextFieldValue(
                text = shortcutDisplayName(shortcut),
                selection = TextRange(shortcutDisplayName(shortcut).length),
            ),
        )
    }
    var iconBase64 by remember(shortcut) { mutableStateOf(shortcut.iconBase64) }
    val trimmedName = shortcutName.text.trim()
    val canSave = trimmedName.isNotBlank()

    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val encoded = loadCustomIconAsBase64(context, uri) ?: return@rememberLauncherForActivityResult
            iconBase64 = encoded
        }

    val iconBitmap =
        remember(iconBase64) {
            val encoded = iconBase64 ?: return@remember null
            val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                ?: return@remember null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    val appIconResult = rememberAppIcon(packageName = shortcut.packageName, iconPackPackage = iconPackPackage)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.settings_app_shortcuts_edit_dialog_title))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.settings_app_shortcuts_delete_action),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(50.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { pickIconLauncher.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            iconBitmap != null -> {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = trimmedName,
                                    modifier =
                                        Modifier
                                            .size(34.dp)
                                            .offset(y = 2.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            appIconResult.bitmap != null -> {
                                Image(
                                    bitmap = appIconResult.bitmap,
                                    contentDescription = trimmedName,
                                    modifier =
                                        Modifier
                                            .size(34.dp)
                                            .offset(y = 2.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = trimmedName,
                                    modifier =
                                        Modifier
                                            .size(28.dp)
                                            .offset(y = 2.dp),
                                )
                            }
                        }
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-4).dp, y = (-6).dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.settings_edit_label),
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    OutlinedTextField(
                        value = shortcutName,
                        onValueChange = { shortcutName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        maxLines = 1,
                        isError = !canSave,
                        label = { Text(stringResource(R.string.settings_app_shortcuts_shortcut_name_label)) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmedName, iconBase64) },
                enabled = canSave,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
fun AppShortcutCardHeader(
    packageName: String,
    appLabel: String,
    shortcutCount: Int,
    searchTarget: SearchTarget?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    iconPackPackage: String?,
) {
    val iconResult = rememberAppIcon(packageName = packageName, iconPackPackage = iconPackPackage)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        if (searchTarget != null) {
            SearchTargetIcon(
                target = searchTarget,
                iconSize = DesignTokens.IconSize,
                style = IconRenderStyle.ADVANCED,
            )
        } else if (iconResult.bitmap != null) {
            Image(
                bitmap = iconResult.bitmap,
                contentDescription = appLabel,
                modifier = Modifier.size(DesignTokens.IconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(DesignTokens.IconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = appLabel.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                text =
                    pluralStringResource(
                        R.plurals.settings_app_shortcuts_card_count,
                        shortcutCount,
                        shortcutCount,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize),
        )
    }
}

@Composable
fun ShortcutToggleRow(
    shortcut: StaticShortcut,
    checked: Boolean,
    showToggle: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onShortcutNameClick: () -> Unit,
    onEditClick: (() -> Unit)?,
    iconPackPackage: String?,
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val shortcutName = shortcutDisplayName(shortcut)
    val iconSize = DesignTokens.IconSize
    val density = LocalDensity.current
    val iconSizePx =
        remember(iconSize, density) {
            with(density) { iconSize.roundToPx().coerceAtLeast(1) }
        }
    val iconBitmap = rememberShortcutIcon(shortcut = shortcut, iconSizePx = iconSizePx)
    val appIconResult = rememberAppIcon(packageName = shortcut.packageName, iconPackPackage = iconPackPackage)
    val displayIcon = iconBitmap ?: appIconResult.bitmap

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardVerticalPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.ItemRowSpacing),
    ) {
        if (displayIcon != null) {
            Image(
                bitmap = displayIcon,
                contentDescription = shortcutName,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(iconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shortcutName.take(1).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = shortcutName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onShortcutNameClick,
                    ),
        )

        if (onEditClick != null) {
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.settings_edit_label),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (showToggle) {
            Switch(
                checked = checked,
                onCheckedChange = {
                    hapticToggle(view)()
                    onCheckedChange(it)
                },
                modifier = Modifier.scale(0.85f),
            )
        }
    }
}