package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tk.quicksearch.shared.permissions.PermissionsPageContent

/**
 * Permissions settings screen with permission status and request options.
 */
@Composable
fun PermissionsSettings(
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PermissionsPageContent(
        isOnboarding = false,
        modifier = modifier,
        onRequestUsagePermission = onRequestUsagePermission,
        onRequestFilePermission = onRequestFilePermission,
    )
}
