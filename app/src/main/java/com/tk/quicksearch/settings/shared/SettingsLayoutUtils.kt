package com.tk.quicksearch.settings.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.isLandscape
import com.tk.quicksearch.shared.util.isTablet

private const val TABLET_SETTINGS_CONTENT_WIDTH_FRACTION = 0.70f
private const val TABLET_LANDSCAPE_SETTINGS_CONTENT_WIDTH_FRACTION = 0.50f

@Composable
fun Modifier.settingsContentWidth(): Modifier =
    if (isTablet()) {
        if (isLandscape()) {
            fillMaxWidth(TABLET_LANDSCAPE_SETTINGS_CONTENT_WIDTH_FRACTION)
        } else {
            fillMaxWidth(TABLET_SETTINGS_CONTENT_WIDTH_FRACTION)
        }
    } else {
        fillMaxWidth()
    }

private val SettingsScrollViewportShape = RoundedCornerShape(DesignTokens.SegmentedListOuterCorner)

/**
 * Clips a settings scroll viewport to the settings card corner radius so cards scrolling past
 * the top or bottom edge follow the same rounded outline. Apply after the horizontal content
 * padding and before any scroll modifier.
 */
fun Modifier.settingsScrollClip(): Modifier = clip(SettingsScrollViewportShape)
