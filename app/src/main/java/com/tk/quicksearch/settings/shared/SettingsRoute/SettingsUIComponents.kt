package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

@Composable
fun SectionSettingsSection(
    sectionOrder: List<SearchSection>,
    disabledSections: Set<SearchSection>,
    onToggleSection: (SearchSection, Boolean) -> Unit,
    appsSubtitle: String? = null,
    onAppsClick: (() -> Unit)? = null,
    onAppsClickNoRipple: Boolean = false,
    appShortcutsSubtitle: String? = null,
    onAppShortcutsClick: (() -> Unit)? = null,
    onAppShortcutsClickNoRipple: Boolean = false,
    contactsSubtitle: String? = null,
    onContactsClick: (() -> Unit)? = null,
    onContactsClickNoRipple: Boolean = false,
    filesSubtitle: String? = null,
    onFilesClick: (() -> Unit)? = null,
    onFilesClickNoRipple: Boolean = false,
    deviceSettingsSubtitle: String? = null,
    onDeviceSettingsClick: (() -> Unit)? = null,
    onDeviceSettingsClickNoRipple: Boolean = false,
    calculatorEnabled: Boolean? = null,
    onCalculatorToggle: ((Boolean) -> Unit)? = null,
    calculatorSubtitle: String? = null,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_sections_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding),
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = DesignTokens.ExtraLargeCardShape) {
        Column(modifier = Modifier.fillMaxWidth()) {
            sectionOrder.forEachIndexed { index, section ->
                val isSectionEnabled = section !in disabledSections
                val isAppsRow = section == SearchSection.APPS
                val isAppShortcutsRow = section == SearchSection.APP_SHORTCUTS
                val isContactsRow = section == SearchSection.CONTACTS
                val isFilesRow = section == SearchSection.FILES
                val isDeviceSettingsRow = section == SearchSection.SETTINGS
                SectionRowWithoutDrag(
                    section = section,
                    isEnabled = isSectionEnabled,
                    onToggle = { enabled -> onToggleSection(section, enabled) },
                    subtitle =
                        when {
                            isAppsRow -> appsSubtitle
                            isAppShortcutsRow -> appShortcutsSubtitle
                            isContactsRow -> contactsSubtitle?.takeIf { isSectionEnabled }
                            isFilesRow -> filesSubtitle?.takeIf { isSectionEnabled }
                            isDeviceSettingsRow -> deviceSettingsSubtitle
                            else -> null
                        },
                    onRowClick =
                        when {
                            isAppsRow -> onAppsClick
                            isAppShortcutsRow -> onAppShortcutsClick
                            isContactsRow -> onContactsClick?.takeIf { isSectionEnabled }
                            isFilesRow -> onFilesClick?.takeIf { isSectionEnabled }
                            isDeviceSettingsRow -> onDeviceSettingsClick
                            else -> null
                        },
                    noRippleOnRowClick =
                        when {
                            isAppsRow -> onAppsClickNoRipple
                            isAppShortcutsRow -> onAppShortcutsClickNoRipple
                            isContactsRow -> onContactsClickNoRipple
                            isFilesRow -> onFilesClickNoRipple
                            isDeviceSettingsRow -> onDeviceSettingsClickNoRipple
                            else -> false
                        },
                )

                if (index != sectionOrder.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            if (calculatorEnabled != null && onCalculatorToggle != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ExtraToggleRow(
                    title = stringResource(R.string.calculator_toggle_title),
                    subtitle = calculatorSubtitle,
                    icon = Icons.Rounded.Calculate,
                    checked = calculatorEnabled,
                    onToggle = onCalculatorToggle,
                )
            }
        }
    }
}

@Composable
private fun ExtraToggleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    bottomPadding: Dp = DragConstants.rowVerticalPadding,
) {
    val view = LocalView.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = DragConstants.rowHorizontalPadding,
                    end = DragConstants.rowHorizontalPadding,
                    top = DragConstants.rowVerticalPadding,
                    bottom = bottomPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DragConstants.iconSize),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = { enabled ->
                hapticToggle(view)()
                onToggle(enabled)
            },
            modifier = Modifier.scale(0.85f),
        )
    }
}

@Composable
private fun SectionRowWithoutDrag(
    section: SearchSection,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    subtitle: String? = null,
    onRowClick: (() -> Unit)? = null,
    noRippleOnRowClick: Boolean = false,
    bottomPadding: Dp = DragConstants.rowVerticalPadding,
) {
    val view = LocalView.current
    val metadata = getSectionMetadata(section)
    val rowInteractionSource = remember { MutableInteractionSource() }
    val rowIndication = LocalIndication.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .let { rowModifier ->
                    if (onRowClick != null) {
                        rowModifier.clickable(
                            interactionSource = rowInteractionSource,
                            indication = if (noRippleOnRowClick) null else rowIndication,
                            onClick = onRowClick,
                        )
                    } else {
                        rowModifier
                    }
                }
                .padding(
                    start = DragConstants.rowHorizontalPadding,
                    end = DragConstants.rowHorizontalPadding,
                    top = DragConstants.rowVerticalPadding,
                    bottom = bottomPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing),
        ) {
            Icon(
                imageVector = metadata.icon,
                contentDescription = metadata.name,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DragConstants.iconSize),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (onRowClick != null) {
            Row(
                modifier = Modifier.offset(x = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.desc_navigate_forward),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                VerticalDivider(
                    modifier = Modifier.height(24.dp).padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        androidx.compose.material3.Switch(
            checked = isEnabled,
            onCheckedChange = { enabled ->
                hapticToggle(view)()
                onToggle(enabled)
            },
            modifier = Modifier.scale(0.85f),
        )
    }
}