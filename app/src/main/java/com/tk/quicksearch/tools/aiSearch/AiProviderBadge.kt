package com.tk.quicksearch.tools.aiSearch

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.components.TonalIconBadgeContainer

/**
 * An AI provider's logo in a 40dp circular accent-tinted badge, matching `TonalIconBadge` so it
 * reads like other tile and list icons. Custom providers show a sparkle in the accent; Groq, whose
 * logo is a filled tile, fills the circle.
 */
@Composable
fun AiProviderBadge(providerId: AiSearchLlmProviderId) {
    val logoResId =
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> R.drawable.ic_gemini_sparkle_search_engine
            AiSearchLlmProviderId.OPENAI -> R.drawable.chatgpt
            AiSearchLlmProviderId.ANTHROPIC -> R.drawable.claude
            AiSearchLlmProviderId.META -> R.drawable.meta_logo
            AiSearchLlmProviderId.OPENCODE -> openCodeLogoRes(MaterialTheme.colorScheme.onSurface)
            else -> null
        }
    // The OpenAI mark is single-color white, so tint it to stay visible in light themes.
    val logoColorFilter =
        if (providerId == AiSearchLlmProviderId.OPENAI) {
            ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        } else {
            null
        }
    // The Groq mark is already a filled tile, so it becomes the avatar itself instead of sitting
    // inside a second shape.
    if (providerId == AiSearchLlmProviderId.GROQ) {
        Image(
            painter = painterResource(R.drawable.groq_logo),
            contentDescription = null,
            modifier = Modifier.size(AiProviderBadgeSize).clip(CircleShape),
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
    stringResource(
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> R.string.search_engine_gemini
            AiSearchLlmProviderId.OPENAI -> R.string.settings_ai_provider_openai
            AiSearchLlmProviderId.ANTHROPIC -> R.string.search_engine_claude
            AiSearchLlmProviderId.GROQ -> R.string.settings_ai_provider_groq
            AiSearchLlmProviderId.META -> R.string.settings_ai_provider_meta
            AiSearchLlmProviderId.OPENCODE -> R.string.settings_ai_provider_opencode
            else -> R.string.common_custom
        },
    )

private val AiProviderBadgeSize = 40.dp
private val AiProviderBadgeLogoSize = 22.dp

// Matches SettingsIconBadge.
