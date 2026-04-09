package com.tk.quicksearch.widgets.customButtonsWidget

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
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
import androidx.compose.material.icons.rounded.Search
import com.tk.quicksearch.shared.ui.components.AppAlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.loadAppIconBase64
import com.tk.quicksearch.search.data.AppShortcutRepository.rememberShortcutIcon
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants
import com.tk.quicksearch.widgets.utils.WidgetButtonSlotConfig
import kotlinx.coroutines.delay
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
    var activeSlotIndex by remember { mutableStateOf<Int?>(null) }

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
        )
    }

    val slotIndex = activeSlotIndex
    if (slotIndex != null) {
        CustomWidgetButtonDialog(
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
                    text = stringResource(R.string.widget_custom_button_add),
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

@Composable
private fun CustomWidgetButtonDialog(
    currentAction: CustomWidgetButtonAction?,
    searchState: SearchUiState,
    iconPackPackage: String?,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (CustomWidgetButtonAction) -> Unit,
) {
    val context = LocalContext.current
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

    val results =
        remember(searchState, query.text) {
            if (query.text.trim().isEmpty()) {
                emptyList()
            } else {
                buildCustomWidgetSearchResults(searchState)
            }
        }

    AppAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = { Text(text = stringResource(R.string.widget_custom_buttons_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onQueryChange(it.text)
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape = RoundedCornerShape(50.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription =
                                stringResource(R.string.desc_search_icon),
                        )
                    },
                    trailingIcon = {
                        if (query.text.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    query = TextFieldValue("")
                                    onQueryChange("")
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription =
                                        stringResource(R.string.desc_clear_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = dialogTextFieldColors(),
                )

                // Show hint below search bar when there's no query
                if (query.text.isBlank()) {
                    Text(
                        text = stringResource(R.string.widget_custom_buttons_dialog_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max =
                                    WidgetConfigConstants
                                        .CUSTOM_BUTTON_DIALOG_MAX_HEIGHT,
                            ),
                ) {
                    when {
                        query.text.trim().isEmpty() -> {
                            // Empty state - no prompt text needed
                        }

                        results.isEmpty() -> {
                            Text(
                                text =
                                    stringResource(
                                        R.string.widget_custom_buttons_no_results,
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        onClick = {
                                            onSelect(result.toPersistedAction(context))
                                        },
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant,
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
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomWidgetSearchResultRow(
    result: CustomWidgetSearchResult,
    iconPackPackage: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = DesignTokens.SpacingMedium * 2)
                .combinedClickable(onClick = onClick)
                .padding(
                    vertical = DesignTokens.SpacingSmall,
                    horizontal = DesignTokens.SpacingMedium,
                ),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.Top,
    ) {
        CustomWidgetSearchResultIcon(result = result, iconPackPackage = iconPackPackage)

        Text(
            text = result.displayLabel(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.desc_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CustomWidgetSearchResultIcon(
    result: CustomWidgetSearchResult,
    iconPackPackage: String?,
) {
    if (result is CustomWidgetSearchResult.AppShortcut) {
        val iconSize = WidgetConfigConstants.CUSTOM_BUTTON_RESULT_ICON_SIZE
        val iconSizePx = with(LocalDensity.current) { iconSize.roundToPx() }
        val iconBitmap = rememberShortcutIcon(shortcut = result.shortcut, iconSizePx = iconSizePx)
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = result.displayLabel(),
                modifier = Modifier.size(iconSize),
            )
            return
        }
    }

    CustomWidgetButtonIcon(
        action = result.toAction(),
        iconSize = WidgetConfigConstants.CUSTOM_BUTTON_RESULT_ICON_SIZE,
        iconPackPackage = iconPackPackage,
        tintColor = MaterialTheme.colorScheme.secondary,
    )
}

private sealed class CustomWidgetSearchResult {
    data class App(
        val app: AppInfo,
    ) : CustomWidgetSearchResult()

    data class AppShortcut(
        val shortcut: StaticShortcut,
    ) : CustomWidgetSearchResult()

    data class Contact(
        val contact: ContactInfo,
    ) : CustomWidgetSearchResult()

    data class File(
        val file: DeviceFile,
    ) : CustomWidgetSearchResult()

    data class Setting(
        val setting: DeviceSetting,
    ) : CustomWidgetSearchResult()

    fun displayLabel(): String =
        when (this) {
            is App -> {
                app.appName
            }

            is AppShortcut -> {
                val label = shortcut.shortLabel?.takeIf { it.isNotBlank() }
                    ?: shortcut.longLabel?.takeIf { it.isNotBlank() } ?: shortcut.id
                val appName = shortcut.appLabel.takeIf { it.isNotBlank() }
                if (appName != null) "$label ($appName)" else label
            }

            is Contact -> {
                contact.displayName
            }

            is File -> {
                file.displayName
            }

            is Setting -> {
                setting.title
            }
        }

    fun toAction(): CustomWidgetButtonAction =
        when (this) {
            is App -> {
                CustomWidgetButtonAction.App(
                    packageName = app.packageName,
                    appName = app.appName,
                )
            }

            is AppShortcut -> {
                CustomWidgetButtonAction.AppShortcut(
                    packageName = shortcut.packageName,
                    appLabel = shortcut.appLabel,
                    id = shortcut.id,
                    shortLabel = shortcut.shortLabel,
                    longLabel = shortcut.longLabel,
                    iconResId = shortcut.iconResId,
                    iconBase64 = shortcut.iconBase64,
                    enabled = shortcut.enabled,
                    intents = shortcut.intents,
                )
            }

            is Contact -> {
                CustomWidgetButtonAction.Contact(
                    contactId = contact.contactId,
                    lookupKey = contact.lookupKey,
                    displayName = contact.displayName,
                    photoUri = contact.photoUri,
                )
            }

            is File -> {
                CustomWidgetButtonAction.File(
                    uri = file.uri.toString(),
                    displayName = file.displayName,
                    mimeType = file.mimeType,
                    lastModified = file.lastModified,
                    isDirectory = file.isDirectory,
                    relativePath = file.relativePath,
                    volumeName = file.volumeName,
                )
            }

            is Setting -> {
                CustomWidgetButtonAction.Setting(
                    id = setting.id,
                    title = setting.title,
                    description = setting.description,
                    keywords = setting.keywords,
                    action = setting.action,
                    data = setting.data,
                    categories = setting.categories,
                    extras =
                        setting.extras.entries.map { (key, value) ->
                            when (value) {
                                is Boolean -> {
                                    SettingExtra(
                                        key,
                                        SettingExtraType.BOOLEAN,
                                        value.toString(),
                                    )
                                }

                                is Int -> {
                                    SettingExtra(
                                        key,
                                        SettingExtraType.INT,
                                        value.toString(),
                                    )
                                }

                                is Long -> {
                                    SettingExtra(
                                        key,
                                        SettingExtraType.LONG,
                                        value.toString(),
                                    )
                                }

                                else -> {
                                    SettingExtra(
                                        key,
                                        SettingExtraType.STRING,
                                        value.toString(),
                                    )
                                }
                            }
                        },
                    minSdk = setting.minSdk,
                    maxSdk = setting.maxSdk,
                )
            }
        }

    fun toPersistedAction(context: Context): CustomWidgetButtonAction =
        when (this) {
            is AppShortcut -> {
                val iconBase64 =
                    shortcut.iconBase64
                        ?: loadShortcutIconBase64(
                            context = context,
                            packageName = shortcut.packageName,
                            iconResId = shortcut.iconResId,
                        )
                        ?: loadAppIconBase64(context, shortcut.packageName)

                CustomWidgetButtonAction.AppShortcut(
                    packageName = shortcut.packageName,
                    appLabel = shortcut.appLabel,
                    id = shortcut.id,
                    shortLabel = shortcut.shortLabel,
                    longLabel = shortcut.longLabel,
                    iconResId = shortcut.iconResId,
                    iconBase64 = iconBase64,
                    enabled = shortcut.enabled,
                    intents = shortcut.intents,
                )
            }

            else -> toAction()
        }
}

private fun loadShortcutIconBase64(
    context: Context,
    packageName: String,
    iconResId: Int?,
): String? {
    val resId = iconResId ?: return null
    val targetContext = runCatching { context.createPackageContext(packageName, 0) }.getOrNull() ?: return null
    val drawable =
        runCatching {
            targetContext.resources.getDrawable(resId, targetContext.theme)
        }.getOrNull() ?: return null
    val bitmap = runCatching { drawable.toBitmap(width = 96, height = 96) }.getOrNull() ?: return null
    return bitmap.toBase64Png()
}

private fun Bitmap.toBase64Png(): String? =
    runCatching {
        ByteArrayOutputStream().use { output ->
            if (!compress(Bitmap.CompressFormat.PNG, 100, output)) return null
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }.getOrNull()

private fun buildCustomWidgetSearchResults(state: SearchUiState): List<CustomWidgetSearchResult> =
    buildList {
        state.searchResults.forEach { add(CustomWidgetSearchResult.App(it)) }
        state.appShortcutResults.forEach { add(CustomWidgetSearchResult.AppShortcut(it)) }
        state.contactResults.forEach { add(CustomWidgetSearchResult.Contact(it)) }
        state.fileResults.forEach { add(CustomWidgetSearchResult.File(it)) }
        state.settingResults.forEach { add(CustomWidgetSearchResult.Setting(it)) }
    }

private fun CustomWidgetButtonAction.matchesResult(result: CustomWidgetSearchResult): Boolean =
    when (result) {
        is CustomWidgetSearchResult.App -> {
            this is CustomWidgetButtonAction.App && packageName == result.app.packageName
        }

        is CustomWidgetSearchResult.AppShortcut -> {
            this is CustomWidgetButtonAction.AppShortcut &&
                packageName == result.shortcut.packageName &&
                id == result.shortcut.id
        }

        is CustomWidgetSearchResult.Contact -> {
            this is CustomWidgetButtonAction.Contact && contactId == result.contact.contactId
        }

        is CustomWidgetSearchResult.File -> {
            this is CustomWidgetButtonAction.File && uri == result.file.uri.toString()
        }

        is CustomWidgetSearchResult.Setting -> {
            this is CustomWidgetButtonAction.Setting && id == result.setting.id
        }
    }

private fun List<CustomWidgetButtonAction?>.normalizedSlots(maxSlots: Int): List<CustomWidgetButtonAction?> {
    val normalized = take(maxSlots).toMutableList()
    while (normalized.size < maxSlots) {
        normalized.add(null)
    }
    return normalized
}
