# Quick Search Design System

How Quick Search applies Material 3 and where the shared UI values and components live. The code
is the source of truth: check the files named here for exact values rather than copying them from
this page.

- Theme: `app/src/main/java/com/tk/quicksearch/shared/ui/theme/`
- Shared components: `app/src/main/java/com/tk/quicksearch/shared/ui/components/` (app-wide) and
  `settings/shared/` (settings-only rows built on them)
- Icons: `shared/ui/icons/MaterialSymbols.kt`

## Material 3 first

Quick Search follows [Material 3](https://m3.material.io/), including the Material 3 Expressive
updates (shape morphs, connected button groups, segmented lists, expressive springs). Every new
feature, screen, or component starts from the Material 3 guideline for that pattern.

1. **Find the Material 3 pattern.** Before designing, look up the matching component or pattern in
   the Material 3 guidelines (list, card, dialog, bottom sheet, menu, button group, snackbar,
   navigation, and so on). Use its anatomy, roles, and behavior.
2. **Use the shared wrapper for it.** Most Material components already have a Quick Search wrapper
   that applies the theme, background modes, and expressive shapes (see
   [Shared components](#shared-components)). Use the wrapper rather than the raw Material
   component.
3. **Use Material roles, not values.** Use color roles (`primary`, `onSurfaceVariant`,
   `surfaceContainerHigh`, and so on, through `AppColors` where a token exists), the type scale
   (`MaterialTheme.typography.*`), the shape scale (`AppShapes`), and motion springs (`AppMotion`).
   Don't use hex colors, ad hoc `TextStyle`s, raw corner radii, or inline springs.
4. **Keep Material's accessibility baselines.** That means 48dp minimum touch targets, 4.5:1
   contrast for text, a content description or tooltip for every icon-only action, headings
   marked for accessibility, and icons that mirror in RTL.
5. **Deviate only on purpose.** If Material 3 has no pattern for something, or the app needs to
   break one (for example, frosted cards over the wallpaper), build it from Material primitives and
   tokens and record the decision in this file.

## Rules

- **Spacing and sizes:** `DesignTokens`, on the 4dp grid. Use `MinTouchTarget` for a 48dp
  interactive area and `TextColumnSpacing` between primary and supporting text.
- **Colors:** `AppColors` when a token exists, otherwise `MaterialTheme.colorScheme`. Fade colors
  with the alpha scale (`Content*Alpha`, `Tint*Alpha`, `DisabledContentAlpha`), not literals, and
  tint with the accent tint scale, not `Accent.copy(alpha = …)`.
- **Shapes:** `AppShapes` only. Don't build a `RoundedCornerShape` or read `MaterialTheme.shapes`
  in feature code, except for radii the user configures or that animate.
- **Motion:** `AppMotion` for every spring, duration, and easing curve; don't write `spring(...)`
  inline or use Material 2's `FastOutSlowInEasing`, `LinearOutSlowInEasing`, or
  `FastOutLinearInEasing`. Every `tween(...)` passes an `AppMotion` easing; a bare `tween()`
  defaults to `FastOutSlowInEasing`.
- **Press feedback:** every tappable element shows Material's state layer. Don't pass
  `indication = null` unless the tap has other immediate visual feedback, and say why in a comment.
- **Components:** reuse a shared component before building a new dialog, sheet, popup, menu,
  button, or settings row. Never call Material `Button`, `FilledTonalButton`, `OutlinedButton`,
  `TextButton`, `IconButton`, `FilledIconButton`, `FilledTonalIconButton`, `DatePickerDialog`,
  `InputChip`, or `DropdownMenu` directly.
- **Icon buttons:** use `AppIconButton` with a `tooltip`, normally the icon's content description,
  and pick the filled or tonal variant with `style`. Material 3 labels every icon-only action with
  a tooltip.
- **Messages:** show brief feedback with `AppMessages.show(context, …)`, which appears as a
  snackbar on the visible screen. Keep `Toast` only where no app UI is on screen (widgets, Quick
  Settings tiles, trampoline activities, the accessibility service).
- **Touch targets:** keep Material's 48dp minimum. Don't provide
  `LocalMinimumInteractiveComponentSize provides Dp.Unspecified` unless the layout already
  reserves the touch area (`SettingsNestedCheckbox`), and say so in a comment.
- **Type weight:** take weight from the type scale. Override `fontWeight` only to emphasize text
  one step above its style (a featured value, a bold name in a sentence), never to restate the
  style's own weight. Don't override `letterSpacing` or `lineHeight`.
- **Menus:** in action menus, never leave placeholder gaps between options. Reflow so any gap is
  only at the end of the last row.
- **Every mode:** shared surfaces must work over the theme gradient, system wallpaper, custom
  image, and AMOLED black, in light and dark, with Material You colors on and off, and in overlay,
  one-handed (bottom search bar), Compact Layout, and tablet layouts.

### Checklist for new UI

- [ ] Named the Material 3 component or pattern it follows, or recorded why it deviates.
- [ ] Built from shared components and tokens, with no raw colors, radii, springs, or text styles.
- [ ] Checked in light and dark, over wallpaper or a custom image, with AMOLED and Material You,
      and in overlay, bottom search bar, Compact Layout, and tablet where it appears.
- [ ] Meets the 48dp touch target, contrast, content description, and RTL baselines.
- [ ] Updated this file if a token, theme mode, or shared component was added or renamed.

## Theme (`Theme.kt`)

`QuickSearchTheme` wraps every surface. It resolves:

- **App theme** (`AppTheme`): `MONOCHROME` (shown as "Default") or one of the colorful palettes in
  `CUSTOM_APP_THEME_PALETTES` (picked from `ThemePaletteDrawer`). `ThemeColorRegistry` holds each
  theme's accents, gradients, and themed icon colors. Most are hand-tuned; newer ones are generated
  from a hue and chroma by `generateBundle`. AMOLED black applies to any theme in dark mode: on the
  search and Home surfaces only when the background is the theme gradient, and always on settings
  pages.
- **Mode** (`AppThemeMode`): light, dark, or system.
- **Background** (`BackgroundSource`): `THEME` gradient, `SYSTEM_WALLPAPER`, or `CUSTOM_IMAGE`.
- **Accent** (`AccentColorMode`): `NONE`, `FROM_WALLPAPER`, or `CUSTOM`. Device dynamic colors
  (Material You, Android 12+) override these when enabled.
- **Font:** font scale multiplier and system-font toggle.

It exposes composition locals in `AppColorPalette.kt` (`LocalAppIsDarkTheme`, `LocalAppTheme`,
`LocalIsSystemWallpaperActive`, `LocalImageBackgroundIsDark`, `LocalDeviceDynamicColorsActive`,
`LocalAmoledThemeActive`, `LocalSearchColorTheme`). Prefer an existing `AppColors` token over
branching on these directly.

`HomeTextContrastTheme` (also in `Theme.kt`) wraps the search content area in Compact Layout (not
the overlay). Over a wallpaper or image, when `homeBackgroundIsDark()` doesn't match the app theme,
it swaps in the other theme's text, surface, and outline roles, and trades `primary` with
`primaryContainer` so accents stay legible.

## Colors

| File | Contents |
|---|---|
| `AppColors.kt` | Semantic, theme-aware tokens. Use these in composables |
| `AppColorPalette.kt` | Light/dark palette backing `AppColors`, scrim and alpha constants, composition locals |
| `Color.kt` | Default accent (`AppAccentLight`/`AppAccentDark`) and base Material 3 schemes |
| `ThemeColorRegistry.kt` | Per-`AppTheme` accents, gradients, themed icon colors, Material You tone slots |
| `HomeTextColor.kt` | `homeTextColor()` (the user's Home text override, or a wallpaper-derived foreground) and `homeBackgroundIsDark()` (the same choice as a flag, null without an image background) |
| `ItemMenuAccent.kt` | `contrastingItemMenuAccent(...)`, which strengthens the accent until it reaches 4.5:1 on popup surfaces (backs `ItemMenuActiveIconTint`) |

Main `AppColors` groups:

- **Accent:** `Accent`, `OnAccent`, `LinkColor` (stays blue in monochrome so links read as
  tappable), `ItemMenuActiveIconTint` (the accent, strengthened toward `onSurface` until it
  reaches 4.5:1 on popup surfaces), `IconTintSecondary`, `SecondaryIconTint`, `CallIconTint`.
- **Accent tints:** the accent at a fixed alpha for tonal fills. `AccentTintFaint` 5%,
  `AccentTintSubtle` 8%, `AccentTintLight` 12%, `AccentTintTonal` 14% (tonal buttons, badges,
  unselected choices), `AccentTintStrong` 24%, `AccentTintEmphasis` 30%. Use
  `TintFaintAlpha`…`TintEmphasisAlpha` to tint another color, such as error, the same way.
  `FloatingTonalContainer` is the strong tint flattened onto `surfaceContainerHigh` for buttons
  that float over content.
- **Alpha scale:** `ContentFaintAlpha` 0.4, `ContentMutedAlpha` 0.5, `ContentSecondaryAlpha` 0.6,
  `ContentHighAlpha` 0.7, `ContentStrongAlpha` 0.8, `ContentNearOpaqueAlpha` 0.9, and
  `DisabledContentAlpha` 0.38 (Material's disabled value). Snap to the nearest step rather than
  adding a one-off alpha.
- **Search bar and chrome:** `getSearchBarBackground(...)`, `getSearchBarTextAndIconColor(...)`,
  `SearchChromeOutlineBorder`, `KeyboardPill*`, `InlineEngineHighlight*`, `InlineEngineDivider`.
- **Wallpaper surfaces:** `ResultCardWallpaperBackground`, `WallpaperText*`, `WallpaperDivider`,
  `WallpaperDarkCardBorder`, `wallpaperAwareMutedSearchForeground(...)`, `FolderWallpaperBackdrop`
  (folder tile scrim over an image background), `FolderTileBorder`.
- **Indicators and trust:** `indicatorBase(lightFill)` (the neutral that grid indicators blend
  the primary color into), `TrustAccent` (open-source and permission reassurance green, tuned per
  surface luminance).
- **Settings and dialogs:** `getSettingsCardContainerColor()`, `SettingsDivider`,
  `getDialogContainerColor()`, `DialogBackground`, `DialogText`, `Overlay*`.
- **Drawers and sheets:** `DrawerTileBackground` (`AccentTintSubtle`) fills tiles and fields on a
  `DialogBackground` sheet. `AppBottomSheet` provides it through `LocalSegmentedTileContainerColor`,
  so `SegmentedTile`s inside a drawer pick it up by default; use `segmentedTileContainerColor()` when
  composing your own tile color. Never use `getSettingsCardContainerColor()` inside a drawer.
- **Contact actions:** `ActionPhone`, `ActionSms`, `ActionSignal`, `ActionCustom`, `ActionView`.
- **Widgets:** `Widget*` (RemoteViews, so not theme-aware).

To change the default brand accent, edit `AppAccentLight`/`AppAccentDark` in `Color.kt`.

### Result and Home card colors

Resolve card colors only through `SearchResultCardDefaults` (`searchScreen/shared/SearchResultCard.kt`):
`colors(...)`/`containerColor(...)` for card and segment-tile containers, `border(...)`, and
`dividerColor(...)` for row, section, and Compact Layout dividers. They apply the overlay tint
(`LocalOverlayResultCardColor`, `LocalOverlayDividerColor`), then the wallpaper surface, then the
Material default, so don't re-derive these per component. Content inside a card stays transparent.

### Frosted surfaces (a deliberate Material deviation)

Over a wallpaper or custom image on Android 12+ (not on low-RAM devices), result cards, segment
tiles, permission cards, and the widgets panel cards show a blurred copy of the wallpaper behind
them (`searchScreen/FrostedCardBackdrop.kt`). The search background bakes one small blurred bitmap
when the wallpaper or blur setting changes. `Modifier.frostedCardBackground(shape)` draws the
matching region under the card, and `SearchResultCardDefaults.containerColor` lowers the scrim by
`FrostedResultCardScrimFactor` while frost is active. It strengthens the scrim when needed for
4.5:1 supporting-text contrast over the lightest or darkest possible tinted image. A new Home
or result surface that draws its own container adds the modifier before its background. There
is no frost over the system wallpaper, because Android draws it and the app has no copy.

The search bar and bottom chrome (`insetCardBackdrop` / `insetCardBackground`) are solid by
default, flattened onto `surfaceContainer` (black for AMOLED). The "Blurred Background" toggle in
Appearance (`LocalSearchChromeBlurEnabled`) restores frosted chrome. Result cards are unaffected.

### Other deliberate deviations

- **App suggestion tabs.** These scrollable Material tabs are transparent over the Home
  background. The selected label uses heavier type in place of a bottom indicator. Labels use
  the Home foreground color, dimmed to `ContentHighAlpha` when unselected. The tabs are 36dp tall, below the 48dp touch target,
  because users mostly switch categories by swiping the pager rather than tapping a tab.
  Three tabs show at a time, each at least a third of the strip wide (a longer title widens its
  tab rather than being cut off), and
  the selected tab is always scrolled to the center; one empty slot pads each end so the first
  and last tabs can reach it.
- **Search-style fields.** The search bar (`SearchBar.kt`), `SettingsManagementSearchBar`, and the
  icon search in `AppIconOverrideDrawer` build on Material text fields directly so they can own
  their container, shape, and focus behavior.
- **Full-window dialogs.** `HomeAddWidgetSheet`, `SpeedBumpOverlay`, and the onboarding
  `PermissionsScreen` use a raw Compose `Dialog` as a full-window host for custom layouts, not as
  a Material dialog, so they don't use `AppDialogSurface`.
- **Filter chips are pills.** `AppFilterChip` uses `AppShapes.full` instead of Material's 8dp chip
  corners, to match the app's pill buttons.
- **Inline editors.** The note editor (`NoteEditor.kt`), the Home quick note
  (`WidgetPanelQuickNote.kt`), the folder title rename (`FolderContentsPopup.kt`), and the AI
  personal context card (`AiProviderSettings.kt`) use `BasicTextField`. The text is edited in place
  on its own surface, so a form field's container and label would be wrong.
- **Ripple-free taps.** The theme palette drawer and the theme tiles in Appearance apply the choice
  live and show the selection, so they skip the ripple. The long-press easter eggs (version
  footer, contact number copy) have no tap action to give feedback for. In the overlay, tapping the
  scrim dismisses it (the exit animation is the feedback), and the card swallows taps on its empty
  space so they don't reach the scrim.
- **Background picker bento.** Appearance > Background (`BackgroundSourceTile.kt`) lays out the
  sources as a bento grid instead of equal tiles: the wallpaper preview is the large tile, Custom
  sits above a short Theme bar, and Custom takes the large slot when there is no wallpaper source.
  Each label sits below its tile, and the selection check is centered on the preview.
- **Contact row actions.** A tap opens the current contact action and a long press opens its
  action chooser. The two icon buttons use `combinedClickable` with a 48dp target and label
  because `AppIconButton` uses long press for its tooltip.
- **PDF paper.** `FilePreviewDialog` draws PDF pages on `Color.White` in every theme. Pages render
  with a transparent background, and the white is the page itself, not an app surface.
- **Icon shadows.** App icons and search engine icons over a light wallpaper, and the All Apps
  letter bubble, cast a shadow at a Material elevation level. A tonal surface can't separate a bare
  icon from an arbitrary wallpaper.
- **Loading.** Every indeterminate wait uses `AppLoadingIndicator`, Material 3 Expressive's
  `LoadingIndicator`, which is still experimental in material3 1.5 (the wrapper holds the opt-in).
  Information cards (AI answers, weather, dictionary, currency, world clock) pair it with shimmering
  placeholder lines in `InformationCardLoadingAnimation`. Determinate progress bars keep
  `LinearProgressIndicator` / `LinearWavyProgressIndicator`.
- **Segmented tiles.** `SegmentedTile` stays a custom container rather than Material's
  `SegmentedListItem`: tiles hold free-form content, frosted backdrops, and caller-animated shapes
  that the slot-based list item can't take. It follows the same segmented-list spec.
- **Small badges.** Edit/remove badges on wallpaper thumbnails (20dp) and the notification history
  chevron are smaller than 48dp. They are clickable with a button role, and Compose extends their
  hit area to the 48dp minimum touch target where space allows.

## Tokens (`DesignTokens.kt`)

- **Spacing (4dp grid):** `SpacingXSmall` 4, `SpacingSmall` 8, `SpacingMedium` 12,
  `SpacingLarge` 16, `SpacingXLarge` 20, `SpacingXXLarge` 24, `Spacing28`, `SpacingHuge` 32,
  `Spacing40`, `Spacing48`.
- **Semantic spacing:** `ContentHorizontalPadding` 20, `Header*`, `Section*Padding`,
  `Card*Padding`, `ItemRowSpacing`, `TextColumnSpacing`, `Chip*`, `Onboarding*`, and the helpers
  `cardItemTopPadding(isFirstItem)` / `cardItemBottomPadding(isLastItem)`.
- **Corner scale (Material 3 shape scale):** `CornerNone` 0, `CornerExtraSmall` 4, `CornerSmall`
  8, `CornerMedium` 12, `CornerLarge` 16, `CornerLargeIncreased` 20, `CornerExtraLarge` 28. Role
  corners: `CornerFavicon` 6, `CornerIconTile` 10, `CornerIconFrame` 14, `CornerEngineHighlight`
  18, `CornerInlineInput` 24. Every other corner token and `AppShapes` derive from these. Use them
  through `AppShapes`.
- **Press morphs and connected groups:** `ButtonPressedCornerSmall` 8, `ButtonPressedCornerMedium`
  16, `SegmentPressedInnerCorner` 16, `ConnectedGroupGap` 2, `ConnectedGroupInnerCorner` 8 (both
  match Material's connected button group tokens; the groups themselves read
  `ButtonGroupDefaults`).
- **Buttons:** `ButtonHeightExtraSmall` 32, `ButtonHeightSmall` 40, `FloatingActionHeight` 56,
  `TonalSettingsButtonHeight` 48, `ButtonIconSizeExtraSmall` 16, `ButtonIconSizeSmall` 18. Callers
  pick an `AppButtonSize` instead of reading these.
- **Segmented lists:** `SegmentedListOuterCorner` 28, `SegmentedListInnerCorner` 4,
  `SegmentedListGap` 2, `SegmentedListTile*Padding`.
- **Action menus:** `ItemMenuMaxWidth` 640, `ItemMenuRowHeight` 56.
- **Sizes:** `IconSizeSmall` 20, `IconSize` 24, `LargeIconSize` 28, `IconSizeXLarge` 52,
  `ListItemMinHeight` 56 (one-line list row), `AppIconSize` 64, `BorderWidth` 1, `DividerThickness` 0.5,
  `EditBadgeIconSize` 12 (pencil in an edit badge), `InlineProgressSize` 24 (a small loading
  indicator standing in for an icon or icon button).
- **At a Glance rows:** `GlanceRowMinHeight` 52, `GlanceRowHorizontalPadding` 7. The inset is off
  the 4dp grid; every glance row shares it so they line up. `ResultIconStartInset` gives the 24dp
  leading glyph in notes, reminders, calendar, and bookmark rows the same inset.
- **Elevation (Material levels):** `ElevationLevel0` 0, `ElevationLevel2` 3, `ElevationLevel3` 6,
  `ElevationLevel4` 8, `ElevationLevel5` 12. Prefer tonal surface roles over shadows.
- **Other:** `DragAlpha`, `SearchField*`, `WidgetPanelCornerRadius`.

## Shapes (`AppShapes.kt`, `AppPolygons.kt`)

`AppShapes` is the only source of shapes:

- **Scale:** `none`, `extraSmall`, `small`, `medium`, `large`, `largeIncreased`, `extraLarge`,
  `full`, and `material` (the `Shapes` passed to `MaterialTheme`, so stock components use the same
  scale).
- **Roles:** `card`, `resultCard`, `widgetPanelCard`, `segmentGroup`, `iconTile`, `iconFrame`,
  `favicon`, `faviconScaled` (corner is 20% of the icon, for favicons drawn at several sizes),
  `engineHighlight`, `inlineInput`, `field`. Use `full` rather than `CircleShape`.
- **Builders:** `corners(...)`, `segment(...)`, `connected(...)`, `stacked(...)`.
- **Press morphs:** buttons and icon buttons use Material's own morph (`ButtonDefaults.shapes`,
  `IconButtonDefaults.shapes`) inside their wrappers. Surfaces Material doesn't cover use
  `pressMorphShape`, `pressMorphButtonShape`, `floatingActionShape` (FABs have no `shapes` API), and
  `pressMorphSegmentShape`. Connected button groups use Material's `ButtonGroupDefaults` connected
  shapes.
- **Animated segments:** `animatedSegmentShape(index, count)` morphs a tile's outer corners when it
  becomes or stops being the first or last tile, without a press morph. Home result rows reach it
  through `ResultSegment(cornerIndex, cornerCount)` (At a Glance), so a dismissed row's neighbors
  round while it collapses instead of snapping afterward.
- **Expressive polygons:** `AppPolygons` points at Material's `MaterialShapes` (`cookie9`,
  `cookie4`), plus `PolygonShape`. `AppShapes.cookie9`/`cookie4` use them.

## Motion (`AppMotion.kt`)

The theme (`MaterialExpressiveTheme` in `Theme.kt`) runs stock components on
`MotionScheme.expressive()`. App-owned springs mirror that scheme: `fastSpatial()` (small shape
changes), `defaultSpatial()` (moves, rotations), `settle()` (no-bounce settle such as page snaps),
`dragFeedback()` (grid drag and merge), and
`placement(visibilityThreshold)` (items settling into a new grid slot). Durations follow the
Material 3 duration scale: `DurationMicro` 50, `DurationQuick` 100 (short2), `DurationExit` 150
(short3, exits and fades out), `DurationShort` 200, `DurationStandard` 250 (medium1),
`DurationMedium` 300, `DurationLarge` 400 (medium4, large enter transitions), `DurationGlowFade`
500, `DurationExtraLong` 700 (long2), `DurationLong` 4000. Point file-local duration constants at
these rather than raw numbers; snap to the nearest step. Looping animation periods (the AI loading
morph and shimmer) are not transition durations and stay local.

Tweens use the Material 3 easing curves: `EmphasizedEasing` for moves, resizes, and crossfades that
stay on screen, `EmphasizedDecelerateEasing` for enters, and `EmphasizedAccelerateEasing` for exits.

## Typography (`Type.kt`)

`quickSearchTypography(useSystemFont)` returns the app `Typography`. Use the Material 3 type scale
(`MaterialTheme.typography.*`) rather than ad hoc `TextStyle`s. The font depends on the flavor
(`DistributionTypography.kt`): `standard` bundles Google Sans and `fdroid` uses the system font.

Common roles, following the Material 3 list and dialog specs:

- **List rows** (every search result row, Home pinned item, and At a Glance row): `bodyLarge`
  headline and `bodyMedium` supporting text, in both layouts. Glances that feature a value (an OTP
  code, workout stats) keep their larger display style.
- **Section headers:** sentence-case `titleSmall` (Home), or accent `titleSmall` as an
  accessibility heading (`SegmentedGroup`).
- **Dialogs:** `headlineSmall` title, `bodyMedium` `onSurfaceVariant` text.
- **Drawers:** `titleLarge` header. **Buttons and actions:** `labelLarge`.

## Icons (`shared/ui/icons/MaterialSymbols.kt`)

UI icons are Material Symbols Rounded (weight 400, grade 0, 24dp), filled by default; a name
ending in `Outlined` is the unfilled variant. In Material 3, a filled icon signals a selected
state, so use outlined icons for unselected actions such as card footer actions. Use
`MaterialSymbols.<Name>` wherever an `ImageVector` is expected, and don't add `material-icons-*`
back. Directional icons (arrows, send, label, missed call) set `autoMirror`. To add an icon, copy
its path data from [fonts.google.com/icons](https://fonts.google.com/icons) with the same style
settings and add a property in sorted order.

## Shared components

| Component | Material 3 pattern | Use for |
|---|---|---|
| `AppButton`, `AppTonalButton`, `AppOutlinedButton`, `AppTextButton`, `TonalButton` (`AppButtons.kt`) | Buttons | Every button. Pick a `tone` (`AppButtonTone.Accent` default, `Secondary` inside result cards, `Error` for destructive, `Neutral` for de-emphasized, `Custom(color)` over wallpaper), a `size` (`AppButtonSize.ExtraSmall` 32dp, `Small` 40dp default, `Medium` 56dp), and an optional `icon`. The wrappers own shape (a pill that squares on press, via Material's `ButtonDefaults.shapes`), colors, and padding. `TonalButton` is the 48dp tonal action inside a tile (Reset, Edit, Delete) |
| `ConnectedButtonRow` (`ConnectedButtonGroup.kt`) | Connected button group (Material `FilledTonalButton` + `ButtonGroupDefaults` shapes) | A row of related actions (not a choice). Declare buttons with `button(onClick, width, weight) { … }`; the row works out first and last, so conditional buttons need no position flags |
| `ConnectedChoiceGroup`, `ChoiceOption`, `ChoiceTile` (`ConnectedChoiceGroup.kt`) | Connected button group (single select), Material `ToggleButton` | Single choices in settings and app-setting results. The selected option fills with `Accent` and morphs fully round. Labels use the Material type scale. Options stack vertically when they cannot fit side by side. `ChoiceTile` is a segmented tile with a `titleMedium` heading over a full-width group |
| `SegmentedGroup`, `SegmentedTile`, `SegmentedTileRow`, `SegmentedToggleTile`, `SegmentedTileContent`, `AppSwitch`, `TonalIconBadge`, `TonalIconBadgeContainer` (`SegmentedList.kt`) | Segmented list | The default layout for settings pages, dialogs that list items, widget config screens, onboarding, and drawers. Each row is its own tile, with 28dp outer corners, 4dp inner corners, and 2dp gaps; inner corners round to 16dp while pressed. Rows lead with a 40dp `TonalIconBadge`. Inside a tile, `LocalSegmentedItemModifier` and `LocalInSegmentedTile` adjust nested rows |
| `AnimatedSegmentedTiles`, `SettingsRevealVisibility` (`AnimatedSegmentedTiles.kt`) | Segmented list | Tiles or groups that show only while another setting allows them. Declare every tile with `tile(visible = …)` or `item(visible = …) { index, count -> … }` instead of leaving it out; the list works out positions, a tile expands and fades in or shrinks out on the default spatial spring, and neighbors morph their outer corners (`pressMorphSegmentShape`). `SettingsRevealVisibility` does the same for a whole group or a nested option inside a tile. Put the gap above a revealed group inside it, not in the parent's `spacedBy`, so it doesn't jump |
| `ExpandableSegmentedGroup` | Expandable list | A `SegmentedGroup` that collapses to its title row (`titleMedium` heading, rotating chevron) |
| `PermissionsPageContent` (`shared/permissions/`) | Segmented list | The shared checklist and icon-free permission tiles for onboarding and Settings. Each page shows its applicable rows. |
| `SettingsToggleRow`, `SettingsNavigationToggleRow`, `SettingsSegmentNavigationRow`, `SettingsIconBadge`, `BooleanSettingTile`, `SettingsNestedCheckbox`, `SettingsRemoveButton` (`settings/shared/`) | List items | Settings toggle, navigation, and sub-option rows. `BooleanSettingTile` binds a registered boolean preference to the same command that searchable Settings uses |
| `AppTopAppBar` | Top app bar (small) | Full-screen page headers (Settings and its detail pages, widget config screens): back button, `titleLarge` heading, and `actions`. `navigationIcon` and `navigationContentDescription` swap the back arrow (a close icon for config screens that cancel); `windowInsets` is zero by default, and screens that draw under the status bar pass `TopAppBarDefaults.windowInsets`. The container is transparent over the page background, and there is no on-scroll fill because the page clips its scrolling content below the bar |
| `AppIconButton`, `AppIconButtonStyle` | Icon button (standard, filled, tonal) with plain tooltip | Every icon-only action. `style` picks `Standard` (default), `Filled`, or `Tonal`; `colors` overrides the style's colors. `tooltip` is shown on long-press or hover. Buttons round-to-square on press (`IconButtonDefaults.shapes`). `modifier` goes on the button; parent-data modifiers (`align`, `weight`) go in `anchorModifier`, because the tooltip anchor wraps the button |
| `AppPlainTooltip` | Plain tooltip | Custom icon-only actions that need to own their click or long press. Pass `preserveLongPress = true` when the gesture already runs an action. The tooltip appears on hover and the action keeps its accessible name. |
| `AppAlertDialog` | Dialog | Alerts and confirmations on `surfaceContainerHigh`. Pass `confirmLabel`/`onConfirm` and an optional `dismissLabel`; set `destructive = true` for an error-tinted confirm |
| `AppDialogSurface` | Dialog (custom content) | Custom centered content (File Preview, Pinned Notification Items). The feature owns sizing and actions |
| `AppBottomSheet` | Modal bottom sheet | Bottom sheets with 28dp top corners. `scrimColor = Color.Transparent` keeps the screen behind undimmed for live previews |
| `AppPickerDrawer`, `AppPickerDrawerRow`, `AppPickerDrawerSearch` | Bottom sheet with list | Pick one item from a list, with an optional search field, header, and footer. Prefer it over a dialog with a radio list. The search bar section picker uses it with a scrollable grid |
| `AppDrawerHeader` | Sheet header | `titleLarge` drawer header with optional leading content and a 48dp close button |
| `AppBottomPopup` | Bottom sheet (floating) | Bottom-anchored floating popup with the drawer header, 28dp corners, and a 640dp max width. `animateSizeChanges` animates height changes with the bottom anchored |
| `ItemMenuPopup` | Menu (expressive) | Long-press action menus: tonal quick-action tiles, segmented option rows, and pill buttons on `surfaceContainerHighest`. Contact action popups reuse its tiles. Reflow options so gaps fall only at the end of the last row |
| `AppDropdownMenu`, `AppDropdownMenuItem`, `AppSelectableDropdownMenuItem`, `AppDropdownMenuDivider`, `AppOverflowMenu` | Menu | Every three-dot and long-press dropdown: Material's expressive `DropdownMenuPopup` with one `DropdownMenuGroup`, 28dp corners on `surfaceContainer`; long menus scroll inside the group. `destructive = true` items must still confirm with `AppAlertDialog`. `AppDropdownMenuItem` takes an `icon: ImageVector` or a `leadingIcon` slot for custom leading content. A single choice inside a menu (sort order) uses `AppSelectableDropdownMenuItem` (Material `SelectableDropdownMenuItem`: check, accent tint, radio semantics); don't use raw `DropdownMenuItem` |
| `AppUndoSnackbarHost`, `AppUndoSnackbarPopup`, `rememberUndoSnackbarController()` | Snackbar | Undo after a removal. Put the host in a screen's root overlay; settings pages without one use the popup. It lifts above `Modifier.undoSnackbarAvoidArea()` so it never covers the bottom search bar. `showMessage` shows a message with no action |
| `AppMessages`, `AppMessageSnackbarHost` (`AppMessages.kt`) | Snackbar | Brief feedback in place of a `Toast`. `AppMessages.show(context, text or @StringRes, long)` works from any thread and goes to the highest-priority host registered by a resumed screen, falling back to a `Toast` when none is. The search route and overlay register their undo snackbar with `AppMessages.HostEffect(controller, ScreenHostPriority)`; navigation's root and `WidgetConfigScaffold` (with `inScaffold = true`) hold an `AppMessageSnackbarHost` at fallback priority |
| `AppDatePickerDialog` | Date picker dialog | Picking a date. Wraps Material's `DatePickerDialog` and `DatePicker` with `AppTextButton` confirm and cancel actions |
| `AppInputChip` | Input chip | A removable entry the user added (excluded file extensions): pill shape, trailing close icon, and a `SettingsDivider` outline |
| `AliasPill` (`settings/shared/`) | Input chip | A tonal alias chip. The label opens the editor and the trailing close button clears the alias. Both actions keep independent 48dp touch targets. |
| `AppOutlinedTextField`, `dialogTextFieldColors()`, `CardTextField`, `cardTextFieldColors()` | Text fields | Every form input: outlined fields in dialogs, sheets, and settings forms (`String`, `TextFieldValue`, and `TextFieldState` overloads take an optional label, placeholder, supporting text, icons, keyboard options, and a `shape`, `AppShapes.extraSmall` by default for strings; filter input with an `InputTransformation` such as `maxLengthTrim` and style it with an `OutputTransformation`, both on the `TextFieldState` overload, rather than a `VisualTransformation`); transparent fields with a visible focus indicator stacked as rows on a settings surface. Don't call Material `TextField` or `OutlinedTextField` directly; search-style fields are the exception below |
| `AppLoadingIndicator` | Loading indicator (expressive) | Every indeterminate wait: Material's morphing `LoadingIndicator` in `Accent`, 48dp by default, `InlineProgressSize` inline. Don't use `CircularProgressIndicator` |
| `AppFloatingActionButton`, `AppExtendedFloatingActionButton`, `AppFabTone` (`AppFloatingActionButton.kt`) | FAB, extended FAB | 56dp page-bottom actions with a 24dp icon, fully round and squaring on press. `AppFabTone.Primary` (`Accent`) for the main action, `Tonal` (`FloatingTonalContainer`) for a secondary one, `Error` for destructive. The extended FAB takes a label and an `enabled` flag (fades to `DisabledContentAlpha`); the caller sets its width. Don't call Material FABs directly |
| `AppFilterChip`, `AppFilterChipStyle` (`AppFilterChip.kt`) | Filter chip | Filter and multi-select chips. Selected fills with `Accent`; `Outlined` (default) has a `SettingsDivider` outline, `Tonal` uses `AccentTintTonal` for chips on a tonal surface. `showSelectedCheck` adds the multi-select check. Over a wallpaper, pass the wallpaper-aware label and border colors |
| `PickerFieldButton` | Text field (read-only) | Outlined field that opens a date, time, or repeat picker |
| `AppSlider`, `SliderWithValue` | Slider | `AppSlider` is the app's slider, built on Material's `SliderState` overload. The caller owns the value and saves it from `onValueChange`, and colors default to `AppColors.sliderColors()` so the inactive track stays visible in Monochrome. Don't call Material `Slider` directly. `SliderWithValue` puts a value beside or above a slider, with an optional leading label for endpoint scales; the caller owns range, ticks, and saving |
| `TipBanner` | Card | Dismissible tips with optional links and an action |
| `PredictiveBackPreviewHandler`, `PredictiveBackSlideHandler`, `PreserveHostKeyboardStateEffect` | Predictive back | Predictive back animations for nested pages, and keeping the host's keyboard state while a popup is open |
| `AppVoiceCallIcon` | — | App logo plus phone icon for call actions |
| `MarkdownText` (`shared/util/MarkdownRenderer.kt`) | — | Short Markdown, such as AI answers, rendered as body text |

### Search surface components

| Component | Use for |
|---|---|
| `SearchResultCard`, `SearchResultCardDefaults` (`searchScreen/shared/`) | Every Home and result card. In Compact Layout (`LocalSearchCardLayoutEnabled` false) it drops its container and border and draws a full-width divider under each card. A stack with a gap other than `SpacingMedium` provides `LocalCardlessStackSpacing` |
| `ResultSegment`, `ResultRowDivider`, `ExpandButtonSegment` (`SegmentedResultRows.kt`) | Segmented result rows: each row is its own tile when cards are on, with dividers on one card otherwise. Sections opt in with `segmented = true` on `ExpandableResultsCard` |
| `ExpandButton`, `CollapseButton` | A section's More and Collapse actions: chevron and `labelLarge` in `primary` |
| `SectionActionPill` | The Collapse button's frosted pill, reused by other section actions that should match it; `compact` gives a 36dp size with a 16dp icon (All Apps) |
| `InformationCard`, `InformationCardFooter` | Tool and AI answer cards. The footer is the Material card action area: source at the start, outlined 20dp `IconButton`s with tooltips at the end |
| `QuickActionButton` and pills (`searchScreen/components/Pills.kt`) | Search quick actions: 32dp tonal extra-small buttons that share one line and shrink labels instead of wrapping |
| `OpenKeyboardAction` | Home's bottom keyboard strip, as a `ConnectedButtonRow` |
| `HomeSectionHeaderRow`, `GlanceStatusRow`, `GlanceActionChip` (`searchScreenLayout/`, `searchScreenLayout/glance/`) | Home section titles, At a Glance rows, and their pill actions |
| `WidgetConfigScaffold`, `WidgetSettingTile`, `WidgetColorChoiceGroup`, `WidgetSaveButton` | Widget configuration screens, built on segmented tiles and connected choices |

## Moving to a new Material version

The design language is concentrated in a few places, so a redesign or a Material update changes
these rather than every screen:

1. **Colors:** `Color.kt`, `ThemeColorRegistry.kt`, `AppColorPalette.kt`, and the `AppColors` tokens.
2. **Shape:** the corner scale in `DesignTokens`, `AppShapes.kt`, and `AppPolygons.kt` (backed by
   `MaterialShapes`).
3. **Motion:** the `motionScheme` passed to `MaterialExpressiveTheme` in `Theme.kt`, and
   `AppMotion.kt`, which mirrors it for app-owned animations.
4. **Typography:** `Type.kt` and both flavors' `DistributionTypography.kt`.
5. **Components:** the wrappers in `shared/ui/components/`. `ConnectedButtonGroup.kt` and
   `ConnectedChoiceGroup.kt` are built on Material's `FilledTonalButton`/`ToggleButton` with
   `ButtonGroupDefaults` connected shapes.

## Keeping this file current

Update this file in the same change that adds, renames, or removes a token, theme mode, or shared
component, or that makes a deliberate Material 3 deviation. Keep feature-specific layout details
(exact paddings of one popup, one screen's tile order) in code comments, not here.
