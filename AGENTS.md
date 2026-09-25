# Quick Search Agent Guide

Repository playbook for coding agents. When prose and code disagree, trust the code and build files.

## Working rules

- Keep changes narrow; preserve unrelated work in the tree. Don't mix feature work with speculative refactors or cleanup.
- Don't `git add`, commit, tag, push, or publish without explicit permission.
- Don't install on or drive the attached device unless asked. Bug reports are often from other users' devices, and installing resets the user's accessibility grant (see Validation).
- Don't run instrumented/Compose UI tests unless asked; the user does manual UI testing.
- A successful build proves only that it builds. Don't claim a visual state, gesture, keyboard interaction, provider response, or intermittent issue is fixed without reproducing it.
- In app action menus, never leave placeholder gaps between options; reflow so any gap is only at the end of the last row.

## Project shape

- Android launcher + unified search app: Kotlin, Jetpack Compose, Material 3. MVVM with unidirectional data flow, `StateFlow`, immutable state.
- Modules: `:app` and `:benchmark` (macrobenchmark/baseline profile).
- Flavors `standard` (Play review/update libs) and `fdroid` (no Play libs). Flavor-specific code lives in `app/src/standard/` and `app/src/fdroid/`; shared code in `app/src/main/`.
- App ID `com.tk.quicksearch`; debug builds use `com.tk.quicksearch.debug` ("QS Debug"), which is separate from the user's installed app but holds their real test data.
- Source paths below are relative to `app/src/main/java/com/tk/quicksearch/`.
- Entry points: `app/MainActivity.kt` (regular launch, assistant/search/share/process-text/import), `app/HomeActivity.kt` (HOME role), `overlay/OverlayActivity.kt` (draw-over-apps mode).
- Persistence: modular `SharedPreferences` (secrets via the existing encrypted path). Room for notes (`search/data/notes/`) and notification history (`search/notificationHistory/`). Startup app catalog: `search/data/AppCache.kt`.

## Architecture map

### Search

- UI state models: `search/core/SearchModels.kt`, `SearchStateModels.kt`.
- Orchestrator: `search/core/SearchViewModel.kt`. Add logic to its focused delegates/API files, not the main class.
- Query flow: `search/core/SearchQueryCoordinator.kt`, `UnifiedSearchHandler.kt`, `searchEngines/SecondarySearchOrchestrator.kt`.
- `search/core/SearchSectionRegistry.kt` is the single table for section order, aliases, settings toggles, permissions, and minimum query lengths.
- Ranking/matching: `search/utils/` (matchers, normalizers, ranking), `search/fuzzy/`. Preserve query-version checks, debounce, and stale-result suppression.

### Data and preferences

- Repositories: `search/data/`; feature-specific policies/handlers stay in their feature package.
- New preference: add to the relevant class in `search/data/preferences/`, expose via `search/data/userAppPreferences/UserAppPreferences.kt`.
- Startup-critical reads go through `search/data/userAppPreferences/StartupPreferencesFacade.kt`. No one-off blocking reads in startup paths or composables.
- Search history: `search/searchHistory/SearchHistoryPreferences.kt`.
- Searchable settings catalog: `search/appSettings/AppSettingsRepository.kt`. New user-facing settings must be wired here as well as in the Settings UI.

### UI

- Search route: `search/searchScreen/searchRoute/SearchRoute.kt`. Composition: `search/searchScreen/SearchScreen.kt`, `SearchScreenContent.kt`, `SectionRenderingComposables.kt`, `searchScreenLayout/` (includes Home sections and At a Glance).
- Settings: `settings/settingsScreen/` (main screen, backup/export), `settings/settingsDetailScreen/` (detail pages, `SettingsDetailType`, routing), `settings/shared/`, plus feature-specific settings packages.
- Reuse `shared/ui/components/`, `shared/ui/theme/DesignTokens.kt`, `shared/ui/theme/AppColors.kt`. Read `DESIGN_SYSTEM.md` (repo root) before adding UI or tokens; update it when you add or rename tokens, theme modes, or shared components. For shared paths, account for wallpaper, custom background, one-handed/bottom search bar, tablet, and overlay modes.
- Other feature packages: `widgetsPanel/` (Home widget grid), `widgets/` (app widgets), `reminders/`, `media/`, `edgeGesture/`, `floatingButton/`, `pinnedNotifications/`, `tile/`, `onboarding/`.

### Strings

- All user-facing text goes in resources. Reuse an existing string when the content matches exactly; don't add duplicates.
- When adding or changing copy, update `values/strings.xml` and all 16 localized `values-*/strings.xml` files unless the user narrows scope.

### Startup and caches

- Startup is phased: `app/startup/StartupCoordinator.kt`, `search/core/SearchStartupCoordinator.kt`, `SearchStartupLifecycleDelegate.kt`. Keep expensive work off the main thread and out of the first visible phase.
- `MainActivity` and `OverlayActivity` share process-wide icon caches. Route cleanup through `app/UiSurfaceMemoryManager.kt`; don't clear caches while another surface is active.
- In `search/apps/AppIconManager.kt`: `clearAppIconMemoryCache()` evicts memory without refreshing displayed icons; `invalidateAppIconCache()` is only for explicit icon changes that must refresh the UI.

## Feature guides

Read the matching guide before implementing:

| Change | Guide |
|---|---|
| Built-in search engine | `searchEngines/new-search-engine.md` |
| Searchable app-setting row (every new user-facing setting) | `search/appSettings/new-app-setting.md` |

A new section typically touches the model/repository or handler, `SearchUiState`, `SearchSectionRegistry`, orchestration, rendering/order, permission degradation, preferences, and the searchable app-setting entry. A new tool lives in its own `tools/<name>/` package, following `calculator/` and `unitConverter/`. It also touches `search/core/SearchHandlerContainer.kt`, `search/core/SearchToolCoordinator.kt`, `settings/settingsDetailScreen/ToolSettingsRegistry.kt`, and `searchEngines/AliasHandler.kt`.

## Guardrails

- UI reads state and emits events; repositories, handlers, and the ViewModel own business and permission logic.
- Use immutable `copy(...)` updates and existing visibility/loading/result state types.
- Extend existing repositories, policies, registries, and delegates rather than creating parallel abstractions.
- Provider and network work stays off the main thread. Local-first; no analytics or tracking.
- On permission denial, hide or degrade only the affected feature; never crash or block unrelated search.
- Check both flavor source sets when changing review/update behavior, typography, or distribution defaults.
- If a touched Kotlin file is already very large, put new cohesive logic in a focused file or delegate.

## Validation

- Compile check: `./gradlew :app:compileStandardDebugKotlin` (add `:app:compileFdroidDebugKotlin` when flavor code changes). Plain `compileDebugKotlin` doesn't exist because of flavors. Confirm `BUILD SUCCESSFUL`; don't just grep for `e:` lines, which hides task-not-found failures.
- Run focused unit tests (`app/src/test/`) for changed pure logic, e.g. `./gradlew :app:testStandardDebugUnitTest --tests '<pattern>'`.
- After resource XML changes, check every affected `values*/strings.xml` and run `git diff --check`.
- Finish a coding task with `./gradlew assembleStandardDebug`; the APK is at `app/build/outputs/apk/standard/debug/app-standard-debug.apk`. Report its path rather than installing.
- Only when the user asks for on-device verification:

```bash
./gradlew assembleStandardDebug && adb install --user 0 -r app/build/outputs/apk/standard/debug/app-standard-debug.apk && adb shell am force-stop com.tk.quicksearch.debug && adb shell am start -W -n com.tk.quicksearch.debug/com.tk.quicksearch.app.MainActivity
```

  Both `adb install` and `am force-stop` revoke the app's accessibility service grant (edge gesture, lock action). Check with `adb shell settings get secure enabled_accessibility_services`. If it's gone, ask the user to re-enable it; don't write that setting over adb.
- Gradle cache permission/lock failures: set `GRADLE_USER_HOME=$PWD/.gradle-codex`. Kotlin daemon marker failures: add `-Pkotlin.compiler.execution.strategy=in-process`.

## Release

- For F-Droid work, read `docs/FDROID.md` and use the `fdroid-release` skill (`.claude/skills/fdroid-release/`).
- User-facing release notes: `app/src/main/assets/RELEASE_NOTES.md`; feature list: `app/src/main/assets/FEATURES.md`. Version is in `app/build.gradle.kts`.
- Version tags are immutable. Before any authorized publish, align version name/code, release notes, artifact, and tag, then verify the remote result.
