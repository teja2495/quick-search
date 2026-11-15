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
  `WallpaperText*`, `WallpaperDivider`, `wallpaperAwareMutedSearchForeground(...)`.
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
- `WidgetPanelCardShape` (20dp): the widgets panel.

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
| `AppAlertDialog` | All alert dialogs; Material 3 layout and `surfaceContainerHigh` tonal surface |
| `AppDropdownMenu` | All popup (three-dot / long-press) menus: 28dp corners, `surfaceContainer` tonal surfaces, 3dp shadow, non-focusable by default. Long-press menus over result items use the same tonal role through `itemMenuContainerColor`. Put `AppDropdownMenuDivider` between items. `AppDropdownMenuItem` is the text + icon row, with optional `supportingText` for the current value (Default Browser in Bookmarks) and optional `iconTint` (`AppColors.ItemMenuActiveIconTint` marks an assigned folder swipe action); `destructive = true` tints it with the error color for bulk or irreversible actions (Delete All), which must still confirm with an `AppAlertDialog`. Never call `DropdownMenu` directly |
| `AppUndoSnackbar` | Undo after a removal (hide, exclude, delete). `rememberUndoSnackbarController()` shows one snackbar at a time (a new one replaces the visible one) and runs the undo callback only on Undo. Place `AppUndoSnackbarHost` in a screen's root overlay (search route, overlay); settings pages without one use `AppUndoSnackbarPopup`. Inside search the route provides `LocalShowUndoSnackbar`. It's an accent pill (`AppColors.Accent`/`OnAccent`) that lifts above whatever is marked with `Modifier.undoSnackbarAvoidArea()` (the search screen's bottom chrome) through `LocalUndoSnackbarAnchor`, so it never covers the bottom search bar |
| `AppBottomSheet` | Modal bottom sheets (swipe/scrim/back dismissal handled) |
| `AppPickerDrawer` | Pick-one-item drawers (Select Action, Select Model, folder, language, calendar app and gesture pickers): title with close button, optional `titleIcon` before the title (the app's icon in the App notifications keywords drawer), optional pill search field (`AppPickerDrawerSearch`, `autoFocus` when the list is empty until typing; `onSubmit` turns it into an entry field with an add icon, `ShapeLarge` corners, and a Done key that submits, as in the App notifications keywords drawer), optional `header`/`footer`. Fixed 85% height with search, wraps short lists without. Build lists from `AppPickerDrawerRow` (no dividers; space rows with `AppPickerDrawerRowSpacing`). A `selected` row gets an accent tint and a plain accent check unless a custom `trailing` replaces the check. Leading visuals: `AppPickerDrawerAppIcon(packageName)` for apps, `AppPickerDrawerIconBadge(icon)` (accent-tinted rounded square) for everything else, both `AppPickerDrawerLeadingSize`. Use `AppPickerDrawerMessage` for hints/empty states. Custom items (such as the adaptive language radio tiles) reuse `AppPickerDrawerRowShape`, `appPickerDrawerItemBackground(selected)` and `AppPickerDrawerSelectedCheck` so selection looks the same. Prefer it over an `AppAlertDialog` with a radio list |
| `AppBottomPopup` | Bottom-anchored popup with header and scrollable options; safe drawing insets, 640dp maximum width, 48dp close target, and expressive spring drag recovery |
| `ItemMenuPopup` | Long-press action menus: tonal quick-action tiles, segmented option rows, pill buttons, and long-press dropdowns on a `surfaceContainerLow` shell. Never leave placeholder gaps between options; reflow so any gap falls only at the end of the last row. |
| `TipBanner` | Dismissible tips/hints, optional links and trailing action |
| `AppPill` | Small pill labels, with an optional accent-tinted `leadingIcon`; `compact` for tight labels. Non-interactive file metadata uses plain `bodyMedium` supporting text (`onSurfaceVariant`), with type and parent folder separated by a middle dot; filenames use `bodyLarge` (`onSurface`) |
| `AppVoiceCallIcon` | App logo plus phone icon for call actions |
| `dialogTextFieldColors()` | Text fields inside dialogs |
| `CardTextField`, `cardTextFieldColors()` | Borderless text fields stacked as rows of a `SettingsCard` (divided by `HorizontalDivider`); dimmed placeholder that shows before focus (label stays floated when a placeholder is set), and the floated label keeps a gap above the input |
| `SettingsCard` and `Settings*Row` (`settings/shared/`) | Settings cards, toggle/checkbox/navigation rows. `SettingsToggleRow` takes `subtitleTextStyle` (default `bodyMedium`, the Material 3 list supporting-text style), a `trailingAction` placed after the switch, such as a delete button, and `showNavigationChevron`, which with `onRowClick` adds the chevron and divider of `SettingsNavigationToggleRow` before the switch |
| `SettingsSegmentGroup`, `SettingsSegment`, `SettingsSegment*Row`, `SettingsIconBadge` (`settings/shared/SettingsSegmentedList.kt`) | Material 3 Expressive segmented settings lists, used by the main Settings page, Appearance, Gestures, Launch options, More options, Permissions, and At a Glance (also its widget drawer). `SettingsSegmentGroup` accepts an optional section title (accent `titleSmall`, marked as an accessibility heading) and `bodyMedium` description above the tiles. Inside a tile `LocalSettingsSegmented` is true: `SettingsToggleRow` drops its divider and `extraVerticalPadding`, uses 20dp sides and 16dp top/bottom, leads with a tonal icon badge, and shows the check thumb. Search Results count sliders sit below their switch rows to use the full tile width; app result row counts use `SettingsChoiceGroup`, and Choose Tabs is a separate navigation tile. Refresh Data retains its card with side-by-side icon-above-label actions, using filled tonal pill buttons. Free-form content (sliders, pickers) goes in `SettingsSegmentContent`. Each row is its own tile on `getSettingsCardContainerColor()` (28dp outer, 4dp inner corners, 2dp apart; a pressed tile rounds its inner corners to 16dp on `expressiveFastSpatialSpring()`). Rows lead with a 40dp circular accent-tinted `SettingsIconBadge` (brand drawables keep their colors) and drop the chevron; `SettingsSegmentToggleRow` toggles from the whole tile and shows a check in the switch thumb. `containerColor` overrides the tile color: dialogs list items as tiles too (section reorder, icon pack), tinting a selected tile with the accent and a dragged one fully round with a stronger accent tint. `SettingsNavigationToggleRow` in a tile takes the same layout (badge, 16dp padding, bodyMedium supporting text); Search Results (result types, Top Matches, and options), Manage Apps (app tiles with a tonal selected state, app icons, and separate uninstall checkboxes; its compact app-details dialog puts the package name under the header, pairs version and SDK fields in columns, and separates the groups with subtle dividers; labels are muted and App Info stays tonal), Search Engines, Tools, tool examples (`ToolExamplesSection`), the AI tool editor, and `ModelFeatureSettingsCard` are tiled (`ModelFeatureSettingsCard` is a model tile with an `AiProviderBadge`, then Thinking and Web Search as `SettingsSegmentToggleRow` switch tiles; the model drawer leads each row with the same badge only when several providers are set up), and so are AI Provider (Configure API key, Personal Context, the custom provider's advanced payload) and API Key Setup (`ApiKeyTile`: one tile per provider with its badge and name; with no key the tile pastes the clipboard and says so in the accent, a pasted key shows Clear and Save, a saved key shows its last four and a delete button; Add Custom Provider expands as the group's last tile; Tavily is its own group whose tile carries its explanation, with its mode as a `SettingsChoiceGroup` tile). Three-line tiles (Tavily, Add Search Engine while open) top-align their badge, per Material 3 (`SettingsSegmentRow(verticalAlignment = Alignment.Top)`). Add Search Engine is the same kind of collapsed tile (+ badge, chevron) as Add Custom Provider: opening it shows the URL instructions under its title, scrolls the card into view once expanded, and it collapses again after saving. The Permissions page opts into segmented `PermissionCard` rows with tonal badges, full-row permission requests, and granted status icons; onboarding retains its existing layout. Export data uses checkbox tiles in `AppPickerDrawer`, with Settings always included and export/cancel actions in its fixed footer. Other detail pages still use `SettingsCard` |
| `AliasPill` (`settings/shared/AliasPill.kt`) | Compact alias chip under a row's title (24dp, 8dp corners, labelMedium, 14dp icons, no border). Both states use a 12% accent tonal fill so they read as tappable: a set alias shows the code and a remove button, "Setup Alias" the action label. Kept quiet on purpose so it never competes with the row title; it opts out of the 48dp minimum touch size so it doesn't pad the row. `ToolExamplesSection` shows example queries as bodyMedium text on an 8% onSurface fill (8dp corners, no border). |
| `SettingsChoiceGroup`, `SettingsChoice`, `SettingsTonalButton` (`settings/shared/SettingsChoiceGroup.kt`) | Material 3 Expressive connected button group for a single choice inside a settings tile (Home text colour, app columns, theme mode, accent, search bar colours, compact engine rows). Buttons are 48dp, 2dp apart, fully round at the group ends with 8dp inner corners (4dp pressed); the selected one fills with `AppColors.Accent`/`OnAccent` and morphs fully round. Unselected buttons are a 14% accent tint so they read over wallpaper, AMOLED, and Material You. Options take an icon or colour swatch; plain options show a check when selected. Buttons share the row in proportion to their labels (plain options reserve the check slot so selection never shifts them), and labels stay on one line, shrinking to 11sp before ellipsizing. Built on stable APIs because material3 1.4 keeps `ToggleButton`/`ButtonGroup` internal; switch to them once 1.5 is stable. `SettingsTonalButton` is the tonal action (Reset, Edit, Delete) in the same accent tint, `small` for buttons beside text. |
| `SettingsFloatingActionHeight`, `settingsFloatingActionShape`, `SettingsFloatingTonalContainer` (`settings/shared/SettingsFloatingActions.kt`) | Floating actions at the bottom of settings pages (`CalendarEventsBottomBar`: At a Glance, Reminders, Bookmarks; Manage Apps uses the same height and animated shape for its uninstall action). Both buttons are 56dp and fully round, morphing to 16dp corners while pressed. The main action is `Accent`/`OnAccent`; the secondary (Search) is an opaque 24% accent tint over `surfaceContainerHigh`, since it floats over the list. |
| `SettingsNestedCheckbox` (`settings/shared/SettingsNestedCheckbox.kt`) | Sub-option under a settings toggle (e.g. At a Glance "tomorrow" options): checkbox and label on a 16dp-rounded 8% accent panel that toggles as a whole. |
| `SearchResultCard` (`searchScreen/shared/`) | Every home and result card (sections, history, suggestions, engine cards, tools via `InformationCard`). When the Compact Layout setting is on (`LocalSearchCardLayoutEnabled` is false, provided in `SearchScreen`), it drops its container color and border so content sits on the screen background at the width of section titles and the app grid, and draws a divider under each card in search results (not on Home, via `LocalCardlessSectionDividers`). A list that stacks cards with a gap other than `SpacingMedium` must provide `LocalCardlessStackSpacing` so those dividers stay centred; new home/result cards must use it so they follow the setting |
| `ResultSegment`, `ResultRowDivider`, `ExpandButtonSegment` (`searchScreen/shared/SegmentedResultRows.kt`) | Material 3 Expressive segmented lists. A section passes `segmented = true` to `ExpandableResultsCard`; with cards on, each row is its own tile (28dp outer, 4dp inner corners, 2dp apart, the card's container color) and the More button is the last tile. Wrap rows in `ResultSegment(index, count)`, use `ResultRowDivider` (drawn only when not segmented) and `resultRowsContainerPadding()` for the list's own padding. In Compact Layout and sections that don't opt in, rows stay on one card with dividers. Opted in: files, contacts, app shortcuts, device settings, app settings, notes, reminders, bookmarks. Top Matches (consecutive result rows; the app grid and Screen Time stay separate cards) and web suggestions build the same tiles from `segmentShape()` on `SearchResultCard` with the outline dropped |
| `OpenKeyboardAction` (`searchScreen/components/Pills.kt`) | Home's bottom keyboard strip: a Material 3 Expressive connected button group (voice search + Open Keyboard, 2dp apart, pill outer and 8dp inner corners that square on press), filled with the strip's foreground color at 10% (`onSurface` on wallpaper and Material You, white or black otherwise) so it stays neutral over any background |
| `HomeSectionHeaderRow` (`searchScreenLayout/`) | Home section titles. The expand chevron is one icon that rotates on `expressiveDefaultSpatialSpring()`, a bare `primary` icon with no background |
| `GlanceStatusRow` (`searchScreenLayout/`) | Home At a Glance rows: icon, title, subtitle or `belowText`, optional pill, trailing action, dismiss |
| `GlanceActionChip` (`searchScreenLayout/`) | Pill button for a Glance row's own actions: outlined by default, or filled with `GlancePillColors` and an optional icon (missed call Call) |
| `MarkdownText` (`shared/util/MarkdownRenderer.kt`) | Short Markdown such as AI answers rendered as body text; `RenderMarkdownDocument` is for full documents |
