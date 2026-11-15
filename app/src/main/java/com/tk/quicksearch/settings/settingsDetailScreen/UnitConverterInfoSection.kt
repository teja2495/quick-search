package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.R

private val unitCategories = listOf(
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_length,
        examples = listOf("5 miles in km", "100 cm in inches", "6 ft in metres"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_weight_mass,
        examples = listOf("5 lbs in kg", "100 grams in oz", "2 stone in kg"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_temperature,
        examples = listOf("100F in C", "37 celsius in fahrenheit", "300K in C"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_area,
        examples = listOf("100 sqft in sqm", "2 acres in hectares", "1 km2 in miles2"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_volume,
        examples = listOf("1 gallon in litres", "500 ml in cups", "2 tbsp in tsp"),
    ),
    ToolExampleCategory(
        nameRes = R.string.common_time,
        examples = listOf("2 hours in minutes", "90 mins in hours", "1 week in days"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_speed,
        examples = listOf("60 mph in km/h", "100 kph in mph", "30 m/s in km/h"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_data,
        examples = listOf("1 GB in MB", "500 MB in bytes", "2 TB in GB"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_energy,
        examples = listOf("100 kcal in kJ", "1000 BTU in kWh", "5 kWh in J"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_power,
        examples = listOf("100 W in hp", "5 kW in watts", "1 hp in kW"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_pressure,
        examples = listOf("14.7 psi in atm", "1 bar in psi", "101 kPa in bar"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_angle,
        examples = listOf("180 degrees in rad", "1 rev in degrees", "90 deg in grad"),
    ),
    ToolExampleCategory(
        nameRes = R.string.unit_converter_category_frequency,
        examples = listOf("2.4 GHz in MHz", "1000 Hz in kHz", "60 rpm in Hz"),
    ),
)

@Composable
internal fun UnitConverterInfoSection(modifier: Modifier = Modifier) {
    ToolExamplesSection(categories = unitCategories, modifier = modifier)
}
