package com.tk.quicksearch.settings.settingsScreen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shortcut
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.ui.theme.DesignTokens
import com.tk.quicksearch.util.hapticToggle
import sh.calvin.reorderable.ReorderableColumn

/**
 * Constants for drag and drop behavior and animations.
 */
private object DragConstants {
    val rowHorizontalPadding: Dp = DesignTokens.CardHorizontalPadding
    val rowVerticalPadding: Dp = DesignTokens.CardVerticalPadding
    val iconSize: Dp = DesignTokens.IconSize
    val rowSpacing: Dp = DesignTokens.ItemRowSpacing
}

/**
 * Data class holding section display metadata.
 */
private data class SectionMetadata(
    val name: String,
    val icon: ImageVector
)

/**
 * Gets the display metadata for a given search section.
 */
@Composable
private fun getSectionMetadata(section: SearchSection): SectionMetadata {
    return when (section) {
        SearchSection.APPS -> SectionMetadata(
            name = stringResource(R.string.section_apps),
            icon = Icons.Rounded.Apps
        )
        SearchSection.APP_SHORTCUTS -> SectionMetadata(
            name = stringResource(R.string.section_app_shortcuts),
            icon = Icons.Rounded.Shortcut
        )
        SearchSection.CONTACTS -> SectionMetadata(
            name = stringResource(R.string.section_contacts),
            icon = Icons.Rounded.Contacts
        )
        SearchSection.FILES -> SectionMetadata(
            name = stringResource(R.string.section_files),
            icon = Icons.Rounded.InsertDriveFile
        )
        SearchSection.SETTINGS -> SectionMetadata(
            name = stringResource(R.string.section_settings),
            icon = Icons.Rounded.Settings
        )
    }
}

@Composable
fun SectionSettingsSection(
    sectionOrder: List<SearchSection>,
    disabledSections: Set<SearchSection>,
    onToggleSection: (SearchSection, Boolean) -> Unit,
    onReorderSections: (List<SearchSection>) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    if (showTitle) {
        Text(
            text = stringResource(R.string.settings_sections_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier.padding(bottom = DesignTokens.SectionTitleBottomPadding)
        )
    }

    val view = LocalView.current

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = DesignTokens.ExtraLargeCardShape
    ) {
        ReorderableColumn(
            list = sectionOrder,
            onSettle = { fromIndex, toIndex ->
                if (fromIndex != toIndex) {
                    val newOrder = sectionOrder.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                    onReorderSections(newOrder)
                }
            },
            onMove = {
                ViewCompat.performHapticFeedback(
                    view,
                    HapticFeedbackConstantsCompat.SEGMENT_FREQUENT_TICK
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { index, section, isDragging ->
            key(section) {
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 4.dp else 0.dp,
                    label = "elevation"
                )

                Surface(
                    shadowElevation = elevation,
                    color = if (isDragging) MaterialTheme.colorScheme.surface else Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SectionRow(
                            section = section,
                            isEnabled = section !in disabledSections,
                            onToggle = { enabled -> onToggleSection(section, enabled) },
                            dragHandleModifier = Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    ViewCompat.performHapticFeedback(
                                        view,
                                        HapticFeedbackConstantsCompat.GESTURE_START
                                    )
                                },
                                onDragStopped = {
                                    ViewCompat.performHapticFeedback(
                                        view,
                                        HapticFeedbackConstantsCompat.GESTURE_END
                                    )
                                }
                            )
                        )

                        if (index != sectionOrder.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionRow(
    section: SearchSection,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    dragHandleModifier: Modifier,
    bottomPadding: Dp = DragConstants.rowVerticalPadding
) {
    val view = LocalView.current
    val metadata = getSectionMetadata(section)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = DragConstants.rowHorizontalPadding,
                end = DragConstants.rowHorizontalPadding,
                top = DragConstants.rowVerticalPadding,
                bottom = bottomPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DragConstants.rowSpacing)
    ) {
        Icon(
            imageVector = Icons.Rounded.DragHandle,
            contentDescription = stringResource(R.string.settings_action_reorder),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(DragConstants.iconSize)
                .then(dragHandleModifier)
        )

        Icon(
            imageVector = metadata.icon,
            contentDescription = metadata.name,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DragConstants.iconSize)
        )

        Text(
            text = metadata.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = isEnabled,
            onCheckedChange = { enabled ->
                hapticToggle(view)()
                onToggle(enabled)
            }
        )
    }
}
