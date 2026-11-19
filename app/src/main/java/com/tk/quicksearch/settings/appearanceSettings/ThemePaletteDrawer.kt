package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.CUSTOM_APP_THEME_PALETTES
import com.tk.quicksearch.shared.ui.theme.AppThemeColors
import com.tk.quicksearch.shared.ui.components.AppBottomSheet
import com.tk.quicksearch.shared.ui.components.AppDrawerHeader
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.ThemeColorRegistry
import com.tk.quicksearch.shared.ui.theme.AppShapes

private const val PALETTES_PER_ROW = 5
private const val MAX_HEIGHT_FRACTION = 0.3f
private val ScrollbarGutter = DesignTokens.SpacingMedium
private val ScrollbarThumbWidth = DesignTokens.SpacingXSmall
private val ScrollbarMinThumbHeight = DesignTokens.SpacingXXLarge

/**
 * Bottom drawer of preset palettes for the Colorful theme; [selectedTheme] is highlighted. It stays
 * open after a pick, without dimming the screen behind, so palettes can be compared live.
 */
@Composable
internal fun ThemePaletteDrawer(
    selectedTheme: AppTheme?,
    isDarkMode: Boolean,
    intensity: Float,
    onPaletteSelected: (AppTheme) -> Unit,
    onDismissRequest: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismissRequest, scrimColor = Color.Transparent) { dismiss ->
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
                Text(text = stringResource(R.string.settings_app_theme_custom_dialog_title))
            }
            // At most 30% of the screen, so the app behind stays visible while palettes are tried.
            val maxGridHeight = LocalConfiguration.current.screenHeightDp.dp * MAX_HEIGHT_FRACTION
            val scrollState = rememberScrollState()
            val thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.ContentMutedAlpha)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxGridHeight)
                        .verticalScrollbar(scrollState, thumbColor)
                        .verticalScroll(scrollState)
                        .padding(end = ScrollbarGutter),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                CUSTOM_APP_THEME_PALETTES.chunked(PALETTES_PER_ROW).forEach { rowPalettes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                    ) {
                        rowPalettes.forEach { palette ->
                            PaletteSwatch(
                                palette = palette,
                                isSelected = palette == selectedTheme,
                                isDarkMode = isDarkMode,
                                intensity = intensity,
                                onClick = { onPaletteSelected(palette) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(PALETTES_PER_ROW - rowPalettes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: AppTheme,
    isSelected: Boolean,
    isDarkMode: Boolean,
    intensity: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = ThemeColorRegistry.accent(palette)
    val accentColor = if (isDarkMode) accent.darkPrimary else accent.lightPrimary
    val onAccentColor = if (isDarkMode) accent.darkOnPrimary else accent.lightOnPrimary
    Column(
        modifier =
            modifier.selectable(
                selected = isSelected,
                // No ripple: the palette applies live, which is feedback enough.
                interactionSource = null,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(AppShapes.full)
                    .background(
                        Brush.linearGradient(
                            AppThemeColors(theme = palette, isDarkMode = isDarkMode, intensity = intensity),
                        ),
                    ).border(
                        width = if (isSelected) 2.dp else DesignTokens.BorderWidth,
                        color = if (isSelected) accentColor else AppColors.SettingsDivider,
                        shape = AppShapes.full,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            // The accent color the palette applies to buttons, toggles and highlights.
            Box(
                modifier = Modifier.size(DesignTokens.IconSizeSmall).clip(AppShapes.full).background(accentColor),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = MaterialSymbols.Check,
                        contentDescription = null,
                        tint = onAccentColor,
                        modifier = Modifier.size(DesignTokens.EditBadgeIconSize),
                    )
                }
            }
        }
        Text(
            text = stringResource(customPaletteLabelRes(palette)),
            style = MaterialTheme.typography.labelSmall,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun customPaletteLabelRes(palette: AppTheme): Int =
    when (palette) {
        AppTheme.SUNSET -> R.string.settings_app_theme_sunset
        AppTheme.AMBER -> R.string.settings_app_theme_amber
        AppTheme.FOREST -> R.string.settings_app_theme_forest
        AppTheme.TEAL -> R.string.settings_app_theme_teal
        AppTheme.AURORA -> R.string.settings_app_theme_aurora
        AppTheme.INDIGO -> R.string.settings_app_theme_indigo
        AppTheme.LAVENDER -> R.string.settings_app_theme_lavender
        AppTheme.SLATE -> R.string.settings_app_theme_slate
        AppTheme.ROSE -> R.string.settings_app_theme_rose
        AppTheme.MOCHA -> R.string.settings_app_theme_mocha
        AppTheme.MONOCHROME -> R.string.settings_app_theme_monochrome
    }

/**
 * Preview gradient for the Default / Colorful theme tiles. The Colorful tile shows the chosen
 * palette, or the Default gradient until one is chosen (with a palette icon on top).
 */
internal fun themeOptionPreviewColors(
    isCustom: Boolean,
    selectedTheme: AppTheme,
    isDarkMode: Boolean,
    intensity: Float,
    amoledPreviewActive: Boolean,
): List<Color> =
    when {
        isCustom && selectedTheme != AppTheme.MONOCHROME ->
            AppThemeColors(theme = selectedTheme, isDarkMode = isDarkMode, intensity = intensity)
        amoledPreviewActive -> listOf(Color.Black, Color.Black)
        else -> AppThemeColors(theme = AppTheme.MONOCHROME, isDarkMode = isDarkMode, intensity = intensity)
    }

/** Always-visible scrollbar thumb at the end edge while the content overflows. */
private fun Modifier.verticalScrollbar(
    state: ScrollState,
    color: Color,
): Modifier =
    drawWithContent {
        drawContent()
        val maxScroll = state.maxValue
        if (maxScroll <= 0 || maxScroll == Int.MAX_VALUE) return@drawWithContent
        val viewport = size.height
        val thumbWidth = ScrollbarThumbWidth.toPx()
        val thumbHeight =
            (viewport * viewport / (viewport + maxScroll)).coerceAtLeast(ScrollbarMinThumbHeight.toPx())
        val thumbTop = (viewport - thumbHeight) * state.value / maxScroll
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - thumbWidth, thumbTop),
            size = Size(thumbWidth, thumbHeight),
            cornerRadius = CornerRadius(thumbWidth / 2),
        )
    }
