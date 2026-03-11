# Quick Search - Complete Feature Documentation

This document highlights all the available features. It also includes details about hidden or non-obvious features that you might miss.

**Version**: 2.7 (Updated March 11 2026)

### Search

- **App Search** - Find and launch applications instantly
- **App Shortcuts Search** - Find and launch app shortcuts instantly
- **Contact Search** - Quickly locate contacts in your phone and perform various actions for that contact, e.g. Call, SMS, Google Meet Voice/Video call, WhatsApp (Chat, Audio/Video Call), Telegram (Chat, Audio/Video Call), Signal (Chat, Audio/Video Call)
- **File Search** - Search through device files & folders
- **Device Settings Search** - Find specific device settings
- **App Suggestions** - Shows suggested apps when no search query is entered. You can also choose to hide app suggestions completely. This feature requires usage access permission. If permission is not granted, the app shows suggestions based on apps you recently opened from within the app.

### Internet Search

- **Multi-App Search** - Type a query and tap any "search on" card to quickly search using that search engine. If compact mode is enabled, you can just tap on any icon in the search engine section above your keyboard.
- **Browser Integration** - Installed browsers automatically appear as search engine options
- **Supported Search Engines** (25 total): Direct Search, Google, ChatGPT, Gemini, Perplexity, Grok, Google Maps, Google Drive, Google Photos, Google Play, YouTube, YouTube Music, Spotify, Reddit, Amazon, X (Twitter), Facebook Marketplace, Bing, DuckDuckGo, Brave, Startpage, You.com, AI Mode, Claude, Wikipedia
- **Direct Search (AI-Powered)** - Configure your Gemini API key to see answers directly within Quick Search. 
- **Custom Search Engines** - Add your own search engines from the Search Engines settings screen.
- **Web Search Suggestions** - Web suggestions automatically appear when you type your query; you can disable them or change the count in settings.

### Tools

- **Calculator** - Type basic math expressions in the search bar to see results directly. Switching to the numeric keyboard while using the calculator displays calculator buttons for easy access. You can trigger calculator mode with an alias.

### Search Algorithm

- **Smart Ranking System** - Results are ranked by relevance with multiple priority levels:
  - Exact matches (highest priority)
  - Names starting with query
  - Second word matches
  - Names containing query anywhere
- **Recently Opened Results** - Recently opened results are prioritized in the result order.
- **App and App Shortcut Searches** - These searches support fuzzy search to ignore typos.

### Long-Press Actions

Long-press on most results to open a context menu with quick actions.

- **Pin / Unpin** - Pin or unpin apps, app shortcuts, contacts, files, and device settings so that they stay on your home screen when you open the app.
- **Add / Edit Nickname** - Add or edit nicknames for apps, app shortcuts, contacts, files, and device settings so you can search them using custom names.
- **Exclude / Include** - Exclude individual contacts, app shortcuts, files, and device settings from results. You can include them again later by clearing them from Excluded Items list in Search Results settings.
- **Add to Home Screen** - Add apps, app shortcuts, contacts, files, and device settings directly to your home screen as shortcuts for one-tap access.
- **App Info / Uninstall / Hide** - For apps, open system app info, uninstall supported apps, or hide them from suggestions/results using the long-press menu on the app icon.
- **File-Specific Options** - For files, long-press to open the containing folder, exclude all files with the same extension, or view a detailed file info dialog (type, size, path, last modified, etc.).


### Settings

#### Appearance

- **Overlay Mode** - Enable to make the search bar appear over other apps anywhere in the system. This defines the main experience: search from any screen without leaving what you're doing.
- **Font Size** - Choose between small, medium, and large font sizes for all in-app text.
- **Background Theme** - Pick among the available themes - Mono, Forest, Aurora, Sunset - and adjust their intensity. You can also choose your wallpaper or any custom image as your background theme. When using wallpaper/custom images, you can also tune transparency and blur to keep content readable.
- **Search Engine Style** - Choose between:
  - **Inline Mode**: Search engines scroll with the content
  - **Compact Mode**: Search engines stay fixed at the bottom of the screen above the keyboard for quick access, with support for one or two rows of engines.
- **One-Handed Mode** - Most relevant items appear at the bottom of your screen for easy access.
- **Bottom Searchbar** - You can choose to move the search bar to the bottom.
- **App Labels** - You can choose to hide app labels.
- **Icon Packs** - Select an installed icon pack. If none of them are installed, tapping on the card will open the Play Store and search for icon packs. Once installed, come back to the app and hit the refresh button on the icon packs option to refresh the icon packs list.

#### Search Results

- **Search Sections Management** - Enable/disable individual result sections (apps, app shortcuts, contacts, files, device settings, history, etc.), and assign alias shortcuts to each section. Tap on the search type options to navigate to their respective screen, which offers additional options.
- **App Suggestions** - You can choose to disable app suggestions if you want a cleaner home screen. 
- **Web Search Suggestions** - Turn web suggestions on or off and control how many suggestions (1–5) appear under the search bar.
- **Search History** - Enable or disable search history. Your search queries and recently opened items (files, contacts, etc.) appear in your search history.
- **Excluded Items** - When you exclude any item on the search results page, you can find them here and clear them if you want to.
- **Open Keyboard Toggle** - Choose to disable automatic keyboard opening during app launch. This is useful if you want to use this app just to quickly access pinned items.
- **Clear Query** - The app clears your query by default when you reopen the app; you can choose to keep the query by disabling this toggle.
- **Refresh Data** - The app automatically refreshes your data from time to time, but if you want to manually refresh the data, you can do it here.

#### Search Engines

- **Enable / Disable Search Engines** - Turn individual search engines on or off and control the order in which they appear.
- **Aliases for Engines** - Assign or edit alias codes to search engines.
- **Alias after Query** - You can choose to disable search engine alias triggers at the end of your query.
- **Amazon Domain** - Select the Amazon region/domain used when searching via the Amazon engine. Updating this will open your Amazon queries within the installed app instead of the website.
- **Custom Search Engines** - Add your own search engines by specifying a URL template with the {{query}} placeholder. The app automatically fetches the name and icon for the website, but you can choose to edit them if needed. Tapping on {{query}} in the error hint will add this string to your URL so that you don't have to type it.
- **Direct Search (AI)** - Connect a Gemini API key to enable AI answers directly in Quick Search, view if a key is configured, and open the dedicated Direct Search configuration screen.

#### Direct Search Configuration

- **Gemini API Key** - Use the "Get Free API Key" button to see a guide on how to set up your Gemini API key for free. 
- **Personal Context** - Provide optional personal context so AI results can be better tailored to you (kept on-device within Quick Search).
- **Gemini Model Selection** - Choose among the available Gemini / Gemma models. Gemma models do not support Google search and personal context.
- **Grounding** - Enable or disable grounding with Google search as needed.

### Aliases

Add aliases at the start of your query to quickly let the app know what you want to do.

- **Search Section Aliases** - Configure aliases to instantly focus on specific result sections (apps, app shortcuts, contacts, files, device settings) from the search bar.
- **Search Engine Aliases** - Assign aliases (like `ggl` for Google) to quickly trigger any search engine just by typing your alias at the start or end of the query. (When you add an alias at the end of your query, hit space to trigger it.)
- **Calculator Alias** - Set a dedicated alias that switches the search bar into calculator mode on demand.

#### Permissions

- **Usage Access** - View and request the usage access permission used for app suggestions and usage‑based ranking.
- **Contacts Permission** - View and request contacts access, required for searching contacts and calls/texts.
- **Files / Storage Permission** - View and request file access to enable file search.
- **Phone Permission** - View and request phone calling permission, required for direct dial and some calling integrations.

#### File Settings

- Can be accessed in Search Results settings page via Files option.
- **Show Folders** - Choose whether folders appear in search results.
- **File Types** - Enable or disable specific file categories (documents, pictures, videos, audio, APKs, other) to fine‑tune which files are searchable.
- **Excluded Extensions** - See and remove file extensions that are currently excluded from results.
- **System & Hidden Files** - Show or hide system files and hidden files from file search results.
- **Folder Filters (Whitelist / Blacklist)** - Configure which folder paths should always be included or excluded using simple comma‑separated patterns.

#### Calls & Texts Settings

- Can be accessed in Search Result settings page via Contacts Option
- **Direct Dial** - Enable direct dialing from search results (tapping a phone result calls immediately instead of opening the dialer).
- **Default Calling App** - Choose which app to show up as the default calling action in contact result cards (supported apps when installed: Google Meet, WhatsApp, Telegram, Signal).
- **Default Messaging App** - Choose which app to show up as the default messaging action in the contact result cards (supported apps when installed: Google Meet, WhatsApp, Telegram, Signal).

#### Apps Management Screen

- Can be accessed in Search Results settings page via Apps option.
- **Installed Apps List** - View all installed apps known to Quick Search.
- **Sorting Options** - Sort apps by name, APK size, most/least used (when usage access is granted), installation date, last update time, or target API level.
- **App Search & Selection** - Search within your installed apps, multi‑select them, and perform batch actions.
- **Batch Uninstall & App Info** - Uninstall multiple user apps in one flow and open the system App Info screen for any app, with a detailed info dialog (package name, version, SDK levels, install/update dates).

#### App Shortcuts Management Screen

- This page can be accessed in Search Result settings page via App Shortcuts Option.
- **Shortcuts List** - View all app shortcuts grouped by app, search engine, or browser inside expandable cards.
- **Enable / Disable Shortcuts** - Turn individual shortcuts on or off while keeping them available on the device.
- **Search & Filters** - Search within shortcuts and filter by all apps, apps with shortcuts only, search engines, or browsers, with expand/collapse all controls.
- **Add Shortcuts from Apps** - Discover and add app-provided shortcuts directly from supported apps. (Example: Add Google Drive files as shortcuts to open them directly)
- **Create Search & Browser Shortcuts** - Create query or URL shortcuts for search engines and browsers to instantly open them when needed.
- **Custom Deep Link Shortcuts** - Add, edit, or delete your own deep-link shortcuts that jump straight into specific screens inside apps. (Example: Add YouTube Music playlists as deep links to open them directly.)
- **Add App Activities as Shortcuts** - See and add available app activities as shortcuts. This is not a reliable feature; apps do not always provide useful activities, but when they do, you can add them here.

### Launch Options

- **Default Assistant** - Set Quick Search as your default assistant app to replace Google Assistant or Gemini. You can enable Assistant Voice Mode to open the app with voice typing enabled when triggered with digital assistant gesture.
- **Quick Settings Tile** - Add a Quick Search tile to notification quick settings for quick access

### Widgets

- **Search Widget** - Tap the widget to instantly open Quick Search. Highly customizable with mic button support and up to 2 custom buttons alongside the search functionality.
- **Custom Buttons Widget** - A dedicated widget for custom buttons - apps, app shortcuts, contacts, files, device settings - with enhanced customizable layouts and actions.

### Widget Customization

- **Layouts & Variants** - Choose between the main search widget and the custom buttons widget, with flexible button layouts for each.
- **Themes & Colors** - Adjust widget theme, background, and text/icon colors to match your home screen.
- **Size & Spacing** - Control corner radius, internal padding, and spacing using intuitive sliders.
- **Icons & Buttons** - Toggle search and mic icons and configure up to 2 custom buttons on the search widget or a full set of custom buttons on the custom buttons widget.
- The widget mic button can be customized to open other device assistants like Gemini, Perplexity, Bixby, etc.

### Hidden / Non-Obvious Features

- **URL Detection in Search Bar** - Paste or type a URL in the search bar to open it quickly in your installed browsers of your choice.
- **App Search** - Acronym matching is supported for app search. For example, you can search for "yt" to find "YouTube."
- **Direct Search (Gemini API)**
  - If the results contain emails, phone numbers, or links, you can tap them to email, call, or open them.
  - You can quickly change the model by tapping on "Powered by" text in the result.
- **Show/Switch Keyboard Button** - When you close the keyboard, a small "Open Keyboard" button appears so that you don't have to tap on the search bar. When you type numbers in search, a "Switch to number" keyboard will appear. This is useful when using the calculator.
- **Alias-based Contact and File Searches** - When you trigger contact/files search using an alias, the app will search more deeply.
- **Search History Items** - Long-press on any search history item to remove it from your search history.
- **Search Engines Long Press** - Long-press any search engine icon or card (inline or compact) to jump directly to the Search Engines settings screen for managing engines, order, aliases, and other options.
- **Contacts**
- **Contact Action Customization** - Long-press the call/message action buttons on a contact card to replace them with any action you like.
- **Add Contact Action to Home** - Open any contact and long-press on any of the actions (e.g. WhatsApp call, Telegram video, etc.) to add it to your home screen as a shortcut.
- **Contact Image Tap** - Tap on the contact photo to open the contact in the default contacts app to edit it.


