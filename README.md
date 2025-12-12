# Quick Search

A powerful Android app that lets you search across **apps, contacts, device files, device settings, and the web** from a single screen. Built with Kotlin and Jetpack Compose using Material 3 design.

[![Get it on Google Play](https://img.shields.io/badge/Get%20it%20on%20Google%20Play-4285F4?style=for-the-badge&logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=com.tk.quicksearch)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## ✨ Key Features

### 🔍 Unified Search
- **Apps**: Search and launch installed applications with smart ranking
- **Contacts**: Find and call/text contacts with multi-number support
- **Files**: Search device files (images, videos, documents, APKs, etc.)
- **Settings**: Quick access to Android system settings shortcuts
- **Web**: Integrated search engines with customizable shortcuts. 
- **Gemini API**: Direct search can be enabled by configuring your own Gemini API key.

### 🎯 Smart Features
- **App Nicknames**: Assign custom names to apps for easier searching
- **Direct Search**: AI-powered answers using Gemini API (optional)
- **Optional Keyboard-Aligned Layout**: Optimized for typing with search results first
- **Section Ordering**: Customize which content appears first
- **Pinned Apps**: Keep favorite apps always visible
- **Recent Apps**: Smart ranking based on usage patterns

### 📱 Widget Support
- Home screen widget with customizable appearance
- Configurable colors, borders, and labels
- Instant access to search functionality

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
Download the latest APK from the [Releases](https://github.com/your-username/quick-search/releases) page.

## 📖 Usage

### Getting Started
1. **Grant Permissions**: On first launch, grant Usage Access permission (required)
2. **Optional Permissions**: Enable contacts and storage access for full functionality
3. **Start Searching**: Type in the search field to find apps, contacts, files, or web results

### Search Shortcuts
Configure custom keyboard shortcuts for search engines:
- `g` → Google
- `yt` → YouTube
- `maps` → Google Maps
- And more...

### Direct Search
Enable AI-powered answers by:
1. Getting a Gemini API key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Adding the key in Settings → Search Engines → Direct Search
3. Optionally add personal context for better answers

## ⚙️ Configuration

### Search Engines
- **Supported**: Google, ChatGPT, Perplexity, Grok, Google Maps, Google Play, Reddit, YouTube, Amazon
- **Customizable**: Reorder, enable/disable, and set shortcuts
- **Direct Search**: AI answers with Gemini API integration

### Sections
Toggle and reorder search result sections:
- Apps (always visible)
- Contacts (requires permission)
- Files (requires permission)
- Settings (device shortcuts)

### File Types
Filter which file types to include in search:
- Images, Videos, Audio
- Documents, APKs, Other files

### Contact Preferences
- Choose default messaging app (Messages, WhatsApp, Telegram)
- Enable direct dial (call without opening dialer)
- Set preferred numbers per contact

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
├── model/          # Data models
├── permissions/    # Permission handling
├── search/         # Main search UI and logic
├── settings/       # Settings screens
├── ui/theme/       # Material 3 theming
├── util/           # Utility functions
└── widget/         # Home screen widget
```


## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

- **Email**: tejakarlapudi.apps@gmail.com
- **Issues**: [GitHub Issues](https://github.com/your-username/quick-search/issues)

---

**Made with ❤️ for Android users who want fast, unified search across their device.**