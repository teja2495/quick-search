package com.tk.quicksearch.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R

/**
 * Data class representing a permission item.
 */
private data class PermissionItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isGranted: Boolean,
    val onRequest: () -> Unit
)

@Composable
fun PermissionsSection(
    hasUsagePermission: Boolean,
    hasContactPermission: Boolean,
    hasFilePermission: Boolean,
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_section_permissions),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column {
                val permissions = listOf(
                    PermissionItem(
                        name = stringResource(R.string.settings_usage_access_title),
                        icon = Icons.Rounded.Info,
                        isGranted = hasUsagePermission,
                        onRequest = onRequestUsagePermission
                    ),
                    PermissionItem(
                        name = stringResource(R.string.settings_contacts_permission_title),
                        icon = Icons.Rounded.Contacts,
                        isGranted = hasContactPermission,
                        onRequest = onRequestContactPermission
                    ),
                    PermissionItem(
                        name = stringResource(R.string.settings_files_permission_title),
                        icon = Icons.Rounded.InsertDriveFile,
                        isGranted = hasFilePermission,
                        onRequest = onRequestFilePermission
                    )
                )
                
                permissions.forEachIndexed { index, permission ->
                    PermissionRow(
                        permission = permission,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (index != permissions.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    permission: PermissionItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = permission.icon,
            contentDescription = permission.name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        
        Text(
            text = permission.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        
        Box(
            modifier = Modifier
                .widthIn(min = 80.dp)
                .heightIn(min = 40.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (permission.isGranted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.settings_usage_access_granted),
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                TextButton(onClick = permission.onRequest) {
                    Text(
                        text = stringResource(R.string.settings_permission_grant),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

