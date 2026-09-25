---
name: new-search-section
description: Add a new search result section to Quick Search (a data source searched on every query, like contacts, files, calendar, or notes). Use when asked to make a new kind of item searchable or to add a results section.
---

# New search section

Model it on the notes section (commit `cdf153ee4`; run `git show cdf153ee4`). Package paths have moved since then (`29189f2be organize code`), so use the current paths below. Paths are relative to `app/src/main/java/com/tk/quicksearch/`.

## Checklist

1. **Model and data:**
   - Put the item model in `search/models/`.
   - Put the repository in `search/data/` (or its feature package), querying off the main thread.
   - Put the feature package under `search/<feature>/` for policies and the results UI.
2. **Registry:** add a `SearchSection` value and its definition in `search/core/SearchSectionRegistry.kt`: order, alias ID, settings toggle, permission, and minimum query length. Also update `SearchSectionUiMetadata.kt` and `ItemPriorityConfig.kt`. `SearchSectionRegistryContractTest` guards this.
3. **State:**
   - Add result fields in `search/core/SearchStateModels.kt` and `SearchModels.kt`.
   - Read them in `SearchStateExtractor.kt`.
   - Make visibility decisions in `SearchVisibilityStateResolver.kt`.
4. **Orchestration:**
   - Run the search in `search/core/UnifiedSearchHandler.kt` and/or `searchEngines/SecondarySearchOrchestrator.kt`.
   - Construct the handler in `SearchHandlerContainer.kt` and hook the query flow in `SearchQueryCoordinator.kt`.
   - Put item actions in a focused `SearchViewModel*Api.kt` file, not `SearchViewModel.kt`.
   - Keep query-version checks and stale-result suppression.
5. **Rendering:**
   - Render from `search/searchScreen/SectionRenderingComposables.kt`, `search/core/SectionRenderingHelpers.kt`, and `search/searchScreen/searchScreenLayout/` (`SectionRenderingState.kt`, `ContentLayout.kt`, `SearchContentArea.kt`).
   - Check the overlay (`overlay/`), one-handed/bottom search bar, and wallpaper modes.
6. **Alias:** section aliases come from the registry (`aliasTargetId`); confirm `searchEngines/AliasHandler.kt` picks it up.
7. **Permissions:** on denial, hide or degrade only this section; never block other search.
8. **Preferences and backup:**
   - Put the preferences class in `search/data/preferences/` and expose it via `UserAppPreferences.kt`.
   - Include new keys in `settings/settingsScreen/SettingsBackupManager.kt` / `SettingsExportDialog.kt` if they should be backed up.
9. **Settings:** add a detail page if needed (`settings/settingsDetailScreen/SettingsDetailType.kt` + routing). The searchable toggle row is generated from the registry. Use the `new-app-setting` skill for any other settings.
10. **Strings:** `values/strings.xml` plus all 16 `values-*/strings.xml` files.
11. **Tests:** unit-test the matching/ranking policy (see `search/*/…PolicyTest.kt`).

Split any file that would pass 800 lines. Finish with `scripts/verify.sh`.
