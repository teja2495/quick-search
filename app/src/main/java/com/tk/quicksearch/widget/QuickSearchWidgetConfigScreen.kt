package com.tk.quicksearch.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tk.quicksearch.R
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSearchWidgetConfigScreen(
    state: QuickSearchWidgetPreferences,
    isLoaded: Boolean,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.widget_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.widget_action_cancel)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = onCancel
                    ) {
                        Text(text = stringResource(R.string.widget_action_cancel))
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = onApply,
                        enabled = isLoaded
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(text = stringResource(R.string.widget_action_save))
                    }
                }
            }
        }
    ) { innerPadding ->
        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.widget_loading_state))
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fixed preview section at the top
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.widget_preview_title),
                    style = MaterialTheme.typography.titleMedium
                )
                WidgetPreviewCard(state = state)
            }

            // Scrollable preferences section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.widget_section_colors),
                    style = MaterialTheme.typography.titleSmall
                )

                WidgetPresetSection(
                    title = stringResource(R.string.widget_section_light_wallpapers),
                    presets = WidgetColorPresets.light,
                    selectedId = state.presetId,
                    onSelect = { preset -> onStateChange(state.withPreset(preset)) }
                )

                WidgetPresetSection(
                    title = stringResource(R.string.widget_section_dark_wallpapers),
                    presets = WidgetColorPresets.dark,
                    selectedId = state.presetId,
                    onSelect = { preset -> onStateChange(state.withPreset(preset)) }
                )

                WidgetSlidersSection(state = state, onStateChange = onStateChange)

                WidgetToggleSection(state = state, onStateChange = onStateChange)
            }
        }
    }
}

@Composable
private fun WidgetPreviewCard(state: QuickSearchWidgetPreferences) {
    val shape = RoundedCornerShape(state.borderRadiusDp.dp)
    val borderColor = Color(state.borderColor)
    val background = Color(state.backgroundColor).copy(alpha = state.backgroundAlpha)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            var chipModifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(background, shape)

            if (state.borderWidthDp > 0f) {
                chipModifier = chipModifier.border(
                    width = state.borderWidthDp.dp,
                    color = borderColor,
                    shape = shape
                )
            }

            Box(
                modifier = chipModifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_widget_search),
                    contentDescription = stringResource(R.string.desc_search_icon),
                    tint = borderColor,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.CenterStart)
                )
                if (state.showLabel) {
                    Text(
                        text = stringResource(R.string.widget_label_text),
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        color = borderColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPresetSection(
    title: String,
    presets: List<WidgetColorPreset>,
    selectedId: String,
    onSelect: (WidgetColorPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            presets.forEach { preset ->
                WidgetPresetChip(
                    preset = preset,
                    isSelected = preset.id == selectedId,
                    onSelect = { onSelect(preset) }
                )
            }
        }
    }
}

@Composable
private fun WidgetPresetChip(
    preset: WidgetColorPreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        color = Color(preset.backgroundArgb),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(preset.borderArgb)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color(preset.borderArgb), CircleShape)
            )
            Text(
                text = stringResource(id = preset.labelRes),
                color = Color(preset.borderArgb),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun WidgetSlidersSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.widget_section_style),
            style = MaterialTheme.typography.titleSmall
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_radius),
            value = state.borderRadiusDp,
            valueRange = 0f..48f,
            valueFormatter = { "${it.roundToInt()} dp" },
            onValueChange = { onStateChange(state.copy(borderRadiusDp = it)) }
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_border),
            value = state.borderWidthDp,
            valueRange = 0f..4f,
            valueFormatter = { String.format(Locale.US, "%.1f dp", it) },
            onValueChange = { onStateChange(state.copy(borderWidthDp = it)) }
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_transparency),
            value = state.backgroundAlpha,
            valueRange = 0.2f..1f,
            valueFormatter = { "${(it * 100).roundToInt()}%" },
            onValueChange = { onStateChange(state.copy(backgroundAlpha = it)) }
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueFormatter: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label)
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun WidgetToggleSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.widget_section_text),
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.widget_toggle_show_label))
            Switch(
                checked = state.showLabel,
                onCheckedChange = { onStateChange(state.copy(showLabel = it)) }
            )
        }
    }
}

