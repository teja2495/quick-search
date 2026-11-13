package com.tk.quicksearch.widgets.widgetConfigScreen.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.util.defaultAssistantPackage
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
                if (display != SearchIconDisplay.LEFT && state.hasCustomButtons) {
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

        // Show which app the mic opens when Digital Assistant is selected
        if (state.micAction == MicAction.DIGITAL_ASSISTANT) {
            val context = LocalContext.current
            var assistantName by remember { mutableStateOf(context.defaultAssistantLabel()) }
            // The user may change the assistant in system settings and come back.
            LifecycleResumeEffect(Unit) {
                assistantName = context.defaultAssistantLabel()
                onPauseOrDispose {}
            }
            val hintText =
                assistantName?.let {
                    stringResource(R.string.widget_mic_action_digital_assistant_current, it)
                } ?: stringResource(R.string.widget_mic_action_digital_assistant_none)
            val linkText =
                stringResource(R.string.widget_mic_action_digital_assistant_tap_here)

            val annotatedString =
                createClickableText(
                    fullText = hintText,
                    linkText = linkText,
                    onClick = {
                        // Voice input settings hold the digital assistant choice.
                        try {
                            val intent =
                                Intent(
                                    android.provider.Settings
                                        .ACTION_VOICE_INPUT_SETTINGS,
                                )
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent =
                                    Intent(
                                        android.provider
                                            .Settings
                                            .ACTION_SETTINGS,
                                    )
                                context.startActivity(intent)
                            } catch (e: Exception) {
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

private fun Context.defaultAssistantLabel(): String? =
    defaultAssistantPackage()?.let { assistantPackage ->
        runCatching {
            packageManager
                .getApplicationLabel(packageManager.getApplicationInfo(assistantPackage, 0))
                .toString()
        }.getOrNull()
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

            append(fullText.substring(0, linkStartIndex))

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

            append(fullText.substring(linkEndIndex))
        } else {
            append(fullText)
        }
    }
}
