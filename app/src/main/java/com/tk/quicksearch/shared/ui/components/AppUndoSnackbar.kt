package com.tk.quicksearch.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalAppIsDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Snackbar content with an optional icon and a smaller line of supporting text under the message. */
data class UndoSnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    val supportingText: String? = null,
    val icon: ImageVector? = MaterialSymbols.VisibilityOff,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val withDismissAction: Boolean = false,
) : SnackbarVisuals

/**
 * Shows [UndoSnackbarVisuals] on a [SnackbarHostState]. A new snackbar replaces the visible one right
 * away instead of queueing behind it, and the undo callback runs only when Undo is tapped.
 */
class UndoSnackbarController internal constructor(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
    private val undoLabel: String,
) {
    private var job: Job? = null

    fun show(
        visuals: UndoSnackbarVisuals,
        onUndo: () -> Unit,
    ) {
        job?.cancel()
        job =
            scope.launch {
                if (hostState.showSnackbar(visuals) == SnackbarResult.ActionPerformed) onUndo()
            }
    }

    fun show(
        message: String,
        icon: ImageVector? = MaterialSymbols.VisibilityOff,
        onUndo: () -> Unit,
    ) {
        show(UndoSnackbarVisuals(message = message, actionLabel = undoLabel, icon = icon), onUndo)
    }

    /** Builds visuals with the Undo label, for callers that need supporting text. */
    fun visuals(
        message: String,
        supportingText: String? = null,
        icon: ImageVector? = MaterialSymbols.VisibilityOff,
    ) = UndoSnackbarVisuals(message = message, actionLabel = undoLabel, supportingText = supportingText, icon = icon)
}

@Composable
fun rememberUndoSnackbarController(hostState: SnackbarHostState = remember { SnackbarHostState() }): UndoSnackbarController {
    val scope = rememberCoroutineScope()
    val undoLabel = stringResource(R.string.action_undo)
    return remember(hostState, scope, undoLabel) { UndoSnackbarController(hostState, scope, undoLabel) }
}

/** The app's undo snackbar: icon badge, message, optional supporting text and an Undo button. */
@Composable
fun AppUndoSnackbarHost(
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
                icon = if (undoVisuals != null) undoVisuals.icon else MaterialSymbols.VisibilityOff,
                actionLabel = visuals.actionLabel,
                onAction = { data.performAction() },
            )
        },
        modifier = modifier,
    )
}

/**
 * [AppUndoSnackbarHost] in a popup pinned to the bottom of the window, for screens with no root
 * overlay to place it in (settings pages). Composed only while a snackbar is showing.
 */
@Composable
fun AppUndoSnackbarPopup(hostState: SnackbarHostState) {
    if (hostState.currentSnackbarData == null) return
    Popup(
        popupPositionProvider = WindowBottomPositionProvider,
        properties = PopupProperties(focusable = false),
    ) {
        AppUndoSnackbarHost(
            hostState = hostState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingLarge,
                        bottom = DesignTokens.SpacingHuge,
                    ),
        )
    }
}

private object WindowBottomPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset =
        IntOffset(
            x = (windowSize.width - popupContentSize.width) / 2,
            y = windowSize.height - popupContentSize.height,
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

/**
 * Shows the route's undo snackbar with [message] and an optional leading icon, running the callback on
 * Undo. Null where no screen provides it (outside the search route).
 */
val LocalShowUndoSnackbar =
    compositionLocalOf<((message: String, icon: ImageVector?, onUndo: () -> Unit) -> Unit)?> { null }
