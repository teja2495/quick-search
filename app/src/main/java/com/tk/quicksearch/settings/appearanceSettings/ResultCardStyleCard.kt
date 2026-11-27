package com.tk.quicksearch.settings.appearanceSettings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.ResultCardStyle
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.components.ExpandableSegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import java.util.Locale
import kotlin.math.roundToInt

private const val CardCornerRadiusStepDp = 2f
private const val CardCornerRadiusSteps =
    ((ResultCardStyle.MAX_CORNER_RADIUS_DP - ResultCardStyle.MIN_CORNER_RADIUS_DP) / CardCornerRadiusStepDp).toInt() - 1
private const val CardTransparencySteps = 9

/**
 * Corner radius, background transparency and wallpaper blur of search result and home cards (pinned items, At a
 * Glance, widgets and other result cards). Follows the expandable segmented group pattern of the
 * search bar card.
 */
@Composable
fun ResultCardStyleCard(
    resultCardStyle: ResultCardStyle,
    onSetResultCardStyle: (ResultCardStyle) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    // Sliders hold their value locally and persist once the drag settles.
    var cornerRadiusDp by remember(resultCardStyle.cornerRadiusDp) {
        mutableFloatStateOf(resultCardStyle.cornerRadiusDp)
    }
    var transparency by remember(resultCardStyle.transparency) {
        mutableFloatStateOf(resultCardStyle.transparency)
    }
    val currentStyle = resultCardStyle.copy(cornerRadiusDp = cornerRadiusDp, transparency = transparency)

    ExpandableSegmentedGroup(
        title = stringResource(R.string.settings_result_card_style_title),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        tileCount = 3,
        modifier = modifier,
        firstTileContent = {
            SliderSetting(
                label = stringResource(R.string.widget_slider_radius),
                value = cornerRadiusDp,
                valueRange = ResultCardStyle.MIN_CORNER_RADIUS_DP..ResultCardStyle.MAX_CORNER_RADIUS_DP,
                steps = CardCornerRadiusSteps,
                valueText = String.format(Locale.getDefault(), "%.0f dp", cornerRadiusDp),
                onValueChange = { value ->
                    val snapped = (value / CardCornerRadiusStepDp).roundToInt() * CardCornerRadiusStepDp
                    if (snapped != cornerRadiusDp) {
                        hapticToggle(view)()
                        cornerRadiusDp = snapped
                    }
                },
                onValueChangeFinished = {
                    onSetResultCardStyle(resultCardStyle.copy(cornerRadiusDp = cornerRadiusDp))
                },
            )

            SliderSetting(
                label = stringResource(R.string.settings_wallpaper_transparency_label),
                value = transparency,
                valueRange = 0f..1f,
                steps = CardTransparencySteps,
                valueText = "${(transparency * 100).roundToInt()}%",
                onValueChange = { value ->
                    val snapped =
                        (value * (CardTransparencySteps + 1)).roundToInt() / (CardTransparencySteps + 1).toFloat()
                    if (snapped != transparency) {
                        hapticToggle(view)()
                        transparency = snapped
                    }
                },
                onValueChangeFinished = {
                    onSetResultCardStyle(resultCardStyle.copy(transparency = transparency))
                },
            )
        },
    ) {
        SegmentedTile(index = 1, count = 3) {
            SettingsToggleRow(
                title = stringResource(R.string.settings_search_chrome_blur_title),
                subtitle = stringResource(R.string.settings_result_card_blur_desc),
                checked = resultCardStyle.blurEnabled,
                onCheckedChange = { enabled -> onSetResultCardStyle(currentStyle.copy(blurEnabled = enabled)) },
                showDivider = false,
            )
        }
        SegmentedTile(index = 2, count = 3) {
            TonalButton(
                text = stringResource(R.string.settings_launcher_icon_reset_default),
                icon = MaterialSymbols.RestartAlt,
                onClick = { onSetResultCardStyle(ResultCardStyle()) },
                enabled = !currentStyle.isDefault,
                modifier = Modifier.fillMaxWidth().padding(DesignTokens.SpacingLarge),
            )
        }
    }
}
