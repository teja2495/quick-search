# Adding a New Setting (AI Agent Instructions)

These instructions are for AI coding agents working in this repository.
Follow this flow when implementing a new setting in Quick Search.

## 1) Define the preference

1. Add the preference in the correct class under `search/data/preferences/`.
2. Keep naming consistent (`*Preference` style, clear key names, sensible defaults).
3. If the setting is startup-critical, make sure it fits existing startup preference loading phases.

## 2) Expose through the central facade

1. Wire getters/setters through `search/data/userAppPreferences/UserAppPreferences.kt`.
2. Keep `UserAppPreferences` as the single access point for feature code.

## 3) Connect to runtime state (if UI/search behavior depends on it)

1. Add state to `SearchUiState` in `search/core/SearchModels.kt` or related models in `search/core/SearchStateModels.kt`.
2. Update `SearchViewModel` to read/write the setting via immutable `copy(...)` state updates.
3. Avoid placing business logic directly in composables.

## 4) Add settings UI

1. Add/update the correct settings screen in `settings/` (usually under `settingsDetailScreen/`, `appearanceSettings/`, `searchEngineSettings/`, or feature-specific settings package).
2. Reuse shared components from `shared/ui/components/`.
3. Use design tokens from `shared/ui/theme/DesignTokens.kt` and colors from `shared/ui/theme/AppColors.kt`.
4. Put all user-facing text in `app/src/main/res/values/strings.xml`.

## 5) Wire feature behavior

1. Apply the setting where behavior is implemented (repository/handler/viewmodel), not inside UI-only logic.
2. Preserve existing architecture and flow: repository -> ViewModel -> `SearchUiState` -> composables.
3. Keep changes localized; avoid unrelated refactors.

## 6) Validate

- Project compiles.
- Setting persists across app restart.
- Default value behavior is correct.
- UI reflects state changes immediately.
- Permission-off/degraded states still behave correctly (if relevant).
- Overlay and wallpaper modes still render correctly if shared UI is impacted.

## Notes

- Follow package conventions in `AGENTS.md`.
- Keep preference modules modular and delegate through `UserAppPreferences`.
- If multiple screens/features may reuse the setting UI pattern, extract a shared component.
- Keep changes focused; do not mix unrelated refactors.
