package com.tk.quicksearch.onboarding

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.ui.theme.floatingActionShape
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppOutlinedButton

/**
 * Material 3 Expressive medium (56dp) button for onboarding actions: fully round at rest, squaring
 * its corners while pressed, with a `titleMedium` label. Filled with the accent, or outlined for
 * secondary actions such as Skip and for the glowing button that finishes setup.
 */
@Composable
fun OnboardingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    enabled: Boolean = true,
    finishesSetup: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var windowBounds by remember { mutableStateOf<Rect?>(null) }
    // The finishing tap turns this button into the search bar; see WelcomeMorph.
    val welcomeMorph = LocalWelcomeMorph.current.takeIf { finishesSetup }
    val isOutlined = outlined || finishesSetup
    val labelStyle = MaterialTheme.typography.titleMedium
    val containerColor = if (isOutlined) Color.Transparent else AppColors.Accent
    val contentColor = if (isOutlined) MaterialTheme.colorScheme.onSurfaceVariant else AppColors.OnAccent
    val handleClick = {
        val bounds = windowBounds
        if (welcomeMorph != null && bounds != null) {
            welcomeMorph.start(bounds, text, containerColor, contentColor, labelStyle, onClick)
        } else {
            onClick()
        }
    }
    val buttonModifier =
        modifier
            .fillMaxWidth()
            .height(DesignTokens.FloatingActionHeight)
            .then(
                if (welcomeMorph == null) {
                    Modifier
                } else {
                    Modifier
                        .onGloballyPositioned { windowBounds = it.boundsInWindow() }
                        .graphicsLayer { alpha = if (welcomeMorph.isActive) 0f else 1f }
                        .welcomeGlow()
                },
            )
    val shape = floatingActionShape(interactionSource)
    val content: @Composable () -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(DesignTokens.IconSize),
            )
            Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
        }
        Text(
            text = text,
            style = labelStyle,
            textAlign = TextAlign.Center,
        )
    }
    if (isOutlined) {
        AppOutlinedButton(
            onClick = handleClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            interactionSource = interactionSource,
            // The welcome glow is this button's border.
            border = if (welcomeMorph != null) null else ButtonDefaults.outlinedButtonBorder(enabled),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = contentColor,
                ),
        ) { content() }
    } else {
        AppButton(
            onClick = handleClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            interactionSource = interactionSource,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor,
                ),
        ) { content() }
    }
}

/** Bottom action of an onboarding step, inset like the other onboarding steps. */
@Composable
fun OnboardingBottomActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    enabled: Boolean = true,
    finishesSetup: Boolean = false,
) {
    OnboardingActionButton(
        text = text,
        onClick = onClick,
        outlined = outlined,
        enabled = enabled,
        finishesSetup = finishesSetup,
        modifier = modifier.padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
    )
}
