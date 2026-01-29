package com.tk.quicksearch.widget

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.ui.components.TipBanner
import com.tk.quicksearch.util.hapticToggle
import com.tk.quicksearch.widget.customButtons.CustomWidgetButtonsSection
import com.tk.quicksearch.widget.voiceSearch.MicAction
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSearchWidgetConfigScreen(
        state: QuickSearchWidgetPreferences,
        isLoaded: Boolean,
        onStateChange: (QuickSearchWidgetPreferences) -> Unit,
        onApply: () -> Unit,
        onCancel: () -> Unit,
        searchViewModel: SearchViewModel,
        showConfigTip: Boolean = false,
        onDismissConfigTip: (() -> Unit)? = null
) {
        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                text =
                                                        stringResource(
                                                                R.string.widget_settings_title
                                                        ),
                                                style = MaterialTheme.typography.titleLarge
                                        )
                                },
                                navigationIcon = {
                                        IconButton(onClick = onCancel) {
                                                Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription =
                                                                stringResource(
                                                                        R.string
                                                                                .widget_action_cancel
                                                                )
                                                )
                                        }
                                }
                        )
                },
                bottomBar = {
                        Surface(shadowElevation = WidgetConfigConstants.SURFACE_ELEVATION) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                start =
                                                                        WidgetConfigConstants
                                                                                .BOTTOM_BAR_HORIZONTAL_PADDING,
                                                                end =
                                                                        WidgetConfigConstants
                                                                                .BOTTOM_BAR_HORIZONTAL_PADDING,
                                                                top =
                                                                        WidgetConfigConstants
                                                                                .BOTTOM_BAR_VERTICAL_PADDING,
                                                                bottom =
                                                                        WidgetConfigConstants
                                                                                .BOTTOM_BAR_BOTTOM_PADDING
                                                        ),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Button(
                                                onClick = onApply,
                                                enabled = isLoaded,
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .height(
                                                                        WidgetConfigConstants
                                                                                .BOTTOM_BUTTON_HEIGHT
                                                                )
                                        ) {
                                                Text(
                                                        text = stringResource(R.string.dialog_save),
                                                        style = MaterialTheme.typography.labelLarge
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

                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        // Fixed preview section at the top
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .padding(
                                                        horizontal =
                                                                WidgetConfigConstants
                                                                        .HORIZONTAL_PADDING
                                                ),
                                verticalArrangement =
                                        Arrangement.spacedBy(
                                                WidgetConfigConstants.PREVIEW_SECTION_SPACING
                                        )
                        ) { WidgetPreviewCard(state = state) }

                        // Scrollable preferences section
                        Column(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .weight(1f)
                                                .verticalScroll(rememberScrollState())
                                                .padding(
                                                        start =
                                                                WidgetConfigConstants
                                                                        .HORIZONTAL_PADDING,
                                                        end =
                                                                WidgetConfigConstants
                                                                        .HORIZONTAL_PADDING,
                                                        bottom =
                                                                WidgetConfigConstants
                                                                        .SCROLLABLE_SECTION_BOTTOM_PADDING
                                                ),
                                verticalArrangement =
                                        Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING)
                        ) {
                                WidgetThemeSection(state = state, onStateChange = onStateChange)

                                // Tip banner (only shown once)
                                if (showConfigTip) {
                                        TipBanner(
                                                text =
                                                        stringResource(
                                                                R.string.widget_config_scroll_tip
                                                        ),
                                                onDismiss = onDismissConfigTip,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                }

                                WidgetSlidersSection(state = state, onStateChange = onStateChange)
                                WidgetSearchIconSection(
                                        state = state,
                                        onStateChange = onStateChange
                                )
                                WidgetMicIconSection(state = state, onStateChange = onStateChange)
                                WidgetToggleSection(
                                        state = state,
                                        hasCustomButtons = state.hasCustomButtons,
                                        onStateChange = onStateChange
                                )
                                WidgetTextIconColorSection(
                                        state = state,
                                        onStateChange = onStateChange
                                )
                                CustomWidgetButtonsSection(
                                        state = state,
                                        searchViewModel = searchViewModel,
                                        onStateChange = onStateChange
                                )
                        }
                }
        }
}

@Composable
private fun WidgetLoadingState(innerPadding: PaddingValues) {
        Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
        ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(
                                modifier =
                                        Modifier.height(WidgetConfigConstants.LOADING_STATE_SPACING)
                        )
                        Text(
                                text = stringResource(R.string.widget_loading_state),
                                style = MaterialTheme.typography.bodyMedium
                        )
                }
        }
}

@Composable
private fun WidgetSlidersSection(
        state: QuickSearchWidgetPreferences,
        onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
        Column(
                verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SLIDER_ROW_SPACING)
        ) {
                SliderRow(
                        label = stringResource(R.string.widget_slider_radius),
                        value = state.borderRadiusDp,
                        valueRange = 0f..30f,
                        steps = 30,
                        valueFormatter = { "${it.roundToInt()} dp" },
                        onValueChange = { onStateChange(state.copy(borderRadiusDp = it)) }
                )
                SliderRow(
                        label = stringResource(R.string.widget_slider_border),
                        value = state.borderWidthDp,
                        valueRange = 0f..4f,
                        steps = 8,
                        valueFormatter = { formatBorderWidth(it) },
                        onValueChange = { onStateChange(state.copy(borderWidthDp = it)) }
                )
                SliderRow(
                        label = stringResource(R.string.widget_slider_transparency),
                        value = state.backgroundAlpha,
                        valueRange = 0f..1f,
                        steps = 10,
                        valueFormatter = { "${(it * 100).roundToInt()}%" },
                        onValueChange = { onStateChange(state.copy(backgroundAlpha = it)) }
                )
        }
}

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
        steps: Int,
        valueFormatter: (Float) -> String,
        onValueChange: (Float) -> Unit
) {
        val view = LocalView.current
        val start = valueRange.start
        val span = valueRange.endInclusive - start
        var lastStepIndex by remember {
                mutableStateOf(
                        if (span > 0)
                                ((value - start) / span * steps).roundToInt().coerceIn(0, steps)
                        else 0
                )
        }
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
                        onValueChange = { v ->
                                val step =
                                        if (span > 0)
                                                ((v - start) / span * steps)
                                                        .roundToInt()
                                                        .coerceIn(0, steps)
                                        else 0
                                if (step != lastStepIndex) {
                                        hapticToggle(view)()
                                        lastStepIndex = step
                                }
                                onValueChange(v)
                        },
                        valueRange = valueRange
                )
        }
}

@Composable
private fun WidgetThemeSection(
        state: QuickSearchWidgetPreferences,
        onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
        Column(
                verticalArrangement =
                        Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)
        ) {
                Text(
                        text = stringResource(R.string.widget_theme),
                        style = MaterialTheme.typography.titleSmall
                )
                ThemeChoiceSegmentedButtonRow(
                        selectedTheme = state.theme,
                        onSelectionChange = { onStateChange(state.copy(theme = it)) }
                )
        }
}

@Composable
private fun WidgetSearchIconSection(
        state: QuickSearchWidgetPreferences,
        onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
        val context = LocalContext.current
        Column(
                verticalArrangement =
                        Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)
        ) {
                Text(
                        text = stringResource(R.string.widget_search_icon),
                        style = MaterialTheme.typography.titleSmall
                )
                SearchIconChoiceSegmentedButtonRow(
                        selectedDisplay = state.searchIconDisplay,
                        onSelectionChange = { display ->
                                if (display == SearchIconDisplay.CENTER && state.hasCustomButtons) {
                                        Toast.makeText(
                                                        context,
                                                        context.getString(
                                                                R.string
                                                                        .widget_custom_buttons_restriction
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                } else {
                                        onStateChange(state.copy(searchIconDisplay = display))
                                }
                        }
                )
        }
}

@Composable
private fun WidgetToggleSection(
        state: QuickSearchWidgetPreferences,
        hasCustomButtons: Boolean,
        onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
        val context = LocalContext.current
        Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.SECTION_SPACING)) {
                ToggleRow(
                        label = stringResource(R.string.widget_toggle_show_label),
                        checked = state.showLabel,
                        onCheckedChange = { enabled ->
                                if (enabled && hasCustomButtons) {
                                        Toast.makeText(
                                                        context,
                                                        context.getString(
                                                                R.string
                                                                        .widget_custom_buttons_restriction
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        return@ToggleRow
                                }
                                onStateChange(state.copy(showLabel = enabled))
                        }
                )
        }
}

@Composable
private fun WidgetMicIconSection(
        state: QuickSearchWidgetPreferences,
        onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
        Column(
                verticalArrangement =
                        Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)
        ) {
                Text(
                        text = stringResource(R.string.widget_mic_icon),
                        style = MaterialTheme.typography.titleSmall
                )
                MicActionChoiceSegmentedButtonRow(
                        selectedAction = state.micAction,
                        onSelectionChange = { onStateChange(state.copy(micAction = it)) }
                )

                // Show limitation text when Digital Assistant is selected
                if (state.micAction == MicAction.DIGITAL_ASSISTANT) {
                        val context = LocalContext.current
                        val limitationText =
                                stringResource(
                                        R.string.widget_mic_action_digital_assistant_limitation
                                )
                        val linkText = "digital assistant app"

                        val annotatedString =
                                createClickableText(
                                        fullText = limitationText,
                                        linkText = linkText,
                                        onClick = {
                                                // Open voice input settings (contains digital
                                                // assistant settings)
                                                try {
                                                        val intent =
                                                                Intent(
                                                                        android.provider.Settings
                                                                                .ACTION_VOICE_INPUT_SETTINGS
                                                                )
                                                        context.startActivity(intent)
                                                } catch (e: Exception) {
                                                        // Fallback to general settings if voice
                                                        // input settings not
                                                        // available
                                                        try {
                                                                val intent =
                                                                        Intent(
                                                                                android.provider
                                                                                        .Settings
                                                                                        .ACTION_SETTINGS
                                                                        )
                                                                context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                                // Ignore if settings can't be
                                                                // opened
                                                        }
                                                }
                                        }
                                )

                        Text(
                                text = annotatedString,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                        )
                }
        }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
                Text(text = label)
                Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
}

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
                        pushLink(
                                LinkAnnotation.Clickable(
                                        tag = "LINK",
                                        styles =
                                                TextLinkStyles(
                                                        style =
                                                                SpanStyle(
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        textDecoration =
                                                                                TextDecoration
                                                                                        .Underline
                                                                )
                                                ),
                                        linkInteractionListener = { onClick() }
                                )
                        )
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

@Composable
private fun ThemeChoiceSegmentedButtonRow(
        selectedTheme: WidgetTheme,
        onSelectionChange: (WidgetTheme) -> Unit
) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                        selected = selectedTheme == WidgetTheme.LIGHT,
                        onClick = { onSelectionChange(WidgetTheme.LIGHT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_theme_light)) }
                SegmentedButton(
                        selected = selectedTheme == WidgetTheme.DARK,
                        onClick = { onSelectionChange(WidgetTheme.DARK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_theme_dark)) }
                SegmentedButton(
                        selected = selectedTheme == WidgetTheme.SYSTEM,
                        onClick = { onSelectionChange(WidgetTheme.SYSTEM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_theme_system)) }
        }
}

@Composable
private fun SearchIconChoiceSegmentedButtonRow(
        selectedDisplay: SearchIconDisplay,
        onSelectionChange: (SearchIconDisplay) -> Unit
) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                        selected = selectedDisplay == SearchIconDisplay.LEFT,
                        onClick = { onSelectionChange(SearchIconDisplay.LEFT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_icon_left)) }
                SegmentedButton(
                        selected = selectedDisplay == SearchIconDisplay.CENTER,
                        onClick = { onSelectionChange(SearchIconDisplay.CENTER) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_icon_center)) }
                SegmentedButton(
                        selected = selectedDisplay == SearchIconDisplay.OFF,
                        onClick = { onSelectionChange(SearchIconDisplay.OFF) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_icon_off)) }
        }
}

@Composable
private fun MicActionChoiceSegmentedButtonRow(
        selectedAction: MicAction,
        onSelectionChange: (MicAction) -> Unit
) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                        selected = selectedAction == MicAction.DEFAULT_VOICE_SEARCH,
                        onClick = { onSelectionChange(MicAction.DEFAULT_VOICE_SEARCH) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_mic_action_default)) }
                SegmentedButton(
                        selected = selectedAction == MicAction.DIGITAL_ASSISTANT,
                        onClick = { onSelectionChange(MicAction.DIGITAL_ASSISTANT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_mic_action_digital_assistant)) }
                SegmentedButton(
                        selected = selectedAction == MicAction.OFF,
                        onClick = { onSelectionChange(MicAction.OFF) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {}
                ) { Text(stringResource(R.string.widget_mic_action_off)) }
        }
}

@Composable
private fun WidgetTextIconColorSection(
        state: QuickSearchWidgetPreferences,
        onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
        Column(
                verticalArrangement =
                        Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)
        ) {
                Text(
                        text = stringResource(R.string.widget_text_icon_color),
                        style = MaterialTheme.typography.titleSmall
                )
                TextIconColorChoiceSegmentedButtonRow(
                        selectedOverride = state.textIconColorOverride,
                        onSelectionChange = {
                                onStateChange(state.copy(textIconColorOverride = it))
                        }
                )
        }
}

@Composable
private fun TextIconColorChoiceSegmentedButtonRow(
        selectedOverride: TextIconColorOverride,
        onSelectionChange: (TextIconColorOverride) -> Unit
) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                        selected = selectedOverride == TextIconColorOverride.THEME,
                        onClick = { onSelectionChange(TextIconColorOverride.THEME) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {}
                ) { Text("Theme") }
                SegmentedButton(
                        selected = selectedOverride == TextIconColorOverride.WHITE,
                        onClick = { onSelectionChange(TextIconColorOverride.WHITE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {}
                ) { Text("White") }
                SegmentedButton(
                        selected = selectedOverride == TextIconColorOverride.BLACK,
                        onClick = { onSelectionChange(TextIconColorOverride.BLACK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {}
                ) { Text("Black") }
        }
}
