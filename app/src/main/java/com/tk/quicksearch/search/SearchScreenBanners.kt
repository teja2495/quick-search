package com.tk.quicksearch.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders permission card and error banner if needed.
 */
@Composable
fun PermissionAndErrorBanners(
    hasUsagePermission: Boolean,
    errorMessage: String?,
    onRequestPermission: () -> Unit
) {
    if (!hasUsagePermission) {
        UsagePermissionCard(
            modifier = Modifier.fillMaxWidth(),
            onRequestPermission = onRequestPermission
        )
    }

    errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
        InfoBanner(message = message)
    }
}
