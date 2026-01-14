# Quick Search Design System

This document describes the centralized design system for the Quick Search Android app. It provides a single source of truth for spacing, shapes, colors, typography, and reusable UI components.

---

## Table of Contents

1. [Overview](#overview)
2. [Design Tokens](#design-tokens)
3. [Spacing System](#spacing-system)
4. [Shape System](#shape-system)
5. [Elevation System](#elevation-system)
6. [Component Sizes](#component-sizes)
7. [Common Components](#common-components)
8. [Usage Guidelines](#usage-guidelines)
9. [Migration Guide](#migration-guide)

---

## Overview

The Quick Search design system is built on **DesignTokens** and **CommonComponents** to ensure consistency across the entire application. All spacing, shapes, and common UI patterns are centralized in:

- **File**: [`DesignTokens.kt`](file:///Users/teja2495/Projects/quick-search/app/src/main/java/com/tk/quicksearch/ui/theme/DesignTokens.kt)
- **File**: [`CommonComponents.kt`](file:///Users/teja2495/Projects/quick-search/app/src/main/java/com/tk/quicksearch/ui/components/CommonComponents.kt)

### Benefits

✅ **Consistency** - All UI elements follow the same spacing and styling rules
✅ **Maintainability** - Change values in one place to update the entire app
✅ **Developer Experience** - Autocomplete shows available tokens
✅ **Scalability** - Easy to add new tokens as the app grows
✅ **Type Safety** - Compile-time errors prevent typos

---

## Design Tokens

All design tokens are accessed via the `DesignTokens` object:

```kotlin
import com.tk.quicksearch.ui.theme.DesignTokens

// Example usage
Column(
    modifier = Modifier.padding(DesignTokens.SpacingLarge),
    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)
)
```

---

## Spacing System

The spacing system follows the **Material 3 4-point grid** for visual harmony.

### Universal Spacing Scale

| Token | Value | Use Cases |
|-------|-------|-----------|
| `SpacingXXSmall` | 4dp | Minimal text spacing, very tight layouts (Aligned to 4dp grid) |
| `SpacingXSmall` | 4dp | Small gaps between icons and text, minimal padding |
| `SpacingSmall` | 8dp | Standard gaps between related elements, compact sections |
| `SpacingMedium` | 12dp | Standard gaps between sections, moderate padding |
| `SpacingLarge` | 16dp | Section spacing, standard card padding |
| `SpacingXLarge` | 20dp | Large section padding, card content padding |
| `SpacingXXLarge` | 24dp | Major section breaks, large padding |
| `Spacing28` | 28dp | Specific container or break needs |
| `SpacingHuge` | 32dp | Major visual breaks, special spacing needs |
| `Spacing40` | 40dp | Major visual breaks |
| `Spacing48` | 48dp | Major visual breaks |

### Examples

```kotlin
// Compact spacing within a component
Row(
    horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    modifier = Modifier.padding(DesignTokens.SpacingXSmall)
)

// Standard card padding
Column(
    modifier = Modifier.padding(DesignTokens.SpacingLarge),
    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium)
)

// Large content area
Column(
    modifier = Modifier.padding(
        horizontal = DesignTokens.SpacingXLarge,
        vertical = DesignTokens.SpacingLarge
    )
)
```

### Semantic Spacing (Legacy)

These are mapped to the universal scale for backward compatibility:

| Token | Maps To | Value |
|-------|---------|-------|
| `ContentHorizontalPadding` | `SpacingXLarge` | 20dp |
| `HeaderVerticalPadding` | `SpacingLarge` | 16dp |
| `HeaderIconSpacing` | `SpacingSmall` | 8dp |
| `SectionTopPadding` | `SpacingXXLarge` | 24dp |
| `SectionTitleBottomPadding` | `SpacingSmall` | 8dp |

**Recommendation**: Use universal spacing tokens for new code.

---

## Shape System

All shapes use consistent corner radii following Material 3 guidelines.

### Shape Scale

| Token | Radius | Use Cases |
|-------|--------|-----------|
| `ShapeExtraSmall` | 4dp | Autocomplete menu, small containers |
| `ShapeSmall` | 8dp | Chips, Dialogs, Small FAB |
| `ShapeMedium` | 12dp | Cards, Small FAB |
| `ShapeLarge` | 16dp | Large FAB, Extended FAB, Drawers |
| `ShapeXLarge` | 28dp | Large Dialogs, Large Cards |
| `ShapeXXLarge` | 28dp | Search Bar, Round Buttons |
| `ShapeFull` | Circle | Fully rounded elements |

### Examples

```kotlin
// Standard card
ElevatedCard(
    shape = DesignTokens.ShapeMedium
) { /* content */ }

// Search bar
Surface(
    shape = DesignTokens.ShapeXXLarge
) { /* search field */ }

// Small chip
Surface(
    shape = DesignTokens.ShapeSmall,
    modifier = Modifier.clickable { /* action */ }
) { /* chip content */ }
```

### Legacy Shape Tokens

| Token | Maps To | Radius |
|-------|---------|--------|
| `CardShape` | `ShapeMedium` | 12dp |
| `ExtraLargeCardShape` | `ShapeXLarge` | 28dp |

---

## Elevation System

Follows Material 3 elevation levels.

| Token | Value | Use Cases |
|-------|-------|-----------|
| `ElevationLevel0` | 0dp | Surface, No elevation |
| `ElevationLevel1` | 1dp | Low elevation |
| `ElevationLevel2` | 3dp | Standard Cards (Elevated) |
| `ElevationLevel3` | 6dp | Moderate elevation |
| `ElevationLevel4` | 8dp | Dialogs |
| `ElevationLevel5` | 12dp | Modal sheets |

---

## Component Sizes

Consistent sizing for icons, borders, and other UI elements.

### Icon Sizes

| Token | Size | Use Cases |
|-------|------|-----------|
| `IconSizeSmall` | 20dp | Compact UI elements (4dp grid aligned) |
| `IconSize` | 24dp | **Standard** UI icons |
| `LargeIconSize` | 28dp | Messaging options, prominent items |
| `IconSizeXLarge` | 52dp | App icons in grid |
| `AppIconSize` | 64dp | App icon container size |

### Other Sizes

| Token | Size | Use Cases |
|-------|------|-----------|
| `BorderWidth` | 1dp | Standard borders, outlines |
| `DividerThickness` | 0.5dp | Horizontal/vertical dividers |
| `ButtonCornerRadius` | 24dp | Button shapes |

---

## Common Components

Reusable UI components that follow the design system automatically.

### AppCard

Standardized card wrapper with consistent styling and wallpaper background support.

```kotlin
import com.tk.quicksearch.ui.components.AppCard

AppCard(
    showWallpaperBackground = false
) {
    // Your content here
    Text("Card content")
}
```

### SectionDivider

Consistent horizontal divider (0.5dp thickness).

```kotlin
import com.tk.quicksearch.ui.components.SectionDivider

Column {
    Text("Item 1")
    SectionDivider()
    Text("Item 2")
}
```

### IconWithText

Icon + text row pattern with standard 8dp spacing.

```kotlin
import com.tk.quicksearch.ui.components.IconWithText

IconWithText(
    icon = Icons.Rounded.Search,
    text = "Search",
    iconTint = MaterialTheme.colorScheme.primary,
    textStyle = MaterialTheme.typography.bodyLarge
)
```

### LoadingIndicator

Standardized loading state with optional message.

```kotlin
import com.tk.quicksearch.ui.components.LoadingIndicator

LoadingIndicator(
    message = "Loading results..."
)
```

### EmptyStateMessage

Empty state display with consistent spacing.

```kotlin
import com.tk.quicksearch.ui.components.EmptyStateMessage

EmptyStateMessage(
    title = "No results found",
    subtitle = "Try a different search query"
)
```

### SectionHeader

Section titles with optional subtitles.

```kotlin
import com.tk.quicksearch.ui.components.SectionHeader

SectionHeader(
    title = "Recent Contacts",
    subtitle = "Last 7 days"
)
```

---

## Usage Guidelines

### When to Use Design Tokens

✅ **Always use design tokens for:**
- Padding and margins
- Spacing between elements
- Card corner radii
- Icon sizes
- Divider thickness
- Border widths
- Elevations

❌ **Don't use design tokens for:**
- Component-specific sizes that don't fit the scale
- Animations and transitions
- Custom spacing required for pixel-perfect designs

### Best Practices

#### 1. Prefer Universal Spacing Tokens

```kotlin
// ✅ Good - Using universal tokens
Column(
    modifier = Modifier.padding(DesignTokens.SpacingLarge),
    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)
)

// ❌ Avoid - Hardcoded values
Column(
    modifier = Modifier.padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
)
```

#### 2. Use Common Components

```kotlin
// ✅ Good - Using common component
SectionDivider()

// ❌ Avoid - Manual divider
HorizontalDivider(
    thickness = 0.5.dp,
    color = MaterialTheme.colorScheme.outlineVariant
)
```

#### 3. Consistent Shape Usage

```kotlin
// ✅ Good - Using shape tokens
ElevatedCard(
    shape = DesignTokens.ShapeMedium
) { /* content */ }

// ❌ Avoid - Hardcoded shape
ElevatedCard(
    shape = RoundedCornerShape(12.dp)
) { /* content */ }
```

#### 4. Import Design Tokens Once

```kotlin
import com.tk.quicksearch.ui.theme.DesignTokens

// Then use throughout the file
DesignTokens.SpacingLarge
DesignTokens.ShapeMedium
DesignTokens.IconSize
```

---

## Migration Guide

### Migrating Existing Code

#### Step 1: Spacing Updates
Existing hardcoded `2.dp` should utilize `SpacingXXSmall` (now 4dp) or remain hardcoded if sub-grid precision is absolutely required.

#### Step 2: Shape Updates
Note that shape tokens have been rescaled to match Material 3:
- Old `ShapeSmall` (12dp) users might want to switch to `ShapeMedium` (12dp) if they want to keep the same radius.
- Old `ShapeMedium` (16dp) users might want to switch to `ShapeLarge` (16dp) if they want to keep the same radius.
- However, adopting the new Semantic names (`ShapeSmall`, `ShapeMedium`) is better for long-term consistency.

### Common Replacements Reference

| Hardcoded / Old | Design Token | New Value |
|-----------------|--------------|-----------|
| `2.dp` | `SpacingXXSmall` | **4dp** |
| `4.dp` | `SpacingXSmall` | 4dp |
| `8.dp` | `SpacingSmall` | 8dp |
| `12.dp` | `SpacingMedium` | 12dp |
| `16.dp` | `SpacingLarge` | 16dp |
| `20.dp` | `SpacingXLarge` | 20dp |
| `24.dp` | `SpacingXXLarge` | 24dp |
| `28.dp` | `Spacing28` | 28dp |
| `RoundedCornerShape(8.dp)` | `ShapeSmall` | 8dp |
| `RoundedCornerShape(12.dp)` | `ShapeMedium` | 12dp |
| `RoundedCornerShape(16.dp)` | `ShapeLarge` | 16dp |
| `RoundedCornerShape(28.dp)` | `ShapeXLarge` | 28dp |
| `Modifier.size(20.dp)` | `IconSizeSmall` | 20dp |
| `Modifier.size(24.dp)` | `IconSize` | 24dp |

---

## Semantic Colors

In addition to Material3 theme colors (`MaterialTheme.colorScheme`), `DesignTokens` provides some semantic colors, primarily for specific brand actions.

**Note:** Wherever possible, prefer using `MaterialTheme.colorScheme` (e.g., `primary`, `tertiary`, `error`) over hardcoded hex colors to support dynamic theming.

### Action Colors

| Token | Color | Hex | Use Cases |
|-------|-------|-----|-----------|
| `ColorPhone` | Material Green | `#4CAF50` | Phone call actions |
| `ColorSms` | Material Blue | `#2196F3` | SMS actions |
| `ColorWhatsApp` | WhatsApp Green | `#25D366` | WhatsApp brand |
| `ColorTelegram` | Telegram Blue | `#0088CC` | Telegram brand |
| `ColorEmail` | Material Orange | `#FF9800` | Email actions |

### Examples

```kotlin
// Contact action button
Icon(
    imageVector = Icons.Rounded.Phone,
    tint = DesignTokens.ColorPhone // Consider using a theme color if possible
)
```

---

## Summary

✅ Use `DesignTokens` for spacing, shapes, and sizes
✅ Use `CommonComponents` for reusable UI patterns
✅ Follow the 4-point grid
✅ Use semantic component names from the tables above
✅ Prefer Theme colors over fixed colors

This design system ensures **consistency**, **maintainability**, and a **premium Material 3 user experience** across the entire Quick Search app!
