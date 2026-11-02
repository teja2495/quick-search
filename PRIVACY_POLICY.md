# Quick Search – Privacy Policy

Last updated: 2026-11-02

Quick Search is an Android app and optional home screen launcher that lets you search apps, contacts, calendar events, device files, notes, settings, notifications, and the web from a single screen, and to see an optional At a Glance summary, reminders, and widgets on your Home screen. This policy explains what data the app accesses, how it is used, and your choices.

**In short: Quick Search does not collect your data.** The app has no analytics, no tracking, no ads, no accounts, and no backend server. Every feature and permission exists solely to serve you on your device; nothing is automatically sent to or stored by the developer. Data leaves your device only when you use a feature that talks to a third-party service you choose, such as web suggestions, a search engine, or an AI provider or Tavily using your own API key. Those requests go directly from your device to that service and are governed by its privacy policy.

## Data the app accesses
- Search input: Text you type is processed on-device to show local results. It is not stored after the session, except as described under Web suggestions, AI features, and external searches below.
- Voice typing (optional): When you tap the mic button, speech recognition is handled by your device's speech recognition service (such as Google), under that service's privacy policy. Quick Search only receives the recognized text and does not access your microphone or record audio itself.
- Web suggestions (optional): When web suggestions are enabled, the text you type is sent to Google's search suggestion service (suggestqueries.google.com) to show suggested searches. This is on by default in the Google Play version and off by default in the F-Droid version, and can be turned off in settings.
- Apps: App names, package IDs, launch counts, and last-used timestamps (requires Usage Access) to rank and show recently used apps. If you use Android Private Space, Quick Search lists the apps in it so you can find and open them; this information stays on your device.
- Contacts (optional): Contact names, phone numbers, birthdays (for At a Glance birthday reminders), and your chosen “preferred number” when you enable contact search.
- Calendar (optional): Event titles, start and end times, all-day status, and recurrence rules from your device calendars, to search and show upcoming events. Quick Search only reads calendar data; it does not create, change, or delete events.
- Files (optional): File names, types, URIs, and basic metadata to show matching files; no file contents are read.
- Notes, reminders, and countdowns: Notes, reminders, countdown widgets, and scheduled Custom Info items you create in Quick Search are stored locally on your device.
- Notification access (optional): If you grant notification access, Quick Search uses Android's NotificationListenerService to read the notifications on your device, including the app, title, and text. This is used to:
  - Show notification dots on app icons.
  - Keep a searchable notification history. The title and text of recent notifications (up to the latest 500) are stored in a local database on your device. You can hide apps from history or clear it at any time in settings, and it is excluded from device backups.
  - Show At a Glance items drawn from notifications, such as app notifications you choose, one-time passcodes (OTPs), missed and ongoing calls, timers and stopwatches, progress updates, weather, and workouts. OTP codes are only kept in memory while shown.
  - Show and control currently playing media (title, artist, artwork, and playback state) in At a Glance and the Media Controls widget.
  If you add the At a Glance widget, these items can also appear in that widget on your Home screen, in whatever launcher you use.
  Notification data stays on your device and is never sent to the developer or to AI providers.
- Accessibility service (optional): When you enable the Lock Screen gesture, Quick Search uses Android's AccessibilityService API only to perform the system Lock Screen action after you double-tap the Home screen. Through this service, Quick Search does not read, collect, store, or share screen content or other personal data.
- App lock (optional): If you lock apps, Quick Search uses Android's system biometric or screen-lock prompt to confirm it is you. The prompt is handled by Android; Quick Search only receives whether authentication succeeded and never accesses your fingerprint, face, or PIN data.
- Preferences: Hidden/pinned apps, contacts, files, nicknames, section order, layout choices, aliases, custom search engines, custom tools, pinned notification shortcuts, At a Glance options, Home widget panel layout, and widget settings stored locally. Sensitive items such as AI provider API keys are stored with EncryptedSharedPreferences when available.
- AI features (optional): If you add an API key for an AI provider (Google Gemini, OpenAI, Anthropic, Groq, Meta, or a custom OpenAI-compatible endpoint you configure) and use AI answers, `@help` questions, custom tools, or AI-powered tools (such as weather lookups), your query and any personal context you provide are sent to that provider to generate a response. For weather, the location you enter or save is included. Scheduled Custom Info items in At a Glance send their saved prompt to the provider you chose at the times you schedule, in the background. Custom endpoints on your local network may use unencrypted HTTP if you enter an http:// address. Request details may be logged to your device's logcat for troubleshooting, with API keys redacted, and are not sent to the developer.
- Dictionary (optional): When the dictionary tool is enabled and you search for a definition (for example "define serendipity"), the word you look up is sent to the Free Dictionary API (api.dictionaryapi.dev) to fetch its definition. No API key or other personal data is included.
- AI web search (optional): If you add a Tavily API key, your AI search query may be sent to Tavily (api.tavily.com) to fetch web results for the answer.
- Device status (At a Glance): Battery charging state, free storage, Do Not Disturb, airplane mode, hotspot, flashlight, and whether your Wi-Fi network needs a sign-in page. This is read on-device only to show At a Glance items and is not stored or shared.
- Home widgets: If you add other apps' widgets to the Home widget panel, those widgets are provided and run by their own apps under their policies.
- Settings export (optional): When you export a backup, Quick Search writes the items you select (such as settings, pinned items, notes, reminders, and optionally API keys and AI personal context) to a file in your Downloads folder. The file is not encrypted, so keep it private.
- Network calls: Other web searches you launch (Google, Maps, Play, Reddit, YouTube, Amazon, ChatGPT/Perplexity/Grok links, custom search engines, etc.) are opened in the chosen browser/app and handled under those providers' policies.

## How we use data
- Provide search results for apps, contacts, calendar events, files, notes, settings, and shortcuts.
- Show notification dots, notification history, At a Glance items, media controls, and reminders; lock apps; and perform the Lock Screen gesture when you enable those features.
- Maintain your preferences (pinned/hidden items, nicknames, filters, widget look, shortcuts).
- Generate AI answers through the provider you choose, when you request them.
- Launch external search providers you pick; those providers receive the query you submit.

## Data sharing and transfers
- We do not sell data, run ads, or use third-party analytics. No backend operated by the developer receives your data.
- AI requests are sent directly from your device to the AI provider whose API key you added (over HTTPS for all built-in providers), and to Tavily if you added a Tavily key. These requests never pass through a developer server. How the provider or Tavily handles that data is governed by their own privacy policies and terms, which you should review before adding a key.
- Web suggestions, when enabled, send your typed text to Google.
- Queries you open with other search engines are sent to those providers at your direction.
- Diagnostic logs (optional): Quick Search never sends logs automatically. If you choose to share a log from the app, for example to report a bug, it is shared through the app you pick, such as your email app. The log includes your device model, Android version, app version, memory details, the time, and Quick Search's own recent log output, which can include details of recent AI requests (with API keys redacted). Review the log before sending if you're concerned about its contents.
- The app also makes these requests that do not include your personal data:
  - Daily currency exchange rates for the currency converter, from the open currency-api dataset (served from cdn.jsdelivr.net, with latest.currency-api.pages.dev as a mirror) and, as a last fallback, the European Central Bank (ecb.europa.eu).
  - Release notes from GitHub (api.github.com).
  - Website icons from Google (google.com/s2/favicons) when you add a custom search engine.
  - In the Google Play version, Google Play's in-app review and update services.

## Storage and retention
- Preferences, notes, reminders, notification history, and caches are stored locally on your device. API keys are stored encrypted when the device supports it; personal context is stored locally in preferences.
- Some preferences (such as layout and pinned items) may be included in Android's device backup to your Google account if you have backup enabled. API keys, notification history, usage history, launch counts, and hidden-item lists are excluded from backup.
- App, contact, calendar, and file data are refreshed from the device as needed. Clearing the app's data or uninstalling removes locally stored preferences, notes, reminders, notification history, and caches. You can also clear cached apps from in-app settings.

## Permissions
- Usage Access (required): Needed to list and rank recently used apps.
- QUERY_ALL_PACKAGES: Declared to show installed apps in search results and the launcher grid.
- Access hidden profiles: Used to show apps in Android Private Space, when you use it.
- Contacts (optional): Needed to search and act on your contacts.
- Phone calls (optional): Lets Quick Search place a call directly when you tap call on a contact. Without it, calls open in your dialer instead.
- Answer phone calls (optional): Used only to end an ongoing call when you tap hang up in At a Glance. Without it, the call screen opens instead.
- Calendar (optional): Needed to search and show your calendar events. Read-only.
- Storage/Media and All files access (optional): Needed to search file names across your device storage. File contents are not read. On older Android versions, storage write access is used only to save exported backups and the feature list to your Downloads folder.
- Notification access (optional): Used for notification dots, notification history, At a Glance, and media controls, as described above. You grant it in Android settings and can revoke it at any time.
- Accessibility service (optional): Used only to lock the device when you invoke the configured double-tap Home gesture. You must explicitly agree before Quick Search opens Android's Accessibility settings, and you can disable the service at any time in Android settings.
- Notifications (optional): Used to show reminders and the shortcuts and notes you pin to the notification shade.
- Alarms and reminders: Exact alarm scheduling and starting at boot are used only to deliver reminders and scheduled Custom Info on time and to reschedule them after a restart. Set alarm lets the alarm tool hand an alarm or timer to your clock app.
- Expand status bar: Used to open the notification shade when you use the configured gesture.
- Request app uninstall: Used to open Android's uninstall dialog when you choose to uninstall an app. Android always asks you to confirm.
- Biometric: Used through Android's system prompt for app lock, as described above.
- Vibration: Used for haptic feedback.
- Network: Used for web suggestions, AI features, dictionary lookups, currency rates, release notes, website icons, and external searches, as described above.
- Network and Wi-Fi state: Used only for At a Glance items that show Wi-Fi sign-in and hotspot status.

## Security
- Local processing by default; HTTPS is used for all built-in outbound requests. Plain HTTP is only used if you configure a custom AI endpoint with an http:// address.
- API keys are stored with EncryptedSharedPreferences where supported and excluded from backups. No backend operated by the developer stores your data.

## Your choices
- Turn off web suggestions in settings to stop sending typed text to Google.
- Do not add an AI provider API key if you do not want queries sent to AI providers; remove keys and personal context in settings to stop further use.
- Turn off or decline Contacts, Phone, Calendar, and Storage permissions to keep those data types inaccessible.
- Don't grant, or revoke, Quick Search's notification access in Android settings to stop notification dots, notification history, and notification-based At a Glance items. You can also hide apps from notification history or clear it in settings.
- Turn off At a Glance items, Custom Info, or reminders you don't want.
- Choose “Not now” when asked about Accessibility access, or disable “Quick Search Lock Screen” in Android's Accessibility settings, to keep the Lock Screen gesture inactive.
- Clear app data or uninstall to remove local preferences, notes, and caches; use in-app options to clear cached apps or edit pinned/hidden items.

## Children
Quick Search is not directed to children under 13 and should not be used by them.

## Changes
We may update this policy. Material changes will be reflected by updating the “Last updated” date above.

## Contact
Questions? Contact us at tejakarlapudi.apps@gmail.com.
