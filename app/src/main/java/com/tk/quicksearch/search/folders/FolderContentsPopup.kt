package com.tk.quicksearch.search.folders

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppBottomPopup
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.LocalHomeTextColorOverride
import com.tk.quicksearch.shared.ui.theme.LocalImageBackgroundIsDark

private val GridBottomPadding = DesignTokens.SpacingSmall
// Always reserved under the grid, so showing the remove zone never resizes the popup.
private val RemoveZoneSpace = 48.dp
// AppBottomPopup's padding below its content card.
private val PopupBottomPadding = 24.dp
// Covers the opened space and everything below it, down to the popup's bottom edge.
private val RemoveZoneHeight = RemoveZoneSpace + GridBottomPadding + PopupBottomPadding
private const val RemoveZoneFadeMillis = 150
private const val RemoveZoneGlowAlpha = 0.16f
private const val RemoveZoneHoveredGlowAlpha = 0.32f

/** The popup's "Remove from folder" drop zone, shown while a member is held or dragged. */
@Stable
internal class FolderRemoveZoneState {
    var isActive by mutableStateOf(false)
    var isHovered by mutableStateOf(false)
    internal var boundsInRoot: Rect? = null

    fun contains(rootPosition: Offset): Boolean = boundsInRoot?.contains(rootPosition) == true
}

/**
 * Opened folder, in the same popup as an app's long-press menu: an editable name, saved trimmed on
 * done or dismiss, above the members grid rendered by [content]. Holding a member shows a remove
 * zone along the popup's bottom edge. `dismiss` closes the popup, e.g. before launching.
 */
@Composable
internal fun FolderContentsPopup(
        folder: ResolvedAppFolder,
        onRename: (String) -> Unit,
        onDismiss: () -> Unit,
        content: @Composable (removeZone: FolderRemoveZoneState, dismiss: () -> Unit) -> Unit,
) {
    val removeZone = remember(folder.id) { FolderRemoveZoneState() }
    var name by rememberSaveable(folder.id) { mutableStateOf(folder.name) }
    val currentName by rememberUpdatedState(name)
    val currentOnRename by rememberUpdatedState(onRename)
    val focusManager = LocalFocusManager.current
    DisposableEffect(folder.id) { onDispose { currentOnRename(currentName) } }
    val dialogBackground = AppColors.DialogBackground

    AppBottomPopup(
            onDismiss = onDismiss,
            // Keeps the name field above the keyboard.
            modifier = Modifier.imePadding(),
            containerColor = dialogBackground,
            contentCardColor = dialogBackground,
            contentSpacing = DesignTokens.SpacingSmall,
            headerSpacing = DesignTokens.SpacingMedium,
            contentTopPadding = 0.dp,
            contentBottomPadding = 0.dp,
            contentHorizontalPadding = 4.dp,
            showCloseButton = false,
            bottomOverlay = {
                FolderRemoveZone(
                        state = removeZone,
                        modifier = Modifier.align(Alignment.BottomCenter),
                )
            },
            title = {
                // A bare name field: just the hint, with no background or border.
                val textStyle =
                        MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                        )
                BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = textStyle,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions =
                                KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Done,
                                ),
                        keyboardActions =
                                KeyboardActions(
                                        onDone = {
                                            currentOnRename(name)
                                            focusManager.clearFocus()
                                        },
                                ),
                        decorationBox = { innerTextField ->
                            Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                            ) {
                                if (name.isEmpty()) {
                                    Text(
                                            text = stringResource(R.string.folder_name_placeholder),
                                            style =
                                                    textStyle.copy(
                                                            color =
                                                                    MaterialTheme.colorScheme
                                                                            .onSurfaceVariant,
                                                    ),
                                    )
                                }
                                innerTextField()
                            }
                        },
                )
            },
    ) {
        // Labels sit on the popup, not on the wallpaper.
        CompositionLocalProvider(
                LocalImageBackgroundIsDark provides null,
                LocalHomeTextColorOverride provides null,
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .padding(top = DesignTokens.SpacingSmall, bottom = GridBottomPadding),
            ) {
                content(removeZone, onDismiss)
                Spacer(Modifier.height(RemoveZoneSpace))
            }
        }
    }
}

/**
 * A subtle red glow over the popup's bottom edge, reaching its sides and bottom, while a member is
 * held or dragged. It covers the space reserved under the grid, so it never hides a member.
 */
@Composable
private fun FolderRemoveZone(
        state: FolderRemoveZoneState,
        modifier: Modifier = Modifier,
) {
    val visibility by
            animateFloatAsState(
                    targetValue = if (state.isActive) 1f else 0f,
                    animationSpec = tween(RemoveZoneFadeMillis),
                    label = "folderRemoveZoneVisibility",
            )
    val glowAlpha by
            animateFloatAsState(
                    targetValue =
                            if (state.isHovered) RemoveZoneHoveredGlowAlpha else RemoveZoneGlowAlpha,
                    label = "folderRemoveZoneGlow",
            )
    val errorColor = MaterialTheme.colorScheme.error
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .height(RemoveZoneHeight)
                            .onGloballyPositioned { state.boundsInRoot = it.boundsInRoot() }
                            .graphicsLayer { alpha = visibility }
                            .background(
                                    Brush.verticalGradient(
                                            listOf(Color.Transparent, errorColor.copy(alpha = glowAlpha)),
                                    ),
                            ),
            contentAlignment = Alignment.Center,
    ) {
        Text(
                text = stringResource(R.string.folder_remove_from_folder),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (state.isHovered) FontWeight.SemiBold else FontWeight.Medium,
                color = errorColor,
        )
    }
}
