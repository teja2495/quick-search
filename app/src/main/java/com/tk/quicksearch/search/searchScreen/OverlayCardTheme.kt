package com.tk.quicksearch.search.searchScreen

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tk.quicksearch.search.core.AppIconShape

internal val LocalOverlayResultCardColor =
    staticCompositionLocalOf<Color?> { null }

internal val LocalOverlayDividerColor =
    staticCompositionLocalOf<Color?> { null }

internal val LocalOverlayActionColor =
    staticCompositionLocalOf<Color?> { null }

internal val LocalAppIconShape =
    staticCompositionLocalOf<AppIconShape> { AppIconShape.SQUARE }