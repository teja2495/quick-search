# Quick Search - Complete Feature Documentation

## Core Search Capabilities

### Search Functions
- **App Search** - Find and launch applications instantly
- **Contact Search** - Quickly locate contacts in your phone
- **File Search** - Search through device files
- **Settings Search** - Find specific device settings
- **App Suggestions** - Displays suggested apps when no search query is entered
- **Built-in Calculator** - Type basic math expressions in the search bar to see results directly

### Internet Search
- **Multi-App Search** - Type a query and tap any search engine icon (located above the keyboard) to search within that specific app
- **Horizontal Scroll** - Swipe horizontally to access additional search engines
- **Customize Search Engines** - Long press any search engine to reorder, enable, or disable it
- **Search Engine Shortcuts** - Add a search engine shortcut at the start of your query to quickly search with that search engine
- **Supported Search Engines**:
  - **Direct Search** (dsh) - AI-powered answers using Gemini API
  - **Google** (ggl) - Default web search
  - **ChatGPT** (cgpt) - OpenAI's conversational AI
  - **Gemini** (gmi) - Google's AI assistant
  - **Perplexity** (ppx) - AI-powered search with citations
  - **Grok** (grk) - xAI's AI assistant
  - **Google Maps** (mps) - Location and directions
  - **Google Drive** (gdr) - File search in Google Drive
  - **Google Photos** (gph) - Photo search and organization
  - **Google Play** (gpl) - Android app search
  - **YouTube** (ytb) - Video search and streaming
  - **YouTube Music** (ytm) - Music streaming and discovery
  - **Spotify** (sfy) - Music and podcast streaming
  - **Reddit** (rdt) - Community discussions and content
  - **Amazon** (amz) - E-commerce shopping
  - **X (Twitter)** (twt) - Social media platform
  - **Facebook Marketplace** (fbm) - Local buying and selling
  - **Bing** (bng) - Microsoft's search engine
  - **DuckDuckGo** (ddg) - Privacy-focused search
  - **Brave** (brv) - Privacy-focused browser search
  - **Startpage** (stp) - Privacy-focused search engine
  - **You.com** (yu) - AI-powered search platform
  - **AI Mode** (gai) - Google's AI-focused search
- **Web Search Suggestions** - Automatically appear when your query has no app, contact, file, or calculator results (choose how many to display)
- **Search Engine Style** - Choose between:
  - **Inline Mode**: Search engines scroll with the content
  - **Compact Mode**: Search engines stay fixed at the bottom of the screen for quick access

### Direct Search (Gemini API)
- **AI-Powered Answers** - Configure your Gemini API key to see answers directly within Quick Search
- **Personal Context** - Optionally add personal information to customize AI responses (e.g., location, preferences)
- **Easy Setup** - Tap "Get Free API Key" in settings for setup instructions
- **Interactive Results** - Tap phone numbers to call or email addresses to send emails directly from search results
- **Secure Storage** - API keys are encrypted using Android's EncryptedSharedPreferences

## Customization & Settings

### Search Result Management
- **Reorder Results** - Arrange search result categories according to your preference
- **Enable/Disable Categories** - Toggle individual search result types on or off
- **Auto-Expand Results** - Show all results at once without tapping "More" button (This option is available in additional settings)
- **Sort Options** - Results are ordered by most-opened apps by default (can be disabled)
- **Recent Searches** - Show your recent search queries when the search bar is empty (choose how many to display, default is 3)
- **Delete Recent Queries** - Remove individual recent queries with a tap

### Visual Customization
- **Wallpaper Integration** - Your default phone wallpaper automatically sets as the app's background (it needs files permission and can be disabled in settings)
- **Icon Pack Support** - Apply 3rd party icon packs to app suggestions and results
- **One-Hand Mode** - Position search results at the bottom of the screen for easier reach (bottom-aligned layout)
- **Section Titles** - Show/hide section headers (Apps, Contacts, Files, Settings)
- **App Labels** - Toggle visibility of app names in suggestions

### Contact Features
- **Comprehensive Contact Actions** - Tap any contact result to access all available options:
  - Call, SMS, Google Meet
  - WhatsApp: chat, audio call, video call
  - Telegram: chat, audio call, video call
- **Default Messaging App** - Set your preferred messaging app (Messages/WhatsApp/Telegram). The default messaging app will appear for contact results.
- **Direct Calling Toggle** - Enable to call immediately, or disable to open dialer first
- **Multiple Numbers** - When you open a contact result which has multiple phone numbers, tap the arrow beside the phone number to access other numbers. The app will remember the last opened number and opens it first when you reopen the contact.
- **Preferred Number Memory** - The app remembers your preferred number for each contact

### File Management
- **File Type Filtering** - Ignore specific categories in settings:
  - Photos & Videos
  - Documents
  - All other file types
- **Custom Exclusions** - Long press any file result for example, .pdf and tap "Exclude .pdf files" to ignore that file type

### Result Management
- **Exclude Items** - Long press any item and tap "Exclude" to remove from suggestions or results
  - App suggestions and app results are treated as separate exclusion categories
- **Manage Exclusions** - View and manage all excluded items in settings
- **Pin Results** - Long press any result (app, contact, file, or setting) to pin it to the search home screen for quick access
- **Nicknames** - Long press any app to add a nickname, then search using that nickname in the future
- **App Management** - Long press apps to view app info or uninstall

### Behavior Settings
- **Clear Query Toggle** - Automatically clear search query after performing an action
  - App and settings results: always cleared
  - Contact results: never cleared
  - Other results: depends on your setting
- **Permission Management** - All permissions are optional and can be granted later through settings
- **Refresh Data** - Manually refresh apps, contacts, and file data
- **Haptic Feedback** - Context-aware vibration feedback for different interactions (confirm, toggle, strong)

## System Integration

### Assistant & Quick Access
- **Default Assistant** - Set Quick Search as your default assistant app to replace Google Assistant or Gemini
- **Quick Settings Tile** - Add a Quick Search tile to notification quick settings for instant access

### First-Launch Experience
- **Onboarding Flow** - Guided setup for permissions, search engines, and preferences
- **Search Engine Configuration** - Customize your preferred search engines during setup
- **Messaging Preferences** - Set your default messaging app (Messages/WhatsApp/Telegram) during setup
- **File Type Selection** - Choose which file types to include in search results during setup

## Home Screen Widget

### Widget Functionality
- Tap the widget to instantly open Quick Search
- Customize after adding by long pressing and tapping the settings button

### Widget Customization Options
- **Background Color** - Choose black or white
- **Corner Radius** - Adjust roundness of corners
- **Border Thickness** - Modify border width
- **Transparency** - Control widget opacity
- **Element Visibility** - Show/hide text, search icon, and mic icon
- **Color Options** - Set text, search icon, and mic icon to white or black
- **Icon Alignment** - Position search icon left or center
- **Mic Icon Action** - Choose what happens when tapping the mic:
  - Default: Opens Quick Search with voice typing enabled
  - Digital Assistant: Triggers device's digital assistant (Note: Your default assistant will be triggered)

## App Updates & Feedback

### In-App Updates
- **Automatic Update Detection** - App checks for updates from Google Play Store
- **Update Prompts** - Smart prompts based on usage patterns
- **Seamless Installation** - Update without leaving the app

### In-App Reviews
- **Smart Review Prompts** - Non-intrusive prompts to rate the app after positive usage patterns
- **One-Tap Rating** - Quick access to Play Store rating
- **Usage-Based Timing** - Review prompts appear at appropriate times

### Release Notes
- **What's New** - View release notes for each app version
- **In-App Display** - Release notes shown directly within the app
- **Version Information** - Clear indication of current version and changes

## Support & Development

- **Send Feedback** - Submit feedback, bug reports, and feature requests via email from app settings
- **Rate App** - Rate Quick Search on the Play Store with in-app review prompts
- **View Source Code** - Access the Quick Search project's open-source code on GitHub
- **Version Information** - View app version number at the bottom of settings
- **Contact Developer** - Tap the developer name below the version number to get in touch