package com.tk.quicksearch.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.ui.theme.DesignTokens

/**
 * Constants for consistent spacing throughout the settings screen.
 * @deprecated Use DesignTokens instead for new components.
 */
object SettingsSpacing {
    val headerHorizontalPadding = DesignTokens.ContentHorizontalPadding
    val headerVerticalPadding = DesignTokens.HeaderVerticalPadding
    val headerIconSpacing = DesignTokens.HeaderIconSpacing
    val contentHorizontalPadding = DesignTokens.ContentHorizontalPadding
    val sectionTopPadding = DesignTokens.SectionTopPadding
    val sectionTitleBottomPadding = DesignTokens.SectionTitleBottomPadding
    val sectionDescriptionBottomPadding = DesignTokens.SectionDescriptionBottomPadding
    val versionTopPadding = DesignTokens.VersionTopPadding
    val versionBottomPadding = DesignTokens.VersionBottomPadding
    val singleCardPadding = DesignTokens.singleCardPadding()
}

/**
 * Retrieves the app version name from the package manager.
 */
@Composable
fun getAppVersionName(): String? {
    val context = LocalContext.current
    return try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        null
    }
}