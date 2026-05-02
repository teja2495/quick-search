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
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.SearchTargetShortcutMode
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.searchEngines.shared.IconRenderStyle
import com.tk.quicksearch.searchEngines.shared.SearchTargetIcon
import com.tk.quicksearch.settings.AppShortcutsSettings.AppShortcutSource
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.util.withoutWhitespaces
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
fun AppDeepLinkSourceRow(
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
                contentDescription = source.appLabel,
                modifier = Modifier.size(DesignTokens.IconSize),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Public,
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

        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.Add,
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_app_shortcuts_add_deep_link_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                                    contentDescription = appLabel,
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
                                    contentDescription = appLabel,
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
                                    contentDescription = appLabel,
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
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(nameFocusRequester),
                        singleLine = true,
                        maxLines = 1,
                        label = { Text(stringResource(R.string.settings_app_shortcuts_shortcut_name_label)) },
                        colors = dialogTextFieldColors(),
                    )
                }
                OutlinedTextField(
                    value = deepLinkInput,
                    onValueChange = { deepLinkInput = it.withoutWhitespaces() },
                    singleLine = true,
                    maxLines = 1,
                    label = { Text(stringResource(R.string.settings_app_shortcuts_deep_link_label)) },
                    colors = dialogTextFieldColors(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(trimmedName, normalizedValue, iconBase64) }, enabled = canSave) {
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
fun AddSearchTargetShortcutDialog(
    targetLabel: String,
    shortcutKind: SearchTargetShortcutKind,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFocusRequester = remember { FocusRequester() }
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

    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
        keyboardController?.show()
    }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(titleResId, targetLabel)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = shortcutName,
                    onValueChange = { shortcutName = it },
                    modifier = Modifier.focusRequester(nameFocusRequester),
                    singleLine = true,
                    maxLines = 1,
                    label = { Text(stringResource(R.string.settings_app_shortcuts_shortcut_name_label)) },
                    colors = dialogTextFieldColors(),
                )
                OutlinedTextField(
                    value = shortcutValue,
                    onValueChange = {
                        shortcutValue =
                            if (shortcutKind == SearchTargetShortcutKind.URL) {
                                it.withoutWhitespaces()
                            } else {
                                it
                            }
                    },
                    singleLine = false,
                    maxLines = 3,
                    label = { Text(stringResource(valueLabelResId)) },
                    colors = dialogTextFieldColors(),
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
    onSave: (String, String?, String?) -> Unit,
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
                        colors = dialogTextFieldColors(),
                    )
                }
                editableConfiguredValue?.let { editableValue ->
                    OutlinedTextField(
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
                        label = {
                            val labelResId =
                                when (editableValue.kind) {
                                    EditableShortcutValueKind.QUERY -> R.string.settings_app_shortcuts_query_label
                                    EditableShortcutValueKind.URL -> R.string.settings_app_shortcuts_url_label
                                    EditableShortcutValueKind.DEEP_LINK ->
                                        R.string.settings_app_shortcuts_deep_link_label
                                }
                            Text(stringResource(labelResId))
                        },
                        colors = dialogTextFieldColors(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(trimmedName, editableConfiguredValue?.let { trimmedValue }, iconBase64) },
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
    val hasEmbeddedOrOverrideIcon = !shortcut.iconBase64.isNullOrBlank()
    val displayIcon =
        if (hasEmbeddedOrOverrideIcon) {
            iconBitmap
        } else {
            iconBitmap ?: appIconResult.bitmap
        }

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
