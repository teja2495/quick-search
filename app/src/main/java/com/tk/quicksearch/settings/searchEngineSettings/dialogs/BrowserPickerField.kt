package com.tk.quicksearch.settings.searchEnginesScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.BrowserApp
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * A styled browser picker shown inside Add/Edit custom search engine dialogs.
 *
 * Renders a compact selector row that expands into a list of available browsers.
 * [selectedPackage] == null means "Default browser".
 */
@Composable
fun BrowserPickerField(
    availableBrowsers: List<BrowserApp>,
    selectedPackage: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
) {
    if (availableBrowsers.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val chevronAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = DesignTokens.AnimationDurationShort),
        label = "chevron",
    )

    val defaultLabel = stringResource(R.string.settings_search_engine_browser_default)
    val selectedLabel = availableBrowsers.firstOrNull { it.packageName == selectedPackage }?.label ?: defaultLabel

    val accentColor = AppColors.Accent
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(8.dp)
    val expandedShape = RoundedCornerShape(
        topStart = 8.dp, topEnd = 8.dp,
        bottomStart = 0.dp, bottomEnd = 0.dp,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Selector row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(if (expanded) expandedShape else shape)
                .border(
                    width = if (expanded) 1.dp else 0.5.dp,
                    color = if (expanded) accentColor else accentColor.copy(alpha = 0.3f),
                    shape = if (expanded) expandedShape else shape,
                )
                .clickable {
                    expanded = !expanded
                    if (expanded) onExpand()
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = if (expanded) accentColor else onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_search_engine_browser_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expanded) accentColor else onSurfaceVariant,
                )
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurface,
                )
            }
            Icon(
                imageVector = Icons.Rounded.UnfoldMore,
                contentDescription = null,
                tint = if (expanded) accentColor else onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronAngle),
            )
        }

        // ── Dropdown list ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(DesignTokens.AnimationDurationShort)) + fadeIn(tween(DesignTokens.AnimationDurationShort)),
            exit = shrinkVertically(tween(DesignTokens.AnimationDurationShort)) + fadeOut(tween(DesignTokens.AnimationDurationShort)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 0.dp, topEnd = 0.dp,
                            bottomStart = 8.dp, bottomEnd = 8.dp,
                        )
                    )
                    .background(accentColor.copy(alpha = 0.06f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(
                            topStart = 0.dp, topEnd = 0.dp,
                            bottomStart = 8.dp, bottomEnd = 8.dp,
                        ),
                    )
                    .verticalScroll(rememberScrollState()),
            ) {
                // Default browser option
                BrowserOptionRow(
                    label = defaultLabel,
                    isSelected = selectedPackage == null,
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )

                availableBrowsers.forEach { browser ->
                    HorizontalDivider(
                        color = accentColor.copy(alpha = 0.12f),
                        thickness = DesignTokens.DividerThickness,
                    )
                    BrowserOptionRow(
                        label = browser.label,
                        isSelected = browser.packageName == selectedPackage,
                        onClick = {
                            onSelect(browser.packageName)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val accentColor = AppColors.Accent
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) accentColor.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) accentColor else accentColor.copy(alpha = 0.25f)
                    ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) accentColor else onSurface,
            )
        }
    }
}
