package com.tk.quicksearch.shared.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Centralized design tokens for the QuickSearch app. Refined to align with Material 3 guidelines
 * (4dp grid, standard shapes).
 */
object DesignTokens {
    // ============================================================================
    // UNIVERSAL SPACING SCALE (Material 3 - 4dp Grid)
    // ============================================================================

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

    /** Minimum touch target for any tappable element (Material 3 accessibility) - 48dp */
    val MinTouchTarget = 48.dp

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

    /** Spacing between items in a row */
    val ItemRowSpacing = SpacingMedium // 12.dp

    /** Spacing between columns of text */
    val TextColumnSpacing = SpacingXSmall // 4.dp (Updated from 2dp to match grid)

    /** Spacing between text button icon and text */
    val TextButtonIconSpacing = SpacingXSmall // 4.dp

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

    /** Pencil icon inside the small edit badge on an editable icon or avatar. */
    val EditBadgeIconSize = 12.dp

    /** Size of a small inline `CircularProgressIndicator` that stands in for an icon or icon button. */
    val InlineProgressSize = IconSize

    /** Stroke width of [InlineProgressSize] progress indicators. */
    val InlineProgressStrokeWidth = 2.dp

    /** Large icon size - 28dp M3 Large usually 40dp+, but 28dp fits grid. */
    val LargeIconSize = 28.dp

    /** Smaller icon for Signal message (logo has more visual weight). */
    val SignalMessageIconSize = 26.dp

    /** Extra large icon size - 52dp App icons in grid. (52/4 = 13). Fits grid. */
    val IconSizeXLarge = 52.dp

    /** Minimum height of a one-line list row (Material 3 list item). */
    val ListItemMinHeight = 56.dp

    /** Minimum height of an At a Glance row on Home. */
    val GlanceRowMinHeight = 52.dp

    /** Horizontal inset of an At a Glance row; every glance row shares it so they line up (off-grid). */
    val GlanceRowHorizontalPadding = 7.dp

    /**
     * Start inset of the 24dp leading glyph in notes, reminders, calendar, and bookmark result rows,
     * matching [GlanceRowHorizontalPadding] so their icons line up with the glance rows (off-grid).
     */
    val ResultIconStartInset = GlanceRowHorizontalPadding

    /** App icon container size - 64dp */
    val AppIconSize = 64.dp

    /** Border width for chips and outlines */
    val BorderWidth = 1.dp

    /** Divider thickness */
    val DividerThickness = 0.5.dp

    // ============================================================================
    // CORNERS (Material 3 corner scale)
    // ============================================================================
    // Every corner radius in the app comes from this scale or a role token below it. Shapes are
    // built only in [AppShapes]; a new design language changes the values here, not at call sites.

    /** No rounding. */
    val CornerNone = 0.dp

    /** Extra small corner - 4dp. */
    val CornerExtraSmall = 4.dp

    /** Small corner - 8dp. */
    val CornerSmall = 8.dp

    /** Medium corner - 12dp. */
    val CornerMedium = 12.dp

    /** Large corner - 16dp. */
    val CornerLarge = 16.dp

    /** Large increased corner - 20dp (Material 3 Expressive). */
    val CornerLargeIncreased = 20.dp

    /** Extra large corner - 28dp. */
    val CornerExtraLarge = 28.dp

    /** Corner of small icon tiles inside rows. */
    val CornerIconTile = 10.dp

    /** Corner of editable icon frames. */
    val CornerIconFrame = 14.dp

    /** Corner of website favicons. */
    val CornerFavicon = 6.dp

    /** Corner of the selected search engine highlight. */
    val CornerEngineHighlight = 18.dp

    /** Corner of single-line inline text inputs. */
    val CornerInlineInput = 24.dp

    // ============================================================================
    // PRESS MORPH (Material 3 Expressive shape change on press)
    // ============================================================================
    // Read through [AppShapes] helpers (pressMorphShape, segment shapes).

    /** Corner a fully round extra-small/small button (32-40dp) squares to while pressed. */
    val ButtonPressedCornerSmall = CornerSmall

    /** Corner a fully round medium button (48-56dp) squares to while pressed. */
    val ButtonPressedCornerMedium = CornerLarge

    /** Inner corner a pressed segmented tile rounds to. */
    val SegmentPressedInnerCorner = CornerLarge

    /** Extra-small button height (Material 3 Expressive XS): compact actions beside text. */
    val ButtonHeightExtraSmall = 32.dp

    /** Small button height (Material 3 Expressive S): the default button. */
    val ButtonHeightSmall = 40.dp

    /** Icon size in an extra-small button. */
    val ButtonIconSizeExtraSmall = 16.dp

    /** Icon size in a small button. */
    val ButtonIconSizeSmall = 18.dp

    /** Minimum height of a full-size tonal action inside a settings tile. */
    val TonalSettingsButtonHeight = 48.dp

    /** Height of floating actions (bottom bars of settings pages, onboarding, save buttons). */
    val FloatingActionHeight = 56.dp

    /** Gap between the buttons of a connected button group. */
    val ConnectedGroupGap = 2.dp

    /** Corners between neighbouring buttons of a connected button group. */
    val ConnectedGroupInnerCorner = CornerSmall

    /** Maximum width of floating action popups on tablets and landscape windows. */
    val ItemMenuMaxWidth = 640.dp

    /** Minimum height of an action row, including its touch target. */
    val ItemMenuRowHeight = 56.dp

    /** Outer corners of a segmented result list (first and last tile): the result card radius. */
    val SegmentedListOuterCorner = CornerExtraLarge

    /** Corners between neighbouring tiles of a segmented result list. */
    val SegmentedListInnerCorner = CornerExtraSmall

    /** Gap between tiles of a segmented result list. */
    val SegmentedListGap = 2.dp

    /** Side inset inside a segmented tile (what the card used to pad its rows by). */
    val SegmentedListTileHorizontalPadding = SpacingMedium

    /** Top and bottom inset inside a segmented tile. */
    val SegmentedListTileVerticalPadding = SpacingXSmall

    /** Corner radius for widgets and cards shown in the widgets panel. */
    val WidgetPanelCornerRadius = CornerExtraLarge

    /**
     * Mix factor for [androidx.compose.material3.MaterialTheme.colorScheme] `primary` into the
     * neutral top-result highlight (wallpaper-aware white/black and elevated cards).
     */
    const val PredictedSubmitHighlightAccentBlend = 0.4f

    // ============================================================================
    // ELEVATION (Material 3)
    // ============================================================================

    val ElevationLevel0 = 0.dp
    val ElevationLevel2 = 3.dp // Standard Card (Elevated)
    val ElevationLevel3 = 6.dp
    val ElevationLevel4 = 8.dp
    val ElevationLevel5 = 12.dp

    // ============================================================================
    // DRAG AND DROP
    // ============================================================================

    /** Drag alpha for visual feedback */
    val DragAlpha = 0.8f

    // ============================================================================
    // WALLPAPER BACKGROUND CONSTANTS
    // ============================================================================

    /** Luminance threshold for determining dark mode (0.5f = 50% luminance) */
    val DarkModeLuminanceThreshold = 0.5f

    /** Red channel coefficient for luminance calculation (ITU-R BT.709 standard) */
    val LuminanceRedCoefficient = 0.299f

    /** Green channel coefficient for luminance calculation (ITU-R BT.709 standard) */
    val LuminanceGreenCoefficient = 0.587f

    /** Blue channel coefficient for luminance calculation (ITU-R BT.709 standard) */
    val LuminanceBlueCoefficient = 0.114f

    // ============================================================================
    // ONBOARDING
    // ============================================================================

    /** Horizontal padding for onboarding screens */
    val OnboardingHorizontalPadding = SpacingXXLarge

    /** Standard spacer used between onboarding sections */
    val OnboardingSectionSpacing = SpacingHuge

    /** Compact spacer used in onboarding content blocks */
    val OnboardingCompactSpacing = SpacingLarge

    /** Insets applied to full-width onboarding CTA buttons */
    val OnboardingButtonOuterHorizontalPadding = SpacingSmall

    /** Corner radius for permission cards in onboarding */
    val OnboardingPermissionCardCornerRadius = SpacingXLarge

    /** Max height of the onboarding permission list: four rows plus a faded half-row peek that signals scrolling */
    val OnboardingPermissionCardViewportHeight = 480.dp

    /** Backdrop blur radius used by full-screen onboarding dialogs */
    val OnboardingDialogBlurRadius = SpacingXLarge

    // ============================================================================
    // SEARCH FIELD
    // ============================================================================

    /** Border width for the animated search field border */
    val SearchFieldBorderWidth = 2.dp

    /** Peak opacity for the accent resting search field outline (multiplied by welcome fade 0–1) */
    const val SearchFieldAccentOutlineAlpha = 0.4f

    /** Border stroke for keyboard operator pills */
    val KeyboardPillBorderStrokeWidth = 0.75.dp

    /** Placeholder alpha for search field hint text */
    const val SearchFieldPlaceholderAlpha = 0.6f

    /** Width multiplier for animated search border gradient */
    const val SearchFieldGradientWidthMultiplier = 4f

    /** Progress multiplier used to slide search border gradient across the field */
    const val SearchFieldGradientTravelMultiplier = 3f

    // ============================================================================
    // UTILITY FUNCTIONS
    // ============================================================================

    /** Calculates top padding for card items */
    fun cardItemTopPadding(isFirstItem: Boolean) = if (isFirstItem) CardTopPadding else CardVerticalPadding

    /** Calculates bottom padding for card items */
    fun cardItemBottomPadding(isLastItem: Boolean) = if (isLastItem) CardBottomPadding else CardVerticalPadding
}
