# Add a New Built-in Search Engine (AI Agent Guide)

Use this checklist when adding a **new built-in** search engine under `searchEngines/`.

## Scope

This guide is for adding a `SearchEngine` enum-backed engine (not user-created custom engines from settings).

## Architecture Constraints

- Keep search engine metadata centralized in `SearchEngineRegistry.kt`.
- Do not add ad-hoc per-engine conditionals unless unavoidable.
- Reuse existing flow through `SearchEngineManager`, `SearchEngineUtils`, and settings UI.
- Keep changes minimal and localized; avoid unrelated refactors.

## Required Changes

1. Add enum value
- File: `app/src/main/java/com/tk/quicksearch/search/core/SearchModels.kt`
- Update `enum class SearchEngine` with the new engine constant.

2. Add registry definition
- File: `app/src/main/java/com/tk/quicksearch/searchEngines/SearchEngineRegistry.kt`
- Add a `SearchEngineDefinition(...)` entry with:
  - `engine`
  - `drawableResId`
  - `contentDescriptionResId`
  - `urlTemplate` (must contain `%s` for query insertion)
  - `defaultShortcutCode`
- Set optional fields only when needed:
  - `homeUrl`
  - `appPackages`
  - `installOnly`
  - `defaultDisabledOnFirstRun`
  - `defaultDisableIfAppMissing`
  - `iconColorPolicy`
  - `nativeLaunchMode`

Important:
- `SearchEngineRegistry` has init checks for missing/duplicate mappings. Build will fail if enum and registry diverge.

3. Add package constant (if app install detection or native launch is needed)
- File: `app/src/main/java/com/tk/quicksearch/shared/util/PackageConstants.kt`
- Add package name constant and reference it in `SearchEngineRegistry` `appPackages`.

4. Add icon resource
- Add drawable in `app/src/main/res/drawable/`.
- Use naming pattern consistent with existing search engines (lowercase snake_case).

5. Add user-facing string
- File: `app/src/main/res/values/strings.xml`
- Add `search_engine_<name>` (or project-consistent key) and wire it to `contentDescriptionResId`.

## Optional Changes (Only If Needed)

1. Native in-app launch behavior
- Files:
  - `app/src/main/java/com/tk/quicksearch/searchEngines/SearchEngineRegistry.kt`
  - `app/src/main/java/com/tk/quicksearch/searchEngines/SearchEngineUtils.kt`
  - `app/src/main/java/com/tk/quicksearch/searchEngines/SearchTargetUtils.kt`
- Add `SearchEngineNativeLaunchMode` enum value and handling if the target supports native app launch.

2. Special URL behavior
- File: `app/src/main/java/com/tk/quicksearch/searchEngines/SearchEngineUtils.kt`
- Avoid special-casing unless absolutely required (example: Amazon domain behavior already exists).

3. Settings-specific controls
- If engine needs dedicated settings UI, follow existing patterns in:
  - `app/src/main/java/com/tk/quicksearch/settings/searchEngineSettings/`
- Keep generic engines on existing reusable UI paths.

## Validation Checklist (Minimum)

1. Compile check
- Run: `./gradlew :app:compileDebugKotlin`

2. Functional checks
- Engine appears in search engine settings list.
- Enable/disable works and persists after app restart.
- Reorder works and persists.
- Search launches correct URL for:
  - non-empty query
  - empty query (home/base URL behavior)

3. State and regression checks
- No crashes from registry mapping checks.
- Alias/default shortcut code works with existing alias flow.
- Compact and inline search engine UIs still render correctly.

## Common Pitfalls

- Added enum value but forgot `SearchEngineRegistry` entry.
- Added registry entry but missing drawable or string resource.
- `urlTemplate` missing `%s`, causing invalid query substitution.
- Overusing engine-specific conditionals instead of registry metadata.

## Notes on Custom Engines

Custom engines (user-added URL templates) are handled separately via:
- `SearchEngineManager.kt`
- `CustomSearchEngineUtils.kt`
- settings dialogs under `settings/searchEngineSettings/dialogs/`

Do not mix custom-engine logic into built-in engine onboarding unless explicitly required.
