package com.tk.quicksearch.search.searchEngines

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Layout modifier that extends the content width to reach screen edges
 * by compensating for parent padding (20dp on each side).
 * 
 * Note: This must be used within a Composable context where LocalDensity is available.
 */
@Composable
fun Modifier.extendToScreenEdges(): Modifier {
    val density = LocalDensity.current
    val parentPadding = with(density) { 20.dp.roundToPx() }
    
    return this.layout { measurable, constraints ->
        val extendedWidth = constraints.maxWidth + (parentPadding * 2)
        val extendedConstraints = constraints.copy(
            minWidth = extendedWidth,
            maxWidth = extendedWidth
        )
        val placeable = measurable.measure(extendedConstraints)
        layout(
            width = constraints.maxWidth, // Report parent width to avoid overflow
            height = placeable.height
        ) {
            // Place 20dp to the left so it extends to screen edges
            placeable.placeRelative(x = -parentPadding, y = 0)
        }
    }
}





