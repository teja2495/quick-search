package com.tk.quicksearch.search.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.model.ContactMethod

// ============================================================================
// Contact Methods List
// ============================================================================

@Composable
private fun ContactMethodsList(
    contactMethods: List<ContactMethod>,
    onMethodClick: (ContactMethod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp, end = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        contactMethods.forEach { method ->
            ContactMethodItem(
                method = method,
                onClick = { onMethodClick(method) }
            )
        }
    }
}

@Composable
private fun ContactMethodItem(
    method: ContactMethod,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on method type
        ContactMethodIcon(method = method)

        // Method label and data (show subtext only for email)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = method.displayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Show subtext only for Email methods
            if (method is ContactMethod.Email) {
                Text(
                    text = method.data,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactMethodIcon(method: ContactMethod) {
    val (icon, tint) = when (method) {
        is ContactMethod.Phone -> Pair(Icons.Rounded.Call, Color.White)
        is ContactMethod.Sms -> Pair(Icons.Rounded.Sms, Color.White)
        is ContactMethod.WhatsAppCall,
        is ContactMethod.WhatsAppMessage,
        is ContactMethod.WhatsAppVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.whatsapp),
                contentDescription = null,
                tint = Color(0xFF25D366),
                modifier = Modifier.size(24.dp)
            )
            return
        }
        is ContactMethod.TelegramMessage,
        is ContactMethod.TelegramCall,
        is ContactMethod.TelegramVideoCall -> {
            Icon(
                painter = painterResource(id = R.drawable.telegram),
                contentDescription = null,
                tint = Color(0xFF0088CC),
                modifier = Modifier.size(24.dp)
            )
            return
        }
        is ContactMethod.GoogleMeet -> {
            Icon(
                painter = painterResource(id = R.drawable.google_meet),
                contentDescription = null,
                tint = Color.Unspecified, // Use original colors from the drawable
                modifier = Modifier.size(24.dp)
            )
            return
        }
        is ContactMethod.Email -> Pair(Icons.Rounded.Email, Color.White)
        is ContactMethod.VideoCall -> Pair(Icons.Rounded.Call, Color.White)
        is ContactMethod.CustomApp -> Pair(Icons.Rounded.Person, Color.White)
        is ContactMethod.ViewInContactsApp -> Pair(Icons.Rounded.Person, Color.White)
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp)
    )
}
