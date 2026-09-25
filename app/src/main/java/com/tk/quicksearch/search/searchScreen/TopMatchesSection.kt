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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appSettings.AppSettingResultRow
import com.tk.quicksearch.search.appShortcuts.AppShortcutRow
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchPolicy
import com.tk.quicksearch.search.apps.AppGridView
import com.tk.quicksearch.search.apps.AppItemDropdownMenu
import com.tk.quicksearch.search.apps.AppSearchInitials
import com.tk.quicksearch.search.apps.AppSearchPolicy
import com.tk.quicksearch.search.apps.notificationDots.AppNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.hasNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotKeys
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.calendar.CalendarEventRow
import com.tk.quicksearch.search.common.AddToHomeHandler
import com.tk.quicksearch.search.contacts.components.ContactResultRow
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SectionRenderContext
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.deviceSettings.SettingResultRow
import com.tk.quicksearch.search.files.FileResultRow
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.reminders.ReminderRow
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.notes.NoteRow
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.other.ScreenTimeResultCard
import com.tk.quicksearch.search.core.ScreenTimeState
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitHighlight
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContentPadding
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCard
import com.tk.quicksearch.search.searchScreen.shared.SearchResultCardDefaults
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
import com.tk.quicksearch.shared.featureFlags.FeatureFlags
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticConfirm

internal const val APP_ROW_MIN_HEIGHT = 52
internal const val APP_ICON_SIZE = 32

internal sealed interface TopMatchItem {
    val priority: Int
    val sectionOrder: Int
    val secondaryScore: Long
    val index: Int

    data class App(
        val app: AppInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class AppGrid(
        val apps: List<AppInfo>,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class AppShortcut(
        val shortcut: StaticShortcut,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class Contact(
        val contact: ContactInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class File(
        val file: DeviceFile,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class Setting(
        val setting: DeviceSetting,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class AppSetting(
        val setting: AppSettingResult,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class Calendar(
        val event: CalendarEventInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class Reminder(
        val reminder: ReminderInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class Note(
        val note: NoteInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class Other(
        val itemId: OtherSearchItemId,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
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
    secondaryRankingSignal: SecondaryRankingSignal,
    otherSearchItemIds: List<OtherSearchItemId> = emptyList(),
    filterStaleCandidates: Boolean = false,
): List<TopMatchItem> =
    remember(
        query,
        renderingState,
        context,
        params,
        limit,
        topMatchesSectionOrder,
        disabledTopMatchesSections,
        secondaryRankingSignal,
        otherSearchItemIds,
        filterStaleCandidates,
    ) {
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
                secondaryRankingSignal = secondaryRankingSignal,
                otherSearchItemIds = otherSearchItemIds,
                filterStaleCandidates = filterStaleCandidates,
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
    secondaryRankingSignal: SecondaryRankingSignal,
    otherSearchItemIds: List<OtherSearchItemId>,
    filterStaleCandidates: Boolean,
): List<TopMatchItem> {
    val sectionOrder =
        topMatchesSectionOrder.mapIndexed { index, section -> section to index }.toMap()
    fun order(section: SearchSection): Int = sectionOrder[section] ?: Int.MAX_VALUE
    fun isTopMatchesSectionEnabled(section: SearchSection): Boolean =
        section !in disabledTopMatchesSections && section in sectionOrder
    fun priority(text: String, nickname: String? = null): Int =
        SearchRankingUtils.calculateMatchPriorityWithNickname(text, nickname, query)
    val queryContext = SearchQueryContext.fromRawQuery(query)
    val recencyIndex = context.recentResultRecencyIndex

    val matches = mutableListOf<TopMatchItem>()
    otherSearchItemIds.forEachIndexed { index, itemId ->
        matches +=
            TopMatchItem.Other(
                itemId = itemId,
                priority = otherSearchItemMatchPriority(itemId, query),
                sectionOrder = Int.MAX_VALUE,
                secondaryScore = 0L,
                index = index,
            )
    }
    if (isTopMatchesSectionEnabled(SearchSection.APPS)) {
        renderingState.displayApps.firstOrNull()?.let { topApp ->
            matches +=
                TopMatchItem.AppGrid(
                    apps = renderingState.displayApps,
                    priority =
                        appTopMatchPriority(
                            app = topApp,
                            nickname = params.appsParams?.getAppNickname?.invoke(topApp.packageName),
                            query = queryContext,
                        ),
                    sectionOrder = order(SearchSection.APPS),
                    secondaryScore =
                        topMatchSecondaryScore(
                            secondaryRankingSignal,
                            recencyScore = topApp.lastUsedTime,
                            openCount = topApp.launchCount.toLong(),
                        ),
                    index = 0,
                )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.APP_SHORTCUTS)) {
        context.appShortcutsList.forEachIndexed { index, shortcut ->
            val id = shortcutKey(shortcut)
            matches += TopMatchItem.AppShortcut(
                shortcut = shortcut,
                priority = appShortcutTopMatchPriority(
                    shortcut = shortcut,
                    nickname = params.appShortcutsParams?.getShortcutNickname?.invoke(id),
                    query = queryContext,
                ),
                sectionOrder = order(SearchSection.APP_SHORTCUTS),
                secondaryScore = secondaryScore(
                    secondaryRankingSignal,
                    recencyIndex.appShortcutScores[id],
                    recencyIndex.appShortcutOpenCounts[id],
                ),
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.CONTACTS)) {
        context.contactsList.forEachIndexed { index, contact ->
            matches += TopMatchItem.Contact(
                contact = contact,
                priority = priority(
                    contact.displayName,
                    params.contactsParams.getContactNickname(contact.contactId),
                ),
                sectionOrder = order(SearchSection.CONTACTS),
                secondaryScore = secondaryScore(
                    secondaryRankingSignal,
                    recencyIndex.contactScores[contact.contactId],
                    recencyIndex.contactOpenCounts[contact.contactId],
                ),
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.FILES)) {
        context.filesList.forEachIndexed { index, file ->
            matches += TopMatchItem.File(
                file = file,
                priority = priority(
                    file.displayName,
                    params.filesParams.getFileNickname(file.uri.toString()),
                ),
                sectionOrder = order(SearchSection.FILES),
                secondaryScore = secondaryScore(
                    secondaryRankingSignal,
                    recencyIndex.fileScores[file.uri.toString()],
                    recencyIndex.fileOpenCounts[file.uri.toString()],
                ),
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.SETTINGS)) {
        context.settingsList.forEachIndexed { index, setting ->
            matches += TopMatchItem.Setting(
                setting = setting,
                priority = priority(
                    setting.title,
                    params.settingsParams?.getSettingNickname?.invoke(setting.id),
                ),
                sectionOrder = order(SearchSection.SETTINGS),
                secondaryScore = secondaryScore(
                    secondaryRankingSignal,
                    recencyIndex.settingScores[setting.id],
                    recencyIndex.settingOpenCounts[setting.id],
                ),
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.APP_SETTINGS)) {
        context.appSettingsList.forEachIndexed { index, setting ->
            matches += TopMatchItem.AppSetting(
                setting = setting,
                priority = priority(setting.title),
                sectionOrder = order(SearchSection.APP_SETTINGS),
                secondaryScore = secondaryScore(
                    secondaryRankingSignal,
                    recencyIndex.appSettingScores[setting.id],
                    recencyIndex.appSettingOpenCounts[setting.id],
                ),
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.CALENDAR)) {
        context.calendarEventsList.forEachIndexed { index, event ->
            matches += TopMatchItem.Calendar(
                event = event,
                priority = priority(
                    event.title,
                    params.calendarParams?.getEventNickname?.invoke(event.eventId),
                ),
                sectionOrder = order(SearchSection.CALENDAR),
                secondaryScore = topMatchSecondaryScore(
                    secondaryRankingSignal,
                    recencyScore = recencyIndex.calendarLastOpenedTimes[event.eventId] ?: 0L,
                    openCount = (recencyIndex.calendarOpenCounts[event.eventId] ?: 0).toLong(),
                ),
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.REMINDERS)) {
        context.remindersList.forEachIndexed { index, reminder ->
            matches += TopMatchItem.Reminder(
                reminder = reminder,
                priority = priority(reminder.title),
                sectionOrder = order(SearchSection.REMINDERS),
                // No open history; `index` keeps the repository's soonest-first order.
                secondaryScore = 0L,
                index = index,
            )
        }
    }
    if (
        FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES) &&
            isTopMatchesSectionEnabled(SearchSection.NOTES)
    ) {
        context.notesList.forEachIndexed { index, note ->
            matches += TopMatchItem.Note(
                note = note,
                priority = priority(note.title),
                sectionOrder = order(SearchSection.NOTES),
                secondaryScore = secondaryScore(
                    secondaryRankingSignal,
                    recencyIndex.noteScores[note.noteId],
                    recencyIndex.noteOpenCounts[note.noteId],
                ),
                index = index,
            )
        }
    }

    return rankTopMatches(
        matches = filterTopMatchesForActiveQuery(matches, filterStaleCandidates),
        limit = limit,
    )
}

internal fun appTopMatchPriority(
    app: AppInfo,
    nickname: String?,
    query: SearchQueryContext,
): Int =
    AppSearchPolicy.matchPriority(
        appName = app.appName,
        searchAliases = app.searchAliases,
        nickname = nickname,
        query = query,
        initials = AppSearchInitials.initialsFor(app),
    )

internal fun appShortcutTopMatchPriority(
    shortcut: StaticShortcut,
    nickname: String?,
    query: SearchQueryContext,
): Int =
    AppShortcutSearchPolicy.matchPriority(
        displayName = shortcutDisplayName(shortcut),
        appLabel = shortcut.appLabel,
        nickname = nickname,
        query = query,
    )

internal fun filterTopMatchesForActiveQuery(
    matches: List<TopMatchItem>,
    filterStaleCandidates: Boolean,
): List<TopMatchItem> =
    if (filterStaleCandidates) {
        matches.filterNot { SearchRankingUtils.isOtherMatch(it.priority) }
    } else {
        matches
    }

internal fun otherSearchItemMatchPriority(
    itemId: OtherSearchItemId,
    query: String,
): Int =
    OtherSearchItemRegistry.searchTerms(itemId).minOf { searchTerm ->
        SearchRankingUtils.calculateMatchPriority(searchTerm, query)
    }

internal fun rankTopMatches(
    matches: List<TopMatchItem>,
    limit: Int,
): List<TopMatchItem> =
    matches
        .sortedWith(
            compareBy<TopMatchItem> { it.priority }
                .thenByDescending { it.secondaryScore }
                .thenBy { it.sectionOrder }
                .thenBy { it.index },
        )
        .take(limit)

internal fun topMatchSecondaryScore(
    signal: SecondaryRankingSignal,
    recencyScore: Long,
    openCount: Long,
): Long =
    when (signal) {
        SecondaryRankingSignal.RECENCY -> recencyScore
        SecondaryRankingSignal.MOST_OPENED -> openCount
        SecondaryRankingSignal.NONE -> 0L
    }

internal fun shouldDeferTopMatchesForLocalSearch(
    query: String,
    isAppSearchInProgress: Boolean,
    isSecondarySearchInProgress: Boolean,
): Boolean =
    query.isNotBlank() &&
        (isAppSearchInProgress || isSecondarySearchInProgress)

private fun secondaryScore(
    signal: SecondaryRankingSignal,
    recencyScore: Int?,
    openCount: Int?,
): Long =
    topMatchSecondaryScore(
        signal,
        recencyScore = recencyScore?.toLong() ?: 0L,
        openCount = openCount?.toLong() ?: 0L,
    )
