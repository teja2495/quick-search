package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.search.utils.PermissionUtils
import com.tk.quicksearch.onboarding.permissionScreen.PermissionRequestHandler

private val GrantedPermissionColor = Color(0xFF4CAF50)

/**
 * Data class representing a permission item.
 */
private data class PermissionItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isGranted: Boolean,
    val onRequest: () -> Unit
)

/**
 * Permissions settings screen with permission status and request options.
 * Permission status is checked actively using PermissionUtils.
 */
@Composable
fun PermissionsSettings(
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasUsagePermission = remember { PermissionUtils.hasUsageStatsPermission(context) }
    val hasContactPermission = remember { PermissionUtils.hasContactsPermission(context) }
    val hasFilePermission = remember { PermissionUtils.hasFileAccessPermission(context) }
    val hasCallPermission = remember { PermissionRequestHandler.checkCallPermission(context) }
    Column(modifier = modifier) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = DesignTokens.ExtraLargeCardShape
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
                    ),
                    PermissionItem(
                        name = stringResource(R.string.settings_call_permission_title),
                        icon = Icons.Rounded.Call,
                        isGranted = hasCallPermission,
                        onRequest = onRequestCallPermission
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
                    tint = GrantedPermissionColor,
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