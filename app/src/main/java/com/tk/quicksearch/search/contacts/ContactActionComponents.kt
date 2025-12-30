package com.tk.quicksearch.search.contacts

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactMethod

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
        is ContactMethod.Phone -> Color(0xFF4CAF50)
        is ContactMethod.Sms -> Color(0xFF2196F3)
        is ContactMethod.WhatsAppCall,
        is ContactMethod.WhatsAppMessage,
        is ContactMethod.WhatsAppVideoCall -> Color(0xFF25D366)
        is ContactMethod.TelegramMessage,
        is ContactMethod.TelegramCall,
        is ContactMethod.TelegramVideoCall -> Color(0xFF0088CC)
        is ContactMethod.GoogleMeet -> Color.Unspecified
        is ContactMethod.Email -> Color(0xFFFF9800)
        is ContactMethod.VideoCall -> Color(0xFF9C27B0)
        is ContactMethod.CustomApp -> Color(0xFF607D8B)
        is ContactMethod.ViewInContactsApp -> Color(0xFF9E9E9E)
    }

    Surface(
        modifier = modifier
            .width(90.dp)
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
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
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.Sms -> {
            Icon(
                imageVector = Icons.Rounded.Sms,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.WhatsAppCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.WhatsAppMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.WhatsAppVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.TelegramMessage -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.TelegramCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.TelegramVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.GoogleMeet -> {
            Icon(
                painter = painterResource(id = R.drawable.google_meet),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.Email -> {
            Icon(
                imageVector = Icons.Rounded.Email,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.VideoCall -> {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.CustomApp -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
        is ContactMethod.ViewInContactsApp -> {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun getActionButtonLabel(method: ContactMethod): String {
    return when (method) {
        is ContactMethod.Phone -> "Call"
        is ContactMethod.Sms -> "Message"
        is ContactMethod.WhatsAppCall -> "Voice Call"
        is ContactMethod.WhatsAppMessage -> "Chat"
        is ContactMethod.WhatsAppVideoCall -> "Video Call"
        is ContactMethod.TelegramMessage -> "Chat"
        is ContactMethod.TelegramCall -> "Voice Call"
        is ContactMethod.TelegramVideoCall -> "Video Call"
        is ContactMethod.GoogleMeet -> "Meet"
        is ContactMethod.Email -> "Email"
        is ContactMethod.VideoCall -> "Video Call"
        is ContactMethod.CustomApp -> method.displayLabel
        is ContactMethod.ViewInContactsApp -> "Contacts"
    }
}
