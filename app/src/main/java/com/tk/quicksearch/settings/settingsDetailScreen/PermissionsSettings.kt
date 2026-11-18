package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tk.quicksearch.shared.permissions.PermissionsPageContent
import com.tk.quicksearch.shared.util.isDefaultDigitalAssistant
import com.tk.quicksearch.shared.util.isDefaultHomeApp

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
    val context = LocalContext.current
    val showBackgroundUsage =
        !context.isDefaultHomeApp() && !context.isDefaultDigitalAssistant()

    PermissionsPageContent(
        isOnboarding = false,
        modifier = modifier,
        showBackgroundUsage = showBackgroundUsage,
        onRequestUsagePermission = onRequestUsagePermission,
        onRequestFilePermission = onRequestFilePermission,
    )
}
