package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

/**
 * One tile of the Background picker's bento grid, with its label below and the selection check
 * centered on the preview. The preview fills the height left over by the label, or is
 * [tileHeight] tall when given.
 */
@Composable
internal fun BackgroundSourceTile(
        label: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        hasImage: Boolean = false,
        tileHeight: Dp? = null,
        content: @Composable BoxScope.() -> Unit,
) {
    val isDarkMode = LocalAppIsDarkTheme.current
    Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .then(if (tileHeight != null) Modifier.height(tileHeight) else Modifier.weight(1f))
                                .clip(AppShapes.medium)
                                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                                .then(
                                        if (!hasImage || isDarkMode) {
                                            Modifier.border(
                                                    width = DesignTokens.BorderWidth,
                                                    color = AppColors.SettingsDivider,
                                                    shape = AppShapes.medium,
                                            )
                                        } else {
                                            Modifier
                                        },
                                ),
                contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
            if (selected) SourceTileCheck()
        }
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SourceTileCheck() {
    Box(
            modifier =
                    Modifier.size(DesignTokens.IconSize)
                            .clip(AppShapes.full)
                            .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
    ) {
        Icon(
                imageVector = MaterialSymbols.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(DesignTokens.ButtonIconSizeExtraSmall),
        )
    }
}
