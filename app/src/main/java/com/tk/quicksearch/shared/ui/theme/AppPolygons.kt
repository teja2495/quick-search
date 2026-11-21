package com.tk.quicksearch.shared.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

/**
 * The Material 3 Expressive shapes the app draws, from Material's [MaterialShapes] catalog
 * (normalized to a unit square). Decorative badges read them through [AppShapes].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object AppPolygons {
    val cookie9: RoundedPolygon get() = MaterialShapes.Cookie9Sided

    val cookie4: RoundedPolygon get() = MaterialShapes.Cookie4Sided
}

/** Compose [Shape] that draws a unit-square [polygon] scaled to the layout size. */
internal class PolygonShape(
    private val polygon: RoundedPolygon,
) : Shape {
    private val unitPath by lazy { polygon.toPath().asComposePath() }

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = androidx.compose.ui.graphics.Path().apply { addPath(unitPath) }
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}
