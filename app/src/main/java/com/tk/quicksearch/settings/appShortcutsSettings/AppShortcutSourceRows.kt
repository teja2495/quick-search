package com.tk.quicksearch.settings.appShortcutsSettings

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.withoutWhitespaces
import java.util.Locale
import com.tk.quicksearch.shared.ui.components.AppTextButton
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppIconButtonStyle

@Composable
fun ShortcutSourceRow(
    source: AppShortcutSource,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        val icon = rememberShortcutSourceIcon(source)
        val iconBitmap = icon?.bitmap
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = source.label,
                modifier = Modifier.size(DesignTokens.IconSize),
                contentScale = ContentScale.Fit,
            )
        } else if (icon == null) {
            Box(modifier = Modifier.size(DesignTokens.IconSize))
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

        AppIconButton(
            onClick = onClick,
            tooltip = stringResource(R.string.settings_app_shortcuts_add_button),
            style = AppIconButtonStyle.Tonal,
        ) {
            Icon(
                imageVector = MaterialSymbols.Add,
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
    val actionLabelResId = R.string.settings_app_shortcuts_add_query_shortcut
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
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

        AppIconButton(
            onClick = onClick,
            tooltip = stringResource(actionLabelResId),
            style = AppIconButtonStyle.Tonal,
        ) {
            Icon(
                imageVector = MaterialSymbols.Add,
                contentDescription = stringResource(actionLabelResId),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun AppDeepLinkSourceRow(
    source: AppShortcutSource,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(
                    horizontal = DesignTokens.SpacingLarge,
                    vertical = DesignTokens.SpacingLarge,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        val icon = rememberShortcutSourceIcon(source)
        val iconBitmap = icon?.bitmap
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = source.appLabel,
                modifier = Modifier.size(DesignTokens.IconSize),
                contentScale = ContentScale.Fit,
            )
        } else if (icon == null) {
            Box(modifier = Modifier.size(DesignTokens.IconSize))
        } else {
            Icon(
                imageVector = MaterialSymbols.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
        }

        Text(
            text = stringResource(R.string.settings_app_shortcuts_add_deep_link),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        AppIconButton(
            onClick = onClick,
            tooltip = stringResource(R.string.settings_app_shortcuts_add_deep_link),
            style = AppIconButtonStyle.Tonal,
        ) {
            Icon(
                imageVector = MaterialSymbols.Add,
                contentDescription = stringResource(R.string.settings_app_shortcuts_add_deep_link),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun AddAppDeepLinkDialog(
    packageName: String,
    appLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }
    var shortcutName by remember(appLabel) {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var deepLinkInput by remember(appLabel) {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var iconBase64 by remember(packageName) { mutableStateOf<String?>(null) }
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
    val appIconResult = rememberAppIcon(packageName = packageName, iconPackPackage = null)
    val trimmedName = shortcutName.text.trim()
    val normalizedValue = deepLinkInput.text.trim()
    val canSave = trimmedName.isNotBlank() && normalizedValue.isNotBlank()

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
        keyboardController?.show()
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_app_shortcuts_add_deep_link_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
                Text(
                    text = stringResource(R.string.settings_app_shortcuts_add_deep_link_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    ShortcutIconPicker(
                        iconBitmap = iconBitmap ?: appIconResult.bitmap,
                        contentDescription = appLabel,
                        onClick = { pickIconLauncher.launch(arrayOf("image/*")) },
                    )
                    AppOutlinedTextField(
                        value = shortcutName,
                        onValueChange = { shortcutName = it },
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(nameFocusRequester),
                        label = stringResource(R.string.settings_app_shortcuts_shortcut_name_label),
                        shape = AppShapes.extraSmall,
                    )
                }
                AppOutlinedTextField(
                    value = deepLinkInput,
                    onValueChange = { deepLinkInput = it.withoutWhitespaces() },
                    label = stringResource(R.string.settings_app_shortcuts_deep_link_label),
                    shape = AppShapes.extraSmall,
                )
            }
        },
        confirmButton = {
            AppTextButton(onClick = { onSave(trimmedName, normalizedValue, iconBase64) }, enabled = canSave) {
                Text(text = stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
fun AddSearchTargetShortcutDialog(
    targetLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }
    var shortcutName by remember(targetLabel) {
        mutableStateOf(
            TextFieldValue(
                text = "",
                selection = TextRange(0),
            ),
        )
    }
    var shortcutValue by remember(targetLabel) {
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
    val titleResId = R.string.settings_app_shortcuts_add_query_dialog_title
    val valueLabelResId = R.string.settings_app_shortcuts_query_label

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
        keyboardController?.show()
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleResId, targetLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
                AppOutlinedTextField(
                    value = shortcutName,
                    onValueChange = { shortcutName = it },
                    modifier = Modifier.focusRequester(nameFocusRequester),
                    label = stringResource(R.string.settings_app_shortcuts_shortcut_name_label),
                    shape = AppShapes.extraSmall,
                )
                AppOutlinedTextField(
                    value = shortcutValue,
                    onValueChange = { shortcutValue = it },
                    singleLine = false,
                    maxLines = 3,
                    label = stringResource(valueLabelResId),
                    shape = AppShapes.extraSmall,
                )
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { onSave(trimmedName, trimmedValue) },
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
}
