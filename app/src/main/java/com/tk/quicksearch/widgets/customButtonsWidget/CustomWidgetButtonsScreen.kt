package com.tk.quicksearch.widgets.customButtonsWidget

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import com.tk.quicksearch.shared.ui.components.AppBottomSheet
import com.tk.quicksearch.shared.ui.components.dialogTextFieldColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.rounded.Info
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.loadAppIconBase64
import com.tk.quicksearch.search.data.appShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.media.MediaCommand
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.searchEngines.loadCustomIconAsBase64
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetButtonSlotConfig
import com.tk.quicksearch.widgets.widgetConfigScreen.components.WidgetColorPickerDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
fun CustomWidgetButtonsSection(
    state: WidgetPreferences,
    searchViewModel: SearchViewModel,
    maxButtons: Int = WidgetButtonSlotConfig.STANDARD_COUNT,
    onStateChange: (WidgetPreferences) -> Unit,
) {
    val searchState by searchViewModel.uiState.collectAsState()
    val iconPackPackage = searchState.selectedIconPackPackage
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeSlotIndex by remember { mutableStateOf<Int?>(null) }
    var iconEditSlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var folderColourEditSlotIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val pickIconLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val slotIndex = iconEditSlotIndex
        iconEditSlotIndex = null
        if (uri == null || slotIndex == null) return@rememberLauncherForActivityResult
        scope.launch {
            val encoded = withContext(Dispatchers.IO) {
                loadCustomIconAsBase64(context, uri, maxSizePx = 128)
            }
            if (encoded != null) {
                val updatedButtons = state.customButtons.normalizedSlots(maxButtons).toMutableList()
                val existing = updatedButtons.getOrNull(slotIndex) ?: return@launch
                updatedButtons[slotIndex] = existing.withCustomIcon(encoded)
                onStateChange(state.copy(customButtons = updatedButtons))
            }
        }
    }

    val onDismissDialog = {
        activeSlotIndex = null
        searchViewModel.onQueryChange("")
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING),
    ) {
        Text(
            text = stringResource(R.string.widget_custom_buttons_title),
            style = MaterialTheme.typography.titleSmall,
        )

        CustomButtonsRow(
            actions = state.customButtons.normalizedSlots(maxButtons),
            iconPackPackage = iconPackPackage,
            compactMode = maxButtons > WidgetButtonSlotConfig.STANDARD_COUNT,
            onSlotClick = { index -> activeSlotIndex = index },
            onReorder = { reordered ->
                val normalized = reordered.normalizedSlots(maxButtons)
                val hasButtons = normalized.any { it != null }
                val updated =
                    state.copy(
                        customButtons = normalized,
                        showLabel = if (hasButtons) false else state.showLabel,
                    )
                onStateChange(updated)
            },
            onReset = { index ->
                val updatedButtons = state.customButtons.normalizedSlots(maxButtons).toMutableList()
                updatedButtons[index] = null
                val hasButtons = updatedButtons.any { it != null }
                val updated =
                    state.copy(
                        customButtons = updatedButtons,
                        showLabel = if (hasButtons) false else state.showLabel,
                    )
                onStateChange(updated)
            },
            onChangeIcon = { index ->
                iconEditSlotIndex = index
                pickIconLauncher.launch(arrayOf("image/*"))
            },
            onChangeColour = { index -> folderColourEditSlotIndex = index },
        )
    }

    folderColourEditSlotIndex?.let { slotIndex ->
        val folderAction = state.customButtons.normalizedSlots(maxButtons).getOrNull(slotIndex)
        if (folderAction is CustomWidgetButtonAction.File && folderAction.isDirectory) {
            FolderIconColourDialog(
                selectedColour = folderAction.resolvedFolderIconColorArgb(),
                onDismiss = { folderColourEditSlotIndex = null },
                onColourSelected = { colour ->
                    val updatedButtons = state.customButtons.normalizedSlots(maxButtons).toMutableList()
                    updatedButtons[slotIndex] = folderAction.withFolderIconColor(colour.toArgb())
                    onStateChange(state.copy(customButtons = updatedButtons))
                    folderColourEditSlotIndex = null
                },
            )
        } else {
            folderColourEditSlotIndex = null
        }
    }

    val slotIndex = activeSlotIndex
    if (slotIndex != null) {
        CustomWidgetButtonPickerDialog(
            currentAction = state.customButtons.normalizedSlots(maxButtons).getOrNull(slotIndex),
            searchState = searchState,
            iconPackPackage = iconPackPackage,
            onQueryChange = searchViewModel::onQueryChange,
            onDismiss = onDismissDialog,
            onSelect = { action ->
                val updatedButtons = state.customButtons.normalizedSlots(maxButtons).toMutableList()
                updatedButtons[slotIndex] = action
                val updated = state.copy(customButtons = updatedButtons, showLabel = false)
                onStateChange(updated)
                onDismissDialog()
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomButtonsRow(
    actions: List<CustomWidgetButtonAction?>,
    iconPackPackage: String?,
    compactMode: Boolean,
    onSlotClick: (Int) -> Unit,
    onReorder: (List<CustomWidgetButtonAction?>) -> Unit,
    onReset: (Int) -> Unit,
    onChangeIcon: (Int) -> Unit,
    onChangeColour: (Int) -> Unit,
) {
    val view = LocalView.current
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var slotWidthPx by remember { mutableStateOf(0) }
    val slotSize =
        if (compactMode) {
            56.dp
        } else {
            WidgetConfigConstants.CUSTOM_BUTTON_SLOT_SIZE
        }
    val slotIconSize =
        if (compactMode) {
            20.dp
        } else {
            WidgetConfigConstants.CUSTOM_BUTTON_ICON_SIZE
        }
    val slotShape = RoundedCornerShape(if (compactMode) 12.dp else 16.dp)
    if (compactMode) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            actions.chunked(3).forEachIndexed { rowIndex, rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowActions.forEachIndexed { columnIndex, action ->
                        val index = rowIndex * 3 + columnIndex
                        var showMenu by remember { mutableStateOf(false) }
                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(slotSize)
                                    .clip(slotShape)
                                    .combinedClickable(
                                        onClick = { onSlotClick(index) },
                                        onLongClick =
                                            if (action != null) {
                                                { showMenu = true }
                                            } else {
                                                null
                                            },
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            CustomButtonSlotContent(
                                action = action,
                                iconPackPackage = iconPackPackage,
                                compactMode = compactMode,
                                iconSize = slotIconSize,
                                shape = slotShape,
                            )

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                shape = RoundedCornerShape(24.dp),
                                properties = PopupProperties(focusable = false),
                                containerColor = AppColors.DialogBackground,
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.action_change_icon)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Rounded.Image, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onChangeIcon(index)
                                    },
                                )
                                if (action is CustomWidgetButtonAction.File && action.isDirectory) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = stringResource(R.string.widget_custom_button_change_colour),
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Palette,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onChangeColour(index)
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(text = stringResource(R.string.action_remove)) },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onReset(index)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WidgetConfigConstants.CUSTOM_BUTTON_SPACING),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            actions.forEachIndexed { index, action ->
                var showMenu by remember { mutableStateOf(false) }
                val isDragging = draggingIndex == index
                val alpha = if (isDragging) DesignTokens.DragAlpha else 1f

                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(slotSize)
                            .zIndex(if (isDragging) 1f else 0f)
                            .alpha(alpha)
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state =
                                    rememberDraggableState { delta ->
                                        if (draggingIndex == index) {
                                            dragOffset += delta
                                        }
                                    },
                                onDragStarted = {
                                    draggingIndex = index
                                    hapticToggle(view)()
                                },
                                onDragStopped = {
                                    val threshold = slotWidthPx * 0.5f
                                    if (draggingIndex == index && slotWidthPx > 0) {
                                        val shouldSwapRight =
                                            dragOffset > threshold &&
                                                index < actions.lastIndex
                                        val shouldSwapLeft =
                                            dragOffset < -threshold && index > 0
                                        if (shouldSwapRight || shouldSwapLeft) {
                                            val targetIndex =
                                                if (shouldSwapRight) {
                                                    index + 1
                                                } else {
                                                    index - 1
                                                }
                                            val reordered =
                                                actions.toMutableList().apply {
                                                    add(
                                                        targetIndex,
                                                        removeAt(index),
                                                    )
                                                }
                                            onReorder(reordered)
                                        }
                                    }
                                    dragOffset = 0f
                                    draggingIndex = null
                                },
                            ).then(
                                if (isDragging) {
                                    Modifier
                                        .zIndex(1f)
                                        .offset(
                                            x =
                                                with(LocalDensity.current) {
                                                    dragOffset.toDp()
                                                },
                                        )
                                } else {
                                    Modifier
                                },
                            ).clip(slotShape)
                            .combinedClickable(
                                onClick = { onSlotClick(index) },
                                onLongClick =
                                    if (action != null) {
                                        { showMenu = true }
                                    } else {
                                        null
                                    },
                            ).onSizeChanged { slotWidthPx = it.width },
                    contentAlignment = Alignment.Center,
                ) {
                    CustomButtonSlotContent(
                        action = action,
                        iconPackPackage = iconPackPackage,
                        compactMode = compactMode,
                        iconSize = slotIconSize,
                        shape = slotShape,
                    )

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(24.dp),
                        properties = PopupProperties(focusable = false),
                        containerColor = AppColors.DialogBackground,
                    ) {
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.action_change_icon)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.Image, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onChangeIcon(index)
                            },
                        )
                        if (action is CustomWidgetButtonAction.File && action.isDirectory) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.widget_custom_button_change_colour),
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Palette,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onChangeColour(index)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(text = stringResource(R.string.action_remove)) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onReset(index)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderIconColourDialog(
    selectedColour: Int?,
    onDismiss: () -> Unit,
    onColourSelected: (Color) -> Unit,
) {
    WidgetColorPickerDialog(
        initialColor = Color(selectedColour ?: DEFAULT_FOLDER_ICON_COLOR_ARGB),
        title = stringResource(R.string.widget_custom_button_choose_colour),
        onDismiss = onDismiss,
        onConfirm = onColourSelected,
    )
}

private const val DEFAULT_FOLDER_ICON_COLOR_ARGB = 0xFF4F8F74.toInt()

@Composable
private fun CustomButtonSlotContent(
    action: CustomWidgetButtonAction?,
    iconPackPackage: String?,
    compactMode: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    shape: Shape,
) {
    Surface(
        color = Color.Transparent,
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(if (compactMode) 4.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactMode) 6.dp else 4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (action == null) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.widget_custom_button_add_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize),
                )
                Text(
                    text = stringResource(R.string.common_action_add),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            } else {
                CustomWidgetButtonIcon(
                    action = action,
                    iconSize = iconSize,
                    iconPackPackage = iconPackPackage,
                    tintColor = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = action.displayLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
