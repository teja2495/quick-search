# Adding a New App Setting Result (AI Agent Guide)

This guide is for AI coding agents working in this repository.
Follow `AGENTS.md`: keep changes minimal, use existing architecture, and avoid unrelated refactors.

## 1) Choose the setting type

1. Use `NAVIGATE` when the row should open a settings detail page or trigger an action (reload, feedback, etc.).
2. Use `TOGGLE` when the row should change a boolean app behavior.

## 2) Add strings first

1. Add title/description strings in `app/src/main/res/values/strings.xml`.
2. Keep text concise and searchable.
3. Add keyword synonyms only when they improve search intent matching.

## 3) Define enum key (if needed)

1. For navigation: add a value to `AppSettingsDestination` in `AppSettingResult.kt` if no existing destination fits.
2. For toggle: add a value to `AppSettingsToggleKey` in `AppSettingResult.kt` if no existing key fits.

## 4) Register the result in repository

1. Add the row in `AppSettingsRepository.loadSettings()` using `addNavigation(...)` or `addToggle(...)`.
2. Use a stable, unique `id` (do not rename existing ids unless migration is intended).
3. Point `titleRes`/`descriptionRes` to new strings.
4. Add `keywords` only when they improve discovery.

## 5) Wire navigation destinations (NAVIGATE only)

1. If destination opens a settings detail screen, map it in `app/navigation/AppSettingsDestinationMapper.kt` (`toSettingsDetailTypeOrNull`).
2. If destination is an action (not a detail screen), handle it in `app/navigation/AppSettingsDestinationHandler.kt` (`handleAppSettingsDestination`).
3. If a new settings detail type is needed, add it in `settings/settingsDetailScreen/` and wire navigation as done for existing types.

## 6) Wire toggle behavior and state (TOGGLE only)

1. Update `settings/shared/SettingsCommands.kt`:
   - `applySettingsCommand(...)` for write behavior.
   - `SearchUiState.isAppSettingToggleEnabled(...)` for read/checked state.
2. If behavior already has dedicated handling in `SearchRoute.kt` (for example permission-gated toggles), follow that pattern instead of bypassing it.
3. Back the toggle with existing ViewModel + preference flow (ViewModel setter -> `UserAppPreferences` -> `SearchUiState`).

## 7) Handle special UI behavior (only if required)

1. Add row-specific UI logic in `AppSettingsResultsSection.kt` only when needed (example: slider/chips for special keys).
2. Keep generic rows untouched when possible.

## 8) Keep visibility rules safe

1. If the setting should be conditionally hidden, add that logic in `AppSettingsSearchHandler.getVisibleSettings()`.
2. Avoid permission/business checks directly in composables when state can represent it.

## 9) Validate

- Build compiles.
- New app setting appears in app settings search.
- Search keywords find the new row.
- Toggle stays persisted and restores after app restart (if toggle type).
- Navigation/action opens the correct target (if navigate type).
- Overlay mode and permission-off behavior still work if impacted.

## Common pitfalls

- Added repository row but forgot destination/toggle enum.
- Added destination enum but forgot mapper/handler wiring.
- Added toggle enum but forgot `SettingsCommands.kt` read/write mapping.
- Hardcoded text instead of `strings.xml`.
