# Quick Search - AI Agent Guide

This file is for AI coding agents only.
Use it as the primary implementation playbook for building new features and updating existing ones in this repository.

## 1) Project Snapshot

- App type: Android launcher/search app
- Language: Kotlin
- UI: Jetpack Compose + Material 3
- Architecture: MVVM with unidirectional data flow
- Package: `com.tk.quicksearch`
- Core behavior: unified search for apps, app shortcuts, contacts, calendar events, files, device settings, app settings, web, and tools (calculator/unit/date/direct search)

## 2) Core Architecture (Do Not Bypass)

### Layers

- **UI layer**: composables under `search/`, `settings/`, `widgets/`, `overlay/`, `onboarding/`
- **ViewModel layer**: orchestration and state in `search/core/`
- **Data layer**: repositories and preferences in `search/data/`

### Single source of truth

- Main state: `SearchUiState` in `search/core/SearchModels.kt`
- Additional state models: `search/core/SearchStateModels.kt`
- Main orchestrator: `search/core/SearchViewModel.kt`
- State transport: `StateFlow`
- Update pattern: always use immutable `copy(...)` through ViewModel update helpers

### State conventions

- Use sealed classes for visibility/loading/no-results/showing-results states.
- Do not perform permission checks directly inside UI composables when state already exists in `SearchUiState`.
- Prefer adding state in `SearchModels.kt`/`SearchStateModels.kt` before wiring UI logic.

## 3) Repository and Preference Patterns

### Repositories (search data)

- `AppsRepository.kt`
- `ContactRepository.kt`
- `FileSearchRepository.kt`
- `CalendarRepository.kt`
- `search/data/appShortcutRepository/AppShortcutRepository.kt`
- `search/deviceSettings/DeviceSettingsRepository.kt`

### Preferences

- Central facade: `search/data/userAppPreferences/UserAppPreferences.kt`
- Startup facade: `search/data/userAppPreferences/StartupPreferencesFacade.kt`
- Specialized modules: `search/data/preferences/`
- Keep new preferences modular; delegate through `UserAppPreferences`.
- `SearchHistoryPreferences.kt` stays in `search/searchHistory/`.

### Cache/Startup behavior

- App cache: `search/data/AppCache.kt`
- Startup strategy is phased (critical prefs -> remaining prefs -> deferred init) via `app/startup/StartupCoordinator.kt` and `search/core/SearchStartupCoordinator.kt`.
- Avoid expensive synchronous work on startup paths.

## 4) Key Feature Areas and Main Files

### Search core

- `search/core/SearchViewModel.kt`
- `search/core/SearchViewModelSearchOperations.kt`
- `search/core/UnifiedSearchHandler.kt`
- `search/core/SectionManager.kt`
- `search/core/SearchSectionRegistry.kt`
- `search/core/SearchModels.kt`
- `search/core/SearchStateModels.kt`

### Search UI composition

- `search/searchScreen/SearchScreen.kt`
- `search/searchScreen/SearchScreenContent.kt`
- `search/searchScreen/SectionRenderingComposables.kt`
- `search/searchScreen/SearchScreenLayout.kt`
- `search/searchScreen/searchScreen/SearchRoute.kt`
- `search/searchScreen/searchScreenLayout/`

### Feature packages

- Apps: `search/apps/`
- App settings results: `search/appSettings/`
- App shortcuts: `search/appShortcuts/`
- Contacts: `search/contacts/`
- Calendar: `search/calendar/`
- Files: `search/files/`
- Device settings: `search/deviceSettings/`
- Web suggestions: `search/webSuggestions/`
- Search history: `search/searchHistory/`
- Fuzzy search: `search/fuzzy/`

### Search engines and tools

- Search engines: `searchEngines/`
- Orchestration/debounce: `searchEngines/SecondarySearchOrchestrator.kt`
- Direct Search (Gemini/Gemma): `tools/directSearch/`
- Built-in tools: `tools/calculator/`, `tools/unitConverter/`, `tools/dateCalculator/`, `tools/aiTools/`

### Overlay mode

- `overlay/OverlayActivity.kt`
- `overlay/OverlayRoot.kt`
- `overlay/OverlayModeController.kt`
- App theme color utils: `search/searchScreen/AppThemeUtils.kt`

### Settings

- Root: `settings/SettingsScreen.kt`
- Detail navigation: `settings/navigation/`
- Detail sections: `settings/settingsDetailScreen/`
- Appearance: `settings/appearanceSettings/`
- Search engine settings: `settings/searchEngineSettings/`
- App shortcuts settings: `settings/appShortcutsSettings/`
- Shared settings route/state: `settings/shared/settingsRoute/`

## 5) UI Design System Rules

### Tokens and colors

- Use `shared/ui/theme/DesignTokens.kt` for spacing, shapes, dimensions.
- Use `shared/ui/theme/AppColors.kt` for colors and wallpaper-aware surfaces.
- Avoid hardcoded dp, corner radii, and color literals unless extending tokens.

### Shared components first

- Prefer components in `shared/ui/components/` before creating new one-off UI.
- Keep composables stateless where possible; hoist state to ViewModel/parent.
- Keep `modifier: Modifier = Modifier` in reusable composables.
- Try existing design tokens first.
- Add new design tokens when they are reusable across multiple places.
- If a value is file-specific, add a top-level `const` in that file instead of a global token.

### Responsive behavior

- Device/orientation helpers: `shared/util/DeviceUtils.kt`
- Preserve tablet behavior for app grid and search engine layout.

## 6) Search and Ranking Behavior

### Ranking stack

- Traditional ranking: `search/common/SearchRankingUtils.kt`
  - exact > startsWith > second-word startsWith > contains
- Fuzzy enhancement: `search/fuzzy/FuzzySearchEngine.kt`
  - typo tolerance + acronym matching + nickname support

### App result rules

- Empty query: pinned + recent apps
- Non-empty query: ranked app results + fuzzy enhancements
- Respect existing max grid/result constraints from current UI layout logic

### Secondary search

- Contacts/files/settings/app shortcuts/calendar are debounced and orchestrated
- Preserve query version checks and no-result cache behavior to prevent regressions

## 7) Permissions and Graceful Degradation

- Required baseline permission: usage stats
- Optional permissions: contacts, calendar, files/storage, call phone, package visibility, wallpaper-related access, overlay permission
- Rule: if permission unavailable, hide/degrade corresponding section cleanly through state

## 8) Navigation and Entry Points

- Main app entry: `app/MainActivity.kt`
- Overlay entry: `overlay/OverlayActivity.kt`
- Main routing: `app/navigation/NavigationManager.kt`
- Search route: `search/searchScreen/searchScreen/`
- Settings routes: `settings/shared/` + `settings/navigation/`

## 9) Naming and Organization Conventions

- Data classes: `*Info`, `*State`, `*Model`
- Repositories: `*Repository`
- Handlers: `*Handler`
- Section composables: `*Section`
- Utility files: `*Utils`
- Organize code by feature package first, then by layer inside feature when needed.
- File names should be PascalCase.
- Folder names should be lowerCamelCase.
- If a file grows beyond ~700 lines, split it into focused files rather than adding more to the same file.

## 10) Standard Implementation Workflows

### Add or update a search section

1. Add/update models (`search/models/` or feature models)
2. Add/update repository or handler logic
3. Add/update state in `SearchUiState` (`SearchModels.kt`/`SearchStateModels.kt`)
4. Wire orchestration in ViewModel/core handlers
5. Render section composable (typically via `SectionRenderingComposables.kt` and `SearchRoute.kt`)
6. Integrate section rendering and ordering
7. Add settings toggles/preferences if user-configurable

### Add a new preference

1. Add preference in correct class under `search/data/preferences/`
2. Expose through `search/data/userAppPreferences/UserAppPreferences.kt`
3. Reflect in `SearchUiState` if needed for runtime UI
4. Update settings UI and mappers
5. Verify persistence and default handling

### Modify search UI

1. Locate the correct composable in `search/searchScreen/` or `search/searchScreen/searchScreen/`
2. Use shared tokens/components
3. Verify wallpaper mode and one-handed mode behavior
4. Verify overlay mode if shared rendering path is used

## 11) Performance Guardrails

- Keep heavy work off main thread.
- Prefer debounced/reused search pipelines over new parallel ad-hoc searches.
- Reuse caches where existing architecture already supports them.
- For startup-sensitive logic, avoid adding blocking work to early initialization.

## 12) Security and Privacy Constraints

- Keep local-first behavior; no unsolicited analytics/tracking additions.
- Sensitive values (such as API keys) must use existing encrypted storage patterns.
- Preserve intent safety and permission checks around external app integrations.

## 13) Testing and Validation Checklist (Minimum)

- Build compiles after changes.
- Search behavior verified for:
  - empty query
  - normal query
  - typo/acronym query (if search logic touched)
- Permission-off states verified for impacted sections.
- Wallpaper mode + standard mode verified if UI touched.
- Overlay mode sanity check if shared UI/state touched.
- Settings persistence verified when preferences are changed.

## 14) High-Risk Files (Edit Carefully)

- `search/core/SearchViewModel.kt`
- `search/core/SearchModels.kt`
- `search/core/SearchStateModels.kt`
- `search/core/SearchSectionRegistry.kt`
- `searchEngines/SecondarySearchOrchestrator.kt`
- `search/data/userAppPreferences/UserAppPreferences.kt`
- `search/searchScreen/searchScreen/SearchRoute.kt`

When touching these files, keep changes minimal, localized, and regression-aware.

## 15) Agent Do/Do Not

### Do

- Follow existing package structure and naming.
- Extend existing handlers/repositories before introducing new abstractions.
- Keep logic readable and incremental.
- Reuse design tokens/shared components.
- Preserve sealed-state and immutable-state patterns.
- Put user-facing strings in `strings.xml`.
- Remove unused code when touching related areas.
- Create reusable functions/components when there is a significant chance of code duplication.

### Do Not

- Do not hardcode colors/spacing where tokens already exist.
- Do not bypass ViewModel/state flow with UI-local business logic.
- Do not mix unrelated refactors into feature changes.
- Do not introduce unnecessary complexity or speculative architecture.

## 16) Useful File Index

- `search/core/SearchModels.kt`
- `search/core/SearchStateModels.kt`
- `search/core/SearchViewModel.kt`
- `search/core/SectionManager.kt`
- `search/core/SearchSectionRegistry.kt`
- `search/common/SearchRankingUtils.kt`
- `search/fuzzy/FuzzySearchEngine.kt`
- `search/searchScreen/SearchScreen.kt`
- `search/searchScreen/searchScreen/SearchRoute.kt`
- `overlay/OverlayRoot.kt`
- `search/data/userAppPreferences/UserAppPreferences.kt`
- `shared/ui/theme/DesignTokens.kt`
- `shared/ui/theme/AppColors.kt`
- `app/navigation/NavigationManager.kt`

---

Last updated: 2026-04-03
