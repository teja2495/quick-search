# Quick Search Agent Guide

Repository playbook for coding agents. When prose and code disagree, trust the code and build files.

## Working rules

- Keep changes narrow; preserve unrelated work in the tree. Don't mix feature work with speculative refactors or cleanup.
- Don't `git add`, commit, tag, push, or publish without explicit permission.
- Don't edit `app/src/main/assets/FEATURES.md` or `RELEASE_NOTES.md` as part of feature work; the user updates them before a release. Touch them only when asked.
- `scripts/verify.sh` installs and launches the debug build on all connected USB devices, or all wireless devices when none use USB (`ANDROID_SERIAL` overrides this); that is expected. Beyond that, don't drive the device (taps, gestures, settings changes) unless asked. Bug reports are often from other users' devices, so a local run doesn't prove their bug is fixed.
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
- Persistence: modular `SharedPreferences` (secrets via the existing encrypted path). Room for notes (`search/data/notes/`), bookmarks (`search/data/bookmarks/`), and notification history (`search/notificationHistory/`). Startup app catalog: `search/data/AppCache.kt`.

## Architecture map

### Search

- UI state models: `search/core/SearchModels.kt`, `SearchStateModels.kt`.
- Orchestrator: `search/core/SearchViewModel.kt`. Add logic to its focused delegates/API files, not the main class.
- Query flow: `search/core/SearchQueryCoordinator.kt`, `UnifiedSearchHandler.kt`, `searchEngines/SecondarySearchOrchestrator.kt`.
- `search/core/SearchSectionRegistry.kt` is the single table for section order, aliases, settings toggles, permissions, and minimum query lengths.
- Ranking/matching: `search/utils/` (matchers, normalizers, ranking), `search/fuzzy/`. Preserve query-version checks, debounce, and stale-result suppression.

### Data and preferences

- Repositories: `search/data/`; feature-specific policies/handlers stay in their feature package.
- Plain searchable boolean settings use `search/appSettings/BooleanSetting.kt`. For other preferences, use the relevant class in `search/data/preferences/` and expose it through `UserAppPreferences`.
- Startup-critical reads go through `search/data/userAppPreferences/StartupPreferencesFacade.kt`. No one-off blocking reads in startup paths or composables. Add new typed snapshot fields to `StartupPreferences` and decode them once in `StartupPreferenceDecoder.kt`.
- Search history: `search/searchHistory/SearchHistoryPreferences.kt`.
- Searchable settings catalog: `search/appSettings/AppSettingsRepository.kt`. New user-facing settings must be wired here as well as in the Settings UI.

### UI

- Search route: `search/searchScreen/searchRoute/SearchRoute.kt`. Composition: `search/searchScreen/SearchScreen.kt`, `SearchScreenContent.kt`, `SectionRenderingComposables.kt`, `searchScreenLayout/` (Home sections; At a Glance rows in `searchScreenLayout/glance/`). At a Glance data (notification parsers, `GlanceNotificationsStore`, `GlanceTiming`) lives in `search/glance/`.
- Settings: `settings/settingsScreen/` (main screen, backup/export), `settings/settingsDetailScreen/` (shared detail pages, `SettingsDetailType`, routing), `settings/shared/`, plus feature-specific `settings/<feature>Settings/` packages (`aiSettings`, `gestureSettings`, `toolsSettings`, `notesSettings`, `remindersSettings`, `calendarSettings`, `aboutSettings`, …). Put a new feature's settings UI in its own `<feature>Settings/` package, not in `settingsDetailScreen/`.
- **Material 3 first.** All new UI follows the Material 3 guidelines (m3.material.io), including Material 3 Expressive. Before designing a screen or component, read `DESIGN_SYSTEM.md` (repo root), name the Material 3 pattern it follows, and build it from the shared wrappers in `shared/ui/components/` and `settings/shared/`, plus the tokens in `shared/ui/theme/` (`DesignTokens`, `AppColors`, `AppShapes`, `AppMotion`, the type scale, `MaterialSymbols`). Don't use raw Material buttons or `DropdownMenu`, hex colors, ad hoc radii, inline springs, or one-off `TextStyle`s. Keep 48dp touch targets, 4.5:1 text contrast, and content descriptions.
- Work through the "Checklist for new UI" in `DESIGN_SYSTEM.md`. It covers every background and layout mode: theme gradient, wallpaper, custom image, AMOLED, Material You, light and dark, overlay, one-handed/bottom search bar, Compact Layout, and tablet.
- If no Material 3 pattern fits, or you must deviate from one, say so when proposing the design and record the decision in `DESIGN_SYSTEM.md`. Update `DESIGN_SYSTEM.md` in the same change when you add, rename, or remove a token, theme mode, or shared component.
- Other feature packages: `widgetsPanel/` (Home widget grid), `widgets/` (app widgets), `reminders/`, `customInfo/` (At a Glance Custom Info), `appNotifications/` (At a Glance app notifications), `media/`, `edgeGesture/`, `notchGesture/`, `floatingButton/`, `pinnedNotifications/`, `tile/`, `onboarding/`.

### Strings

- All user-facing text goes in resources. Reuse an existing string when the content matches exactly; don't add duplicates.
- When adding or changing copy, update `values/strings.xml` and all 16 localized `values-*/strings.xml` files unless the user narrows scope. `StringResourceParityTest` (and `scripts/check_strings.py`) fail on missing or stale keys and mismatched format arguments. Use the `localize-strings` skill (`.claude/skills/localize-strings/SKILL.md`) and `scripts/add_strings.py` to write all locales at once.

### Startup and caches

- Startup is phased: `app/startup/StartupCoordinator.kt`, `search/core/SearchStartupCoordinator.kt`, `SearchStartupLifecycleDelegate.kt`. Keep expensive work off the main thread and out of the first visible phase.
- `MainActivity` and `OverlayActivity` share process-wide icon caches. Route cleanup through `app/UiSurfaceMemoryManager.kt`; don't clear caches while another surface is active.
- In `search/apps/AppIconManager.kt`: `clearAppIconMemoryCache()` evicts memory without refreshing displayed icons; `invalidateAppIconCache()` is only for explicit icon changes that must refresh the UI.

### Performance and lifecycle

Build new features this way from the start rather than fixing them in a later "performance and lifecycle" pass. When planning one, state which of these apply.

- **Threads:** Run provider queries, `ContentResolver`, `PackageManager`, `AppWidgetManager`, disk, and bitmap decoding on `Dispatchers.IO`. Run CPU work such as parsing, ranking, and fuzzy matching on `Dispatchers.Default`. Never do either in a composable body or on the first frame.
- **Cancellation:** Per-query work must be cancellable and must drop stale results (follow `SearchToolCoordinator` and `WebSuggestionHandler`). Call `ensureActive()` in long loops. Open network calls inside `withCancellableHttp { ... }` with `.trackCancellation()` (`tools/aiSearch/CancellableHttp.kt`) so cancellation actually disconnects them. When starting a new refresh, cancel the previous `Job` instead of stacking another one.
- **Queries:** Request only the columns you need, push filters into the selection, and apply a `limit`. Reuse snapshots or caches you already have before querying again (for example, `CalendarRepository` checks the cached search rows first, then fetches only the missing IDs).
- **Caches:** Bound memory caches with `LruCache` (by bytes for bitmaps), decode images at the size they're drawn, and key them by everything that changes their output. Invalidate them with a `ContentObserver` or change tracker (`ContactsChangeTracker`), not on a timer. Free memory through `UiSurfaceMemoryManager` or an `onSurfaceVisibilityChanged` / `releaseMemoryCaches()` hook, following `CalendarSearchRowsCache`, `FileNameIndex`, and `ContactRepository`.
- **Lifecycle:** Register observers, listeners, and widget hosts in a `DisposableEffect`, or in the start/stop hooks, and unregister them on dispose. Re-check permissions and settings that can change in the background when the screen resumes (`rememberResumeRefreshKey()`), not on every recomposition. Remember that `OverlayActivity` and `MainActivity` can both be alive at once.
- **Compose:** Hoist and `remember` formatters, parsed values, and lookups by their real inputs. Read state as late as possible, and use `derivedStateOf` for values computed from scroll or animation state. Give lazy list items stable `key`s. For long first renders, reveal rows progressively (`rememberProgressiveRowCount` in contacts) instead of composing everything in one frame.
- **Startup:** New startup work joins a later `StartupPhase`, not the first visible one. Preference reads go through the startup snapshot, not one-off reads.
- **Measuring:** Use the existing perf loggers (`AppSearchPerformanceLogger`, `FuzzySearchPerformanceLogger`) or the `:benchmark` module when a change claims a speedup. Don't report a performance gain you haven't measured.

## Feature guides

Read the matching guide before implementing. Claude Code loads the skill of the same name from `.claude/skills/` automatically; other agents should open the file.

- Built-in search engine: `searchEngines/new-search-engine.md`
- Searchable app-setting row (every new user-facing setting): `search/appSettings/new-app-setting.md`
- New search tool (`tools/<name>/`): `.claude/skills/new-search-tool/SKILL.md`
- New search result section: `.claude/skills/new-search-section/SKILL.md`
- Adding or changing user-facing strings: `.claude/skills/localize-strings/SKILL.md`

## Guardrails

- UI reads state and emits events; repositories, handlers, and the ViewModel own business and permission logic.
- Use immutable `copy(...)` updates and existing visibility/loading/result state types.
- Extend existing repositories, policies, registries, and delegates rather than creating parallel abstractions.
- Provider and network work stays off the main thread. Local-first; no analytics or tracking.
- On permission denial, hide or degrade only the affected feature; never crash or block unrelated search.
- Check both flavor source sets when changing review/update behavior, typography, or distribution defaults.
- Kotlin files stay at or under 800 lines (`scripts/verify.sh` enforces it). If a change would push a file past that, move cohesive logic into a focused file or delegate first.

## Validation

- **Definition of done:** `scripts/verify.sh` prints `VERIFY PASSED`. It runs whitespace, string parity, and file-size checks, the standard flavor compile, and `assembleStandardDebug`. It also runs the full standard unit suite when the `versionCode` in `app/build.gradle.kts` differs from `HEAD`, so verify a version bump before committing it. The full Gradle log is in `build/verify-gradle.log`. It installs and launches `com.tk.quicksearch.debug` on all USB devices when any are connected, otherwise all wireless devices (`ANDROID_SERIAL` overrides this) (this revokes the accessibility grant; the script says when it is off). Use `--no-assemble` for intermediate checks and `--no-device` to skip the install. Report the APK path (`app/build/outputs/apk/standard/debug/app-standard-debug.apk`) and whether it was installed.
- Faster iteration: `./gradlew -q :app:compileStandardDebugKotlin` (plus `:app:compileFdroidDebugKotlin` for flavor code; plain `compileDebugKotlin` doesn't exist), `./gradlew -q :app:testStandardDebugUnitTest --tests '<pattern>'`, and `python3 scripts/check_strings.py` after resource XML changes.
- On-device verification only when asked: use the `device-verify` skill (`.claude/skills/device-verify/SKILL.md`).
- Formatting: follow `.editorconfig` and the surrounding code. The codebase is not ktlint-clean, so don't run ktlint or any formatter over whole files.
- Gradle cache permission/lock failures: set `GRADLE_USER_HOME=$PWD/.gradle-codex`. Kotlin daemon marker failures: add `-Pkotlin.compiler.execution.strategy=in-process`.

## Release

- Release notes: `app/src/main/assets/RELEASE_NOTES.md`; feature list: `FEATURES.md` beside it; version in `app/build.gradle.kts`. F-Droid releases: use the `fdroid-release` skill (`.claude/skills/fdroid-release/`).
- Version tags are immutable. Before any authorized publish, align version name/code, release notes, artifact, and tag, then verify the remote result.
