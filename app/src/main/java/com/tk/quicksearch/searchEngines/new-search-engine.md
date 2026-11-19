# Add a built-in search engine

Append one entry to [SearchEngine.kt](SearchEngine.kt). The entry name is the ID
stored in preferences, aliases, and shortcuts. Existing names and order must stay stable.

```kotlin
EXAMPLE(
    drawableResId = R.drawable.example,
    contentDescriptionResId = R.string.search_engine_example,
    urlTemplate = "https://search.example/?q=%s",
    defaultShortcutCode = "exm",
),
```

- Use `%s` where the encoded query goes. Choose a unique shortcut code, or use an empty string
  when the engine should have no default alias.
- Add the name to `values/strings.xml` and all 16 localized string files with
  [scripts/add_strings.py](../../../../../../../../scripts/add_strings.py). Brand names usually
  stay the same across locales.
- Add a drawable in `res/drawable/`. An install-only engine can omit it to use the installed
  app's icon.

Settings, aliases, icons, and URL construction read from the entry. Existing users get the new
engine before the first browser in their saved order.

## Optional metadata

| Field | When to set it |
|---|---|
| `homeUrl` | An empty query should open a specific landing page |
| `appPackages` | Package candidates are needed for icons, availability, or app lock |
| `installOnly` | Hide the engine when no candidate app is installed |
| `defaultDisabledOnFirstRun` | Start disabled for users encountering the engine for the first time |
| `defaultDisableIfAppMissing` | Start disabled if the app is missing |
| `iconColorPolicy` | Adjust a monochrome icon for light surfaces |
| `nativeLaunch` | The provider needs a dedicated Android launch handler |

For a native launch, implement the handler in
[SearchEngineIntents.kt](../search/core/intentHelpers/SearchEngineIntents.kt)
and set `nativeLaunch = SearchEngineIntents::openExample` on the entry. Follow
`openMuse` or `openKagi` for an app launch, query handoff, and web fallback.

## Validate

Run `scripts/verify.sh`. `SearchEngineContractTest` checks shortcut codes, URL placeholders,
and package and icon requirements for all entries. On a device, check the icon, enable and reorder
behavior, alias, and both query and empty-query launch.
