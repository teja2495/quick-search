package com.tk.quicksearch.widgets.countdownWidget

import com.tk.quicksearch.shared.ui.components.AppDatePickerDialog
import com.tk.quicksearch.shared.ui.components.AppFilterChip
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetConfigScaffold
import com.tk.quicksearch.widgets.noteWidget.NoteWidgetBackgroundSection
import com.tk.quicksearch.widgets.noteWidget.WidgetTextColorSection
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.rememberWidgetPreviewWallpaper
import com.tk.quicksearch.widgets.widgetConfigScreen.components.SliderRow
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSettingTile
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetSlidersSection
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetTileTextInset
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import com.tk.quicksearch.shared.ui.components.TonalIconBadge
import com.tk.quicksearch.shared.ui.components.AppTextButton

private const val MILLIS_PER_DAY = 86_400_000L

/** Settings for a Countdown widget: its date, title, display format and look. */
@Composable
fun CountdownWidgetConfigScreen(
    config: CountdownWidgetConfig,
    onConfigChange: (CountdownWidgetConfig) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    // A new widget has no date yet, so it opens straight to the date picker.
    var showDatePicker by rememberSaveable { mutableStateOf(config.targetEpochDay == null) }

    WidgetConfigScaffold(
        title = stringResource(R.string.widget_countdown_widget_title),
        onCancel = onCancel,
        onSave = onSave,
        saveEnabled = config.targetEpochDay != null,
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
                SegmentedGroup {
                    CountdownDateSection(
                        targetEpochDay = config.targetEpochDay,
                        onClick = { showDatePicker = true },
                        index = 0,
                        count = 2,
                    )
                    WidgetSettingTile(index = 1, count = 2) {
                        AppOutlinedTextField(
                            value = config.title,
                            onValueChange = {
                                onConfigChange(config.copy(title = it.take(CountdownWidgetDefaults.TITLE_MAX_LENGTH)))
                            },
                            label = stringResource(R.string.notes_title_hint),
                            singleLine = true,
                            shape = AppShapes.large,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                CountdownFormatSection(config = config, onConfigChange = onConfigChange)
                SegmentedGroup {
                    NoteWidgetBackgroundSection(
                        state = config.style,
                        index = 0,
                        count = 2,
                        onStateChange = { onConfigChange(config.copy(style = it)) },
                    )
                    WidgetTextColorSection(
                        style = config.style,
                        index = 1,
                        count = 2,
                        onStyleChange = { onConfigChange(config.copy(style = it)) },
                    )
                }
                WidgetSlidersSection(
                    state = config.style,
                    showBorderControls = false,
                    leadingTile = {
                        SliderRow(
                            label = stringResource(R.string.settings_font_size_title),
                            value = config.textSizeSp,
                            valueRange = CountdownWidgetDefaults.TEXT_SIZE_MIN_SP..CountdownWidgetDefaults.TEXT_SIZE_MAX_SP,
                            steps = ((CountdownWidgetDefaults.TEXT_SIZE_MAX_SP - CountdownWidgetDefaults.TEXT_SIZE_MIN_SP) / 2).toInt(),
                            valueFormatter = { "${it.roundToInt()} sp" },
                            showTicks = false,
                            onValueChange = { onConfigChange(config.copy(textSizeSp = it)) },
                        )
                    },
                    onStateChange = { onConfigChange(config.copy(style = it)) },
                )
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
    index: Int,
    count: Int,
) {
    SegmentedTile(index = index, count = count, onClick = onClick) {
        SegmentedTileRow(
            title = stringResource(R.string.widget_countdown_date),
            description =
                targetEpochDay?.let {
                    LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                } ?: stringResource(R.string.calendar_create_event_select_date),
            leading = { TonalIconBadge(icon = MaterialSymbols.CalendarMonth) },
        )
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
    AppDatePickerDialog(
        state = state,
        onDismiss = onDismiss,
        onConfirm = { state.selectedDateMillis?.let { onConfirm(Math.floorDiv(it, MILLIS_PER_DAY)) } ?: onDismiss() },
    )
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

    WidgetSettingTile(
        title = stringResource(R.string.settings_shortcut_display),
        description =
            stringResource(R.string.widget_countdown_progress_hint).takeIf { config.format == CountdownFormat.PROGRESS },
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = WidgetTileTextInset),
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            formats.forEach { format ->
                val selected = config.format == format
                AppFilterChip(
                    selected = selected,
                    onClick = { onConfigChange(config.copy(format = format)) },
                    label = stringResource(format.labelResId),
                    unselectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    showSelectedCheck = true,
                )
            }
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
