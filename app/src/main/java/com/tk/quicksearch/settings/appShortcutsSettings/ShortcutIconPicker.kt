package com.tk.quicksearch.settings.appShortcutsSettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private val ShortcutIconSize = 32.dp
private val ShortcutIconBadgeSize = 16.dp

/**
 * Shortcut icon with an edit badge, tapped to pick a custom image. Shared by the shortcut edit
 * dialog and the add-deep-link dialog. Falls back to a globe when there's no icon yet.
 */
@Composable
internal fun ShortcutIconPicker(
    iconBitmap: ImageBitmap?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(DesignTokens.MinTouchTarget)
                .clip(AppShapes.medium)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.settings_edit_label),
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = contentDescription,
                modifier = Modifier.size(ShortcutIconSize).offset(y = 2.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(
                imageVector = MaterialSymbols.Public,
                contentDescription = contentDescription,
                modifier = Modifier.size(ShortcutIconSize).offset(y = 2.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = -DesignTokens.SpacingXSmall, y = (-6).dp)
                    .size(ShortcutIconBadgeSize)
                    .clip(AppShapes.full)
                    .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.Edit,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.EditBadgeIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
