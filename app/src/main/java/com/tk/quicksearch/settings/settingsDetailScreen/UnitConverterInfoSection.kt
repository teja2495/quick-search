package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppPill
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private data class UnitCategory(@StringRes val nameRes: Int, val examples: List<String>)

private val unitCategories = listOf(
    UnitCategory(
        nameRes = R.string.unit_converter_category_length,
        examples = listOf("5 miles in km", "100 cm in inches", "6 ft in metres"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_weight_mass,
        examples = listOf("5 lbs in kg", "100 grams in oz", "2 stone in kg"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_temperature,
        examples = listOf("100F in C", "37 celsius in fahrenheit", "300K in C"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_area,
        examples = listOf("100 sqft in sqm", "2 acres in hectares", "1 km2 in miles2"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_volume,
        examples = listOf("1 gallon in litres", "500 ml in cups", "2 tbsp in tsp"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_time,
        examples = listOf("2 hours in minutes", "90 mins in hours", "1 week in days"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_speed,
        examples = listOf("60 mph in km/h", "100 kph in mph", "30 m/s in km/h"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_data,
        examples = listOf("1 GB in MB", "500 MB in bytes", "2 TB in GB"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_energy,
        examples = listOf("100 kcal in kJ", "1000 BTU in kWh", "5 kWh in J"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_power,
        examples = listOf("100 W in hp", "5 kW in watts", "1 hp in kW"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_pressure,
        examples = listOf("14.7 psi in atm", "1 bar in psi", "101 kPa in bar"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_angle,
        examples = listOf("180 degrees in rad", "1 rev in degrees", "90 deg in grad"),
    ),
    UnitCategory(
        nameRes = R.string.unit_converter_category_frequency,
        examples = listOf("2.4 GHz in MHz", "1000 Hz in kHz", "60 rpm in Hz"),
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UnitConverterInfoSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        Text(
            text = stringResource(R.string.settings_tools_info_examples_header),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                horizontal = DesignTokens.SpacingXSmall,
                vertical = DesignTokens.SpacingXSmall,
            ),
        )

        unitCategories.forEach { category ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = AppColors.getCardColors(showWallpaperBackground = false),
                elevation = AppColors.getCardElevation(false),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = DesignTokens.CardHorizontalPadding,
                        vertical = DesignTokens.CardVerticalPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(category.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        category.examples.forEach { example ->
                            AppPill(text = example)
                        }
                    }
                }
            }
        }
    }
}
