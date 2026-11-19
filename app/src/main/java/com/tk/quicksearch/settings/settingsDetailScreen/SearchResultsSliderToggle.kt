package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.components.SliderWithValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import com.tk.quicksearch.shared.ui.components.AppSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.settings.shared.SettingsToggleSliderDetails
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Keeps the switch beside its label and gives the count slider the tile's full width. */
@Composable
internal fun SearchResultsSliderToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: ImageVector,
    sliderDetails: SettingsToggleSliderDetails?,
    subtitle: String? = null,
) {
    Column {
        SettingsToggleRow(
            title = title,
            subtitle = subtitle,
            checked = checked,
            onCheckedChange = onCheckedChange,
            leadingIcon = leadingIcon,
        )
        if (sliderDetails != null) {
            SliderWithValue(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingXLarge)
                    .padding(bottom = DesignTokens.SpacingLarge),
                valueSpacing = DesignTokens.SpacingLarge,
                valueLabel = {
                    Text(
                        text = sliderDetails.valueLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) { sliderModifier ->
                AppSlider(
                    value = sliderDetails.value,
                    onValueChange = sliderDetails.onValueChange,
                    valueRange = sliderDetails.valueRange,
                    steps = sliderDetails.steps,
                    modifier = sliderModifier.semantics { contentDescription = title },
                )
            }
        }
    }
}
