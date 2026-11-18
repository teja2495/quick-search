package com.tk.quicksearch.search.contacts.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.models.ContactActionIconStyle
import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import com.tk.quicksearch.shared.ui.components.AppVoiceCallIcon
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Icon for a contact card button's default action, styled like the contact row buttons. */
@Composable
internal fun ContactButtonActionIcon(
    action: ContactButtonAction,
    enabled: Boolean = true,
) {
    val disabledTint = MaterialTheme.colorScheme.onSurfaceVariant
    val logoTint = if (enabled) Color.Unspecified else disabledTint
    val callIconTint = if (enabled) AppColors.CallIconTint else disabledTint
    val iconSize = ContactUiConstants.ACTION_ICON_SIZE.dp
    val label = stringResource(action.labelRes)

    @Composable
    fun LogoIcon(
        @DrawableRes res: Int,
        tint: Color = logoTint,
        size: Dp = iconSize,
    ) {
        Icon(
            painter = painterResource(id = res),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }

    when (action) {
        ContactButtonAction.CALL ->
            Icon(
                imageVector = MaterialSymbols.Call,
                contentDescription = label,
                tint = callIconTint,
                modifier = Modifier.size(iconSize),
            )
        ContactButtonAction.SMS ->
            Icon(
                imageVector = MaterialSymbols.Sms,
                contentDescription = label,
                tint = callIconTint,
                modifier = Modifier.size((ContactUiConstants.ACTION_ICON_SIZE * 0.9f).dp),
            )
        ContactButtonAction.EMAIL ->
            Icon(
                imageVector = MaterialSymbols.Email,
                contentDescription = label,
                tint = callIconTint,
                modifier = Modifier.size(iconSize),
            )
        else -> {
            val drawable = checkNotNull(action.drawableRes)
            when (action.iconStyle) {
                ContactActionIconStyle.LOGO -> LogoIcon(drawable)
                ContactActionIconStyle.APP_VOICE_CALL ->
                    AppVoiceCallIcon(logoPainterRes = drawable, size = iconSize, enabled = enabled)
                ContactActionIconStyle.SIGNAL_MESSAGE ->
                    LogoIcon(
                        res = drawable,
                        tint = if (enabled) AppColors.ActionSignal else disabledTint,
                        size = DesignTokens.SignalMessageIconSize,
                    )
            }
        }
    }
}
