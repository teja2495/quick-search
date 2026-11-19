package com.tk.quicksearch.search.searchScreen

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.search.appSettings.AppSettingResult
import com.tk.quicksearch.search.appShortcuts.AppShortcutSearchPolicy
import com.tk.quicksearch.search.apps.AppNameAbbreviation
import com.tk.quicksearch.search.apps.AppSearchInitials
import com.tk.quicksearch.search.apps.AppSearchPolicy
import com.tk.quicksearch.search.apps.notificationDots.hasNotificationDot
import com.tk.quicksearch.search.apps.notificationDots.rememberNotificationDotKeys
import com.tk.quicksearch.search.apps.rememberAppIcon
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SectionRenderContext
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.data.appShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.appShortcutRepository.isShortcutDisabled
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.appShortcutRepository.shortcutKey
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.CalendarEventInfo
import com.tk.quicksearch.search.models.ReminderInfo
import com.tk.quicksearch.search.models.BookmarkInfo
import com.tk.quicksearch.search.data.bookmarks.BookmarkUrls
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import com.tk.quicksearch.search.models.NoteInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.other.OtherSearchItemId
import com.tk.quicksearch.search.other.OtherSearchItemRegistry
import com.tk.quicksearch.search.searchScreen.components.predictedSubmitHighlight
import com.tk.quicksearch.search.searchScreen.components.topPredictedRowContainer
import com.tk.quicksearch.search.searchScreen.components.rememberQueryHighlightedText
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.search.utils.SearchRankingUtils
import com.tk.quicksearch.search.utils.SearchQueryContext
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

    data class Bookmark(
        val bookmark: BookmarkInfo,
        override val priority: Int,
        override val sectionOrder: Int,
        override val secondaryScore: Long,
        override val index: Int,
    ) : TopMatchItem

    data class Noted(
        val item: com.tk.quicksearch.search.models.NotedItem,
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

/**
 * Exactly what [buildTopMatches] reads. Keying the build on this, rather than on the whole render
 * context and params, keeps unrelated changes (predicted target, startup phase, pinned lists,
 * expand callbacks) from re-ranking every candidate.
 */
internal data class TopMatchInputs(
    val displayApps: List<AppInfo>,
    val appShortcuts: List<StaticShortcut>,
    val contacts: List<ContactInfo>,
    val files: List<DeviceFile>,
    val settings: List<DeviceSetting>,
    val appSettings: List<AppSettingResult>,
    val calendarEvents: List<CalendarEventInfo>,
    val reminders: List<ReminderInfo>,
    val bookmarks: List<BookmarkInfo>,
    val noted: List<com.tk.quicksearch.search.models.NotedItem>,
    val notes: List<NoteInfo>,
    val recencyIndex: com.tk.quicksearch.search.utils.RecentResultRankingUtils.RecencyIndex,
    val getAppNickname: ((String) -> String?)?,
    val getShortcutNickname: ((String) -> String?)?,
    val getContactNickname: (Long) -> String?,
    val getFileNickname: (String) -> String?,
    val getSettingNickname: ((String) -> String?)?,
    val getEventNickname: ((Long) -> String?)?,
)

internal fun topMatchInputs(
    renderingState: SectionRenderingState,
    context: SectionRenderContext,
    params: SectionRenderParams,
): TopMatchInputs =
    TopMatchInputs(
        displayApps = renderingState.displayApps,
        appShortcuts = context.appShortcutsList,
        contacts = context.contactsList,
        files = context.filesList,
        settings = context.settingsList,
        appSettings = context.appSettingsList,
        calendarEvents = context.calendarEventsList,
        reminders = context.remindersList,
        bookmarks = context.bookmarksList,
        noted = context.notedList,
        notes = context.notesList,
        recencyIndex = context.recentResultRecencyIndex,
        getAppNickname = params.appsParams?.getAppNickname,
        getShortcutNickname = params.appShortcutsParams?.getShortcutNickname,
        getContactNickname = params.contactsParams.getContactNickname,
        getFileNickname = params.filesParams.getFileNickname,
        getSettingNickname = params.settingsParams?.getSettingNickname,
        getEventNickname = params.calendarParams?.getEventNickname,
    )

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
): List<TopMatchItem> {
    val inputs = topMatchInputs(renderingState, context, params)
    return remember(
        query,
        inputs,
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
                inputs = inputs,
                limit = limit,
                topMatchesSectionOrder = topMatchesSectionOrder,
                disabledTopMatchesSections = disabledTopMatchesSections,
                secondaryRankingSignal = secondaryRankingSignal,
                otherSearchItemIds = otherSearchItemIds,
                filterStaleCandidates = filterStaleCandidates,
            )
        }
    }
}

private fun buildTopMatches(
    query: String,
    inputs: TopMatchInputs,
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
    // Normalize the query once; the raw-query overload would redo it for every candidate.
    val rankingQuery = SearchRankingUtils.prepareRankingQuery(query)
    fun priority(text: String, nickname: String? = null): Int =
        SearchRankingUtils.calculateMatchPriorityWithNickname(text, nickname, rankingQuery)
    val queryContext = SearchQueryContext.fromRawQuery(query)
    val recencyIndex = inputs.recencyIndex

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
        inputs.displayApps.firstOrNull()?.let { topApp ->
            matches +=
                TopMatchItem.AppGrid(
                    apps = inputs.displayApps,
                    priority =
                        appTopMatchPriority(
                            app = topApp,
                            nickname = inputs.getAppNickname?.invoke(topApp.launchCountKey()),
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
        inputs.appShortcuts.forEachIndexed { index, shortcut ->
            val id = shortcutKey(shortcut)
            matches += TopMatchItem.AppShortcut(
                shortcut = shortcut,
                priority = appShortcutTopMatchPriority(
                    shortcut = shortcut,
                    nickname = inputs.getShortcutNickname?.invoke(id),
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
        inputs.contacts.forEachIndexed { index, contact ->
            matches += TopMatchItem.Contact(
                contact = contact,
                priority = priority(
                    contact.displayName,
                    inputs.getContactNickname(contact.contactId),
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
        inputs.files.forEachIndexed { index, file ->
            val uri = file.uri.toString()
            matches += TopMatchItem.File(
                file = file,
                priority = priority(file.displayName, inputs.getFileNickname(uri)),
                sectionOrder = order(SearchSection.FILES),
                secondaryScore = secondaryScore(
                    secondaryRankingSignal,
                    recencyIndex.fileScores[uri],
                    recencyIndex.fileOpenCounts[uri],
                ),
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.SETTINGS)) {
        inputs.settings.forEachIndexed { index, setting ->
            matches += TopMatchItem.Setting(
                setting = setting,
                priority = priority(
                    setting.title,
                    inputs.getSettingNickname?.invoke(setting.id),
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
        inputs.appSettings.forEachIndexed { index, setting ->
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
        inputs.calendarEvents.forEachIndexed { index, event ->
            matches += TopMatchItem.Calendar(
                event = event,
                priority = priority(
                    event.title,
                    inputs.getEventNickname?.invoke(event.eventId),
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
        inputs.reminders.forEachIndexed { index, reminder ->
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
    if (isTopMatchesSectionEnabled(SearchSection.BOOKMARKS)) {
        inputs.bookmarks.forEachIndexed { index, bookmark ->
            matches += TopMatchItem.Bookmark(
                bookmark = bookmark,
                // The site's domain counts like a nickname, one step below a name match.
                priority = priority(bookmark.name, BookmarkUrls.displayHost(bookmark.url)),
                sectionOrder = order(SearchSection.BOOKMARKS),
                secondaryScore = 0L,
                index = index,
            )
        }
    }
    if (isTopMatchesSectionEnabled(SearchSection.NOTED)) {
        inputs.noted.forEachIndexed { index, item ->
            matches += TopMatchItem.Noted(
                item = item,
                priority = priority(item.title),
                sectionOrder = order(SearchSection.NOTED),
                // No open history; `index` keeps Noted's most-recently-updated-first order.
                secondaryScore = 0L,
                index = index,
            )
        }
    }
    if (
            isTopMatchesSectionEnabled(SearchSection.NOTES)
    ) {
        inputs.notes.forEachIndexed { index, note ->
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
        abbreviationTargets = AppNameAbbreviation.targetsFor(app),
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
