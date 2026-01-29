package com.tk.quicksearch.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Centralized design tokens for the QuickSearch app. Refined to align with Material 3 guidelines
 * (4dp grid, standard shapes).
 */
object DesignTokens {
    // ============================================================================
    // UNIVERSAL SPACING SCALE (Material 3 - 4dp Grid)
    // ============================================================================

    /**
     * Extra extra small spacing - 4dp (Aligned to 4dp grid) Use for: Minimal text spacing, very
     * tight layouts
     */
    val SpacingXXSmall = 4.dp

    /** Extra small spacing - 4dp Use for: Small gaps between icons and text, minimal padding */
    val SpacingXSmall = 4.dp

    /** Small spacing - 8dp Use for: Standard gaps between related elements, compact sections */
    val SpacingSmall = 8.dp

    /** Medium spacing - 12dp Use for: Standard gaps between sections, moderate padding */
    val SpacingMedium = 12.dp

    /** Large spacing - 16dp Use for: Section spacing, standard card padding */
    val SpacingLarge = 16.dp

    /** Extra large spacing - 20dp Use for: Large section padding, card content padding */
    val SpacingXLarge = 20.dp

    /** Extra extra large spacing - 24dp Use for: Major section breaks, large padding */
    val SpacingXXLarge = 24.dp

    /** Spacing 28dp - Grid aligned */
    val Spacing28 = 28.dp

    /** Huge spacing - 32dp Use for: Major visual breaks, special spacing needs */
    val SpacingHuge = 32.dp

    /** Major layout spacing - 40dp */
    val Spacing40 = 40.dp

    /** Major layout spacing - 48dp */
    val Spacing48 = 48.dp

    // ============================================================================
    // SEMANTIC SPACING
    // ============================================================================

    /** Horizontal padding for content areas (screens, cards) */
    val ContentHorizontalPadding = SpacingXLarge // 20.dp

    /** Vertical padding for headers */
    val HeaderVerticalPadding = SpacingLarge // 16.dp

    /** Spacing between header icon and text */
    val HeaderIconSpacing = SpacingSmall // 8.dp

    /** Top padding between sections */
    val SectionTopPadding = SpacingXXLarge // 24.dp

    /** Bottom padding for section titles */
    val SectionTitleBottomPadding = SpacingSmall // 8.dp

    /** Bottom padding for section descriptions */
    val SectionDescriptionBottomPadding = SpacingLarge // 16.dp

    /** Bottom padding for version display */
    val VersionBottomPadding = 100.dp

    /** Top padding for version display */
    val VersionTopPadding = 45.dp

    // ============================================================================
    // CARD SPACING
    // ============================================================================

    /** Horizontal padding inside cards */
    val CardHorizontalPadding = SpacingXLarge // 20.dp

    /** Vertical padding for card items (not first/last) */
    val CardVerticalPadding = SpacingMedium // 12.dp

    /** Top padding for the first item in a card */
    val CardTopPadding = SpacingXLarge // 20.dp

    /** Bottom padding for the last item in a card */
    val CardBottomPadding = SpacingXLarge // 20.dp

    /** Spacing between toggle switch and text */
    val ToggleSpacing = SpacingMedium // 12.dp

    /** Spacing between items in a row */
    val ItemRowSpacing = SpacingMedium // 12.dp

    /** Spacing between columns of text */
    val TextColumnSpacing = SpacingXXSmall // 4.dp (Updated from 2dp to match grid)

    /** Spacing between text button icon and text */
    val TextButtonIconSpacing = SpacingXSmall // 4.dp

    /** Spacing between messaging option chips */
    val ChipSpacing = SpacingMedium // 12.dp

    /** Vertical padding for chips */
    val ChipVerticalPadding = SpacingMedium // 12.dp

    /** Horizontal padding for chips */
    val ChipHorizontalPadding = SpacingMedium // 12.dp

    /** Spacing between chip icon and text */
    val ChipIconSpacing = 10.dp // Note: Off-grid (not div by 4), consider 8dp or 12dp

    // ============================================================================
    // COMPONENT SIZES
    // ============================================================================

    /** Small icon size - 20dp M3 Compact icon. On 4dp grid. */
    val IconSizeSmall = 20.dp

    /** Standard icon size - 24dp M3 Standard. */
    val IconSize = 24.dp

    /** Large icon size - 28dp M3 Large usually 40dp+, but 28dp fits grid. */
    val LargeIconSize = 28.dp

    /** Extra large icon size - 52dp App icons in grid. (52/4 = 13). Fits grid. */
    val IconSizeXLarge = 52.dp

    /** App icon container size - 64dp */
    val AppIconSize = 64.dp

    /** Border width for chips and outlines */
    val BorderWidth = 1.dp

    /** Divider thickness */
    val DividerThickness = 0.5.dp

    /** Approximate height of draggable items */
    val DraggableItemHeight = 60.dp

    // ============================================================================
    // SHAPES (Material 3 Scale)
    // ============================================================================

    /** Extra Small shape - 4dp Use for: Autocomplete menu, very small containers */
    val ShapeExtraSmall: Shape = RoundedCornerShape(4.dp)

    /** Small shape - 8dp Use for: Chips, Dialogs, Small FAB */
    val ShapeSmall: Shape = RoundedCornerShape(8.dp)

    /** Medium shape - 12dp Use for: Cards, Small FAB */
    val ShapeMedium: Shape = RoundedCornerShape(12.dp)

    /** Large shape - 16dp Use for: Large FAB, Extended FAB, Navigation drawers */
    val ShapeLarge: Shape = RoundedCornerShape(16.dp)

    /** Extra large shape - 28dp Use for: Large Dialogs, Large Cards */
    val ShapeXLarge: Shape = RoundedCornerShape(28.dp)

    /**
     * Extra extra large shape - 28dp (Circle-like for search bar height) Use for: Search bar,
     * Round buttons
     */
    val ShapeXXLarge: Shape = RoundedCornerShape(28.dp)

    /** Full circle shape */
    val ShapeFull: Shape = CircleShape

    /** Standard card shape Usage: Mapped to ShapeMedium (12dp) for M3 consistency (was 16dp) */
    val CardShape: Shape = ShapeMedium

    /** Extra large card shape for main cards Usage: Mapped to ShapeXLarge (28dp) */
    val ExtraLargeCardShape: Shape = ShapeXLarge

    /**
     * Button corner radius - 24dp Note: M3 buttons often use full circle height (e.g. 40dp
     * height -> 20dp radius). 24dp radius suitable for 48dp height buttons.
     */
    val ButtonCornerRadius = 24.dp

    // ============================================================================
    // ELEVATION (Material 3)
    // ============================================================================

    val ElevationLevel0 = 0.dp
    val ElevationLevel1 = 1.dp
    val ElevationLevel2 = 3.dp // Standard Card (Elevated)
    val ElevationLevel3 = 6.dp
    val ElevationLevel4 = 8.dp
    val ElevationLevel5 = 12.dp

    // ============================================================================
    // SEMANTIC COLORS
    // Note: In Material 3, prefer extracting colors from MaterialTheme.colorScheme
    // ============================================================================

    /**
     * Success/Phone action color - Material Green Use for: Phone call actions, success states,
     * positive indicators
     */
    val ColorPhone = Color(0xFF4CAF50)

    /** Info/SMS action color - Material Blue Use for: SMS actions, information states, links */
    val ColorSms = Color(0xFF2196F3)

    /** WhatsApp brand color Use for: WhatsApp-related actions and icons */
    val ColorWhatsApp = Color(0xFF25D366)

    /** Telegram brand color Use for: Telegram-related actions and icons */
    val ColorTelegram = Color(0xFF0088CC)

    /** Email action color - Material Orange Use for: Email actions, warnings */
    val ColorEmail = Color(0xFFFF9800)

    /**
     * Video call action color - Material Purple Use for: Video call actions, premium features
     */
    val ColorVideoCall = Color(0xFF9C27B0)

    /** Custom/neutral action color - Blue Gray Use for: Custom apps, neutral actions */
    val ColorCustom = Color(0xFF607D8B)

    /** Secondary/view action color - Gray Use for: View actions, secondary buttons */
    val ColorView = Color(0xFF9E9E9E)

    /**
     * Search bar text and icon color Use for: Search input text, search icons on dark
     * backgrounds
     */
    val ColorSearchText = Color(0xFFE0E0E0)

    // ============================================================================
    // DRAG AND DROP
    // ============================================================================

    /** Drag threshold for reordering */
    val DragThreshold = 0.5f

    /** Drag alpha for visual feedback */
    val DragAlpha = 0.8f

    /** Spring damping ratio for drag animations */
    val SpringDampingRatio = 0.8f

    /** Spring stiffness for drag animations */
    val SpringStiffness = 300f

    // ============================================================================
    // WALLPAPER BACKGROUND CONSTANTS
    // ============================================================================

    /** Standard animation duration for wallpaper background fade-in */
    val WallpaperFadeInDuration = 300

    /** Standard animation duration for short transitions (e.g. height changes) */
    val AnimationDurationShort = 200

    /** Standard animation duration for medium transitions (e.g. entry/exit fade) */
    val AnimationDurationMedium = 300

    /** Luminance threshold for determining dark mode (0.5f = 50% luminance) */
    val DarkModeLuminanceThreshold = 0.5f

    /** Red channel coefficient for luminance calculation (ITU-R BT.709 standard) */
    val LuminanceRedCoefficient = 0.299f

    /** Green channel coefficient for luminance calculation (ITU-R BT.709 standard) */
    val LuminanceGreenCoefficient = 0.587f

    /** Blue channel coefficient for luminance calculation (ITU-R BT.709 standard) */
    val LuminanceBlueCoefficient = 0.114f

    // ============================================================================
    // UTILITY FUNCTIONS
    // ============================================================================

    /** Creates padding values for single card content */
    fun singleCardPadding() =
        androidx.compose.foundation.layout.PaddingValues(
            start = CardHorizontalPadding,
            top = CardTopPadding,
            end = CardHorizontalPadding,
            bottom = CardBottomPadding,
        )

    /** Creates header padding values */
    fun headerPadding() =
        androidx.compose.foundation.layout.PaddingValues(
            horizontal = ContentHorizontalPadding,
            vertical = HeaderVerticalPadding,
        )

    /** Calculates top padding for card items */
    fun cardItemTopPadding(isFirstItem: Boolean) = if (isFirstItem) CardTopPadding else CardVerticalPadding

    /** Calculates bottom padding for card items */
    fun cardItemBottomPadding(isLastItem: Boolean) = if (isLastItem) CardBottomPadding else CardVerticalPadding
}
