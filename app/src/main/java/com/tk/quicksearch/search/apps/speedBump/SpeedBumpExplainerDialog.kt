package com.tk.quicksearch.search.apps.speedBump

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.components.AppTextButton

/**
 * Explains what SpeedBump does.
 *
 * Shown once, right after the user first turns the feature on, and again on every long press of
 * the action. It never changes state — the toggle has already happened by the time it appears.
 */
@Composable
fun SpeedBumpExplainerDialog(
    /** App the feature was just enabled for, or null when this is a long-press reminder. */
    justEnabledForAppName: String?,
    onDismiss: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = MaterialSymbols.Spa, contentDescription = null) },
        // Material's default icon tint is the secondary colour, which reads as purple here.
        iconContentColor = AppColors.DialogText,
        title = { Text(text = stringResource(R.string.speed_bump_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge)) {
                Text(
                    text = stringResource(R.string.speed_bump_explainer_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                justEnabledForAppName?.let { appName ->
                    Text(
                        text = boldAppName(
                            text = stringResource(R.string.speed_bump_explainer_just_enabled, appName),
                            appName = appName,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.release_notes_action_got_it))
            }
        },
    )
}

/** Emphasises just the app name inside an already-formatted sentence. */
private fun boldAppName(text: String, appName: String): AnnotatedString {
    val start = text.lastIndexOf(appName)
    if (appName.isEmpty() || start < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(appName) }
        append(text.substring(start + appName.length))
    }
}
