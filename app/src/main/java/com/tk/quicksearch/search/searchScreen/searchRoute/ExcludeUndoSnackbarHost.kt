package com.tk.quicksearch.search.searchScreen.searchRoute

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme

/** Snackbar content with an optional icon and a smaller line of supporting text under the message. */
internal data class UndoSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    val supportingText: String? = null,
    val icon: ImageVector? = Icons.Rounded.VisibilityOff,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val withDismissAction: Boolean = false,
) : SnackbarVisuals

@Composable
internal fun ExcludeUndoSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        snackbar = { data ->
            val visuals = data.visuals
            val undoVisuals = visuals as? UndoSnackbarVisuals
            UndoSnackbar(
                message = rememberHighlightedExcludeMessage(visuals.message),
                supportingText = undoVisuals?.supportingText,
                icon = if (undoVisuals != null) undoVisuals.icon else Icons.Rounded.VisibilityOff,
                actionLabel = visuals.actionLabel,
                onAction = { data.performAction() },
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun UndoSnackbar(
    message: AnnotatedString,
    supportingText: String?,
    icon: ImageVector?,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isDarkTheme = LocalAppIsDarkTheme.current
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.ShapeLarge,
        color = backgroundColor,
        contentColor = contentColor,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            icon?.let { imageVector ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(contentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (supportingText != null) FontWeight.SemiBold else null,
                    color = contentColor,
                )
                supportingText?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                    )
                }
            }
            actionLabel?.let { label ->
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.heightIn(min = 36.dp),
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Bolds the item name in messages like "Maps hidden from results". */
@Composable
private fun rememberHighlightedExcludeMessage(message: String): AnnotatedString {
    val marker = stringResource(R.string.exclude_marker)
    val markerIndex = message.indexOf(marker)
    return if (markerIndex > 0) {
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(message.substring(0, markerIndex))
            }
            append(message.substring(markerIndex))
        }
    } else {
        AnnotatedString(message)
    }
}
