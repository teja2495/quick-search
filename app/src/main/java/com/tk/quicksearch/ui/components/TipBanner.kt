package com.tk.quicksearch.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Common tip banner component for displaying dismissible tips and hints throughout the app.
 *
 * Supports both plain text and annotated text with links.
 *
 * @param text The text content to display
 * @param annotatedText Optional annotated text with styling/links (takes precedence over text if provided)
 * @param onContentClick Optional click handler for the content area (for plain text)
 * @param onTextClick Optional click handler for annotated text (receives click offset)
 * @param onDismiss Callback when the dismiss button is clicked
 * @param modifier Modifier to be applied to the banner
 * @param textStyle Text style to use (defaults to bodyMedium)
 */
@Composable
fun TipBanner(
    text: String? = null,
    annotatedText: AnnotatedString? = null,
    onContentClick: (() -> Unit)? = null,
    onTextClick: ((Int) -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    showDismissButton: Boolean = true,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = DesignTokens.ShapeXXLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Content area - optionally clickable
            val contentModifier = Modifier.weight(1f).let { mod ->
                if (onContentClick != null && annotatedText == null) {
                    mod.clickable(onClick = onContentClick)
                } else {
                    mod
                }
            }

            if (annotatedText != null) {
                ClickableText(
                    text = annotatedText,
                    style = textStyle.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = contentModifier,
                    onClick = onTextClick ?: { /* No-op */ }
                )
            } else if (text != null) {
                Text(
                    text = text,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = contentModifier
                )
            }

            // Dismiss button (optional)
            if (showDismissButton && onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.desc_close),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}