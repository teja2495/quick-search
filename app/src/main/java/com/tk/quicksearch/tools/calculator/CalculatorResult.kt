package com.tk.quicksearch.tools.calculator

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.calendar.calendarRelativeDateLabel
import com.tk.quicksearch.search.calendar.formatAbsoluteDate
import com.tk.quicksearch.search.calendar.getDayOfWeekName
import com.tk.quicksearch.search.core.CalculatorState
import com.tk.quicksearch.search.core.SearchToolType
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import com.tk.quicksearch.tools.aiSearch.CalculatorAttributionRow
import com.tk.quicksearch.tools.aiSearch.compactAttributionCardBottomPadding
import com.tk.quicksearch.tools.aiSearch.compactAttributionTopPadding

private val unitResultRegex = Regex("^([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))(?:\\s+(.+))?$")
private val dateNumberRegex = Regex("(\\d+)")
private const val COPY_CONFIRMATION_MILLIS = 1500L
private const val ACTION_CHIP_BACKGROUND_ALPHA = 0.08f
private val ACTION_CHIP_ICON_SIZE = 14.dp
private val ACTION_CHIP_VERTICAL_PADDING = 6.dp
// 48dp chip touch target plus the bottom inset and a small gap above it.
private val ACTION_ROW_RESERVED_HEIGHT = 56.dp

/** Replaces the search query; provided by the search screen so the calculator can reuse its result. */
internal val LocalCalculatorResultQueryHandler = staticCompositionLocalOf<((String) -> Unit)?> { null }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CalculatorResult(
        calculatorState: CalculatorState,
        showWallpaperBackground: Boolean = false,
) {
    val result = calculatorState.result
    val isToolMode = calculatorState.isToolMode
    val showInvalidExpression = calculatorState.showInvalidExpression

    val isReverseDateMode = calculatorState.isReverseDateMode
    val parsedDateMillis = calculatorState.parsedDateMillis
    val dateDiffLabel = calculatorState.dateDiffLabel
    val timeResultLabel = calculatorState.timeResultLabel
    val timeContextLabel = calculatorState.timeContextLabel
    val isTimeAbsoluteResult = calculatorState.isTimeAbsoluteResult
    val timeResultLabel2 = calculatorState.timeResultLabel2

    val dateLabel: String? =
            if (calculatorState.toolType == SearchToolType.DATE_CALCULATOR &&
                    parsedDateMillis != null &&
                    !isReverseDateMode) {
                calendarRelativeDateLabel(parsedDateMillis)
            } else {
                null
            }

    val absoluteDateLabel: String? =
            if (isReverseDateMode && parsedDateMillis != null) {
                formatAbsoluteDate(parsedDateMillis)
            } else {
                null
            }

    val dayOfWeek: String? = parsedDateMillis?.let { getDayOfWeekName(it) }

    if (result == null && dateLabel == null && absoluteDateLabel == null && dateDiffLabel == null && timeResultLabel == null && !isToolMode) return

    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val copyText = timeResultLabel ?: absoluteDateLabel ?: dateDiffLabel ?: dateLabel ?: result
    var showCopyConfirmation by remember { mutableStateOf(false) }
    val onCopy: (() -> Unit)? =
            if (copyText != null) {
                {
                    clipboardManager.setText(AnnotatedString(copyText))
                    showCopyConfirmation = true
                }
            } else {
                null
            }
    LaunchedEffect(showCopyConfirmation) {
        if (showCopyConfirmation) {
            delay(COPY_CONFIRMATION_MILLIS)
            showCopyConfirmation = false
        }
    }
    val queryHandler = LocalCalculatorResultQueryHandler.current
    val onUseResult: (() -> Unit)? =
            if (calculatorState.toolType == SearchToolType.CALCULATOR &&
                    result != null &&
                    queryHandler != null) {
                { queryHandler(result) }
            } else {
                null
            }

    Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        // With the action chips the card sizes to its content; the fixed minimum only pads empty states.
        val cardMinHeight = if (onCopy != null) 0.dp else 175.dp
        androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            com.tk.quicksearch.search.searchScreen.shared.InformationCard(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(min = cardMinHeight)
                                    .combinedClickable(
                                            onClick = {},
                                            onLongClick = onCopy,
                                    )
                                    // The chips sit closer to the card edge than text content does.
                                    .padding(
                                            bottom =
                                                    compactAttributionCardBottomPadding(
                                                            if (onCopy != null) DesignTokens.SpacingSmall
                                                            else DesignTokens.SpacingXSmall,
                                                    ),
                                    ),
                    showWallpaperBackground = showWallpaperBackground,
            ) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = cardMinHeight)) {
                    Column(
                            modifier =
                                    Modifier.align(Alignment.CenterStart)
                                            .padding(
                                                    start = DesignTokens.SpacingLarge,
                                                    end = DesignTokens.SpacingLarge,
                                                    top = DesignTokens.SpacingLarge,
                                                    bottom =
                                                            if (onCopy != null) ACTION_ROW_RESERVED_HEIGHT
                                                            else DesignTokens.SpacingLarge,
                                            ),
                    ) {
                        CalculatorResultContent(
                                calculatorState = calculatorState,
                                result = result,
                                dateLabel = dateLabel,
                                absoluteDateLabel = absoluteDateLabel,
                                dateDiffLabel = dateDiffLabel,
                                timeResultLabel = timeResultLabel,
                                timeContextLabel = timeContextLabel,
                                timeResultLabel2 = timeResultLabel2,
                                isTimeAbsoluteResult = isTimeAbsoluteResult,
                                dayOfWeek = dayOfWeek,
                                showInvalidExpression = showInvalidExpression,
                        )
                    }
                    if (onCopy != null) {
                        CalculatorResultActions(
                                // Chips carry their own 48dp touch-target slack below them.
                                modifier =
                                        Modifier.align(Alignment.BottomStart)
                                                .padding(
                                                        start = DesignTokens.SpacingLarge,
                                                        bottom = DesignTokens.SpacingXSmall,
                                                ),
                                showCopyConfirmation = showCopyConfirmation,
                                onCopy = onCopy,
                                onUseResult = onUseResult,
                        )
                    }
                }
            }
        }

        CalculatorAttributionRow(
                modifier = Modifier.fillMaxWidth().padding(top = compactAttributionTopPadding()),
                toolType = calculatorState.toolType,
        )
    }
}

@Composable
private fun CalculatorResultActions(
        modifier: Modifier,
        showCopyConfirmation: Boolean,
        onCopy: () -> Unit,
        onUseResult: (() -> Unit)?,
) {
    Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        CalculatorActionChip(
                icon = if (showCopyConfirmation) MaterialSymbols.Check else MaterialSymbols.ContentCopy,
                label = stringResource(R.string.calculator_copy_result),
                onClick = onCopy,
        )
        if (onUseResult != null) {
            CalculatorActionChip(
                    icon = MaterialSymbols.NorthWest,
                    label = stringResource(R.string.calculator_use_result),
                    onClick = onUseResult,
            )
        }
    }
}

@Composable
private fun CalculatorActionChip(
        icon: ImageVector,
        label: String,
        onClick: () -> Unit,
) {
    Surface(
            onClick = onClick,
            shape = DesignTokens.ShapeFull,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = ACTION_CHIP_BACKGROUND_ALPHA),
            contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
                modifier =
                        Modifier.padding(
                                horizontal = DesignTokens.SpacingMedium,
                                vertical = ACTION_CHIP_VERTICAL_PADDING,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ACTION_CHIP_ICON_SIZE),
            )
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CalculatorResultContent(
        calculatorState: CalculatorState,
        result: String?,
        dateLabel: String?,
        absoluteDateLabel: String?,
        dateDiffLabel: String?,
        timeResultLabel: String?,
        timeContextLabel: String?,
        timeResultLabel2: String?,
        isTimeAbsoluteResult: Boolean,
        dayOfWeek: String?,
        showInvalidExpression: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            timeResultLabel != null && timeResultLabel2 != null -> {
                // Absolute time ("10am"): what the countdown targets, the countdown, then the last one.
                Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
                    if (timeContextLabel != null) {
                        Text(
                                text = timeContextLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DateCalculatorResultText(label = timeResultLabel, isAbsoluteDate = false)
                    Row(
                            modifier = Modifier.padding(top = DesignTokens.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
                    ) {
                        Icon(
                                imageVector = MaterialSymbols.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                        )
                        Text(
                                text = timeResultLabel2,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            timeResultLabel != null -> {
                DateCalculatorResultText(
                        label = timeResultLabel,
                        contextLabel = timeContextLabel,
                        isAbsoluteDate = isTimeAbsoluteResult,
                )
            }
            absoluteDateLabel != null -> {
                DateCalculatorResultText(
                        label = absoluteDateLabel,
                        dayOfWeek = dayOfWeek,
                        isAbsoluteDate = true,
                )
            }
            dateDiffLabel != null -> {
                DateCalculatorResultText(label = dateDiffLabel)
            }
            dateLabel != null -> {
                DateCalculatorResultText(label = dateLabel, dayOfWeek = dayOfWeek)
            }
            result != null -> {
                if (calculatorState.toolType == SearchToolType.UNIT_CONVERTER) {
                    UnitConverterResultText(result = result)
                } else {
                    CalculatorValueText(result = result)
                }
            }
            showInvalidExpression -> {
                Text(
                        text =
                                when (calculatorState.toolType) {
                                    SearchToolType.UNIT_CONVERTER ->
                                            stringResource(
                                                    R.string.unit_converter_invalid_or_unsupported_query
                                            )
                                    SearchToolType.DATE_CALCULATOR ->
                                            stringResource(R.string.date_calculator_invalid_date)
                                    else ->
                                            stringResource(
                                                    R.string.calculator_invalid_or_unsupported_expression
                                            )
                                },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                // Intentionally empty while in tool mode with no expression.
            }
        }
    }
}

/** Steps the result down through smaller styles until it fits on one line; wraps at the smallest. */
@Composable
private fun CalculatorValueText(result: String) {
    // Non-breaking space keeps "=" on the same line as the number when a long result wraps.
    val text = "=\u00A0$result"
    val typography = MaterialTheme.typography
    val styles =
            listOf(
                    typography.displayMedium,
                    typography.displaySmall,
                    typography.headlineLarge,
                    typography.headlineMedium,
            )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val textMeasurer = rememberTextMeasurer()
        val maxWidthPx = constraints.maxWidth
        val style =
                styles.firstOrNull {
                    textMeasurer.measure(text = text, style = it, maxLines = 1).size.width <=
                            maxWidthPx
                } ?: styles.last()
        Text(
                text = text,
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun UnitConverterResultText(result: String) {
    val match = unitResultRegex.matchEntire(result)
    val value = match?.groupValues?.getOrNull(1) ?: result
    val unit = match?.groupValues?.getOrNull(2).orEmpty()
    val valueTextStyle = MaterialTheme.typography.displayMedium
    val unitTextStyle = MaterialTheme.typography.bodyMedium

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val valueWidthPx = textMeasurer.measure(text = value, style = valueTextStyle).size.width
        val unitWidthPx =
                if (unit.isNotBlank()) {
                    textMeasurer.measure(text = unit, style = unitTextStyle).size.width
                } else {
                    0
                }
        val spacingPx =
                with(density) {
                    if (unit.isNotBlank()) {
                        DesignTokens.SpacingSmall.toPx().toInt()
                    } else {
                        0
                    }
                }
        val hasRoomForSingleLine =
                valueWidthPx + spacingPx + unitWidthPx <=
                        with(density) { maxWidth.toPx().toInt() }

        if (unit.isNotBlank() && !hasRoomForSingleLine) {
            Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
            ) {
                Text(
                        text = value,
                        style = valueTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                        text = unit,
                        style = unitTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            ) {
                Text(
                        text = value,
                        style = valueTextStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                )
                if (unit.isNotBlank()) {
                    Text(
                            text = unit,
                            style = unitTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.offset(y = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DateCalculatorResultText(
        label: String,
        dayOfWeek: String? = null,
        contextLabel: String? = null,
        isAbsoluteDate: Boolean = false,
) {
    val segments = remember(label) {
        val list = mutableListOf<Pair<String, Boolean>>()
        var lastEnd = 0
        for (match in dateNumberRegex.findAll(label)) {
            if (match.range.first > lastEnd) {
                list.add(label.substring(lastEnd, match.range.first) to false)
            }
            list.add(match.value to true)
            lastEnd = match.range.last + 1
        }
        if (lastEnd < label.length) list.add(label.substring(lastEnd) to false)
        list
    }

    val hasNumbers = segments.any { it.second }

    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall)) {
        if (!hasNumbers || isAbsoluteDate) {
            Text(
                    text = label,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Row(
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
            ) {
                for ((text, isNumber) in segments) {
                    val trimmed = text.trim()
                    if (trimmed.isEmpty()) continue
                    if (isNumber) {
                        Text(
                                text = trimmed,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.alignByBaseline(),
                        )
                    } else {
                        Text(
                                text = trimmed,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
            }
        }

        if (contextLabel != null) {
            Text(
                    text = contextLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (dayOfWeek != null) {
            Text(
                    text = dayOfWeek,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
