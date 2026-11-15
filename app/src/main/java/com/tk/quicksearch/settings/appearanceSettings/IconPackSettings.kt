package com.tk.quicksearch.settings.appearanceSettings

import com.tk.quicksearch.settings.shared.SettingsSwitch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.settings.shared.SettingsSegment
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun IconPackPickerDialog(
        availableIconPacks: List<IconPackInfo>,
        selectedPackage: String?,
        maskUnsupportedIcons: Boolean,
        onSelect: (String?) -> Unit,
        onMaskUnsupportedIconsChange: (Boolean) -> Unit,
        onDownloadIconPacks: () -> Unit = {},
        onResetAllIcons: () -> Unit,
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
                        modifier =
                                Modifier.fillMaxWidth()
                                        .heightIn(
                                                min = if (availableIconPacks.isEmpty()) 220.dp else 0.dp,
                                                max = 400.dp,
                                        ),
                        verticalArrangement =
                                if (availableIconPacks.isEmpty()) {
                                    Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                                } else {
                                    Arrangement.spacedBy(12.dp)
                                },
                ) {
                    if (availableIconPacks.isEmpty()) {
                        Text(
                                text =
                                        androidx.compose.ui.res.stringResource(
                                                R.string.settings_icon_pack_empty,
                                        ),
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                        )
                        Button(
                                onClick = {
                                            onDismiss()
                                            onDownloadIconPacks()
                                        },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = AppColors.Accent,
                                                contentColor = AppColors.OnAccent,
                                        ),
                        ) {
                            Text(stringResource(R.string.icon_picker_download_icon_packs))
                        }
                    } else {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .heightIn(max = 320.dp)
                                                .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(DesignTokens.SegmentedListGap),
                        ) {
                            val count = availableIconPacks.size + 1
                            IconPackOptionRow(
                                    label =
                                            androidx.compose.ui.res.stringResource(
                                                    R.string.settings_icon_pack_option_system,
                                            ),
                                    packageName = null,
                                    selected = selectedPackage == null,
                                    onClick = { onSelect(null) },
                                    index = 0,
                                    count = count,
                            )
                            availableIconPacks.forEachIndexed { index, pack ->
                                IconPackOptionRow(
                                        label = pack.label,
                                        packageName = pack.packageName,
                                        selected = selectedPackage == pack.packageName,
                                        onClick = { onSelect(pack.packageName) },
                                        index = index + 1,
                                        count = count,
                                )
                            }
                        }
                        if (selectedPackage != null) {
                            IconPackMaskToggleRow(
                                    enabled = maskUnsupportedIcons,
                                    onEnabledChange = onMaskUnsupportedIconsChange,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_done))
                }
            },
            dismissButton = {
                TextButton(
                        onClick = {
                            onResetAllIcons()
                            onDismiss()
                        },
                        colors =
                                ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                ) {
                    Text(stringResource(R.string.settings_icon_pack_reset_all_icons))
                }
            },
    )
}

/** Whether to mask icons the pack lacks: a tile that toggles as a whole, with the expressive switch. */
@Composable
private fun IconPackMaskToggleRow(
        enabled: Boolean,
        onEnabledChange: (Boolean) -> Unit,
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(DesignTokens.SegmentedListOuterCorner))
                            .background(AppColors.Accent.copy(alpha = 0.08f))
                            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                            .padding(
                                    start = DesignTokens.SpacingLarge,
                                    end = DesignTokens.SpacingMedium,
                                    top = DesignTokens.SpacingMedium,
                                    bottom = DesignTokens.SpacingMedium,
                            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Text(
                text = stringResource(R.string.settings_icon_pack_mask_unsupported_icons),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
        )
        SettingsSwitch(checked = enabled, onCheckedChange = null)
    }
}

/** One icon pack as a selectable tile at [index] of [count]: radio, pack icon, and name. */
@Composable
private fun IconPackOptionRow(
        label: String,
        packageName: String?,
        selected: Boolean,
        onClick: () -> Unit,
        index: Int,
        count: Int,
) {
    val iconBitmap = packageName?.let { rememberAppIcon(packageName = it).bitmap }
    SettingsSegment(
            index = index,
            count = count,
            containerColor =
                    if (selected) {
                        AppColors.Accent.copy(alpha = 0.14f).compositeOver(AppColors.getSettingsCardContainerColor())
                    } else {
                        AppColors.getSettingsCardContainerColor()
                    },
    ) {
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                                .padding(horizontal = DesignTokens.SpacingMedium, vertical = DesignTokens.SpacingMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            if (iconBitmap != null) {
                Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(32.dp))
            } else {
                Icon(
                        imageVector = MaterialSymbols.Android,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = AppColors.Accent,
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
            RadioButton(
                    selected = selected,
                    onClick = null,
                    colors =
                            RadioButtonDefaults.colors(
                                    selectedColor = AppColors.Accent,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
            )
        }
    }
}
