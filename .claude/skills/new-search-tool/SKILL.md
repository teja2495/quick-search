---
name: new-search-tool
description: Add a new inline search tool to Quick Search (a query-driven result like the calculator, unit converter, date calculator, or color visualizer). Use when asked to add a tool, converter, parser, or instant answer that turns what the user typed into a result card.
---

# New search tool

Model it on the most recent tool, the color visualizer (commit `125565d5a`). Run `git show 125565d5a` to see the real diff. Package paths have moved since then (`29189f2be organize code`), so use the current paths below. Paths are relative to `app/src/main/java/com/tk/quicksearch/`.

## Checklist

1. **Package:** `tools/<name>/` with a `<Name>Handler.kt` (pure parsing and evaluation, no Android UI) following `tools/calculator/` and `tools/unitConverter/`.
2. **Type:** add a `SearchToolType` constant in `search/core/SearchModels.kt`, plus any result fields in `SearchStateModels.kt` and `SearchStateExtractor.kt`.
3. **Wiring:**
   - Construct the handler in `search/core/SearchHandlerContainer.kt`.
   - Dispatch it in `search/core/SearchToolCoordinator.kt`, for both the explicit-alias path and automatic detection if the tool auto-triggers.
   - Hook the query flow in `search/core/SearchQueryCoordinator.kt` if needed.
4. **Alias:** add a feature alias ID and a `FeatureAliasDefinition` in `searchEngines/AliasHandler.kt`.
5. **Enable/disable preference:**
   - Store it in `search/data/preferences/` (e.g. `UiPreferences.kt`).
   - Expose it via `search/data/userAppPreferences/UserAppPreferences.kt`.
   - Surface it through `search/core/SearchPreferencesDelegate.kt` and `SearchViewModelPreferencesApi.kt`.
   - Read it at startup in `SearchStartupLifecycleDelegate.kt`.
6. **Settings UI:**
   - Add a `ToolSettingDefinition` in `settings/settingsDetailScreen/ToolSettingsRegistry.kt` (with `toggleKey`).
   - Wire it through `settings/shared/SettingsCommands.kt`, `SettingsStateMappers.kt`, and `settings/shared/settingsRoute/` (`SettingsDataModels.kt`, `SettingsCallbacksBuilder.kt`).
7. **Searchable setting row:** use the `new-app-setting` skill (`AppSettingResult.kt`, `AppSettingsRepository.kt`).
8. **Result UI:** a composable for the result card, rendered from `search/searchScreen/searchScreenLayout/ContentLayout.kt`. Reuse `shared/ui/components/` and design tokens (read `DESIGN_SYSTEM.md`).
9. **Strings:** `values/strings.xml` plus all 16 `values-*/strings.xml` files.
10. **Tests:** a unit test for the handler/parser in `app/src/test/.../tools/<name>/`, like `ColorVisualizerParserTest`.
11. **Release notes / features:** mention in `app/src/main/assets/FEATURES.md` only if the user asks.

Finish with `scripts/verify.sh`.
