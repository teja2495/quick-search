package com.tk.quicksearch.widget

import android.content.Intent
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.widget.MicAction
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
            Surface(shadowElevation = WidgetConfigConstants.SURFACE_ELEVATION) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WidgetConfigConstants.BOTTOM_BAR_HORIZONTAL_PADDING,
                            end = WidgetConfigConstants.BOTTOM_BAR_HORIZONTAL_PADDING,
                            top = WidgetConfigConstants.BOTTOM_BAR_VERTICAL_PADDING,
                            bottom = WidgetConfigConstants.BOTTOM_BAR_BOTTOM_PADDING
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onApply,
                        enabled = isLoaded,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WidgetConfigConstants.BOTTOM_BUTTON_HEIGHT)
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
            WidgetLoadingState(innerPadding = innerPadding)
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
                    .padding(horizontal = WidgetConfigConstants.HORIZONTAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.PREVIEW_SECTION_SPACING)
            ) {
                WidgetPreviewCard(state = state)
            }

            // Scrollable preferences section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = WidgetConfigConstants.HORIZONTAL_PADDING,
                        end = WidgetConfigConstants.HORIZONTAL_PADDING,
                        bottom = WidgetConfigConstants.SCROLLABLE_SECTION_BOTTOM_PADDING
                    ),
                verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING)
            ) {
                WidgetBackgroundColorSection(state = state, onStateChange = onStateChange)
                WidgetSlidersSection(state = state, onStateChange = onStateChange)
                WidgetToggleSection(state = state, onStateChange = onStateChange)
                if (state.showSearchIcon) {
                    WidgetIconAlignmentSection(state = state, onStateChange = onStateChange)
                }
                WidgetMicIconSection(state = state, onStateChange = onStateChange)
                if (state.showMicIcon) {
                    WidgetMicActionSection(state = state, onStateChange = onStateChange)
                }
            }
        }
    }
}

/**
 * Loading state displayed while widget preferences are being loaded.
 */
@Composable
private fun WidgetLoadingState(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(WidgetConfigConstants.LOADING_STATE_SPACING))
            Text(text = stringResource(R.string.widget_loading_state))
        }
    }
}

@Composable
private fun WidgetSlidersSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SLIDER_ROW_SPACING)) {
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
            valueFormatter = { formatBorderWidth(it) },
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

/**
 * Formats border width value for display.
 */
private fun formatBorderWidth(value: Float): String {
    return if (value == 0f) {
        "0 dp"
    } else {
        String.format(Locale.US, "%.1f dp", value)
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
    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(text = stringResource(R.string.widget_background_color))
        ColorChoiceSegmentedButtonRow(
            selectedIsWhite = state.backgroundColorIsWhite,
            onSelectionChange = { onStateChange(state.copy(backgroundColorIsWhite = it)) }
        )

        Spacer(modifier = Modifier.height(WidgetConfigConstants.COLOR_SECTION_SPACING))
        Text(text = stringResource(R.string.widget_text_icon_color))
        ColorChoiceSegmentedButtonRow(
            selectedIsWhite = state.textIconColorIsWhite,
            onSelectionChange = { onStateChange(state.copy(textIconColorIsWhite = it)) }
        )
    }
}

@Composable
private fun WidgetIconAlignmentSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(text = stringResource(R.string.widget_icon_alignment))
        AlignmentChoiceSegmentedButtonRow(
            selectedAlignLeft = state.iconAlignLeft,
            onSelectionChange = { onStateChange(state.copy(iconAlignLeft = it)) }
        )
    }
}

@Composable
private fun WidgetToggleSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING)) {
        ToggleRow(
            label = stringResource(R.string.widget_toggle_show_label),
            checked = state.showLabel,
            onCheckedChange = { onStateChange(state.copy(showLabel = it)) }
        )
        ToggleRow(
            label = stringResource(R.string.widget_toggle_show_search_icon),
            checked = state.showSearchIcon,
            onCheckedChange = { onStateChange(state.copy(showSearchIcon = it)) }
        )
    }
}

@Composable
private fun WidgetMicIconSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    ToggleRow(
        label = stringResource(R.string.widget_toggle_show_mic_icon),
        checked = state.showMicIcon,
        onCheckedChange = { onStateChange(state.copy(showMicIcon = it)) }
    )
}

@Composable
private fun WidgetMicActionSection(
    state: QuickSearchWidgetPreferences,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(text = stringResource(R.string.widget_mic_action))
        MicActionChoiceSegmentedButtonRow(
            selectedAction = state.micAction,
            onSelectionChange = { onStateChange(state.copy(micAction = it)) }
        )

        // Show limitation text when Digital Assistant is selected
        if (state.micAction == MicAction.DIGITAL_ASSISTANT) {
            val context = LocalContext.current
            val limitationText = stringResource(R.string.widget_mic_action_digital_assistant_limitation)
            val linkText = "digital assistant app"

            val annotatedString = createClickableText(
                fullText = limitationText,
                linkText = linkText,
                onClick = {
                    // Open voice input settings (contains digital assistant settings)
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to general settings if voice input settings not available
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Ignore if settings can't be opened
                        }
                    }
                }
            )

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Creates an annotated string with a clickable link for the specified text.
 */
@Composable
private fun createClickableText(
    fullText: String,
    linkText: String,
    onClick: () -> Unit
): AnnotatedString {
    val linkStartIndex = fullText.indexOf(linkText, ignoreCase = true)

    return buildAnnotatedString {
        if (linkStartIndex >= 0) {
            val linkEndIndex = linkStartIndex + linkText.length

            // Add text before the link
            append(fullText.substring(0, linkStartIndex))

            // Add the clickable link
            pushLink(LinkAnnotation.Clickable(
                tag = "LINK",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = { onClick() }
            ))
            append(linkText)
            pop()

            // Add text after the link
            append(fullText.substring(linkEndIndex))
        } else {
            // Fallback: just add the whole text normally
            append(fullText)
        }
    }
}

/**
 * Segmented button row for choosing between white and black colors.
 */
@Composable
private fun ColorChoiceSegmentedButtonRow(
    selectedIsWhite: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = selectedIsWhite,
            onClick = { onSelectionChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {}
        ) {
            Text(stringResource(R.string.widget_background_white))
        }
        SegmentedButton(
            selected = !selectedIsWhite,
            onClick = { onSelectionChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {}
        ) {
            Text(stringResource(R.string.widget_background_black))
        }
    }
}

/**
 * Segmented button row for choosing between left and center icon alignment.
 */
@Composable
private fun AlignmentChoiceSegmentedButtonRow(
    selectedAlignLeft: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = selectedAlignLeft,
            onClick = { onSelectionChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {}
        ) {
            Text(stringResource(R.string.widget_icon_align_left))
        }
        SegmentedButton(
            selected = !selectedAlignLeft,
            onClick = { onSelectionChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {}
        ) {
            Text(stringResource(R.string.widget_icon_align_center))
        }
    }
}

/**
 * Segmented button row for choosing between mic actions.
 */
@Composable
private fun MicActionChoiceSegmentedButtonRow(
    selectedAction: MicAction,
    onSelectionChange: (MicAction) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        SegmentedButton(
            selected = selectedAction == MicAction.DEFAULT_VOICE_SEARCH,
            onClick = { onSelectionChange(MicAction.DEFAULT_VOICE_SEARCH) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {}
        ) {
            Text(stringResource(R.string.widget_mic_action_default))
        }
        SegmentedButton(
            selected = selectedAction == MicAction.DIGITAL_ASSISTANT,
            onClick = { onSelectionChange(MicAction.DIGITAL_ASSISTANT) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {}
        ) {
            Text(stringResource(R.string.widget_mic_action_digital_assistant))
        }
    }
}

