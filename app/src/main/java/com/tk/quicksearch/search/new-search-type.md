# AI Agent Guide: Add a New Search Setting

This document is for AI coding agents working in `search/`.
Read `AGENTS.md` in the repo root for the full architecture playbook before starting. Follow this sequence exactly to add a new setting safely.

## Scope and constraints

1. Do not bypass MVVM + unidirectional state flow.
2. Do not add business logic directly in composables.
3. Do not hardcode UI colors/spacing when tokens exist.
4. Keep changes minimal and localized; avoid unrelated refactors.
5. Put all user-facing text in `app/src/main/res/values/strings.xml`.

## Implementation workflow

### 1) Define persistence

1. Add the setting in the correct file under `search/data/preferences/` (or extend an existing related preference class).
2. Set an explicit default value.
3. Keep naming consistent (`isXEnabled`, `showX`, `xMode`, etc.).

### 2) Expose via central preferences facade

1. Add accessors/mutators in `search/data/userAppPreferences/UserAppPreferences.kt`.
2. If startup loading is required, integrate with existing startup preference flow only.
3. Do not introduce new blocking startup work.

### 3) Add runtime state

1. Add required state to `search/core/SearchModels.kt` and/or `search/core/SearchStateModels.kt`.
2. Maintain immutable updates (`copy(...)`).
3. Use existing sealed-state patterns when the setting affects section visibility/loading/no-results.

### 4) Wire ViewModel orchestration

1. Update `search/core/SearchViewModel.kt` (or companion core handlers) to:
   - read initial setting state
   - observe updates
   - expose intent handlers to mutate setting
2. Route all state updates through existing ViewModel update helpers.

### 5) Update Settings UI

1. Add/extend UI in the appropriate settings package (`settings/`, `settings/navigation/`, `settings/settingsDetailScreen/`, or feature-specific settings package).
2. Reuse components from `shared/ui/components/`.
3. Use tokens from `shared/ui/theme/DesignTokens.kt` and colors from `shared/ui/theme/AppColors.kt`.
4. Keep composables stateless where possible; hoist state.

### 6) Connect behavior to search flow

1. If setting affects search results/order/sections, update relevant logic in:
   - `search/core/`
   - feature repositories/handlers
2. Preserve existing debounce/query-version/no-result-cache behavior.
3. Preserve ranking expectations unless the setting explicitly changes them.

## Required validation checklist

1. Build compiles.
2. Setting persists after app restart.
3. Search sanity:
   - empty query
   - normal query
   - typo/acronym query (if affected)
4. Permission-off behavior degrades cleanly for impacted sections.
5. Wallpaper mode + standard mode sanity check if UI changed.
6. Overlay mode sanity check if shared search UI/state changed.

## High-risk files (extra caution)

1. `search/core/SearchViewModel.kt`
2. `search/core/SearchModels.kt`
3. `search/core/SearchStateModels.kt`
4. `search/core/SearchSectionRegistry.kt`
5. `searchEngines/SecondarySearchOrchestrator.kt`
6. `search/data/userAppPreferences/UserAppPreferences.kt`
