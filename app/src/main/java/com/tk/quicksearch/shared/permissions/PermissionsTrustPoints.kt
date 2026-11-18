package com.tk.quicksearch.shared.permissions

import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private const val SOURCE_CODE_URL = "https://github.com/teja2495/quick-search"

/**
 * Compact checklist of the three promises shown at the top of both permissions pages:
 * every permission is optional, data stays on the device, and the code is public. The last
 * row carries a View code pill and opens the project's source code.
 */
@Composable
fun PermissionsTrustPoints(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val accent = trustAccentColor()
    val openSource = {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_CODE_URL)))
        }
        Unit
    }

    val sourceDescription = stringResource(R.string.settings_feedback_github_desc)

    Column(
        modifier = modifier,
    ) {
        TrustPointLine(text = AnnotatedString(stringResource(R.string.permissions_trust_optional)), accent = accent)
        TrustPointLine(text = AnnotatedString(stringResource(R.string.permissions_trust_on_device)), accent = accent)
        TrustPointLine(
            text = AnnotatedString(stringResource(R.string.permissions_trust_open_source)),
            accent = accent,
            onClick = openSource,
            modifier = Modifier.semantics { contentDescription = sourceDescription },
            trailing = { ViewCodePill() },
        )
    }
}

@Composable
private fun TrustPointLine(
    text: AnnotatedString,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(AppShapes.small)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .heightIn(min = if (onClick != null) DesignTokens.MinTouchTarget else DesignTokens.Spacing28),
    ) {
        Row(
            // Keep all three checklist lines on the same rhythm. The link target extends below the
            // last line into the section gap.
            modifier = Modifier.fillMaxWidth().heightIn(min = DesignTokens.Spacing28),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
        ) {
            Icon(
                imageVector = MaterialSymbols.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(DesignTokens.IconSizeSmall),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
            trailing?.invoke()
        }
    }
}

@Composable
private fun ViewCodePill() {
    Row(
        modifier =
            Modifier
                .border(
                    width = DesignTokens.BorderWidth,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = AppShapes.full,
                ).padding(horizontal = DesignTokens.SpacingMedium, vertical = DesignTokens.SpacingXSmall / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall),
    ) {
        Text(
            text = stringResource(R.string.permissions_trust_view_code),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = MaterialSymbols.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DesignTokens.SpacingMedium + DesignTokens.SpacingXSmall / 2),
        )
    }
}

@Composable
internal fun trustAccentColor(): Color {
    return AppColors.TrustAccent
}
