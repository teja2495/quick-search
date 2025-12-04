package com.tk.quicksearch.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var isBannerDismissed by remember { mutableStateOf(false) }
    
    if (!hasUsagePermission && !isBannerDismissed) {
        UsagePermissionCard(
            modifier = Modifier.fillMaxWidth(),
            onRequestPermission = onRequestPermission,
            onDismiss = { isBannerDismissed = true }
        )
    }

    errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
        InfoBanner(message = message)
    }
}
