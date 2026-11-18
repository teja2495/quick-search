package com.tk.quicksearch.shared.permissions

import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

internal const val SOURCE_CODE_URL = "https://github.com/teja2495/quick-search"

/**
 * Reassurance card shown under the permissions subtitle, linking to the public source code
 * so users can verify how each permission is used.
 */
@Composable
fun OpenSourceTrustCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val accent = AppColors.TrustAccent
    val shape = AppShapes.largeIncreased

    val openSource = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE_URL)))
        }
        Unit
    }

    Row(
        modifier =
            modifier
                .clip(shape)
                .background(accent.copy(alpha = if (isDark) 0.12f else 0.1f))
                .clickable(role = Role.Button, onClick = openSource)
                .padding(horizontal = DesignTokens.SpacingLarge, vertical = DesignTokens.SpacingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
    ) {
        Icon(
            imageVector = MaterialSymbols.VerifiedUser,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(DesignTokens.IconSize),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.permissions_open_source_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val description = stringResource(R.string.permissions_open_source_desc)
            val action = stringResource(R.string.permissions_open_source_action)
            Text(
                text =
                    buildAnnotatedString {
                        append(description)
                        append(" ")
                        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Medium)) {
                            append(action)
                        }
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            imageVector = MaterialSymbols.OpenInNew,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(DesignTokens.IconSizeSmall),
        )
    }
}
