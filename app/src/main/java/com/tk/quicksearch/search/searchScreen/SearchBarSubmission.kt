package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.search.core.SectionRenderParams
import com.tk.quicksearch.search.core.SearchUiState
import com.tk.quicksearch.search.core.isLikelyWebUrl
import com.tk.quicksearch.searchEngines.defaultBrowserTarget
import com.tk.quicksearch.searchEngines.resolveDefaultBrowserPackage
import com.tk.quicksearch.search.searchScreen.searchScreenLayout.SectionRenderingState
import com.tk.quicksearch.shared.featureFlags.FeatureFlags

internal fun submitSearchBarAction(
    state: SearchUiState,
    renderingState: SectionRenderingState,
    isNonSubmittableSuggestionsTab: Boolean,
    isOtherSearchResultVisible: Boolean,
    enabledTargets: List<SearchTarget>,
    onSearchTargetClick: (String, SearchTarget) -> Unit,
    showCurrencyConverterSearchCard: Boolean,
    onCurrencyConversionClick: () -> Unit,
    showDictionarySearchCard: Boolean,
    onDictionarySearchClick: () -> Unit,
    showWeatherSearchCard: Boolean,
    onWeatherSearchClick: () -> Unit,
    showCustomToolSearchCard: Boolean,
    onCustomToolSearchClick: () -> Unit,
    showTaskerIntentCard: Boolean,
    onTaskerIntentClick: () -> Unit,
    showWorldClockSearchCard: Boolean,
    onWorldClockSearchClick: () -> Unit,
    firstSubmittableGridApp: com.tk.quicksearch.search.models.AppInfo?,
    onAppClick: (com.tk.quicksearch.search.models.AppInfo) -> Unit,
    shouldSubmitTopMatch: Boolean,
    topMatchesForSubmit: List<TopMatchItem>,
    selectedTopMatchIndex: Int?,
    topMatchSubmitParams: SectionRenderParams,
    openMatchingTrigger: (String) -> Boolean,
    onQueryChanged: (String) -> Unit,
    appShortcutsParams: AppShortcutsSectionParams,
    contactsParams: ContactsSectionParams,
    filesParams: FilesSectionParams,
    settingsParams: SettingsSectionParams,
    calendarParams: CalendarSectionParams,
    notesParams: NotesSectionParams,
    isCalculatorMode: Boolean,
    suffixAliasMatchIgnoringTrailingSpace: Pair<String, SearchTarget>?,
    context: android.content.Context,
): Boolean {

                    if (isNonSubmittableSuggestionsTab) {
                        return true
                    }
                    if (isOtherSearchResultVisible && !state.topMatchesEnabled) {
                        return true
                    }

                    if (!state.openTopResultUsingKeyboardEnabled) {
                        val query = state.query.trim()
                        enabledTargets.firstOrNull()?.let { target ->
                            if (query.isNotBlank()) onSearchTargetClick(query, target)
                        }
                        return false
                    }

                    // Tool prompt cards take priority: Done triggers the card action.
                    // When no card is visible, fall through to the search engine.
                    if (showCurrencyConverterSearchCard) {
                        onCurrencyConversionClick()
                        return true // keep keyboard open
                    }
                    if (showDictionarySearchCard) {
                        onDictionarySearchClick()
                        return true // keep keyboard open
                    }
                    if (showWeatherSearchCard) {
                        onWeatherSearchClick()
                        return true
                    }
                    if (showCustomToolSearchCard) {
                        onCustomToolSearchClick()
                        return true // keep keyboard open
                    }
                    if (showTaskerIntentCard) {
                        onTaskerIntentClick()
                        return true
                    }
                    if (showWorldClockSearchCard) {
                        onWorldClockSearchClick()
                        return true // keep keyboard open
                    }

                    // The app grid is the primary result surface. Done follows its visible
                    // ordering before considering the cross-section Top Matches fallback.
                    val firstApp = firstSubmittableGridApp
                    if (firstApp != null) {
                        onAppClick(firstApp)
                        return false
                    }

                    if (shouldSubmitTopMatch) {
                        openTopMatch(
                                item = topMatchesForSubmit.getOrNull(selectedTopMatchIndex ?: 0) ?: topMatchesForSubmit.first(),
                                params = topMatchSubmitParams,
                        )?.let { keepKeyboardOpen ->
                            return keepKeyboardOpen
                        }
                    }

                    val trimmedQuery = state.query.trim()
                    if (openMatchingTrigger(state.query)) {
                        return false
                    }

                    val isUrlQuery = isLikelyWebUrl(trimmedQuery)

                    // If query has trailing/leading spaces, trim it first
                    if (state.query != trimmedQuery) {
                        onQueryChanged(trimmedQuery)
                    }

                    if (isUrlQuery && trimmedQuery.isNotBlank()) {
                        val defaultBrowserPackage = resolveDefaultBrowserPackage(context)
                        val browserTarget =
                                defaultBrowserTarget(state.searchTargetsOrder, defaultBrowserPackage)
                        if (browserTarget != null) {
                            onSearchTargetClick(trimmedQuery, browserTarget)
                            return false
                        }
                    }

                    val firstAppShortcut = renderingState.appShortcutResults.firstOrNull()
                    if (firstAppShortcut != null) {
                        appShortcutsParams.onShortcutClick(firstAppShortcut)
                        return false
                    }

                    val firstContact = renderingState.contactResults.firstOrNull()
                    if (firstContact != null) {
                        if (firstContact.hasContactMethods) {
                            contactsParams.onShowContactMethods(firstContact)
                        } else {
                            contactsParams.onContactClick(firstContact)
                        }
                        return false
                    }

                    val firstFile = renderingState.fileResults.firstOrNull()
                    if (firstFile != null) {
                        filesParams.onFileClick(firstFile)
                        return false
                    }

                    val firstSetting = renderingState.settingResults.firstOrNull()
                    if (firstSetting != null) {
                        settingsParams.onSettingClick(firstSetting)
                        return false
                    }

                    val firstCalendarEvent = renderingState.calendarEvents.firstOrNull()
                    if (firstCalendarEvent != null) {
                        calendarParams.onEventClick(firstCalendarEvent)
                        return false
                    }

                    val firstNote =
                        if (FeatureFlags.isSearchSectionEnabled(SearchSection.NOTES)) {
                            renderingState.noteResults.firstOrNull()
                        } else {
                            null
                        }
                    if (firstNote != null) {
                        notesParams.onNoteClick(firstNote)
                        return false
                    }

                    val firstAppSetting = renderingState.appSettingResults.firstOrNull()
                    if (firstAppSetting != null) {
                        if (firstAppSetting.isToggleAction) {
                            val currentValue = settingsParams.isAppSettingToggleChecked(firstAppSetting)
                            settingsParams.onAppSettingToggle(firstAppSetting, !currentValue)
                            return true // keep keyboard open for toggles
                        } else {
                            settingsParams.onAppSettingClick(firstAppSetting)
                            return false
                        }
                    }

                    // Check if a shortcut is detected
                    if (isCalculatorMode) {
                        return false
                    } else if (state.detectedShortcutTarget != null) {
                        // Query already has shortcut stripped by ViewModel when
                        // shortcut-at-start is detected
                        onSearchTargetClick(trimmedQuery, state.detectedShortcutTarget)
                    } else {
                        val shouldResolveSuffixAliasOnDone =
                                state.isSearchEngineAliasSuffixEnabled &&
                                        state.isAliasTriggerAfterSpaceEnabled &&
                                        state.query.lastOrNull()?.isWhitespace() == false
                        if (shouldResolveSuffixAliasOnDone) {
                            val suffixAliasMatch = suffixAliasMatchIgnoringTrailingSpace
                            if (suffixAliasMatch != null) {
                                val aliasQuery = suffixAliasMatch.first.trim()
                                if (aliasQuery.isNotBlank()) {
                                    onQueryChanged(aliasQuery)
                                    onSearchTargetClick(aliasQuery, suffixAliasMatch.second)
                                    return false
                                }
                            }
                        }
                        val primaryTarget = enabledTargets.firstOrNull()
                        if (primaryTarget != null && trimmedQuery.isNotBlank()) {
                            onSearchTargetClick(trimmedQuery, primaryTarget)
                        }
                    }
                    return false
}
