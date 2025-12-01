package com.tk.quicksearch.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onApply,
                        enabled = isLoaded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_save),
                            style = MaterialTheme.typography.titleMedium
                        )
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
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WidgetPreviewCard(state = state)
            }

            // Scrollable preferences section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                WidgetBackgroundColorSection(state = state, onStateChange = onStateChange)
                
                WidgetSlidersSection(state = state, onStateChange = onStateChange)

                WidgetToggleSection(state = state, onStateChange = onStateChange)
            }
        }
    }
}

@Composable
private fun WidgetPreviewCard(state: QuickSearchWidgetPreferences) {
    val borderShape = RoundedCornerShape(state.borderRadiusDp.dp)
    val borderColor = Color(state.borderColor).copy(alpha = state.backgroundAlpha)
    val backgroundColor = if (state.backgroundColorIsWhite) Color.White else Color.Black
    val backgroundWithAlpha = backgroundColor.copy(alpha = state.backgroundAlpha)
    
    // Determine text and icon color based on background and transparency
    // Text and icon should remain fully opaque (no transparency)
    val baseBorderColor = Color(state.borderColor)
    val textIconColor = if (state.backgroundAlpha > 0.6f && state.backgroundColorIsWhite) {
        Color(0xFF424242) // Dark grey
    } else {
        baseBorderColor // Fully opaque border color
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(backgroundWithAlpha, shape = borderShape)
                    .then(
                        if (state.borderWidthDp >= 0.05f) {
                            Modifier.border(
                                width = state.borderWidthDp.dp,
                                color = borderColor,
                                shape = borderShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_widget_search),
                    contentDescription = stringResource(R.string.desc_search_icon),
                    tint = textIconColor,
                    modifier = Modifier.size(20.dp)
                )
                if (state.showLabel) {
                    Text(
                        text = stringResource(R.string.widget_label_text),
                        modifier = Modifier.padding(start = 8.dp),
                        color = textIconColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetSlidersSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SliderRow(
            label = stringResource(R.string.widget_slider_radius),
            value = state.borderRadiusDp,
            valueRange = 0f..30f,
            valueFormatter = { "${it.roundToInt()} dp" },
            onValueChange = { onStateChange(state.copy(borderRadiusDp = it)) }
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_border),
            value = state.borderWidthDp,
            valueRange = 0f..4f,
            valueFormatter = { if (it == 0f) "0 dp" else String.format(Locale.US, "%.1f dp", it) },
            onValueChange = { onStateChange(state.copy(borderWidthDp = it)) }
        )
        SliderRow(
            label = stringResource(R.string.widget_slider_transparency),
            value = state.backgroundAlpha,
            valueRange = 0f..1f,
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
private fun WidgetBackgroundColorSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = stringResource(R.string.widget_background_color))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = state.backgroundColorIsWhite,
                onClick = { onStateChange(state.copy(backgroundColorIsWhite = true)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = {}
            ) {
                Text(stringResource(R.string.widget_background_white))
            }
            SegmentedButton(
                selected = !state.backgroundColorIsWhite,
                onClick = { onStateChange(state.copy(backgroundColorIsWhite = false)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = {}
            ) {
                Text(stringResource(R.string.widget_background_black))
            }
        }
    }
}

@Composable
private fun WidgetToggleSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
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

