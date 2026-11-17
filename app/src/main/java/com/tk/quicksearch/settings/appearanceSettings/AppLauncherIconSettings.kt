package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm
import com.tk.quicksearch.shared.ui.theme.pressMorphShape

private object LauncherIconPickerSpacing {
    val optionSpacing = DesignTokens.ItemRowSpacing
    val tilePaddingHorizontal = DesignTokens.ChipHorizontalPadding
    val tilePaddingVertical = DesignTokens.ChipVerticalPadding
}

private val LauncherIconPreviewSize = DesignTokens.Spacing48

private const val IconsPerRow = 4

private val LauncherIconTileRingWidth = 2.dp
private val LauncherIconCheckSize = 20.dp
private val LauncherIconCheckIconSize = 14.dp
private val LauncherIconCheckInset = 6.dp

// Same 14% accent tint as unselected choice buttons, so tiles read over wallpaper and AMOLED.

@Composable
fun AppLauncherIconCard(
    launcherAppIcon: LauncherAppIcon,
    onSetLauncherAppIcon: (LauncherAppIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = launcherIconPickerTiles()

    SegmentedGroup(modifier = modifier) {
      SegmentedTile(index = 0, count = 1) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.SpacingLarge,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            Text(
                text = stringResource(R.string.settings_launcher_icon_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = DesignTokens.SpacingXSmall),
            )

            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(LauncherIconPickerSpacing.optionSpacing),
            ) {
                tiles.chunked(IconsPerRow).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LauncherIconPickerSpacing.optionSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        rowTiles.forEach { tile ->
                            val selected = launcherAppIcon == tile.selection
                            LauncherIconOptionTile(
                                previewMipmapRes = tile.previewMipmapRes,
                                selected = selected,
                                onClick = {
                                    if (launcherAppIcon != tile.selection) {
                                        onSetLauncherAppIcon(tile.selection)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(IconsPerRow - rowTiles.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            TonalButton(
                text = stringResource(R.string.settings_launcher_icon_reset_default),
                icon = MaterialSymbols.RestartAlt,
                onClick = { onSetLauncherAppIcon(LauncherAppIcon.DEFAULT) },
                enabled = launcherAppIcon != LauncherAppIcon.DEFAULT,
                modifier = Modifier.fillMaxWidth(),
            )
        }
      }
    }
}

@Composable
private fun LauncherIconOptionTile(
    previewMipmapRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sizePx = with(density) { LauncherIconPreviewSize.roundToPx() }
    val painter =
        remember(previewMipmapRes, sizePx) {
            val drawable = AppCompatResources.getDrawable(context, previewMipmapRes) ?: return@remember null
            BitmapPainter(drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap())
        }
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    // Material 3 Expressive selection: corners round further while pressed or selected.
    val shape =
        pressMorphShape(
            interactionSource,
            rest = DesignTokens.CornerLarge,
            pressed = DesignTokens.CornerExtraLarge,
            held = selected,
        )
    val containerColor by animateColorAsState(
        targetValue = if (selected) AppColors.AccentTintEmphasis else AppColors.AccentTintTonal,
        label = "launcherIconTileContainer",
    )
    val ringColor by animateColorAsState(
        targetValue = if (selected) AppColors.Accent else Color.Transparent,
        label = "launcherIconTileRing",
    )

    Box(
        modifier =
            modifier
                .clip(shape)
                .background(color = containerColor, shape = shape)
                .border(width = LauncherIconTileRingWidth, color = ringColor, shape = shape)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = {
                        hapticConfirm(view)()
                        onClick()
                    },
                    role = Role.RadioButton,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = LauncherIconPickerSpacing.tilePaddingVertical,
                        horizontal = LauncherIconPickerSpacing.tilePaddingHorizontal,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(LauncherIconPreviewSize).clip(CircleShape),
                )
            } else {
                Spacer(modifier = Modifier.size(LauncherIconPreviewSize))
            }
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(LauncherIconCheckInset),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(LauncherIconCheckSize)
                        .clip(CircleShape)
                        .background(AppColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Check,
                    contentDescription = null,
                    tint = AppColors.OnAccent,
                    modifier = Modifier.size(LauncherIconCheckIconSize),
                )
            }
        }
    }
}

private data class LauncherIconTile(
    val selection: LauncherAppIcon,
    val previewMipmapRes: Int,
)

private fun launcherIconPickerTiles(): List<LauncherIconTile> {
    fun tile(icon: LauncherAppIcon) =
        LauncherIconTile(
            selection = icon,
            previewMipmapRes = launcherAppIconToPreviewMipmap(icon),
        )

    return listOf(
        tile(LauncherAppIcon.MONOCHROME_LIGHT),
        tile(LauncherAppIcon.MONOCHROME_DARK),
        tile(LauncherAppIcon.FOREST_LIGHT),
        tile(LauncherAppIcon.FOREST_DARK),
        tile(LauncherAppIcon.AURORA_LIGHT),
        tile(LauncherAppIcon.AURORA_DARK),
        tile(LauncherAppIcon.SUNSET_LIGHT),
        tile(LauncherAppIcon.SUNSET_DARK),
    )
}

private fun launcherAppIconToPreviewMipmap(icon: LauncherAppIcon): Int =
    when (icon) {
        LauncherAppIcon.DEFAULT -> R.mipmap.ic_launcher
        LauncherAppIcon.MONOCHROME_LIGHT -> R.mipmap.ic_launcher_monochrome_light
        LauncherAppIcon.MONOCHROME_DARK -> R.mipmap.ic_launcher_monochrome_dark
        LauncherAppIcon.FOREST_LIGHT -> R.mipmap.ic_launcher_forest_light
        LauncherAppIcon.FOREST_DARK -> R.mipmap.ic_launcher_forest_dark
        LauncherAppIcon.AURORA_LIGHT -> R.mipmap.ic_launcher_aurora_light
        LauncherAppIcon.AURORA_DARK -> R.mipmap.ic_launcher_aurora_dark
        LauncherAppIcon.SUNSET_LIGHT -> R.mipmap.ic_launcher_sunset_light
        LauncherAppIcon.SUNSET_DARK -> R.mipmap.ic_launcher_sunset_dark
    }
