# Quick Search Design System

Where shared UI values live and how to use them. The code is the source of truth. Check the
files below for exact values rather than copying them from here.

Theme files: `app/src/main/java/com/tk/quicksearch/shared/ui/theme/`
Shared components: `app/src/main/java/com/tk/quicksearch/shared/ui/components/` (app-wide) and
`settings/shared/` (settings-only rows built on them)

## Rules

- Use `DesignTokens` for spacing, sizes and elevation, and `AppMotion` for durations, instead of
  raw `dp`/ms values when a token fits. Add a token when a value is reused across files.
- Use `AppColors` for colors when a token exists, and fall back to `MaterialTheme.colorScheme`.
  Avoid hex literals so wallpaper, custom image, AMOLED, and accent modes stay correct. Fade a
  color with the alpha scale (`Content*Alpha`, `Tint*Alpha`, `Disabled*Alpha`), not a literal.
- `AppShapes` is the only source of shapes: the corner scale, semantic roles, and the
  position/press builders (segmented tiles, connected buttons, press morphs). Don't build a
  `RoundedCornerShape` or read `MaterialTheme.shapes` in feature code; the exceptions are radii the
  user configures or that animate. Use `AppMotion` for every spring and duration; don't write
  `spring(...)` values inline.
- Reuse shared components before building a new dialog, sheet, popup, or settings row. Never call
  Material `Button`/`FilledTonalButton`/`OutlinedButton`/`TextButton` directly; use the
  `AppButtons.kt` wrappers, and pick a `tone`/`size` rather than passing colors, shapes or
  heights.
- Every shared surface must work over a theme gradient, system wallpaper, custom image, and
  AMOLED black, in light and dark, and in overlay, one-handed (bottom search bar), and tablet
  layouts.

## Theme (`Theme.kt`)

`QuickSearchTheme` wraps every surface. It resolves:

- **App theme** (`AppTheme`): `MONOCHROME` (shown as "Default") or one of the Colorful palettes
  listed in `CUSTOM_APP_THEME_PALETTES` (picked from `ThemePaletteDrawer`). Per-theme accents,
  gradients, and themed icon colors live in `ThemeColorRegistry`: most are hand-tuned, and newer
  ones are generated from a hue and chroma by `generateBundle`. AMOLED black applies to any theme in dark mode; on the search and Home
  surfaces only when the background is the theme gradient (not a wallpaper), while settings
  pages always use it.
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
| `AppShapes.kt` | Every shape. Corner scale `none`, `extraSmall`, `small`, `medium`, `large`, `largeIncreased`, `extraLarge`, `full`, and `material` (the `Shapes` passed to `MaterialTheme`). Roles: `card`, `resultCard`, `widgetPanelCard`, `segmentGroup`, `iconTile`, `iconFrame`, `favicon`, `engineHighlight`, `inlineInput`, `field`. Builders: `corners(...)`, `segment(...)`, `connected(...)`, `stacked(...)`. Decorative `cookie9`/`cookie4`. Press morphs: `pressMorphPillShape` (every button), `pressMorphShape`, `pressMorphButtonShape`, `floatingActionShape`, `pressMorphSegmentShape`, `connectedButtonShape` |
| `AppPolygons.kt` | The Material 3 Expressive polygons (`softBurst`, `cookie9`, `pentagon`, `pill`, `sunny`, `cookie4`, `oval`) as normalized `RoundedPolygon`s, and `PolygonShape` to use one as a `Shape`. Backs `AppShapes.cookie9`/`cookie4` and the Gemini loading morph until `MaterialShapes` is public |
| `AppMotion.kt` | Springs: `fastSpatial()` (small shape changes), `defaultSpatial()` (moves, rotations), `settle()` (no-bounce settle, e.g. page snaps), `loadingMorph()` (looping shape morphs), `dragFeedback()` (grid drag/merge scale and fade). Durations: `DurationMicro` 50, `DurationShort` 200, `DurationMedium` 300, `DurationGlowFade` 500, `DurationLong` 4000 |
| `Theme.kt` (`HomeTextContrastTheme`) | Wraps the search content area in Compact Layout (not the overlay). Over a wallpaper or image, when `homeBackgroundIsDark()` doesn't match the app theme, it swaps in the other theme's text, surface and outline roles (and `LocalAppIsDarkTheme`/palette), and trades `primary` with `primaryContainer` so accents stay legible |

Main `AppColors` groups:

- **Accent:** `Accent`, `OnAccent`, `LinkColor` (stays blue in monochrome so links read as
  tappable), `ItemMenuActiveIconTint`, `IconTintPrimary`/`IconTintSecondary`.
- **Accent tints** (the accent at a fixed alpha, for tonal fills): `AccentTintFaint` 5%,
  `AccentTintSubtle` 8%, `AccentTintLight` 12%, `AccentTintTonal` 14% (tonal buttons, badges,
  unselected choices), `AccentTintStrong` 24%, `AccentTintEmphasis` 30%. The alphas are
  `TintFaintAlpha`…`TintEmphasisAlpha` for tinting another color the same way (e.g. error).
  `FloatingTonalContainer` is the strong tint flattened onto `surfaceContainerHigh` for buttons
  that float over content. Use these instead of `Accent.copy(alpha = …)`.
- **Alpha scale:** `ContentFaintAlpha` 0.4, `ContentMutedAlpha` 0.5, `ContentSecondaryAlpha` 0.6,
  `ContentHighAlpha` 0.7, `ContentStrongAlpha` 0.8, `ContentNearOpaqueAlpha` 0.9 for fading
  content; `DisabledContentAlpha` 0.38 and `DisabledContainerAlpha` 0.12 (Material's disabled
  values); `SecondaryTonalContainerAlpha` 0.6 for the secondary tonal button. Snap to the nearest
  step rather than adding a one-off alpha.
- **Search bar and chrome:** `getSearchBarBackground(...)`, `getSearchBarTextAndIconColor(...)`,
  `SearchChromeOutlineBorder`, `KeyboardPill*`, `InlineEngineHighlight*`,
  `InlineEngineDivider`.
- **Wallpaper surfaces:** `ResultCardWallpaperBackground`,
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
`Spacing40`, `Spacing48`.

**Semantic spacing:** `ContentHorizontalPadding`, `Card*Padding`, `ItemRowSpacing`,
`TextColumnSpacing`, `Chip*`, `Section*Padding`, `Onboarding*`. Helpers: `singleCardPadding()`,
`headerPadding()`, `cardItemTopPadding(isFirst)`, `cardItemBottomPadding(isLast)`.

**Corner scale** (Material 3 shape scale, in dp): `CornerNone` 0, `CornerExtraSmall` 4,
`CornerSmall` 8, `CornerMedium` 12, `CornerLarge` 16, `CornerLargeIncreased` 20,
`CornerExtraLarge` 28, plus role corners `CornerFavicon` 6, `CornerIconTile` 10,
`CornerIconFrame` 14, `CornerEngineHighlight` 18, `CornerInlineInput` 24. Every other corner token
and `AppShapes` derive from these, so retuning the scale retunes the app. Use the shapes through
`AppShapes` (see the theme files table), not these values.

**Action menus:** `ItemMenuMaxWidth` caps floating popups at 640dp; `ItemMenuRowHeight`
sets a 56dp minimum action-row height. Result action tiles use a fast expressive spring
to round their corners from 20dp to 8dp while pressed. Settings-like rows use the existing
segmented-list corner and gap tokens; secondary buttons use `AppShapes.full`. Quick-action tiles and secondary buttons share
`surfaceContainerHighest`; destructive buttons keep that neutral fill with error-colored
text and icons for a restrained warning. `ItemMenuActiveIconTint` preserves the chosen
accent while strengthening it toward `onSurface` until it reaches 4.5:1 contrast across
the neutral popup surfaces. Shared long-press popups for apps, files,
shortcuts, and contacts use 8dp between the header/action groups and buttons, with
2dp between grouped list rows. Tiles, pill buttons and grouped list rows all use the same
`surfaceContainerHighest` fill. The `ItemMenuPopup` title row has a 16dp top inset
(`AppBottomPopup.headerTopPadding`) and 16dp below it, and the popup ends with a 16dp
`AppBottomPopup.bottomPadding`; the package caption sits 16dp below the footer buttons.
Contact action popups reuse `ItemMenuTileButton`
for call/message/provider actions, retain provider icon colors, and use segmented rows
for remaining methods. Contact action tiles and the number selector use 8dp gaps; the grouped method
list uses the 2dp segmented-list gap. Their phone-number selector uses 48dp navigation targets on a
`surfaceContainerHigh` surface. The contact actions shell uses 20dp top corners,
28dp bottom corners, and a 16dp header top inset to balance its circular avatar.
It enables `AppBottomPopup.animateSizeChanges` so switching numbers smoothly resizes
the shell with the default expressive spring, keeping its bottom edge anchored.

**Press morph and connected groups:** `ButtonPressedCornerSmall` 8 (extra-small buttons),
`ButtonPressedCornerMedium` 16 (medium buttons), `SegmentPressedInnerCorner` 16 (pressed segment
tile), `FloatingActionHeight` 56, `ConnectedGroupGap` 2, `ConnectedGroupInnerCorner` 8,
`ConnectedGroupPressedInnerCorner` 4. Read them through the `AppShapes.kt` helpers.

**Buttons:** `ButtonHeightExtraSmall` 32, `ButtonHeightSmall` 40, `FloatingActionHeight` 56 (medium),
`ButtonIconSizeExtraSmall` 16, `ButtonIconSizeSmall` 18, `TonalSettingsButtonHeight` 48. Callers
pick an `AppButtonSize`, which reads these.

**Sizes:** `IconSizeSmall` 20, `IconSize` 24, `LargeIconSize` 28, `IconSizeXLarge` 52,
`AppIconSize` 64, `BorderWidth` 1, `DividerThickness` 0.5.

**Elevation:** `ElevationLevel0`–`ElevationLevel5` (0, 1, 3, 6, 8, 12dp).

**Motion:** only `DragAlpha` (dragged item opacity). Durations and springs live in `AppMotion`
(see the theme files table).

**Segmented lists:** `SegmentedListOuterCorner` 28, `SegmentedListInnerCorner` 4, `SegmentedListGap` 2,
`SegmentedListTileHorizontalPadding`, `SegmentedListTileVerticalPadding`.

**Search field:** `SearchField*` border widths, alphas, and gradient multipliers.

## Restyling and moving to a new Material version

The design language is concentrated in a few places so a redesign (or Material 4/5) is a change to
them, not to every screen:

1. **Colors:** `Color.kt`, `ThemeColorRegistry.kt`, `AppColorPalette.kt`, and the `AppColors`
   tokens (including the accent tint scale).
2. **Shape:** the corner scale in `DesignTokens`, the press-morph tokens, and `AppShapes.kt`
   (every shape in the app goes through it). When `MaterialShapes` is available, point
   `AppPolygons` at it.
3. **Motion:** `AppMotion.kt`. Once `MotionScheme.expressive()` is public, pass it to both
   `MaterialTheme` calls in `Theme.kt` and read the springs from `MaterialTheme.motionScheme`.
4. **Typography:** `Type.kt` and both flavors' `DistributionTypography.kt`.
5. **Components:** the wrappers in `shared/ui/components/`: `AppButtons.kt` (all buttons; tones
   and sizes map to Material's button sizes, so adopting a new button API happens there),
   `ConnectedButtonGroup.kt` and `ConnectedChoiceGroup.kt` (swap to Material's `ButtonGroup`/
   `ToggleButton` when the dependency has them), `SegmentedList.kt` (segmented tiles, switches,
   tonal badges), `AppAlertDialog`, `AppBottomSheet`, `AppDropdownMenu`, `AppPickerDrawer`, and
   `ItemMenuPopup`. Screens use these, so changing a wrapper's defaults restyles every caller.

`Theme.kt` passes `AppShapes.material` to `MaterialTheme`, and `HomeTextContrastTheme` forwards
it, so stock Material components pick up the same scale.

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
| `AppAlertDialog` | All alert dialogs; Material 3 layout and `surfaceContainerHigh` tonal surface. Confirm and dismiss actions are both `AppTextButton`s (error-tinted for destructive confirms); keep the default `headlineSmall` title and `bodyMedium` `onSurfaceVariant` text |
| `AppButton`, `AppTonalButton`, `AppOutlinedButton`, `AppTextButton`, `TonalButton` (`AppButtons.kt`) | Every button. Filled, tonal, outlined and text styles take a semantic `tone` (`AppButtonTone.Accent` default, `Secondary` for actions inside result cards, `Error` for destructive, `Neutral` for de-emphasized, `Custom(color)` for text over wallpaper or a snackbar's action color), a `size` (`AppButtonSize.ExtraSmall` 32dp, `Small` 40dp default, `Medium` 56dp) and an optional leading `icon`. The wrappers own shape (pill that squares toward the size's pressed corner, `pressMorphPillShape`), colors, padding and icon spacing; callers don't pass any of these. Tonal is the tone's color at `TintTonalAlpha` under content in that color (`Secondary` uses `secondaryContainer`). `AppOutlinedButton(showBorder = false)` lets a caller draw its own outline. `TonalButton` is the tonal action inside a tile (Reset, Edit, Delete): 48dp, wrapping rather than ellipsizing its label, `small` (32dp) for buttons beside text |
| `ConnectedButtonRow` (`ConnectedButtonGroup.kt`) | Material 3 Expressive connected button group for actions (not a choice). Declare buttons with `button(onClick, width = null, weight = 1f) { content }`; the row works out which button is first and last, so conditional buttons need no position flags. Buttons are tonal in the row's `tone`, `ConnectedGroupGap` apart, fully round on the group's outer edges with `ConnectedGroupInnerCorner` between, tightening while pressed unless `pressMorph = false`. Used by Home's Open Keyboard strip and the More options backup and developer-link buttons |
| `PickerFieldButton` (`PickerFieldButton.kt`) | Outlined field that opens a date, time or repeat picker: leading icon and the current value, `AppShapes.field` corners. Used by the reminder form |
| `AppDropdownMenu` | All popup (three-dot / long-press) menus: 28dp corners, `surfaceContainer` tonal surfaces, 3dp shadow, non-focusable by default. Long-press menus over result items use the same tonal role through `itemMenuContainerColor`. Put `AppDropdownMenuDivider` between items. `AppDropdownMenuItem` is the text + icon row, with optional `supportingText` for the current value (Default Browser in Bookmarks) and optional `iconTint` (`AppColors.ItemMenuActiveIconTint` marks an assigned folder swipe action); `destructive = true` tints it with the error color for bulk or irreversible actions (Delete All), which must still confirm with an `AppAlertDialog`. Never call `DropdownMenu` directly |
| `AppUndoSnackbar` | Undo after a removal (hide, exclude, delete). `rememberUndoSnackbarController()` shows one snackbar at a time (a new one replaces the visible one) and runs the undo callback only on Undo. Place `AppUndoSnackbarHost` in a screen's root overlay (search route, overlay); settings pages without one use `AppUndoSnackbarPopup`. Inside search the route provides `LocalShowUndoSnackbar`. It uses the Material 3 snackbar roles (`inverseSurface`/`inverseOnSurface`, 8dp corners), with the action in `primaryContainer` (the inverse theme's accent tone, since the baseline `inversePrimary` isn't derived from the accent), and lifts above whatever is marked with `Modifier.undoSnackbarAvoidArea()` (the search screen's bottom chrome) through `LocalUndoSnackbarAnchor`, so it never covers the bottom search bar |
| `AppBottomSheet` | Modal bottom sheets (swipe/scrim/back dismissal handled). `scrimColor = Color.Transparent` keeps the screen behind undimmed, for drawers that preview a change live (the theme palette drawer) |
| `AppPickerDrawer` | Pick-one-item drawers (Select Action, Select Model, folder, language, calendar app and gesture pickers): title with close button, optional `titleIcon` before the title (the app's icon in the App notifications keywords drawer), optional pill search field (`AppPickerDrawerSearch`, `autoFocus` when the list is empty until typing; `onSubmit` turns it into an entry field with an add icon, `AppShapes.large` corners, and a Done key that submits, as in the App notifications keywords drawer), optional `header`/`footer`. Fixed 85% height with search, wraps short lists without. Build lists from `AppPickerDrawerRow` (no dividers; space rows with `AppPickerDrawerRowSpacing`). A `selected` row gets an accent tint and a plain accent check unless a custom `trailing` replaces the check. Leading visuals: `AppPickerDrawerAppIcon(packageName)` for apps, `AppPickerDrawerIconBadge(icon)` (a `TonalIconBadgeContainer` with `AppShapes.small` corners) for everything else, both `AppPickerDrawerLeadingSize`. Use `AppPickerDrawerMessage` for hints/empty states. Custom items (such as the adaptive language radio tiles) reuse `AppPickerDrawerRowShape`, `appPickerDrawerItemBackground(selected)` and `AppPickerDrawerSelectedCheck` so selection looks the same. Prefer it over an `AppAlertDialog` with a radio list |
| `AppBottomPopup` | Bottom-anchored popup with header and scrollable options; safe drawing insets, 640dp maximum width, 48dp close target, and expressive spring drag recovery |
| `ItemMenuPopup` | Long-press action menus: tonal quick-action tiles, segmented option rows, pill buttons, and long-press dropdowns on a `surfaceContainerLow` shell. Never leave placeholder gaps between options; reflow so any gap falls only at the end of the last row. |
| `TipBanner` | Dismissible tips/hints, optional links and trailing action |
| `AppVoiceCallIcon` | App logo plus phone icon for call actions |
| `dialogTextFieldColors()` | Text fields inside dialogs |
| `CardTextField`, `cardTextFieldColors()` | Borderless text fields stacked as rows in a settings surface (divided by `HorizontalDivider`); dimmed placeholder that shows before focus (label stays floated when a placeholder is set), and the floated label keeps a gap above the input |
| `Settings*Row` (`settings/shared/`) | Settings toggle and navigation rows. `SettingsToggleRow` takes `subtitleTextStyle` (default `bodyMedium`, the Material 3 list supporting-text style), a `trailingAction` placed after the switch, such as a delete button, and `showNavigationChevron`, which with `onRowClick` adds the chevron and divider of `SettingsNavigationToggleRow` before the switch |
| `SegmentedGroup`, `SegmentedTile`, `SegmentedTileRow`, `SegmentedToggleTile`, `SegmentedTileContent`, `AppSwitch`, `TonalIconBadge`, `TonalIconBadgeContainer` (`SegmentedList.kt`); `SettingsSegmentNavigationRow`, `SettingsIconBadge` (`settings/shared/SettingsSegmentedList.kt`) | Material 3 Expressive segmented lists, the default layout for settings pages, dialogs that list items, widget configure screens, onboarding and drawers. `SegmentedGroup` takes an optional section title (accent `titleSmall`, an accessibility heading) and `bodyMedium` description. Each row is its own `SegmentedTile` on `getSettingsCardContainerColor()`: 28dp outer and 4dp inner corners, 2dp apart, inner corners rounding to 16dp while pressed (`pressMorphSegmentShape`). `containerColor` overrides the fill (accent tint for selected, stronger tint for dragged); `backdrop`, `contentPadding` and `pressMorph = false` let result lists reuse it; `shape` replaces the index-based shape for tiles that animate their own corners. Inside a tile `LocalInSegmentedTile` is true, so `SettingsToggleRow` drops its divider, uses 20dp sides and 16dp vertical padding, leads with a tonal badge and shows the check thumb. Rows lead with a 40dp circular `TonalIconBadge` (brand drawables keep their colors; `TonalIconBadgeContainer` takes custom content, size and shape) and drop the chevron. `SegmentedToggleTile` toggles from the whole tile with `AppSwitch`. Free-form content (sliders, pickers, previews) goes in `SegmentedTileContent`; three-line tiles top-align their badge (`SegmentedTileRow(verticalAlignment = Alignment.Top)`). Onboarding provides `LocalSegmentedItemModifier` (from `NavigationManager`) to animate tiles out; the components themselves don't depend on onboarding. Lists that scroll inside a page clip their viewport to `AppShapes.extraLarge`. |
| `BooleanSettingTile` (`settings/shared/BooleanSettingTile.kt`) | A segmented switch for a registered boolean preference. Reads the same definition and value used by searchable Settings and dispatches the shared Settings command. |
| `ExpandableSegmentedGroup` (`ExpandableSegmentedGroup.kt`) | A `SegmentedGroup` that collapses to its title, the Material 3 expandable list pattern. The first tile leads with a 56dp-min header row (`titleMedium` title as an accessibility heading, trailing `ExpandMore` chevron turning 180° on `AppMotion.defaultSpatial()`); tapping it toggles with a haptic and a ripple that press-morphs the tile. Expanded, `firstTileContent` (16dp inset, 12dp gaps) shows under the header and `remainingTiles` (indexed from 1 of `tileCount`) follow; content enters with `expandVertically + fadeIn`. The first tile's bottom corners morph between outer (collapsed) and inner (expanded). Used by the Theme, Background, Search Bar and Search Engines Style cards in Appearance settings, expanded by default; each card's state persists in session prefs (`isAppearanceCardExpanded`) |
| `AliasPill` (`settings/shared/AliasPill.kt`) | Compact alias chip under a row's title (24dp, 8dp corners, labelMedium, 14dp icons, no border). Both states use a 12% accent tonal fill so they read as tappable: a set alias shows the code and a remove button, "Setup Alias" the action label. Kept quiet on purpose so it never competes with the row title; it opts out of the 48dp minimum touch size so it doesn't pad the row. `ToolExamplesSection` shows example queries as bodyMedium text on an 8% onSurface fill (8dp corners, no border). |
| `ConnectedChoiceGroup`, `ChoiceOption`, `ChoiceTile` (`ConnectedChoiceGroup.kt`) | Material 3 Expressive connected button group for a single choice: inside settings tiles (Home text colour, app columns, theme mode, accent, search bar colours, compact engine rows, widget options) and on app-setting search results. Buttons are 48dp, `ConnectedGroupGap` apart, fully round at the group ends with 8dp inner corners (4dp pressed) via `connectedButtonShape`; the selected one fills with `Accent`/`OnAccent` and morphs fully round, unselected ones use `AccentTintTonal`. Options take an icon or colour swatch; plain options show a check when selected. Buttons share the row in proportion to their labels and labels stay on one line down to 11sp, then wrap; they never ellipsize. Built on stable APIs because material3 1.4 keeps `ToggleButton`/`ButtonGroup` internal; switch to them once available. `ChoiceTile` is a segmented tile with a `titleMedium` heading over a full-width group |
| `WidgetSettingTile`, `WidgetColorChoiceGroup`, `WidgetSaveButton` (`widgets/widgetConfigScreen/components/WidgetSettingTiles.kt`) | Widget configure screens. `WidgetSettingTile` is a `SegmentedTile` with an optional `titleMedium` heading and description over free-form content; related settings share a `SegmentedGroup`. Single choices use `ConnectedChoiceGroup`; `WidgetColorChoiceGroup` adds a Custom option that opens `WidgetColorPickerDialog`. `WidgetSaveButton` is the accent full-width Save, `FloatingActionHeight` tall with `floatingActionShape` |
| Floating actions (`DesignTokens.FloatingActionHeight`, `floatingActionShape`, `AppColors.FloatingTonalContainer`) | Actions at the bottom of pages (`CalendarEventsBottomBar`: At a Glance, Reminders, Bookmarks, Notes; Manage Apps' uninstall; onboarding; widget Save; Collapse). 56dp and fully round, squaring to 16dp while pressed. The main action is `Accent`/`OnAccent`; a secondary one (Search) uses `FloatingTonalContainer`, opaque because it floats over the list |
| `SettingsNestedCheckbox` (`settings/shared/SettingsNestedCheckbox.kt`) | Sub-option under a settings toggle (e.g. At a Glance "tomorrow" options): checkbox and label on a 16dp-rounded 8% accent panel that toggles as a whole. |
| `InformationCardFooter`, `InformationCardFooterAction`, `InformationCardCopyAction` (`searchScreen/shared/InformationCardFooter.kt`) | Material 3 card action area on the bottom edge of every `InformationCard` (calculator, unit converter, date calculator, color visualizer, AI answers and AI tools). A full-bleed band tinted `onSurface` at 6% over the card container (no fill in Compact Layout), at least 48dp tall: the result's source at the start, aligned with the 16dp body inset (outlined tool icon and name, or the AI provider wordmark, both `onSurfaceVariant` `labelMedium`), and standard `IconButton` actions at the end (outlined 20dp icons, since filled icons signal a selected state in Material 3; `onSurfaceVariant`, 48dp touch targets) each labelled by a `PlainTooltip`. Copy swaps to a check mark for 1.5s. The tappable provider wordmark gets a ripple and 48dp target. When the card has a minimum height, the footer is pinned to the bottom edge |
| `SearchResultCard` (`searchScreen/shared/`) | Every home and result card (sections, history, suggestions, engine cards, tools and AI answers via `InformationCard`). When the Compact Layout setting is on (`LocalSearchCardLayoutEnabled` is false, provided in `SearchScreen`), it drops its container color and border so content sits on the screen background at the width of section titles and the app grid, and draws a full-width divider under every card on Home and in search results, including the last one, so the list is closed off at the bottom. The app grid draws the same divider below it (`compactBottomDivider`), so the first home section is separated from the grid too. When the list is bottom-anchored (reversed), `reversedListTopDivider` also closes its open top end with a divider above the topmost item. A list that stacks cards with a gap other than `SpacingMedium` must provide `LocalCardlessStackSpacing` so those dividers stay centred; new home/result cards must use it so they follow the setting |
| Result row typography | Following the Material 3 list item spec, every search result row (contacts, files, events, reminders, notes, bookmarks, shortcuts, device and app settings, Top Matches app rows, web suggestions, search history, and engine rows), every Home pinned item, and every At a Glance row (`GlanceStatusRow` defaults, alarms, reminders, missed calls, battery, now playing, app notifications, Custom Info, OTP source line) uses `bodyLarge` for its headline and `bodyMedium` for supporting text (dates, file metadata, app names, notification source and age) in both layouts. Glances that feature a value (OTP code, workout stats) keep their larger display style. Home section headers use sentence-case `titleSmall` in both layouts; Compact Layout only swaps the card for a divider below the header |
| `ResultSegment`, `ResultRowDivider`, `ExpandButtonSegment` (`searchScreen/shared/SegmentedResultRows.kt`) | Segmented search results. A section passes `segmented = true` to `ExpandableResultsCard`; with cards on, each row is its own tile and the More button is the last tile. `ResultSegment(index, count)` is a `SegmentedTile` with the frosted card backdrop, the result card container color, result row padding and no press morph. Use `ResultRowDivider` (drawn only when not segmented) and `resultRowsContainerPadding()`. In Compact Layout and sections that don't opt in, rows stay on one card with dividers. Opted in: files, contacts, app shortcuts, device settings, app settings, notes, reminders, bookmarks, search history. Top Matches and web suggestions build the same tiles from `AppShapes.segment(...)` on `SearchResultCard` with the outline dropped |
| `ExpandButton`, `CollapseButton` (`searchScreen/components/ExpandCollapseButtons.kt`) | A section's More and Collapse actions: a leading chevron and `labelLarge` label in `primary`. In segmented sections the whole last tile is the More button (`ExpandButtonSegment`, like Clear History); other card sections use an `AppTextButton`. In Compact Layout the More button is a full-width, edge-to-edge list row (48dp minimum, ripple to the screen edges) with the same chevron and `labelLarge` label; `ExpandButtonSegment` falls back to it outside segmented lists. Collapse floats over the expanded section as a 48dp pill on the result card surface, squaring its corners while pressed (`floatingActionShape`) |
| `OpenKeyboardAction` (`searchScreen/components/Pills.kt`) | Home's bottom keyboard strip: a `ConnectedButtonRow` (voice search + Open Keyboard, 56dp, pill outer and 8dp inner corners that square on press) in `AppButtonTone.Custom` of the strip's foreground color (`onSurface` on wallpaper and Material You, white or black otherwise), so its tonal fill stays neutral over any background |
| `HomeSectionHeaderRow` (`searchScreenLayout/`) | Home section titles. The expand chevron is one icon that rotates on `AppMotion.defaultSpatial()`, a bare `primary` icon with no background |
| `GlanceStatusRow` (`searchScreenLayout/`) | Home At a Glance rows: icon, title, subtitle or `belowText`, optional pill, trailing action, dismiss |
| `KeyboardSwitchPill`, `PhoneCallPill`, `SetAlarmPill`, `StartTimerPill`, `CreateReminderPill`, `AllAppsPill` (`searchScreen/components/Pills.kt`) | Search quick actions share `QuickActionButton`: a Material 3 Expressive extra-small button (32dp, `labelLarge`, 16dp leading icon, symmetric 16dp horizontal padding so content stays centered, fully round corners that square to `ButtonPressedCornerSmall` on press (`pressMorphButtonShape`)). Filled tonal (`secondaryContainer`/`onSecondaryContainer`); the search quick actions draw the container at 50% opacity, while `AllAppsPill` keeps it opaque. The quick action row stays on one line: buttons share the width (`weight(1f, fill = false)`) and labels auto-shrink to 10sp instead of wrapping or ellipsizing; with three or more buttons the icons are hidden |
| `GlanceActionChip` (`searchScreenLayout/`) | Pill button for a Glance row's own actions: outlined by default, or filled with `GlancePillColors` and an optional icon (missed call Call) |
| `MarkdownText` (`shared/util/MarkdownRenderer.kt`) | Short Markdown such as AI answers rendered as body text; `RenderMarkdownDocument` is for full documents |

Appearance and App Settings results share the launcher icon and pinned section order drawers. The Search Engines Style result uses the same connected choices as Appearance, with row count choices shown in Compact mode.

Appearance and the Accent Color search result share the same connected choices and Custom color picker. The Search History result uses an inline slider for its item count while history is enabled.

The main Settings screen's About tiles follow the segment pattern: "More Apps from the Developer" has an `Apps` icon badge with a `ConnectedButtonRow` for Google Play and GitHub, and "Open Source Licenses" has a `Description` icon badge. Below them, the footer shows the app icon on `AppShapes.cookie9` and the version as a `labelLarge` pill on `secondaryContainer`. The "Made by" line is `bodyMedium` `onSurfaceVariant`, with the developer name in `primary`. On the search screen, the Open Keyboard strip carries its background down behind the navigation bar to the screen edge (not in overlay mode).
