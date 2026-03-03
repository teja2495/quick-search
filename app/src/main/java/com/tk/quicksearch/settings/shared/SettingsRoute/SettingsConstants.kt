package com.tk.quicksearch.settings.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.Shortcut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.hapticToggle

/** Constants for drag and drop behavior and animations. */
object DragConstants {
    val rowHorizontalPadding: Dp = DesignTokens.CardHorizontalPadding
    val rowVerticalPadding: Dp = DesignTokens.CardVerticalPadding
    val iconSize: Dp = DesignTokens.IconSize
    val rowSpacing: Dp = DesignTokens.ItemRowSpacing
}

/** Data class holding section display metadata. */
internal data class SectionMetadata(
    val name: String,
    val icon: ImageVector,
)

/** Gets the display metadata for a given search section. */
@Composable
internal fun getSectionMetadata(section: SearchSection): SectionMetadata =
    when (section) {
        SearchSection.APPS -> {
            SectionMetadata(
                name = stringResource(R.string.section_apps),
                icon = Icons.Rounded.Apps,
            )
        }

        SearchSection.APP_SHORTCUTS -> {
            SectionMetadata(
                name = stringResource(R.string.section_app_shortcuts),
                icon = Icons.AutoMirrored.Rounded.Shortcut,
            )
        }

        SearchSection.CONTACTS -> {
            SectionMetadata(
                name = stringResource(R.string.section_contacts),
                icon = Icons.Rounded.Contacts,
            )
        }

        SearchSection.FILES -> {
            SectionMetadata(
                name = stringResource(R.string.section_files),
                icon = Icons.AutoMirrored.Rounded.InsertDriveFile,
            )
        }

        SearchSection.SETTINGS -> {
            SectionMetadata(
                name = stringResource(R.string.section_settings),
                icon = Icons.Rounded.Settings,
            )
        }
    }