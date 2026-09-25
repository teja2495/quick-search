# Quick Search Agent Guide

Repository playbook for coding agents. When prose and code disagree, trust the code and build files.

## Working rules

- Keep changes narrow; preserve unrelated work in the tree. Don't mix feature work with speculative refactors or cleanup.
- Don't `git add`, commit, tag, push, or publish without explicit permission.
- `scripts/verify.sh` installs and launches the debug build when one device is connected; that is expected. Beyond that, don't drive the device (taps, gestures, settings changes) unless asked. Bug reports are often from other users' devices, so a local run doesn't prove their bug is fixed.
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
- When adding or changing copy, update `values/strings.xml` and all 16 localized `values-*/strings.xml` files unless the user narrows scope. `StringResourceParityTest` (and `scripts/check_strings.py`) fail on missing or stale keys and mismatched format arguments.

### Startup and caches

- Startup is phased: `app/startup/StartupCoordinator.kt`, `search/core/SearchStartupCoordinator.kt`, `SearchStartupLifecycleDelegate.kt`. Keep expensive work off the main thread and out of the first visible phase.
- `MainActivity` and `OverlayActivity` share process-wide icon caches. Route cleanup through `app/UiSurfaceMemoryManager.kt`; don't clear caches while another surface is active.
- In `search/apps/AppIconManager.kt`: `clearAppIconMemoryCache()` evicts memory without refreshing displayed icons; `invalidateAppIconCache()` is only for explicit icon changes that must refresh the UI.

## Feature guides

Read the matching guide before implementing. Claude Code loads the skill of the same name from `.claude/skills/` automatically; other agents should open the file.

- Built-in search engine: `searchEngines/new-search-engine.md`
- Searchable app-setting row (every new user-facing setting): `search/appSettings/new-app-setting.md`
- New search tool (`tools/<name>/`): `.claude/skills/new-search-tool/SKILL.md`
- New search result section: `.claude/skills/new-search-section/SKILL.md`

## Guardrails

- UI reads state and emits events; repositories, handlers, and the ViewModel own business and permission logic.
- Use immutable `copy(...)` updates and existing visibility/loading/result state types.
- Extend existing repositories, policies, registries, and delegates rather than creating parallel abstractions.
- Provider and network work stays off the main thread. Local-first; no analytics or tracking.
- On permission denial, hide or degrade only the affected feature; never crash or block unrelated search.
- Check both flavor source sets when changing review/update behavior, typography, or distribution defaults.
- Kotlin files stay at or under 800 lines (`scripts/verify.sh` enforces it). If a change would push a file past that, move cohesive logic into a focused file or delegate first.

## Validation

- **Definition of done:** `scripts/verify.sh` prints `VERIFY PASSED`. It runs whitespace, string parity, and file-size checks, the standard flavor compile, all unit tests, and `assembleStandardDebug`, and prints only the errors on failure (full log in `build/verify-gradle.log`). When one device is connected it also installs and launches `com.tk.quicksearch.debug` there (this revokes the accessibility grant; the script says when it is off). Use `--no-assemble` for intermediate checks and `--no-device` to skip the install. Report the APK path (`app/build/outputs/apk/standard/debug/app-standard-debug.apk`) and whether it was installed.
- Faster iteration: `./gradlew -q :app:compileStandardDebugKotlin` (plus `:app:compileFdroidDebugKotlin` for flavor code; plain `compileDebugKotlin` doesn't exist), `./gradlew -q :app:testStandardDebugUnitTest --tests '<pattern>'`, and `python3 scripts/check_strings.py` after resource XML changes.
- On-device verification only when asked: use the `device-verify` skill (`.claude/skills/device-verify/SKILL.md`).
- Formatting: follow `.editorconfig` and the surrounding code. The codebase is not ktlint-clean, so don't run ktlint or any formatter over whole files.
- Gradle cache permission/lock failures: set `GRADLE_USER_HOME=$PWD/.gradle-codex`. Kotlin daemon marker failures: add `-Pkotlin.compiler.execution.strategy=in-process`.

## Release

- Release notes: `app/src/main/assets/RELEASE_NOTES.md`; feature list: `FEATURES.md` beside it; version in `app/build.gradle.kts`. F-Droid releases: use the `fdroid-release` skill (`.claude/skills/fdroid-release/`).
- Version tags are immutable. Before any authorized publish, align version name/code, release notes, artifact, and tag, then verify the remote result.
