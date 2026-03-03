# Quick Search - AI Agent Context

## Project Overview

**Type**: Android Application (Quick Search Launcher)  
**Language**: Kotlin  
**UI Framework**: Jetpack Compose with Material 3  
**Architecture**: MVVM (Model-View-ViewModel)  
**Package**: `com.tk.quicksearch`  
**Purpose**: Unified search launcher that searches apps, contacts, files, device settings, web, and provides calculator functionality from a single screen

---

## 🏗️ Architecture & Design Patterns

### MVVM Architecture

The app follows the **Model-View-ViewModel (MVVM)** pattern with clear separation of concerns:

- **Model Layer** (`search/models/`, `search/data/`): Data classes, repositories, and data sources
- **ViewModel Layer** (`search/core/SearchViewModel.kt`): Business logic, state management using StateFlow
- **View Layer** (`search/searchScreen/`, `search/overlay/`, UI composables): Jetpack Compose UI components

### State Management

**Primary Pattern**: Unidirectional Data Flow with StateFlow

```kotlin
// Core UI State (search/core/SearchModels.kt)
data class SearchUiState(
    val query: String = "",
    val hasUsagePermission: Boolean = false,
    val hasContactPermission: Boolean = false,
    val hasFilePermission: Boolean = false,
    
    // Visibility states using sealed classes
    val screenState: ScreenVisibilityState = ScreenVisibilityState.Initializing,
    val appsSectionState: AppsSectionVisibility = AppsSectionVisibility.Hidden,
    val appShortcutsSectionState: AppShortcutsSectionVisibility = ...,
    val contactsSectionState: ContactsSectionVisibility = ContactsSectionVisibility.Hidden,
    val filesSectionState: FilesSectionVisibility = FilesSectionVisibility.Hidden,
    val settingsSectionState: SettingsSectionVisibility = SettingsSectionVisibility.Hidden,
    val searchEnginesState: SearchEnginesVisibility = SearchEnginesVisibility.Hidden,
    
    // Data
    val recentApps: List<AppInfo> = emptyList(),
    val searchResults: List<AppInfo> = emptyList(),
    val appShortcutResults: List<StaticShortcut> = emptyList(),
    val contactResults: List<ContactInfo> = emptyList(),
    val fileResults: List<DeviceFile> = emptyList(),
    val recentItems: List<RecentSearchItem> = emptyList(),
    val overlayModeEnabled: Boolean = false,
    val oneHandedMode: Boolean = false,
    val showWallpaperBackground: Boolean = true,
    val selectedIconPackPackage: String? = null,
    val DirectSearchState: DirectSearchState = ...,
    // ... more state fields
)
```

**Key Patterns**:
- **Sealed Classes for State**: Used for visibility and screen states to ensure type-safe state transitions
- **StateFlow**: Single source of truth managed by `SearchViewModel`
- **State Updates**: Centralized through `updateUiState()` method in ViewModel
- **Immutable State**: All updates create new state copies

**SearchSection enum** (`SearchModels.kt`): `APPS`, `APP_SHORTCUTS`, `CONTACTS`, `FILES`, `SETTINGS` — used for section ordering and disabling. **SearchTarget** (Engine vs Browser): used for search engine/browser targets and shortcuts.

### Data Layer Architecture

**Repository Pattern** with specialized repositories:

1. **AppsRepository** (`search/data/AppsRepository.kt`)
   - Loads launchable apps and usage statistics
   - Manages app cache for fast startup

2. **ContactRepository** (`search/data/ContactRepository.kt`)
   - Contact search by name
   - Aggregates phone numbers per contact
   - Supports preferred numbers and exclusions

3. **FileSearchRepository** (`search/data/FileSearchRepository.kt`)
   - Device file search via MediaStore API
   - Respects enabled file type filters

4. **AppShortcutRepository** (`search/data/AppShortcutRepository.kt`)
   - Loads app shortcuts (StaticShortcut) for search and pinning

5. **UserAppPreferences** (`search/data/UserAppPreferences.kt`)
   - Centralized preference management
   - Delegates to specialized preference classes in `search/data/preferences/`

**Preference Management Pattern**:
```kotlin
// Modular preferences with base class (search/data/preferences/)
- BasePreferences.kt - Base utilities and common operations
- AppPreferences.kt, AppShortcutPreferences.kt - App and shortcut preferences
- ContactPreferences.kt, FilePreferences.kt - Contact and file preferences
- UiPreferences.kt - UI settings (wallpaper, one-handed, overlay mode)
- SearchEnginePreferences.kt, ShortcutPreferences.kt - Search engine and shortcuts
- GeminiPreferences.kt - Direct Search / Gemini API
- NicknamePreferences.kt, SettingsPreferences.kt, AmazonPreferences.kt
// SearchHistoryPreferences.kt lives in search/searchHistory/
```

**Caching Strategy**:
- **AppCache** (`search/data/AppCache.kt`): SharedPreferences-based JSON serialization for instant app list loading
- **Startup Performance**: Three-phase initialization (critical prefs → cached data → deferred handlers)

---

## 📁 Project Structure

```
app/src/main/java/com/tk/quicksearch/

│
├── app/                          # Application entry point and app-level handlers
│   ├── MainActivity.kt           # Main activity, navigation setup, edge-to-edge
│   ├── ReleaseNotesHandler.kt
│   ├── ReviewHelper.kt          # Play Review API
│   ├── ReviewPromptDialogs.kt
│   └── UpdateHelper.kt           # Play App Update API
│
├── navigation/                   # Navigation management
│   ├── NavigationHandler.kt
│   └── NavigationManager.kt     # Centralized navigation with animated transitions
│
├── shared/                       # Shared components and utilities (new centralized package)
│   ├── ui/                       # Shared UI components and theming
│   │   ├── components/           # Reusable UI components (SectionHeader, AppCard, etc.)
│   │   └── theme/                # Material 3 theming (moved from ui/theme/)
│   │       ├── Theme.kt
│   │       ├── Color.kt
│   │       ├── AppColors.kt      # Centralized color definitions
│   │       ├── DesignTokens.kt   # Design system tokens
│   │       └── Type.kt
│   └── util/                     # Shared utility functions (moved from util/)
│       ├── DeviceUtils.kt        # Device detection and responsive layout utilities
│       ├── WallpaperUtils.kt
│       ├── HapticUtils.kt
│       ├── AssistantUtils.kt
│       ├── FeedbackUtils.kt
│       ├── InAppBrowserUtils.kt
│       └── PackageConstants.kt
│
├── search/                       # Main search functionality
│   ├── models/                   # Data models
│   │   ├── AppInfo.kt           # App model with search matching
│   │   ├── ContactInfo.kt       # Contact data model
│   │   ├── ContactMethod.kt     # Contact method (phone, email)
│   │   ├── DeviceFile.kt        # File data model
│   │   └── FileType.kt          # Enum for file types
│   │
│   ├── data/                     # Data layer
│   │   ├── AppsRepository.kt
│   │   ├── AppCache.kt
│   │   ├── AppShortcutRepository.kt
│   │   ├── ContactRepository.kt
│   │   ├── FileSearchRepository.kt
│   │   ├── UserAppPreferences.kt # Central preferences manager
│   │   └── preferences/          # Modular preference classes
│   │       ├── BasePreferences.kt
│   │       ├── AppPreferences.kt
│   │       ├── AppShortcutPreferences.kt
│   │       ├── ContactPreferences.kt
│   │       ├── FilePreferences.kt
│   │       ├── UiPreferences.kt
│   │       ├── SearchEnginePreferences.kt
│   │       ├── GeminiPreferences.kt
│   │       ├── NicknamePreferences.kt
│   │       ├── SettingsPreferences.kt
│   │       ├── ShortcutPreferences.kt
│   │       ├── AmazonPreferences.kt
│   │       └── ... (more preference modules)
│   │
│   ├── core/                     # Core search logic
│   │   ├── SearchViewModel.kt   # Main ViewModel (2946 lines)
│   │   ├── SearchModels.kt      # State and model definitions
│   │   ├── SearchViewModelPermissionManager.kt
│   │   ├── SearchViewModelSearchOperations.kt
│   │   ├── UnifiedSearchHandler.kt
│   │   ├── SectionManager.kt
│   │   ├── SectionRenderingHelpers.kt
│   │   ├── ItemPriorityConfig.kt
│   │   ├── ManagementHandler.kt
│   │   ├── UrlUtils.kt
│   │   └── intentHelpers/       # Intent handling utilities
│   │       ├── IntentHelpers.kt
│   │       ├── IntentUtils.kt
│   │       ├── SearchIntents.kt
│   │       ├── AppLaunchingIntents.kt
│   │       ├── AppManagementIntents.kt
│   │       ├── AppSettingsIntents.kt
│   │       ├── SearchEngineIntents.kt
│   │       └── FileIntents.kt
│   │
│   ├── apps/                     # App search features
│   │   ├── AppSearchManager.kt  # Search logic and filtering
│   │   ├── AppGridView.kt       # Grid rendering
│   │   ├── AppIconManager.kt    # Async icon loading
│   │   ├── AppManagementService.kt
│   │   ├── AppItemMenuView.kt
│   │   ├── FuzzyAppSearchStrategy.kt
│   │   ├── IconPackManager.kt
│   │   └── IconPackService.kt
│   │
│   ├── appShortcuts/             # App shortcut search and actions
│   │   ├── AppShortcutSearchHandler.kt
│   │   ├── AppShortcutResultsSection.kt
│   │   └── AppShortcutManagementHandler.kt
│   │
│   ├── contacts/                 # Contact search and actions
│   │   ├── ContactResultsSection.kt
│   │   ├── ContactExpandCollapse.kt
│   │   ├── actions/             # Contact action handlers
│   │   │   ├── ContactActionHandler.kt
│   │   │   ├── CallSmsActions.kt
│   │   │   ├── WhatsAppActions.kt
│   │   │   ├── TelegramActions.kt
│   │   │   ├── SignalActions.kt
│   │   │   ├── GoogleMeetActions.kt
│   │   │   ├── CustomAppActions.kt
│   │   │   └── ContactDataIntentLauncher.kt
│   │   ├── components/          # Contact UI components
│   │   │   ├── ContactResultRow.kt
│   │   │   ├── ContactMenuView.kt
│   │   │   ├── ContactActionComponents.kt
│   │   │   └── ContactUiConstants.kt
│   │   ├── dialogs/             # Contact selection dialogs
│   │   │   ├── ContactActionPickerDialog.kt
│   │   │   ├── ContactMethodsDialog.kt
│   │   │   ├── PhoneSelectionDialogs.kt
│   │   │   └── ContactDialogUtils.kt
│   │   ├── models/              # Contact data models
│   │   │   └── ContactCardAction.kt
│   │   └── utils/               # Contact utilities
│   │       ├── ContactManagementHandler.kt
│   │       ├── ContactIntentHelpers.kt
│   │       ├── ContactCallingAppResolver.kt
│   │       ├── ContactMessagingAppResolver.kt
│   │       ├── MessagingHandler.kt
│   │       └── TelegramContactUtils.kt
│   │
│   ├── files/                    # File search
│   │   ├── FileResultsSection.kt
│   │   ├── FileManagementHandler.kt
│   │   ├── FileSearchHandler.kt
│   │   └── FileMenuView.kt
│   │
│   ├── deviceSettings/           # Device settings search
│   │   ├── DeviceSettingsSearchHandler.kt
│   │   ├── DeviceSettingsRepository.kt
│   │   ├── DeviceSettingsManagementHandler.kt
│   │   ├── DeviceSettingsMenuView.kt
│   │   ├── DeviceSetting.kt
│   │   └── DeviceSettingsResults.kt
│   │
│   ├── searchScreen/             # Main search UI
│   │   ├── SearchScreen.kt      # Main screen composable
│   │   ├── searchScreen/        # Search screen routing and state management
│   │   ├── searchScreenLayout/  # Layout orchestration (overlay theme applied here)
│   │   │   ├── ContentLayout.kt
│   │   │   ├── SearchContentArea.kt
│   │   │   └── ... (layout components)
│   │   ├── components/          # UI components (Cards, Pills, Banners)
│   │   ├── OverlayGradientThemeUtils.kt  # Overlay theme gradients (Forest, Aurora, Sunset, Mono)
│   │   ├── SearchScreenContent.kt
│   │   ├── SearchScreenComponents.kt
│   │   ├── SearchScreenSectionRendering.kt
│   │   ├── SearchScreenHelpers.kt
│   │   ├── SearchScreenBackground.kt
│   │   ├── SearchScreenScroll.kt
│   │   ├── SearchScreenConstants.kt
│   │   ├── SearchEngineOnboardingOverlay.kt
│   │   ├── SectionRenderingComposables.kt
│   │   ├── ComposeVisibilityExtensions.kt
│   │   └── dialogs/
│   │
│   ├── overlay/                  # Overlay mode (search over other apps)
│   │   ├── OverlayRoot.kt       # Overlay UI root composable (uses overlay theme)
│   │   ├── OverlayActivity.kt   # Activity for overlay window
│   │   └── OverlayModeController.kt
│   │
│   ├── webSuggestions/           # Web search suggestions
│   │   ├── WebSuggestionHandler.kt
│   │   ├── WebSuggestionsSection.kt
│   │   └── WebSuggestionsUtils.kt
│   │
│   ├── fuzzy/                    # Fuzzy search engine
│   │   ├── FuzzySearchEngine.kt
│   │   ├── FuzzySearchStrategy.kt
│   │   ├── FuzzySearchConfig.kt
│   │   └── FuzzySearchConfigurationManager.kt
│   │
│   ├── searchHistory/           # Recent items tracking and display
│   │   ├── SearchHistorySection.kt
│   │   ├── SearchHistoryModels.kt
│   │   └── SearchHistoryPreferences.kt
│   │
│   └── common/                   # Shared utilities and handlers
│       ├── PinningHandler.kt
│       ├── SearchRankingUtils.kt
│       ├── PhoneNumberUtils.kt
│       ├── PermissionUtils.kt
│       └── FileUtils.kt
│
├── searchEngines/                # Search engine integration (moved from search/searchEngines/)
│   ├── SearchEngineManager.kt
│   ├── SearchEngineLayout.kt
│   ├── SearchTargetUtils.kt
│   ├── ShortcutHandler.kt
│   ├── ShortcutValidator.kt
│   ├── BrowserTargets.kt
│   ├── CustomSearchEngineUtils.kt
│   ├── SecondarySearchOrchestrator.kt
│   ├── compact/                  # Compact mode UI
│   ├── inline/                   # Inline mode (SearchEngineSection, etc.)
│   └── shared/                   # Shared search engine components
│
├── tools/                        # Specialized tools and utilities
│   ├── calculator/               # Calculator functionality
│   │   ├── CalculatorHandler.kt
│   │   └── CalculatorUtils.kt
│   │
│   └── directSearch/             # Direct Search (Gemini API)
│       ├── DirectSearchClient.kt
│       ├── DirectSearchHandler.kt
│       ├── GeminiLoadingAnimation.kt
│       ├── GeminiModelPickerDialog.kt  # Model selection (Gemini/Gemma)
│       ├── GeminiModels.kt        # Gemini/Gemma model definitions
│       └── SearchScreenDirectResults.kt  # Results UI; "Powered by" tap = model picker, long press = configure
│
├── settings/                     # Settings screens (restructured)
│   ├── SettingsScreen.kt
│   ├── SettingsBackupManager.kt
│   ├── appearanceSettings/       # Appearance settings (new subdirectory)
│   │   ├── AppearanceSettings.kt     # Overlay theme card (OverlayThemeCard)
│   │   ├── OverlayThemeSettings.kt
│   │   ├── IconPackSettings.kt
│   │   ├── LayoutIconSettings.kt
│   │   └── FontSizeSettings.kt
│   ├── appShortcutsSettings/     # App shortcuts settings (new subdirectory)
│   │   ├── AppShortcutsSettings.kt
│   │   ├── Components.kt
│   │   └── Models.kt
│   ├── searchEngineSettings/     # Search engine settings (new subdirectory)
│   ├── navigation/               # Settings navigation (new subdirectory)
│   │   ├── SettingsDetailScreen.kt
│   │   ├── SettingsDetailRoute.kt
│   │   └── SettingsDetailLevel2Screen.kt
│   ├── shared/                   # Shared settings components (expanded)
│   │   ├── SettingsRoute.kt
│   │   ├── SettingsNavigationRow.kt
│   │   ├── SettingsToggleRow.kt
│   │   ├── SettingsStateMappers.kt
│   │   ├── SettingsLayoutUtils.kt
│   │   └── settingsRoute/
│   ├── AppManagementSettings.kt
│   ├── CallsAndTextsSettings.kt
│   ├── ExcludedItemSettings.kt
│   ├── FileSettings.kt
│   └── SearchResultsSettings.kt
│
├── onboarding/                   # First-launch setup
│   ├── SearchEngineSetupScreen.kt
│   ├── FinalSetupScreen.kt
│   ├── OnboardingHeader.kt
│   └── permissionScreen/        # PermissionsScreen, PermissionRequestHandler, etc.
│
├── widgets/                      # Home screen widgets (restructured from widget/)
│   ├── customButtonsWidget/     # Custom buttons widget
│   ├── searchWidget/            # Search widget
│   ├── widgetConfigScreen/      # Widget configuration UI
│   └── utils/                   # Widget utilities
│
├── tile/                         # Quick Settings tile
│   ├── QuickSearchTileService.kt
│   └── QuickSettingsTileUtils.kt
│
└── util/                         # Legacy utility functions (most moved to shared/util/)
```

---

## 🔄 Data Flow & State Update Pattern

### Search Query Flow

1. **User Input** → `SearchScreen.kt` / `SearchRoute` (or `OverlayRoot.kt` in overlay mode)
2. **ViewModel Update** → `SearchViewModel.onQueryChanged()`
3. **State Calculation** → Multiple handlers compute results:
   - App search: `AppSearchManager`
   - App shortcuts: `AppShortcutSearchHandler`
   - Contact search: `ContactRepository`
   - File search: `FileSearchRepository`
   - Web suggestions: `WebSuggestionHandler` (`search/webSuggestions/`)
   - Calculator: `CalculatorHandler`
4. **State Emission** → Updated `SearchUiState` via StateFlow
5. **UI Recomposition** → Composables observe state and recompose

### Typical State Update Pattern

```kotlin
// In SearchViewModel
fun updateQuery(newQuery: String) {
    updateUiState { currentState ->
        currentState.copy(query = newQuery)
    }
    // Trigger search operations
    performSearch(newQuery)
}

private fun updateUiState(updater: (SearchUiState) -> SearchUiState) {
    _uiState.update(updater)
}
```

---

## 🎨 UI Design System

### Color System (`shared/ui/theme/AppColors.kt`)

**Theme Colors**:
- Primary: Deep Purple (`#651FFF`)
- Secondary: Neon Purple (`#D500F9`)
- Tertiary: Indigo (`#5E35B1`)

**Dynamic Theming**:
- Wallpaper mode: Semi-transparent overlays (`OverlayMedium` = Black @ 40% alpha in `AppColors.kt`)
- Standard mode: Material 3 surface containers
- Theme colors: `ThemeDeepPurple`, `ThemeNeonPurple`, `ThemeIndigo`, `ThemePurple` (quaternary)

**Card Styling Pattern**:
```kotlin
@Composable
fun getCardColors(showWallpaperBackground: Boolean): CardColors {
    return if (showWallpaperBackground) {
        CardDefaults.cardColors(containerColor = OverlayMedium)
    } else {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }
}
```

### Design Tokens (`shared/ui/theme/DesignTokens.kt`)

Centralized spacing (4dp grid) and dimensions:
- Spacing scale: `SpacingXXSmall` (4.dp) through `Spacing48` (48.dp); `ContentHorizontalPadding = SpacingXLarge` (20.dp)
- `CardShape = ShapeMedium` (12.dp rounded)
- `ExtraLargeCardShape = ShapeXLarge` (28.dp rounded)
- Card padding: `singleCardPadding()`, `CardHorizontalPadding`, `CardTopPadding`, `CardBottomPadding`
- `IconSize`, `LargeIconSize`; elevation levels; semantic colors (ColorPhone, ColorSms, etc.)

### Composable Structure Pattern

```kotlin
// Typical section composable structure
@Composable
fun ResultsSection(
    results: List<Result>,
    onItemClick: (Result) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = AppColors.getCardColors(showWallpaper),
        shape = DesignTokens.CardShape
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.singleCardPadding())
        ) {
            // Section content
        }
    }
}
```

### Shared UI Components (`shared/ui/components/`)

Centralized reusable UI components extracted from feature-specific code:

**Common Components**:
- `SectionHeader.kt` - Consistent section headers with optional action buttons
- `EmptyStateMessage.kt` - Empty state displays with icons and messages
- `IconWithText.kt` - Icon-text combinations with consistent spacing
- `TipBanner.kt` - Informational banners for user guidance
- `AppCard.kt` - Standardized app display cards
- `LoadingIndicator.kt` - Consistent loading states
- `SectionDivider.kt` - Visual separators between sections

**Usage Pattern**:
```kotlin
// Import shared components
import com.tk.quicksearch.shared.ui.components.SectionHeader
import com.tk.quicksearch.shared.ui.components.EmptyStateMessage

@Composable
fun MySection() {
    SectionHeader(
        title = "Recent Apps",
        actionText = "See All",
        onActionClick = { /* navigate */ }
    )

    if (apps.isEmpty()) {
        EmptyStateMessage(
            icon = Icons.Default.Apps,
            title = "No recent apps",
            message = "Start using apps to see them here"
        )
    }
}
```

### Responsive Design & Tablet Optimizations

**Device Detection** (`shared/util/DeviceUtils.kt`):
- **Tablet Detection**: Devices with `smallestScreenWidthDp >= 600` are considered tablets
- **Orientation Awareness**: Separate logic for portrait vs landscape modes

**Dynamic Layouts**:
- **App Grid Columns**:
  - Phones: 5 columns (fixed)
  - Tablets: 7 columns portrait, 9 columns landscape
- **Search Engine Icons**:
  - Phones: 6 per row
  - Tablets: 8 per row portrait, 10 per row landscape

**Adaptive UI Patterns**:
```kotlin
// Responsive column calculation
@Composable
fun getAppGridColumns(): Int {
    return if (isTablet()) {
        if (isLandscape()) 9 else 7
    } else {
        5
    }
}
```

---

## 🔍 Search Algorithm & Ranking

### Multi-Source Ranking (`search/common/SearchRankingUtils.kt`)

**Ranking Priorities** (highest to lowest):
1. **Exact match** - Query exactly matches name
2. **Starts with** - Name starts with query
3. **Second word starts with** - Second word in name starts with query
4. **Contains** - Name contains query anywhere

**Applied To**:
- Apps (by name and nicknames)
- Contacts (by name)
- Files (by filename)
- Settings (by title)

### Fuzzy Search Engine (`search/fuzzy/FuzzySearchEngine.kt`)

**Advanced Fuzzy Matching** for enhanced search accuracy:

```kotlin
// FuzzySearchEngine.kt
class FuzzySearchEngine {
    fun computeScore(query: String, targetText: String, targetNickname: String?): Int {
        // Returns score 0-100 based on fuzzy matching algorithms
    }
}
```

**Key Features**:
- **Typo Tolerance**: Handles common typos and spelling variations (e.g., "chrmoe" → "Chrome")
- **Acronym Matching**: Short queries match app acronyms (e.g., "yt" → "YouTube", "gm" → "Google Maps")
- **Token-Based Scoring**: Uses FuzzyWuzzy library with token set ratio for accurate matching
- **Nickname Support**: Considers app nicknames for enhanced matching
- **Minimum Query Length**: Only applies fuzzy matching for queries ≥ 3 characters (or 2-4 for acronyms)

**Scoring Algorithm**:
- **100**: Exact acronym match or perfect fuzzy match
- **Variable (0-100)**: Token set ratio using FuzzyWuzzy algorithms
- **0**: Below minimum threshold or no match

### App Search Logic

```kotlin
// AppInfo.kt
fun matches(query: String): Boolean {
    val lowerQuery = query.lowercase()
    return label.lowercase().contains(lowerQuery) ||
           packageName.lowercase().contains(lowerQuery) ||
           nickname?.lowercase()?.contains(lowerQuery) == true
}
```

**App Display Logic**:
- Empty query: Show **pinned apps** (first) + **recent apps**
- With query: Show **filtered search results** using hybrid ranking:
  - Primary: `SearchRankingUtils` for exact/starts-with/contains matching
  - Secondary: `FuzzySearchEngine` for typo tolerance and acronym matching
- Grid: 2 rows × 5 columns = **10 apps maximum**

**Search Enhancement**:
- **Traditional Ranking**: Exact match → starts with → contains (via `SearchRankingUtils`)
- **Fuzzy Enhancement**: Typo correction and acronym matching (via `FuzzySearchEngine`)
- **Combined Results**: Best matches from both algorithms presented together

---

## 🔐 Permissions Architecture

### Permission Types

**Required** (app cannot function without):
- `PACKAGE_USAGE_STATS` - For app usage statistics and recent apps

**Optional** (graceful degradation):
- `READ_CONTACTS` - Contact search functionality
- `READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE` - File search
- `CALL_PHONE` - Direct dial feature
- `QUERY_ALL_PACKAGES` - List all installed apps
- Wallpaper/display access - For wallpaper background in search UI (reflected in `hasWallpaperPermission`, `wallpaperAvailable`)

### Permission Handling Pattern

1. **Check** → `SearchViewModel` checks permissions
2. **Request** → `PermissionRequestHandler` manages permission requests
3. **State Update** → Permission state reflected in `SearchUiState`
4. **UI Adaptation** → Sections hidden/shown based on permissions

---

## 📱 Key Features & Implementation

### 1. Search Engines

**Supported Engines** (25 total):
Direct Search (dsh), Google (ggl), ChatGPT (cgpt), Gemini (gmi), Perplexity (ppx), Grok (grk), Reddit (rdt), Google Maps (mps), Waze (wze), Google Drive (gdr), Google Photos (gph), Google Play (gpl), YouTube (ytb), YouTube Music (ytm), Spotify (sfy), Amazon (amz), X/Twitter (twt), Facebook Marketplace (fbm), Bing (bng), DuckDuckGo (ddg), Brave (brv), Startpage (stp), You.com (yu), AI Mode (gai), Claude (cld)

**Features**:
- **Shortcuts**: Custom keyboard shortcuts (e.g., "ggl" for Google)
- **Reordering**: User-customizable order
- **Enable/Disable**: Toggle individual engines
- **Custom Search Engines**: Users can add their own search engines from settings
- **Direct Search**: Gemini API integration; model selection (several Gemini and Gemma models via `GeminiModelPickerDialog.kt`); optional personal context. "Powered by" attribution row: tap opens model picker, long press opens Direct Search configuration.
- **Display Modes**: Inline (scrolls) vs Compact (fixed at bottom)

**Implementation**: `searchEngines/SearchEngineManager.kt`. Display modes: `searchEngines/inline/` (SearchEngineSection) and `searchEngines/compact/`. Direct Search in `tools/directSearch/` (DirectSearchClient, DirectSearchHandler, SearchScreenDirectResults, GeminiModelPickerDialog). Model preference: `GeminiPreferences.kt`.

### 2. Contact Integration

**Messaging Support**:
- Default SMS/Messages
- WhatsApp (chat, audio, video)
- Telegram (chat, audio, video)
- Signal
- Google Meet

**Features**:
- Multi-number support with number picker
- Preferred number "remember choice"
- Direct dial (call without dialer)
- Number normalization via `PhoneNumberUtils`

**Implementation**: `search/contacts/` (ContactResultsSection, actions/, components/, utils/)

### 3. Calculator

**Capabilities**:
- Basic operations: `+`, `-`, `*`, `/`
- Brackets for precedence
- Real-time evaluation in search bar
- Long-press calculator results to copy to clipboard

**Implementation**: `tools/calculator/CalculatorHandler.kt`, `tools/calculator/CalculatorUtils.kt`

### 4. Web Suggestions

- Google-powered suggestions via Google Suggest API
- Configurable suggestion count (default: 3)

**Implementation**: `search/webSuggestions/WebSuggestionHandler.kt`, `WebSuggestionsSection.kt`

### 5. Recent Searches

**Enhanced Recent Items Tracking**:
- Shows recently accessed files, contacts, device settings, and app shortcuts when search bar is empty
- Supports up to 10 recent items with configurable display count (default: 3)
- Items are automatically tracked when accessed through search results
- Individual items can be removed from recent list via long-press context menu

**Supported Item Types**:
- **RecentSearchItem.Query**: Text search queries
- **RecentSearchItem.Contact**: Recently accessed contacts with full contact data
- **RecentSearchItem.File**: Recently accessed device files
- **RecentSearchItem.Setting**: Recently accessed device settings
- **RecentSearchItem.AppShortcut**: Recently accessed app shortcuts

**Data Models**:
- `RecentSearchEntry` (sealed class): Storage format for different item types
- `RecentSearchItem` (sealed class): UI presentation format with resolved data

**Implementation**: `search/searchHistory/SearchHistorySection.kt`, `SearchHistoryModels.kt`, `SearchHistoryPreferences.kt`

### 6. Overlay Mode

**Behavior**: Search bar appears over other apps so users can search from any screen without leaving the current app.

**Overlay Themes**: Multiple gradient themes (Forest, Aurora, Sunset, Mono) with adjustable intensity (lighter/darker). Configured in Settings → Appearance (Search Results → Appearance). State: `overlayGradientTheme`, `overlayThemeIntensity` in `SearchUiState`; persisted in `UiPreferences`.

**Font Size Customization**: Adjustable font scaling throughout the app (95%, 100%, 105%). Provides better readability for different users and devices. Configured in Settings → Appearance.

**Key Files**:
- `search/overlay/OverlayRoot.kt` - Overlay UI root composable (entry animation, close tip, SearchRoute; uses overlay theme)
- `search/overlay/OverlayActivity.kt` - Activity that hosts the overlay window
- `search/overlay/OverlayModeController.kt` - Overlay mode state/control
- `search/searchScreen/OverlayGradientThemeUtils.kt` - Theme gradient colors and tone adjustment
- `search/core/SearchModels.kt` - `OverlayGradientTheme` enum
- `settings/settingsDetailScreen/AppearanceSettings.kt` - OverlayThemeCard UI

**State**: `SearchUiState.overlayModeEnabled`, `overlayGradientTheme`, `overlayThemeIntensity`, `showOverlayCloseTip`. Preferences in `UiPreferences`.

### 7. App Shortcuts

**Behavior**: Long-press any app to access its shortcuts; enable shortcuts in search results. Pinnable and excludable like apps.

**Shortcut Management**: Enable, disable, or add custom app shortcuts via Settings → Search Results → App Shortcuts. UI: `settings/settingsDetailScreen/AppShortcutsSettings.kt` (AppShortcutsSettingsSection).

**Key Files**: `search/appShortcuts/AppShortcutSearchHandler.kt`, `AppShortcutResultsSection.kt`, `AppShortcutManagementHandler.kt`. Settings: `AppShortcutsSettings.kt`. Data: `StaticShortcut`, `AppShortcutRepository`, `AppShortcutPreferences`.

### 8. App Management

**Behavior**: View app details or bulk uninstall apps. Access via Settings → Search Results → App Management. Long-press on app results still opens app info/uninstall from context menu.

**Key Files**: `settings/settingsDetailScreen/AppManagementSettings.kt` (AppManagementSettingsSection), `settings/settingsDetailScreen/SearchResultsSettings.kt` (navigates to APP_MANAGEMENT), `settings/settingsDetailScreen/SettingsDetailLevel2Screen.kt` (hosts AppManagementSettingsSection). Logic: `search/apps/AppManagementService.kt`. Detail type: `SettingsDetailType.APP_MANAGEMENT`.

### 9. Pinning System

**Pinnable Items**:
- Apps
- App Shortcuts
- Contacts
- Files
- Device Settings

**Storage**: Stored in `UserAppPreferences` (package names, shortcut IDs, contact IDs, file paths, setting IDs)

**UI Pattern**: Long-press → Context menu → Pin/Unpin option

### 10. File Search

**Supported Types**:
- Photos & Videos
- Documents  
- Other files

**Features**:
- Filter by file type
- Exclude specific extensions
- MediaStore integration

---

## 🚀 Performance Optimizations

### Startup Performance Strategy

**Three-Phase Initialization**:

```kotlin
// Phase 1: Critical Preferences (minimal, layout-only)
loadCacheAndMinimalPrefs()

// Phase 2: Remaining Startup Preferences
loadRemainingStartupPreferences()

// Phase 3: Deferred Initialization (background handlers)
launchDeferredInitialization()
```

**App Cache**:
- JSON serialization to SharedPreferences
- Instant loading on subsequent launches
- Background refresh after initial display

### Icon Loading

**Async Icon Manager** (`search/apps/AppIconManager.kt`):
- Lazy loading with caching
- Prefetch icons for visible apps
- Icon pack support

### Secondary Search Orchestration (`searchEngines/SecondarySearchOrchestrator.kt`)

**Debounced Secondary Searches**:
- Prevents redundant searches for queries that previously yielded no results
- 150ms debounce delay for secondary search operations (contacts, files, settings, app shortcuts)
- Tracks "no-results" states per query prefix to avoid repeated searches

**Query State Tracking**:
- Maintains query version control to cancel outdated operations
- Intelligent result caching to skip searches for known empty result sets
- Optimized for rapid typing scenarios

### Baseline Profile Optimization (`app/src/main/baseline-prof.txt`)

**Ahead-of-Time (AOT) Compilation**:
- Defines critical methods compiled ahead-of-time for improved startup performance
- Reduces app launch time and eliminates jank during initial interactions
- Covers essential startup paths, data loading, and UI rendering

**Profiled Classes**:
- **Core Startup**: `MainActivity`, `SearchViewModel`, repositories, and managers
- **Data Loading**: `AppsRepository`, `ContactRepository`, `FileSearchRepository`, `AppShortcutRepository`
- **Search Infrastructure**: `SearchEngineManager`, `SectionManager`, `PermissionManager`
- **UI Framework**: Compose runtime classes and essential collections
- **Utilities**: `WallpaperUtils`, `PermissionUtils`, `SearchRankingUtils`, JSON parsing

---

## 🎯 Navigation & Routing

### Navigation Structure

**Root Destinations** (`navigation/NavigationManager.kt`):
- `RootDestination.Search` - Main search screen
- `RootDestination.Settings` - Settings navigation

**App Screens** (onboarding): `AppScreen.Permissions`, `SearchEngineSetup`, `FinalSetup`, `Main`

**Settings Detail Types** (`SettingsDetailType` in `settings/settingsDetailScreen/`):
- `SEARCH_ENGINES`, `EXCLUDED_ITEMS`, `SEARCH_RESULTS`, `APPEARANCE`, `CALLS_TEXTS`, `FILES`, `LAUNCH_OPTIONS`, `PERMISSIONS`, `FEEDBACK_DEVELOPMENT`
- From Search Results: `APP_MANAGEMENT` (view app details, bulk uninstall), App Shortcuts (enable/disable/add shortcuts)
- Appearance: Overlay theme selection (OverlayThemeCard in `AppearanceSettings.kt`)

**Transitions**: Animated transitions using Compose AnimatedContent. Settings use `SettingsRoute` and `SettingsDetailRoute`.

---

## 🧪 Testing Patterns

### Current Testing Strategy

- Unit tests for utilities (`SearchRankingUtils`, `PhoneNumberUtils`, etc.)
- UI tests for critical user flows
- Manual testing for permissions and integrations

**Note**: Testing infrastructure is minimal; expansion opportunity exists.

---

## 🛠️ Build Configuration

### Gradle Setup

**Version Catalog** (`gradle/libs.versions.toml`):
```toml
[versions]
agp = "8.12.3"
kotlin = "2.0.21"
coreKtx = "1.17.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.9.4"
activityCompose = "1.12.0"
browser = "1.8.0"
composeBom = "2025.12.01"
material = "1.12.0"
glance = "1.1.1"
securityCrypto = "1.1.0-alpha06"
okhttp = "4.12.0"
playReview = "2.0.1"
playUpdate = "2.1.0"
playServicesOssLicenses = "17.3.0"
ossLicensesPlugin = "0.10.10"
libphonenumber = "8.13.36"
reorderable = "2.4.3"
fuzzywuzzy = "1.0.1"
```

**Build Configuration**:
- Min SDK: 24 (Android 7.0)
- Target SDK: 36 (Android 15)
- Compile SDK: 36
- Version: 2.5.2 (Code 33)

**Key Dependencies**:
- Jetpack Compose (BOM 2025.12.01)
- Material 3
- Glance (App Widget framework)
- Security Crypto (for encrypted preferences)
- OkHttp (for web requests)
- Play Review/Update APIs
- Play Services OSS Licenses
- libphonenumber (phone number parsing/normalization)
- FuzzyWuzzy (fuzzywuzzy-kotlin for advanced fuzzy search)
- Reorderable (drag-and-drop reordering)
- Browser (AndroidX browser integration)
- Activity Compose (Compose activity integration)

---

## 🔑 Key Conventions & Patterns

### Naming Conventions

- **Data models**: Suffix with type (e.g., `SearchUiState`, `AppInfo`)
- **Repositories**: Suffix with `Repository` (e.g., `ContactRepository`)
- **Handlers**: Suffix with `Handler` (e.g., `ContactActionHandler`)
- **Sections**: UI composables for sections suffix with `Section` (e.g., `ContactResultsSection`)
- **Utils**: Utility classes suffix with `Utils` (e.g., `SearchRankingUtils`)

### File Organization

- **Feature-based**: Organized by feature domain (apps, contacts, files, etc.)
- **Layer separation**: Clear separation between data, logic, and UI
- **Shared utilities**: Common utilities in `shared/util/` and `search/common/`

### State Management Conventions

- **Immutability**: All state classes are immutable data classes
- **Sealed classes**: For state variants (visibility, screen states)
- **Single StateFlow**: One central `SearchUiState` in ViewModel
- **Update function**: Centralized `updateUiState` for all state changes

### Composable Conventions

- **Preview annotations**: Use `@Preview` for composable previews
- **Modifier parameter**: Always include `Modifier = Modifier` parameter
- **State hoisting**: State owned by ViewModel, events passed down
- **Reusability**: Use shared components from `components/` packages

---

## 🔒 Security & Privacy

### Data Storage

- **Encrypted SharedPreferences**: Gemini API keys stored with `EncryptedSharedPreferences`
- **Local-first**: All search processing happens on-device
- **No analytics**: No tracking or analytics libraries

### Permission Philosophy

- **Optional by default**: Only usage permission is truly required
- **Graceful degradation**: App works with minimal permissions
- **User control**: All permissions manageable in settings

---

## 📝 Common Development Workflows

### Adding a New Search Section

1. Create data model in `search/models/` (or feature-specific `models/`)
2. Create repository in `search/data/` if needed
3. Add state to `SearchUiState` in `SearchModels.kt`
4. Add search logic to `SearchViewModel.kt` and/or a dedicated handler
5. Create section composable in appropriate feature package (e.g. `search/contacts/`, `search/files/`)
6. Add section rendering in `SearchScreenSectionRendering.kt` / `SectionRenderingComposables.kt`
7. Update section ordering in `SectionManager.kt` and `SearchSection` enum if applicable

### Adding a New Preference

1. Add preference field to appropriate preference class in `search/data/preferences/`
2. Add getter/setter methods
3. Update `UserAppPreferences.kt` to delegate to new preference class
4. Add UI in appropriate settings section
5. Update `SearchUiState` if needed for runtime reflection

### Modifying Search UI

1. Identify relevant composable in `search/searchScreen/`
2. Check if design tokens should be updated in `shared/ui/theme/DesignTokens.kt`
3. Update composable logic
4. Test with different permission states
5. Verify wallpaper mode compatibility

---

## 🐛 Common Issues & Solutions

### Issue: State not updating in UI
**Solution**: Ensure state is properly hoisted and StateFlow is being collected

### Issue: Permission not reflecting in UI
**Solution**: Check permission state is updated in `SearchUiState` and UI observes the state

### Issue: Search results not ranking correctly
**Solution**: Verify `SearchRankingUtils` is applied and check for case sensitivity

### Issue: App cache not updating
**Solution**: Force cache rebuild by clearing or deleting cache in `AppCache`

---

## 📚 Important Files for AI Agents

### Critical Files to Understand

1. **SearchViewModel.kt** (2946 lines) - Central business logic
2. **SearchUiState** in **SearchModels.kt** - Complete state definition
3. **SearchScreenLayout.kt** - Main UI layout orchestration
4. **UserAppPreferences.kt** - All preference management
5. **SearchRankingUtils.kt** - Search algorithm (in `search/common/`)
6. **OverlayRoot.kt** - Overlay mode UI root (`search/overlay/`)
7. **GeminiModelPickerDialog.kt** - Direct Search model selection (Gemini/Gemma)
8. **OverlayGradientThemeUtils.kt** - Overlay theme gradients
9. **AppearanceSettings.kt** - Overlay theme UI (OverlayThemeCard); **AppManagementSettings.kt** - App management (details, bulk uninstall); **AppShortcutsSettings.kt** - Shortcut management UI

### Entry Points

- **MainActivity.kt** - Application entry (full-screen search)
- **OverlayActivity.kt** - Overlay mode entry (search over other apps)
- **SearchScreen.kt** / **SearchRoute** - Main search UI entry
- **SettingsScreen.kt** / **SettingsRoute** - Settings entry
- **NavigationManager.kt** - Navigation flow

### Configuration Files

- **build.gradle.kts** - Build configuration
- **libs.versions.toml** - Version catalog
- **AndroidManifest.xml** - Permissions and components

---

## 🎨 UI Architecture

### Layout Modes

**Normal Layout**:
- Pinned items at top
- Search results below
- Standard scrolling

**One-Handed (Keyboard-Aligned) Layout** (`oneHandedMode`):
- Search results positioned at bottom (near keyboard)
- Single-handed mode friendly
- Reversed scroll direction

**Overlay Mode** (`overlayModeEnabled`):
- Search UI shown in a floating overlay over other apps
- Entry point: `OverlayActivity`; UI root: `OverlayRoot.kt`
- Same ViewModel and state as full-screen search

### Section Visibility Logic

Sections are shown/hidden based on:
- Permission state
- Query content
- Section enabled/disabled setting
- Available results

**Visibility States** (Sealed Classes):
- `Hidden` - Section not shown
- `Loading` - Loading state
- `NoResults` - No results to show
- `ShowingResults` - Results available

### Enhanced Layout Components

**Modular Search Screen Layout** (`search/searchScreen/searchScreenLayout/`):
- `ContentLayout.kt` - Main content orchestration with responsive layout logic
- `SearchContentArea.kt` - Content area management with scrolling and positioning
- `SectionRenderingState.kt` - Section rendering state management
- `SearchScreenLayoutConstants.kt` - Layout constants and shape definitions

**Layout Constants**:
- `TopRoundedShape` - Custom shape for top-rounded corners (16dp radius)
- Enhanced responsive design with improved tablet optimizations

---

## 🔧 Widget System (Glance)

**Technology**: Jetpack Glance App Widget framework

**Widget Types**:

**Search Widget**:
- Tap to instantly open Quick Search
- Voice search support with mic button
- Highly customizable with up to 2 custom buttons alongside search functionality
- Configurable mic button actions (voice search or digital assistant)

**Custom Buttons Widget**:
- Dedicated widget for custom button layouts
- Enhanced customization options with flexible button arrangements
- Independent from search widget for specialized use cases

**Features**:
- Custom widget buttons for apps, shortcuts, files, contacts, and settings
- Customizable colors (background, text, icons) with expanded color options beyond white/black
- Adjustable border thickness and corner radius
- Configurable transparency and opacity
- Show/hide elements (text, icons, mic icon)
- Icon alignment options (left or center)
- Enhanced visual customization for better personalization

**Key Files**:
- `widgets/searchWidget/` - Search widget implementation
- `widgets/customButtonsWidget/` - Custom buttons widget with advanced configuration
- `widgets/widgetConfigScreen/` - Shared widget configuration UI components

---

## 🌐 External Integrations

### Google APIs
- **Play Review API**: In-app review prompts
- **Play App Update API**: In-app update prompts
- **Google Suggest API**: Web search suggestions
- **Gemini API**: Direct Search / AI answers

### Third-Party App Integrations
- WhatsApp (via intent)
- Telegram (via intent)
- Google Meet (via intent)
- Icon packs (via icon pack launcher APIs)

---

## 📖 Additional Resources

### Documentation Files
- **README.md** - User-facing documentation
- **FEATURES.md** - Complete feature list
- **PRIVACY_POLICY.md** - Privacy policy
- **LICENSE** - MIT License

### Code Comments
- Most complex logic includes inline documentation
- Public APIs have KDoc comments
- UI composables include usage examples in comments

---

## 🎯 Design Philosophy

### Core Principles

1. **Speed First**: Optimized for instant search results
2. **Unified Experience**: All search types in one place
3. **Privacy Focused**: Local processing, no tracking
4. **Customizable**: Extensive user configuration options
5. **Graceful Degradation**: Works with minimal permissions
6. **Material Design**: Follows Material 3 guidelines

### UI Principles

1. **Wallpaper Integration**: Background showcases device wallpaper
2. **Dark Theme**: Optimized for dark mode
3. **Minimal Friction**: Auto-focus, keyboard-first
4. **Gesture Support**: Long-press for context menus
5. **Visual Feedback**: Haptic feedback for interactions

---

## 💡 Tips for AI Agents

### When Modifying Code

✅ **Do**:
- Follow existing naming conventions
- Use sealed classes for state variants
- Leverage design tokens for spacing/colors
- Test with different permission states
- Consider wallpaper mode impact
- Update state immutably

❌ **Don't**:
- Hardcode dimensions or colors
- Bypass `updateUiState()` for state changes
- Create direct permission checks in UI (use ViewModel state)
- Ignore existing utility classes
- Break immutability of state classes

### When Adding Features

1. Start with data model
2. Add repository if needed
3. Update ViewModel state
4. Create UI composable
5. Integrate with existing sections
6. Add preferences if user-configurable
7. Update settings UI if needed

### When Debugging

1. Check StateFlow emissions in ViewModel
2. Verify composable is observing correct state
3. Check permission states in `SearchUiState`
4. Review search ranking logic
5. Test with empty query vs with query
6. Verify preference persistence

---

## 📊 Metrics & Analytics

**Current State**: No analytics or tracking

**Measurement Points** (if implemented):
- Search query patterns
- Feature usage (search engines, sections)
- Performance metrics (startup time, search latency)
- Error rates (crashes, permission denials)

**Privacy**: Any analytics should be optional and privacy-preserving

---

## 🔮 Future Architecture Considerations

### Potential Improvements

1. **Modularization**: Split into feature modules
2. **Dependency Injection**: Consider Hilt or Koin
3. **Testing**: Expand test coverage
4. **Repository Abstraction**: Interface-based repositories
5. **UseCase Layer**: Add domain layer with UseCases
6. **Flow Optimization**: More granular StateFlows per section
7. **Background Processing**: WorkManager for cache updates
8. **Database**: Consider Room for complex data

### Migration Opportunities

- **From SharedPreferences to DataStore**: For modern coroutine-based persistence
- **From single StateFlow to multiple**: For better performance isolation
- **From monolithic ViewModel**: To feature-specific ViewModels (coordinated by parent)

---

**Last Updated**: March 3, 2026 (v2.5.2)
**For Questions**: Refer to code comments, README.md, FEATURES.md, or analyze usage patterns in codebase