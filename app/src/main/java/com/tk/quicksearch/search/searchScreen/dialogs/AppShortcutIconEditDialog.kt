package com.tk.quicksearch.search.searchScreen.dialogs

import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.util.Locale
import com.tk.quicksearch.shared.ui.components.AppTextButton

private val PreviewIconBoxSize = DesignTokens.AppIconSize + DesignTokens.SpacingMedium

@Composable
internal fun AppShortcutIconEditDialog(
    shortcut: StaticShortcut,
    iconPackPackage: String?,
    currentIconBase64: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var iconBase64 by remember(shortcut, currentIconBase64) { mutableStateOf(currentIconBase64) }
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
    val title = shortcutDisplayName(shortcut)
    val previewDescription = stringResource(R.string.action_edit_icon)

    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = AppShapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = DesignTokens.ElevationLevel0,
                modifier = Modifier.size(DesignTokens.IconSizeXLarge),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = MaterialSymbols.Image,
                        contentDescription = null,
                        modifier = Modifier.size(DesignTokens.LargeIconSize),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.action_edit_icon),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge)) {
                Text(
                    text = stringResource(R.string.dialog_app_shortcut_icon_message, title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    onClick = { pickIconLauncher.launch(arrayOf("image/*")) },
                    shape = AppShapes.resultCard,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = AppColors.ContentMutedAlpha),
                    border =
                        BorderStroke(
                            DesignTokens.BorderWidth,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppColors.ContentNearOpaqueAlpha),
                        ),
                    tonalElevation = DesignTokens.ElevationLevel0,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = previewDescription },
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = DesignTokens.SpacingXLarge,
                                    horizontal = DesignTokens.SpacingMedium,
                                ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                    ) {
                        ShortcutIconPreviewContent(
                            title = title,
                            iconBitmap = iconBitmap,
                            iconBase64 = iconBase64,
                            appIconBitmap = appIconResult.bitmap,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
                if (iconBase64 != null) {
                    AppTextButton(
                        onClick = { iconBase64 = null },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(DesignTokens.IconSizeSmall),
                            )
                            Text(
                                text = stringResource(R.string.action_reset_app_shortcut_icon),
                                modifier = Modifier.padding(start = DesignTokens.TextButtonIconSpacing),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = { onSave(iconBase64) }) {
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
private fun ShortcutIconPreviewContent(
    title: String,
    iconBitmap: ImageBitmap?,
    iconBase64: String?,
    appIconBitmap: ImageBitmap?,
) {
    Box(
        modifier = Modifier.size(PreviewIconBoxSize),
        contentAlignment = Alignment.Center,
    ) {
        when {
            iconBitmap != null -> {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = title,
                    modifier = Modifier.size(DesignTokens.IconSizeXLarge),
                    contentScale = ContentScale.Fit,
                )
            }
            !iconBase64.isNullOrBlank() -> {
                Text(
                    text =
                        title.trim().take(1).uppercase(Locale.getDefault()).ifBlank {
                            "?"
                        },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            appIconBitmap != null -> {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = title,
                    modifier = Modifier.size(DesignTokens.IconSizeXLarge),
                    contentScale = ContentScale.Fit,
                )
            }
            else -> {
                Icon(
                    imageVector = MaterialSymbols.Public,
                    contentDescription = title,
                    modifier = Modifier.size(DesignTokens.LargeIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-4).dp)
                    .size(DesignTokens.IconSize)
                    .clip(AppShapes.full)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.Edit,
                contentDescription = stringResource(R.string.settings_edit_label),
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
