package com.tk.quicksearch.search.directSearch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

@Composable
fun GeminiLoadingAnimation(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GeminiSparkle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "sparkleRotation",
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_gemini_sparkle_animation),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .rotate(rotation),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            val barModifier =
                Modifier
                    .height(14.dp)
                    .geminiLoadingEffect(primaryColor, containerColor)

            Box(
                modifier =
                    barModifier
                        .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier =
                    barModifier
                        .fillMaxWidth(0.95f),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier =
                    barModifier
                        .fillMaxWidth(0.6f),
            )
        }
    }
}

fun Modifier.geminiLoadingEffect(
    primaryColor: Color,
    containerColor: Color,
): Modifier =
    composed {
        var size by remember {
            mutableStateOf(IntSize.Zero)
        }
        val transition = rememberInfiniteTransition(label = "GeminiLoading")
        val colors =
            listOf(
                primaryColor,
                containerColor,
                primaryColor,
            )
        val width = size.width.toFloat()

        val offsetXAnimation by transition.animateFloat(
            initialValue = -width,
            targetValue = width,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "gradientAnimation",
        )

        this
            .onGloballyPositioned {
                size = it.size
            }.background(
                brush =
                    Brush.linearGradient(
                        colors = colors,
                        start = Offset(x = offsetXAnimation, y = 0f),
                        end = Offset(x = offsetXAnimation + width, y = 0f),
                        tileMode = TileMode.Clamp,
                    ),
                shape = RoundedCornerShape(24.dp),
            )
    }
