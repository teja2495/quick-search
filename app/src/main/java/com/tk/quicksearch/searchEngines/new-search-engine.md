# Adding a Built-in Search Engine

For `SearchEngine` enum-backed engines. User-added custom engines are separate
(`SearchEngineManager.kt`, `CustomSearchEngineUtils.kt`, `settings/searchEngineSettings/dialogs/`);
don't mix the two.

Almost all engine behavior derives from `SearchEngineRegistry`. Prefer registry metadata over
per-engine `if`/`when` branches elsewhere.

## Required

1. **Enum:** add the constant to `enum class SearchEngine` in `search/core/SearchModels.kt`.
2. **Registry:** add a `SearchEngineDefinition(...)` to `searchEngines/SearchEngineRegistry.kt`.
   Its `init` checks fail at startup if an enum value has no entry or has two.
   - `urlTemplate` must contain `%s` for the query.
   - `defaultShortcutCode`: short, unique alias (e.g. `kgi`). It feeds the alias system
     automatically.
   - `contentDescriptionResId`: display name string.
3. **Name string:** `search_engine_<name>` in `values/strings.xml` and all 16 localized
   `values-*/strings.xml` files (brand names stay untranslated).
4. **Icon**, pick one:
   - Add a drawable in `res/drawable/` (lowercase snake_case, e.g. `kagi.xml`) and set
     `drawableResId`.
   - Or, for app-only engines, omit `drawableResId`, set `appPackages` and `installOnly = true`.
     The installed app's icon is used.

Existing users get the new engine automatically: `SearchEngineManager.mergeMissingEngines`
appends missing engines before any browser targets.

## Optional registry fields

| Field | Use when |
|---|---|
| `homeUrl` | Empty query should open a page other than the template with no query |
| `appPackages` | Engine has an Android app. Add the package constant to `shared/util/PackageConstants.kt`. The app has `QUERY_ALL_PACKAGES`, so no manifest `<queries>` entry is needed. |
| `installOnly` | Engine is only offered when its app is installed |
| `defaultDisabledOnFirstRun` | Engine should start disabled for new users |
| `defaultDisableIfAppMissing` | Engine should start disabled when its app isn't installed |
| `iconColorPolicy` | Monochrome drawable needs `INVERT_ON_LIGHT` or `DARKEN_ON_LIGHT` |
| `nativeLaunchMode` | Query should open inside the engine's app instead of a browser (see below) |

## Native app launch (only if needed)

1. Add a value to `SearchEngineNativeLaunchMode` in `SearchEngineRegistry.kt` and set it on the
   definition.
2. In `search/core/intentHelpers/SearchEngineIntents.kt`, map the new mode to an `open<Engine>`
   function and implement it. Follow `openMuse`/`openKagi`: launch the app for an empty query,
   send the query via its intent, and fall back to `openWebUrl(buildSearchUrl(...))` when the app
   is missing or the intent fails.

Special URL handling (like Amazon's custom domain in `SearchEngineUtils.buildSearchUrl`) is a
last resort. Dedicated settings UI goes in `settings/searchEngineSettings/`.

## Validate

- `./gradlew :app:compileStandardDebugKotlin`
- Ask the user to check on device: the engine appears in search engine settings; enable, disable,
  and reorder persist; the alias works; query and empty-query launches open the right target;
  compact and inline engine UIs render the icon in light and dark.

## Common mistakes

- Enum added without a registry entry (crashes at startup), or a registry entry without its
  string or drawable.
- `urlTemplate` missing `%s`.
- Duplicate `defaultShortcutCode`.
- Name string added only to `values/strings.xml`.
