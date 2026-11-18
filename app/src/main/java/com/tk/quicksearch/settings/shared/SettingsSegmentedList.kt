package com.tk.quicksearch.settings.shared

import com.tk.quicksearch.onboarding.welcomeExitItem
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.shared.ui.theme.pressMorphSegmentShape
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.TonalIconBadgeContainer
import com.tk.quicksearch.shared.ui.components.TonalIconBadgeIconSize
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

/** A [SettingsCardItem] as a tappable tile with a tonal icon badge. */
@Composable
fun SettingsSegmentNavigationRow(
    item: SettingsCardItem,
    index: Int,
    count: Int,
) {
    SegmentedTile(
        index = index,
        count = count,
        onClick = if (item.isEnabled) item.actionOnPress else null,
    ) {
        SegmentedTileRow(
            title = item.title,
            description = item.description,
            leading = { SettingsIconBadge(item) },
        )
    }
}

/**
 * A [SettingsCardItem]'s icon in a [TonalIconBadge]. Vector icons take the accent; drawables
 * with their own colors (brand marks) keep them unless the item sets a tint.
 */
@Composable
fun SettingsIconBadge(item: SettingsCardItem) {
    TonalIconBadgeContainer {
        when {
            item.iconBitmap != null ->
                Image(
                    bitmap = item.iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(TonalIconBadgeIconSize),
                    contentScale = ContentScale.Fit,
                )
            item.icon != null ->
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.iconTint ?: AppColors.Accent,
                    modifier = Modifier.size(TonalIconBadgeIconSize),
                )
            item.iconResId != null ->
                Icon(
                    painter = painterResource(id = item.iconResId),
                    contentDescription = null,
                    tint = item.iconTint ?: Color.Unspecified,
                    modifier = Modifier.size(TonalIconBadgeIconSize),
                )
        }
    }
}
