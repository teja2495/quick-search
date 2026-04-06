package com.tk.quicksearch.search.core

internal class SearchVisibilityStateResolver {
    fun apply(state: SearchUiState): SearchUiState =
        state.copy(
            screenState = computeScreenVisibilityState(state),
            appsSectionState = computeAppsSectionVisibility(state),
            appShortcutsSectionState = computeAppShortcutsSectionVisibility(state),
            contactsSectionState = computeContactsSectionVisibility(state),
            filesSectionState = computeFilesSectionVisibility(state),
            settingsSectionState = computeSettingsSectionVisibility(state),
            calendarSectionState = computeCalendarSectionVisibility(state),
            notesSectionState = computeNotesSectionVisibility(state),
            searchEnginesState = computeSearchEnginesVisibility(state),
        )

    private fun computeScreenVisibilityState(state: SearchUiState): ScreenVisibilityState =
        when {
            state.isInitializing -> ScreenVisibilityState.Initializing
            state.isLoading -> ScreenVisibilityState.Loading
            state.errorMessage != null -> ScreenVisibilityState.Error(state.errorMessage, canRetry = true)
            !state.hasUsagePermission -> ScreenVisibilityState.NoPermissions
            state.query.isBlank() && state.recentApps.isEmpty() && state.pinnedApps.isEmpty() ->
                ScreenVisibilityState.Empty
            else -> ScreenVisibilityState.Content
        }

    private fun computeAppsSectionVisibility(state: SearchUiState): AppsSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.APPS)

        return when {
            !sectionEnabled -> AppsSectionVisibility.Hidden
            state.isInitializing || state.isLoading -> AppsSectionVisibility.Loading
            state.query.isBlank() -> {
                val hasContent = state.recentApps.isNotEmpty() || state.pinnedApps.isNotEmpty()
                if (hasContent) {
                    AppsSectionVisibility.ShowingResults(hasPinned = state.pinnedApps.isNotEmpty())
                } else {
                    AppsSectionVisibility.NoResults
                }
            }
            else -> {
                val hasResults = state.searchResults.isNotEmpty()
                if (hasResults) {
                    AppsSectionVisibility.ShowingResults(hasPinned = false)
                } else {
                    AppsSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeAppShortcutsSectionVisibility(
        state: SearchUiState,
    ): AppShortcutsSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.APP_SHORTCUTS)

        return when {
            !sectionEnabled -> AppShortcutsSectionVisibility.Hidden
            else -> {
                val hasResults = state.appShortcutResults.isNotEmpty()
                val hasPinned = state.pinnedAppShortcuts.isNotEmpty()
                if (hasResults || hasPinned) {
                    AppShortcutsSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    AppShortcutsSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeContactsSectionVisibility(state: SearchUiState): ContactsSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.CONTACTS)

        return when {
            !sectionEnabled -> ContactsSectionVisibility.Hidden
            !state.hasContactPermission -> ContactsSectionVisibility.NoPermission
            else -> {
                val hasResults = state.contactResults.isNotEmpty()
                val hasPinned = state.pinnedContacts.isNotEmpty()
                if (hasResults || hasPinned) {
                    ContactsSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    ContactsSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeFilesSectionVisibility(state: SearchUiState): FilesSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.FILES)

        return when {
            !sectionEnabled -> FilesSectionVisibility.Hidden
            !state.hasFilePermission -> FilesSectionVisibility.NoPermission
            else -> {
                val hasResults = state.fileResults.isNotEmpty()
                val hasPinned = state.pinnedFiles.isNotEmpty()
                if (hasResults || hasPinned) {
                    FilesSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    FilesSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeSettingsSectionVisibility(state: SearchUiState): SettingsSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.SETTINGS)
        val hasAppSettingResults = state.appSettingResults.isNotEmpty()
        val hasPinned = state.pinnedSettings.isNotEmpty()
        val hasDeviceSettingResults = state.settingResults.isNotEmpty()

        return when {
            hasAppSettingResults -> SettingsSectionVisibility.ShowingResults(hasPinned = hasPinned)
            !sectionEnabled -> SettingsSectionVisibility.Hidden
            else -> {
                if (hasDeviceSettingResults || hasPinned) {
                    SettingsSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    SettingsSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeCalendarSectionVisibility(state: SearchUiState): CalendarSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.CALENDAR)

        return when {
            !sectionEnabled -> CalendarSectionVisibility.Hidden
            !state.hasCalendarPermission -> CalendarSectionVisibility.NoPermission
            else -> {
                val hasResults = state.calendarEvents.isNotEmpty()
                val hasPinned = state.pinnedCalendarEvents.isNotEmpty()
                if (hasResults || hasPinned) {
                    CalendarSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    CalendarSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeNotesSectionVisibility(state: SearchUiState): NotesSectionVisibility {
        val sectionEnabled = isSectionEnabledForCurrentQuery(state, SearchSection.NOTES)
        return when {
            !sectionEnabled -> NotesSectionVisibility.Hidden
            else -> {
                val hasResults = state.noteResults.isNotEmpty()
                val hasPinned = state.pinnedNotes.isNotEmpty()
                if (hasResults || hasPinned) {
                    NotesSectionVisibility.ShowingResults(hasPinned = hasPinned)
                } else {
                    NotesSectionVisibility.NoResults
                }
            }
        }
    }

    private fun computeSearchEnginesVisibility(state: SearchUiState): SearchEnginesVisibility =
        when {
            state.detectedShortcutTarget != null ->
                SearchEnginesVisibility.ShortcutDetected(state.detectedShortcutTarget)
            state.detectedAliasSearchSection != null -> SearchEnginesVisibility.Hidden
            state.isCurrencyConverterAliasMode -> SearchEnginesVisibility.Hidden
            state.isWordClockAliasMode -> SearchEnginesVisibility.Hidden
            isLikelyWebUrl(state.query) -> SearchEnginesVisibility.Hidden
            state.isSearchEngineCompactMode -> SearchEnginesVisibility.Compact
            else -> SearchEnginesVisibility.Hidden
        }

    private fun isSectionEnabledForCurrentQuery(
        state: SearchUiState,
        section: SearchSection,
    ): Boolean = section !in state.disabledSections || state.detectedAliasSearchSection == section
}
