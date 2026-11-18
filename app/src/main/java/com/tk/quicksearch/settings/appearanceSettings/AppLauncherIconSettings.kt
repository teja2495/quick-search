package com.tk.quicksearch.settings.appearanceSettings

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.CUSTOM_APP_THEME_PALETTES
import com.tk.quicksearch.search.core.LauncherAppIcon
import com.tk.quicksearch.shared.ui.components.AppBottomSheet
import com.tk.quicksearch.shared.ui.components.AppDrawerHeader
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.pressMorphShape
import com.tk.quicksearch.shared.util.hapticConfirm

private val LauncherIconPreviewSize = DesignTokens.Spacing48
private val LauncherIconRowPreviewSize = DesignTokens.Spacing40

private const val ICONS_PER_ROW = 4
private val LauncherIconGridSpacing = DesignTokens.SpacingSmall

private val LauncherIconTileRingWidth = 2.dp

/** Settings row showing the current launcher icon; tapping it opens [LauncherIconDrawer]. */
@Composable
fun AppLauncherIconTile(
    launcherAppIcon: LauncherAppIcon,
    onSetLauncherAppIcon: (LauncherAppIcon) -> Unit,
    index: Int,
    count: Int,
) {
    var showDrawer by rememberSaveable { mutableStateOf(false) }
    SegmentedTile(index = index, count = count, onClick = { showDrawer = true }) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = DesignTokens.SpacingXLarge,
                        vertical = DesignTokens.SpacingLarge,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            Text(
                text = stringResource(R.string.settings_launcher_icon_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            LauncherIconImage(
                previewMipmapRes = launcherAppIconToPreviewMipmap(launcherAppIcon),
                size = LauncherIconRowPreviewSize,
            )
            Icon(
                imageVector = MaterialSymbols.ChevronRight,
                contentDescription = stringResource(R.string.desc_navigate_forward),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showDrawer) {
        LauncherIconDrawer(
            selectedIcon = launcherAppIcon,
            onIconSelected = { icon ->
                if (icon != launcherAppIcon) onSetLauncherAppIcon(icon)
            },
            onDismissRequest = { showDrawer = false },
        )
    }
}

/**
 * Bottom drawer of the light and dark launcher icon for each theme color, in an evenly spaced
 * grid; the original icon is restored with the reset button. Closes after a pick, since each
 * change re-registers the launcher entry.
 */
@Composable
internal fun LauncherIconDrawer(
    selectedIcon: LauncherAppIcon,
    onIconSelected: (LauncherAppIcon) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismissRequest) { dismiss ->
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = DesignTokens.ContentHorizontalPadding,
                        end = DesignTokens.ContentHorizontalPadding,
                        bottom = DesignTokens.SpacingLarge,
                    ),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        ) {
            AppDrawerHeader(onClose = dismiss) {
                Text(text = stringResource(R.string.settings_launcher_icon_title))
            }
            // Fits its content; scrolls only when the screen is too short, keeping the reset visible.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(LauncherIconGridSpacing),
            ) {
                themedLauncherIcons().chunked(ICONS_PER_ROW).forEach { rowIcons ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LauncherIconGridSpacing),
                    ) {
                        rowIcons.forEach { icon ->
                            LauncherIconOptionTile(
                                previewMipmapRes = launcherAppIconToPreviewMipmap(icon),
                                selected = icon == selectedIcon,
                                onClick = {
                                    onIconSelected(icon)
                                    dismiss()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(ICONS_PER_ROW - rowIcons.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            TonalButton(
                text = stringResource(R.string.settings_launcher_icon_reset_default),
                icon = MaterialSymbols.RestartAlt,
                onClick = {
                    onIconSelected(LauncherAppIcon.DEFAULT)
                    dismiss()
                },
                enabled = selectedIcon != LauncherAppIcon.DEFAULT,
                modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.SpacingXSmall),
            )
        }
    }
}

@Composable
private fun LauncherIconImage(
    previewMipmapRes: Int,
    size: Dp,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val painter =
        remember(previewMipmapRes, sizePx) {
            val drawable = AppCompatResources.getDrawable(context, previewMipmapRes) ?: return@remember null
            BitmapPainter(drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap())
        }
    if (painter != null) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    } else {
        Spacer(modifier = Modifier.size(size))
    }
}

@Composable
private fun LauncherIconOptionTile(
    previewMipmapRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
    // Same 14% accent tint as unselected choice buttons, so tiles read over wallpaper and AMOLED.
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
                        vertical = DesignTokens.ChipVerticalPadding,
                        horizontal = DesignTokens.SpacingXSmall,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            LauncherIconImage(previewMipmapRes = previewMipmapRes, size = LauncherIconPreviewSize)
        }
    }
}

/** Light then dark icon for each theme color, in the color drawer's order. */
private fun themedLauncherIcons(): List<LauncherAppIcon> =
    (listOf(AppTheme.MONOCHROME) + CUSTOM_APP_THEME_PALETTES).flatMap { themeLauncherIcons(it).toList() }

private fun themeLauncherIcons(theme: AppTheme): Pair<LauncherAppIcon, LauncherAppIcon> =
    when (theme) {
        AppTheme.MONOCHROME -> LauncherAppIcon.MONOCHROME_LIGHT to LauncherAppIcon.MONOCHROME_DARK
        AppTheme.FOREST -> LauncherAppIcon.FOREST_LIGHT to LauncherAppIcon.FOREST_DARK
        AppTheme.AURORA -> LauncherAppIcon.AURORA_LIGHT to LauncherAppIcon.AURORA_DARK
        AppTheme.SUNSET -> LauncherAppIcon.SUNSET_LIGHT to LauncherAppIcon.SUNSET_DARK
        AppTheme.LAVENDER -> LauncherAppIcon.LAVENDER_LIGHT to LauncherAppIcon.LAVENDER_DARK
        AppTheme.TEAL -> LauncherAppIcon.TEAL_LIGHT to LauncherAppIcon.TEAL_DARK
        AppTheme.AMBER -> LauncherAppIcon.AMBER_LIGHT to LauncherAppIcon.AMBER_DARK
        AppTheme.INDIGO -> LauncherAppIcon.INDIGO_LIGHT to LauncherAppIcon.INDIGO_DARK
        AppTheme.SLATE -> LauncherAppIcon.SLATE_LIGHT to LauncherAppIcon.SLATE_DARK
        AppTheme.ROSE -> LauncherAppIcon.ROSE_LIGHT to LauncherAppIcon.ROSE_DARK
        AppTheme.MOCHA -> LauncherAppIcon.MOCHA_LIGHT to LauncherAppIcon.MOCHA_DARK
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
        LauncherAppIcon.LAVENDER_LIGHT -> R.mipmap.ic_launcher_lavender_light
        LauncherAppIcon.LAVENDER_DARK -> R.mipmap.ic_launcher_lavender_dark
        LauncherAppIcon.TEAL_LIGHT -> R.mipmap.ic_launcher_teal_light
        LauncherAppIcon.TEAL_DARK -> R.mipmap.ic_launcher_teal_dark
        LauncherAppIcon.AMBER_LIGHT -> R.mipmap.ic_launcher_amber_light
        LauncherAppIcon.AMBER_DARK -> R.mipmap.ic_launcher_amber_dark
        LauncherAppIcon.INDIGO_LIGHT -> R.mipmap.ic_launcher_indigo_light
        LauncherAppIcon.INDIGO_DARK -> R.mipmap.ic_launcher_indigo_dark
        LauncherAppIcon.SLATE_LIGHT -> R.mipmap.ic_launcher_slate_light
        LauncherAppIcon.SLATE_DARK -> R.mipmap.ic_launcher_slate_dark
        LauncherAppIcon.ROSE_LIGHT -> R.mipmap.ic_launcher_rose_light
        LauncherAppIcon.ROSE_DARK -> R.mipmap.ic_launcher_rose_dark
        LauncherAppIcon.MOCHA_LIGHT -> R.mipmap.ic_launcher_mocha_light
        LauncherAppIcon.MOCHA_DARK -> R.mipmap.ic_launcher_mocha_dark
    }
