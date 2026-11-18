package com.tk.quicksearch.settings.appearanceSettings

import com.tk.quicksearch.shared.ui.components.SliderWithValue
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.HomeTextColor
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.util.hapticToggle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun FontSizeCard(
        fontScaleMultiplier: Float,
        onFontScaleMultiplierChange: (Float) -> Unit,
        useSystemFont: Boolean,
        onUseSystemFontChange: (Boolean) -> Unit,
        homeTextColorOverride: HomeTextColor?,
        onHomeTextColorChange: (HomeTextColor) -> Unit,
        backgroundSource: BackgroundSource,
        customImageUri: String?,
        compactLayoutEnabled: Boolean = false,
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    SegmentedGroup(modifier = modifier) {
        SegmentedTile(index = 0, count = 3) {
            SettingsToggleRow(
                    title = stringResource(R.string.settings_use_system_font_title),
                    subtitle = stringResource(R.string.settings_use_system_font_desc),
                    checked = useSystemFont,
                    onCheckedChange = onUseSystemFontChange,
                    leadingIcon = MaterialSymbols.TextFields,
                    showDivider = false,
            )
        }
        SegmentedTile(index = 1, count = 3) {
            SegmentedTileContent {
                FontSizeSlider(
                        fontScaleMultiplier = fontScaleMultiplier,
                        onFontScaleMultiplierChange = onFontScaleMultiplierChange,
                )
            }
        }
        SegmentedTile(index = 2, count = 3) {
            SegmentedTileContent {
                HomeTextColorOptions(
                        selectedColor = homeTextColorOverride,
                        onColorSelected = onHomeTextColorChange,
                        backgroundSource = backgroundSource,
                        customImageUri = customImageUri,
                        compactLayoutEnabled = compactLayoutEnabled,
                )
            }
        }
    }
}

internal val FONT_SIZE_STEPS =
        listOf(
                FontSizeStep(0.90f, R.string.settings_font_size_extra_small),
                FontSizeStep(0.95f, R.string.settings_font_size_small),
                FontSizeStep(1.00f, R.string.settings_font_size_medium),
                FontSizeStep(1.05f, R.string.settings_font_size_big),
                FontSizeStep(1.10f, R.string.settings_font_size_extra_big),
        )

internal data class FontSizeStep(val scale: Float, val labelRes: Int)

@Composable
private fun FontSizeSlider(
        fontScaleMultiplier: Float,
        onFontScaleMultiplierChange: (Float) -> Unit,
) {
    val view = LocalView.current
    val committedIndex =
            FONT_SIZE_STEPS.indices.minByOrNull { abs(FONT_SIZE_STEPS[it].scale - fontScaleMultiplier) } ?: 2
    var sliderValue by remember(committedIndex) { mutableFloatStateOf(committedIndex.toFloat()) }
    val activeIndex = sliderValue.roundToInt().coerceIn(0, FONT_SIZE_STEPS.lastIndex)

    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpacingXSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                    text = stringResource(R.string.settings_font_size_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                    text = stringResource(FONT_SIZE_STEPS[activeIndex].labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.Accent,
            )
        }

        SliderWithValue(
            modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.SpacingXSmall),
            valueSpacing = DesignTokens.SpacingMedium,
            valueLabel = {
                Text(
                        text = "A",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                )
            },
            leadingLabel = {
                Text(
                        text = "A",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                )
            },
        ) { sliderModifier ->
            Slider(
                    value = sliderValue,
                    onValueChange = { value ->
                        val index = value.roundToInt().coerceIn(0, FONT_SIZE_STEPS.lastIndex)
                        if (index != activeIndex) hapticToggle(view)()
                        sliderValue = value
                    },
                    // Apply the scale only on release: changing it mid-drag changes density and cancels the drag.
                    // On a tap the slider calls this right after onValueChange, before recomposition, so read
                    // the live slider value instead of the composition-captured activeIndex.
                    onValueChangeFinished = {
                        val releasedIndex = sliderValue.roundToInt().coerceIn(0, FONT_SIZE_STEPS.lastIndex)
                        if (releasedIndex != committedIndex) {
                            onFontScaleMultiplierChange(FONT_SIZE_STEPS[releasedIndex].scale)
                        }
                    },
                    valueRange = 0f..FONT_SIZE_STEPS.lastIndex.toFloat(),
                    steps = FONT_SIZE_STEPS.size - 2,
                    modifier = sliderModifier,
            )
        }
    }
}
