# Quick Search — Claude Code Guide

Android launcher/search app. Kotlin + Jetpack Compose + Material 3. MVVM with unidirectional data flow. Package: `com.tk.quicksearch`.

## Build & Run

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests
./gradlew lint                   # lint checks
```

## Architecture

### Layers

- **UI**: composables under `search/`, `settings/`, `widgets/`, `overlay/`, `onboarding/`
- **ViewModel**: orchestration and state in `search/core/`
- **Data**: repositories and preferences in `search/data/`

### State management

- Single source of truth: `SearchUiState` in `search/core/SearchModels.kt`
- Additional state models: `search/core/SearchStateModels.kt`
- Main orchestrator: `search/core/SearchViewModel.kt`
- Transport: `StateFlow`
- Updates: always immutable `copy(...)` through ViewModel helpers
- Use sealed classes for visibility/loading/no-results/showing-results states
- Never put business logic or permission checks inside composables when state already exists in `SearchUiState`

### Data flow

```
UI Events → ViewModel → Repositories/Handlers → Data Layer
                   ↓
               StateFlow
                   ↓
            Composables (re-render)
```

## Key Files

| Area | File |
|------|------|
| State models | `search/core/SearchModels.kt` |
| Extended state models | `search/core/SearchStateModels.kt` |
| Main ViewModel | `search/core/SearchViewModel.kt` |
| Search ops | `search/core/SearchViewModelSearchOperations.kt` |
| Unified search | `search/core/UnifiedSearchHandler.kt` |
| Section mgmt | `search/core/SectionManager.kt` |
| Section registry | `search/core/SearchSectionRegistry.kt` |
| Main search UI | `search/searchScreen/SearchScreen.kt` |
| Search route wiring | `search/searchScreen/searchScreen/SearchRoute.kt` |
| Section rendering composables | `search/searchScreen/SectionRenderingComposables.kt` |
| Preferences facade | `search/data/userAppPreferences/UserAppPreferences.kt` |
| Startup prefs facade | `search/data/userAppPreferences/StartupPreferencesFacade.kt` |
| App cache | `search/data/AppCache.kt` |
| Design tokens | `shared/ui/theme/DesignTokens.kt` |
| Colors | `shared/ui/theme/AppColors.kt` |
| Navigation | `app/navigation/NavigationManager.kt` |
| Traditional ranking | `search/common/SearchRankingUtils.kt` |
| Fuzzy search | `search/fuzzy/FuzzySearchEngine.kt` |
| Overlay root | `overlay/OverlayRoot.kt` |
| Secondary search orchestration | `searchEngines/SecondarySearchOrchestrator.kt` |

## High-Risk Files — Edit Carefully

Changes here require extra care; keep them minimal and regression-aware:

- `search/core/SearchViewModel.kt`
- `search/core/SearchModels.kt`
- `search/core/SearchStateModels.kt`
- `search/core/SearchSectionRegistry.kt`
- `search/searchScreen/searchScreen/SearchRoute.kt`
- `searchEngines/SecondarySearchOrchestrator.kt`
- `search/data/userAppPreferences/UserAppPreferences.kt`

## Naming & Organization Conventions

- Data classes: `*Info`, `*State`, `*Model`
- Repositories: `*Repository`
- Handlers: `*Handler`
- Section composables: `*Section`
- Utilities: `*Utils`
- File names: PascalCase
- Folder names: lowerCamelCase
- If a file exceeds ~700 lines, split into focused files
- Organize by feature package first, then by layer inside the feature

## UI & Design System

- Spacing, shapes, dimensions -> `shared/ui/theme/DesignTokens.kt`
- Colors and wallpaper-aware surfaces -> `shared/ui/theme/AppColors.kt`
- Shared components -> `shared/ui/components/` (use before creating new UI)
- Never hardcode dp values, corner radii, or color literals where tokens exist
- Always include `modifier: Modifier = Modifier` in reusable composables
- Keep composables stateless; hoist state to ViewModel/parent
- Add a new design token only when the value is reused across multiple places; otherwise use a top-level `const` in the file
- Device/orientation helpers: `shared/util/DeviceUtils.kt` (preserve tablet behavior)

## Repositories

- `AppsRepository.kt`, `ContactRepository.kt`, `FileSearchRepository.kt`, `CalendarRepository.kt`
- `search/data/appShortcutRepository/AppShortcutRepository.kt`
- `search/deviceSettings/DeviceSettingsRepository.kt`
- Extend existing repositories/handlers before introducing new abstractions

## Preferences

- Central facade: `search/data/userAppPreferences/UserAppPreferences.kt`
- Startup preference facade: `search/data/userAppPreferences/StartupPreferencesFacade.kt`
- Specialized modules: `search/data/preferences/`
- `SearchHistoryPreferences.kt` stays in `search/searchHistory/`
- New preferences go in the correct module under `preferences/` and are exposed through `UserAppPreferences`

## Search & Ranking

- Ranking: exact > startsWith > second-word startsWith > contains (`SearchRankingUtils.kt`)
- Fuzzy layer: typo tolerance, acronym matching, nickname support (`FuzzySearchEngine.kt`)
- Empty query -> pinned + recent apps
- Non-empty query -> ranked results + fuzzy enhancements
- Contacts/files/settings/shortcuts/calendar are debounced via `SecondarySearchOrchestrator`; preserve query version checks and no-result cache behavior

## Permissions & Graceful Degradation

- Required: usage stats
- Optional: contacts, calendar, files/storage, call phone, package visibility, wallpaper access, overlay permission
- When a permission is unavailable, hide/degrade the section through state; never crash or show broken UI

## Standard Workflows

### Add/update a search section
1. Add/update models (`search/models/` or feature models)
2. Add/update repository or handler logic
3. Add/update state in `SearchUiState` (`SearchModels.kt`/`SearchStateModels.kt`)
4. Wire orchestration in ViewModel/core handlers
5. Render section composable (typically through `SectionRenderingComposables.kt` + `SearchRoute.kt`)
6. Integrate section rendering and ordering
7. Add settings toggles/preferences if user-configurable

### Add a new preference
1. Add preference in `search/data/preferences/`
2. Expose through `search/data/userAppPreferences/UserAppPreferences.kt`
3. Reflect in `SearchUiState` if needed for runtime UI
4. Update settings UI and mappers
5. Verify persistence and default handling

### Modify search UI
1. Locate the composable in `search/searchScreen/` or `search/searchScreen/searchScreen/`
2. Use shared tokens/components
3. Verify wallpaper mode and one-handed mode behavior
4. Verify overlay mode if a shared rendering path is touched

## Performance Guardrails

- Keep heavy work off the main thread
- Prefer debounced/reused search pipelines over new parallel ad-hoc searches
- Reuse existing caches; don't duplicate them
- Avoid blocking work in early startup paths (`app/startup/StartupCoordinator.kt`, `search/core/SearchStartupCoordinator.kt`)

## Security & Privacy

- Local-first; do not add unsolicited analytics or tracking
- API keys and sensitive values must use existing encrypted storage patterns
- Preserve intent safety and permission checks around external app integrations

## Testing Checklist (Minimum)

- Build compiles
- Search verified for: empty query, normal query, typo/acronym query (if search logic touched)
- Permission-off states verified for impacted sections
- Wallpaper mode + standard mode verified if UI touched
- Overlay mode sanity check if shared UI/state touched
- Settings persistence verified when preferences are changed

## Do / Do Not

**Do:**
- Follow existing package structure and naming
- Extend existing handlers/repositories before introducing new abstractions
- Reuse design tokens and shared components
- Put user-facing strings in `strings.xml`
- Remove unused code when touching related areas
- Create reusable functions/components when there is a meaningful chance of duplication

**Do Not:**
- Hardcode colors/spacing where tokens already exist
- Bypass ViewModel/StateFlow with UI-local business logic
- Mix unrelated refactors into feature changes
- Introduce speculative abstractions or unnecessary complexity

---

Last updated: 2026-04-03
