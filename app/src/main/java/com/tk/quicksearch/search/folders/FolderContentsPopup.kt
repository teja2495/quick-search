package com.tk.quicksearch.search.folders

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.theme.AppMotion
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetColorPickerDialog
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
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
import com.tk.quicksearch.shared.ui.theme.AppShapes

private val GridBottomPadding = DesignTokens.SpacingSmall
// Always reserved under the grid, so showing the remove zone never resizes the popup.
private val RemoveZoneSpace = 48.dp
// AppBottomPopup's padding below its content card.
private val PopupBottomPadding = 24.dp
// Covers the opened space and everything below it, down to the popup's bottom edge.
private val RemoveZoneHeight = RemoveZoneSpace + GridBottomPadding + PopupBottomPadding
private const val RemoveZoneFadeMillis = AppMotion.DurationExit
private val ColorSwatchSize = 24.dp
// Muted pastel hues, so the unset swatch hints at color without shouting.
private val UnsetSwatchHues =
        listOf(0f, 60f, 120f, 180f, 240f, 300f, 0f).map { Color.hsv(it, 0.3f, 0.85f) }

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
        actions: AppGridFolderActions,
        onDismiss: () -> Unit,
        content: @Composable (removeZone: FolderRemoveZoneState, dismiss: () -> Unit) -> Unit,
) {
    val removeZone = remember(folder.id) { FolderRemoveZoneState() }
    val nameState = rememberSaveable(folder.id, saver = TextFieldState.Saver) { TextFieldState(folder.name) }
    val currentName by rememberUpdatedState(nameState.text.toString())
    val currentOnRename by rememberUpdatedState<(String) -> Unit> { actions.onRenameFolder(folder.id, it) }
    val focusManager = LocalFocusManager.current
    DisposableEffect(folder.id) { onDispose { currentOnRename(currentName) } }
    var showColorPicker by remember { mutableStateOf(false) }
    val dialogBackground = folder.color?.let { folderColorPopupBackground(it) } ?: AppColors.DialogBackground

    AppBottomPopup(
            onDismiss = onDismiss,
            // Keeps the name field above the keyboard.
            modifier = Modifier.imePadding(),
            containerColor = dialogBackground,
            contentCardColor = dialogBackground,
            contentSpacing = DesignTokens.SpacingSmall,
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
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                ) {
                    FolderNameField(
                            state = nameState,
                            onDone = {
                                currentOnRename(nameState.text.toString())
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.weight(1f),
                    )
                    FolderColorSwatch(
                            color = folder.color,
                            onClick = { showColorPicker = true },
                    )
                }
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

    if (showColorPicker) {
        WidgetColorPickerDialog(
                initialColor = folder.color?.let(::Color) ?: AppColors.Accent,
                onDismiss = { showColorPicker = false },
                onConfirm = { color ->
                    actions.onSetFolderColor(folder.id, color.toArgb())
                    showColorPicker = false
                },
                title = stringResource(R.string.folder_color_title),
                onReset = {
                    actions.onSetFolderColor(folder.id, null)
                    showColorPicker = false
                },
        )
    }
}

/** A bare, left-aligned name field: just a faint hint, with no background or border. */
@Composable
private fun FolderNameField(
        state: TextFieldState,
        onDone: () -> Unit,
        modifier: Modifier = Modifier,
) {
    val textStyle =
            MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
            )
    BasicTextField(
            state = state,
            modifier = modifier,
            lineLimits = TextFieldLineLimits.SingleLine,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions =
                    KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                    ),
            onKeyboardAction = { onDone() },
            decorator = { innerTextField ->
                Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                ) {
                    if (state.text.isEmpty()) {
                        Text(
                                text = stringResource(R.string.folder_name_placeholder),
                                style =
                                        textStyle.copy(
                                                color =
                                                        MaterialTheme.colorScheme.onSurface
                                                                .copy(alpha = AppColors.ContentSecondaryAlpha),
                                        ),
                        )
                    }
                    innerTextField()
                }
            },
    )
}

/** Circular swatch opening the color picker: the picked color, or a hue ring when unset. */
@Composable
private fun FolderColorSwatch(
        color: Int?,
        onClick: () -> Unit,
) {
    val description = stringResource(R.string.folder_color_title)
    Box(
            modifier =
                    Modifier.size(DesignTokens.MinTouchTarget)
                            .clip(AppShapes.full)
                            .clickable(role = Role.Button, onClick = onClick)
                            .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
    ) {
        Box(
                modifier =
                        Modifier.size(ColorSwatchSize)
                                .clip(AppShapes.full)
                                .background(
                                        if (color != null) {
                                            SolidColor(Color(color))
                                        } else {
                                            Brush.sweepGradient(UnsetSwatchHues)
                                        },
                                        alpha = if (color != null) 1f else AppColors.ContentSecondaryAlpha,
                                )
                                .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = AppColors.ContentMutedAlpha),
                                        shape = AppShapes.full,
                                ),
        )
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
                    animationSpec = tween(RemoveZoneFadeMillis, easing = AppMotion.EmphasizedEasing),
                    label = "folderRemoveZoneVisibility",
            )
    val glowAlpha by
            animateFloatAsState(
                    targetValue =
                            if (state.isHovered) AppColors.TintEmphasisAlpha else AppColors.TintTonalAlpha,
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
                color = errorColor,
        )
    }
}
