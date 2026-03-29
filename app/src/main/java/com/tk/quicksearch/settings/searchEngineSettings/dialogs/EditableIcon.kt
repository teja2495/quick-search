package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

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
                .size(52.dp)
                .border(
                    width = 1.25.dp,
                    color = AppColors.SettingsDivider,
                    shape = RoundedCornerShape(14.dp),
                ).clickable { onClick() },
    ) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .align(androidx.compose.ui.Alignment.Center),
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = contentDescription,
                    modifier =
                        Modifier
                            .size(26.dp)
                            .align(androidx.compose.ui.Alignment.Center),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Public,
                    contentDescription = contentDescription,
                    modifier =
                        Modifier
                            .size(24.dp)
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
                        shape = CircleShape,
                    ),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
