package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalOverlayResultCardColor =
    staticCompositionLocalOf<Color?> { null }

internal val LocalOverlayDividerColor =
    staticCompositionLocalOf<Color?> { null }

internal val LocalOverlayActionColor =
    staticCompositionLocalOf<Color?> { null }

private const val OverlayBlurCardAlphaLight = 0.72f
private const val OverlayBlurCardAlphaDark = 0.62f
private const val OverlayBlurDividerAlphaLight = 0.12f
private const val OverlayBlurDividerAlphaDark = 0.16f
private const val OverlayBlurActionAlphaLight = 0.58f
private const val OverlayBlurActionAlphaDark = 0.44f

internal fun overlayBlurResultCardColor(isDarkMode: Boolean): Color =
    if (isDarkMode) {
        Color(0xFF1A221F).copy(alpha = OverlayBlurCardAlphaDark)
    } else {
        Color(0xFFF7F9F2).copy(alpha = OverlayBlurCardAlphaLight)
    }

internal fun overlayBlurDividerColor(isDarkMode: Boolean): Color =
    if (isDarkMode) {
        Color.White.copy(alpha = OverlayBlurDividerAlphaDark)
    } else {
        Color.Black.copy(alpha = OverlayBlurDividerAlphaLight)
    }

internal fun overlayBlurActionColor(isDarkMode: Boolean): Color =
    if (isDarkMode) {
        Color.Black.copy(alpha = OverlayBlurActionAlphaDark)
    } else {
        Color.White.copy(alpha = OverlayBlurActionAlphaLight)
    }
