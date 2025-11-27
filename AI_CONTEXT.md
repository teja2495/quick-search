**Project Context**: Android app (Kotlin + Jetpack Compose, Material 3, MVVM). Package: `com.tk.quicksearch`. Quick launcher that searches **apps, contacts, device files, and web** from a single screen.

**Structure**:
- `MainActivity.kt` - Entry point, edge-to-edge setup, in-app nav between Search and Settings, optional permission flows
- `model/AppInfo.kt` - App data model with `matches()` for search
- `model/ContactInfo.kt`, `model/DeviceFile.kt`, `model/FileType.kt` - Models for contacts and device files, plus file-type categorisation
- `data/AppUsageRepository.kt` - Loads launchable apps, usage stats, and maintains an on-disk cache (`AppCache`)
- `data/ContactRepository.kt` - Contact search (by name), aggregates numbers per contact
- `data/FileSearchRepository.kt` - Device file search via `MediaStore`, respects enabled file types
- `data/UserAppPreferences.kt` - Persists hidden/pinned apps, UI settings, search engine order/disabled engines, file-type filters, preferred contact numbers
- `search/SearchScreen.kt` - Main UI composables (`SearchRoute`, `SearchScreen`), app grid, contacts/files sections, search-engine row, permission banners
- `search/SearchViewModel.kt` - State management (StateFlow), app launching, contact/file actions, permissions, search ranking, search engine URL building
- `settings/SettingsScreen.kt` - Settings screen for app labels toggle, search engine enable/reorder, file-type filters, hidden apps management
- `util/SearchRankingUtils.kt` - Centralised ranking for apps/contacts/files (exact/starts-with/second-word/contains)
- `ui/theme/` - Material 3 theme (Color, Theme, Type)

**Key Details**:
- Grid: 2 rows × 5 columns (10 apps max)
- Apps: **recent apps + pinned apps** when query is empty (pinned first, then recents), filtered search results when typing
- Contacts: inline result card (call/SMS/open contact), with multi-number picker + "remember choice" per contact
- Files: inline result card (opens with appropriate app), filtered by **enabled file types** in settings
- Search ranking: app search uses `AppInfo.matches()` + `SearchRankingUtils` for priority ordering
- Permissions:
  - Mandatory: `PACKAGE_USAGE_STATS` (usage access)
  - Optional: `READ_CONTACTS`, `READ_EXTERNAL_STORAGE` (pre-R), `MANAGE_EXTERNAL_STORAGE`
  - Manifest also declares `QUERY_ALL_PACKAGES` and `REQUEST_DELETE_PACKAGES`
- Search engines (configurable order + enable/disable): Google, ChatGPT, Perplexity, Grok, Google Maps, Google Play, Reddit, YouTube, Amazon
- Auto-focus multi-line search field, keyboard forced visible on start/resume
- Edge-to-edge display with system bar padding and bottom search-engine row behaviour depending on whether app results exist

**Config**: Gradle Kotlin DSL, version catalog in `gradle/libs.versions.toml`. Min SDK 24, Target/Compile SDK 36.


