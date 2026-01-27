package com.tk.quicksearch.widget.customButtons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import java.util.Locale
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import com.tk.quicksearch.search.data.rememberShortcutIcon

@Composable
fun CustomWidgetButtonIcon(
    action: CustomWidgetButtonAction,
    iconSize: Dp,
    iconPackPackage: String?,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.secondary
) {
    when (action) {
        is CustomWidgetButtonAction.App -> {
            val iconBitmap = rememberAppIcon(
                packageName = action.packageName,
                iconPackPackage = iconPackPackage
            )
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = action.contentDescription(),
                    modifier = modifier.size(iconSize)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = action.contentDescription(),
                    tint = tintColor,
                    modifier = modifier.size(iconSize)
                )
            }
        }
        is CustomWidgetButtonAction.AppShortcut -> {
            val shortcut = remember(action) { action.toStaticShortcut() }
            val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
            val iconBitmap = rememberShortcutIcon(shortcut = shortcut, iconSizePx = iconSizePx)
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = action.contentDescription(),
                    modifier = modifier.size(iconSize)
                )
            } else {
                // Fallback for when shortcut icon can't be loaded
                val fallback = action.displayLabel().trim().take(1)
                    .uppercase(
                        Locale.getDefault())
                    .ifBlank { "?" }
                Text(
                    text = fallback,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = modifier
                )
            }
        }
        is CustomWidgetButtonAction.Contact -> {
            ContactAvatar(
                photoUri = action.photoUri,
                displayName = action.displayName,
                onClick = null,
                modifier = modifier.size(iconSize),
                textStyle = MaterialTheme.typography.labelSmall
            )
        }
        is CustomWidgetButtonAction.File -> {
            Icon(
                imageVector = fileIconVector(action),
                contentDescription = action.contentDescription(),
                tint = tintColor,
                modifier = modifier.size(iconSize)
            )
        }
        is CustomWidgetButtonAction.Setting -> {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = action.contentDescription(),
                tint = tintColor,
                modifier = modifier.size(iconSize)
            )
        }
    }
}

private fun fileIconVector(action: CustomWidgetButtonAction.File) =
    if (action.isDirectory) Icons.Rounded.Folder
    else Icons.AutoMirrored.Rounded.InsertDriveFile
