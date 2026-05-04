# Quick Search - Complete Feature Documentation

This document highlights all the available features. It also includes details about hidden or non-obvious features that you might miss.

**Version**: 3.4 (Updated May 4 2026)

### Search

- **App Search** - Find and launch applications instantly
- **App Shortcuts Search** - Find and launch app shortcuts instantly
- **Contact Search** - Quickly locate contacts in your phone and perform various actions for that contact, e.g. Call, SMS, Google Meet Voice/Video call, WhatsApp (Chat, Audio/Video Call), Telegram (Chat, Audio/Video Call), Signal (Chat, Audio/Video Call)
- **File Search** - Search through device files & folders
- **Calendar Events Search** - Search through your calendar events
- **Notes Search** - Write notes and search them quickly from the search bar. Swipe left from home for a quick note.
- **Device Settings Search** - Find specific device settings
- **App Settings Search** - Find and access Quick Search app settings directly by searching
- **App Suggestions** - Shows suggested apps when no search query is entered. You can also choose to hide app suggestions completely. This feature requires usage access permission. If permission is not granted, the app shows suggestions based on apps you recently opened from within the app.

---

### Internet Search

- **Multi-App Search** - Type a query and tap any "search on" card to quickly search using that search engine. If compact mode is enabled, you can just tap on any icon in the search engine section above your keyboard.
- **Browser Integration** - Installed browsers automatically appear as search engine options
- **In-App Browser** - Option to open URLs inside the app instead of an external browser
- **Supported Search Engines** (25 total): AI Search, Google, ChatGPT, Gemini, Perplexity, Grok, Google Maps, Google Drive, Google Photos, Google Play, YouTube, YouTube Music, Spotify, Reddit, Amazon, X (Twitter), Facebook Marketplace, Bing, DuckDuckGo, Brave, Startpage, You.com, AI Mode, Claude, Wikipedia
- **AI Search** - Configure your Gemini API key to see answers directly within Quick Search. 
- **Custom Search Engines** - Add your own search engines from the Search Engines settings screen.
- **Web Search Suggestions** - Web suggestions automatically appear when you type your query; you can disable them or change the count in settings.

---

### Tools

- **Calculator** - Type basic math expressions in the search bar to see results directly. Switching to the numeric keyboard while using the calculator displays calculator buttons for easy access. You can trigger calculator mode with an alias.
- **Unit Converter** - Convert units directly from the search bar example: 5 lbs in kgs. Supported conversions:
  - Length: m, km, cm, mm, µm (um), nm, mi, yd, ft, in, nmi
  - Mass: kg, g, mg, µg (ug), lb, oz, st, tonne (metric ton), US ton (short ton)
  - Temperature: °C, °F, K
  - Area: m², km², cm², mm², ft², in², yd², mi², acre, ha
  - Volume: L, mL, m³, cm³ (cc), mm³, ft³, in³, gal, qt, pt, cup, fl oz, tbsp, tsp
  - Time: s, ms, µs (us), ns, min, h, day, week, month, year
  - Speed: m/s, km/h, mph, kt, ft/s, in/s
  - Data: byte (B), bit, KB/MB/GB/TB, KiB/MiB/GiB/TiB
  - Energy: J, kJ, cal, kcal, Wh, kWh, eV, BTU
  - Power: W, kW, mW, hp
  - Pressure: Pa, kPa, MPa, bar, mbar, psi, atm, torr, mmHg
  - Angle: rad, deg, grad, rev
  - Frequency: Hz, kHz, MHz, GHz, rpm
- **Date & Time Calculator** - Parse natural language dates and times in the search bar. You can enable or disable the tool and set an optional alias (in Tools settings; tap the row for examples). Supported input types include:
  - **Named dates** — e.g. March 12 2025, July 4, Dec 25 2026
  - **Relative dates** — e.g. in 3 months, 2 weeks ago, in 1 year 6 months, 10 days ago
  - **Date differences** — e.g. March 5 to March 20, Jan 1 to Dec 31 2025
  - **Offset from a date** — e.g. 5 days from March 12, 3 months before June 1, 2 weeks after July 4
  - **Time arithmetic** — e.g. 6 hours from now, 45 minutes ago, 2 hours 30 minutes later
  - **Time ranges** — e.g. 9am to 5pm, 14:00 to 17:30, 8:30am to 12:00pm
  - **Absolute time** — e.g. 5pm, 14:30, 9am
  - **Time offset from a time** — e.g. 3 hours after 5pm, 30 minutes before 9am, 1 hour after 14:00
- **Dictionary** - Example usage: "define serendipity" or "serendipity meaning", to see search dictionary card, tapping on which will show dictionary result. This requires Gemini API key configuration.
- **World Clock** - Check the current time across different time zones. Search for city names to see their current time instantly.
- **Currency Converter** - Convert between currencies directly from the search bar. Example: "100 USD to EUR" to see the converted amount with the current exchange rate.
- **Custom AI Tools** - Create your own tools for AI Search. This feature requires an API key to be configured.

---

### Aliases

Add aliases at the start of your query to quickly let the app know what you want to do.

- **Search Section Aliases** - Configure aliases to instantly focus on specific result sections (apps, app shortcuts, contacts, files, device settings) from the search bar.
- **Search Engine Aliases** - Assign aliases (like `ggl` for Google) to quickly trigger any search engine just by typing your alias at the start or end of the query. (When you add an alias at the end of your query, hit space to trigger it.)
- **Calculator Alias** - Set a dedicated alias that switches the search bar into calculator mode on demand.
- **Date & Time Calculator Alias** - Set a dedicated alias that switches the search bar into date & time calculator mode on demand.
- When alias is detected for any search result type, but the query is empty, recently opened items for that particular search type will appear. For calendar events, upcoming events will be displayed.

---

### Search Algorithm

- **Smart Ranking System** - Results are ranked by relevance with multiple priority levels:
  - Exact matches (highest priority)
  - Names starting with query
  - Second word matches
  - Names containing query anywhere
- **Recently Opened Results** - Recently opened results are prioritized in the result order.
- **Apps** - Use the ranking above plus optional fuzzy matching, acronym-style shortcuts (for example matching initials to an app name), and nicknames you assign.
- **Device settings** and **Quick Search app settings** - Typo-tolerant fuzzy matching is used in the unified secondary search so small spelling mistakes still surface the right setting rows.
- **Contacts** and **files** - Also respect **nicknames** you add; using a **section alias** for contacts or files runs a deeper, section-focused search (see Hidden / Non-Obvious Features).

---

### Long-Press Actions

Long-press on most results to open a context menu with quick actions.

- **Triggers** - Long-press a result to add a trigger phrase. When you type that trigger and press space, Quick Search opens that result automatically.
- **Pin / Unpin** - Pin or unpin apps, app shortcuts, contacts, files, and device settings so that they stay on your home screen when you open the app.
- **Add / Edit Nickname** - Add or edit nicknames for apps, app shortcuts, contacts, files, and device settings so you can search them using custom names.
- **Exclude / Include** - Exclude individual contacts, app shortcuts, files, and device settings from results. You can include them again later by clearing them from Excluded Items list in Search Results settings.
- **Add to Home Screen** - Add apps, app shortcuts, contacts, files, and device settings directly to your home screen as shortcuts for one-tap access.
- **Edit Icons for App Shortcuts** - Option to add custom icons for app shortcuts.
- **App Info / Uninstall / Hide** - For apps, open system app info, uninstall supported apps, or hide them from suggestions/results using the long-press menu on the app icon.
- **File-Specific Options** - For files, long-press to share, open the containing folder, exclude all files with the same extension, or view a detailed file info dialog (type, size, path, last modified, etc.).

---

### Appearance Settings

- **Overlay Mode** - Enable to make the search bar appear over other apps anywhere in the system. This defines the main experience: search from any screen without leaving what you're doing.
- **System Font** - Option to use your device system font throughout the app for a native look and better consistency with your phone theme.
- **Font Size** - Choose between small, medium, and large font sizes for all in-app text.
- **Theme** - Set base theme to Light, Dark or System (follows system theme). Then, pick among the available themes - Mono, Forest, Aurora, Sunset - and adjust their intensity. These themes will adapt to light and dark mode seamlessly.
- **Material You** - Enable to use your device's color palette from Material You for the app's theme and accent colors.
- **Themed App Icons** - Enable or disable themed app icons for app results and for the Quick Search launcher icon.
- **Wallpaper Background** - Use your wallpaper or any custom image as your search screen background, you can also tune transparency and blur to keep content readable. Choose whether to derive accent colors from wallpaper.
- **Search Engine Style** - Choose between:
  - **Inline Mode**: Search engines scroll with the content
  - **Compact Mode**: Search engines stay fixed at the bottom of the screen above the keyboard for quick access, with support for one or two rows of engines.
- **One-Handed Mode** - Most relevant items appear at the bottom of your screen for easy access.
- **Bottom Searchbar** - You can choose to move the search bar to the bottom.
- **Apps Per Row** - Choose no. of apps displayed per row in results and suggestions.
- **App Labels** - You can choose to hide app labels.
- **Circular App icons** - Option to force circular app icons
- **Icon Packs** - Select an installed icon pack. If none of them are installed, tapping on the card will open the Play Store and search for icon packs. Once installed, come back to the app and hit the refresh button on the icon packs option to refresh the icon packs list.

---

### Search Results Settings

- **Search Sections Management** - Enable/disable individual result sections (apps, app shortcuts, contacts, files, device settings, history, etc.), and assign alias shortcuts to each section. Tap on the search type options to navigate to their respective screen, which offers additional options.
- **Top Matches** - Turn on Top Matches to show the best results for your query across enabled categories. You can include or exclude categories and reorder them to control priority.
- **App Suggestions** - You can choose to disable app suggestions if you want a cleaner home screen. 
- **Web Search Suggestions** - Turn web suggestions on or off and control how many suggestions (1–5) appear under the search bar.
- **Search History** - Enable or disable search history. The expanded list shows up to 15 items; **Clear all** history is available at the bottom of the list. Your search queries, recently opened items (files, contacts, etc.) appear in your search history.
- **Excluded Items** - When you exclude any item on the search results page, you can find them here and clear them if you want to.
- **Refresh Data** - The app automatically refreshes your data from time to time, but if you want to manually refresh the data, you can do it here.
- **Calendar Options** - Create custom calendar entries and choose to ignore past events in results. Today's events also appear on the home screen.

---

### Search Engine Settings

- **Enable / Disable Search Engines** - Turn individual search engines on or off and control the order in which they appear.
- **Aliases for Engines** - Assign or edit alias codes to search engines.
- **Alias after Query** - You can choose to disable search engine alias triggers at the end of your query. Also, choose whether space is required for the trigger.
- **Amazon Domain** - Select the Amazon region/domain used when searching via the Amazon engine. Updating this will open your Amazon queries within the installed app instead of the website.
- **Custom Search Engines** - Add your own search engines by specifying a URL template with the {{query}} placeholder. The app automatically fetches the name and icon for the website, but you can choose to edit them if needed. Tapping on {{query}} in the error hint will add this string to your URL so that you don't have to type it. You can choose any installed browser to open custom search engine queries.
- **AI Search** - Connect a Gemini API key to enable AI answers directly in Quick Search, view if a key is configured, and open the dedicated AI Search configuration screen. Gemini Flash Latest is the model which is chosen by default.
- **AI Providers** - Configure API keys for OpenAI, Claude, and Groq providers.

---

### Gemini API configuration

- **Gemini API Key** - Use the "Get Free API Key" button to see a guide on how to set up your Gemini API key for free. 
- **Personal Context** - Provide optional personal context so AI results can be better tailored to you (kept on-device within Quick Search).
- **Gemini Model Selection** - Choose among the available Gemini / Gemma models. Gemma models do not support Google search and personal context.
- **Grounding** - Enable or disable grounding with Google search as needed.

---

### Permissions Settings

- **Usage Access** - View and request the usage access permission used for app suggestions and usage‑based ranking.
- **Contacts Permission** - View and request contacts access, required for searching contacts and calls/texts.
- **Files / Storage Permission** - View and request file access to enable file search.
- **Phone Permission** - View and request phone calling permission, required for direct dial and some calling integrations.

---

### File Settings

- Can be accessed in Search Results settings page via Files option.
- **Show Folders** - Choose whether folders appear in search results.
- **File Types** - Enable or disable specific file categories (documents, pictures, videos, audio, APKs, other) to fine‑tune which files are searchable.
- **Excluded Extensions** - See and remove file extensions that are currently excluded from results.
- **System & Hidden Files** - Show or hide system files and hidden files from file search results.
- **Folder Filters (Whitelist / Blacklist)** - Configure which folders should be included or excluded for files search.

---

### Calls & Texts Settings

- Can be accessed in Search Result settings page via Contacts Option
- **Direct Dial** - Enable direct dialing from search results (tapping a phone result calls immediately instead of opening the dialer).
- **Default Calling App** - Choose which app to show up as the default calling action in contact result cards (supported apps when installed: Google Meet, WhatsApp, Telegram, Signal).
- **Default Messaging App** - Choose which app to show up as the default messaging action in the contact result cards (supported apps when installed: Google Meet, WhatsApp, Telegram, Signal).

---

### Apps Management Screen

- Can be accessed in Search Results settings page via Apps option.
- **Installed Apps List** - View all installed apps known to Quick Search.
- **Sorting Options** - Sort apps by name, APK size, most/least used (when usage access is granted), installation date, last update time, or target API level.
- **App Search & Selection** - Search within your installed apps, multi‑select them, and perform batch actions.
- **Batch Uninstall & App Info** - Uninstall multiple user apps in one flow and open the system App Info screen for any app, with a detailed info dialog (package name, version, SDK levels, install/update dates).

---

### App Shortcuts Management Screen

- This page can be accessed in Search Result settings page via App Shortcuts Option.
- **Shortcuts List** - View all app shortcuts grouped by app, search engine, or browser inside expandable cards.
- **Enable / Disable Shortcuts** - Turn individual shortcuts on or off while keeping them available on the device.
- **Search & Filters** - Search within shortcuts and filter by all apps, apps with shortcuts only, search engines, or browsers, with expand/collapse all controls.
- **Add Shortcuts from Apps** - Discover and add app-provided shortcuts directly from supported apps. (Example: Add Google Drive files as shortcuts to open them directly)
- **Create Search & Browser Shortcuts** - Create query or URL shortcuts for search engines and browsers—including the in-app browser—to instantly open them when needed.
- **Custom Deep Link Shortcuts** - Add, edit, or delete your own deep-link shortcuts that jump straight into specific screens inside apps. (Example: Add YouTube Music playlists as deep links to open them directly.)
- **Add App Activities as Shortcuts** - See and add available app activities as shortcuts. This is not a reliable feature; apps do not always provide useful activities, but when they do, you can add them here.

---

### More Options

- **Top Result Indicator Toggle** - Option to hide the top result indicator for top results that can be opened with the keyboard.
- **Open Keyboard Toggle** - Choose to disable automatic keyboard opening during app launch. This is useful if you want to use this app just to quickly access pinned items.
- **Clear Query** - The app clears your query by default when you reopen the app; you can choose to keep the query by disabling this toggle.
- **Auto-Close App** - Option to toggle auto-close app after any action

---

### Launch Options

- **Default Assistant** - Set Quick Search as your default assistant app to replace Google Assistant or Gemini. You can enable Assistant Voice Mode to open the app with voice typing enabled when triggered with digital assistant gesture.
- **Quick Settings Tile** - Add a Quick Search tile to notification quick settings for quick access

---

### Widgets

- **Search Widget** - Tap the widget to instantly open Quick Search. Highly customizable with mic button support and up to 2 custom buttons alongside the search functionality.
- **Custom Buttons Widget** - A dedicated widget for custom buttons - apps, app shortcuts, contacts, files, device settings, and notes - with enhanced customizable layouts and actions.

---

### Widget Customization

- **Layouts & Variants** - Choose between the main search widget and the custom buttons widget, with flexible button layouts for each.
- **Themes & Colors** - Adjust widget theme, background, and text/icon colors to match your home screen.
- **Border Color** - Customize the widget border color to better match your wallpaper and icon style.
- **Size & Spacing** - Control corner radius, internal padding, and spacing using intuitive sliders.
- **Icons & Buttons** - Toggle search and mic icons and configure up to 2 custom buttons on the search widget or a full set of custom buttons on the custom buttons widget.
- **Custom Button Icons** - Set custom icons for widget buttons to personalize how shortcuts appear on your home screen.
- The widget mic button can be customized to open other device assistants like Gemini, Perplexity, Bixby, etc.

---

### Hidden / Non-Obvious Features

- Swipe left to quickly write a note.
- **Search Icon tap behavior** - Tap on the Search icon in search bar to narrow down your search to a specific type. 
- **URL Detection in Search Bar** - Paste or type a URL in the search bar to open it quickly in your installed browsers of your choice.
- **App Search** - Acronym matching is supported for app search. For example, you can search for "yt" to find "YouTube."
- **AI Search**
  - If the results contain emails, phone numbers, or links, you can tap them to email, call, or open them.
  - You can quickly change the model by tapping on "Powered by" text in the result.
- **Keyboard Action Button** - Tap on keyboard action button to automatically open the top result
- **Show/Switch Keyboard Button** - When you close the keyboard, a small "Open Keyboard" button appears so that you don't have to tap on the search bar. When you type numbers in search, a "Switch to number" keyboard will appear. This is useful when using the calculator.
- **Alias-based Contact and File Searches** - When you trigger contact/files search using an alias, the app will search more deeply.
- **Search History Items** - Up to 15 items in the expanded list, with **Clear all** at the bottom. Long-press on any item to remove it.
- **Search Engines Long Press** - Long-press any search engine icon or card (inline or compact) to jump directly to the Search Engines settings screen for managing engines, order, aliases, and other options.
- **Contacts**
- **Contact Action Customization** - Long-press the call/message action buttons on a contact card to replace them with any action you like.
- **Add Contact Action to Home** - Open any contact and long-press on any of the actions (e.g. WhatsApp call, Telegram video, etc.) to add it to your home screen as a shortcut.
- **Contact Image Tap** - Tap on the contact photo to open the contact in the default contacts app to edit it.
- **Access Beta Features** - Tap on version number 5 times to unlock new beta features. These features are hidden because they're not fully implemented & tested, so expect bugs. You can disable these features by long-pressing on the version number.
- Long-press AI Search, Calculator or any tool result to copy it to clipboard.
- You can send text to Quick Search by selecting Quick Search in phone's text-selection menu and share sheet.
- Long press on any contact actions to add it to your launcher or add a trigger for it.

---

### Other Options

- **Backup & Restore Settings** - Import or export your app settings when switching devices or if you just want a backup of your settings. Even if you don't manually back up settings, the app will automatically back up and restore them using Android's built-in backup system.
- **Send Feedback** - Send feedback, bug reports, and feature requests to the developer at [tejakarlapudi.apps@gmail.com](mailto:tejakarlapudi.apps@gmail.com).
- **Development** - View this project's source code on GitHub.
- **Contact Developer** - Tap on the developer name below the version number to contact the developer.

---

### Note from the Developer

I've been an Android enthusiast my whole life, and it's been awesome working on this project. Because I have the privilege of a full-time job, I decided to keep this app completely free, ad-free, and open source — my small contribution to the Android community that's given me so much.

This is what I ask in return: if this app brings value to your life, share it with the people around you and consider leaving a 5-star review on the Play Store. That means a lot to me.

Your feedback is important — it's what shapes every update and improvement. So please, keep it coming. Thank you for downloading, using, and being a part of this journey.

-

[Teja Karlapudi](https://teja2495.github.io/teja-karlapudi-links/)
