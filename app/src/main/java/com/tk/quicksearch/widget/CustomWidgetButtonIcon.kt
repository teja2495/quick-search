package com.tk.quicksearch.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import com.tk.quicksearch.search.data.ShortcutIcon
import com.tk.quicksearch.search.data.rememberShortcutIcon
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils

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
            Box(modifier = modifier.size(iconSize)) {
                ShortcutIcon(
                    icon = iconBitmap,
                    displayName = action.displayLabel(),
                    size = iconSize
                )
            }
        }
        is CustomWidgetButtonAction.Contact -> {
            ContactAvatar(
                photoUri = action.photoUri,
                displayName = action.displayName,
                onClick = null,
                modifier = modifier.size(iconSize)
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
    when {
        action.isDirectory -> Icons.Rounded.Folder
        else -> when (FileTypeUtils.getFileType(action.toDeviceFile())) {
            FileType.MUSIC -> Icons.Rounded.MusicNote
            FileType.PICTURES -> Icons.Rounded.Image
            FileType.VIDEOS -> Icons.Rounded.VideoLibrary
            FileType.APKS -> Icons.Rounded.Android
            else -> Icons.AutoMirrored.Rounded.InsertDriveFile
        }
    }
