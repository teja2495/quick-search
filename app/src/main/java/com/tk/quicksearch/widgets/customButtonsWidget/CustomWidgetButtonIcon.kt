package com.tk.quicksearch.widgets.customButtonsWidget

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.contacts.components.ContactAvatar
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import java.util.Locale

@Composable
fun CustomWidgetButtonIcon(
    action: CustomWidgetButtonAction,
    iconSize: Dp,
    iconPackPackage: String?,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.secondary,
) {
    // User-set custom icon takes precedence over all type-specific icons.
    val customIconBase64 = action.customIconBase64
    if (!customIconBase64.isNullOrBlank()) {
        val bitmap = remember(customIconBase64) {
            runCatching {
                val bytes = Base64.decode(customIconBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = action.contentDescription(),
                modifier = modifier.size(iconSize),
            )
            return
        }
    }

    when (action) {
        is CustomWidgetButtonAction.App -> {
            val iconBitmap =
                rememberAppIcon(
                    packageName = action.packageName,
                    iconPackPackage = iconPackPackage,
                ).bitmap
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = action.contentDescription(),
                    modifier = modifier.size(iconSize),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = action.contentDescription(),
                    tint = tintColor,
                    modifier = modifier.size(iconSize),
                )
            }
        }

        is CustomWidgetButtonAction.AppShortcut -> {
            val shortcut =
                remember(action) {
                    action.toStaticShortcut().let { staticShortcut ->
                        if (action.iconBase64.isNullOrBlank()) {
                            staticShortcut.copy(iconResId = null)
                        } else {
                            staticShortcut
                        }
                    }
                }
            val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
            val iconBitmap = rememberShortcutIcon(shortcut = shortcut, iconSizePx = iconSizePx)
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = action.contentDescription(),
                    modifier = modifier.size(iconSize),
                )
            } else {
                // Fallback for when shortcut icon can't be loaded
                val fallback =
                    action
                        .displayLabel()
                        .trim()
                        .take(1)
                        .uppercase(Locale.getDefault())
                        .ifBlank { "?" }
                Text(
                    text = fallback,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = modifier,
                )
            }
        }

        is CustomWidgetButtonAction.Contact -> {
            ContactAvatar(
                photoUri = action.photoUri,
                displayName = action.displayName,
                onClick = null,
                modifier = modifier.size(iconSize),
                textStyle = MaterialTheme.typography.labelSmall.copy(
                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.7f,
                ),
            )
        }

        is CustomWidgetButtonAction.File -> {
            val customIconRes = customWidgetFileIconRes(action)
            if (customIconRes != null) {
                Icon(
                    imageVector = ImageVector.vectorResource(customIconRes),
                    contentDescription = action.contentDescription(),
                    tint = Color.Unspecified,
                    modifier = modifier.size(iconSize),
                )
            } else {
                Icon(
                    imageVector = widgetFileIconVector(action),
                    contentDescription = action.contentDescription(),
                    tint = tintColor,
                    modifier = modifier.size(iconSize),
                )
            }
        }

        is CustomWidgetButtonAction.Setting -> {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = action.contentDescription(),
                tint = tintColor,
                modifier = modifier.size(iconSize),
            )
        }

        is CustomWidgetButtonAction.Note -> {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = action.contentDescription(),
                tint = tintColor,
                modifier = modifier.size(iconSize),
            )
        }
    }
}
