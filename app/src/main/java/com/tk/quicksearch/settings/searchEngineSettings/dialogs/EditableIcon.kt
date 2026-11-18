package com.tk.quicksearch.settings.searchEngineSettings.dialogs

import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
fun EditableIcon(
    iconBitmap: ImageBitmap?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(DesignTokens.IconSizeXLarge)
                .border(
                    width = DesignTokens.BorderWidth,
                    color = AppColors.SettingsDivider,
                    shape = AppShapes.iconFrame,
                ).clip(AppShapes.iconFrame)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(
            modifier =
                Modifier
                    .size(DesignTokens.IconSize)
                    .align(androidx.compose.ui.Alignment.Center),
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = contentDescription,
                    modifier =
                        Modifier
                            .size(DesignTokens.IconSize)
                            .align(androidx.compose.ui.Alignment.Center),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = MaterialSymbols.Public,
                    contentDescription = contentDescription,
                    modifier =
                        Modifier
                            .size(DesignTokens.IconSize)
                            .align(androidx.compose.ui.Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .align(androidx.compose.ui.Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = AppShapes.full,
                    ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.Edit,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
