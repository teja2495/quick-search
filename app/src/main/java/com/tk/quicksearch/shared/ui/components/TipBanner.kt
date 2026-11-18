package com.tk.quicksearch.shared.ui.components

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Common tip banner component for displaying dismissible tips and hints throughout the app.
 *
 * Supports both plain text and annotated text with links.
 *
 * @param text The text content to display
 * @param annotatedText Optional annotated text with styling/links (takes precedence over text if provided)
 * @param icon Optional leading icon to display before the text
 * @param onContentClick Optional click handler for the content area (for plain text)
 * @param onContentLongClick Optional long-press handler for the content area
 * @param onDismiss Callback when the dismiss button is clicked
 * @param actionIcon Optional trailing action icon
 * @param actionContentDescription Content description for the trailing action icon
 * @param onActionClick Callback when the trailing action icon is clicked
 * @param modifier Modifier to be applied to the banner
 * @param textStyle Text style to use (defaults to bodyMedium)
 */
@Composable
fun TipBanner(
    text: String? = null,
    annotatedText: AnnotatedString? = null,
    icon: ImageVector? = null,
    onContentClick: (() -> Unit)? = null,
    onContentLongClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionContentDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
    showDismissButton: Boolean = true,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Card(
        modifier = modifier.fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = AppColors.ContentFaintAlpha),
                shape = AppShapes.extraLarge,
            ),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = AppColors.TintSubtleAlpha),
            ),
        shape = AppShapes.extraLarge,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingLarge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(DesignTokens.SpacingMedium))
            }

            val contentModifier =
                Modifier.weight(1f).let { mod ->
                    val withLongClick =
                        if (onContentLongClick != null) {
                            mod.pointerInput(Unit) {
                                detectTapGestures(onLongPress = { onContentLongClick.invoke() })
                            }
                        } else mod
                    if (onContentClick != null) {
                        withLongClick.clickable(role = Role.Button, onClick = onContentClick)
                    } else {
                        withLongClick
                    }
                }

            if (annotatedText != null) {
                // Links are styled spans; the content area's own click handles taps on them.
                Text(
                    text = annotatedText,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = contentModifier,
                )
            } else if (text != null) {
                Text(
                    text = text,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = contentModifier,
                )
            }

            if (actionIcon != null && onActionClick != null) {
                AppIconButton(onClick = onActionClick, tooltip = actionContentDescription) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionContentDescription,
                        tint = AppColors.Accent,
                    )
                }
            } else if (showDismissButton && onDismiss != null) {
                AppIconButton(onClick = onDismiss, tooltip = stringResource(R.string.common_close)) {
                    Icon(
                        imageVector = MaterialSymbols.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = AppColors.Accent,
                    )
                }
            }
        }
    }
}
