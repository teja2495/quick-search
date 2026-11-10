package com.tk.quicksearch.widgets.customButtonsWidget

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.tk.quicksearch.shared.ui.components.AppPickerDrawer
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRowSpacing
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerMessage
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerRow
import com.tk.quicksearch.shared.ui.components.AppPickerDrawerSearch
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
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
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
fun CustomWidgetButtonPickerDialog(
    currentAction: CustomWidgetButtonAction?,
    searchState: SearchUiState,
    iconPackPackage: String?,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelect: (CustomWidgetButtonAction) -> Unit,
    title: String? = null,
    tipText: String? = null,
    // When set, shown inside the search field in place of the hint text below it.
    searchPlaceholder: String? = null,
    // When set and an action is assigned, the tip is replaced by the current action and a Reset button.
    // Reset leaves the picker open; pass the updated (null) [currentAction] to bring the tip back.
    onReset: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var selectedFilter by rememberSaveable { mutableStateOf(CustomWidgetResultFilter.ALL) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { onQueryChange("") }

    // Set once a result is picked or the sheet starts closing. The keyboard can still commit its
    // in-progress text as the field goes away, which would otherwise re-apply the old query.
    var isClosing by remember { mutableStateOf(false) }

    val clearQueryForClose = {
        isClosing = true
        keyboardController?.hide()
        query = TextFieldValue("")
        onQueryChange("")
    }

    val results =
        remember(searchState, query.text, selectedFilter) {
            if (query.text.trim().isEmpty()) {
                emptyList()
            } else {
                buildCustomWidgetSearchResults(searchState, query.text, context).filter(selectedFilter::matches)
            }
        }

    AppPickerDrawer(
        title = title ?: stringResource(R.string.widget_custom_buttons_dialog_title),
        onDismiss = onDismiss,
        search =
            AppPickerDrawerSearch(
                query = query,
                onQueryChange = {
                    if (!isClosing) {
                        query = it
                        onQueryChange(it.text)
                    }
                },
                placeholder = searchPlaceholder,
                autoFocus = true,
            ),
        dismissOnClickOutside = false,
        onDismissStarted = clearQueryForClose,
        header = {
            CustomWidgetResultFilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
            )
        },
    ) {
        when {
            query.text.trim().isEmpty() -> {
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(top = DesignTokens.SpacingSmall),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXLarge),
                ) {
                    if (searchPlaceholder == null) {
                        Text(
                            text = stringResource(R.string.widget_custom_buttons_dialog_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(0.75f),
                        )
                    }
                    if (currentAction != null && onReset != null) {
                        CustomWidgetCurrentActionCard(
                            action = currentAction,
                            iconPackPackage = iconPackPackage,
                            onReset = onReset,
                        )
                    } else tipText?.let {
                        TipBanner(
                            text = it,
                            icon = MaterialSymbols.Info,
                            showDismissButton = false,
                        )
                    }
                }
            }

            results.isEmpty() ->
                AppPickerDrawerMessage(text = stringResource(R.string.widget_custom_buttons_no_results))

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppPickerDrawerRowSpacing)) {
                    itemsIndexed(results) { _, result ->
                        CustomWidgetSearchResultRow(
                            result = result,
                            iconPackPackage = iconPackPackage,
                            isSelected = currentAction?.matchesResult(result) == true,
                            onClick = {
                                val action = result.toPersistedAction(context)
                                clearQueryForClose()
                                onSelect(action)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomWidgetSearchResultRow(
    result: CustomWidgetSearchResult,
    iconPackPackage: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    AppPickerDrawerRow(
        title = result.displayLabel(),
        selected = isSelected,
        onClick = onClick,
        leading = { CustomWidgetSearchResultIcon(result = result, iconPackPackage = iconPackPackage) },
    )
}

private enum class CustomWidgetResultFilter(
    @StringRes val labelRes: Int,
) {
    ALL(R.string.widget_custom_buttons_filter_all),
    APPS(R.string.section_apps),
    APP_SHORTCUTS(R.string.section_app_shortcuts),
    CONTACTS(R.string.contacts_action_button_contacts),
    FILES(R.string.section_files),
    SETTINGS(R.string.section_settings),
    NOTES(R.string.section_notes),
    MEDIA(R.string.section_media),
    ;

    fun matches(result: CustomWidgetSearchResult): Boolean =
        when (this) {
            ALL -> true
            APPS -> result is CustomWidgetSearchResult.App
            APP_SHORTCUTS -> result is CustomWidgetSearchResult.AppShortcut
            CONTACTS -> result is CustomWidgetSearchResult.Contact
            FILES -> result is CustomWidgetSearchResult.File
            SETTINGS -> result is CustomWidgetSearchResult.Setting
            NOTES -> result is CustomWidgetSearchResult.Note
            MEDIA -> result is CustomWidgetSearchResult.Media
        }
}

@Composable
private fun CustomWidgetResultFilterChips(
    selectedFilter: CustomWidgetResultFilter,
    onFilterSelected: (CustomWidgetResultFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        items(CustomWidgetResultFilter.entries) { filter ->
            val selected = filter == selectedFilter
            FilterChip(
                selected = selected,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = stringResource(filter.labelRes)) },
                shape = RoundedCornerShape(50.dp),
                border =
                    FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = AppColors.SettingsDivider,
                        selectedBorderColor = Color.Transparent,
                    ),
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
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

    data class Note(
        val note: NoteInfo,
    ) : CustomWidgetSearchResult()

    data class Media(
        val command: MediaCommand,
        val label: String,
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

            is Note -> {
                note.title
            }

            is Media -> {
                label
            }
        }

    fun toAction(): CustomWidgetButtonAction =
        when (this) {
            is App -> {
                CustomWidgetButtonAction.App(
                    packageName = app.packageName,
                    appName = app.appName,
                    userHandleId = app.userHandleId,
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

            is Note -> {
                CustomWidgetButtonAction.Note(
                    noteId = note.noteId,
                    title = note.title,
                )
            }

            is Media -> {
                CustomWidgetButtonAction.Media(
                    command = command,
                    title = label,
                )
            }
        }

    fun toPersistedAction(context: Context): CustomWidgetButtonAction =
        when (this) {
            is App -> {
                CustomWidgetButtonAction.App(
                    packageName = app.packageName,
                    appName = app.appName,
                    userHandleId = app.userHandleId,
                    // A package-only bitmap would turn a cloned app back into the original icon.
                    // The live icon loader can apply the profile badge for this case instead.
                    customIconBase64 =
                        if (app.userHandleId == null) loadAppIconBase64(context, app.packageName) else null,
                )
            }

            is AppShortcut -> {
                // No app-icon fallback here: the icon loaders fall back at render time, and a
                // persisted app icon would be badged with a second copy of itself.
                val iconBase64 =
                    shortcut.iconBase64
                        ?: loadShortcutIconBase64(
                            context = context,
                            packageName = shortcut.packageName,
                            iconResId = shortcut.iconResId,
                        )

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

private fun buildCustomWidgetSearchResults(
    state: SearchUiState,
    query: String,
    context: Context,
): List<CustomWidgetSearchResult> =
    buildList {
        matchingMediaResults(context, query).forEach { add(it) }
        state.searchResults.forEach { add(CustomWidgetSearchResult.App(it)) }
        state.appShortcutResults.forEach { add(CustomWidgetSearchResult.AppShortcut(it)) }
        state.contactResults.forEach { add(CustomWidgetSearchResult.Contact(it)) }
        state.fileResults.forEach { add(CustomWidgetSearchResult.File(it)) }
        state.settingResults.forEach { add(CustomWidgetSearchResult.Setting(it)) }
        state.noteResults.forEach { add(CustomWidgetSearchResult.Note(it)) }
    }

/**
 * Media transport controls are hardcoded rather than sourced from search, since they are not
 * device content to look up but fixed actions the custom-buttons widget can dispatch.
 */
private fun matchingMediaResults(context: Context, query: String): List<CustomWidgetSearchResult.Media> {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isEmpty()) return emptyList()
    return MediaCommand.entries.mapNotNull { command ->
        val label = context.getString(command.labelRes)
        val haystack = (command.searchKeywords + label.lowercase()).joinToString(" ")
        if (haystack.contains(normalizedQuery)) {
            CustomWidgetSearchResult.Media(command = command, label = label)
        } else {
            null
        }
    }
}

private fun CustomWidgetButtonAction.matchesResult(result: CustomWidgetSearchResult): Boolean =
    when (result) {
        is CustomWidgetSearchResult.App -> {
            this is CustomWidgetButtonAction.App &&
                packageName == result.app.packageName &&
                userHandleId == result.app.userHandleId
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

        is CustomWidgetSearchResult.Note -> {
            this is CustomWidgetButtonAction.Note && noteId == result.note.noteId
        }

        is CustomWidgetSearchResult.Media -> {
            this is CustomWidgetButtonAction.Media && command == result.command
        }
    }

internal fun List<CustomWidgetButtonAction?>.normalizedSlots(maxSlots: Int): List<CustomWidgetButtonAction?> {
    val normalized = take(maxSlots).toMutableList()
    while (normalized.size < maxSlots) {
        normalized.add(null)
    }
    return normalized
}
