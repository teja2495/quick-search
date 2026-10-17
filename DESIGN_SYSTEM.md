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
| `HomeTextColor.kt` | `homeTextColor()`, which applies the user's Home text override or else a wallpaper-derived foreground |

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

**Sizes:** `IconSizeSmall` 20, `IconSize` 24, `LargeIconSize` 28, `IconSizeXLarge` 52,
`AppIconSize` 64, `BorderWidth` 1, `DividerThickness` 0.5, `ButtonCornerRadius` 24.

**Elevation:** `ElevationLevel0`–`ElevationLevel5` (0, 1, 3, 6, 8, 12dp).

**Motion:** `AnimationDurationMicro` 50, `AnimationDurationShort` 200,
`AnimationDurationMedium` 300, `AnimationDurationFast` 500, `AnimationDurationLong` 4000;
drag/spring constants (`DragAlpha`, `SpringDampingRatio`, `SpringStiffness`).

**Search field:** `SearchField*` border widths, alphas, and gradient multipliers.

## Typography (`Type.kt`)

`quickSearchTypography(useSystemFont)` returns the app `Typography`. Use
`MaterialTheme.typography.*` styles rather than ad hoc `TextStyle`s. The font is flavor-specific
(`DistributionTypography.kt`): `standard` bundles Google Sans, `fdroid` uses the system font. Check
both flavors when changing typography.

## Shared components

| Component | Use for |
|---|---|
| `AppAlertDialog` | All alert dialogs |
| `AppBottomSheet` | Modal bottom sheets (swipe/scrim/back dismissal handled) |
| `AppPickerDrawer` | Pick-one-item drawers (Select Action, Select Model, folder, language, calendar app and gesture pickers): title with close button, optional `titleIcon` before the title (the app's icon in the App notifications keywords drawer), optional pill search field (`AppPickerDrawerSearch`, `autoFocus` when the list is empty until typing; `onSubmit` turns it into an entry field with an add icon, `ShapeLarge` corners, and a Done key that submits, as in the App notifications keywords drawer), optional `header`/`footer`. Fixed 85% height with search, wraps short lists without. Build lists from `AppPickerDrawerRow` (no dividers; space rows with `AppPickerDrawerRowSpacing`). A `selected` row gets an accent tint and a plain accent check unless a custom `trailing` replaces the check. Leading visuals: `AppPickerDrawerAppIcon(packageName)` for apps, `AppPickerDrawerIconBadge(icon)` (accent-tinted rounded square) for everything else, both `AppPickerDrawerLeadingSize`. Use `AppPickerDrawerMessage` for hints/empty states. Custom items (such as the language tiles) reuse `AppPickerDrawerRowShape`, `appPickerDrawerItemBackground(selected)` and `AppPickerDrawerSelectedCheck` so selection looks the same. Prefer it over an `AppAlertDialog` with a radio list |
| `AppBottomPopup` | Bottom-anchored popup with header and scrollable card of options |
| `ItemMenuPopup` | Long-press action menus: tile grids, rows, buttons, long-press dropdowns. Never leave placeholder gaps between options; reflow so any gap falls only at the end of the last row. |
| `TipBanner` | Dismissible tips/hints, optional links and trailing action |
| `AppPill` | Small pill labels |
| `AppVoiceCallIcon` | App logo plus phone icon for call actions |
| `dialogTextFieldColors()` | Text fields inside dialogs |
| `CardTextField`, `cardTextFieldColors()` | Borderless text fields stacked as rows of a `SettingsCard` (divided by `HorizontalDivider`); dimmed placeholder that shows before focus (label stays floated when a placeholder is set), and the floated label keeps a gap above the input |
| `SettingsCard` and `Settings*Row` (`settings/shared/`) | Settings cards, toggle/checkbox/navigation rows. `SettingsToggleRow` takes `subtitleTextStyle` (default `bodySmall`) a `trailingAction` placed after the switch, such as a delete button, and `showNavigationChevron`, which with `onRowClick` adds the chevron and divider of `SettingsNavigationToggleRow` before the switch |
| `SearchResultCard` (`searchScreen/shared/`) | Every home and result card (sections, history, suggestions, engine cards, tools via `InformationCard`). When the Compact Layout setting is on (`LocalSearchCardLayoutEnabled` is false, provided in `SearchScreen`), it drops its container color and border so content sits on the screen background at the width of section titles and the app grid, and draws a divider under each card in search results (not on Home, via `LocalCardlessSectionDividers`). A list that stacks cards with a gap other than `SpacingMedium` must provide `LocalCardlessStackSpacing` so those dividers stay centred; new home/result cards must use it so they follow the setting |
| `GlanceStatusRow` (`searchScreenLayout/`) | Home At a Glance rows: icon, title, subtitle or `belowText`, optional pill, trailing action, dismiss |
| `GlanceActionChip` (`searchScreenLayout/`) | Pill button for a Glance row's own actions: outlined by default, or filled with `GlancePillColors` and an optional icon (missed call Call) |
| `MarkdownText` (`shared/util/MarkdownRenderer.kt`) | Short Markdown such as AI answers rendered as body text; `RenderMarkdownDocument` is for full documents |
