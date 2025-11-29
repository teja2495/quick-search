package com.tk.quicksearch.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

@Composable
fun MessagingSection(
    useWhatsAppForMessages: Boolean,
    onToggleUseWhatsAppForMessages: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_messaging_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            // Messages option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleUseWhatsAppForMessages(false) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_messaging_option_messages),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RadioButton(
                    selected = !useWhatsAppForMessages,
                    onClick = { onToggleUseWhatsAppForMessages(false) }
                )
            }
            
            // Divider
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // WhatsApp option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleUseWhatsAppForMessages(true) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.settings_messaging_option_whatsapp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                RadioButton(
                    selected = useWhatsAppForMessages,
                    onClick = { onToggleUseWhatsAppForMessages(true) }
                )
            }
        }
    }
}

