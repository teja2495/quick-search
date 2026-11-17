package com.tk.quicksearch.search.contacts.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.search.models.ContactMethodMimeTypes
import com.tk.quicksearch.shared.ui.components.ItemMenuTile
import com.tk.quicksearch.shared.ui.components.ItemMenuTileButton
import com.tk.quicksearch.shared.ui.components.AppVoiceCallIcon
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

// ============================================================================
// Contact Action Button (Messaging App Style)
// ============================================================================

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

            is ContactMethod.Email -> callIconTint

            is ContactMethod.VideoCall -> callIconTint

            is ContactMethod.CustomApp -> AppColors.ActionCustom

            is ContactMethod.ViewInContactsApp -> AppColors.ActionView
        }
    ItemMenuTileButton(
        tile = ItemMenuTile(
            label = getActionButtonLabel(method),
            icon = {
                ContactMethodIcon(
                    method = method,
                    usePhoneIconForCallActions = usePhoneIconForCallActions,
                    tintOverride = iconColor,
                    iconSize = DesignTokens.IconSize,
                )
            },
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        modifier = modifier,
    )
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
                is ContactMethod.Email -> callIconTint
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
            imageVector = MaterialSymbols.Call,
            contentDescription = null,
            tint = AppColors.CallIconTint,
            modifier = modifier.size(iconSize),
        )
        return
    }
    when (method) {
        is ContactMethod.Phone -> {
            Icon(
                imageVector = MaterialSymbols.Call,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.Sms -> {
            Icon(
                imageVector = MaterialSymbols.Sms,
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
                imageVector = MaterialSymbols.Email,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.VideoCall -> {
            Icon(
                imageVector = MaterialSymbols.Call,
                contentDescription = null,
                tint = tint,
                modifier = modifier.size(iconSize),
            )
        }

        is ContactMethod.CustomApp -> {
            if (method.isWhatsAppBusinessMethod()) {
                when (method.mimeType) {
                    ContactMethodMimeTypes.WHATSAPP_BUSINESS_VOICE_CALL ->
                        AppVoiceCallIcon(
                            logoPainterRes = R.drawable.whatsapp_call,
                            size = iconSize,
                        )
                    ContactMethodMimeTypes.WHATSAPP_BUSINESS_VIDEO_CALL ->
                        Icon(
                            painter = painterResource(id = R.drawable.whatsapp_video_call),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = modifier.size(iconSize),
                        )
                    else ->
                        Icon(
                            painter = painterResource(id = R.drawable.whatsapp),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = modifier.size(iconSize),
                        )
                }
                return
            }
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
                    imageVector = MaterialSymbols.Person,
                    contentDescription = null,
                    tint = tint,
                    modifier = modifier.size(iconSize),
                )
            }
        }

        is ContactMethod.ViewInContactsApp -> {
            Icon(
                imageVector = MaterialSymbols.Person,
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
            when (method.mimeType) {
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VOICE_CALL ->
                    stringResource(R.string.contact_method_call_label)
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VIDEO_CALL ->
                    stringResource(R.string.contacts_action_button_video_call)
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_MESSAGE ->
                    stringResource(R.string.contact_method_message_label)
                else -> method.displayLabel
            }
        }

        is ContactMethod.ViewInContactsApp -> {
            stringResource(R.string.contacts_action_button_contacts)
        }
    }

private fun ContactMethod.CustomApp.isWhatsAppBusinessMethod(): Boolean =
    packageName == "com.whatsapp.w4b" &&
        mimeType in
            setOf(
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VOICE_CALL,
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_MESSAGE,
                ContactMethodMimeTypes.WHATSAPP_BUSINESS_VIDEO_CALL,
            )
