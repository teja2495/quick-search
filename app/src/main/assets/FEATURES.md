# Quick Search - Complete Feature Documentation

This document highlights all the available features. It also includes details about hidden or non-obvious features that you might miss. Sections from **Overlay Mode** onward follow the order of the Settings screen (Tools settings are covered in the **Tools** section above).

**Version**: 4.6 (Updated September 21, 2026)

### Search

- **App Search** - Find and launch apps instantly.
- **App Shortcuts Search** - Find and launch app shortcuts instantly.
- **Contact Search** - Find contacts and act on them right away: call, SMS, Google Meet (voice/video call), or WhatsApp, Telegram, and Signal (chat, voice/video call).
- **File Search** - Search files and folders on your device.
- **Calendar Events Search** - Search your calendar events.
- **Reminders Search** - Create and search reminders directly from the search bar. For example, search `Laundry in 2 hrs`.
- **Notes Search** - Write notes and search them from the search bar. Quick Note is also available in the Widgets Panel.
- **Notification History** - Search notifications you've received, or open the full Notification History screen, where you can filter by app, hide an app's notifications, or clear them all. Requires notification access; only notifications received after access is granted are saved.
- **Device Settings Search** - Jump straight to specific device settings.
- **App Settings Search** - Find any Quick Search setting by searching for it.
- **App Suggestions** - When the search bar is empty, suggested apps appear. Swipe the row to switch between **New & Updated**, **Pinned**, and **Most Used**. Drag one pinned app onto another to create a folder. Works best with usage access; without it, suggestions are based on apps you opened from Quick Search.
- **View All Apps** - Browse all installed apps from a button below app suggestions. Turn on **All Apps** in More Options.
- **Screen Time** - Search `screen time` to see today's phone usage. Long-press an app to see its screen time.

---

### Internet Search

- **Multi-App Search** - Type a query, then tap any search engine card to search there. In compact mode, tap an engine icon in the bar above the keyboard.
- **Browser Integration** - Installed browsers automatically appear as search engines.
- **In-App Browser** - Open links inside Quick Search instead of an external browser.
- **Supported Search Engines** (31 total): AI Search, Google, ChatGPT, Gemini, Perplexity, Grok, Google Maps, Waze, Google Drive, Google Photos, Google Play, Google Translate, Kagi, Kagi Assistant, Muse, YouTube, YouTube Music, Spotify, Reddit, Amazon, X (Twitter), Facebook Marketplace, Bing, DuckDuckGo, Brave, Startpage, You.com, AI Mode, Claude, Wikipedia, F-Droid. Waze, Kagi Assistant, and Muse require their apps to be installed.
- **AI Search** - Get AI answers directly in Quick Search once an AI provider is set up, and ask follow-up questions to continue the conversation.
- **Web Search Suggestions** - Suggestions appear as you type. Turn them off or change how many appear in Search Results settings.

---

### Tools

- **Calculator** - Type a math expression to see the result instantly. Switch to the number keyboard to get calculator buttons.
- **Unit Converter** - Convert units directly from the search bar, e.g. `5 lbs in kgs`. Supported conversions:
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
- **Date & Time Calculator** - Calculate dates and times using natural language. Turn it on or off in Tools settings, where you can also tap the row for examples. Supported inputs:
  - **Named dates** — e.g. March 12 2025, July 4, Dec 25 2026
  - **Relative dates** — e.g. in 3 months, 2 weeks ago, in 1 year 6 months, 10 days ago
  - **Date differences** — e.g. March 5 to March 20, Jan 1 to Dec 31 2025
  - **Offset from a date** — e.g. 5 days from March 12, 3 months before June 1, 2 weeks after July 4
  - **Time arithmetic** — e.g. 6 hours from now, 45 minutes ago, 2 hours 30 minutes later
  - **Time ranges** — e.g. 9am to 5pm, 14:00 to 17:30, 8:30am to 12:00pm
  - **Absolute time** — e.g. 5pm, 14:30, 9am
  - **Time offset from a time** — e.g. 3 hours after 5pm, 30 minutes before 9am, 1 hour after 14:00
- **Alarms & Timers** - Set alarms and start timers directly from search. For example, search `2:45pm` to set an alarm or `15 min` to start a timer.
- **Dictionary** - Search `define serendipity` or `serendipity meaning` to see a dictionary card; tap it to view the definition. Requires an AI provider API key.
- **World Clock** - Search a city name to see its current time. Requires an AI provider API key.
- **Currency Converter** - Convert currencies at the current exchange rate, e.g. `100 USD to EUR`. No API key needed.
- **Weather** - See the current weather for a location directly in search. Requires an AI provider API key and a model with web search (or a Tavily API key).
- **Color Visualizer** - Preview a color from its hex code (e.g. `#FF0000`) or RGB value (e.g. `rgb(255, 0, 0)`).
- **Custom AI Tools** - Create your own AI Search tools with a custom system prompt. Add `{time}` to the prompt to include the current time. Requires an AI provider API key.
- **Tasker Integration** - Create Tasker actions using broadcast intents and trigger them from Quick Search with aliases; your query is sent to Tasker as an extra. You can also add Tasker tasks from the App Shortcuts page and launch them from search.

---

### Aliases

Type an alias before your query to tell Quick Search what you want to do.

- **Search Section Aliases** - Search only one section, such as apps, app shortcuts, contacts, files, or device settings. For contacts and files, this also runs a deeper search.
- **Search Engine Aliases** - Search a specific engine by typing its alias (e.g. `ggl` for Google). Aliases also work at the end of a query; press space to trigger them.
- **Tool Aliases** - Set aliases to switch the search bar into a tool's mode, such as calculator, date & time calculator, currency converter, dictionary, or weather (`wtr` by default).
- **Empty Alias** - Type just an alias to see recently opened items for that section (upcoming events for calendar).

---

### Search Algorithm

- **Smart Ranking System** - Results are ranked by relevance with multiple priority levels:
  - Exact matches (highest priority)
  - Names starting with query
  - Second word matches
  - Names containing query anywhere
- **Recently Opened Results** - Items you opened recently rank higher. You can switch this to most opened in Search Results settings.
- **Apps** - Also match with typos (fuzzy search), initials (e.g. `yt` for YouTube), and nicknames you add.
- **Device and app settings** - Small spelling mistakes still find the right setting.
- **Contacts and files** - Also match nicknames you add.

---

### Long-Press Actions

Long-press on most results to open a context menu with quick actions.

- **Triggers** - Add a trigger phrase to a result. Type the trigger and press space to open that result instantly.
- **Pin / Unpin** - Pin apps, app shortcuts, contacts, files, and device settings to Home. Drag pinned items to reorder them, or long-press and tap **Move Up** / **Move Down**. Tap a pinned section header to expand or collapse it.
- **Pin to Notification Panel** - Pin a result to the notification panel, either grouped with other items or in its own notification.
- **Nicknames** - Give apps, app shortcuts, contacts, files, and device settings one or more custom names to search by.
- **Exclude** - Hide contacts, app shortcuts, files, and device settings from results. Bring them back from **Hidden Items** in Search Results settings.
- **Add to Home Screen** - Add apps, app shortcuts, contacts, files, and device settings to your home screen as shortcuts.
- **Edit Icons for App Shortcuts** - Set a custom icon for an app shortcut.
- **App Info / Uninstall / Hide** - Open an app's system info, uninstall it, or hide it from suggestions and results.
- **Lock / Unlock App** - Lock an app with biometrics (available when Quick Search is your launcher).
- **Open in Split Screen** - Open an app in split screen.
- **SpeedBump** - Add a short delay before an app opens, to help break habitual app opening.
- **Copy Content** - Copy a note's text to the clipboard.
- **File Options** - Share a file, open its folder, exclude all files with the same extension, or view file details (type, size, path, last modified).

---

### Launcher Features

Available when Quick Search is set as your launcher.

- **Swipe Down for Notifications** - By default, swipe down on Home to open the notification shade.
- **Swipe Up for Keyboard** - Swipe up on Home to bring up the keyboard, ready to type.
- **Home Long-Press Menu** - Long-press an empty area on Home to change your wallpaper, open Gesture settings or Settings, or add a widget to Home.
- **More App Shortcuts** - More app shortcuts are available from the system.
- **App Lock** - Lock apps with biometrics from their long-press menu. If biometrics fail, long-press the unlock button for 4 seconds for other options, such as PIN.

---

### Widgets

- **Widgets Panel** - A side panel inside Quick Search for your home-screen widgets and Quick Note. You don't need to set Quick Search as your launcher; open it with a swipe gesture.
- **Add Widgets to Home** - Long-press a widget in the Widgets Panel to pin it to Home, or long-press an empty area on Home and choose **Widgets** from the popup menu. Long-press a widget on Home to move it above or below other Home items, resize it, or remove it.
- **Search Widget** - Tap to open Quick Search. Supports a mic button and up to 2 custom buttons.
- **Custom Buttons Widget** - A row of buttons for apps, app shortcuts, contacts, files, device settings, and notes, with customizable layouts and actions.
- **Media Controls Widget** - Shows the current track and album art, with play/pause, previous/next, and rewind/forward controls. Requires notification access.

---

### Widget Customization

- **Layouts** - Choose a button layout for each widget.
- **Themes & Colors** - Adjust the widget theme, background, and text and icon colors.
- **Border Color** - Change the widget border color.
- **Size & Spacing** - Adjust corner radius, padding, and spacing.
- **Icons & Buttons** - Show or hide the search and mic icons. Add up to 2 custom buttons on the search widget, or a full set on the custom buttons widget.
- **Icon Size** - Make widget icons larger.
- **Custom Button Icons** - Set a custom icon for any widget button.
- **Media Controls** - Add media control buttons to the custom buttons widget.
- **Folder Icon Color** - Change the color of folder icons on custom buttons.
- **Mic Button** - Make the mic button open another assistant, such as Gemini, Perplexity, or Bixby.

---

### App Icon Shortcuts

- Long-press the Quick Search app icon to start **Voice Search** or open **Manage Apps**, **Widgets**, or **Notification History**.

---

### Hidden / Non-Obvious Features

- **Reorder Quick Note** - Hold and drag the Quick Note header to reorder it in the Widgets Panel.
- **Search Icon** - Tap the search icon in the search bar to limit your search to one type of result.
- **Open URLs** - Paste or type a URL in the search bar to open it in the browser of your choice.
- **AI Search**
  - Tap emails, phone numbers, or links in an answer to email, call, or open them.
  - Tap the "Powered by" text in a result to quickly switch models.
- **Physical Keyboard Shortcuts** - Navigate results, open items, and perform actions with a physical keyboard.
- **Keyboard Buttons** - When the keyboard is closed, tap the small **Open Keyboard** button to reopen it. When you type numbers, a **Switch to number** button appears, handy for the calculator.
- **Remove History Items** - Long-press any search history item to remove it.
- **Search Engine Long-Press** - Long-press any search engine icon or card (inline or compact) to jump to Search Engines settings, where you can manage engines, order, and aliases.
- **Contacts**
  - **Customize Actions** - Long-press the call or message button on a contact card to replace it with any action.
  - **Action Shortcuts & Triggers** - Open a contact and long-press any action (e.g. WhatsApp call) to add it to your home screen or give it a trigger.
  - **Edit Contact** - Tap the contact photo to edit the contact in your contacts app.
- **Beta Features** - Tap the version number 5 times to unlock beta features. They aren't fully tested, so expect bugs. Tap it 5 times again to turn them off.
- **Send Crash Log** - Long-press the version number to send feedback with the crash log attached.
- **Copy Results** - Long-press an AI Search, calculator, or other tool result to copy it.
- **Send Text to Quick Search** - Choose Quick Search from the text-selection menu or share sheet to search the selected text.

---

### Overlay Mode

- **Overlay Mode** - Show the search bar over whatever app you're using, so you can search without leaving it.

---

### Appearance Settings

- **System Font** - Use your device's system font throughout the app.
- **Font Size** - Choose small, medium, or big text.
- **Theme** - Choose Light, Dark, or System, then pick a theme (Mono, Forest, Aurora, Sunset) and adjust its intensity. Themes adapt to light and dark mode.
- **AMOLED / True Black** - With Dark Mono selected, use a true-black background instead of dark gray.
- **Material You** - Use your device's Material You colors for the app theme and accents.
- **Accent Color** - Pick a custom accent color, take it from your wallpaper, or turn accent coloring off.
- **Themed App Icons** - Use themed icons for app results and the Quick Search launcher icon.
- **Wallpaper Background** - Use your wallpaper or any image as the background, and adjust transparency and blur to keep content readable.
- **One-Handed Mode** - Show the most relevant items at the bottom of the screen, within easy reach.
- **Bottom Searchbar** - Move the search bar to the bottom of the screen.
- **Search Hints** - Show or hide the rotating hints in the search bar.
- **Settings Icon** - Show or hide the settings icon in the search bar.
- **Pinned Sections Order** - Reorder pinned sections on Home.
- **Pinned App Shortcuts in App Grid** - Show pinned app shortcuts in the app grid alongside apps.
- **Unified Pinned Items** - Show all pinned items in one list on Home instead of separate sections. Off by default.
- **Apps Per Row** - Choose how many apps appear per row in results and suggestions.
- **App Icon Size** - Adjust the size of app icons in results and suggestions.
- **App Labels** - Show or hide app names under icons.
- **Home Text Colors** - Customize text colors on Home, including app labels and section titles.
- **Circular App Icons** - Force all app icons to be circular.
- **Icon Packs** - Apply an installed icon pack. If you have none, tap the card to find one on the Play Store, then tap refresh once it's installed.
- **Individual App Icons** - Change a single app's icon from its long-press menu (requires an icon pack).
- **Launcher Icon** - Choose a different icon for Quick Search itself, and reset it anytime.

---

### Search Results Settings

- **Search Sections** - Turn result sections (apps, contacts, files, and more) on or off and set their aliases. Tap a section to open its own settings.
- **Top Matches** - Show the best non-app results below the app grid. Choose which categories are included and their priority.
- **App Suggestions** - Turn app suggestions off, or choose which tabs (New & Updated, Pinned, Most Used) to show.
- **App Result Rows** - Choose whether search results show one or two rows of apps.
- **Fuzzy Search** - Find results even when your search has a typo.
- **Web Search Suggestions** - Turn web suggestions on or off and choose how many (1–5) appear.
- **Search History** - Save your recent queries, opened items, and AI answers. Choose how many appear before the list expands (up to 15). Swipe the expanded list to switch between **recent queries** and **recently opened results**; **Clear all** is at the bottom.
- **Search Result Ranking** - Choose whether equally matched results are ordered by recently opened or most opened.
- **Hidden Items** - See everything you've excluded from results and bring items back.
- **Nicknames & Triggers** - View and manage all the nicknames and triggers you have added.
- **Refresh Data** - Data refreshes automatically, but you can refresh apps, contacts, and files manually here.
- **Calendar Options** - Create and edit custom calendar events (they don't sync to your device calendar), choose which app opens events, and hide past events. All-day events stay on Home all day; timed events appear 15 minutes before they start and stay until they end. Tap **More Events** to see all of today's events.
- **Reminders Options** - Choose whether past reminders appear in results.
- **Notes Options** - Turn Quick Note off if you don't use it.

The following pages open from Search Results settings.

#### Apps

- **Installed Apps List** - See all installed apps known to Quick Search.
- **Sorting Options** - Sort apps by name, APK size, most/least used (when usage access is granted), installation date, last update time, or target API level.
- **Search & Select** - Search your apps and select several at once for batch actions.
- **Batch Uninstall & App Info** - Uninstall several user apps at once, open an app's system App Info screen, or view its details (package name, version, SDK levels, install/update dates).

#### App Shortcuts

- **Shortcuts List** - Browse all shortcuts in expandable cards, grouped by app, search engine, or browser.
- **Enable / Disable Shortcuts** - Turn individual shortcuts on or off in Quick Search without removing them from your device.
- **Search & Filters** - Search shortcuts and filter by all apps, apps with shortcuts, search engines, or browsers. Expand or collapse all cards at once.
- **Add Shortcuts from Apps** - Add shortcuts that apps provide, e.g. Google Drive files.
- **Search & Browser Shortcuts** - Create shortcuts that open a saved search or URL in any search engine or browser, including the in-app browser.
- **Deep Link Shortcuts** - Add, edit, or delete shortcuts that open a specific screen inside an app, e.g. a YouTube Music playlist.
- **App Activities** - Add an app's screens (activities) as shortcuts. Not every app exposes useful ones.

#### Calls & Texts

- **Number Search** - Find contacts by phone number.
- **Direct Dial** - Tap a phone result to call immediately instead of opening the dialer.
- **Default Calling & Messaging Apps** - Choose which app contact cards use for calls and messages. Supported when installed: Google Meet, WhatsApp, WhatsApp Business, Telegram, and Signal.

#### Files & Folders

- **Show Folders** - Choose whether folders appear in search results.
- **File Types** - Choose which file types are searchable (documents, pictures, videos, audio, APKs, other).
- **Excluded Extensions** - See and remove excluded file extensions.
- **System & Hidden Files** - Show or hide system and hidden files.
- **Folder Filters** - Choose folders to include or exclude from file search.
- **File Previews** - Preview PDFs and images before opening them.

---

### Search Engine Settings

- **Enable & Reorder** - Turn search engines on or off and change their order.
- **Aliases** - Set or edit each engine's alias.
- **Alias after Query** - Turn off aliases at the end of a query, or choose whether a space is needed to trigger them.
- **Search Engine Style** - Choose between:
  - **Inline**: Search engines scroll with the results.
  - **Compact**: Search engines stay in one or two rows above the keyboard.
- **Amazon Domain** - Choose your Amazon region. Once set, Amazon searches open in the Amazon app instead of the website.
- **Custom Search Engines** - Add any site using a URL with a `{{query}}` placeholder (tap the hint to insert it). The name and icon are filled in automatically (you can edit them), and you can choose which browser opens it.

---

### At a Glance
An optional Home section that shows useful information when it matters.

- **Today's Calendar Events** - See your events for today.
- **Upcoming Reminders & Timers** - See reminders and timers due within 30 minutes or overdue.
- **Media Controls** - Control currently playing media.
- **Low Battery Warnings** - See a warning when your battery is at 15% or lower.
- **Upcoming Alarms** - Your next alarm appears on Home when it is within 45 minutes of going off. Hide alarms from specific apps with **Hidden Alarm Apps**.

---

### AI Provider Settings

- **AI Providers** - Add API keys for Gemini, OpenAI, Claude, Groq, and Meta AI, and switch between models anytime.
- **Free Gemini API Key** - Tap **Get Free API Key** for a step-by-step guide. Gemini Flash Latest is the default model.
- **Model Selection** - Choose a model. Gemma models don't support Google Search grounding or personal context.
- **Grounding** - Let Gemini use Google Search for up-to-date answers.
- **Personal Context** - Add details about yourself so answers are more relevant. Stored only on your device.
- **Tavily Web Search** - Add a Tavily API key to give models without built-in web search access to the web, either only when needed or for every search.
- **Custom AI Providers** - Connect any OpenAI-compatible API, including self-hosted and third-party endpoints, by entering its base URL, API key, and model. Advanced users can supply a custom JSON payload for full control over request parameters.

---

### Gestures Settings

- **Swipe Left/Right** - By default, swipe right opens the Widgets Panel and swipe left opens Settings. Assign any action, search engine, or tool instead, set a swipe to close Quick Search, or turn it off.
- **Home Swipe Up/Down** - Assign an action, search engine, or tool, or turn the gesture off. Swipe down can also open the notification panel.
- **Double Tap** - Double-tap an empty area on Home to run an action, search engine, or tool, or to lock your screen.
- **App Icon Swipes** - Swipe up or down on an app icon to open any item you choose.
- **Keyboard Gestures** - Choose which swipes open and close the keyboard. They work on Home only when there's nothing to scroll.
- **Launcher Swipe Right** - When Quick Search is your launcher, swipe right opens the Widgets Panel.

---

### More Options

- **Top Result Indicator** - Show or hide the marker on the result that Enter will open.
- **Open Top Result with Keyboard** - Enter opens the top result by default. Turn this off to make Enter search your default search engine instead.
- **Open Keyboard** - Turn off to stop the keyboard opening automatically, useful if you mainly use pinned items.
- **Clear Query** - Your last query is cleared when you reopen the app. Turn this off to keep it.
- **Auto-Close App** - Close Quick Search automatically after you open something.
- **Show in Recents** - Show Quick Search in your device's recent apps.
- **Notification Dots** - Show or hide notification dots on app icons.
- **All Apps** - Show a button below app suggestions for browsing all installed apps.
- **Non-Launchable Apps** - Show system services and other apps that can't be opened directly.

---

### Launch Options

- **Default Assistant** - Replace Google Assistant or Gemini with Quick Search. Turn on **Assistant Voice Mode** to start with voice typing when opened by the assistant gesture.
- **Set as Launcher** - Make Quick Search your home screen so it opens with the home button.
- **Home Screen Widget** - Add the Quick Search widget to your home screen directly from settings.
- **Quick Settings Tile** - Add a Quick Search tile to Quick Settings.
- **Edge Swipe** - Open Quick Search from anywhere by swiping from the screen edge.
- **Floating Button** - Open Quick Search from a floating button.

---

### Permissions Settings

See the status of each permission and grant it from here.

- **Usage Access** - Used for app suggestions and usage-based ranking.
- **Contacts** - Needed to search contacts and use calls & texts features.
- **Files / Storage** - Needed for file search.
- **Phone** - Needed for direct dial and some calling apps.
- **Calendar** - Needed to search calendar events.
- **Notification Access** - Needed for notification dots, notification history, and media controls.
- **Post Notifications** - Needed for reminder notifications and pinning results to the notification panel.
- **Alarms & Reminders** - Needed to deliver reminder notifications on time.
- **Accessibility** - Needed for double-tap to lock screen, the edge swipe gesture, and the floating button.
- **Allow Background Usage** - Lets Quick Search start faster.

---

### Language

- **App Language** - Use a different language in Quick Search than on your device. Available in Arabic, Chinese (Simplified), Dutch, English, French, German, Greek, Hindi, Indonesian, Italian, Japanese, Polish, Portuguese (Brazil), Russian, Spanish, Telugu, and Turkish.

---

### Other Options

- **Backup & Restore Settings** - Export or import your settings, e.g. when switching phones. Settings are also backed up automatically through Android's backup system.
- **Send Feedback** - Send feedback, bug reports, and feature requests to [tejakarlapudi.apps@gmail.com](mailto:tejakarlapudi.apps@gmail.com).
- **Rate Quick Search** - Leave a rating for the app.
- **Development** - View the source code on GitHub.
- **Release Notes & Features List** - See what's new and browse every feature.
- **Open Source Licenses** - View licenses for the libraries Quick Search uses.
- **More Apps from the Developer** - See other apps by the developer.
- **Contact Developer** - Tap the developer name below the version number.

---

### Note from the Developer

I've been an Android enthusiast my whole life, and it's been awesome working on this project. Because I have the privilege of a full-time job, I decided to keep this app completely free, ad-free, and open source — my small contribution to the Android community that's given me so much.

This is what I ask in return: if this app brings value to your life, share it with the people around you and consider leaving a 5-star review on the Play Store. That means a lot to me.

Your feedback is important — it's what shapes every update and improvement. So please, keep it coming. Thank you for downloading, using, and being a part of this journey.

[Teja Karlapudi](https://teja2495.github.io/teja-karlapudi-links/)
