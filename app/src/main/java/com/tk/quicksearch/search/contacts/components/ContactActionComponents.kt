package com.tk.quicksearch.search.contacts.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.shared.ui.components.AppVoiceCallIcon
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

// ============================================================================
// Contact Action Button (Messaging App Style)
// ============================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContactActionButton(
    method: ContactMethod,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    usePhoneIconForCallActions: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val callIconTint = AppColors.CallIconTint
    val iconColor =
        when (method) {
            is ContactMethod.Phone -> callIconTint

            is ContactMethod.Sms -> AppColors.ActionSms

            is ContactMethod.WhatsAppCall,
            is ContactMethod.TelegramCall,
            is ContactMethod.SignalCall,
            -> callIconTint

            is ContactMethod.WhatsAppMessage,
            is ContactMethod.WhatsAppVideoCall,
            is ContactMethod.TelegramMessage,
            is ContactMethod.TelegramVideoCall,
            is ContactMethod.SignalMessage,
            is ContactMethod.SignalVideoCall,
            -> Color.Unspecified

            is ContactMethod.GoogleMeet -> Color.Unspecified

            is ContactMethod.Email -> AppColors.ActionEmail

            is ContactMethod.VideoCall -> callIconTint

            is ContactMethod.CustomApp -> AppColors.ActionCustom

            is ContactMethod.ViewInContactsApp -> AppColors.ActionView
        }
    val actionButtonBorderColor = AppColors.OnboardingBubbleBorder
    val actionButtonContainerColor = Color.Transparent
    val actionButtonTextColor = AppColors.DialogText

    Surface(
        modifier =
            modifier
                .width(90.dp)
                .height(80.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .border(
                    width = DesignTokens.BorderWidth,
                    color = actionButtonBorderColor,
                    shape = DesignTokens.ShapeSmall,
                ),
        shape = DesignTokens.ShapeSmall,
        color = actionButtonContainerColor,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ContactMethodIcon(
                method = method,
                usePhoneIconForCallActions = usePhoneIconForCallActions,
                tintOverride = iconColor,
                iconSize = DesignTokens.LargeIconSize,
            )
            Text(
                text = getActionButtonLabel(method),
                style = MaterialTheme.typography.bodySmall,
                color = actionButtonTextColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.9f,
            )
        }
    }
}

@Composable
internal fun ContactMethodIcon(
    method: ContactMethod,
    usePhoneIconForCallActions: Boolean = false,
    modifier: Modifier = Modifier,
    tintOverride: Color? = null,
    iconSize: Dp = DesignTokens.LargeIconSize,
) {
    val callIconTint = AppColors.CallIconTint
    val tint =
        tintOverride
            ?: when (method) {
                is ContactMethod.Phone -> callIconTint
                is ContactMethod.Sms -> AppColors.ActionSms
                is ContactMethod.WhatsAppCall,
                is ContactMethod.TelegramCall,
                is ContactMethod.SignalCall,
                is ContactMethod.VideoCall,
                -> callIconTint
                is ContactMethod.WhatsAppMessage,
                is ContactMethod.WhatsAppVideoCall,
                is ContactMethod.TelegramMessage,
                is ContactMethod.TelegramVideoCall,
                is ContactMethod.SignalMessage,
                is ContactMethod.SignalVideoCall,
                is ContactMethod.GoogleMeet,
                -> Color.Unspecified
                is ContactMethod.Email -> AppColors.ActionEmail
                is ContactMethod.CustomApp -> AppColors.ActionCustom
                is ContactMethod.ViewInContactsApp -> AppColors.ActionView
            }

    ContactActionIcon(
        method = method,
        tint = tint,
        usePhoneIconForCallActions = usePhoneIconForCallActions,
        iconSize = iconSize,
        modifier = modifier,
    )
}

@Composable
private fun ContactActionIcon(
    method: ContactMethod,
    tint: Color,
    usePhoneIconForCallActions: Boolean = false,
    iconSize: Dp = DesignTokens.LargeIconSize,
    modifier: Modifier = Modifier,
) {
    if (usePhoneIconForCallActions &&
        (method is ContactMethod.Phone ||
            method is ContactMethod.VideoCall ||
            method is ContactMethod.WhatsAppCall ||
            method is ContactMethod.TelegramCall ||
            method is ContactMethod.SignalCall)
    ) {
        Icon(
            imageVector = Icons.Rounded.Call,
            contentDescription = null,
            tint = AppColors.CallIconTint,
            modifier = modifier.size(iconSize),
        )
        return
    }
    when (method) {
        is ContactMethod.Phone -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.Sms -> {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize * 0.9f),
            )
        }

        is ContactMethod.WhatsAppCall -> {
            AppVoiceCallIcon(
                logoPainterRes = R.drawable.whatsapp_call,
                size = iconSize,
            )
        }

        is ContactMethod.WhatsAppMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.WhatsAppVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp_video_call),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.TelegramMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.TelegramCall -> {
            AppVoiceCallIcon(
                logoPainterRes = R.drawable.telegram_call,
                size = iconSize,
            )
        }

        is ContactMethod.TelegramVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram_video_call),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.SignalVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.signal_video_call),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.SignalCall -> {
            AppVoiceCallIcon(
                logoPainterRes = R.drawable.signal_call,
                size = iconSize,
            )
        }

        is ContactMethod.SignalMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.signal),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.GoogleMeet -> {
            Icon(
                painter = painterResource(id = R.drawable.google_meet),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.Email -> {
            Icon(
                imageVector = Icons.Rounded.Email,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.VideoCall -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.CustomApp -> {
            val context = LocalContext.current
            val appIconBitmap =
                remember(method.packageName) {
                    method.packageName?.let { packageName ->
                        runCatching {
                            context.packageManager
                                .getApplicationIcon(packageName)
                                .toBitmap()
                                .asImageBitmap()
                        }.getOrNull()
                    }
                }
            if (appIconBitmap != null) {
                Image(
                    bitmap = appIconBitmap,
                    contentDescription = null,
                    modifier = modifier.size(iconSize),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = tint,
                    modifier = modifier.size(iconSize),
                )
            }
        }

        is ContactMethod.ViewInContactsApp -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize),
            )
        }
    }
}

@Composable
internal fun getActionButtonLabel(method: ContactMethod): String =
    when (method) {
        is ContactMethod.Phone -> {
            stringResource(R.string.contact_method_call_label)
        }

        is ContactMethod.Sms -> {
            stringResource(R.string.contact_method_message_label)
        }

        is ContactMethod.WhatsAppCall -> {
            stringResource(R.string.contacts_action_button_voice_call)
        }

        is ContactMethod.WhatsAppMessage -> {
            stringResource(R.string.contacts_action_button_chat)
        }

        is ContactMethod.WhatsAppVideoCall -> {
            stringResource(R.string.contacts_action_button_video_call)
        }

        is ContactMethod.TelegramMessage -> {
            stringResource(R.string.contacts_action_button_chat)
        }

        is ContactMethod.TelegramCall -> {
            stringResource(R.string.contacts_action_button_voice_call)
        }

        is ContactMethod.TelegramVideoCall -> {
            stringResource(R.string.contacts_action_button_video_call)
        }

        is ContactMethod.SignalMessage -> {
            stringResource(R.string.contacts_action_button_chat)
        }

        is ContactMethod.SignalCall -> {
            stringResource(R.string.contacts_action_button_voice_call)
        }

        is ContactMethod.SignalVideoCall -> {
            stringResource(R.string.contacts_action_button_video_call)
        }

        is ContactMethod.GoogleMeet -> {
            stringResource(R.string.contacts_action_button_meet)
        }

        is ContactMethod.Email -> {
            stringResource(R.string.contact_method_email_label)
        }

        is ContactMethod.VideoCall -> {
            stringResource(R.string.contacts_action_button_video_call)
        }

        is ContactMethod.CustomApp -> {
            method.displayLabel
        }

        is ContactMethod.ViewInContactsApp -> {
            stringResource(R.string.contacts_action_button_contacts)
        }
    }
