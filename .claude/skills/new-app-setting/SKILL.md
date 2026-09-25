---
name: new-app-setting
description: Make a Quick Search setting findable from search by adding its searchable app-setting row. Use whenever a change adds or renames a user-facing setting, toggle, or settings page, including as part of a larger feature.
---

# New searchable app-setting row

1. Read and follow `app/src/main/java/com/tk/quicksearch/search/appSettings/new-app-setting.md`. It is the source of truth.
2. Check first whether the row is generated for you: section toggles come from `SearchSectionRegistry`, tool toggles from `ToolSettingsRegistry`.
3. Strings go in `values/strings.xml` and all 16 `values-*/strings.xml` files. Reuse an existing string when the text matches exactly.
4. Run `AppSettingsCatalogContractTest`, then `scripts/verify.sh`.
5. If the guide is wrong or missing a step you needed, update the guide in the same change.
