# New Tool Implementation Guide (AI Agents)

This document is for AI coding agents.
Use it when implementing a new built-in tool in `tools/` that participates in search/direct-search flows.

## Before you start

Read `AGENTS.md` in the repo root for the full architecture playbook (state management, naming conventions, design system, high-risk files). The steps below assume you have read it.

## Agent contract

- Follow existing architecture and naming patterns.
- Keep changes minimal and localized.
- Do not include unrelated refactors.
- Keep heavy work off the main thread.
- Use immutable state/update flows already established in the project.
- Put all user-visible strings in `strings.xml`.

## 1) Choose the feature package

Create a dedicated folder under:
- `app/src/main/java/com/tk/quicksearch/tools/`

Use lowerCamelCase folder names (example: `temperatureConverter`) and keep file names PascalCase.

## 2) Create core tool files

At minimum, add:
- `YourToolHandler.kt`: parses query input and returns tool result(s)
- `YourToolUtils.kt` (optional): shared parsing/format logic if handler grows

Follow existing patterns from:
- `calculator/CalculatorHandler.kt`
- `unitConverter/UnitConverterHandler.kt`
- `dateCalculator/DateCalculatorHandler.kt`

## 3) Define/extend result models

If the tool needs a new result type, add a dedicated model in:
- `app/src/main/java/com/tk/quicksearch/tools/directSearch/`

Keep naming consistent with existing result classes (for example `CalculatorResult`, `DictionaryResult`).

## 4) Wire the tool into orchestration

Integrate handler invocation where tool parsing/execution is orchestrated:
- `app/src/main/java/com/tk/quicksearch/tools/directSearch/DirectSearchHandler.kt`

Rules:
- Keep logic incremental and localized.
- Preserve existing ordering/fallback behavior.
- Avoid heavy work on main thread.

## 5) Update UI rendering if needed

If result UI is unique, add/update rendering in the existing direct-search UI path under:
- `app/src/main/java/com/tk/quicksearch/tools/directSearch/`

Reuse shared design tokens and components where possible.

## 6) Add user-facing strings

Put all new user-facing text in:
- `app/src/main/res/values/strings.xml`

Do not hardcode display strings in Kotlin files.

## 7) Preferences (only if configurable)

If the tool has settings:
1. Add preference in `search/data/preferences/`
2. Expose through `search/data/userAppPreferences/UserAppPreferences.kt`
3. Reflect in state models only if runtime UI needs it
4. Add settings UI in the appropriate settings package

## 8) Validation checklist

Before finalizing:
- Project compiles
- Empty query behavior unchanged
- Tool triggers on valid query input
- Invalid input fails gracefully (no crashes)
- Existing tools still work
- Overlay mode sanity check if shared rendering path changed
- Settings persist (if preferences were added)

## 9) High-risk touchpoints

Edit carefully if required:
- `search/core/SearchViewModel.kt`
- `search/core/SearchModels.kt`
- `search/core/SearchStateModels.kt`
- `search/core/SearchSectionRegistry.kt`

## 10) Done criteria (agent exit conditions)

Only consider the task complete when:
- New tool code is added in the correct `tools/` package.
- Tool is wired into orchestration path(s) and returns expected result type(s).
- Required UI/result rendering is connected.
- Strings are externalized.
- Build/checks relevant to the change pass, or failure reasons are documented.
