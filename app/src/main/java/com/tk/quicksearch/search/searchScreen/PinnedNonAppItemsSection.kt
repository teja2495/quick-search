package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.appShortcuts.AppShortcutRow
import com.tk.quicksearch.search.calendar.CalendarEventRow
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.contacts.utils.ContactCallingAppResolver
import com.tk.quicksearch.search.contacts.utils.ContactMessagingAppResolver
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.deviceSettings.SettingResultRow
import com.tk.quicksearch.search.files.FileResultRow
import com.tk.quicksearch.search.notes.NoteRow
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.searchScreen.LocalOverlayDividerColor
import com.tk.quicksearch.search.searchScreen.LocalOverlayResultCardColor
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

@Composable
internal fun PinnedNonAppItemsSection(
    pinnedItemOrder: List<String>,
    contacts: List<ContactInfo>,
    files: List<DeviceFile>,
    appShortcuts: List<StaticShortcut>,
    settings: List<DeviceSetting>,
    calendarEvents: List<CalendarEventInfo>,
    notes: List<NoteInfo>,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    appShortcutsParams: AppShortcutsSectionParams,
    settingsParams: SettingsSectionParams,
    calendarParams: CalendarSectionParams,
    notesParams: NotesSectionParams,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val orderedItems =
        remember(
            pinnedItemOrder,
            contacts,
            files,
            appShortcuts,
            settings,
            calendarEvents,
            notes,
        ) {
            orderedPinnedNonAppItems(
                pinnedItemOrder = pinnedItemOrder,
                contacts = contacts,
                files = files,
                appShortcuts = appShortcuts,
                settings = settings,
                calendarEvents = calendarEvents,
                notes = notes,
            )
        }

    if (orderedItems.isEmpty()) return

    val overlayCardColor = LocalOverlayResultCardColor.current
    val overlayDividerColor = LocalOverlayDividerColor.current
    val dividerColor = overlayDividerColor
        ?: if (showWallpaperBackground) AppColors.WallpaperDivider else MaterialTheme.colorScheme.outlineVariant

    SearchResultCard(
        modifier = modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
        overlayContainerColor = overlayCardColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.SpacingMedium, vertical = 4.dp),
        ) {
            orderedItems.forEachIndexed { index, item ->
                when (item) {
                    is PinnedNonAppItem.AppShortcut -> {
                        val shortcut = item.shortcut
                        val shortcutId = shortcutKey(shortcut)
                        AppShortcutRow(
                            shortcut = shortcut,
                            isPinned = appShortcutsParams.pinnedShortcutIds.contains(shortcutId),
                            isExcluded = appShortcutsParams.excludedShortcutIds.contains(shortcutId),
                            hasNickname = !appShortcutsParams.getShortcutNickname(shortcutId).isNullOrBlank(),
                            hasTrigger = appShortcutsParams.getShortcutTrigger(shortcutId)?.word?.isNotBlank() == true,
                            onShortcutClick = appShortcutsParams.onShortcutClick,
                            onTogglePin = appShortcutsParams.onTogglePin,
                            onMovePinned = appShortcutsParams.onMovePinned,
                            onExclude = appShortcutsParams.onExclude,
                            onInclude = appShortcutsParams.onInclude,
                            onAppInfoClick = appShortcutsParams.onAppInfoClick,
                            onNicknameClick = appShortcutsParams.onNicknameClick,
                            onTriggerClick = appShortcutsParams.onTriggerClick,
                            onEditCustomShortcut = appShortcutsParams.onEditCustomShortcut,
                            onEditShortcutIcon = appShortcutsParams.onEditShortcutIcon,
                            iconPackPackage = appShortcutsParams.iconPackPackage,
                            showPinnedItemMenu = true,
                        )
                    }

                    is PinnedNonAppItem.Contact -> {
                        val contact = item.contact
                        ContactResultRow(
                            contactInfo = contact,
                            callingApp = ContactCallingAppResolver.resolveCallingAppForContact(
                                contact,
                                contactsParams.callingApp ?: CallingApp.CALL,
                            ),
                            messagingApp = ContactMessagingAppResolver.resolveMessagingAppForContact(
                                contact,
                                contactsParams.messagingApp ?: MessagingApp.MESSAGES,
                            ),
                            primaryAction = contactsParams.getPrimaryContactCardAction(contact.contactId),
                            secondaryAction = contactsParams.getSecondaryContactCardAction(contact.contactId),
                            onContactClick = contactsParams.onContactClick,
                            onShowContactMethods = contactsParams.onShowContactMethods,
                            onCallContact = contactsParams.onCallContact,
                            onSmsContact = contactsParams.onSmsContact,
                            onContactMethodClick = { method -> contactsParams.onContactMethodClick(contact, method) },
                            isPinned = contactsParams.pinnedContactIds.contains(contact.contactId),
                            onTogglePin = contactsParams.onTogglePin,
                            onMovePinned = contactsParams.onMovePinned,
                            onExclude = contactsParams.onExclude,
                            onNicknameClick = contactsParams.onNicknameClick,
                            hasNickname = !contactsParams.getContactNickname(contact.contactId).isNullOrBlank(),
                            onTriggerClick = contactsParams.onTriggerClick,
                            hasTrigger = contactsParams.getContactTrigger(contact.contactId)?.word?.isNotBlank() == true,
                            onPrimaryActionLongPress = contactsParams.onPrimaryActionLongPress,
                            onSecondaryActionLongPress = contactsParams.onSecondaryActionLongPress,
                            onCustomAction = contactsParams.onCustomAction,
                            showPinnedItemMenu = true,
                        )
                    }

                    is PinnedNonAppItem.File -> {
                        val file = item.file
                        val fileUri = file.uri.toString()
                        FileResultRow(
                            deviceFile = file,
                            onClick = filesParams.onFileClick,
                            onOpenFolder = filesParams.onOpenFolder,
                            isPinned = filesParams.pinnedFileUris.contains(fileUri),
                            onTogglePin = filesParams.onTogglePin,
                            onMovePinned = filesParams.onMovePinned,
                            onExclude = filesParams.onExclude,
                            onExcludeExtension = filesParams.onExcludeExtension,
                            onNicknameClick = filesParams.onNicknameClick,
                            hasNickname = !filesParams.getFileNickname(fileUri).isNullOrBlank(),
                            onTriggerClick = filesParams.onTriggerClick,
                            hasTrigger = filesParams.getFileTrigger(fileUri)?.word?.isNotBlank() == true,
                            showPinnedItemMenu = true,
                        )
                    }

                    is PinnedNonAppItem.Setting -> {
                        val setting = item.setting
                        SettingResultRow(
                            shortcut = setting,
                            isPinned = settingsParams.pinnedSettingIds.contains(setting.id),
                            onClick = settingsParams.onSettingClick,
                            onTogglePin = settingsParams.onTogglePin,
                            onMovePinned = settingsParams.onMovePinned,
                            onExclude = settingsParams.onExclude,
                            onNicknameClick = settingsParams.onNicknameClick,
                            hasNickname = !settingsParams.getSettingNickname(setting.id).isNullOrBlank(),
                            onTriggerClick = settingsParams.onTriggerClick,
                            hasTrigger = settingsParams.getSettingTrigger(setting.id)?.word?.isNotBlank() == true,
                            showPinnedItemMenu = true,
                        )
                    }

                    is PinnedNonAppItem.CalendarEvent -> {
                        val event = item.event
                        CalendarEventRow(
                            event = event,
                            isPinned = calendarParams.pinnedEventIds.contains(event.eventId),
                            isExcluded = calendarParams.excludedEventIds.contains(event.eventId),
                            hasNickname = !calendarParams.getEventNickname(event.eventId).isNullOrBlank(),
                            onClick = calendarParams.onEventClick,
                            onTogglePin = calendarParams.onTogglePin,
                            onMovePinned = calendarParams.onMovePinned,
                            onExclude = calendarParams.onExclude,
                            onInclude = calendarParams.onInclude,
                            onNicknameClick = calendarParams.onNicknameClick,
                            isPredicted = false,
                            isHomescreenTodayEvent = false,
                            onArchive = calendarParams.onArchiveTodayEvent,
                            showPinnedItemMenu = true,
                        )
                    }

                    is PinnedNonAppItem.Note -> {
                        val note = item.note
                        NoteRow(
                            note = note,
                            isPinned = notesParams.pinnedNoteIds.contains(note.noteId),
                            onClick = notesParams.onNoteClick,
                            onTogglePin = notesParams.onTogglePin,
                            onMovePinned = notesParams.onMovePinned,
                            onDelete = notesParams.onDelete,
                            onTriggerClick = notesParams.onTriggerClick,
                            hasTrigger = notesParams.getNoteTrigger(note.noteId)?.word?.isNotBlank() == true,
                            isPredicted = false,
                            showPinnedItemMenu = true,
                        )
                    }
                }

                if (index < orderedItems.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = dividerColor,
                    )
                }
            }
        }
    }
}

private sealed class PinnedNonAppItem(
    val key: String,
) {
    class AppShortcut(val shortcut: StaticShortcut) : PinnedNonAppItem("shortcut:${shortcutKey(shortcut)}")
    class Contact(val contact: ContactInfo) : PinnedNonAppItem("contact:${contact.contactId}")
    class File(val file: DeviceFile) : PinnedNonAppItem("file:${file.uri}")
    class Setting(val setting: DeviceSetting) : PinnedNonAppItem("setting:${setting.id}")
    class CalendarEvent(val event: CalendarEventInfo) : PinnedNonAppItem("calendar:${event.eventId}")
    class Note(val note: NoteInfo) : PinnedNonAppItem("note:${note.noteId}")
}

private fun orderedPinnedNonAppItems(
    pinnedItemOrder: List<String>,
    contacts: List<ContactInfo>,
    files: List<DeviceFile>,
    appShortcuts: List<StaticShortcut>,
    settings: List<DeviceSetting>,
    calendarEvents: List<CalendarEventInfo>,
    notes: List<NoteInfo>,
): List<PinnedNonAppItem> {
    val defaultItems =
        buildList {
            addAll(appShortcuts.map { PinnedNonAppItem.AppShortcut(it) })
            addAll(contacts.map { PinnedNonAppItem.Contact(it) })
            addAll(files.map { PinnedNonAppItem.File(it) })
            addAll(calendarEvents.map { PinnedNonAppItem.CalendarEvent(it) })
            addAll(settings.map { PinnedNonAppItem.Setting(it) })
            addAll(notes.map { PinnedNonAppItem.Note(it) })
        }
    if (defaultItems.isEmpty()) return emptyList()

    val itemByKey = defaultItems.associateBy { it.key }
    val orderedKeys = pinnedItemOrder.filter { it in itemByKey }
    val missingKeys = defaultItems.map { it.key }.filterNot { it in orderedKeys }
    return (orderedKeys + missingKeys).mapNotNull(itemByKey::get)
}
