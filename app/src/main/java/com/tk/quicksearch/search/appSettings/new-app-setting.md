# Adding a Searchable App-Setting Row

App-setting rows are the Quick Search settings that appear as search results. Every new
user-facing setting needs one. Paths are relative to `app/src/main/java/com/tk/quicksearch/`.

## Plain on/off settings

1. Add one entry to `BooleanSettings` in `search/appSettings/BooleanSetting.kt`. Give it a
   stable preference key, a default, a stable search row ID, and string resources. Add keywords if
   useful. Search registration, startup hydration, and backup restore use that definition.
2. Place a `BooleanSettingTile` on the existing Settings page. Pass the definition,
   `state.booleanSettings`, and `callbacks.onApplySettingsCommand`. Use the same state and command
   if the page needs a custom row.
3. At the feature's point of use, read `uiState.isBooleanSettingEnabled(definition)`. Background
   work can read the setting through `UserAppPreferences.isBooleanSettingEnabled`.

See `FILE_PREVIEWS` and its tile in `settings/settingsDetailScreen/FileSettings.kt`. For strings,
use `.claude/skills/localize-strings/SKILL.md`. Use the dedicated command path below for settings
that need an action, permission prompt, or coordinated side effect.

## Existing registration paths

- **Search section toggles** come from `SearchSectionRegistry` (`appSettingsToggleKey`) via
  `searchSectionToggleRows()`, with row metadata from `AppSettingsCatalogRows.kt`. Reads and writes
  route through the section state and `setSectionEnabled`.
- **Inline tools** supply their own `BooleanSetting` in the inline tool definition. See
  `.claude/skills/new-search-tool/SKILL.md`. Other tools use their existing settings commands.

For anything else, pick a type:

- `NAVIGATE`: opens a settings page or runs an action (reload, feedback, create note, …).
- `TOGGLE`: flips a boolean.

## 1) Strings

Title (and optional description) in `values/strings.xml` plus all 16 localized
`values-*/strings.xml`. Reuse an existing string when the text matches exactly.

## 2) Enum key

In `search/appSettings/AppSettingResult.kt`, reuse or add:

- `AppSettingsDestination` for NAVIGATE
- `AppSettingsToggleKey` for TOGGLE

`AppSettingResult` requires exactly one of `destination`/`toggleKey` matching the action, and
throws otherwise.

## 3) Register the row

In `AppSettingsRepository.loadSettings()` (pages) or `AppSettingsToggleCatalog.kt` (toggles and inline controls), call `addNavigation(...)` or `addToggle(...)`:

- `id`: stable and unique. Never rename an existing one; it keys recent-tap ranking.
- `titleRes`, optional `descriptionRes`.
- `keywords`: synonyms users might type (e.g. "theme", "colour"). They're matched in search and
  can appear as the row's dynamic description.

## 4a) NAVIGATE wiring

- **Opens a settings page:** map it in `app/navigation/AppSettingsDestinationMapper.kt`
  (`toSettingsDetailTypeOrNull`). The `when` is exhaustive, so the compiler flags a missing case.
  - A new page needs a value in `settings/settingsDetailScreen/SettingsDetailType.kt` and rendering in
    `SettingsDetailScreen.kt` (or `SettingsNestedDetailScreen.kt` for nested pages).
- **Runs an action:** map it to `null` in the mapper, then handle it in
  `app/navigation/AppSettingsDestinationHandler.kt`. That `when` ends in `else -> Unit`, so a
  missing case fails silently. Actions that need route UI (a dialog, for example) are
  intercepted in `SearchRouteSettingActions.kt` instead (see `OPEN_EVENTS_IN`).

## 4b) TOGGLE wiring

Back it with the usual flow: preference → `UserAppPreferences` → ViewModel setter →
`SearchUiState`. Then, in `settings/shared/SettingsCommands.kt`:

- Write: add the key to the `SettingsCommand.Toggle` branch of `applySettingsCommand`.
- Read: add the key to `SearchUiState.isAppSettingToggleEnabled`.

Both `when`s are exhaustive, so the compiler flags missing keys.

If turning the toggle on needs a permission, confirmation, or side effect, intercept it in
`search/searchScreen/searchRoute/SearchRouteSettingActions.kt` (see `OVERLAY_MODE`,
`DIRECT_DIAL`, `NOTIFICATION_DOTS`) instead of doing it in the ViewModel setter.

## 5) Optional

- **Hide conditionally:** add the rule in `AppSettingsSearchHandler.getVisibleSettings()`. Don't
  put this logic in composables.
- **Custom row UI** (slider, chips, etc.): special-case the key in `AppSettingsResultsSection.kt`
  only when a plain toggle or navigate row won't do.

## Validate

- `./gradlew :app:compileStandardDebugKotlin`
- Ask the user to check on device:
  - The row appears when searching its title and keywords.
  - A toggle reflects its current state and persists across restart.
  - A navigate row opens the right page or runs the action.
  - Rows hidden by conditions disappear.

## Common mistakes

- Enum added but row not registered, or row registered without mapper/handler/`SettingsCommands`
  wiring.
- Action destination added to the handler but not mapped to `null` in the mapper.
- Strings missing from the localized files.
