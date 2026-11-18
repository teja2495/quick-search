package com.tk.quicksearch.search.appSettings

import com.tk.quicksearch.app.navigation.AppSettingsDestinationHandlers
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.settings.settingsDetailScreen.GestureSettingTarget
import com.tk.quicksearch.settings.settingsDetailScreen.NotesNavigationMemory
import com.tk.quicksearch.settings.settingsDetailScreen.SettingsDetailType

/** A searchable Settings row's destination. */
sealed interface AppSettingsTarget {
    /** Navigation and commands that the app or overlay host can handle. */
    sealed interface Host : AppSettingsTarget

    data class Page(val detailType: SettingsDetailType) : Host

    data class Gesture(val gesture: GestureSettingTarget) : AppSettingsTarget
}

/** Actions whose callbacks are supplied by both the app and overlay hosts. */
enum class AppSettingsAction(internal val perform: (AppSettingsDestinationHandlers) -> Unit) : AppSettingsTarget.Host {
    RELOAD_APPS({ it.onReloadApps() }),
    RELOAD_CONTACTS({ it.onReloadContacts() }),
    RELOAD_FILES({ it.onReloadFiles() }),
    SEND_FEEDBACK({ it.onSendFeedback() }),
    RATE_QUICK_SEARCH({ it.onRateQuickSearch() }),
    DEVELOPMENT({ it.onOpenDevelopmentPage() }),
    SET_DEFAULT_ASSISTANT({ it.onSetDefaultAssistant() }),
    SET_DEFAULT_LAUNCHER({ it.onSetDefaultLauncher() }),
    ADD_HOME_SCREEN_WIDGET({ it.onAddHomeScreenWidget() }),
    ADD_QUICK_SETTINGS_TILE({ it.onAddQuickSettingsTile() }),
    CREATE_NOTE({
        NotesNavigationMemory.setPendingNoteId(null)
        it.onOpenSettingsDetail(SettingsDetailType.NOTE_EDITOR)
    }),
    CREATE_NOTIFICATION_NOTE({
        NotesNavigationMemory.setPendingNoteId(null, pinToNotification = true)
        it.onOpenSettingsDetail(SettingsDetailType.NOTE_EDITOR)
    }),
    CREATE_REMINDER({ ReminderEditorRequests.openNew() }),
}

/** Opens UI owned by the current search presentation. */
enum class AppSettingsRouteAction : AppSettingsTarget {
    ICON_PACKS,
    SEARCH_RESULT_RANKING,
    TOP_MATCHES_PRIORITY,
    APP_SUGGESTION_TABS,
    LAUNCHER_ICON,
    PINNED_SECTIONS_ORDER,
    APP_NOTIFICATIONS,
    OPEN_EVENTS_IN,
    PINNED_NOTIFICATION_ITEMS,
    AI_MODEL,
    EXPORT_SETTINGS,
    IMPORT_SETTINGS,
    APP_LANGUAGE,
    RELEASE_NOTES,
}
