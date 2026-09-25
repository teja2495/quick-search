# Quick Search

A fast, private Android search app and launcher. Search **apps, app shortcuts, contacts, files, calendar events, notes, reminders, notification history, device settings, and the web**, and use built-in tools like a **calculator, unit and currency converters, alarms and timers, and AI Search**, all from a single search bar. Built with Kotlin and Jetpack Compose using Material 3 design.

<a href="https://play.google.com/store/apps/details?id=com.tk.quicksearch"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80"></a>
<a href="https://f-droid.org/en/packages/com.tk.quicksearch/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80"></a>
<a href="https://github.com/teja2495/quick-search/releases/latest"><img src="https://github.com/user-attachments/assets/5d36bf7f-3386-4b0e-b7e1-892daba01343" alt="Get it on GitHub" height="80"></a>

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 📸 Screenshots

| | | |
|---|---|---|
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="280" alt="Screenshot 1"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="280" alt="Screenshot 2"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="280" alt="Screenshot 3"> |
| <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="280" alt="Screenshot 5"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="280" alt="Screenshot 6"> | <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" width="280" alt="Screenshot 7"> |

## ✨ Key Features

### 🔍 Unified Search
- **Apps**: Smart ranking that tolerates typos and matches acronyms (`yt` → YouTube) and nicknames. Long-press an app for its shortcuts.
- **App Shortcuts**: Launch app shortcuts, custom deep links, and Tasker tasks alongside app results.
- **Contacts**: Call or text, with multi-number support and WhatsApp, Telegram, Signal, and Google Meet actions. Long-press actions to customize them or add them to Home.
- **Files**: Search device files and folders.
- **Calendar Events**: Search your calendar events.
- **Notes**: Write and search notes, plus a Quick Note in the Widgets Panel.
- **Reminders**: Create reminders in plain language, e.g. `Laundry in 2 hrs`, and search them later.
- **Notification History**: Search notifications received after access is granted.
- **Device & App Settings**: Jump straight to Android settings or Quick Search settings.
- **Screen Time**: Search `Screen Time` to check daily phone usage.
- **Top Matches**: See the best result across all categories, with configurable categories and priority.
- **App Suggestions**: Swipe between New & Updated, Pinned, and Most Used apps. Drag one pinned app onto another to create a folder.
- **Search History**: Switch between recent queries and recently opened results.
- **Aliases & Triggers**: Prefix a query with an alias to target a section, engine, or tool. Long-press any result to set a trigger word that opens it automatically.

### 🌐 Web & AI
- **30 Search Engines**: Google, ChatGPT, Gemini, Perplexity, Claude, Kagi, DuckDuckGo, YouTube, Spotify, Reddit, Wikipedia, and more. Reorder, disable, or assign aliases (`ggl` → Google). Installed browsers and custom engines can be added too.
- **Web Suggestions**: Suggestions as you type, with a configurable count.
- **AI Search**: AI answers with follow-up questions and optional personal context. Works with Gemini, OpenAI, Claude, Groq, and Meta AI, or any OpenAI-compatible provider. Add a Tavily key for web results on models without built-in search.
- **Custom AI Tools**: Build your own AI-powered tools (requires an API key).

### 🧰 Tools
- **Calculator**: Math expressions, with calculator buttons on the numeric keyboard.
- **Unit Converter**: Length, mass, temperature, area, volume, time, speed, data, energy, power, pressure, angle, and frequency.
- **Currency Converter**: Live exchange rates, no API key needed (`100 USD to EUR`).
- **Date & Time**: Natural-language dates, differences, offsets, and time arithmetic.
- **Alarms & Timers**: `2:45pm` sets an alarm; `15 min` starts a timer.
- **More**: World clock, weather, dictionary, hex/RGB color visualizer, and Tasker integration.

### 🏠 Launcher & Home
- **Set as Launcher**: Use Quick Search as your home screen, with app lock, more system app shortcuts, and customizable swipe and double-tap gestures.
- **At a Glance**: Today's events, upcoming reminders and timers, media controls, and low battery warnings.
- **Widgets Panel**: Swipe right to open a side panel for any Android widget, and pin widgets to Home. Works with or without Quick Search as your launcher.
- **Home Screen Widgets**: Search, custom buttons, and media controls widgets with extensive customization.
- **More Ways to Launch**: Default assistant, Quick Settings tile, overlay mode over other apps, system-wide edge swipe, and a floating button.

### 🎨 Customization
- **Themes**: Mono, Forest, Aurora, and Sunset themes; Material You; custom accent colors; AMOLED black.
- **Backgrounds**: Use your wallpaper or any image, with transparency and blur controls.
- **Layout**: Bottom search bar, font size, system font, and themed icons.
- **Backup & Restore**: Import/export settings, plus automatic Android backup.

[View all features](app/src/main/assets/FEATURES.md)

## 🚀 Installation

### Requirements
- Android 7.0 (API 24) or higher

### Download
- **Google Play**: [Google Play Store](https://play.google.com/store/apps/details?id=com.tk.quicksearch) for automatic updates
- **F-Droid**: [F-Droid](https://f-droid.org/en/packages/com.tk.quicksearch/) for repository-based updates
- **APK**: Latest APK from the [Releases](https://github.com/teja2495/quick-search/releases) page

### Build from Source
```bash
git clone https://github.com/teja2495/quick-search.git
cd quick-search

# Google Play / GitHub releases (includes Play in-app review & updates)
./gradlew assembleStandardRelease

# F-Droid build (no Google Play libraries bundled)
./gradlew assembleFdroidRelease
```

See [docs/FDROID.md](docs/FDROID.md) for publishing to F-Droid.

## 🛡️ Permissions & Privacy

All permissions are optional and only unlock the features that need them. Search runs locally on your device. See the [Privacy Policy](PRIVACY_POLICY.md) for details.

- No ads or analytics
- Encrypted storage for API keys
- Granular permission controls
- Local-first data processing

## 🏗️ Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with `StateFlow` and unidirectional data flow
- **Persistence**: Modular SharedPreferences (encrypted for API keys) and Room for notes and notification history
- **Widgets**: Jetpack Glance
- **Build**: Gradle Kotlin DSL with version catalogs; `standard` and `fdroid` flavors; `:benchmark` module for baseline profiles
- **Min SDK**: 24 (Android 7.0) | **Target SDK**: 36 (Android 16)

### Project Structure
```
app/src/main/java/com/tk/quicksearch/
├── app/             # Activities (search, launcher), startup, app-level handlers
├── search/          # Search core, data/preferences, result types, search screen UI
├── searchEngines/   # Web search engines, aliases, secondary search
├── tools/           # Calculator, unit/date tools, alarms, AI Search, Tasker
├── settings/        # Settings screens and navigation
├── widgetsPanel/    # Widgets Panel and Home widgets
├── widgets/         # Glance home-screen widgets
├── reminders/       # Reminder parsing and scheduling
├── overlay/         # Draw-over-other-apps mode
├── edgeGesture/     # System-wide edge swipe launcher
├── floatingButton/  # Floating launch button
├── tile/            # Quick Settings tile
├── onboarding/      # First-launch setup
└── shared/          # Shared UI components, theme, and utilities
```

Contributors and coding agents: see [AGENTS.md](AGENTS.md) for the architecture map and conventions.

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 📞 Contact

- **Email**: tejakarlapudi.apps@gmail.com
- **Issues**: [GitHub Issues](https://github.com/teja2495/quick-search/issues)

---

**Made with ❤️ for Android**
