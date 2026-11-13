package com.tk.quicksearch.widgets.worldClockWidget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetBackgroundSection
import com.tk.quicksearch.widgets.noteWidget.WidgetTextColorSection
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.rememberWidgetPreviewWallpaper
import com.tk.quicksearch.widgets.widgetConfigScreen.components.SliderRow
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSlidersSection
import kotlin.math.roundToInt

/** Settings for a World Clock widget: its one to four locations and its look. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockWidgetConfigScreen(
    config: WorldClockWidgetConfig,
    widgetSize: DpSize?,
    onConfigChange: (WorldClockWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    // A new widget has no locations yet, so it opens straight to the search.
    var showSearch by rememberSaveable { mutableStateOf(config.locations.isEmpty()) }
    // The largest time the preview fits; the font size slider ends there so every step shows.
    var maxTimeSize by remember { mutableFloatStateOf(WorldClockWidgetDefaults.TEXT_SIZE_MAX_SP) }
    val sliderMax =
        maxTimeSize.coerceIn(WorldClockWidgetDefaults.TEXT_SIZE_MIN_SP + 1f, WorldClockWidgetDefaults.TEXT_SIZE_MAX_SP)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.widget_world_clock_widget_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = MaterialSymbols.Close,
                            contentDescription = stringResource(R.string.dialog_cancel),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = WidgetConfigConstants.SURFACE_ELEVATION) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = WidgetConfigConstants.BOTTOM_BAR_HORIZONTAL_PADDING,
                                end = WidgetConfigConstants.BOTTOM_BAR_HORIZONTAL_PADDING,
                                top = WidgetConfigConstants.BOTTOM_BAR_VERTICAL_PADDING,
                                bottom = WidgetConfigConstants.BOTTOM_BAR_BOTTOM_PADDING,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = onSave,
                        enabled = config.locations.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(WidgetConfigConstants.BOTTOM_BUTTON_HEIGHT),
                    ) {
                        Text(text = stringResource(R.string.dialog_save), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
            Box(modifier = Modifier.padding(horizontal = WidgetConfigConstants.HORIZONTAL_PADDING)) {
                WorldClockWidgetPreview(
                    config = config,
                    wallpaperBitmap = rememberWidgetPreviewWallpaper(),
                    widgetSize = widgetSize,
                    onMaxTimeSizeChange = { maxTimeSize = it },
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = WidgetConfigConstants.HORIZONTAL_PADDING,
                            top = WidgetConfigConstants.SECTION_SPACING,
                            end = WidgetConfigConstants.HORIZONTAL_PADDING,
                            bottom = WidgetConfigConstants.SCROLLABLE_SECTION_BOTTOM_PADDING,
                        ),
                verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING),
            ) {
                WorldClockLocationsSection(
                    locations = config.locations,
                    canAdd = config.canAddLocation,
                    onAddClick = { showSearch = true },
                    onLocationsChange = { onConfigChange(config.copy(locations = it)) },
                )
                NoteWidgetBackgroundSection(
                    state = config.style,
                    onStateChange = { onConfigChange(config.copy(style = it)) },
                )
                WidgetTextColorSection(
                    style = config.style,
                    onStyleChange = { onConfigChange(config.copy(style = it)) },
                )
                Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SLIDER_ROW_SPACING)) {
                    SliderRow(
                        label = stringResource(R.string.settings_font_size_title),
                        value = config.textSizeSp.coerceIn(WorldClockWidgetDefaults.TEXT_SIZE_MIN_SP, sliderMax),
                        valueRange = WorldClockWidgetDefaults.TEXT_SIZE_MIN_SP..sliderMax,
                        // The end moves with the widget's size, so the value is rounded instead.
                        steps = 0,
                        valueFormatter = { "${it.roundToInt()} sp" },
                        showTicks = false,
                        onValueChange = { onConfigChange(config.copy(textSizeSp = it.roundToInt().toFloat())) },
                    )
                    WidgetSlidersSection(
                        state = config.style,
                        showBorderControls = false,
                        onStateChange = { onConfigChange(config.copy(style = it)) },
                    )
                }
            }
        }
    }

    if (showSearch) {
        WorldClockLocationSearchDialog(
            selectedZoneIds = config.locations.map { it.zoneId }.toSet(),
            onDismiss = { showSearch = false },
            onSelect = { zone, city ->
                showSearch = false
                if (config.canAddLocation) {
                    val location = WorldClockLocation(zoneId = zone.zoneId, label = city.orEmpty())
                    onConfigChange(config.copy(locations = config.locations + location))
                }
            },
        )
    }
}
