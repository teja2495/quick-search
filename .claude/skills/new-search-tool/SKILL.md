---
name: new-search-tool
description: Add a built-in inline search tool or instant-answer card that evaluates the current query.
---

# New search tool

For a synchronous built-in tool, implement the evaluator and card in `tools/<name>/`, then register one entry in `tools/inlineSearch/SearchToolType.kt`. Paths are relative to `app/src/main/java/com/tk/quicksearch/`.

1. **Evaluator.** Write a pure function with the signature `(query: String, explicit: Boolean) -> CalculatorState?`. It receives trimmed text and returns `null` when it can't match. Use `CalculatorState(result = text)` for a plain result. For structured output, put a feature-specific data class implementing `InlineToolData` in `toolData`. The common path assigns tool identity and handles empty and invalid input in explicit mode.
2. **Card.** Write a composable taking the result state and the wallpaper background flag. Follow `tools/colorVisualizer/` for a custom card. Reuse the result card components and read `DESIGN_SYSTEM.md` before changing UI.
3. **Register the tool.** Add one `SearchToolType` entry with its settings descriptor, evaluator, renderer, and icons. The entry supplies preference loading and persistence, the Tools page, the searchable Settings toggle, and result rendering. Use stable, unique keys for preference storage and the search row. The enum declaration order determines automatic match precedence, so put broader parsers after stricter ones. The settings rows append by default. Override their order only when the placement matters.
4. **Choose triggers.** Set `aliasFeatureId` to expose a user-configurable alias. Aliases intentionally bypass the automatic tool setting, matching the existing calculator and converters. Set `autoDetect = false` for an alias-only tool. Set `replacesSearchResults = false` to keep normal results alongside the automatic card, as the date calculator does.
5. **Localize and verify.** Add the title, description, and any card copy to all 17 string files using the localization guide. Test the evaluator and relevant matching edge cases, then run `scripts/verify.sh`. Device interaction and UI tests are only run when requested.

A typical addition touches one shared Kotlin file and one or two feature files, plus strings and any test. Tools that use asynchronous providers, a separate loading lifecycle, or their own settings page still need feature-specific wiring. Follow the currency converter or weather tool for an asynchronous example. Edit the release notes or feature list only when asked.
