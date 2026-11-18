package com.tk.quicksearch.settings.appShortcutsSettings

import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.components.AppSwitch
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
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.appShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.util.withoutWhitespaces
import java.util.Locale
import com.tk.quicksearch.shared.ui.theme.AppMotion
import com.tk.quicksearch.shared.ui.components.AppTextButton

@Composable
fun EditCustomShortcutDialog(
    shortcut: StaticShortcut,
    iconPackPackage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?) -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember(shortcut) { mutableStateOf(false) }
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
    val editableConfiguredValue = remember(shortcut) { resolveEditableConfiguredValue(shortcut) }
    var shortcutValue by remember(shortcut, editableConfiguredValue) {
        mutableStateOf(
            TextFieldValue(
                text = editableConfiguredValue?.value.orEmpty(),
                selection = TextRange(editableConfiguredValue?.value?.length ?: 0),
            ),
        )
    }
    val trimmedName = shortcutName.text.trim()
    val trimmedValue = shortcutValue.text.trim()
    val canSave = trimmedName.isNotBlank() && (editableConfiguredValue == null || trimmedValue.isNotBlank())

    val scope = rememberCoroutineScope()
    val pickIconLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    loadCustomIconAsBase64(context, uri, maxSizePx = 256)
                } ?: return@launch
                iconBase64 = encoded
            }
        }

    val iconBitmap =
        remember(iconBase64) {
            val encoded = iconBase64 ?: return@remember null
            val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                ?: return@remember null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    val appIconResult = rememberAppIcon(packageName = shortcut.packageName, iconPackPackage = iconPackPackage)

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = stringResource(R.string.settings_app_shortcuts_edit_dialog_title))
                AppIconButton(onClick = { showDeleteConfirm = true }, tooltip = stringResource(R.string.settings_app_shortcuts_delete_action)) {
                    Icon(
                        imageVector = MaterialSymbols.DeleteOutline,
                        contentDescription = stringResource(R.string.settings_app_shortcuts_delete_action),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    ShortcutIconPicker(
                        iconBitmap = iconBitmap ?: appIconResult.bitmap,
                        contentDescription = trimmedName,
                        onClick = { pickIconLauncher.launch(arrayOf("image/*")) },
                    )
                    AppOutlinedTextField(
                        value = shortcutName,
                        onValueChange = { shortcutName = it },
                        modifier = Modifier.weight(1f),
                        isError = !canSave,
                        label = stringResource(R.string.settings_app_shortcuts_shortcut_name_label),
                        shape = AppShapes.extraSmall,
                    )
                }
                editableConfiguredValue?.let { editableValue ->
                    val labelResId =
                        when (editableValue.kind) {
                            EditableShortcutValueKind.QUERY -> R.string.settings_app_shortcuts_query_label
                            EditableShortcutValueKind.URL -> R.string.settings_app_shortcuts_url_label
                            EditableShortcutValueKind.DEEP_LINK ->
                                R.string.settings_app_shortcuts_deep_link_label
                        }
                    AppOutlinedTextField(
                        value = shortcutValue,
                        onValueChange = {
                            shortcutValue =
                                if (editableValue.kind == EditableShortcutValueKind.URL ||
                                    editableValue.kind == EditableShortcutValueKind.DEEP_LINK
                                ) {
                                    it.withoutWhitespaces()
                                } else {
                                    it
                                }
                        },
                        singleLine = editableValue.kind != EditableShortcutValueKind.QUERY,
                        maxLines = if (editableValue.kind == EditableShortcutValueKind.QUERY) 3 else 1,
                        isError = trimmedValue.isBlank(),
                        label = stringResource(labelResId),
                        shape = AppShapes.extraSmall,
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { onSave(trimmedName, editableConfiguredValue?.let { trimmedValue }, iconBase64) },
                enabled = canSave,
            ) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )

    if (showDeleteConfirm) {
        AppAlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.dialog_delete_item_title, shortcutDisplayName(shortcut))) },
            confirmLabel = stringResource(R.string.dialog_delete),
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            dismissLabel = stringResource(R.string.dialog_cancel),
            destructive = true,
        )
    }
}

private enum class EditableShortcutValueKind {
    QUERY,
    URL,
    DEEP_LINK,
}

private data class EditableShortcutValue(
    val kind: EditableShortcutValueKind,
    val value: String,
)

private fun resolveEditableConfiguredValue(shortcut: StaticShortcut): EditableShortcutValue? {
    if (shortcut.id.startsWith("custom_deeplink_")) {
        val deepLink =
            shortcut.intents
                .asSequence()
                .mapNotNull { it.dataString?.trim() }
                .firstOrNull { it.isNotBlank() }
                ?: return null
        return EditableShortcutValue(kind = EditableShortcutValueKind.DEEP_LINK, value = deepLink)
    }

    val searchIntent =
        shortcut.intents.firstOrNull {
            it.action == SearchTargetQueryShortcutActivity.ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT
        } ?: return null
    val query =
        searchIntent
            .getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_QUERY)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
    val targetType = searchIntent.getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_TARGET_TYPE)
    if (targetType == SearchTargetQueryShortcutActivity.TARGET_TYPE_BROWSER) {
        val mode =
            searchIntent
                .getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_BROWSER_SHORTCUT_MODE)
                ?.let { runCatching { SearchTargetShortcutMode.valueOf(it) }.getOrNull() }
                ?: SearchTargetShortcutMode.AUTO
        if (mode == SearchTargetShortcutMode.FORCE_URL) {
            return EditableShortcutValue(kind = EditableShortcutValueKind.URL, value = query)
        }
    }
    return EditableShortcutValue(kind = EditableShortcutValueKind.QUERY, value = query)
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
    allShortcutsDisabled: Boolean = false,
) {
    val iconResult = rememberAppIcon(packageName = packageName, iconPackPackage = iconPackPackage)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = AppMotion.defaultSpatial(),
        label = "shortcutGroupChevron",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggleExpanded)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        if (searchTarget != null) {
            SearchTargetIcon(
                target = searchTarget,
                iconSize = DesignTokens.Spacing40,
                style = IconRenderStyle.ADVANCED,
            )
        } else if (iconResult.bitmap != null) {
            Image(
                bitmap = iconResult.bitmap,
                contentDescription = appLabel,
                modifier = Modifier.size(DesignTokens.Spacing40),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier.size(DesignTokens.Spacing40),
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
                    if (allShortcutsDisabled) {
                        stringResource(R.string.settings_app_shortcuts_card_all_disabled)
                    } else {
                        pluralStringResource(
                            R.plurals.settings_app_shortcuts_card_count,
                            shortcutCount,
                            shortcutCount,
                        )
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = MaterialSymbols.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.IconSize).rotate(chevronRotation),
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
    toggleEnabled: Boolean = true,
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
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
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
                        role = Role.Button,
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onShortcutNameClick,
                    ),
        )

        if (onEditClick != null) {
            AppIconButton(onClick = onEditClick, tooltip = stringResource(R.string.settings_edit_label)) {
                Icon(
                    imageVector = MaterialSymbols.Edit,
                    contentDescription = stringResource(R.string.settings_edit_label),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (showToggle) {
            AppSwitch(
                checked = checked,
                onCheckedChange = {
                    hapticToggle(view)()
                    onCheckedChange(it)
                },
                enabled = toggleEnabled,
            )
        }
    }
}

/** App-wide switch that also covers shortcuts the app adds later. */
@Composable
fun DisableAllAppShortcutsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = {
                        hapticToggle(view)()
                        onCheckedChange(it)
                    },
                ).padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        Text(
            text = stringResource(R.string.settings_app_shortcuts_disable_all),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        AppSwitch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}
