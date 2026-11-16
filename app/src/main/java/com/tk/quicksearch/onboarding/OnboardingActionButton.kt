package com.tk.quicksearch.onboarding

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.tk.quicksearch.settings.shared.SettingsFloatingActionHeight
import com.tk.quicksearch.settings.shared.settingsFloatingActionShape
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Material 3 Expressive medium (56dp) button for onboarding actions: fully round at rest, squaring
 * its corners while pressed, with a `titleMedium` label. Filled with the accent, or outlined for
 * secondary actions such as Skip.
 */
@Composable
fun OnboardingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val buttonModifier =
        modifier
            .fillMaxWidth()
            .height(SettingsFloatingActionHeight)
    val shape = settingsFloatingActionShape(interactionSource)
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
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            interactionSource = interactionSource,
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) { content() }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = shape,
            interactionSource = interactionSource,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor = AppColors.OnAccent,
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
) {
    OnboardingActionButton(
        text = text,
        onClick = onClick,
        outlined = outlined,
        enabled = enabled,
        modifier = modifier.padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
    )
}
