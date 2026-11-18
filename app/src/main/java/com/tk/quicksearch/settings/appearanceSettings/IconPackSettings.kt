package com.tk.quicksearch.settings.appearanceSettings

import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.components.AppSwitch
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.IconPackInfo
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppTextButton

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
    var showResetConfirmation by rememberSaveable { mutableStateOf(false) }
    val resetLabel = stringResource(R.string.settings_icon_pack_reset_all_icons)
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
                                    Arrangement.spacedBy(DesignTokens.SpacingLarge, Alignment.CenterVertically)
                                } else {
                                    Arrangement.spacedBy(DesignTokens.SpacingMedium)
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
                        AppButton(
                                onClick = {
                                            onDismiss()
                                            onDownloadIconPacks()
                                        },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(stringResource(R.string.icon_picker_download_icon_packs))
                        }
                    } else {
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .heightIn(max = 320.dp)
                                                .verticalScroll(rememberScrollState()),
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
                                IconPackOptionRow(
                                        label = pack.label,
                                        packageName = pack.packageName,
                                        selected = selectedPackage == pack.packageName,
                                        onClick = { onSelect(pack.packageName) },
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
                AppTextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_done))
                }
            },
            dismissButton = {
                AppTextButton(
                        onClick = { showResetConfirmation = true },
                        tone = AppButtonTone.Neutral,
                ) {
                    Text(resetLabel)
                }
            },
    )
    if (showResetConfirmation) {
        AppAlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(resetLabel) },
            confirmLabel = stringResource(R.string.dialog_reset),
            dismissLabel = stringResource(R.string.dialog_cancel),
            destructive = true,
            onConfirm = {
                showResetConfirmation = false
                onResetAllIcons()
                onDismiss()
            },
        )
    }
}

/** Whether to mask icons the pack lacks: a row that toggles as a whole, with the expressive switch. */
@Composable
private fun IconPackMaskToggleRow(
        enabled: Boolean,
        onEnabledChange: (Boolean) -> Unit,
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .heightIn(min = IconPackRowMinHeight)
                            .clip(IconPackRowShape)
                            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
                            .padding(horizontal = DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Text(
                text = stringResource(R.string.settings_icon_pack_mask_unsupported_icons),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
        )
        AppSwitch(checked = enabled, onCheckedChange = null)
    }
}

/** One icon pack as a Material 3 single-choice row: radio, pack icon, and name. */
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
                    Modifier.fillMaxWidth()
                            .heightIn(min = IconPackRowMinHeight)
                            .clip(IconPackRowShape)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                            .padding(horizontal = DesignTokens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        RadioButton(
                selected = selected,
                onClick = null,
                colors =
                        RadioButtonDefaults.colors(
                                selectedColor = AppColors.Accent,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
        )
        if (iconBitmap != null) {
            Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(IconPackIconSize))
        } else {
            Icon(
                    imageVector = MaterialSymbols.Android,
                    contentDescription = null,
                    modifier = Modifier.size(IconPackIconSize),
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
    }
}

private val IconPackRowMinHeight = 56.dp
private val IconPackRowShape = AppShapes.medium
private val IconPackIconSize = 32.dp
