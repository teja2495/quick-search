package com.tk.quicksearch.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized design tokens for the QuickSearch app.
 * Contains spacing, shapes, and other design constants used throughout the settings UI.
 */
object DesignTokens {

    // ============================================================================
    // SPACING
    // ============================================================================

    /**
     * Horizontal padding for content areas (screens, cards)
     */
    val ContentHorizontalPadding = 20.dp

    /**
     * Vertical padding for headers
     */
    val HeaderVerticalPadding = 16.dp

    /**
     * Spacing between header icon and text
     */
    val HeaderIconSpacing = 8.dp

    /**
     * Top padding between sections
     */
    val SectionTopPadding = 24.dp

    /**
     * Bottom padding for section titles
     */
    val SectionTitleBottomPadding = 8.dp

    /**
     * Bottom padding for section descriptions
     */
    val SectionDescriptionBottomPadding = 16.dp

    /**
     * Bottom padding for version display
     */
    val VersionBottomPadding = 100.dp

    /**
     * Top padding for version display
     */
    val VersionTopPadding = 45.dp

    // ============================================================================
    // CARD SPACING
    // ============================================================================

    /**
     * Horizontal padding inside cards
     */
    val CardHorizontalPadding = 20.dp

    /**
     * Vertical padding for card items (not first/last)
     */
    val CardVerticalPadding = 12.dp

    /**
     * Top padding for the first item in a card
     */
    val CardTopPadding = 20.dp

    /**
     * Bottom padding for the last item in a card
     */
    val CardBottomPadding = 20.dp

    /**
     * Spacing between toggle switch and text
     */
    val ToggleSpacing = 12.dp

    /**
     * Spacing between items in a row
     */
    val ItemRowSpacing = 12.dp

    /**
     * Spacing between columns of text
     */
    val TextColumnSpacing = 2.dp

    /**
     * Spacing between text button icon and text
     */
    val TextButtonIconSpacing = 4.dp

    /**
     * Spacing between messaging option chips
     */
    val ChipSpacing = 12.dp

    /**
     * Vertical padding for chips
     */
    val ChipVerticalPadding = 12.dp

    /**
     * Horizontal padding for chips
     */
    val ChipHorizontalPadding = 12.dp

    /**
     * Spacing between chip icon and text
     */
    val ChipIconSpacing = 10.dp

    // ============================================================================
    // COMPONENT SIZES
    // ============================================================================

    /**
     * Standard icon size
     */
    val IconSize = 24.dp

    /**
     * Large icon size for messaging options
     */
    val LargeIconSize = 28.dp

    /**
     * Border width for chips
     */
    val BorderWidth = 1.dp

    /**
     * Approximate height of draggable items
     */
    val DraggableItemHeight = 60.dp

    // ============================================================================
    // SHAPES
    // ============================================================================

    /**
     * Standard card shape
     */
    val CardShape: Shape = RoundedCornerShape(16.dp)

    /**
     * Extra large card shape for main cards
     */
    val ExtraLargeCardShape: Shape = RoundedCornerShape(28.dp)

    // ============================================================================
    // DRAG AND DROP
    // ============================================================================

    /**
     * Drag threshold for reordering
     */
    val DragThreshold = 0.5f

    /**
     * Drag alpha for visual feedback
     */
    val DragAlpha = 0.8f

    /**
     * Spring damping ratio for drag animations
     */
    val SpringDampingRatio = 0.8f

    /**
     * Spring stiffness for drag animations
     */
    val SpringStiffness = 300f

    // ============================================================================
    // UTILITY FUNCTIONS
    // ============================================================================

    /**
     * Creates padding values for single card content
     */
    fun singleCardPadding() = androidx.compose.foundation.layout.PaddingValues(
        start = CardHorizontalPadding,
        top = CardTopPadding,
        end = CardHorizontalPadding,
        bottom = CardBottomPadding
    )

    /**
     * Creates header padding values
     */
    fun headerPadding() = androidx.compose.foundation.layout.PaddingValues(
        horizontal = ContentHorizontalPadding,
        vertical = HeaderVerticalPadding
    )

    /**
     * Calculates top padding for card items
     */
    fun cardItemTopPadding(isFirstItem: Boolean) = if (isFirstItem) CardTopPadding else CardVerticalPadding

    /**
     * Calculates bottom padding for card items
     */
    fun cardItemBottomPadding(isLastItem: Boolean) = if (isLastItem) CardBottomPadding else CardVerticalPadding
}
