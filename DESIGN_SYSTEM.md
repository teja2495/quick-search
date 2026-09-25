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
  `SearchChromeOutlineBorder`, `KeyboardPill*`, `InlineEngineHighlight*`.
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
| `AppBottomPopup` | Bottom-anchored popup with header and scrollable card of options |
| `ItemMenuPopup` | Long-press action menus: tile grids, rows, buttons, long-press dropdowns. Never leave placeholder gaps between options; reflow so any gap falls only at the end of the last row. |
| `TipBanner` | Dismissible tips/hints, optional links and trailing action |
| `AppPill` | Small pill labels |
| `AppVoiceCallIcon` | App logo plus phone icon for call actions |
| `dialogTextFieldColors()` | Text fields inside dialogs |
| `SettingsCard` and `Settings*Row` (`settings/shared/`) | Settings cards, toggle/checkbox/navigation rows |
