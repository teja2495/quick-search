package com.tk.quicksearch.search.searchScreen.shared

import com.tk.quicksearch.shared.ui.theme.AppColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import com.tk.quicksearch.shared.ui.components.AppIconButton

/** Tonal step of the footer band over the card container (works on frosted and overlay cards). */
private val FooterMinHeight = 48.dp
private val FooterActionIconSize = 20.dp
private const val CopyConfirmationMillis = 1500L

/**
 * Material 3 card action area for [InformationCard]: a full-bleed tonal band on the card's bottom
 * edge with the result's source (tool name or AI provider) at the start and icon actions at the end.
 * Flat (compact) cards have no container to tint, so the band drops its fill there.
 */
@Composable
fun InformationCardFooter(
    modifier: Modifier = Modifier,
    source: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val bandColor =
        if (LocalSearchCardLayoutEnabled.current) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = AppColors.TintFaintAlpha)
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(bandColor)
                .heightIn(min = FooterMinHeight)
                .padding(InformationCardFooterContentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
            content = source,
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Source text aligns with the card body's [DesignTokens.SpacingLarge] inset; the end inset is
 * smaller because icon buttons carry their own touch-target padding.
 */
private val InformationCardFooterContentPadding =
    PaddingValues(start = DesignTokens.SpacingLarge, end = DesignTokens.SpacingXSmall)

/** Standard icon button for [InformationCardFooter], labelled by a plain tooltip on long-press. */
@Composable
fun InformationCardFooterAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    AppIconButton(
        onClick = onClick,
        tooltip = label,
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(FooterActionIconSize),
        )
    }
}

/** Copy action that swaps to a check mark briefly after copying [text]. */
@Composable
fun InformationCardCopyAction(text: String) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(CopyConfirmationMillis)
            copied = false
        }
    }
    InformationCardFooterAction(
        icon = if (copied) MaterialSymbols.Check else MaterialSymbols.ContentCopyOutlined,
        label = stringResource(R.string.calculator_copy_result),
        onClick = {
            clipboardManager.setText(AnnotatedString(text))
            copied = true
        },
    )
}
