package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow

private val unitResultRegex = Regex("^([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))(?:\\s+(.+))?$")
private val dateNumberRegex = Regex("(\\d+)")

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
    val timeContextLabel2 = calculatorState.timeContextLabel2

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
    val onLongClick: (() -> Unit)? =
            if (copyText != null) {
                { clipboardManager.setText(AnnotatedString(copyText)) }
            } else {
                null
            }

    val isDualTimeResult = timeResultLabel != null && timeResultLabel2 != null

    Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        val cardMinHeight = if (isDualTimeResult) 280.dp else 175.dp
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
                                            onLongClick = onLongClick,
                                    ),
                    showWallpaperBackground = showWallpaperBackground,
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
                        timeContextLabel2 = timeContextLabel2,
                        isTimeAbsoluteResult = isTimeAbsoluteResult,
                        dayOfWeek = dayOfWeek,
                        showInvalidExpression = showInvalidExpression,
                        isDualTimeResult = isDualTimeResult,
                )
            }
        }

        CalculatorAttributionRow(
                modifier = Modifier.fillMaxWidth(),
                toolType = calculatorState.toolType,
        )
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
        timeContextLabel2: String?,
        isTimeAbsoluteResult: Boolean,
        dayOfWeek: String?,
        showInvalidExpression: Boolean,
        isDualTimeResult: Boolean,
) {
    Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(DesignTokens.SpacingLarge),
            verticalArrangement = if (isDualTimeResult) Arrangement.spacedBy(0.dp) else Arrangement.Center,
    ) {
        when {
            timeResultLabel != null && timeResultLabel2 != null -> {
                Column(
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
                ) {
                    DateCalculatorResultText(
                            label = timeResultLabel,
                            contextLabel = timeContextLabel,
                            isAbsoluteDate = false,
                    )
                    DateCalculatorResultText(
                            label = timeResultLabel2,
                            contextLabel = timeContextLabel2,
                            isAbsoluteDate = false,
                    )
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
                    Text(
                            text = "= $result",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                    )
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
