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
- **View Layer** (`search/searchScreen/`, UI composables): Jetpack Compose UI components

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
    val contactsSectionState: ContactsSectionVisibility = ContactsSectionVisibility.Hidden,
    val filesSectionState: FilesSectionVisibility = FilesSectionVisibility.Hidden,
    val settingsSectionState: SettingsSectionVisibility = SettingsSectionVisibility.Hidden,
    val searchEnginesState: SearchEnginesVisibility = SearchEnginesVisibility.Hidden,
    
    // Data
    val recentApps: List<AppInfo> = emptyList(),
    val searchResults: List<AppInfo> = emptyList(),
    val contactResults: List<ContactInfo> = emptyList(),
    val fileResults: List<DeviceFile> = emptyList(),
    // ... more state fields
)
```

**Key Patterns**:
- **Sealed Classes for State**: Used for visibility and screen states to ensure type-safe state transitions
- **StateFlow**: Single source of truth managed by `SearchViewModel`
- **State Updates**: Centralized through `updateUiState()` method in ViewModel
- **Immutable State**: All updates create new state copies

### Data Layer Architecture

**Repository Pattern** with specialized repositories:

1. **AppUsageRepository** (`search/data/AppUsageRepository.kt`)
   - Loads launchable apps and usage statistics
   - Manages app cache for fast startup

2. **ContactRepository** (`search/data/ContactRepository.kt`)
   - Contact search by name
   - Aggregates phone numbers per contact
   - Supports preferred numbers and exclusions

3. **FileSearchRepository** (`search/data/FileSearchRepository.kt`)
   - Device file search via MediaStore API
   - Respects enabled file type filters

4. **UserAppPreferences** (`search/data/UserAppPreferences.kt`)
   - Centralized preference management
   - Delegates to specialized preference classes in `search/data/preferences/`

**Preference Management Pattern**:
```kotlin
// Modular preferences with base class
- BasePreferences.kt - Base utilities and common operations
- AppPreferences.kt - App-related preferences
- ContactPreferences.kt - Contact preferences
- UiPreferences.kt - UI settings
- SearchEnginePreferences.kt - Search engine configuration
// ... etc.
```

**Caching Strategy**:
- **AppCache** (`search/data/AppCache.kt`): SharedPreferences-based JSON serialization for instant app list loading
- **Startup Performance**: Three-phase initialization (critical prefs → cached data → deferred handlers)

---

## 📁 Project Structure

```
app/src/main/java/com/tk/quicksearch/
│
├── app/                          # Application entry point
│   └── MainActivity.kt           # Main activity, navigation setup, edge-to-edge
│
├── navigation/                   # Navigation management
│   └── NavigationManager.kt     # Centralized navigation with animated transitions
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
│   │   ├── AppUsageRepository.kt
│   │   ├── AppCache.kt
│   │   ├── ContactRepository.kt
│   │   ├── FileSearchRepository.kt
│   │   ├── UserAppPreferences.kt # Central preferences manager
│   │   └── preferences/          # Modular preference classes
│   │       ├── BasePreferences.kt
│   │       ├── AppPreferences.kt
│   │       ├── ContactPreferences.kt
│   │       ├── FilePreferences.kt
│   │       ├── UiPreferences.kt
│   │       ├── SearchEnginePreferences.kt
│   │       ├── GeminiPreferences.kt
│   │       └── ... (more preference modules)
│   │
│   ├── core/                     # Core search logic
│   │   ├── SearchViewModel.kt   # Main ViewModel (1434 lines)
│   │   ├── SearchModels.kt      # State and model definitions
│   │   ├── SearchViewModelPermissionManager.kt
│   │   ├── SearchViewModelSearchOperations.kt
│   │   ├── UnifiedSearchHandler.kt
│   │   └── SectionManager.kt
│   │
│   ├── apps/                     # App search features
│   │   ├── AppSearchManager.kt  # Search logic and filtering
│   │   ├── AppGridView.kt       # 2×5 grid rendering
│   │   ├── AppIconManager.kt    # Async icon loading
│   │   └── AppManagementService.kt
│   │
│   ├── contacts/                 # Contact search and actions
│   │   ├── ContactResultsSection.kt
│   │   ├── ContactActionHandler.kt
│   │   ├── ContactManagementHandler.kt
│   │   ├── MessagingHandler.kt  # WhatsApp/Telegram integration
│   │   └── ... (17+ contact-related files)
│   │
│   ├── files/                    # File search
│   │   ├── FilesSection.kt
│   │   └── FileManagementHandler.kt
│   │
│   ├── deviceSettings/           # Device settings search
│   │   ├── DeviceSettingsSearchHandler.kt
│   │   └── SettingsSection.kt
│   │
│   ├── searchEngines/            # Search engine integration
│   │   ├── SearchEngineManager.kt
│   │   ├── SearchEngineSection.kt
│   │   ├── DirectSearchClient.kt  # Gemini API integration
│   │   ├── WebSuggestionHandler.kt
│   │   └── ... (9+ search engine files)
│   │
│   ├── calculator/               # Calculator functionality
│   │   └── CalculatorHandler.kt
│   │
│   ├── searchScreen/             # Main search UI
│   │   ├── SearchScreen.kt      # Main screen composable
│   │   ├── SearchScreenLayout.kt # Layout orchestration
│   │   ├── SearchScreenContent.kt
│   │   ├── SearchScreenComponents.kt
│   │   ├── SearchScreenSectionRendering.kt
│   │   ├── SearchScreenHelpers.kt
│   │   └── ... (16+ UI files)
│   │
│   ├── handlers/                 # Specialized handlers
│   │   ├── PinningHandler.kt
│   │   ├── NavigationHandler.kt
│   │   ├── ShortcutHandler.kt
│   │   └── ReleaseNotesHandler.kt
│   │
│   └── common/                   # Shared utilities
│
├── settings/                     # Settings screens
│   ├── main/                     # Main settings UI
│   ├── appearance/               # Visual settings
│   ├── searchEngines/            # Search engine configuration
│   ├── components/               # Reusable components
│   └── permissions/              # Permission management
│
├── onboarding/                   # First-launch setup
│   ├── SearchEngineSetupScreen.kt
│   └── FinalSetupScreen.kt
│
├── permissions/                  # Permission handling
│   ├── PermissionsScreen.kt
│   └── PermissionRequestHandler.kt
│
├── widget/                       # Home screen widget (Glance)
│   ├── QuickSearchWidget.kt
│   ├── QuickSearchWidgetConfigureActivity.kt
│   └── ... (12+ widget files)
│
├── tile/                         # Quick Settings tile
│   └── QuickSearchTileService.kt
│
├── ui/theme/                     # Material 3 theming
│   ├── Theme.kt
│   ├── Color.kt
│   ├── AppColors.kt             # Centralized color definitions
│   ├── DesignTokens.kt          # Design system tokens
│   └── Type.kt
│
└── util/                         # Utility functions
    ├── SearchRankingUtils.kt    # Search ranking algorithm
    ├── PhoneNumberUtils.kt
    ├── WallpaperUtils.kt
    ├── HapticUtils.kt
    ├── ReviewHelper.kt          # Play Review API
    └── UpdateHelper.kt          # Play App Update API
```

---

## 🔄 Data Flow & State Update Pattern

### Search Query Flow

1. **User Input** → `SearchScreen.kt` composable
2. **ViewModel Update** → `SearchViewModel.onQueryChanged()`
3. **State Calculation** → Multiple handlers compute results:
   - App search: `AppSearchManager`
   - Contact search: `ContactRepository`
   - File search: `FileSearchRepository`
   - Web suggestions: `WebSuggestionHandler`
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

### Color System (`ui/theme/AppColors.kt`)

**Theme Colors**:
- Primary: Deep Purple (`#651FFF`)
- Secondary: Neon Purple (`#D500F9`)
- Tertiary: Indigo (`#5E35B1`)

**Dynamic Theming**:
- Wallpaper mode: Semi-transparent overlays (`OverlayMedium` = Black @ 40% alpha)
- Standard mode: Material 3 surface containers

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

### Design Tokens (`ui/theme/DesignTokens.kt`)

Centralized spacing and dimensions:
- `ContentHorizontalPadding = 20.dp`
- `CardHorizontalPadding = 20.dp`
- `CardShape = RoundedCornerShape(16.dp)`
- `ExtraLargeCardShape = RoundedCornerShape(28.dp)`
- `IconSize = 24.dp`
- `LargeIconSize = 28.dp`

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

---

## 🔍 Search Algorithm & Ranking

### Multi-Source Ranking (`util/SearchRankingUtils.kt`)

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
- With query: Show **filtered search results** ranked by `SearchRankingUtils`
- Grid: 2 rows × 5 columns = **10 apps maximum**

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

### Permission Handling Pattern

1. **Check** → `SearchViewModel` checks permissions
2. **Request** → `PermissionRequestHandler` manages permission requests
3. **State Update** → Permission state reflected in `SearchUiState`
4. **UI Adaptation** → Sections hidden/shown based on permissions

---

## 📱 Key Features & Implementation

### 1. Search Engines

**Supported Engines** (22 total):
Direct Search, Google, ChatGPT, Gemini, Perplexity, Grok, Google Maps, Google Play, YouTube, Amazon, Bing, Brave, DuckDuckGo, Facebook Marketplace, Google Drive, Google Meet, Google Photos, Spotify, X/Twitter, You.com, YouTube Music, Startpage

**Features**:
- **Shortcuts**: Custom keyboard shortcuts (e.g., "ggl" for Google)
- **Reordering**: User-customizable order
- **Enable/Disable**: Toggle individual engines
- **Direct Search**: Gemini API integration with optional personal context
- **Display Modes**: Inline (scrolls) vs Sticky (fixed at bottom)

**Implementation**: `search/searchEngines/SearchEngineManager.kt`

### 2. Contact Integration

**Messaging Support**:
- Default SMS/Messages
- WhatsApp (chat, audio, video)
- Telegram (chat, audio, video)
- Google Meet

**Features**:
- Multi-number support with number picker
- Preferred number "remember choice"
- Direct dial (call without dialer)
- Number normalization via `PhoneNumberUtils`

**Implementation**: `search/contacts/ContactActionHandler.kt`, `MessagingHandler.kt`

### 3. Calculator

**Capabilities**:
- Basic operations: `+`, `-`, `*`, `/`
- Brackets for precedence
- Real-time evaluation in search bar

**Implementation**: `search/calculator/CalculatorHandler.kt`

### 4. Web Suggestions

- Google-powered suggestions via Google Suggest API
- Configurable suggestion count (default: 3)
- Recent queries feature (stores last 10, displays configurable count)

**Implementation**: `search/webSuggestions/WebSuggestionHandler.kt`

### 5. Pinning System

**Pinnable Items**:
- Apps
- Contacts
- Files
- Device Settings

**Storage**: Stored in `UserAppPreferences` (package names, contact IDs, file paths, setting IDs)

**UI Pattern**: Long-press → Context menu → Pin/Unpin option

### 6. File Search

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

---

## 🎯 Navigation & Routing

### Navigation Structure

**Root Destinations** (`navigation/NavigationManager.kt`):
- `FirstLaunch` - Onboarding flow
- `Search` - Main search screen
- `Settings` - Settings navigation

**Settings Detail Types**:
- `SearchEngines` - Search engine configuration
- `ExcludedItems` - Manage hidden/excluded items

**Transitions**: Animated transitions using Compose AnimatedContent

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
composeBom = "2025.12.01"
glance = "1.1.1"
securityCrypto = "1.1.0-alpha06"
```

**Build Configuration**:
- Min SDK: 24 (Android 7.0)
- Target SDK: 36 (Android 15)
- Compile SDK: 36
- Version: 1.2.3 (Code 15)

**Key Dependencies**:
- Jetpack Compose (BOM 2025.12.01)
- Material 3
- Glance (App Widget framework)
- Security Crypto (for encrypted preferences)
- OkHttp (for web requests)
- Play Review/Update APIs

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
- **Shared utilities**: Common utilities in `util/` and `search/common/`

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

1. Create data model in `search/models/`
2. Create repository in `search/data/`
3. Add state to `SearchUiState` in `SearchModels.kt`
4. Add search logic to `SearchViewModel.kt`
5. Create section composable in appropriate feature package
6. Add section rendering in `SearchScreenSectionRendering.kt`
7. Update section ordering in `SectionManager.kt`

### Adding a New Preference

1. Add preference field to appropriate preference class in `search/data/preferences/`
2. Add getter/setter methods
3. Update `UserAppPreferences.kt` to delegate to new preference class
4. Add UI in appropriate settings section
5. Update `SearchUiState` if needed for runtime reflection

### Modifying Search UI

1. Identify relevant composable in `search/searchScreen/`
2. Check if design tokens should be updated in `ui/theme/DesignTokens.kt`
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

1. **SearchViewModel.kt** (1434 lines) - Central business logic
2. **SearchUiState** in **SearchModels.kt** - Complete state definition
3. **SearchScreenLayout.kt** - Main UI layout orchestration
4. **UserAppPreferences.kt** - All preference management
5. **SearchRankingUtils.kt** - Search algorithm

### Entry Points

- **MainActivity.kt** - Application entry
- **SearchScreen.kt** - Main search UI entry
- **SettingsScreen.kt** - Settings entry
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

**Keyboard-Aligned Layout**:
- Search results positioned at bottom (near keyboard)
- Single-handed mode friendly
- Reversed scroll direction

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

---

## 🔧 Widget System (Glance)

**Technology**: Jetpack Glance App Widget framework

**Features**:
- Customizable colors (background, text, icons)
- Adjustable border thickness
- Configurable transparency
- Show/hide elements (text, icons)
- Voice search support

**Key Files**:
- `QuickSearchWidget.kt` - Widget composable
- `QuickSearchWidgetConfigureActivity.kt` - Configuration UI
- `QuickSearchWidgetPreferences.kt` - Widget state persistence

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

**Last Updated**: January 2026 (v1.2.3)  
**For Questions**: Refer to code comments, README.md, or analyze usage patterns in codebase