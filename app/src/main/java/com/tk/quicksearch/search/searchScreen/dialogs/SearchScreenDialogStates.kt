package com.tk.quicksearch.search.searchScreen.dialogs

import com.tk.quicksearch.search.contacts.models.ContactCardAction
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo

sealed class NicknameDialogState {
    data class App(
        val app: AppInfo,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class AppShortcut(
        val shortcut: StaticShortcut,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class Contact(
        val contact: ContactInfo,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class File(
        val file: DeviceFile,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class Setting(
        val setting: DeviceSetting,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()

    data class CalendarEvent(
        val event: CalendarEventInfo,
        val currentNickname: String?,
        val itemName: String,
    ) : NicknameDialogState()
}

sealed class TriggerDialogState {
    data class App(
        val app: AppInfo,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class AppShortcut(
        val shortcut: StaticShortcut,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class Contact(
        val contact: ContactInfo,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class ContactAction(
        val contact: ContactInfo,
        val action: ContactCardAction,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class File(
        val file: DeviceFile,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class Setting(
        val setting: DeviceSetting,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()

    data class Note(
        val note: NoteInfo,
        val currentTrigger: com.tk.quicksearch.search.data.preferences.ResultTrigger?,
        val itemName: String,
    ) : TriggerDialogState()
}
