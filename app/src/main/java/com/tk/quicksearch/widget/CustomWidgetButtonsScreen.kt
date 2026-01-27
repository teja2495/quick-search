package com.tk.quicksearch.widget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle
import kotlinx.coroutines.delay

@Composable
fun CustomWidgetButtonsSection(
    state: QuickSearchWidgetPreferences,
    searchViewModel: SearchViewModel,
    onStateChange: (QuickSearchWidgetPreferences) -> Unit
) {
    val searchState by searchViewModel.uiState.collectAsState()
    val iconPackPackage = searchState.selectedIconPackPackage
    var activeSlotIndex by remember { mutableStateOf<Int?>(null) }

    val onDismissDialog = {
        activeSlotIndex = null
        searchViewModel.onQueryChange("")
    }

    Column(verticalArrangement = Arrangement.spacedBy(WidgetConfigConstants.COLOR_SECTION_SPACING)) {
        Text(
            text = stringResource(R.string.widget_custom_buttons_title),
            style = MaterialTheme.typography.titleSmall
        )

        CustomButtonsRow(
            actions = state.customButtons.normalizedSlots(),
            iconPackPackage = iconPackPackage,
            onSlotClick = { index -> activeSlotIndex = index },
            onReorder = { reordered ->
                val normalized = reordered.normalizedSlots()
                val hasButtons = normalized.any { it != null }
                val updated = state.copy(
                    customButtons = normalized,
                    showLabel = if (hasButtons) false else state.showLabel
                )
                onStateChange(updated)
            },
            onReset = { index ->
                val updatedButtons = state.customButtons.normalizedSlots().toMutableList()
                updatedButtons[index] = null
                val hasButtons = updatedButtons.any { it != null }
                val updated = state.copy(
                    customButtons = updatedButtons,
                    showLabel = if (hasButtons) false else state.showLabel
                )
                onStateChange(updated)
            }
        )
    }

    val slotIndex = activeSlotIndex
    if (slotIndex != null) {
        CustomWidgetButtonDialog(
            currentAction = state.customButtons.normalizedSlots().getOrNull(slotIndex),
            searchState = searchState,
            iconPackPackage = iconPackPackage,
            onQueryChange = searchViewModel::onQueryChange,
            onDismiss = onDismissDialog,
            onSelect = { action ->
                val updatedButtons = state.customButtons.normalizedSlots().toMutableList()
                updatedButtons[slotIndex] = action
                val updated = state.copy(
                    customButtons = updatedButtons,
                    showLabel = false
                )
                onStateChange(updated)
                onDismissDialog()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomButtonsRow(
    actions: List<CustomWidgetButtonAction?>,
    iconPackPackage: String?,
    onSlotClick: (Int) -> Unit,
    onReorder: (List<CustomWidgetButtonAction?>) -> Unit,
    onReset: (Int) -> Unit
) {
    val view = LocalView.current
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var slotWidthPx by remember { mutableStateOf(0) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(WidgetConfigConstants.CUSTOM_BUTTON_SPACING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            var showMenu by remember { mutableStateOf(false) }
            val isDragging = draggingIndex == index
            val alpha = if (isDragging) DesignTokens.DragAlpha else 1f

            Box(
                modifier = Modifier
                    .size(WidgetConfigConstants.CUSTOM_BUTTON_SLOT_SIZE)
                    .zIndex(if (isDragging) 1f else 0f)
                    .alpha(alpha)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
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
                                val shouldSwapRight = dragOffset > threshold && index < actions.lastIndex
                                val shouldSwapLeft = dragOffset < -threshold && index > 0
                                if (shouldSwapRight || shouldSwapLeft) {
                                    val targetIndex = if (shouldSwapRight) index + 1 else index - 1
                                    val reordered = actions.toMutableList().apply {
                                        add(targetIndex, removeAt(index))
                                    }
                                    onReorder(reordered)
                                }
                            }
                            dragOffset = 0f
                            draggingIndex = null
                        }
                    )
                    .then(
                        if (isDragging) {
                            Modifier
                                .zIndex(1f)
                                .offset(x = with(LocalDensity.current) { dragOffset.toDp() })
                        } else {
                            Modifier
                        }
                    )
                    .combinedClickable(
                        onClick = { onSlotClick(index) },
                        onLongClick = if (action != null) {
                            { showMenu = true }
                        } else {
                            null
                        }
                    )
                    .onSizeChanged { slotWidthPx = it.width },
                contentAlignment = Alignment.Center
            ) {
                CustomButtonSlotContent(
                    action = action,
                    iconPackPackage = iconPackPackage
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.widget_custom_button_reset)) },
                        onClick = {
                            showMenu = false
                            onReset(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomButtonSlotContent(
    action: CustomWidgetButtonAction?,
    iconPackPackage: String?
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .size(WidgetConfigConstants.CUSTOM_BUTTON_SLOT_SIZE)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            if (action == null) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.widget_custom_button_add_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(WidgetConfigConstants.CUSTOM_BUTTON_ICON_SIZE)
                )
            } else {
                CustomWidgetButtonIcon(
                    action = action,
                    iconSize = WidgetConfigConstants.CUSTOM_BUTTON_ICON_SIZE,
                    iconPackPackage = iconPackPackage,
                    tintColor = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun CustomWidgetButtonDialog(
    currentAction: CustomWidgetButtonAction?,
    searchState: SearchUiState,
    iconPackPackage: String?,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (CustomWidgetButtonAction) -> Unit
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        onQueryChange("")
        delay(50)
        focusRequester.requestFocus()
        keyboardController?.show()
        query = query.copy(selection = TextRange(query.text.length))
    }

    val results = remember(searchState, query.text) {
        if (query.text.trim().length < 2) {
            emptyList()
        } else {
            buildCustomWidgetSearchResults(searchState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.widget_custom_buttons_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChange(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(24.dp),
                    placeholder = { Text(text = stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.desc_search_icon)
                        )
                    },
                    trailingIcon = {
                        if (query.text.isNotBlank()) {
                            IconButton(onClick = {
                                query = TextFieldValue("")
                                onQueryChange("")
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.desc_clear_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = WidgetConfigConstants.CUSTOM_BUTTON_DIALOG_MAX_HEIGHT)
                ) {
                    when {
                        query.text.trim().length < 2 -> {
                            // Empty state - no prompt text needed
                        }
                        results.isEmpty() -> {
                            Text(
                                text = stringResource(R.string.widget_custom_buttons_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            LazyColumn {
                                items(results) { result ->
                                    val isSelected =
                                        currentAction?.matchesResult(result) == true
                                    CustomWidgetSearchResultRow(
                                        result = result,
                                        iconPackPackage = iconPackPackage,
                                        isSelected = isSelected,
                                        onClick = { onSelect(result.toAction()) }
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomWidgetSearchResultRow(
    result: CustomWidgetSearchResult,
    iconPackPackage: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick)
            .padding(vertical = DesignTokens.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomWidgetButtonIcon(
            action = result.toAction(),
            iconSize = WidgetConfigConstants.CUSTOM_BUTTON_RESULT_ICON_SIZE,
            iconPackPackage = iconPackPackage,
            tintColor = MaterialTheme.colorScheme.secondary
        )

        Text(
            text = result.displayLabel(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.desc_selected),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private sealed class CustomWidgetSearchResult {
    data class App(val app: AppInfo) : CustomWidgetSearchResult()
    data class AppShortcut(val shortcut: StaticShortcut) : CustomWidgetSearchResult()
    data class Contact(val contact: ContactInfo) : CustomWidgetSearchResult()
    data class File(val file: DeviceFile) : CustomWidgetSearchResult()
    data class Setting(val setting: DeviceSetting) : CustomWidgetSearchResult()

    fun displayLabel(): String = when (this) {
        is App -> app.appName
        is AppShortcut -> shortcut.shortLabel?.takeIf { it.isNotBlank() }
            ?: shortcut.longLabel?.takeIf { it.isNotBlank() }
            ?: shortcut.id
        is Contact -> contact.displayName
        is File -> file.displayName
        is Setting -> setting.title
    }

    fun toAction(): CustomWidgetButtonAction = when (this) {
        is App -> CustomWidgetButtonAction.App(
            packageName = app.packageName,
            appName = app.appName
        )
        is AppShortcut -> CustomWidgetButtonAction.AppShortcut(
            packageName = shortcut.packageName,
            appLabel = shortcut.appLabel,
            id = shortcut.id,
            shortLabel = shortcut.shortLabel,
            longLabel = shortcut.longLabel,
            iconResId = shortcut.iconResId,
            enabled = shortcut.enabled,
            intents = shortcut.intents
        )
        is Contact -> CustomWidgetButtonAction.Contact(
            contactId = contact.contactId,
            lookupKey = contact.lookupKey,
            displayName = contact.displayName,
            photoUri = contact.photoUri
        )
        is File -> CustomWidgetButtonAction.File(
            uri = file.uri.toString(),
            displayName = file.displayName,
            mimeType = file.mimeType,
            lastModified = file.lastModified,
            isDirectory = file.isDirectory,
            relativePath = file.relativePath,
            volumeName = file.volumeName
        )
        is Setting -> CustomWidgetButtonAction.Setting(
            id = setting.id,
            title = setting.title,
            description = setting.description,
            keywords = setting.keywords,
            action = setting.action,
            data = setting.data,
            categories = setting.categories,
            extras = setting.extras.entries.map { (key, value) ->
                when (value) {
                    is Boolean -> SettingExtra(key, SettingExtraType.BOOLEAN, value.toString())
                    is Int -> SettingExtra(key, SettingExtraType.INT, value.toString())
                    is Long -> SettingExtra(key, SettingExtraType.LONG, value.toString())
                    else -> SettingExtra(key, SettingExtraType.STRING, value.toString())
                }
            },
            minSdk = setting.minSdk,
            maxSdk = setting.maxSdk
        )
    }
}

private fun buildCustomWidgetSearchResults(state: SearchUiState): List<CustomWidgetSearchResult> {
    return buildList {
        state.searchResults.forEach { add(CustomWidgetSearchResult.App(it)) }
        state.appShortcutResults.forEach { add(CustomWidgetSearchResult.AppShortcut(it)) }
        state.contactResults.forEach { add(CustomWidgetSearchResult.Contact(it)) }
        state.fileResults.forEach { add(CustomWidgetSearchResult.File(it)) }
        state.settingResults.forEach { add(CustomWidgetSearchResult.Setting(it)) }
    }
}

private fun CustomWidgetButtonAction.matchesResult(result: CustomWidgetSearchResult): Boolean {
    return when (result) {
        is CustomWidgetSearchResult.App -> this is CustomWidgetButtonAction.App &&
            packageName == result.app.packageName
        is CustomWidgetSearchResult.AppShortcut ->
            this is CustomWidgetButtonAction.AppShortcut &&
                packageName == result.shortcut.packageName &&
                id == result.shortcut.id
        is CustomWidgetSearchResult.Contact ->
            this is CustomWidgetButtonAction.Contact && contactId == result.contact.contactId
        is CustomWidgetSearchResult.File ->
            this is CustomWidgetButtonAction.File && uri == result.file.uri.toString()
        is CustomWidgetSearchResult.Setting ->
            this is CustomWidgetButtonAction.Setting && id == result.setting.id
    }
}

private fun List<CustomWidgetButtonAction?>.normalizedSlots(): List<CustomWidgetButtonAction?> {
    val normalized = take(2).toMutableList()
    while (normalized.size < 2) {
        normalized.add(null)
    }
    return normalized
}
