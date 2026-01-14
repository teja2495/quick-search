package com.tk.quicksearch.search.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.ContactMethod
import com.tk.quicksearch.ui.theme.DesignTokens

// ============================================================================
// Contact Action Button (Messaging App Style)
// ============================================================================

@Composable
internal fun ContactActionButton(
    method: ContactMethod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = when (method) {
        is ContactMethod.Phone -> DesignTokens.ColorPhone
        is ContactMethod.Sms -> DesignTokens.ColorSms
        is ContactMethod.WhatsAppCall,
        is ContactMethod.WhatsAppMessage,
        is ContactMethod.WhatsAppVideoCall -> DesignTokens.ColorWhatsApp
        is ContactMethod.TelegramMessage,
        is ContactMethod.TelegramCall,
        is ContactMethod.TelegramVideoCall -> DesignTokens.ColorTelegram
        is ContactMethod.GoogleMeet -> Color.Unspecified
        is ContactMethod.Email -> DesignTokens.ColorEmail
        is ContactMethod.VideoCall -> DesignTokens.ColorVideoCall
        is ContactMethod.CustomApp -> DesignTokens.ColorCustom
        is ContactMethod.ViewInContactsApp -> DesignTokens.ColorView
    }

    Surface(
        modifier = modifier
            .width(90.dp)
            .clickable(onClick = onClick)
            .border(
                width = DesignTokens.BorderWidth,
                color = Color.White.copy(alpha = 0.3f),
                shape = DesignTokens.ShapeSmall
            ),
        shape = DesignTokens.ShapeSmall,
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ContactActionIcon(method = method, tint = iconColor)
            Text(
                text = getActionButtonLabel(method),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 0.9f
            )
        }
    }
}

@Composable
private fun ContactActionIcon(method: ContactMethod, tint: Color) {
    when (method) {
        is ContactMethod.Phone -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.Sms -> {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.WhatsAppCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.WhatsAppMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.WhatsAppVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.TelegramMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.TelegramCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.TelegramVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.GoogleMeet -> {
            Icon(
                painter = painterResource(id = R.drawable.google_meet),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.Email -> {
            Icon(
                imageVector = Icons.Rounded.Email,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.VideoCall -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.CustomApp -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
        is ContactMethod.ViewInContactsApp -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(DesignTokens.LargeIconSize)
            )
        }
    }
}

@Composable
private fun getActionButtonLabel(method: ContactMethod): String {
    return when (method) {
        is ContactMethod.Phone -> stringResource(R.string.contacts_action_button_call)
        is ContactMethod.Sms -> stringResource(R.string.contacts_action_button_message)
        is ContactMethod.WhatsAppCall -> stringResource(R.string.contacts_action_button_voice_call)
        is ContactMethod.WhatsAppMessage -> stringResource(R.string.contacts_action_button_chat)
        is ContactMethod.WhatsAppVideoCall -> stringResource(R.string.contacts_action_button_video_call)
        is ContactMethod.TelegramMessage -> stringResource(R.string.contacts_action_button_chat)
        is ContactMethod.TelegramCall -> stringResource(R.string.contacts_action_button_voice_call)
        is ContactMethod.TelegramVideoCall -> stringResource(R.string.contacts_action_button_video_call)
        is ContactMethod.GoogleMeet -> stringResource(R.string.contacts_action_button_meet)
        is ContactMethod.Email -> stringResource(R.string.contacts_action_button_email)
        is ContactMethod.VideoCall -> stringResource(R.string.contacts_action_button_video_call)
        is ContactMethod.CustomApp -> method.displayLabel
        is ContactMethod.ViewInContactsApp -> stringResource(R.string.contacts_action_button_contacts)
    }
}
