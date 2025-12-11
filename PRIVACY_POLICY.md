# Quick Search – Privacy Policy

Last updated: 2025-12-10

Quick Search is an Android app that lets you search apps, contacts, device files, and the web from a single screen. This policy explains what data the app accesses, how it is used, and your choices.

## Data the app accesses
- Search input: Text you type is processed on-device to show local results. It is not stored after the session unless you send it to a search engine or direct answer provider.
- Apps: App names, package IDs, and last-used timestamps (requires Usage Access) to rank and show recently used apps.
- Contacts (optional): Contact names, phone numbers, and your chosen “preferred number” when you enable contact search.
- Files (optional): File names, types, URIs, and basic metadata from MediaStore to show matching files; no file contents are read.
- Preferences: Hidden/pinned apps, contacts, files, nicknames, section order, layout choices, shortcut codes, and widget settings stored locally in SharedPreferences. Sensitive items such as the optional Gemini API key are stored with EncryptedSharedPreferences when available.
- Direct answers (optional): If you add a Gemini API key and use AI answers, the query and any personal context you provide are sent to Google’s Generative Language API to generate a response. Request/response bodies may be logged to your device’s logcat for troubleshooting and are not sent to the developer.
- Network calls: Other web searches you launch (Google, Maps, Play, Reddit, YouTube, Amazon, ChatGPT/Perplexity/Grok links, etc.) are opened in the chosen browser/app and handled under those providers’ policies.

## How we use data
- Provide search results for apps, contacts, files, and shortcuts.
- Maintain your preferences (pinned/hidden items, nicknames, filters, widget look, shortcuts).
- Generate direct answers via Google’s Generative Language API when you request them.
- Launch external search providers you pick; those providers receive the query you submit.

## Data sharing and transfers
- We do not sell data, run ads, or use third-party analytics.
- Direct answers are processed by Google’s Generative Language API (HTTPS). Queries you open with other search engines are sent to those providers at your direction.

## Storage and retention
- Preferences and caches are stored locally on your device. The Gemini API key is stored encrypted when the device supports it; personal context is stored locally in preferences.
- App, contact, and file data are refreshed from the device as needed. Clearing the app’s data or uninstalling removes locally stored preferences and caches. You can also clear cached apps from in-app settings.

## Permissions
- Usage Access (required): Needed to list and rank recently used apps.
- Contacts (optional): Needed to search and act on your contacts.
- Storage/Media (optional): Needed to search files on the device.
- Network: Used only when you open an external search or request a direct AI answer.
- QUERY_ALL_PACKAGES (manifest): Declared to surface installed apps in the launcher grid.

## Security
- Local processing by default; HTTPS is used for outbound requests.
- Sensitive keys are stored with EncryptedSharedPreferences where supported. No backend operated by the developer stores your data.

## Your choices
- Do not enter a Gemini API key if you do not want queries sent to Google for direct answers; remove the key and personal context in settings to stop further use.
- Turn off or decline Contacts/Storage permissions to keep those data types inaccessible.
- Clear app data or uninstall to remove local preferences and caches; use in-app options to clear cached apps or edit pinned/hidden items.

## Children
Quick Search is not directed to children under 13 and should not be used by them.

## Changes
We may update this policy. Material changes will be reflected by updating the “Last updated” date above.

## Contact
Questions? Contact us at tejakarlapudi.apps@gmail.com.
