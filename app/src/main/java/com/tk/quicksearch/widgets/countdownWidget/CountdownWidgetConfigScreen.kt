package com.tk.quicksearch.widgets.countdownWidget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetBackgroundSection
import com.tk.quicksearch.widgets.noteWidget.WidgetTextColorSection
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.rememberWidgetPreviewWallpaper
import com.tk.quicksearch.widgets.widgetConfigScreen.components.SliderRow
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSlidersSection
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private const val MILLIS_PER_DAY = 86_400_000L

/** Settings for a Countdown widget: its date, title, display format and look. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownWidgetConfigScreen(
    config: CountdownWidgetConfig,
    onConfigChange: (CountdownWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    // A new widget has no date yet, so it opens straight to the date picker.
    var showDatePicker by rememberSaveable { mutableStateOf(config.targetEpochDay == null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.widget_countdown_widget_title),
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
                        enabled = config.targetEpochDay != null,
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
                CountdownWidgetPreview(config = config, wallpaperBitmap = rememberWidgetPreviewWallpaper())
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
                CountdownDateSection(
                    targetEpochDay = config.targetEpochDay,
                    onClick = { showDatePicker = true },
                )
                OutlinedTextField(
                    value = config.title,
                    onValueChange = { onConfigChange(config.copy(title = it.take(CountdownWidgetDefaults.TITLE_MAX_LENGTH))) },
                    label = { Text(stringResource(R.string.notes_title_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                CountdownFormatSection(config = config, onConfigChange = onConfigChange)
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
                        value = config.textSizeSp,
                        valueRange = CountdownWidgetDefaults.TEXT_SIZE_MIN_SP..CountdownWidgetDefaults.TEXT_SIZE_MAX_SP,
                        steps = ((CountdownWidgetDefaults.TEXT_SIZE_MAX_SP - CountdownWidgetDefaults.TEXT_SIZE_MIN_SP) / 2).toInt(),
                        valueFormatter = { "${it.roundToInt()} sp" },
                        showTicks = false,
                        onValueChange = { onConfigChange(config.copy(textSizeSp = it)) },
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

    if (showDatePicker) {
        CountdownDatePickerDialog(
            targetEpochDay = config.targetEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = { epochDay ->
                showDatePicker = false
                if (epochDay != config.targetEpochDay) {
                    val isFuture = epochDay > LocalDate.now(ZoneId.systemDefault()).toEpochDay()
                    onConfigChange(
                        config.copy(
                            targetEpochDay = epochDay,
                            // Progress counts from when the date is set.
                            startMillis = System.currentTimeMillis(),
                            format =
                                if (!isFuture && config.format == CountdownFormat.PROGRESS) {
                                    CountdownFormat.DAYS
                                } else {
                                    config.format
                                },
                        ),
                    )
                }
            },
        )
    }
}

@Composable
private fun CountdownDateSection(
    targetEpochDay: Long?,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(
            text = stringResource(R.string.widget_countdown_date),
            style = MaterialTheme.typography.titleSmall,
        )
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onClick)
                        .padding(
                            horizontal = DesignTokens.CardHorizontalPadding,
                            vertical = DesignTokens.CardVerticalPadding,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
            ) {
                Icon(
                    imageVector = MaterialSymbols.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(DesignTokens.IconSizeSmall),
                )
                Text(
                    text =
                        targetEpochDay?.let {
                            LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                        } ?: stringResource(R.string.calendar_create_event_select_date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MaterialSymbols.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Material date picker; any past or future date can be chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownDatePickerDialog(
    targetEpochDay: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    // The picker works in UTC midnights, which map one-to-one onto epoch days.
    val initialEpochDay = targetEpochDay ?: LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpochDay * MILLIS_PER_DAY)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(Math.floorDiv(it, MILLIS_PER_DAY)) } ?: onDismiss() },
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountdownFormatSection(
    config: CountdownWidgetConfig,
    onConfigChange: (CountdownWidgetConfig) -> Unit,
) {
    val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    // Progress needs a date still ahead to count toward.
    val isFuture = config.targetEpochDay?.let { it > today } ?: false
    val formats = CountdownFormat.entries.filter { it != CountdownFormat.PROGRESS || isFuture }

    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(
            text = stringResource(R.string.settings_shortcut_display),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)) {
            formats.forEach { format ->
                FilterChip(
                    selected = config.format == format,
                    onClick = { onConfigChange(config.copy(format = format)) },
                    label = { Text(stringResource(format.labelResId)) },
                )
            }
        }
        if (config.format == CountdownFormat.PROGRESS) {
            Text(
                text = stringResource(R.string.widget_countdown_progress_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val CountdownFormat.labelResId: Int
    get() =
        when (this) {
            CountdownFormat.DAYS -> R.string.widget_countdown_format_days
            CountdownFormat.WEEKS -> R.string.widget_countdown_format_weeks
            CountdownFormat.MONTHS -> R.string.widget_countdown_format_months
            CountdownFormat.YEARS -> R.string.widget_countdown_format_years
            CountdownFormat.DETAILED -> R.string.widget_countdown_format_detailed
            CountdownFormat.PROGRESS -> R.string.widget_countdown_format_progress
        }
