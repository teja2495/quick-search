package com.tk.quicksearch.widgets.WidgetConfigScreen.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.MicActionChoiceSegmentedButtonRow
import com.tk.quicksearch.widgets.WidgetConfigScreen.components.SearchIconChoiceSegmentedButtonRow
import com.tk.quicksearch.widgets.searchWidget.MicAction
import com.tk.quicksearch.widgets.utils.SearchIconDisplay
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetPreferences

@Composable
fun WidgetSearchIconSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val context = LocalContext.current
    Column(
        verticalArrangement =
            Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING),
    ) {
        Text(
            text = stringResource(R.string.widget_search_icon),
            style = MaterialTheme.typography.titleSmall,
        )
        SearchIconChoiceSegmentedButtonRow(
            selectedDisplay = state.searchIconDisplay,
            onSelectionChange = { display ->
                if (display == SearchIconDisplay.CENTER && state.hasCustomButtons) {
                    Toast
                        .makeText(
                            context,
                            context.getString(
                                R.string
                                    .widget_custom_buttons_restriction,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    onStateChange(state.copy(searchIconDisplay = display))
                }
            },
        )
    }
}

@Composable
fun WidgetMicIconSection(
    state: WidgetPreferences,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING),
    ) {
        Text(
            text = stringResource(R.string.widget_mic_icon),
            style = MaterialTheme.typography.titleSmall,
        )
        MicActionChoiceSegmentedButtonRow(
            selectedAction = state.micAction,
            onSelectionChange = { onStateChange(state.copy(micAction = it)) },
        )

        // Show limitation text when Digital Assistant is selected
        if (state.micAction == MicAction.DIGITAL_ASSISTANT) {
            val context = LocalContext.current
            val limitationText =
                stringResource(
                    R.string.widget_mic_action_digital_assistant_limitation,
                )
            val linkText =
                stringResource(R.string.widget_mic_action_digital_assistant_link_phrase)

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
                                        .ACTION_VOICE_INPUT_SETTINGS,
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
                                            .ACTION_SETTINGS,
                                    )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore if settings can't be
                                // opened
                            }
                        }
                    },
                )

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun createClickableText(
    fullText: String,
    linkText: String,
    onClick: () -> Unit,
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
                                            .Underline,
                                ),
                        ),
                    linkInteractionListener = { onClick() },
                ),
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