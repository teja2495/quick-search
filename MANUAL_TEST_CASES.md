# Quick Search - Manual Test Cases

This document contains comprehensive manual test cases for the Quick Search Android application, organized by functional area and priority level.

---

## Test Case Categories

Tests are organized by functional area for better maintainability:

1. **Core Search & Launch** - Basic search functionality and app launching
2. **Contacts & Communication** - Contact search and messaging integration
3. **Files & Media** - File search and management
4. **Apps & Shortcuts** - App management and shortcuts
5. **Search Engines & AI** - Web search engines and AI features
6. **Settings & Customization** - App configuration and UI customization
7. **System Integration** - Widgets, assistants, and system features
8. **Edge Cases & Error Handling** - Boundary conditions and error scenarios

---

## 1. CORE SEARCH & LAUNCH

Tests for basic search functionality and app launching. **All of these should pass before releasing any version.**

### 1.1 First Launch & Onboarding

#### TC-C-001: First Launch Experience
**Preconditions:** Fresh install, app never opened before
- Launch the app for the first time
- Verify onboarding screens appear (permissions → search engine setup → final setup)
- Complete the onboarding flow
- **Expected:** App opens to the main search screen after onboarding
- **Expected:** Usage permission is requested and granted

#### TC-C-002: Permission Management and Core Search
**Preconditions:** Fresh install for onboarding, app installed for settings tests
- **Onboarding Flow:** During first launch, deny usage permission
- **Expected:** Warning screen shown explaining app cannot function without usage data
- Grant usage permission when prompted
- **Expected:** App proceeds to main search screen
- **Permission Status Check:** Go to Settings → Permissions
- **Expected:** Shows current status for all permissions (usage, contacts, files)
- **Grant Additional Permissions:** For denied contact/file permissions:
  - Tap permission item, grant in system dialog
  - **Expected:** Status updates to granted, functionality becomes available
- **Permission Disabled UI:** Search for contacts/files when permission denied
- **Expected:** Permission disabled card appears with clear message and "Grant Permission" button
- Tap "Grant Permission" and grant in system dialog
- **Expected:** Card disappears, search results appear immediately

#### TC-C-003: App Search Functionality
**Preconditions:** At least 10 apps installed, usage permission granted
- Open Quick Search and verify search bar is auto-focused
- **Empty Search State:** Leave search bar empty
- **Expected:** Pinned apps shown first, then recent apps in 2×5 grid (max 10), sorted by usage
- **Successful Search:** Type app name (e.g., "Chrome")
- **Expected:** App appears in results grid, tap launches successfully
- **No Results Scenario:** Type nonexistent app name
- **Expected:** No app results, but calculator/web suggestions may appear for math queries

#### TC-C-004: Contact Search - Basic
**Preconditions:** Contact permission granted, contacts exist
- Type name of a contact in search bar
- **Expected:** Contact appears in results
- Tap the contact
- **Expected:** Contact action menu appears (call, SMS, WhatsApp, Telegram, etc.)
- Tap "Call" option
- **Expected:** Phone dialer opens with contact number (or direct call if enabled)

#### TC-C-005: File Search - Basic
**Preconditions:** File permission granted, files exist on device
- Type name of a file (PDF, image, or document)
- **Expected:** File appears in search results
- Tap the file
- **Expected:** File opens in appropriate app

#### TC-C-006: Fuzzy Search and Ranking
**Preconditions:** Apps with various names installed, including multi-word names
- **Basic Fuzzy Matching:** Type partial app name (e.g., "chr" for "Chrome")
- **Expected:** Chrome appears in results (fuzzy matching)
- **Typo Tolerance:** Type with typos (e.g., "chrmoe" for "Chrome")
- **Expected:** Chrome still appears (fuzzy search tolerance)
- **Acronym Matching:** Type acronym of app name (e.g., "yt" for "YouTube", "gm" for "Google Maps")
- **Expected:** Apps appear in results due to acronym matching
- **Ranking Priority:** Type query matching multiple apps at different levels:
  - Exact match (highest), starts with (second), second word starts (third), contains anywhere (lowest)
- **Expected:** Results appear in correct priority order for apps, contacts, and files
- **Case Insensitivity:** Test that queries work regardless of case
- **Expected:** Same results for "Chrome", "chrome", "CHROME"

#### TC-C-008: Settings Search - Basic
**Preconditions:** App launched
- Type name of a device setting (e.g., "WiFi", "Bluetooth")
- **Expected:** Device settings matching query appear
- Tap a setting
- **Expected:** Device settings screen opens to that specific setting

### 1.3 Search Engine Integration

#### TC-C-009: Search Engine Integration
**Preconditions:** Multiple search engines enabled, various shortcuts configured
- **Display Modes:**
  - **Inline Mode:** Set style to "Inline", type query, scroll to see engines below results
  - **Expected:** Engine cards visible in scrollable area
  - **Compact Mode:** Change to "Compact", type query
  - **Expected:** Engines fixed at bottom above keyboard, horizontal scroll works
- **Engine Launch:** Tap any search engine in either mode
- **Expected:** Browser opens with query on selected engine
- **Shortcut Testing:** Test representative shortcuts:
  - "ggl weather today" → Google
  - "cgpt explain quantum physics" → ChatGPT
  - "ytb cooking tutorials" → YouTube
  - "gmi what is machine learning" → Gemini
  - "dsh summarize this article" → Direct Search (AI Mode)
- **Expected:** Each shortcut opens correct engine with query (prefix removed)

### 1.4 Calculator

#### TC-C-011: Built-in Calculator Functionality
**Preconditions:** App launched, apps and contacts exist that could match calculator queries
- Type basic operations: "2+2", "10*5", "100/4", "50-25"
- **Expected:** Calculator result card shows correct results ("4", "50", "25", "25")
- Type expressions with order of operations: "2+2*3"
- **Expected:** Shows "8" (not 12, demonstrating proper precedence)
- Type complex expressions: "(10+5)*2", "3.5+2.5"
- **Expected:** Shows "30" and "6.0" respectively
- Type calculator expressions that could match other content (e.g., "2+2" matching apps/contacts)
- **Expected:** Calculator result appears prominently at top
- **Expected:** Other search results (apps, contacts) still appear below if they match
- **Expected:** Calculator result is clearly distinguished from other results

### 1.5 Pinning & Exclusions

#### TC-C-012: Item Management - Pin/Exclude/Restore
**Preconditions:** App launched
- **Pin Item:** Long press any app in suggestions or search results
- **Expected:** Context menu appears with pin option
- Tap "Pin"
- **Expected:** Item shows as pinned
- Clear search query
- **Expected:** Pinned app appears at the top
- Restart the app
- **Expected:** Pinned app still appears at top
- **Exclude Item:** Long press a different app in suggestions
- Tap "Exclude from suggestions"
- **Expected:** App removed from suggestions
- Clear and reopen Quick Search
- **Expected:** Excluded app no longer appears in empty search suggestions
- Search for the excluded app by name
- **Expected:** App still appears in search results (not excluded from search)
- **Restore/Unpin Items:** Go to Settings → Excluded Items
- **Expected:** All excluded items listed
- Tap an excluded item to restore it
- **Expected:** Item restored to search suggestions
- Return to search screen, long press the pinned item
- Tap "Unpin"
- **Expected:** Item unpinned and moves to normal position

### 1.6 Settings Access

#### TC-C-013: Access Settings
**Preconditions:** App launched
- Swipe down or tap settings icon (top right)
- **Expected:** Settings screen opens
- **Expected:** All settings categories visible (Appearance, Search Results, etc.)
- Navigate back
- **Expected:** Returns to search screen

#### TC-C-014: Enable/Disable Search Sections
**Preconditions:** In Settings
- Go to "Search Results" settings
- Toggle OFF "Contacts"
- **Expected:** Toggle state changes
- Go back to search screen
- Search for a contact name
- **Expected:** No contact results shown
- Go back to settings and re-enable "Contacts"
- **Expected:** Contact results appear again

### 1.7 Permission Management

#### TC-C-015: Permission Disabled UI
**Preconditions:** Contact or file permission denied
- Search for contacts/files when permission is denied
- **Expected:** Permission disabled card appears with clear explanatory message
- **Expected:** "Grant Permission" button is visible and functional
- Tap "Grant Permission" and grant permission in system dialog
- **Expected:** Permission disabled card disappears immediately
- **Expected:** Search results for that section appear
- Search again to confirm functionality persists

---

## 2. CONTACTS & COMMUNICATION

Tests for contact search, messaging integration, and communication features.

### 2.0 Search Result Management

#### TC-A-001: App Sort Options - Most Used Ordering
**Preconditions:** App launched with multiple apps used at different frequencies
- Go to Settings → Search Results
- Verify "Sort by most used" is enabled by default
- Clear search query to view app suggestions
- **Expected:** Apps appear in order of most recently used
- Disable "Sort by most used"
- **Expected:** Apps appear in alphabetical order
- Re-enable setting
- **Expected:** Apps return to usage-based ordering


### 2.1 Contact Features

#### TC-A-002: Contact Integration and Actions
**Preconditions:** Contact permission granted, various messaging apps installed
- **Multiple Phone Numbers:** Search contact with multiple numbers
- **Expected:** Primary number shown, dropdown reveals all numbers, selection remembered
- **Messaging Integration:** Search contact, tap to open actions menu
- **Expected:** Available messaging options shown (WhatsApp, Telegram, SMS, Google Meet)
- Test each messaging action (chat, call, video where available)
- **Expected:** Respective apps open correctly
- **Default Messaging:** In Settings → Calls and Texts, set default messaging app
- **Expected:** Default app appears prominently in contact actions
- **Direct Calling:** Enable/disable direct calling setting
- **Expected:** Direct calling bypasses dialer when enabled

#### TC-A-007-A: Contact Action Hints
**Preconditions:** First time using contact actions (or reset hint preference)
- Search for and tap a contact to open actions menu
- **Expected:** Action hint/tutorial appears explaining how to use contact actions
- Dismiss the hint (tap outside or dismiss button)
- **Expected:** Hint doesn't appear again on subsequent contact taps
- **Expected:** Contact actions still function normally after hint dismissal

## 3. FILES & MEDIA

Tests for file search, filtering, and media management.

#### TC-A-008: File Type Filtering
**Preconditions:** Files of various types exist
- Go to Settings → File Settings
- Disable "Photos & Videos"
- **Expected:** Toggle turns off
- Search for an image or video name
- **Expected:** No photo/video results appear
- Search for a document name
- **Expected:** Document results still appear
- Re-enable all file types

#### TC-A-009: Exclude File Extension
**Preconditions:** PDF files exist on device
- Search for a PDF file
- Long press the PDF result
- Tap "Exclude .pdf files"
- **Expected:** All PDF files excluded
- Search for other PDFs
- **Expected:** No PDF results shown
- Go to Settings → Excluded Items
- **Expected:** ".pdf" shown in excluded extensions
- Remove the exclusion
- **Expected:** PDFs appear in search again

#### TC-A-010: APK File Handling
**Preconditions:** APK files exist on device
- Search for APK filename
- **Expected:** APK appears in file results
- Tap the APK result
- **Expected:** Android file browser opens to APK location (not direct install)
- **Expected:** No security warnings or crashes

## 4. APPS & SHORTCUTS

Tests for app management, nicknames, and shortcuts.

#### TC-A-012: App Nicknames
**Preconditions:** App launched
- **Add Nickname:** Long press any app
- Tap "Add nickname"
- **Expected:** Nickname dialog appears
- Enter a nickname (e.g., "browser" for Chrome)
- Save the nickname
- **Expected:** Dialog closes
- Type the nickname in search
- **Expected:** App appears in results
- Long press same app
- **Expected:** "Edit nickname" shown (not "Add nickname")
- **Edit/Remove Nickname:** Tap "Edit nickname"
- Clear the nickname field (leave empty or only spaces)
- Save
- **Expected:** Nickname removed
- Long press same app
- **Expected:** Shows "Add nickname" again

#### TC-A-013: App Context Menu Actions
**Preconditions:** App launched, user-installed app exists
- **App Info:** Long press any app
- Tap "App Info"
- **Expected:** System app info screen opens
- **Uninstall:** Long press a user-installed app
- Tap "Uninstall"
- **Expected:** System uninstall dialog appears
- Cancel or proceed with uninstall
- **Expected:** Behaves as expected

#### TC-A-014: File Nicknames
**Preconditions:** File permission granted, files exist on device
- Long press a file result
- Tap "Add nickname"
- Enter a nickname and save
- Search using the nickname
- **Expected:** File appears in search results
- Long press the same file
- **Expected:** Shows "Edit nickname" option instead of "Add nickname"
- Tap "Edit nickname", clear the field to empty/whitespace
- **Expected:** Nickname removed, shows "Add nickname" again

## 5. SEARCH ENGINES & AI

Tests for web search engines, shortcuts, and AI features.

#### TC-A-016: Reorder Search Engines
**Preconditions:** Multiple search engines enabled
- Long press any search engine card
- **Expected:** Reorder/customize menu appears
- Drag to reorder or use settings
- **Expected:** Order changes persist
- Restart app
- **Expected:** New order maintained

#### TC-A-017: Enable/Disable Search Engines
**Preconditions:** In search engine settings
- Go to Settings → Search Engines
- Disable a search engine (e.g., "Bing")
- Save changes
- Return to search screen
- Type a query
- **Expected:** Disabled engine not shown in results
- Re-enable the engine
- **Expected:** Engine appears again

#### TC-A-018: Custom Shortcut Modification
**Preconditions:** In search engine settings
- Find a search engine with a shortcut
- Modify the shortcut text
- Save
- Return to search and use the new shortcut
- **Expected:** New shortcut works, search opens correctly

### 2.5 Web Suggestions & Recent Searches

#### TC-A-019: Web Suggestions Functionality
**Preconditions:** Web suggestions enabled, internet connected, recent searches enabled
- **Web Suggestions:** Type a query with no local results (e.g., "latest news")
- Wait 1-2 seconds
- **Expected:** Web suggestions from Google Suggest API appear
- **Expected:** Number of suggestions matches setting (default 3)
- Tap a suggestion
- **Expected:** Query updates to suggestion text
- **Recent Searches:** Perform several searches (type query, tap search engine)
- Clear search bar
- **Expected:** Recent search queries appear (newest first)
- Tap "X" on a recent query
- **Expected:** That query removed from recent searches
- Tap a recent query
- **Expected:** Query fills search bar

#### TC-A-020: Web Suggestions & Recent Searches Configuration
**Preconditions:** In settings
- **Web Suggestions Count:** Go to Settings → Search Results → Web Suggestions
- **Expected:** Count slider visible (range 1-5)
- Set slider to 1, return to search, type query
- **Expected:** Exactly 1 web suggestion appears
- Set slider to 5, return to search, type different query
- **Expected:** Up to 5 web suggestions appear (if available)
- **Expected:** Slider responds to tap and drag interactions
- **Recent Queries Count:** Go to Settings → Search Results → Recent Queries
- **Expected:** Count slider visible (range 1-10)
- Set slider to 2, perform 5 searches, clear search bar
- **Expected:** Only 2 most recent queries appear
- Set slider to 5, clear search bar
- **Expected:** 5 recent queries appear
- Perform additional searches to exceed limit
- **Expected:** Oldest queries automatically removed
- **Expected:** All settings persist across app restarts

### 2.6 Direct Search (Gemini API)

#### TC-A-021: Direct Search (Gemini AI)
**Preconditions:** Gemini API key configured, internet connected
- **Basic Setup:** Configure API key in Settings → Search Engines → Direct Search
- Search "dsh what is the weather today?"
- **Expected:** AI response appears in dedicated result card
- **Personal Context:** Enable personal context in Direct Search settings
- Add context info (e.g., location), search "dsh restaurants near me"
- **Expected:** Response incorporates personal context
- **Interactive Elements:** Search queries with phone/email data
- **Expected:** Phone numbers and emails are tappable, launch dialer/email app

### 2.7 App Shortcuts

#### TC-A-024: App Shortcuts
**Preconditions:** Apps with shortcuts installed (Chrome, Maps, etc.)
- Search for app with shortcuts available
- **Expected:** Shortcuts section appears below app results
- Tap any shortcut
- **Expected:** App opens to specific shortcut action
- **Pin Shortcuts:** Long press shortcut, tap "Pin"
- **Expected:** Shortcut appears on home screen
- **Exclude Shortcuts:** Long press shortcut, tap "Exclude"
- **Expected:** Shortcut removed from results, persists after restart

## 6. SETTINGS & CUSTOMIZATION

Tests for appearance, behavior settings, and UI customization.

#### TC-A-027: Wallpaper Background
**Preconditions:** File permission granted
- Go to Settings → Appearance
- Enable "Wallpaper Background"
- **Expected:** Search screen background shows device wallpaper
- Cards have semi-transparent overlay
- Disable wallpaper background
- **Expected:** Standard Material 3 surface colors used

#### TC-A-028: One-Handed Mode
**Preconditions:** In appearance settings
- Enable "One-Handed Mode"
- **Expected:** Setting toggles on
- Return to search screen
- Type a query
- **Expected:** Search results positioned at bottom of screen near keyboard
- **Expected:** Scroll direction may be reversed (results stack upward)

#### TC-A-029: Section Titles Toggle
**Preconditions:** Search with multiple result types
- Go to Settings → Appearance
- Disable "Section Titles"
- Return to search screen
- Perform search with multiple result types
- **Expected:** No "Apps", "Contacts", "Files" headers shown
- Re-enable section titles
- **Expected:** Headers appear again

#### TC-A-030: Icon Pack Support
**Preconditions:** Icon pack app installed
- Go to Settings → Appearance
- Select "Icon Pack"
- Choose installed icon pack
- Return to search screen
- **Expected:** App icons use the selected icon pack style

### 2.9 Behavior Settings

#### TC-A-031: Clear Query After Action
**Preconditions:** In settings
- Go to Settings → Launch Options
- Enable "Clear query after action"
- Return to search screen
- Type query and launch an app
- Return to Quick Search
- **Expected:** Search query cleared
- Disable setting
- Repeat test
- **Expected:** Query persists

#### TC-A-032: Auto-Expand Results (UPDATED)
**Preconditions:** Search with results that show "More" button for a section
- Go to Settings → Search Results → Advanced
- Enable "Auto-expand results"
- Return to search screen
- **Expected:** All results shown automatically, no "More" button
- **Expected:** Performance remains smooth with many results
- Disable setting
- **Expected:** "More" button returns for sections with many results

#### TC-A-033: Manual Data Refresh
**Preconditions:** App launched with various permissions granted
- Go to Settings → Behavior Settings
- Tap "Refresh Data"
- **Expected:** Loading indicator appears
- **Expected:** App data refreshes (may take a few seconds)
- **Expected:** Recent apps update based on current usage
- **Expected:** Contact and file data refresh if permissions granted
- **Expected:** No crashes or errors during refresh
- Test with different permission combinations (only usage, usage + contacts, usage + files, all permissions)

#### TC-A-034: Haptic Feedback
**Preconditions:** Device supports haptics
- Perform various actions (toggle settings, long press, tap)
- **Expected:** Device vibrates with context-appropriate feedback
- Disable haptic feedback in device settings
- **Expected:** No haptic feedback in app

### 2.10 Home Screen Widget

#### TC-A-035: Home Screen Widget
**Preconditions:** Device home screen with space for widgets
- **Add Widget:** Long press home screen, select Quick Search widget
- **Expected:** Widget appears as search bar, tap opens Quick Search app
- **Customize Widget:** Long press widget → Settings
- Change styling options (color, transparency, border)
- **Expected:** Preview updates, saved changes persist
- **Voice Search:** In widget settings, configure mic actions
- **Expected:** Mic launches voice typing or digital assistant correctly
- **Multiple Widgets:** Add 2+ widgets with different customizations
- **Expected:** Each maintains separate settings
- **Uninstall Behavior:** Uninstall app while widget exists
- **Expected:** Widget removed or shows unavailable, can re-add after reinstall

## 7. SYSTEM INTEGRATION

Tests for widgets, default assistant, and system-level integration.

#### TC-A-036: Set as Default Assistant
**Preconditions:** App launched
- Go to device Settings → Apps → Default Apps → Digital Assistant
- Select "Quick Search" as assistant
- Long press home button or swipe to trigger assistant
- **Expected:** Quick Search opens instead of Google Assistant
- Alternatively, say "Hey Google" (if configured)
- **Expected:** Quick Search may appear based on system configuration

#### TC-A-037: Quick Settings Tile
**Preconditions:** App installed
- Swipe down to open Quick Settings
- Edit tiles (usually via edit button)
- Add "Quick Search" tile if not present
- **Expected:** Quick Search tile added to Quick Settings
- Tap the tile
- **Expected:** Quick Search app opens

### 2.12 Updates & Feedback

#### TC-A-038: Check for Updates
**Preconditions:** App installed from Play Store
- Open app normally (or after some usage)
- **Expected:** If update available, in-app update prompt appears
- Tap "Update" (if available)
- **Expected:** App updates seamlessly without leaving

#### TC-A-039: In-App Review
**Preconditions:** App used multiple times
- Use app for several sessions
- **Expected:** After sufficient usage, review dialog appears
- **Expected:** Can rate 1-5 stars in-app
- Submit rating
- **Expected:** Rating submitted to Play Store

#### TC-A-040: Release Notes
**Preconditions:** New version installed
- Open app after update
- **Expected:** Release notes dialog shows what's new
- Tap "Dismiss" or "Got it"
- **Expected:** Dialog closes, app continues normally

#### TC-A-041: Send Feedback
**Preconditions:** Email app configured
- Go to Settings → scroll to bottom
- Tap "Send Feedback"
- **Expected:** Email app opens with pre-filled developer email
- Type feedback message
- Send email
- **Expected:** Email sent successfully

#### TC-A-042: Rate App
**Preconditions:** In settings
- Tap "Rate Quick Search"
- **Expected:** Play Store opens to app's page
- **Expected:** Can leave rating and review

---

## 8. EDGE CASES & ERROR HANDLING

Tests for boundary conditions, error scenarios, and unusual usage patterns.

### 3.1 Permission Edge Cases

#### TC-E-001: Revoke Permission While App Running
**Preconditions:** Contact permission granted, viewing contact results
- Open device settings in split-screen or background
- Revoke contact permission
- Return to Quick Search
- Search for a contact
- **Expected:** No contact results shown OR permission denied message
- **Expected:** App doesn't crash

#### TC-E-002: Permission Denied Then Granted
**Preconditions:** Contact permission denied
- Go to Settings → Permissions
- Tap "Grant Contact Permission"
- Deny permission in system dialog
- **Expected:** App handles gracefully, shows permission still denied
- Retry and grant permission
- **Expected:** Permission status updates, contacts now searchable

#### TC-E-003: No Permissions Granted Except Usage
**Preconditions:** Only usage permission granted
- Use app normally
- **Expected:** Only app search and settings search work
- **Expected:** No contact, file results shown
- **Expected:** No errors or crashes
- **Expected:** Web suggestions and search engines still work

#### TC-E-003-A: Error Banner Display
**Preconditions:** Network issues or API failures occur
- Trigger error condition (invalid API key, network failure during Direct Search)
- **Expected:** Error banner appears at top of search screen
- **Expected:** Error message is clear and actionable
- **Expected:** Search still functions for local results (apps, contacts, files)
- Resolve error condition (fix network, correct API key)
- **Expected:** Error banner disappears automatically
- **Expected:** Full functionality returns

### 3.2 Data Edge Cases

#### TC-E-004: No Apps Installed (Minimal Device)
**Preconditions:** Device with very few apps (theoretical)
- Open Quick Search
- **Expected:** Shows available system apps
- **Expected:** No crashes
- **Expected:** Empty search suggestions handled gracefully

#### TC-E-005: No Contacts
**Preconditions:** Contact permission granted, zero contacts on device
- Search for any name
- **Expected:** No contact results shown (not an error state)
- **Expected:** Other search types still work

#### TC-E-006: No Files Found
**Preconditions:** File permission granted, search for nonexistent file
- Type filename that doesn't exist
- **Expected:** No file results
- **Expected:** No error messages
- **Expected:** Other results (apps, search engines) still appear

#### TC-E-007: Storage Almost Full
**Preconditions:** Device storage nearly full
- Use Quick Search normally
- **Expected:** App functions without issues
- **Expected:** File search may be slow but doesn't crash
- **Expected:** No data corruption

#### TC-E-008: Very Large Contact List (1000+ contacts)
**Preconditions:** 1000+ contacts synced to device
- Search for a contact
- **Expected:** Results appear within reasonable time (< 2 seconds)
- **Expected:** Results are accurate despite large dataset
- Scroll through contact results
- **Expected:** Smooth scrolling, no lag

#### TC-E-009: Application Cache Corrupted
**Preconditions:** Manually corrupt app cache files or clear cache mid-operation
- Clear app cache from device settings
- Open Quick Search
- **Expected:** App rebuilds cache
- **Expected:** Apps still load (may take slightly longer first time)
- **Expected:** No crash or data loss

### 3.3 Search Query Edge Cases

#### TC-E-010: Extremely Long Query (200+ characters)
**Preconditions:** App launched
- Type or paste a very long search query (200+ chars)
- **Expected:** Search bar handles long text (may truncate display)
- **Expected:** Search still functions
- **Expected:** Results shown if any matches

#### TC-E-011: Special Characters in Query
**Preconditions:** App launched
- Type queries with special chars: "@#$%^&*()"
- **Expected:** App doesn't crash
- **Expected:** Search attempts to match (likely no results)
- Type calculator expression with special chars: "(2+2)*5"
- **Expected:** Calculator works correctly

#### TC-E-012: Emoji in Query
**Preconditions:** App launched, app with emoji in name exists
- Type emoji in search bar: "😊 🎉 🚀"
- **Expected:** Search processes without crashing
- If app name contains emoji, search for it
- **Expected:** App found correctly

#### TC-E-013: Multilingual/Non-Latin Characters
**Preconditions:** Apps or contacts with non-Latin names exist (Chinese, Arabic, Cyrillic)
- Search for app/contact in native script
- **Expected:** Correct results shown
- Search for same item using Latin characters
- **Expected:** May or may not match depending on implementation

#### TC-E-014: Query with Only Spaces
**Preconditions:** App launched
- Type multiple spaces in search bar
- **Expected:** Treated as empty query
- **Expected:** Shows app suggestions, not search results

#### TC-E-015: Rapid Query Changes
**Preconditions:** App launched
- Type query quickly, changing letters rapidly (e.g., "abcdefghijk" typed in 1 second)
- **Expected:** Search keeps up or debounces correctly
- **Expected:** No crashes or lag
- Final results match final query state

### 3.4 Network & API Edge Cases

#### TC-E-016: No Internet - Web Suggestions
**Preconditions:** Web suggestions enabled, airplane mode ON
- Type a query
- **Expected:** No web suggestions appear OR loading state never completes
- **Expected:** Local results (apps, contacts, files) still work
- **Expected:** No crash

#### TC-E-017: Slow/Timeout Internet - Web Suggestions
**Preconditions:** Very slow or intermittent network
- Type a query requiring web suggestions
- **Expected:** App times out gracefully after reasonable wait
- **Expected:** Shows local results without waiting indefinitely
- **Expected:** No ANR (App Not Responding)

#### TC-E-018: Invalid Gemini API Key
**Preconditions:** Direct search configured with invalid API key
- Search "dsh test query"
- **Expected:** Error message shown (invalid API key or request failed)
- **Expected:** App doesn't crash
- **Expected:** User can still use other search features

#### TC-E-019: Gemini API Rate Limit Exceeded
**Preconditions:** Gemini API rate limit reached
- Perform many direct searches rapidly
- **Expected:** Error message when rate limit hit
- **Expected:** Other search functions continue working
- **Expected:** App handles API error gracefully

#### TC-E-020: Network Lost During Direct Search
**Preconditions:** Direct search in progress, turn off internet mid-request
- Start a direct search query
- Immediately disable WiFi/mobile data
- **Expected:** Loading times out or shows network error
- **Expected:** App doesn't crash
- **Expected:** Can retry when network restored

### 3.5 UI & Interaction Edge Cases

#### TC-E-021: Extremely Fast App Launch & Close
**Preconditions:** App launched
- Tap an app in results
- Immediately double-tap or return to Quick Search rapidly
- **Expected:** App launches correctly, no duplicate launches
- **Expected:** No crashes from rapid state changes

#### TC-E-022: Rotate Device During Search
**Preconditions:** Auto-rotate enabled, mid-search
- Type a query
- Rotate device to landscape
- **Expected:** Query persists
- **Expected:** Results remain visible
- Rotate back to portrait
- **Expected:** UI adapts correctly

#### TC-E-023: Long Press on Non-Interactive Element
**Preconditions:** App launched
- Long press on empty space, dividers, or text labels
- **Expected:** No context menu appears
- **Expected:** No crash or unexpected behavior

#### TC-E-024: Rapid Scrolling Through Results
**Preconditions:** Many results visible (long list)
- Scroll very quickly up and down through results
- Fling scroll to extremes
- **Expected:** Smooth scrolling with no crashes
- **Expected:** Items render correctly
- **Expected:** No blank spaces or rendering errors

#### TC-E-025: Pin/Unpin Rapidly
**Preconditions:** App launched
- Pin an item
- Immediately unpin it
- Pin again
- Repeat rapidly 5-10 times
- **Expected:** State stays consistent
- **Expected:** No duplicate pins
- **Expected:** No crashes

#### TC-E-026: Exclude Then Search Immediately
**Preconditions:** App visible in suggestions
- Exclude an app from suggestions
- Immediately type the app name to search
- **Expected:** App excluded from suggestions but still in search results
- **Expected:** Consistent behavior, no conflicts

### 3.6 Settings Edge Cases

#### TC-E-027: Change Settings During Active Search
**Preconditions:** Search results visible
- With results showing, open settings
- Disable a section (e.g., Contacts)
- Return to search screen
- **Expected:** Contact results disappear immediately OR on next search
- **Expected:** No crashes

#### TC-E-028: Enable All Search Engines
**Preconditions:** In search engine settings
- Enable all 23 search engines
- Return to search screen
- Type query
- **Expected:** All engines visible/scrollable
- **Expected:** UI remains responsive
- **Expected:** Horizontal scroll works smoothly

#### TC-E-029: Disable All Search Engines
**Preconditions:** In search engine settings
- Disable all search engines
- Return to search screen
- Type query with no local results
- **Expected:** No search engines shown
- **Expected:** App doesn't crash
- **Expected:** Calculator and web suggestions may still appear

#### TC-E-030: Maximum Excluded Items
**Preconditions:** App launched
- Exclude 50+ apps, contacts, files, shortcuts
- Go to Settings → Excluded Items
- **Expected:** All items listed, scrollable
- **Expected:** Can manage/restore items
- **Expected:** No performance issues

### 3.7 File Handling Edge Cases

#### TC-E-031: File Deleted After Search
**Preconditions:** File in search results
- Search for a file
- Using another app, delete the file
- Tap the file in Quick Search results
- **Expected:** Error message (file not found) OR graceful handling
- **Expected:** No crash

#### TC-E-032: File Moved After Search
**Preconditions:** File in search results
- Search for a file
- Move file to different folder using file manager
- Tap file in Quick Search
- **Expected:** May not open OR opens from new location (if MediaStore updates)
- **Expected:** No crash

#### TC-E-033: File with No Associated App
**Preconditions:** File type with no default app (unusual extension)
- Search for file with rare extension
- Tap the file
- **Expected:** Android shows "No app can open this file" OR app chooser
- **Expected:** Quick Search doesn't crash

### 3.8 Contact Edge Cases

#### TC-E-034: Contact with No Phone Number
**Preconditions:** Contact with only email exists
- Search for contact
- Tap contact
- **Expected:** Only available actions shown (email, no call/SMS)
- **Expected:** No crash, UI adapts correctly

#### TC-E-035: Contact Deleted After Search
**Preconditions:** Contact in search results
- Search for contact
- Delete contact using Contacts app
- Return to Quick Search
- Tap the contact
- **Expected:** Contact not found error OR graceful handling
- **Expected:** No crash

#### TC-E-036: WhatsApp/Telegram Not Installed
**Preconditions:** WhatsApp/Telegram uninstalled
- Search for contact
- Tap contact actions
- **Expected:** WhatsApp/Telegram options hidden or disabled
- **Expected:** Only available messaging options shown

#### TC-E-037: Multiple Contacts with Identical Names
**Preconditions:** 3+ contacts with exact same name exist
- Search for the duplicate name
- **Expected:** All contacts shown in results
- **Expected:** Can differentiate by phone number or other details
- **Expected:** Tapping each opens correct contact

### 3.9 App Launch Edge Cases

#### TC-E-038: App Uninstalled After Search
**Preconditions:** App in search results
- Search for an app
- Uninstall app using another method
- Return to Quick Search
- Tap the app
- **Expected:** Error message (app not found) OR handled gracefully
- **Expected:** Quick Search refreshes and removes app from results

#### TC-E-039: Disabled App
**Preconditions:** System app that can be disabled
- Disable a system app from device settings
- Search for the disabled app in Quick Search
- **Expected:** App not shown in results OR shown as disabled
- Re-enable app
- **Expected:** App appears in search again

#### TC-E-040: App with Very Long Name (50+ characters)
**Preconditions:** App with extremely long name installed
- Search for the app
- **Expected:** App name displays correctly (may truncate with ellipsis)
- **Expected:** Can still tap and launch
- **Expected:** No layout issues

### 3.10 Calculator Edge Cases

#### TC-E-041: Division by Zero
**Preconditions:** App launched
- Type "10/0" in search bar
- **Expected:** Shows error message (infinity, undefined, or error)
- **Expected:** App doesn't crash

#### TC-E-042: Very Large Numbers
**Preconditions:** App launched
- Type "999999999*999999999"
- **Expected:** Calculates result or shows overflow error
- **Expected:** No crash

#### TC-E-043: Malformed Expression
**Preconditions:** App launched
- Type "2++" or "((5+3" (invalid syntax)
- **Expected:** No calculator result OR error message
- **Expected:** Doesn't crash

### 3.11 Widget Edge Cases

#### TC-E-044: Widget After App Uninstall
**Preconditions:** Widget on home screen
- Uninstall Quick Search
- **Expected:** Widget removed automatically OR shows unavailable
- Reinstall app
- **Expected:** Can add widget again

#### TC-E-045: Multiple Widgets with Different Settings
**Preconditions:** Home screen with space for 2+ widgets
- Add Quick Search widget
- Customize it (e.g., black background)
- Add second Quick Search widget
- Customize differently (e.g., white background)
- **Expected:** Both widgets maintain separate settings
- **Expected:** Tapping either opens Quick Search correctly

### 3.12 Search Engine Edge Cases

#### TC-E-046: Search Engine Shortcut Conflict
**Preconditions:** Multiple engines with similar shortcuts
- If two shortcuts are similar (unlikely but possible)
- Type ambiguous shortcut
- **Expected:** First matching engine OR error handling
- **Expected:** Consistent behavior

#### TC-E-047: Very Long Search Query for Engine
**Preconditions:** App launched
- Type 500+ character query
- Tap a search engine
- **Expected:** Browser opens with query (may be truncated by browser/URL limits)
- **Expected:** Quick Search doesn't crash

### 3.13 Memory & Performance Edge Cases

#### TC-E-048: Low Memory Situation
**Preconditions:** Device with low available RAM
- Open many apps to consume memory
- Open Quick Search
- Perform searches
- **Expected:** App may be slightly slower but functions
- **Expected:** No crash due to out of memory
- **Expected:** System may kill background apps, not Quick Search

#### TC-E-049: After Device Restart
**Preconditions:** App configured with preferences
- Restart device
- Open Quick Search
- **Expected:** All settings, pins, exclusions persist
- **Expected:** Cache rebuilds if necessary
- **Expected:** App functions normally

#### TC-E-050: Background App Kill
**Preconditions:** App in background
- Open Quick Search
- Switch to another app
- Use device heavily, causing system to kill background apps
- Return to Quick Search
- **Expected:** App restarts cleanly
- **Expected:** State may reset (query cleared) but no data lost
- **Expected:** Settings and data intact

---

## Test Execution Notes

### General Testing Guidelines

1. **Test on Multiple Devices**: If possible, test on:
   - Different Android versions (minimum API 24 / Android 7.0+)
   - Different screen sizes (small phone, large phone, tablet)
   - Different manufacturers (Samsung, Google Pixel, OnePlus, etc.)

2. **Test Both Fresh Install and Update**: Some issues only appear after updates, not fresh installs.

3. **Vary Permissions**: Test with different permission combinations to ensure graceful degradation.

4. **Monitor Performance**: Watch for:
   - ANR (App Not Responding) dialogs
   - Slow operations (> 2 seconds for searches)
   - Memory leaks (use Android Profiler if possible)
   - Battery drain

5. **Check Logs**: For any failures, check Logcat for errors/warnings.

### Priority Rankings

- **P0 (Must Pass)**: Core Search & Launch tests (Section 1)
- **P1 (Should Pass)**: All functional area tests (Sections 2-7)
- **P2 (Nice to Pass)**: Edge Cases & Error Handling (Section 8)

### Suggested Testing Workflow

**Before Each Release:**
1. Run all Core Search & Launch tests (Section 1)
2. Run key tests from each functional area (2-7)
3. Spot-check 5-10 Edge Cases (Section 8)
4. Test on at least 2 different devices

**Major Releases (e.g., 2.0):**
1. Run all tests in Sections 1-7
2. Run all Edge Cases (Section 8)
3. Perform exploratory testing for 30+ minutes

**Minor Updates/Hotfixes:**
1. Run tests for affected functional areas
2. Run relevant Edge Cases for changed features
3. Smoke test: Quick run through basic flows

---

## Appendix: Common Test Data

### Sample App Names to Search
- Chrome, Gmail, Maps, YouTube, WhatsApp, Instagram, TikTok, Spotify, Calculator

### Sample Contact Names
- John Doe, Jane Smith, Test User

### Sample File Names
- document.pdf, photo.jpg, video.mp4, notes.txt

### Sample Settings
- WiFi, Bluetooth, Display, Battery, Sound, Location

### Sample Calculator Expressions
- 2+2, 10*5, 100/4, (2+3)*4, 15-8

### Sample Search Engine Shortcuts
- ggl (Google), cgpt (ChatGPT), ytb (YouTube), gmi (Gemini), dsh (Direct Search)

### Sample Fuzzy Search Queries
- "chr" for Chrome, "chrmoe" for Chrome, "face" for Facebook
- "calc" for Calculator, "msgs" for Messages

### Sample Ranking Test Apps
- Chrome (starts with), Calculator (contains), Clock (second word starts)

---

**Document Version:** 1.2
**Last Updated:** January 21, 2026
**Total Test Cases:** 96 (organized by 8 functional areas)
