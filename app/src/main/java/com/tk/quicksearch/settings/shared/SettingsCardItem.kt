package com.tk.quicksearch.settings.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols

/**
 * Data class for settings card items.
 */
data class SettingsCardItem(
    val title: String,
    val description: String,
    val isBeta: Boolean = false,
    val tagLabel: String? = null,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val iconBitmap: ImageBitmap? = null,
    val iconTint: Color? = null,
    val actionIcon: ImageVector = MaterialSymbols.ChevronRight,
    val isEnabled: Boolean = true,
    val actionOnPress: () -> Unit,
)
