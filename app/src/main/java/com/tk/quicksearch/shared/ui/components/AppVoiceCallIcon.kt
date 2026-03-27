package com.tk.quicksearch.shared.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppColors

/**
 * Renders a composite voice-call icon: the app logo at its natural brand color layered with
 * a phone icon tinted by [AppColors.CallIconTint] (dark grey in light mode, white in dark mode).
 *
 * When [enabled] is false both layers are tinted with [MaterialTheme.colorScheme.onSurfaceVariant]
 * so the icon appears fully muted.
 *
 * @param logoPainterRes The drawable resource for the app logo portion of the icon
 *                       (e.g. [R.drawable.whatsapp_call] — logo only, no phone path).
 * @param size           The size to render the icon at.
 * @param enabled        Whether the icon is in an active/enabled state.
 */
@Composable
fun AppVoiceCallIcon(
    @DrawableRes logoPainterRes: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant
    val logoTint = if (enabled) Color.Unspecified else disabledTint
    val phoneTint = if (enabled) AppColors.CallIconTint else disabledTint

    Box(modifier = modifier.size(size)) {
        Icon(
            painter = painterResource(id = logoPainterRes),
            contentDescription = null,
            tint = logoTint,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_call_overlay),
            contentDescription = null,
            tint = phoneTint,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
