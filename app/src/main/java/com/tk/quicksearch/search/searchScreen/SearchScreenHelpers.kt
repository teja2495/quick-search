package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.core.*
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.reminders.ReminderEditorRequests
import com.tk.quicksearch.searchEngines.AliasParser
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.getId
import com.tk.quicksearch.search.searchScreen.dialogs.NicknameDialogState
import com.tk.quicksearch.search.searchScreen.dialogs.TriggerDialogState
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import java.util.Locale

sealed interface PredictedSubmitTarget {
    data class App(val packageName: String, val userHandleId: Int?) : PredictedSubmitTarget

    data class AppShortcut(val id: String) : PredictedSubmitTarget

    data class Contact(val contactId: Long) : PredictedSubmitTarget

    data class File(val uri: String) : PredictedSubmitTarget

    data class Setting(val id: String) : PredictedSubmitTarget

    data class AppSetting(val id: String) : PredictedSubmitTarget

    data class Calendar(val eventId: Long) : PredictedSubmitTarget

    data class Note(val noteId: Long) : PredictedSubmitTarget

    data class SearchTarget(val targetId: String) : PredictedSubmitTarget
}

internal fun resolvePredictedSubmitTarget(
    query: String,
    firstApp: AppInfo?,
    renderingState: SectionRenderingState,
    enabledTargets: List<SearchTarget>,
    detectedShortcutTarget: SearchTarget?,
    searchTargetsOrder: List<SearchTarget>,
    defaultBrowserPackage: String?,
): PredictedSubmitTarget? {
    val trimmedQuery = query.trim()
    if (firstApp != null) {
        return PredictedSubmitTarget.App(
            packageName = firstApp.packageName,
            userHandleId = firstApp.userHandleId,
        )
    }

    if (trimmedQuery.isNotBlank() && isLikelyWebUrl(trimmedQuery)) {
        val browserTarget = defaultBrowserTarget(searchTargetsOrder, defaultBrowserPackage)
        if (browserTarget != null) {
            return PredictedSubmitTarget.SearchTarget(browserTarget.getId())
        }
    }

    val firstShortcut = renderingState.appShortcutResults.firstOrNull()
    if (firstShortcut != null) {
        return PredictedSubmitTarget.AppShortcut(shortcutKey(firstShortcut))
    }

    val firstContact = renderingState.contactResults.firstOrNull()
    if (firstContact != null) {
        return PredictedSubmitTarget.Contact(firstContact.contactId)
    }

    val firstFile = renderingState.fileResults.firstOrNull()
    if (firstFile != null) {
        return PredictedSubmitTarget.File(firstFile.uri.toString())
    }

    val firstSetting = renderingState.settingResults.firstOrNull()
    if (firstSetting != null) {
        return PredictedSubmitTarget.Setting(firstSetting.id)
    }

    val firstCalendarEvent = renderingState.calendarEvents.firstOrNull()
    if (firstCalendarEvent != null) {
        return PredictedSubmitTarget.Calendar(firstCalendarEvent.eventId)
    }

    val firstNote =
        if (FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES)) {
            renderingState.noteResults.firstOrNull()
        } else {
            null
        }
    if (firstNote != null) {
        return PredictedSubmitTarget.Note(firstNote.noteId)
    }

    val firstAppSetting = renderingState.appSettingResults.firstOrNull()
    if (firstAppSetting != null) {
        return PredictedSubmitTarget.AppSetting(firstAppSetting.id)
    }

    if (detectedShortcutTarget != null) {
        return PredictedSubmitTarget.SearchTarget(detectedShortcutTarget.getId())
    }

    if (trimmedQuery.isBlank()) return null

    val firstEnabledTarget = enabledTargets.firstOrNull() ?: return null
    return PredictedSubmitTarget.SearchTarget(firstEnabledTarget.getId())
}

/** The app that occupies the first visible grid position and can be opened with Done. */
internal fun AppsSectionParams.firstSubmittableGridApp(): AppInfo? {
    if (isSearching) return apps.firstOrNull()

    return when (activeHomeSuggestionTab()) {
        // New/updated apps are informational, and pinned apps are opened deliberately; neither is
        // a keyboard submit target, so Done can't open them by accident.
        AppSuggestionTabType.NEW_UPDATED, AppSuggestionTabType.PINNED -> null
        AppSuggestionTabType.RECENTS -> pinnedAndRecentApps.firstOrNull() ?: apps.firstOrNull()
        AppSuggestionTabType.MOST_USED ->
            mostUsedApps.firstOrNull() ?: pinnedAndRecentApps.firstOrNull() ?: apps.firstOrNull()
        null -> null
    }
}

internal fun AppsSectionParams.isNonSubmittableSuggestionsTab(): Boolean =
    !isSearching &&
        activeHomeSuggestionTab().let {
            it == AppSuggestionTabType.NEW_UPDATED || it == AppSuggestionTabType.PINNED
        }

private fun AppsSectionParams.activeHomeSuggestionTab(): AppSuggestionTabType? {
    if (isSearching) return null

    val visibleTabs =
        buildList {
            if (hasUsagePermission && AppSuggestionTabType.NEW_UPDATED in enabledSuggestionTabs) {
                add(AppSuggestionTabType.NEW_UPDATED)
            }
            val hasPinnedGridItems =
                pinnedApps.isNotEmpty() || pinnedGridAppShortcuts.isNotEmpty() || appFolders.isNotEmpty()
            if (hasPinnedGridItems && AppSuggestionTabType.PINNED in enabledSuggestionTabs) {
                add(AppSuggestionTabType.PINNED)
            }
            if (AppSuggestionTabType.RECENTS in enabledSuggestionTabs) {
                add(AppSuggestionTabType.RECENTS)
            }
            if (hasUsagePermission && AppSuggestionTabType.MOST_USED in enabledSuggestionTabs) {
                add(AppSuggestionTabType.MOST_USED)
            }
        }
    return selectedSuggestionTab.takeIf { it in visibleTabs }
        ?: visibleTabs.firstOrNull { it == AppSuggestionTabType.RECENTS }
        ?: visibleTabs.firstOrNull()
}

internal fun detectSuffixSearchTargetAlias(
    query: String,
    enabledTargets: List<SearchTarget>,
    shortcutCodes: Map<String, String>,
    shortcutEnabled: Map<String, Boolean>,
    requireTrailingSpace: Boolean,
): Pair<String, SearchTarget>? {
    if (query.isBlank()) return null

    val aliases = mutableMapOf<String, SearchTarget>()
    enabledTargets.forEach { target ->
        val targetId = target.getId()
        if (shortcutEnabled[targetId] != true) return@forEach

        val aliasCode = shortcutCodes[targetId].orEmpty().trim()
        if (aliasCode.isEmpty()) return@forEach

        aliases[aliasCode.lowercase(Locale.getDefault())] = target
    }
    if (aliases.isEmpty()) return null

    val match =
        AliasParser.detectSuffixAlias(
            query = query,
            aliases = aliases,
            requireTrailingSpace = requireTrailingSpace,
        ) ?: return null

    return match.queryWithoutAlias to match.target
}

/** Data class for Files section parameters */
data class FilesSectionParams(
    val files: List<DeviceFile>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val pinnedFileUris: Set<String>,
    val onFileClick: (DeviceFile) -> Unit,
    val onOpenFolder: (DeviceFile) -> Unit,
    val onRequestPermission: () -> Unit,
    val onTogglePin: (DeviceFile) -> Unit,
    val onMovePinned: (DeviceFile, Boolean) -> Unit = { _, _ -> },
    val onExclude: (DeviceFile) -> Unit,
    val onExcludeExtension: (DeviceFile) -> Unit,
    val onNicknameClick: (DeviceFile) -> Unit,
    val onTriggerClick: (DeviceFile) -> Unit,
    val getFileNickname: (String) -> String?,
    val getFileTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val permissionDisabledCard:
        (
        @Composable (
            title: String,
            message: String,
            actionLabel: String,
            onActionClick: () -> Unit,
        ) -> Unit
        ),
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Settings section parameters */
data class SettingsSectionParams(
    val settings: List<DeviceSetting>,
    val appSettings: List<AppSettingResult>,
    val isExpanded: Boolean,
    val pinnedSettingIds: Set<String>,
    val onSettingClick: (DeviceSetting) -> Unit,
    val onAppSettingClick: (AppSettingResult) -> Unit,
    val onAppSettingToggle: (AppSettingResult, Boolean) -> Unit,
    val onAppSettingWebSuggestionsCountChange: (Int) -> Unit,
    val isAppSettingToggleChecked: (AppSettingResult) -> Boolean,
    val appSettingWebSuggestionsCount: Int,
    val appSettingPhoneAppGridColumns: Int,
    val onAppSettingPhoneAppGridColumnsChange: (Int) -> Unit,
    val appSettingAppResultRowCount: Int,
    val onAppSettingAppResultRowCountChange: (Int) -> Unit,
    val onTogglePin: (DeviceSetting) -> Unit,
    val onMovePinned: (DeviceSetting, Boolean) -> Unit = { _, _ -> },
    val onExclude: (DeviceSetting) -> Unit,
    val onNicknameClick: (DeviceSetting) -> Unit,
    val onTriggerClick: (DeviceSetting) -> Unit,
    val getSettingNickname: (String) -> String?,
    val getSettingTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val onAppSettingExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for App Shortcuts section parameters */
data class AppShortcutsSectionParams(
    val shortcuts: List<StaticShortcut>,
    val isExpanded: Boolean,
    val pinnedShortcutIds: Set<String>,
    val excludedShortcutIds: Set<String>,
    val onShortcutClick: (StaticShortcut) -> Unit,
    val onTogglePin: (StaticShortcut) -> Unit,
    val onMovePinned: (StaticShortcut, Boolean) -> Unit = { _, _ -> },
    val onDisable: (StaticShortcut) -> Unit,
    val onDisableAllForApp: (StaticShortcut) -> Unit,
    val onAppInfoClick: (StaticShortcut) -> Unit,
    val onNicknameClick: (StaticShortcut) -> Unit,
    val onTriggerClick: (StaticShortcut) -> Unit,
    val onEditCustomShortcut: (StaticShortcut) -> Unit,
    val onEditShortcutIcon: (StaticShortcut) -> Unit,
    val getShortcutNickname: (String) -> String?,
    val getShortcutTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val iconPackPackage: String?,
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Contacts section parameters */
data class ContactsSectionParams(
    val contacts: List<ContactInfo>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val callingApp: CallingApp?,
    val messagingApp: MessagingApp?,
    val pinnedContactIds: Set<Long>,
    val onContactClick: (ContactInfo) -> Unit,
    val onShowContactMethods: (ContactInfo) -> Unit,
    val onCallContact: (ContactInfo) -> Unit,
    val onSmsContact: (ContactInfo) -> Unit,
    val onContactMethodClick: (ContactInfo, com.tk.quicksearch.search.models.ContactMethod) -> Unit,
    val onTogglePin: (ContactInfo) -> Unit,
    val onMovePinned: (ContactInfo, Boolean) -> Unit = { _, _ -> },
    val onExclude: (ContactInfo) -> Unit,
    val onNicknameClick: (ContactInfo) -> Unit,
    val onTriggerClick: (ContactInfo) -> Unit,
    val getContactNickname: (Long) -> String?,
    val getContactTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    val getContactActionTrigger: (Long, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    val getPrimaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    val getSecondaryContactCardAction: (Long) -> com.tk.quicksearch.search.contacts.models.ContactCardAction?,
    val onPrimaryActionLongPress: (ContactInfo) -> Unit,
    val onSecondaryActionLongPress: (ContactInfo) -> Unit,
    val onCustomAction: (ContactInfo, com.tk.quicksearch.search.contacts.models.ContactCardAction) -> Unit,
    val showContactActionHint: Boolean = false,
    val onContactActionHintDismissed: () -> Unit = {},
    val onOpenAppSettings: () -> Unit,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp =
        SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val permissionDisabledCard:
        (
        @Composable (
            title: String,
            message: String,
            actionLabel: String,
            onActionClick: () -> Unit,
        ) -> Unit
        ),
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Apps section parameters */
data class AppsSectionParams(
    val apps: List<AppInfo>,
    val allApps: List<AppInfo>,
    val pinnedAndRecentApps: List<AppInfo>,
    val pinnedApps: List<AppInfo>,
    val newOrUpdatedApps: List<AppInfo>,
    val mostUsedApps: List<AppInfo>,
    val appShortcuts: List<StaticShortcut>,
    val isSearching: Boolean,
    val hasUsagePermission: Boolean,
    val selectedSuggestionTab: AppSuggestionTabType,
    val enabledSuggestionTabs: Set<AppSuggestionTabType>,
    val onSuggestionTabSelected: (AppSuggestionTabType) -> Unit,
    val hasAppResults: Boolean,
    val showAllAppsButton: Boolean,
    val pinnedPackageNames: Set<String>,
    val disabledAppShortcutIds: Set<String>,
    val onAppClick: (AppInfo) -> Unit,
    val onAppShortcutClick: (StaticShortcut) -> Unit,
    val onAppInfoClick: (AppInfo) -> Unit,
    val onUninstallClick: (AppInfo) -> Unit,
    val onHideApp: (AppInfo) -> Unit,
    val onDisableAppShortcut: (StaticShortcut) -> Unit = {},
    val onPinApp: (AppInfo) -> Unit,
    val onUnpinApp: (AppInfo) -> Unit,
    val onReorderPinnedApps: (List<AppInfo>) -> Unit,
    val onNicknameClick: (AppInfo) -> Unit,
    val onTriggerClick: (AppInfo) -> Unit,
    val onOpenInSplitScreen: (AppInfo) -> Unit,
    val getAppNickname: (String) -> String?,
    val getAppTrigger: (String) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    val rowCount: Int,
    val phoneColumnOverride: Int = 5,
    val appIconSizeStep: Int = com.tk.quicksearch.search.data.preferences.UiPreferences.DEFAULT_APP_ICON_SIZE_STEP,
    val iconPackPackage: String?,
    val appIconShape: AppIconShape,
    val themedIconsEnabled: Boolean = true,
    val showAppLabels: Boolean,
    val oneHandedMode: Boolean,
    val isInitializing: Boolean,
    val startupPhase: StartupPhase,
    val isOverlayPresentation: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
    val suppressTopResultIndicator: Boolean = false,
    val showWallpaperBackground: Boolean = false,
    val notificationDotsEnabled: Boolean = false,
    val showRateQuickSearchCard: Boolean = false,
    val onRateQuickSearchClick: () -> Unit = {},
    val onRateQuickSearchNotNowClick: () -> Unit = {},
    val showUpdateCard: Boolean = false,
    val onUpdateClick: () -> Unit = {},
    val onUpdateNotNowClick: () -> Unit = {},
    val onGridAppeared: (() -> Unit)? = null,
    val suppressSuggestionsEnterAnimation: Boolean = false,
    val pinnedGridAppShortcuts: List<StaticShortcut> = emptyList(),
    val pinnedAppGridOrder: List<String> = emptyList(),
    val onReorderPinnedAppGrid: (List<String>, List<AppInfo>, List<StaticShortcut>) -> Unit =
        { _, _, _ -> },
    val pinnedGridShortcutActions: com.tk.quicksearch.search.apps.AppGridShortcutActions? = null,
    val appFolders: List<com.tk.quicksearch.search.folders.AppFolder> = emptyList(),
    val appFolderActions: com.tk.quicksearch.search.folders.AppGridFolderActions? = null,
)

/** Data class for Calendar section parameters */
data class CalendarSectionParams(
    val events: List<CalendarEventInfo>,
    val hasPermission: Boolean,
    val isExpanded: Boolean,
    val pinnedEventIds: Set<Long>,
    val excludedEventIds: Set<Long>,
    val onEventClick: (CalendarEventInfo) -> Unit,
    val onRequestPermission: () -> Unit,
    val onTogglePin: (CalendarEventInfo) -> Unit,
    val onMovePinned: (CalendarEventInfo, Boolean) -> Unit = { _, _ -> },
    val onExclude: (CalendarEventInfo) -> Unit,
    val onInclude: (CalendarEventInfo) -> Unit,
    val onNicknameClick: (CalendarEventInfo) -> Unit,
    val onArchiveTodayEvent: (CalendarEventInfo) -> Unit,
    val getEventNickname: (Long) -> String?,
    val showAllResults: Boolean,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
    val permissionDisabledCard:
        (
        @Composable (
            title: String,
            message: String,
            actionLabel: String,
            onActionClick: () -> Unit,
        ) -> Unit
        ),
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
)

/** Data class for Notes section parameters */
data class NotesSectionParams(
    val pinnedNoteIds: Set<Long>,
    val onNoteClick: (NoteInfo) -> Unit,
    val onTogglePin: (NoteInfo) -> Unit,
    val onMovePinned: (NoteInfo, Boolean) -> Unit = { _, _ -> },
    val onDelete: (NoteInfo) -> Unit,
    val onTriggerClick: (NoteInfo) -> Unit,
    val getNoteTrigger: (Long) -> com.tk.quicksearch.search.data.preferences.ResultTrigger?,
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
    val expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
)

/** Reminder callbacks supplied by the route; defaults keep other callers compiling. */
data class ReminderSectionActions(
    val onPin: (ReminderInfo) -> Unit = {},
    val onUnpin: (ReminderInfo) -> Unit = {},
    val onMovePinned: (ReminderInfo, Boolean) -> Unit = { _, _ -> },
    val onMarkDone: (ReminderInfo) -> Unit = {},
    val onDelete: (ReminderInfo) -> Unit = {},
)

/** Data class for Reminders section parameters */
data class RemindersSectionParams(
    val pinnedReminderIds: Set<Long>,
    val onReminderClick: (ReminderInfo) -> Unit,
    val onTogglePin: (ReminderInfo) -> Unit,
    val onMovePinned: (ReminderInfo, Boolean) -> Unit = { _, _ -> },
    val onMarkDone: (ReminderInfo) -> Unit,
    val onDelete: (ReminderInfo) -> Unit = {},
    val showExpandControls: Boolean,
    val onExpandClick: () -> Unit,
    val showWallpaperBackground: Boolean,
    val predictedTarget: PredictedSubmitTarget? = null,
    val expandedCardMaxHeight: Dp = SearchScreenConstants.EXPANDED_CARD_MAX_HEIGHT,
)

/**
 * True when every character could belong to a calculator expression: digits, whitespace, or one of
 * the operators the number keyboard offers. Deliberately stricter than "contains no letters" so
 * punctuation that only shows up in non-arithmetic queries (a time's colon, a URL's slash-slash)
 * does not offer the number keyboard.
 */
internal fun String.isCalculatorStyleQuery(): Boolean =
        isNotEmpty() && all { it.isDigit() || it.isWhitespace() || it in CALCULATOR_QUERY_CHARS }

internal const val CALCULATOR_QUERY_CHARS = "+-*/×÷()[].,%^"

internal fun String.isPhoneNumberQuery(): Boolean {
    val digits = if (startsWith('+')) drop(1) else this
    return digits.length >= 3 && digits.all(Char::isDigit)
}
