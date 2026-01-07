# Quick Search

A fast & powerful Android app that lets you search across **apps, contacts, device files, device settings, web, and basic calculations** from a single screen. Built with Kotlin and Jetpack Compose using Material 3 design.

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.tk.quicksearch)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## ✨ Key Features

### 🔍 Unified Search
- **Apps**: Search and launch installed applications with smart ranking
- **Contacts**: Find and call/text contacts with multi-number support and WhatsApp/Telegram/Google Meet integration
- **Files**: Search device files (images, videos, documents, etc.)
- **Settings**: Search Android system settings
- **Web**: Integrated search engines with customizable shortcuts and Google-powered suggestions
- **Calculator**: Built-in calculator for math expressions (+, -, *, /, brackets)
- **Gemini API**: Direct search can be enabled by configuring your own Gemini API key

### 🎯 Smart Features
- **App Nicknames**: Assign custom names to apps for easier searching
- **App Suggestions**: Shows recommended apps when search bar is empty
- **Direct Search**: AI-powered answers using Gemini API (optional)
- **Web Suggestions**: Google-powered search suggestions as you type
- **Multi-App Search**: Tap search engine icons to search within specific apps
- **Wallpaper Integration**: Automatic device wallpaper background
- **Quick Settings Tile**: Instant access from Android Quick Settings
- **Assistant Integration**: Detects when app is set as default digital assistant
- **Icon Pack Support**: Integration with icon pack launchers
- **Optional Single-hand friendly Layout**: Results appear at the bottom of the screen for easier access.
- **Section Ordering**: Customize which content appears first
- **Pinned Results**: Keep favorite results always visible
- **Recent Apps**: Smart ranking based on usage patterns
- **Feedback System**: In-app feedback options and release notes

### 📱 Widget Support
- Home screen widget with customizable appearance
- Configurable colors, borders, and labels
- Instant access to search functionality
- Voice search with microphone button for hands-free searching

### 🔐 Privacy-Focused
- Local processing by default
- Encrypted storage for sensitive data
- Granular permission controls
- No ads or analytics

## 🚀 Installation

### Requirements
- Android 7.0 (API 24) or higher
- Target SDK: Android 15 (API 36)

### Build from Source
```bash
# Clone the repository
git clone https://github.com/your-username/quick-search.git
cd quick-search

# Build with Gradle
./gradlew assembleRelease
```

### Download
- **Google Play Store**: Get the app from [Google Play Store](https://play.google.com/store/apps/details?id=com.tk.quicksearch) for automatic updates
- **APK Release**: Download the latest APK from the [Releases](https://github.com/your-username/quick-search/releases) page

## 📖 Usage

### Getting Started
1. **Permissions**: On first launch, you'll be prompted for several optional permissions:
   - **Usage Access**: Shows recently used apps and usage statistics
   - **Contacts**: Access contact names and phone numbers for contact search
   - **Storage/Media**: Access device files for file search
   - **Phone**: Direct dial functionality (call without opening dialer)
   - **Query All Packages**: List installed applications
   All permissions are optional and can be granted later in Settings → Permissions
2. **Configure Search Engines**: Visit Settings → Search Engines to customize available search engines, set shortcuts, and optionally enable AI-powered answers with Gemini API
3. **Start Searching**: Type in the search field to find apps, contacts, files, web results, or calculate math expressions

### Quick Access
- **Quick Settings Tile**: Add Quick Search to Android Quick Settings for instant access
- **Home Screen Widget**: Add a customizable widget to your home screen
- **Digital Assistant**: Set Quick Search as your device's default digital assistant for enhanced search integration

### Search Shortcuts
Configure custom keyboard shortcuts for search engines:
- `ggl` → Google
- `ytb` → YouTube
- `mps` → Google Maps
- And more...

### Direct Search
Enable AI-powered answers by:
1. Getting a Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Adding the key in Settings → Search Engines → Direct Search
3. Optionally add personal context for better answers

## ⚙️ Configuration

### Search Engines
- **Supported**: Google, ChatGPT, Perplexity, Grok, Google Maps, Google Play, Reddit, YouTube, Amazon, Bing, Brave, DuckDuckGo, Facebook Marketplace, Google Drive, Google Meet, Google Photos, Spotify, X/Twitter, You.com, YouTube Music
- **Customizable**: Reorder, enable/disable, and set shortcuts
- **Direct Search**: AI answers with Gemini API integration

### Sections
Toggle and reorder search result sections:
- Apps (always visible)
- Contacts (requires permission)
- Files (requires permission)
- Settings (device shortcuts)
- Calculator (built-in math expressions)

### File Types
Filter which file types to include in search:
- Images, Videos, Audio
- Documents, APKs, Other files

### Result Management
- **Exclude Items**: Long press any result to hide it from future searches
- **File Type Filtering**: Exclude specific file types from search results

### Contact Preferences
- Choose default messaging app (Messages, WhatsApp, Telegram, Google Meet)
- Enable direct dial (call without opening dialer)
- Set preferred numbers per contact
- Support for multiple phone numbers per contact

## 🔑 Permissions

### Required
- **Usage Access**: Required to show recently used apps and usage statistics

### Optional
- **Contacts**: Access contact names and phone numbers for contact search
- **Storage/Media**: Access device files for file search
- **Phone**: Direct dial functionality (call without opening dialer)
- **Query All Packages**: List installed applications

## 🛡️ Privacy

Quick Search prioritizes your privacy. All search processing happens locally on your device. For detailed information, see our [Privacy Policy](PRIVACY_POLICY.md).

**Key Points:**
- No ads or analytics
- Encrypted storage for API keys
- Granular permission controls
- Local data processing

## 🏗️ Architecture

Built with modern Android development practices:

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with ViewModels and StateFlow
- **Persistence**: SharedPreferences with encryption for sensitive data
- **Widgets**: Jetpack Glance App Widget framework
- **Build System**: Gradle Kotlin DSL with version catalogs

### Project Structure
```
app/src/main/kotlin/com/tk/quicksearch/
├── data/           # Repositories and data persistence
│   └── preferences/ # Individual preference management
├── model/          # Data models
├── permissions/    # Permission handling
├── search/         # Main search UI and logic
│   ├── apps/       # App search components
│   ├── contacts/   # Contact search components
│   ├── core/       # Core search functionality
│   ├── files/      # File search components
│   ├── handlers/   # Specialized handlers
│   ├── managers/   # Managers (IconPackManager)
│   ├── searchEngines/ # Search engine functionality
│   ├── settings/   # Device settings search
│   └── ui/         # Search UI components
├── settings/       # Settings screens
│   ├── main/       # Main settings components
│   ├── components/ # Shared settings components
│   ├── permissions/# Permission settings
│   └── searchEngines/ # Search engine settings
├── setup/          # Search engine setup screens
├── tiles/          # Quick Settings tile
├── ui/theme/       # Material 3 theming
├── util/           # Utility functions
└── widget/         # Home screen widget
```


## ☕ Support the Development

If you find Quick Search helpful, consider supporting the development:

- **Cash App**: [$teja2495](https://cash.app/$teja2495)
- **Venmo**: [@teja2495](https://account.venmo.com/u/teja2495)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

- **Email**: tejakarlapudi.apps@gmail.com
- **Issues**: [GitHub Issues](https://github.com/your-username/quick-search/issues)

---

**Made with ❤️ for Android**