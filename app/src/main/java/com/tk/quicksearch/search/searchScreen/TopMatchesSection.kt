package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultRow
import com.tk.quicksearch.search.appShortcuts.AppShortcutRow
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.calendar.CalendarEventRow
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SectionRenderContext
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.SettingResultRow
import com.tk.quicksearch.search.files.FileResultRow
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.notes.NoteRow
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitHighlight
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

private const val APP_ROW_MIN_HEIGHT = 52
private const val APP_ICON_SIZE = 32

internal sealed interface TopMatchItem {
    val priority: Int
    val sectionOrder: Int
    val index: Int

    data class App(
        val app: AppInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem

    data class AppShortcut(
        val shortcut: StaticShortcut,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem

    data class Contact(
        val contact: ContactInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem

    data class File(
        val file: DeviceFile,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem

    data class Setting(
        val setting: DeviceSetting,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem

    data class AppSetting(
        val setting: AppSettingResult,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem

    data class Calendar(
        val event: CalendarEventInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem

    data class Note(
        val note: NoteInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val index: Int,
    ) : TopMatchItem
}

@Composable
internal fun rememberTopMatches(
    query: String,
    renderingState: SectionRenderingState,
    context: SectionRenderContext,
    params: SectionRenderParams,
    limit: Int,
    topMatchesSectionOrder: List<SearchSection>,
    disabledTopMatchesSections: Set<SearchSection>,
): List<TopMatchItem> =
    remember(query, renderingState, context, params, limit, topMatchesSectionOrder, disabledTopMatchesSections) {
        if (query.isBlank() || limit <= 0) {
            emptyList()
        } else {
            buildTopMatches(
                query = query,
                renderingState = renderingState,
                context = context,
                params = params,
                limit = limit,
                topMatchesSectionOrder = topMatchesSectionOrder,
                disabledTopMatchesSections = disabledTopMatchesSections,
            )
        }
    }

private fun buildTopMatches(
    query: String,
    renderingState: SectionRenderingState,
    context: SectionRenderContext,
    params: SectionRenderParams,
    limit: Int,
    topMatchesSectionOrder: List<SearchSection>,
    disabledTopMatchesSections: Set<SearchSection>,
): List<TopMatchItem> {
    val sectionOrder =
        topMatchesSectionOrder.mapIndexed { index, section -> section to index }.toMap()
    fun order(section: SearchSection): Int = sectionOrder[section] ?: Int.MAX_VALUE
    fun isTopMatchesSectionEnabled(section: SearchSection): Boolean =
        section !in disabledTopMatchesSections && section in sectionOrder
    fun priority(text: String, nickname: String? = null): Int =
        SearchRankingUtils.calculateMatchPriorityWithNickname(text, nickname, query)

    val matches = mutableListOf<TopMatchItem>()
    if (context.shouldRenderApps && isTopMatchesSectionEnabled(SearchSection.APPS)) {
        renderingState.displayApps.forEachIndexed { index, app ->
            matches += TopMatchItem.App(
                app = app,
                priority = priority(app.appName, params.appsParams?.getAppNickname?.invoke(app.packageName)),
                sectionOrder = order(SearchSection.APPS),
                index = index,
            )
        }
    }
    if (context.shouldRenderAppShortcuts && isTopMatchesSectionEnabled(SearchSection.APP_SHORTCUTS)) {
        context.appShortcutsList.forEachIndexed { index, shortcut ->
            val id = shortcutKey(shortcut)
            matches += TopMatchItem.AppShortcut(
                shortcut = shortcut,
                priority = priority(
                    shortcutDisplayName(shortcut),
                    params.appShortcutsParams?.getShortcutNickname?.invoke(id),
                ),
                sectionOrder = order(SearchSection.APP_SHORTCUTS),
                index = index,
            )
        }
    }
    if (context.shouldRenderContacts && isTopMatchesSectionEnabled(SearchSection.CONTACTS)) {
        context.contactsList.forEachIndexed { index, contact ->
            matches += TopMatchItem.Contact(
                contact = contact,
                priority = priority(
                    contact.displayName,
                    params.contactsParams.getContactNickname(contact.contactId),
                ),
                sectionOrder = order(SearchSection.CONTACTS),
                index = index,
            )
        }
    }
    if (context.shouldRenderFiles && isTopMatchesSectionEnabled(SearchSection.FILES)) {
        context.filesList.forEachIndexed { index, file ->
            matches += TopMatchItem.File(
                file = file,
                priority = priority(
                    file.displayName,
                    params.filesParams.getFileNickname(file.uri.toString()),
                ),
                sectionOrder = order(SearchSection.FILES),
                index = index,
            )
        }
    }
    if (context.shouldRenderSettings && isTopMatchesSectionEnabled(SearchSection.SETTINGS)) {
        context.settingsList.forEachIndexed { index, setting ->
            matches += TopMatchItem.Setting(
                setting = setting,
                priority = priority(
                    setting.title,
                    params.settingsParams?.getSettingNickname?.invoke(setting.id),
                ),
                sectionOrder = order(SearchSection.SETTINGS),
                index = index,
            )
        }
    }
    if (context.shouldRenderSettings && isTopMatchesSectionEnabled(SearchSection.APP_SETTINGS)) {
        context.appSettingsList.forEachIndexed { index, setting ->
            matches += TopMatchItem.AppSetting(
                setting = setting,
                priority = priority(setting.title),
                sectionOrder = order(SearchSection.APP_SETTINGS),
                index = index,
            )
        }
    }
    if (context.shouldRenderCalendar && isTopMatchesSectionEnabled(SearchSection.CALENDAR)) {
        context.calendarEventsList.forEachIndexed { index, event ->
            matches += TopMatchItem.Calendar(
                event = event,
                priority = priority(
                    event.title,
                    params.calendarParams?.getEventNickname?.invoke(event.eventId),
                ),
                sectionOrder = order(SearchSection.CALENDAR),
                index = index,
            )
        }
    }
    if (
        context.shouldRenderNotes &&
            FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES) &&
            isTopMatchesSectionEnabled(SearchSection.NOTES)
    ) {
        context.notesList.forEachIndexed { index, note ->
            matches += TopMatchItem.Note(
                note = note,
                priority = priority(note.title),
                sectionOrder = order(SearchSection.NOTES),
                index = index,
            )
        }
    }

    return matches
        .sortedWith(compareBy<TopMatchItem> { it.priority }.thenBy { it.sectionOrder }.thenBy { it.index })
        .take(limit)
}

@Composable
internal fun TopMatchesSection(
    matches: List<TopMatchItem>,
    params: SectionRenderParams,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    if (matches.isEmpty()) return

    SearchResultCard(
        modifier = modifier.fillMaxWidth(),
        showWallpaperBackground = showWallpaperBackground,
        overlayContainerColor = LocalOverlayResultCardColor.current,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.top_matches_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        start = DesignTokens.SpacingLarge,
                        top = DesignTokens.SpacingSmall,
                        end = DesignTokens.SpacingLarge,
                        bottom = 2.dp,
                    ),
            )
            Column(
                modifier =
                    Modifier.padding(
                        start = DesignTokens.SpacingLarge,
                        end = DesignTokens.SpacingMedium,
                        bottom = 4.dp,
                    ),
            ) {
                matches.forEachIndexed { index, item ->
                    TopMatchRow(
                        item = item,
                        params = params,
                        isPredicted = index == 0,
                    )
                    if (index != matches.lastIndex && index != 0) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color =
                                if (showWallpaperBackground) {
                                    AppColors.WallpaperDivider
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                        )
                    }
                }
            }
        }
    }
}

internal fun openTopMatch(
    item: TopMatchItem,
    params: SectionRenderParams,
): Boolean? {
    when (item) {
        is TopMatchItem.App -> params.appsParams?.onAppClick?.invoke(item.app) ?: return null
        is TopMatchItem.AppShortcut ->
            params.appShortcutsParams?.onShortcutClick?.invoke(item.shortcut) ?: return null
        is TopMatchItem.Contact -> {
            if (item.contact.hasContactMethods) {
                params.contactsParams.onShowContactMethods(item.contact)
            } else {
                params.contactsParams.onContactClick(item.contact)
            }
        }
        is TopMatchItem.File -> params.filesParams.onFileClick(item.file)
        is TopMatchItem.Setting -> params.settingsParams?.onSettingClick?.invoke(item.setting) ?: return null
        is TopMatchItem.AppSetting -> {
            val settingsParams = params.settingsParams ?: return null
            if (item.setting.isToggleAction) {
                val currentValue = settingsParams.isAppSettingToggleChecked(item.setting)
                settingsParams.onAppSettingToggle(item.setting, !currentValue)
                return true
            } else {
                settingsParams.onAppSettingClick(item.setting)
            }
        }
        is TopMatchItem.Calendar ->
            params.calendarParams?.onEventClick?.invoke(item.event) ?: return null
        is TopMatchItem.Note -> params.notesParams?.onNoteClick?.invoke(item.note) ?: return null
    }
    return false
}

@Composable
private fun TopMatchRow(
    item: TopMatchItem,
    params: SectionRenderParams,
    isPredicted: Boolean,
) {
    when (item) {
        is TopMatchItem.App -> TopMatchAppRow(
            app = item.app,
            params = params.appsParams,
            isPredicted = isPredicted,
        )

        is TopMatchItem.AppShortcut -> params.appShortcutsParams?.let { appShortcutsParams ->
            val id = shortcutKey(item.shortcut)
            AppShortcutRow(
                shortcut = item.shortcut,
                isPinned = appShortcutsParams.pinnedShortcutIds.contains(id),
                isExcluded = appShortcutsParams.excludedShortcutIds.contains(id),
                hasNickname = !appShortcutsParams.getShortcutNickname(id).isNullOrBlank(),
                hasTrigger = appShortcutsParams.getShortcutTrigger(id)?.word?.isNotBlank() == true,
                onShortcutClick = appShortcutsParams.onShortcutClick,
                onTogglePin = appShortcutsParams.onTogglePin,
                onExclude = appShortcutsParams.onExclude,
                onInclude = appShortcutsParams.onInclude,
                onAppInfoClick = appShortcutsParams.onAppInfoClick,
                onNicknameClick = appShortcutsParams.onNicknameClick,
                onTriggerClick = appShortcutsParams.onTriggerClick,
                onEditCustomShortcut = appShortcutsParams.onEditCustomShortcut,
                onEditShortcutIcon = appShortcutsParams.onEditShortcutIcon,
                iconPackPackage = appShortcutsParams.iconPackPackage,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.Contact -> ContactResultRow(
            contactInfo = item.contact,
            callingApp = params.contactsParams.callingApp ?: CallingApp.CALL,
            messagingApp = params.contactsParams.messagingApp ?: MessagingApp.MESSAGES,
            primaryAction = params.contactsParams.getPrimaryContactCardAction(item.contact.contactId),
            secondaryAction = params.contactsParams.getSecondaryContactCardAction(item.contact.contactId),
            onContactClick = params.contactsParams.onContactClick,
            onShowContactMethods = params.contactsParams.onShowContactMethods,
            onCallContact = params.contactsParams.onCallContact,
            onSmsContact = params.contactsParams.onSmsContact,
            onContactMethodClick = { method -> params.contactsParams.onContactMethodClick(item.contact, method) },
            isPinned = params.contactsParams.pinnedContactIds.contains(item.contact.contactId),
            onTogglePin = params.contactsParams.onTogglePin,
            onExclude = params.contactsParams.onExclude,
            onNicknameClick = params.contactsParams.onNicknameClick,
            hasNickname = !params.contactsParams.getContactNickname(item.contact.contactId).isNullOrBlank(),
            onTriggerClick = params.contactsParams.onTriggerClick,
            hasTrigger = params.contactsParams.getContactTrigger(item.contact.contactId)?.word?.isNotBlank() == true,
            onPrimaryActionLongPress = params.contactsParams.onPrimaryActionLongPress,
            onSecondaryActionLongPress = params.contactsParams.onSecondaryActionLongPress,
            onCustomAction = params.contactsParams.onCustomAction,
            isPredicted = isPredicted,
        )

        is TopMatchItem.File -> FileResultRow(
            deviceFile = item.file,
            onClick = params.filesParams.onFileClick,
            onOpenFolder = params.filesParams.onOpenFolder,
            isPinned = params.filesParams.pinnedFileUris.contains(item.file.uri.toString()),
            onTogglePin = params.filesParams.onTogglePin,
            onExclude = params.filesParams.onExclude,
            onExcludeExtension = params.filesParams.onExcludeExtension,
            onNicknameClick = params.filesParams.onNicknameClick,
            hasNickname = !params.filesParams.getFileNickname(item.file.uri.toString()).isNullOrBlank(),
            onTriggerClick = params.filesParams.onTriggerClick,
            hasTrigger = params.filesParams.getFileTrigger(item.file.uri.toString())?.word?.isNotBlank() == true,
            isPredicted = isPredicted,
        )

        is TopMatchItem.Setting -> params.settingsParams?.let { settingsParams ->
            SettingResultRow(
                shortcut = item.setting,
                isPinned = settingsParams.pinnedSettingIds.contains(item.setting.id),
                onClick = settingsParams.onSettingClick,
                onTogglePin = settingsParams.onTogglePin,
                onExclude = settingsParams.onExclude,
                onNicknameClick = settingsParams.onNicknameClick,
                hasNickname = !settingsParams.getSettingNickname(item.setting.id).isNullOrBlank(),
                onTriggerClick = settingsParams.onTriggerClick,
                hasTrigger = settingsParams.getSettingTrigger(item.setting.id)?.word?.isNotBlank() == true,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.AppSetting -> params.settingsParams?.let { settingsParams ->
            AppSettingResultRow(
                setting = item.setting,
                checked = settingsParams.isAppSettingToggleChecked(item.setting),
                onToggle = settingsParams.onAppSettingToggle,
                onWebSuggestionsCountChange = settingsParams.onAppSettingWebSuggestionsCountChange,
                onClick = settingsParams.onAppSettingClick,
                webSuggestionsCount = settingsParams.appSettingWebSuggestionsCount,
                appSettingPhoneAppGridColumns = settingsParams.appSettingPhoneAppGridColumns,
                onAppSettingPhoneAppGridColumnsChange = settingsParams.onAppSettingPhoneAppGridColumnsChange,
                isPredicted = isPredicted,
            )
        }

        is TopMatchItem.Calendar -> params.calendarParams?.let { calendarParams ->
            CalendarEventRow(
                event = item.event,
                isPinned = calendarParams.pinnedEventIds.contains(item.event.eventId),
                isExcluded = calendarParams.excludedEventIds.contains(item.event.eventId),
                hasNickname = !calendarParams.getEventNickname(item.event.eventId).isNullOrBlank(),
                onClick = calendarParams.onEventClick,
                onTogglePin = calendarParams.onTogglePin,
                onExclude = calendarParams.onExclude,
                onInclude = calendarParams.onInclude,
                onNicknameClick = calendarParams.onNicknameClick,
                isPredicted = isPredicted,
                onArchive = calendarParams.onArchiveTodayEvent,
            )
        }

        is TopMatchItem.Note -> params.notesParams?.let { notesParams ->
            NoteRow(
                note = item.note,
                isPinned = notesParams.pinnedNoteIds.contains(item.note.noteId),
                onClick = notesParams.onNoteClick,
                onTogglePin = notesParams.onTogglePin,
                onDelete = notesParams.onDelete,
                onTriggerClick = notesParams.onTriggerClick,
                hasTrigger = notesParams.getNoteTrigger(item.note.noteId)?.word?.isNotBlank() == true,
                isPredicted = isPredicted,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopMatchAppRow(
    app: AppInfo,
    params: AppsSectionParams?,
    isPredicted: Boolean,
) {
    if (params == null) return
    val view = LocalView.current
    val iconResult =
        rememberAppIcon(
            packageName = app.packageName,
            iconPackPackage = params.iconPackPackage,
            userHandleId = app.userHandleId,
            forceCircularMask = params.appIconShape == com.tk.quicksearch.search.core.AppIconShape.CIRCLE,
        )

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = APP_ROW_MIN_HEIGHT.dp)
                .topPredictedRowContainer(isTopPredicted = isPredicted)
                .combinedClickable(
                    onClick = {
                        hapticConfirm(view)()
                        params.onAppClick(app)
                    },
                    onLongClick = { params.onAppInfoClick(app) },
                )
                .topPredictedRowContentPadding(isTopPredicted = isPredicted)
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.padding(start = DesignTokens.SpacingXSmall).size(APP_ICON_SIZE.dp),
            contentAlignment = Alignment.Center,
        ) {
            iconResult.bitmap?.let { icon ->
                androidx.compose.foundation.Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(APP_ICON_SIZE.dp),
                )
            } ?: Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
