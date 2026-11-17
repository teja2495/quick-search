package com.tk.quicksearch.shared.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.pill
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * The app's Material 3 Expressive shape catalog, normalized to a unit square. Decorative badges
 * read them through [AppShapes]; the Gemini loading animation morphs between them. Once material3
 * ships `MaterialShapes`, point these entries at it.
 */
object AppPolygons {
    val softBurst: RoundedPolygon by lazy {
        RoundedPolygon.star(
            numVerticesPerRadius = 10,
            innerRadius = 0.82f,
            rounding = CornerRounding(0.12f),
            innerRounding = CornerRounding(0.12f),
        ).normalized()
    }

    val cookie9: RoundedPolygon by lazy {
        RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = 0.8f, rounding = CornerRounding(0.3f)).normalized()
    }

    val pentagon: RoundedPolygon by lazy {
        RoundedPolygon(numVertices = 5, rounding = CornerRounding(0.3f)).normalized()
    }

    val pill: RoundedPolygon by lazy { RoundedPolygon.pill(width = 1.6f, height = 1f).normalized() }

    val sunny: RoundedPolygon by lazy {
        RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.8f, rounding = CornerRounding(0.15f)).normalized()
    }

    val cookie4: RoundedPolygon by lazy {
        RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.62f, rounding = CornerRounding(0.42f)).normalized()
    }

    val oval: RoundedPolygon by lazy { RoundedPolygon.circle(numVertices = 8).normalized() }
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
