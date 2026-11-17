# Quick Search Design System

Where shared UI values live and how to use them. The code is the source of truth. Check the
files below for exact values rather than copying them from here.

Theme files: `app/src/main/java/com/tk/quicksearch/shared/ui/theme/`
Shared components: `app/src/main/java/com/tk/quicksearch/shared/ui/components/` and `settings/shared/`

## Rules

- Use `DesignTokens` for spacing, shapes, sizes, elevation, and animation durations instead of
  raw `dp`/ms values when a token fits. Add a token when a value is reused across files.
- Use `AppColors` for colors when a token exists, and fall back to `MaterialTheme.colorScheme`.
  Avoid hex literals so wallpaper, custom image, AMOLED, and accent modes stay correct.
- Reuse shared components before building a new dialog, sheet, popup, or settings row.
- Every shared surface must work over a theme gradient, system wallpaper, custom image, and
  AMOLED black, in light and dark, and in overlay, one-handed (bottom search bar), and tablet
  layouts.

## Theme (`Theme.kt`)

`QuickSearchTheme` wraps every surface. It resolves:

- **App theme** (`AppTheme`): `FOREST`, `AURORA`, `SUNSET`, `MONOCHROME`. Per-theme accents,
  gradients, and themed icon colors live in `ThemeColorRegistry`.
- **Mode** (`AppThemeMode`): light, dark, or system.
- **Background** (`BackgroundSource`): `THEME` gradient, `SYSTEM_WALLPAPER`, or `CUSTOM_IMAGE`.
- **Accent** (`AccentColorMode`): `NONE`, `FROM_WALLPAPER` (derived from the image), or `CUSTOM`.
  Device dynamic colors (Material You, Android 12+) override these when enabled.
- **Font**: font scale multiplier and system-font toggle.

It exposes composition locals (in `AppColorPalette.kt`) for branching on this state, such as
`LocalAppIsDarkTheme`, `LocalAppTheme`, `LocalIsSystemWallpaperActive`,
`LocalImageBackgroundIsDark`, `LocalDeviceDynamicColorsActive`, `LocalAmoledThemeActive`, and
`LocalSearchColorTheme`. Prefer an existing `AppColors` token over branching on these directly.

## Colors

| File | Contents |
|---|---|
| `AppColors.kt` | Semantic, theme-aware tokens. Use these in composables. |
| `AppColorPalette.kt` | Light/dark palette backing `AppColors`, scrim/alpha constants, composition locals |
| `Color.kt` | Default accent (`AppAccentLight`/`AppAccentDark`) and base Material 3 schemes |
| `ThemeColorRegistry.kt` | Per-`AppTheme` accents, gradients, themed icon colors, Material You tone slots |
| `HomeTextColor.kt` | `homeTextColor()`, which applies the user's Home text override or else a wallpaper-derived foreground; `homeBackgroundIsDark()`, the same choice as a dark/light flag (null without an image background), for anything that picks white or black against the wallpaper |
| `Theme.kt` (`HomeTextContrastTheme`) | Wraps the search content area in Compact Layout (not the overlay). Over a wallpaper or image, when `homeBackgroundIsDark()` doesn't match the app theme, it swaps in the other theme's text, surface and outline roles (and `LocalAppIsDarkTheme`/palette), and trades `primary` with `primaryContainer` so accents stay legible |

Main `AppColors` groups:

- **Accent:** `Accent`, `OnAccent`, `LinkColor` (stays blue in monochrome so links read as
  tappable), `ItemMenuActiveIconTint`, `IconTintPrimary`/`IconTintSecondary`.
- **Search bar and chrome:** `getSearchBarBackground(...)`, `getSearchBarTextAndIconColor(...)`,
  `SearchChromeOutlineBorder`, `KeyboardPill*`, `InlineEngineHighlight*`,
  `InlineEngineDivider`.
- **Wallpaper surfaces:** `ResultCardWallpaperBackground`, `CompactSectionBackground`,
  `WallpaperText*`, `WallpaperDivider`, `WallpaperDarkCardBorder`,
  `wallpaperAwareMutedSearchForeground(...)`.
- **Home and result cards:** resolve colors only through `SearchResultCardDefaults`
  (`searchScreen/shared/SearchResultCard.kt`): `colors(...)`/`containerColor(...)` for card and
  segment-tile containers, `border(...)`, and `dividerColor(...)` for row, section and Compact
  Layout dividers. They apply the overlay theme tint (`LocalOverlayResultCardColor`,
  `LocalOverlayDividerColor`), then the wallpaper surface, then the Material/theme default, so
  don't re-derive these per component. Content inside a card (e.g. the history tab row) stays
  transparent.
- **Frosted cards:** over a wallpaper or custom image on Android 12+ (not low-RAM devices),
  result cards, segment tiles, permission cards, the widgets panel's Quick Note card and widget
  loading placeholder, and the bottom chrome surfaces that use
  `insetCardBackdrop` (search bar, compact engine strip, number keyboard strip) show a blurred copy of the wallpaper behind
  them (`searchScreen/FrostedCardBackdrop.kt`). The search background bakes one small blurred
  bitmap when the wallpaper or blur setting changes; `Modifier.frostedCardBackground(shape)` draws
  the matching region under the card's container, and `SearchResultCardDefaults.containerColor`
  lowers the scrim by `FrostedResultCardScrimFactor` while it is active. New home/result card
  surfaces that draw their own container should add the modifier before their background. No
  frost with the system wallpaper backdrop (Android draws that wallpaper, so the app has no copy).
- **Search chrome blur:** the search bar and bottom chrome (`insetCardBackdrop` /
  `insetCardBackground`) are solid by default: no frosted wallpaper or blurred results behind them,
  and their translucent colour is flattened onto `surfaceContainer` (black for AMOLED). The
  "Blurred Background" toggle in the Appearance search bar card (`LocalSearchChromeBlurEnabled`) restores the
  frosted chrome. Result cards are unaffected.
- **Settings and dialogs:** `SettingsCardBackground`, `SettingsText`, `SettingsIconTint`,
  `DialogBackground`, `DialogText`, `Overlay*`.
- **Contact actions:** `ActionPhone`, `ActionSms`, `ActionWhatsApp`, `ActionTelegram`,
  `ActionSignal`, `ActionEmail`, `ActionVideoCall`, `ActionCustom`, `ActionView`.
- **Widgets** (RemoteViews, not theme-aware): `Widget*`.

To change the default brand accent, edit `AppAccentLight`/`AppAccentDark` in `Color.kt`.

## DesignTokens (`DesignTokens.kt`)

**Spacing** (4dp grid): `SpacingXSmall` 4, `SpacingSmall` 8, `SpacingMedium` 12,
`SpacingLarge` 16, `SpacingXLarge` 20, `SpacingXXLarge` 24, `Spacing28`, `SpacingHuge` 32,
`Spacing40`, `Spacing48`. (`SpacingXXSmall` is also 4dp.)

**Semantic spacing:** `ContentHorizontalPadding`, `Card*Padding`, `ItemRowSpacing`,
`TextColumnSpacing`, `Chip*`, `Section*Padding`, `Onboarding*`. Helpers: `singleCardPadding()`,
`headerPadding()`, `cardItemTopPadding(isFirst)`, `cardItemBottomPadding(isLast)`.

**Shapes:** `ShapeExtraSmall` 4, `ShapeSmall` 8, `ShapeMedium` 12, `ShapeLarge` 16,
`ShapeXLarge`/`ShapeXXLarge` 28, `ShapeFull` (circle). Semantic shapes:

- `CardShape` (12dp)
- `ExtraLargeCardShape` (28dp)
- `SearchResultCardShape`: all search result cards. Change it here to retune them together.
- `WidgetPanelCardShape` (28dp): the widgets panel.

**Action menus:** `ItemMenuMaxWidth` caps floating popups at 640dp; `ItemMenuRowHeight`
sets a 56dp minimum action-row height. Result action tiles use a fast expressive spring
to round their corners from 20dp to 8dp while pressed. Settings-like rows use the existing
segmented-list corner and gap tokens; secondary buttons use `ShapeFull`. Quick-action tiles and secondary buttons share
`surfaceContainerHighest`; destructive buttons keep that neutral fill with error-colored
text and icons for a restrained warning. `ItemMenuActiveIconTint` preserves the chosen
accent while strengthening it toward `onSurface` until it reaches 4.5:1 contrast across
the neutral popup surfaces. Shared long-press popups for apps, files,
shortcuts, and contacts use 8dp between the header/action groups and buttons, with
2dp between grouped list rows. Their title row has 8dp above and below it, using
`AppBottomPopup.headerTopPadding` to keep the top inset independent of the footer.
Contact action popups reuse `ItemMenuTileButton`
for call/message/provider actions, retain provider icon colors, and use segmented rows
for remaining methods. Contact action tiles and the number selector use 8dp gaps; the grouped method
list uses the 2dp segmented-list gap. Their phone-number selector uses 48dp navigation targets on a
`surfaceContainerHigh` surface. The contact actions shell uses 20dp top corners,
28dp bottom corners, and a 16dp header top inset to balance its circular avatar.
It enables `AppBottomPopup.animateSizeChanges` so switching numbers smoothly resizes
the shell with the default expressive spring, keeping its bottom edge anchored.

**Sizes:** `IconSizeSmall` 20, `IconSize` 24, `LargeIconSize` 28, `IconSizeXLarge` 52,
`AppIconSize` 64, `BorderWidth` 1, `DividerThickness` 0.5, `ButtonCornerRadius` 24.

**Elevation:** `ElevationLevel0`–`ElevationLevel5` (0, 1, 3, 6, 8, 12dp).

**Motion:** `AnimationDurationMicro` 50, `AnimationDurationShort` 200,
`AnimationDurationMedium` 300, `AnimationDurationFast` 500, `AnimationDurationLong` 4000;
drag/spring constants (`DragAlpha`, `SpringDampingRatio`, `SpringStiffness`).
Material 3 Expressive springs: `expressiveFastSpatialSpring()` for small shape changes (a pressed
button squaring its corners) and `expressiveDefaultSpatialSpring()` for moves and rotations (a
section chevron turning). Material3 1.4 keeps `MotionScheme` internal, so use these instead.

**Segmented lists:** `SegmentedListOuterCorner` 28, `SegmentedListInnerCorner` 4, `SegmentedListGap` 2,
`SegmentedListTileHorizontalPadding`, `SegmentedListTileVerticalPadding`.

**Search field:** `SearchField*` border widths, alphas, and gradient multipliers.

## Typography (`Type.kt`)

`quickSearchTypography(useSystemFont)` returns the app `Typography`. Use
`MaterialTheme.typography.*` styles rather than ad hoc `TextStyle`s. The font is flavor-specific
(`DistributionTypography.kt`): `standard` bundles Google Sans, `fdroid` uses the system font. Check
both flavors when changing typography.

## Icons (`shared/ui/icons/MaterialSymbols.kt`)

UI icons are Material Symbols Rounded (weight 400, grade 0, 24dp), filled by default; a name ending
in `Outlined` is the unfilled variant. Use `MaterialSymbols.<Name>` wherever an `ImageVector` is
expected; don't add `material-icons-*` back. Directional icons (arrows, send, label, missed call)
set `autoMirror` so they flip in RTL. To add one, copy its path data from
[fonts.google.com/icons](https://fonts.google.com/icons) with the same style settings and add a
property in sorted order.

## Shared components

| Component | Use for |
|---|---|
| `AppAlertDialog` | All alert dialogs; Material 3 layout and `surfaceContainerHigh` tonal surface. Confirm and dismiss actions are both `TextButton`s (error-tinted for destructive confirms); keep the default `headlineSmall` title and `bodyMedium` `onSurfaceVariant` text |
| `AppDropdownMenu` | All popup (three-dot / long-press) menus: 28dp corners, `surfaceContainer` tonal surfaces, 3dp shadow, non-focusable by default. Long-press menus over result items use the same tonal role through `itemMenuContainerColor`. Put `AppDropdownMenuDivider` between items. `AppDropdownMenuItem` is the text + icon row, with optional `supportingText` for the current value (Default Browser in Bookmarks) and optional `iconTint` (`AppColors.ItemMenuActiveIconTint` marks an assigned folder swipe action); `destructive = true` tints it with the error color for bulk or irreversible actions (Delete All), which must still confirm with an `AppAlertDialog`. Never call `DropdownMenu` directly |
| `AppUndoSnackbar` | Undo after a removal (hide, exclude, delete). `rememberUndoSnackbarController()` shows one snackbar at a time (a new one replaces the visible one) and runs the undo callback only on Undo. Place `AppUndoSnackbarHost` in a screen's root overlay (search route, overlay); settings pages without one use `AppUndoSnackbarPopup`. Inside search the route provides `LocalShowUndoSnackbar`. It uses the Material 3 snackbar roles (`inverseSurface`/`inverseOnSurface`, 8dp corners), with the action in `primaryContainer` (the inverse theme's accent tone, since the baseline `inversePrimary` isn't derived from the accent), and lifts above whatever is marked with `Modifier.undoSnackbarAvoidArea()` (the search screen's bottom chrome) through `LocalUndoSnackbarAnchor`, so it never covers the bottom search bar |
| `AppBottomSheet` | Modal bottom sheets (swipe/scrim/back dismissal handled) |
| `AppPickerDrawer` | Pick-one-item drawers (Select Action, Select Model, folder, language, calendar app and gesture pickers): title with close button, optional `titleIcon` before the title (the app's icon in the App notifications keywords drawer), optional pill search field (`AppPickerDrawerSearch`, `autoFocus` when the list is empty until typing; `onSubmit` turns it into an entry field with an add icon, `ShapeLarge` corners, and a Done key that submits, as in the App notifications keywords drawer), optional `header`/`footer`. Fixed 85% height with search, wraps short lists without. Build lists from `AppPickerDrawerRow` (no dividers; space rows with `AppPickerDrawerRowSpacing`). A `selected` row gets an accent tint and a plain accent check unless a custom `trailing` replaces the check. Leading visuals: `AppPickerDrawerAppIcon(packageName)` for apps, `AppPickerDrawerIconBadge(icon)` (accent-tinted rounded square) for everything else, both `AppPickerDrawerLeadingSize`. Use `AppPickerDrawerMessage` for hints/empty states. Custom items (such as the adaptive language radio tiles) reuse `AppPickerDrawerRowShape`, `appPickerDrawerItemBackground(selected)` and `AppPickerDrawerSelectedCheck` so selection looks the same. Prefer it over an `AppAlertDialog` with a radio list |
| `AppBottomPopup` | Bottom-anchored popup with header and scrollable options; safe drawing insets, 640dp maximum width, 48dp close target, and expressive spring drag recovery |
| `ItemMenuPopup` | Long-press action menus: tonal quick-action tiles, segmented option rows, pill buttons, and long-press dropdowns on a `surfaceContainerLow` shell. Never leave placeholder gaps between options; reflow so any gap falls only at the end of the last row. |
| `TipBanner` | Dismissible tips/hints, optional links and trailing action |
| `AppPill` | Small pill labels, with an optional accent-tinted `leadingIcon`; `compact` for tight labels. Non-interactive file metadata uses plain `bodySmall` supporting text (`onSurfaceVariant`), with type and parent folder separated by a middle dot; filenames use `bodyMedium` (`onSurface`). File rows use 12dp vertical padding (halved in Compact Layout), matching the density of other two-line result rows |
| `AppVoiceCallIcon` | App logo plus phone icon for call actions |
| `dialogTextFieldColors()` | Text fields inside dialogs |
| `CardTextField`, `cardTextFieldColors()` | Borderless text fields stacked as rows of a `SettingsCard` (divided by `HorizontalDivider`); dimmed placeholder that shows before focus (label stays floated when a placeholder is set), and the floated label keeps a gap above the input |
| `SettingsCard` and `Settings*Row` (`settings/shared/`) | Settings cards, toggle/checkbox/navigation rows. `SettingsToggleRow` takes `subtitleTextStyle` (default `bodyMedium`, the Material 3 list supporting-text style), a `trailingAction` placed after the switch, such as a delete button, and `showNavigationChevron`, which with `onRowClick` adds the chevron and divider of `SettingsNavigationToggleRow` before the switch |
| `SettingsSegmentGroup`, `SettingsSegment`, `SettingsSegment*Row`, `SettingsIconBadge` (`settings/shared/SettingsSegmentedList.kt`) | Material 3 Expressive segmented settings lists, used by the main Settings page, Appearance, Gestures, Launch options, More options, Permissions, and At a Glance (also its widget drawer). `SettingsSegmentGroup` accepts an optional section title (accent `titleSmall`, marked as an accessibility heading) and `bodyMedium` description above the tiles. Inside a tile `LocalSettingsSegmented` is true: `SettingsToggleRow` drops its divider and `extraVerticalPadding`, uses 20dp sides and 16dp top/bottom, leads with a tonal icon badge, and shows the check thumb. Search Results count sliders sit below their switch rows to use the full tile width; app result row counts use `SettingsChoiceGroup`, and Choose Tabs is a separate navigation tile. Refresh Data retains its card with side-by-side icon-above-label actions, using filled tonal pill buttons. Free-form content (sliders, pickers) goes in `SettingsSegmentContent`. Each row is its own tile on `getSettingsCardContainerColor()` (28dp outer, 4dp inner corners, 2dp apart; a pressed tile rounds its inner corners to 16dp on `expressiveFastSpatialSpring()`). Rows lead with a 40dp circular accent-tinted `SettingsIconBadge` (brand drawables keep their colors) and drop the chevron; `SettingsSegmentToggleRow` toggles from the whole tile and shows a check in the switch thumb. `containerColor` overrides the tile color: dialogs list items as tiles too (section reorder, icon pack), tinting a selected tile with the accent and a dragged one fully round with a stronger accent tint. `SettingsNavigationToggleRow` in a tile takes the same layout (badge, 16dp padding, bodyMedium supporting text); Search Results (result types, Top Matches, and options), Manage Apps (app tiles with a tonal selected state, app icons, and separate uninstall checkboxes; its compact app-details dialog puts the package name under the header, pairs version and SDK fields in columns, and separates the groups with subtle dividers; labels are muted and App Info stays tonal), Search Engines, Tools, tool examples (`ToolExamplesSection`), the AI tool editor, and `ModelFeatureSettingsCard` are tiled (`ModelFeatureSettingsCard` is a model tile with an `AiProviderBadge`, then Thinking and Web Search as `SettingsSegmentToggleRow` switch tiles; the model drawer leads each row with the same badge only when several providers are set up), and so are AI Provider (Configure API key, Personal Context, the custom provider's advanced payload) and API Key Setup (`ApiKeyTile`: one tile per provider with its badge and name; with no key the tile pastes the clipboard and says so in the accent, a pasted key shows Clear and Save, a saved key shows its last four and a delete button; Add Custom Provider expands as the group's last tile; Tavily is its own group whose tile carries its explanation, with its mode as a `SettingsChoiceGroup` tile). Three-line tiles (Tavily, Add Search Engine while open) top-align their badge, per Material 3 (`SettingsSegmentRow(verticalAlignment = Alignment.Top)`). Add Search Engine is the same kind of collapsed tile (+ badge, chevron) as Add Custom Provider: opening it shows the URL instructions under its title, scrolls the card into view once expanded, and it collapses again after saving. The Permissions page and onboarding's permission step use segmented `PermissionCard` rows with tonal badges, full-row permission requests, and granted status icons (onboarding caps the list at `OnboardingPermissionCardViewportHeight`, four rows plus a half-row peek, and fades its scrolled edges); onboarding's final setup uses `SettingsSegmentToggleRow` tiles for folders and file types. Onboarding's Restore Settings step centres its icon, text, accent progress indicator, and Import/Restore button in a single `SettingsSegment` tile above the outlined Skip button, which crossfades to Start Searching after a restore. Onboarding actions use `OnboardingActionButton` (`onboarding/OnboardingActionButton.kt`): a Material 3 Expressive medium button (56dp, `titleMedium`, accent fill or outlined) that is fully round and squares on press via `settingsFloatingActionShape`; each step's bottom action uses `OnboardingBottomActionButton`. Step titles are accessibility headings, and final setup's File types title is the `SettingsSegmentGroup` title. Export data uses checkbox tiles in `AppPickerDrawer`, with Settings always included and export/cancel actions in its fixed footer. App Shortcuts uses one segmented group per app: a 40dp app icon in the header, spring-animated expansion and chevron, and separate shortcut, app-wide switch, and add-action tiles with tonal add buttons. Calls & Texts uses segmented switch tiles, a preview rendered with the real `ContactResultRow` and two action-picker tiles (the fallback explanation sits inside the last tile); contact actions open in `AppPickerDrawer` and keep their provider icons. Files & Folders uses segmented switch tiles, wrapping removable `InputChip` extensions, and one folder-filter card containing its explanation, allow/block sections, tonal add buttons, and path rows; its folder picker uses `AppPickerDrawer`. Calendar Events and Reminders use segmented preference and management-list tiles with tonal icon badges; their shared floating actions and picker drawers remain in use. Calendar preferences stay fixed above the event list; both calendar and reminder list viewports clip to `ShapeXXLarge` so their visible top and bottom corners stay rounded while scrolling. Notes uses a segmented Quick Note navigation tile (visibility is managed in the widgets panel) and note tiles with tonal icon badges, selected-state tint, and a rounded list viewport; its search/new actions reuse `CalendarEventsBottomBar`, and bulk delete uses the shared floating-action height and animated shape. Bookmarks uses segmented website tiles with 40dp tonal favicon slots and a rounded list viewport; its empty-state import action uses a segmented row with a tonal icon badge. Device Settings uses segmented navigation tiles with tonal settings badges, wrapping titles/supporting text, and a rounded lazy-list viewport. Section priority (Manage Top Matches and section order) uses `AppPickerDrawer` with a rounded lazy-list viewport, compact switch rows, and 48dp drag handles; dragging scrolls the list and saves the order when released. Search Result Ranking uses `AppPickerDrawer` with tonal option badges and a selected check; Choose Tabs uses the same drawer with segmented switches, keeping Pinned always enabled. Triggers and Hidden Items use segmented rows with 40dp tonal icon slots, wrapping labels, and rounded lazy-list viewports beneath their descriptions; their host provides bounded height instead of another vertical scroller. Home At a Glance uses the shared result segments (28dp outer corners, 4dp inner corners, 2dp gaps), including groups hosted with today’s calendar events; Compact Layout retains its divider rows, and media controls keep their own card. The unified pinned list uses the shared result segments and theme-aware result surfaces, preserving mixed-item order and each item’s actions; Compact Layout retains flat rows and dividers. App suggestions use a transparent Material secondary scrollable tab row with all enabled categories, text-only selection (full foreground for the selected title, 50% for the others, animated), centered 48dp tap targets, and equal vertical padding without a background or underline. Shared app icons keep their ripple and gently scale to 94% while pressed (or held before a pinned drag); drag/merge presentation remains separate. App, shortcut, and folder grid labels share a 4dp icon gap and `labelMedium` in the regular grid (`labelSmall` in overlay). The widget panel uses a headline header with tonal add/done controls, 28dp widget/Quick Note corners, and the shared picker drawer with segmented app/preview tiles. Add Widget tiles use `surfaceContainerHighest` (visible on the drawer's `surfaceContainerHigh` sheet) rather than the wallpaper settings-card color; an expanded app header animates to `secondaryContainer` and its chevron sits in a 32dp circular container. Each widget tile is tappable (pressed corners morph) and shows its preview on a 176dp inset `surfaceContainerLow` stage with 20dp corners, then a `titleMedium` name, a `secondaryContainer` size pill, and a tonal Add button. Hosted widgets send exact size options only when they differ; the legacy size API is not called afterward, preserving responsive size metadata and avoiding redundant provider updates. Edge Gesture and Floating Button use a switch tile plus an accessibility tile, then `SettingsChoiceTile` choices and slider-only tiles (`SettingsSliderTile`); Nicknames, Notification History (rounded lazy-list viewport, swipe-to-delete tiles), Tasker (add form tile, saved intent tiles), Refresh Data, the messaging-app picker (accent-tinted option chips, outlined when selected) and the Custom Info editor (title/prompt tiles with a tonal Preview button, preview tile, schedule tiles) are tiled too. `SettingsCard` remains only as the shared component for any future card layout |
| `AliasPill` (`settings/shared/AliasPill.kt`) | Compact alias chip under a row's title (24dp, 8dp corners, labelMedium, 14dp icons, no border). Both states use a 12% accent tonal fill so they read as tappable: a set alias shows the code and a remove button, "Setup Alias" the action label. Kept quiet on purpose so it never competes with the row title; it opts out of the 48dp minimum touch size so it doesn't pad the row. `ToolExamplesSection` shows example queries as bodyMedium text on an 8% onSurface fill (8dp corners, no border). |
| `SettingsChoiceGroup`, `SettingsChoice`, `SettingsChoiceTile`, `SettingsTonalButton` (`settings/shared/SettingsChoiceGroup.kt`) | Material 3 Expressive connected button group for a single choice inside a settings tile (Home text colour, app columns, theme mode, accent, search bar colours, compact engine rows), and on app-setting search results (Home text colour and theme mode below the subtitle, theme mode without icons or a chevron so its three labels have room (tapping the row still opens Appearance); app columns and app result rows trailing). The AI model result's Thinking and Web Search toggles are `FilterChip`s in the same accent fill. Buttons are 48dp, 2dp apart, fully round at the group ends with 8dp inner corners (4dp pressed); the selected one fills with `AppColors.Accent`/`OnAccent` and morphs fully round. Unselected buttons are a 14% accent tint so they read over wallpaper, AMOLED, and Material You. Options take an icon or colour swatch; plain options show a check when selected. Buttons share the row in proportion to their labels (plain options reserve the check slot so selection never shifts them; when the row is too narrow for that, only the selected plain option gets the check slot and the widths animate on selection), and labels stay on one line, shrinking to 11sp; past that they wrap at 11sp and the buttons grow taller. Labels never ellipsize. Built on stable APIs because material3 1.4 keeps `ToggleButton`/`ButtonGroup` internal; switch to them once 1.5 is stable. `SettingsChoiceTile` is a segmented tile with a `titleMedium` heading over a full-width group (edge gesture side and activation). `SettingsTonalButton` is the tonal action (Reset, Edit, Delete) in the same accent tint, wrapping rather than ellipsizing its label, `small` for buttons beside text. |
| `WidgetSettingTile`, `WidgetColorChoiceGroup`, `WidgetSaveButton` (`widgets/widgetConfigScreen/components/WidgetSettingTiles.kt`) | Widget configure screens (search, custom buttons, media controls, note, At a Glance, countdown, world clock) use the expressive segmented tiles. `WidgetSettingTile` is a `SettingsSegment` with an optional `titleMedium` heading and description over free-form content; related settings share a `SettingsSegmentGroup` (theme + border color; background + text color; radius, border, transparency and an optional leading font-size tile in `WidgetSlidersSection`; the search bar's icon, mic, Show Label switch, text color and icon size). Single choices use `SettingsChoiceGroup`; `WidgetColorChoiceGroup` adds a Custom option that shows a palette icon until a color is picked, then that color's swatch, and opens `WidgetColorPickerDialog`. White and Black options (text, icon and border colours) and the Media Controls Light/Dark theme use sun and moon icons. Slider labels are `titleMedium` with the value in the accent. Theme color chips use 16dp corners and an accent ring and check when selected. The selected note and the note picker use note tiles with a tonal badge (selected tint in the picker); world clock locations are tiles with a globe badge plus an Add tile; Countdown's date is a navigation tile and its formats are accent-filled chips; custom-button slots are accent-tinted rather than outlined. `WidgetSaveButton` is the accent full-width Save at the bottom of each screen, using the settings floating-action height and press-morphing shape |
| `SettingsFloatingActionHeight`, `settingsFloatingActionShape`, `SettingsFloatingTonalContainer` (`settings/shared/SettingsFloatingActions.kt`) | Floating actions at the bottom of settings pages (`CalendarEventsBottomBar`: At a Glance, Reminders, Bookmarks; Manage Apps uses the same height and animated shape for its uninstall action). Both buttons are 56dp and fully round, morphing to 16dp corners while pressed. The main action is `Accent`/`OnAccent`; the secondary (Search) is an opaque 24% accent tint over `surfaceContainerHigh`, since it floats over the list. |
| `SettingsNestedCheckbox` (`settings/shared/SettingsNestedCheckbox.kt`) | Sub-option under a settings toggle (e.g. At a Glance "tomorrow" options): checkbox and label on a 16dp-rounded 8% accent panel that toggles as a whole. |
| `InformationCardFooter`, `InformationCardFooterAction`, `InformationCardCopyAction` (`searchScreen/shared/InformationCardFooter.kt`) | Material 3 card action area on the bottom edge of every `InformationCard` (calculator, unit converter, date calculator, color visualizer, AI answers and AI tools). A full-bleed band tinted `onSurface` at 6% over the card container (no fill in Compact Layout), at least 48dp tall: the result's source at the start, aligned with the 16dp body inset (outlined tool icon and name, or the AI provider wordmark, both `onSurfaceVariant` `labelMedium`), and standard `IconButton` actions at the end (outlined 20dp icons, since filled icons signal a selected state in Material 3; `onSurfaceVariant`, 48dp touch targets) each labelled by a `PlainTooltip`. Copy swaps to a check mark for 1.5s. The tappable provider wordmark gets a ripple and 48dp target. When the card has a minimum height, the footer is pinned to the bottom edge |
| `SearchResultCard` (`searchScreen/shared/`) | Every home and result card (sections, history, suggestions, engine cards, tools and AI answers via `InformationCard`). When the Compact Layout setting is on (`LocalSearchCardLayoutEnabled` is false, provided in `SearchScreen`), it drops its container color and border so content sits on the screen background at the width of section titles and the app grid, and draws a divider under each card in search results (not on Home, via `LocalCardlessSectionDividers`). A list that stacks cards with a gap other than `SpacingMedium` must provide `LocalCardlessStackSpacing` so those dividers stay centred; new home/result cards must use it so they follow the setting |
| `ResultSegment`, `ResultRowDivider`, `ExpandButtonSegment` (`searchScreen/shared/SegmentedResultRows.kt`) | Material 3 Expressive segmented lists. A section passes `segmented = true` to `ExpandableResultsCard`; with cards on, each row is its own tile (28dp outer, 4dp inner corners, 2dp apart, the card's container color) and the More button is the last tile. Wrap rows in `ResultSegment(index, count)`, use `ResultRowDivider` (drawn only when not segmented) and `resultRowsContainerPadding()` for the list's own padding. In Compact Layout and sections that don't opt in, rows stay on one card with dividers. Opted in: files, contacts, app shortcuts, device settings, app settings, notes, reminders, bookmarks, search history (its expanded tabs, Clear All History and More History are tiles too). Top Matches (consecutive result rows; the app grid and Screen Time stay separate cards) and web suggestions build the same tiles from `segmentShape()` on `SearchResultCard` with the outline dropped |
| `ExpandButton`, `CollapseButton` (`searchScreen/components/ExpandCollapseButtons.kt`) | A section's More and Collapse actions: a leading chevron and `labelLarge` label in `primary`. In segmented sections the whole last tile is the More button (`ExpandButtonSegment`, like Clear History); other card sections use a `TextButton`. In Compact Layout the More button is a full-width, edge-to-edge list row (48dp minimum, ripple to the screen edges) with the same chevron and `labelLarge` label; `ExpandButtonSegment` falls back to it outside segmented lists. Collapse floats over the expanded section as a 48dp pill on the result card surface, squaring its corners while pressed (`settingsFloatingActionShape`) |
| `OpenKeyboardAction` (`searchScreen/components/Pills.kt`) | Home's bottom keyboard strip: a Material 3 Expressive connected button group (voice search + Open Keyboard, 2dp apart, pill outer and 8dp inner corners that square on press), filled with the strip's foreground color at 10% (`onSurface` on wallpaper and Material You, white or black otherwise) so it stays neutral over any background |
| `HomeSectionHeaderRow` (`searchScreenLayout/`) | Home section titles. The expand chevron is one icon that rotates on `expressiveDefaultSpatialSpring()`, a bare `primary` icon with no background |
| `GlanceStatusRow` (`searchScreenLayout/`) | Home At a Glance rows: icon, title, subtitle or `belowText`, optional pill, trailing action, dismiss |
| `KeyboardSwitchPill`, `PhoneCallPill`, `SetAlarmPill`, `StartTimerPill`, `CreateReminderPill`, `AllAppsPill` (`searchScreen/components/Pills.kt`) | Search quick actions share `QuickActionButton`: a Material 3 Expressive extra-small button (32dp, `labelLarge`, 16dp leading icon, symmetric 16dp horizontal padding so content stays centered, fully round corners that square to 8dp on press with `expressiveFastSpatialSpring()`). Filled tonal (`secondaryContainer`/`onSecondaryContainer`); the search quick actions draw the container at 50% opacity, while `AllAppsPill` keeps it opaque. The quick action row stays on one line: buttons share the width (`weight(1f, fill = false)`) and labels auto-shrink to 10sp instead of wrapping or ellipsizing; with three or more buttons the icons are hidden |
| `GlanceActionChip` (`searchScreenLayout/`) | Pill button for a Glance row's own actions: outlined by default, or filled with `GlancePillColors` and an optional icon (missed call Call) |
| `MarkdownText` (`shared/util/MarkdownRenderer.kt`) | Short Markdown such as AI answers rendered as body text; `RenderMarkdownDocument` is for full documents |

The main Settings screen's About tiles follow the segment pattern: "More Apps from the Developer" has an `Apps` icon badge with `FilledTonalIconButton`s for Google Play and GitHub, and "Open Source Licenses" has a `Description` icon badge. Below them, the footer shows the app version as a `labelLarge` pill on `secondaryContainer`. The "Made by" line is `bodyMedium` `onSurfaceVariant`, with the developer name in `primary`. On the search screen, the Open Keyboard strip carries its background down behind the navigation bar to the screen edge (not in overlay mode).
