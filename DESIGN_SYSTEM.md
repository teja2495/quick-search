# Quick Search Design System

This document describes the centralized design system for the Quick Search Android app. It provides a single source of truth for spacing, shapes, colors, typography, and reusable UI components.

---

## Table of Contents

1. [Overview](#overview)
2. [Design Tokens](#design-tokens)
3. [Spacing System](#spacing-system)
4. [Shape System](#shape-system)
5. [Component Sizes](#component-sizes)
6. [Common Components](#common-components)
7. [Usage Guidelines](#usage-guidelines)
8. [Migration Guide](#migration-guide)

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

The spacing system follows an **8-point grid** for visual harmony and consistency.

### Universal Spacing Scale

| Token | Value | Use Cases |
|-------|-------|-----------|
| `SpacingXXSmall` | 2dp | Minimal text spacing, very tight layouts |
| `SpacingXSmall` | 4dp | Small gaps between icons and text, minimal padding |
| `SpacingSmall` | 8dp | Standard gaps between related elements, compact sections |
| `SpacingMedium` | 12dp | Standard gaps between sections, moderate padding |
| `SpacingLarge` | 16dp | Section spacing, standard card padding |
| `SpacingXLarge` | 20dp | Large section padding, card content padding |
| `SpacingXXLarge` | 24dp | Major section breaks, large padding |
| `SpacingHuge` | 32dp | Major visual breaks, special spacing needs |

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

All shapes use consistent corner radii for a cohesive look.

### Shape Scale

| Token | Radius | Use Cases |
|-------|--------|-----------|
| `ShapeSmall` | 12dp | Small chips, compact buttons |
| `ShapeMedium` | 16dp | Standard cards, most UI elements |
| `ShapeLarge` | 20dp | Large surfaces, important cards |
| `ShapeXLarge` | 24dp | Extra large cards, buttons |
| `ShapeXXLarge` | 28dp | Search bar, main UI cards |

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
| `CardShape` | `ShapeMedium` | 16dp |
| `ExtraLargeCardShape` | `ShapeXXLarge` | 28dp |

---

## Component Sizes

Consistent sizing for icons, borders, and other UI elements.

### Icon Sizes

| Token | Size | Use Cases |
|-------|------|-----------|
| `IconSizeSmall` | 20dp | Compact UI elements, small indicators |
| `IconSize` | 24dp | Most UI icons, standard toolbar icons |
| `LargeIconSize` | 28dp | Messaging options, prominent UI elements |
| `IconSizeXLarge` | 52dp | App icons in grid (image size) |
| `AppIconSize` | 64dp | App icon container size |

### Other Sizes

| Token | Size | Use Cases |
|-------|------|-----------|
| `BorderWidth` | 1dp | Standard borders, outlines |
| `DividerThickness` | 0.5dp | Horizontal/vertical dividers |
| `ButtonCornerRadius` | 24dp | Button shapes |

### Examples

```kotlin
// Standard icon
Icon(
    imageVector = Icons.Rounded.Search,
    contentDescription = "Search",
    modifier = Modifier.size(DesignTokens.IconSize)
)

// Divider
HorizontalDivider(
    thickness = DesignTokens.DividerThickness,
    color = MaterialTheme.colorScheme.outlineVariant
)

// Border
Surface(
    modifier = Modifier.border(
        width = DesignTokens.BorderWidth,
        color = Color.White.copy(alpha = 0.3f),
        shape = DesignTokens.ShapeSmall
    )
) { /* content */ }
```

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
    shape = RoundedCornerShape(16.dp)
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

#### Step 1: Add Import

```kotlin
import com.tk.quicksearch.ui.theme.DesignTokens
```

#### Step 2: Replace Hardcoded Values

**Before:**
```kotlin
Column(
    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
)
```

**After:**
```kotlin
Column(
    modifier = Modifier.padding(
        horizontal = DesignTokens.SpacingXLarge,
        vertical = DesignTokens.SpacingLarge
    ),
    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall)
)
```

#### Step 3: Replace Shapes

**Before:**
```kotlin
Surface(
    shape = RoundedCornerShape(16.dp)
) { /* content */ }
```

**After:**
```kotlin
Surface(
    shape = DesignTokens.ShapeMedium
) { /* content */ }
```

#### Step 4: Replace Icon Sizes

**Before:**
```kotlin
Icon(
    imageVector = Icons.Rounded.Search,
    modifier = Modifier.size(24.dp)
)
```

**After:**
```kotlin
Icon(
    imageVector = Icons.Rounded.Search,
    modifier = Modifier.size(DesignTokens.IconSize)
)
```

### Common Replacements Reference

| Hardcoded Value | Design Token |
|-----------------|--------------|
| `Modifier.padding(2.dp)` | `Modifier.padding(DesignTokens.SpacingXXSmall)` |
| `Modifier.padding(4.dp)` | `Modifier.padding(DesignTokens.SpacingXSmall)` |
| `Modifier.padding(8.dp)` | `Modifier.padding(DesignTokens.SpacingSmall)` |
| `Modifier.padding(12.dp)` | `Modifier.padding(DesignTokens.SpacingMedium)` |
| `Modifier.padding(16.dp)` | `Modifier.padding(DesignTokens.SpacingLarge)` |
| `Modifier.padding(20.dp)` | `Modifier.padding(DesignTokens.SpacingXLarge)` |
| `Modifier.padding(24.dp)` | `Modifier.padding(DesignTokens.SpacingXXLarge)` |
| `Arrangement.spacedBy(8.dp)` | `Arrangement.spacedBy(DesignTokens.SpacingSmall)` |
| `Arrangement.spacedBy(12.dp)` | `Arrangement.spacedBy(DesignTokens.SpacingMedium)` |
| `RoundedCornerShape(12.dp)` | `DesignTokens.ShapeSmall` |
| `RoundedCornerShape(16.dp)` | `DesignTokens.ShapeMedium` |
| `RoundedCornerShape(20.dp)` | `DesignTokens.ShapeLarge` |
| `RoundedCornerShape(24.dp)` | `DesignTokens.ShapeXLarge` |
| `RoundedCornerShape(28.dp)` | `DesignTokens.ShapeXXLarge` |
| `Modifier.size(20.dp)` | `Modifier.size(DesignTokens.IconSizeSmall)` |
| `Modifier.size(24.dp)` | `Modifier.size(DesignTokens.IconSize)` |
| `Modifier.size(28.dp)` | `Modifier.size(DesignTokens.LargeIconSize)` |

---

## Semantic Colors

In addition to Material3 theme colors (`MaterialTheme.colorScheme`), we have semantic colors for specific use cases centralized in `DesignTokens`.

### Action Colors

Standardized colors for common messaging and communication actions:

| Token | Color | Hex | Use Cases |
|-------|-------|-----|-----------|
| `ColorPhone` | Material Green | `#4CAF50` | Phone call actions, success states, granted permissions |
| `ColorSms` | Material Blue | `#2196F3` | SMS actions, information states |
| `ColorWhatsApp` | WhatsApp Green | `#25D366` | WhatsApp calls, messages, video calls |
| `ColorTelegram` | Telegram Blue | `#0088CC` | Telegram calls, messages, video calls |
| `ColorEmail` | Material Orange | `#FF9800` | Email actions, warnings |
| `ColorVideoCall` | Material Purple | `#9C27B0` | Generic video call actions |
| `ColorCustom` | Blue Gray | `#607D8B` | Custom app actions, neutral states |
| `ColorView` | Gray | `#9E9E9E` | View/open actions, secondary buttons |

### UI Element Colors

| Token | Color | Hex | Use Cases |
|-------|-------|-----|-----------|
| `ColorSearchText` | Light Gray | `#E0E0E0` | Search input text, icons on dark backgrounds |

### Examples

```kotlin
// Contact action button
Icon(
    imageVector = Icons.Rounded.Phone,
    tint = DesignTokens.ColorPhone
)

// WhatsApp icon
Icon(
    imageVector = Icons.Rounded.WhatsApp,
    tint = DesignTokens.ColorWhatsApp
)

// Search text
Text(
    text = query,
    color = DesignTokens.ColorSearchText
)
```

### Best Practices

✅ **Use semantic colors for actions**: Prefer `ColorPhone` over `Color(0xFF4CAF50)`  
✅ **Consistent action colors**: All phone actions use the same green  
✅ **Brand colors**: Use brand-specific colors (WhatsApp, Telegram) for those apps  
❌ **Don't hardcode hex values**: Always use the named token

### Advanced: AppColors

For more complex theming needs (wallpaper backgrounds, overlays, gradients), use [`AppColors`](file:///Users/teja2495/Projects/quick-search/app/src/main/java/com/tk/quicksearch/ui/theme/AppColors.kt):

```kotlin
// Card colors based on wallpaper mode
AppColors.getCardColors(showWallpaperBackground = true)

// Overlays
AppColors.OverlayMedium  // Black 40% opacity
AppColors.OverlayHigh    // Black 50% opacity

// Theme colors
AppColors.ThemeDeepPurple  // Primary brand purple
AppColors.ThemeNeonPurple  // Secondary brand purple
```

---

## Typography

Typography is centralized in [`Type.kt`](file:///Users/teja2495/Projects/quick-search/app/src/main/java/com/tk/quicksearch/ui/theme/Type.kt).

### Font Family

The app uses **Google Sans** with various weights:
- Regular
- Medium
- Bold

### Text Styles

Access via `MaterialTheme.typography`:

```kotlin
Text(
    text = "Title",
    style = MaterialTheme.typography.titleLarge
)

Text(
    text = "Body text",
    style = MaterialTheme.typography.bodyMedium
)

Text(
    text = "Small caption",
    style = MaterialTheme.typography.bodySmall
)
```

---

## Colors

Colors are centralized in [`AppColors.kt`](file:///Users/teja2495/Projects/quick-search/app/src/main/java/com/tk/quicksearch/ui/theme/AppColors.kt).

### Card Colors

```kotlin
AppColors.getCardColors(showWallpaperBackground = false)
AppColors.getCardColors(showWallpaperBackground = true)
```

### Material3 Theme

Access via `MaterialTheme.colorScheme`:

```kotlin
Text(
    text = "Text",
    color = MaterialTheme.colorScheme.onSurface
)

Surface(
    color = MaterialTheme.colorScheme.surfaceVariant
) { /* content */ }
```

---

## File Structure

```
app/src/main/java/com/tk/quicksearch/
├── ui/
│   ├── theme/
│   │   ├── DesignTokens.kt     ← Spacing, shapes, sizes
│   │   ├── Type.kt              ← Typography
│   │   ├── AppColors.kt         ← Colors
│   │   └── Theme.kt             ← Material3 theme
│   └── components/
│       └── CommonComponents.kt  ← Reusable UI components
```

---

## Contributing

When adding new UI components:

1. **Check existing tokens** - Use existing spacing/shape tokens if possible
2. **Add new tokens** - If you need a new value, add it to `DesignTokens.kt`
3. **Document usage** - Add comments explaining when to use the new token
4. **Use common components** - Reuse existing components from `CommonComponents.kt`
5. **Update this guide** - Document new tokens or components

---

## FAQ

### Q: What if I need custom spacing that doesn't match the scale?

A: Use hardcoded values for truly custom cases, but document why. Most UI should use the standard scale.

### Q: Can I use legacy tokens like `CardHorizontalPadding`?

A: Yes, but prefer universal tokens (`SpacingXLarge`) for new code.

### Q: How do I know which spacing token to use?

A: Follow the 8-point grid: 8dp increments (Small, Medium, Large). Use the "Use Cases" column in the spacing table as a guide.

### Q: Should I use `MaterialTheme.shapes` or `DesignTokens`?

A: Use `DesignTokens` shapes for consistency. `MaterialTheme.shapes.extraLarge` is equivalent to `DesignTokens.ShapeXXLarge`.

---

## Summary

✅ Use `DesignTokens` for spacing, shapes, and sizes  
✅ Use `CommonComponents` for reusable UI patterns  
✅ Follow the 8-point grid for spacing  
✅ Use semantic component names from the tables above  
✅ Add comments when using custom values  

This design system ensures **consistency**, **maintainability**, and a **premium user experience** across the entire Quick Search app!
