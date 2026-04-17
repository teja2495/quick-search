# Quick Search

A fast & powerful Android app that lets you search across **apps, app shortcuts, contacts, calendar events, device files, device settings, app settings, and the web**, plus access tools like **calculator, unit converter, etc**—all from a single search bar. Built with Kotlin and Jetpack Compose using Material 3 design.

<a href="https://play.google.com/store/apps/details?id=com.tk.quicksearch"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80"></a>
<a href="https://github.com/teja2495/quick-search/releases/latest"><img src="https://github.com/user-attachments/assets/5d36bf7f-3386-4b0e-b7e1-892daba01343" alt="Get it on GitHub" height="80"></a>

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📸 Screenshots

| | | |
|---|---|---|
| <img src="screenshots/image1.png" width="280" alt="Screenshot 1"> | <img src="screenshots/image2.png" width="280" alt="Screenshot 2"> | <img src="screenshots/image3.png" width="280" alt="Screenshot 3"> |
| <img src="screenshots/image5.png" width="280" alt="Screenshot 5"> | <img src="screenshots/image6.png" width="280" alt="Screenshot 6"> | <img src="screenshots/image7.png" width="280" alt="Screenshot 7"> |

## ✨ Key Features

### 🔍 Unified Search
- **Apps**: Search and launch installed applications with smart ranking. App search ignores typos and allows abbreviations. Long press apps to access their shortcuts (if available).
- **App Shortcuts**: Find and launch app shortcuts (including custom deep links and search/browser shortcuts) alongside app results.
- **Contacts**: Find and call/text contacts with multi-number support and integrations such as WhatsApp, Telegram, Signal, Google Meet, and more. Long press contact actions to customize them.
- **Calendar Events**: Search your calendar events from the search bar.
- **Notes**: Write notes and search them from the search bar. Swipe left on the home screen for a quick note.
- **Files**: Search device files and folders (images, videos, documents, etc.)
- **Device Settings**: Search Android system settings
- **App Settings**: Find and open Quick Search app settings directly from the search bar
- **Web**: Search the web using 25 search engines with customizable shortcuts and web suggestions. Browsers can be added as search engines.
- **Calculator**: Built-in calculator for math expressions (+, -, *, /, brackets); optional alias to open calculator mode
- **Unit Converter**: Convert units from the search bar (length, mass, temperature, area, volume, time, speed, data, energy, power, pressure, angle, frequency, and more)
- **Currency Converter**: Convert between currencies with live exchange rates directly from the search bar
- **World Clock**: Check the current time across different time zones by searching for city names
- **Date & Time**: Natural-language date parsing, differences, offsets, and time arithmetic from the search bar (optional alias)
- **Gemini API**: Direct search with AI-powered answers using Gemini/Gemma models and optional personal context
- **AI Providers**: Configure API keys for OpenAI, Claude, and Groq
- **Custom AI Tools**: Create your own AI tools (requires an API key)
- **Overlay Mode**: Enable to make the search bar appear over other apps, anywhere—changes how you access search from any screen
- **Home Screen Widget**: Search widget and custom buttons widget with extensive customization options
- **Launch Options**: Widget, Quick Settings Tile & Digital Assistant

### 🎨 Customization & New Features
- **App Themes**: Choose from multiple visual themes (Mono, Forest, Aurora, Sunset) for the app's appearance
- **Material You**: Use your device's color palette from Material You for the app's theme and accent colors
- **Bottom Search Bar**: Option for bottom-positioned search bar for improved accessibility
- **Font Size Control**: Customize font size throughout the app for better readability
- **Custom Backgrounds**: Select any picture from your device for the app background, with transparency and blur controls
- **App Management**: View app details or bulk uninstall apps from search results settings
- **Shortcut Management**: Enable, disable, or add custom shortcuts including search queries, URLs, and app activities
- **Import/Export Settings**: Backup and restore your Quick Search configuration and preferences

### Search Shortcuts
Configure custom keyboard shortcuts for search engines and add them at the start of a query to quickly trigger the respective search engine:
- `ggl` → Google
- `ytb` → YouTube
- `mps` → Google Maps
- And more...

### Direct Search
Enable AI-powered answers by:
1. Getting a Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Adding the key in Settings → Search Engines → Direct Search
3. Optionally add personal context for personalized answers
4. Choose any Gemini/Gemma models of your choice

### Search Engines
- **Supported** (25): Direct Search, Google, ChatGPT, Gemini, Perplexity, Grok, Google Maps, Google Drive, Google Photos, Google Play, YouTube, YouTube Music, Spotify, Reddit, Amazon, X (Twitter), Facebook Marketplace, Bing, DuckDuckGo, Brave, Startpage, You.com, AI Mode, Claude, Wikipedia—each can be reordered or disabled. You also have option to add **custom** search engines.
- **Browsers**: Add installed browsers as search engines
- **Direct Search**: AI answers with Gemini API integration; choose among several Gemini and Gemma models; optional personal context
- **AI Providers**: Support for OpenAI, Claude, and Groq API keys
- **Style**: Choose between inline or compact styles

[View all features](app/src/main/assets/FEATURES.md)

## 🚀 Installation

### Requirements
- Android 7.0 (API 24) or higher
- Target SDK: Android 15 (API 36)

### Build from Source
```bash
# Clone the repository
git clone https://github.com/teja2495/quick-search.git
cd quick-search

# Build with Gradle
./gradlew assembleRelease
```

### Download
- **Google Play Store**: Get the app from [Google Play Store](https://play.google.com/store/apps/details?id=com.tk.quicksearch) for automatic updates
- **APK Release**: Download the latest APK from the [Releases](https://github.com/teja2495/quick-search/releases) page

## 🛡️ Permissions & Privacy

Quick Search prioritizes your privacy. All permissions are optional, only used to unlock additional features. All search processing happens locally on your device. For detailed information, see our [Privacy Policy](PRIVACY_POLICY.md).

**Key Points:**
- No ads or analytics
- Encrypted storage for API keys
- Granular permission controls
- Local data processing

## 🏗️ Architecture

Built with modern Android development practices:

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose with Material 3 (BOM 2025.12.01)
- **Architecture**: MVVM with ViewModels and StateFlow (unidirectional data flow)
- **State Management**: Single source of truth with sealed classes for type-safe states
- **Persistence**: SharedPreferences with encryption for sensitive data (Gemini API keys)
- **Widgets**: Jetpack Glance App Widget framework
- **Build System**: Gradle Kotlin DSL (8.12.3) with version catalogs
- **Min SDK**: 24 (Android 7.0) | **Target SDK**: 36 (Android 15)

### Project Structure
```
app/src/main/java/com/tk/quicksearch/
├── app/                    # Application entry point and app-level handlers
├── navigation/             # Navigation management with animated transitions
├── shared/                 # Shared components and utilities
│   ├── ui/                 # Shared UI components and Material 3 theming
│   │   ├── components/     # Reusable UI components
│   │   └── theme/          # Theming, colors, and design tokens
│   └── util/               # Shared utility functions
├── search/                 # Main search functionality
│   ├── models/             # Data models (AppInfo, ContactInfo, DeviceFile, etc.)
│   ├── data/               # Data layer with repositories and preferences
│   ├── core/               # SearchViewModel, state management, unified search
│   ├── apps/               # App search, icons, and management
│   ├── appShortcuts/       # App shortcut search and actions
│   ├── contacts/           # Contact search with messaging integrations
│   ├── files/              # File search and management
│   ├── deviceSettings/     # Device settings search
│   ├── searchScreen/       # Main search UI and layout orchestration
│   ├── overlay/            # Overlay mode (search over other apps)
│   ├── webSuggestions/     # Web search suggestions
│   ├── fuzzy/              # Fuzzy search engine
│   ├── searchHistory/      # Recent items tracking and display
│   └── common/             # Shared utilities and handlers
├── searchEngines/          # Search engine integration and management
│   ├── compact/            # Compact mode UI
│   ├── inline/             # Inline mode UI
│   └── shared/             # Shared search engine components
├── tools/                  # Specialized tools and utilities
│   ├── calculator/         # Calculator functionality
│   └── aiSearch/            # AI Search (Gemini API integration)
├── settings/               # Settings screens (restructured)
│   ├── appearanceSettings/ # Appearance and theme settings
│   ├── appShortcutsSettings/# App shortcuts configuration
│   ├── searchEngineSettings/# Search engine settings
│   ├── navigation/         # Settings navigation
│   └── shared/             # Shared settings components
├── onboarding/             # First-launch setup flow
├── widgets/                # Home screen widgets (Glance framework)
├── tile/                   # Quick Settings tile service
└── util/                   # Legacy utility functions (most moved to shared/util/)
```

### Key Architectural Patterns
- **Feature-based organization**: Code organized by feature domain
- **Repository pattern**: Data layer abstraction
- **Sealed classes**: Type-safe state management
- **Three-phase initialization**: Optimized startup performance
- **Modular preferences**: Specialized preference management classes


## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

- **Email**: tejakarlapudi.apps@gmail.com
- **Issues**: [GitHub Issues](https://github.com/teja2495/quick-search/issues)

---

**Made with ❤️ for Android**
