package com.tk.quicksearch.tools.aiSearch

import com.tk.quicksearch.shared.ui.theme.DesignTokens
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.components.TonalIconBadgeContainer
import com.tk.quicksearch.shared.ui.theme.AppShapes

/**
 * An AI provider's logo in a 40dp circular accent-tinted badge, matching `TonalIconBadge` so it
 * reads like other tile and list icons. Custom providers show a sparkle in the accent; Groq, whose
 * logo is a filled tile, fills the circle.
 */
@Composable
fun AiProviderBadge(providerId: AiSearchLlmProviderId) {
    val branding = AiSearchLlmProviderRegistry.definition(providerId)?.branding
    val contentColor = MaterialTheme.colorScheme.onSurface
    val logoResId = branding?.let { if (contentColor.luminance() > 0.5f) it.iconOnDark else it.icon }
    val logoColorFilter = if (branding?.tintIcon == true) ColorFilter.tint(contentColor) else null
    if (branding?.fillsBadge == true && logoResId != null) {
        Image(
            painter = painterResource(logoResId),
            contentDescription = null,
            modifier = Modifier.size(AiProviderBadgeSize).clip(AppShapes.full),
        )
        return
    }
    TonalIconBadgeContainer(size = AiProviderBadgeSize) {
        if (logoResId != null) {
            Image(
                painter = painterResource(logoResId),
                contentDescription = null,
                colorFilter = logoColorFilter,
                modifier = Modifier.size(AiProviderBadgeLogoSize),
            )
        } else {
            Icon(
                imageVector = MaterialSymbols.AutoAwesome,
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(AiProviderBadgeLogoSize),
            )
        }
    }
}

/** Short provider name, such as the one shown under a selected model. */
@Composable
fun aiProviderDisplayName(providerId: AiSearchLlmProviderId): String =
    stringResource(AiSearchLlmProviderRegistry.definition(providerId)?.nameRes ?: R.string.common_custom)

private val AiProviderBadgeSize = 40.dp
private val AiProviderBadgeLogoSize = DesignTokens.IconSize

// Matches SettingsIconBadge.
