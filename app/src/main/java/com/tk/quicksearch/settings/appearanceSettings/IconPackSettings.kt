package com.tk.quicksearch.settings.AppearanceSettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.shared.ui.theme.AppColors

@Composable
fun IconPackPickerDialog(
        availableIconPacks: List<IconPackInfo>,
        selectedPackage: String?,
        maskUnsupportedIcons: Boolean,
        onSelect: (String?) -> Unit,
        onMaskUnsupportedIconsChange: (Boolean) -> Unit,
        onDismiss: () -> Unit,
) {
    AppAlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                        text =
                                androidx.compose.ui.res.stringResource(
                                        R.string.settings_icon_pack_picker_title,
                                ),
                )
            },
            text = {
                Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (availableIconPacks.isEmpty()) {
                        Text(
                                text =
                                        androidx.compose.ui.res.stringResource(
                                                R.string.settings_icon_pack_empty,
                                        ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .heightIn(max = 320.dp)
                                                .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconPackOptionRow(
                                    label =
                                            androidx.compose.ui.res.stringResource(
                                                    R.string.settings_icon_pack_option_system,
                                            ),
                                    packageName = null,
                                    selected = selectedPackage == null,
                                    onClick = { onSelect(null) },
                            )
                            availableIconPacks.forEach { pack ->
                                HorizontalDivider(color = AppColors.SettingsDivider)
                                IconPackOptionRow(
                                        label = pack.label,
                                        packageName = pack.packageName,
                                        selected = selectedPackage == pack.packageName,
                                        onClick = { onSelect(pack.packageName) },
                                )
                            }
                        }
                        HorizontalDivider(color = AppColors.SettingsDivider)
                        IconPackMaskToggleRow(
                                enabled = maskUnsupportedIcons,
                                onEnabledChange = onMaskUnsupportedIconsChange,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                            androidx.compose.ui.res.stringResource(R.string.dialog_done),
                    )
                }
            },
    )
}

@Composable
private fun IconPackMaskToggleRow(
        enabled: Boolean,
        onEnabledChange: (Boolean) -> Unit,
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable { onEnabledChange(!enabled) }
                            .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.offset(x = (-4).dp)) {
            Checkbox(
                    checked = enabled,
                    onCheckedChange = { checked -> onEnabledChange(checked) },
            )
        }
        Text(
                text = stringResource(R.string.settings_icon_pack_mask_unsupported_icons),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun IconPackOptionRow(
        label: String,
        packageName: String?,
        selected: Boolean,
        onClick: () -> Unit,
) {
    val iconBitmap = packageName?.let { rememberAppIcon(packageName = it).bitmap }
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = Modifier.offset(x = (-4).dp),
        )
        if (iconBitmap != null) {
            Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(24.dp))
        } else {
            Icon(
                    imageVector = Icons.Rounded.Android,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
        )
    }
}
