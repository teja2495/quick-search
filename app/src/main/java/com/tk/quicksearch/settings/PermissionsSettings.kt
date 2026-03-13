package com.tk.quicksearch.settings.settingsDetailScreen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.permissions.PermissionCardTexts
import com.tk.quicksearch.shared.permissions.PermissionsCardSection
import com.tk.quicksearch.settings.shared.*
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Permissions settings screen with permission status and request options.
 */
@Composable
fun PermissionsSettings(
    onRequestUsagePermission: () -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestFilePermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onRequestCallPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.permissions_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignTokens.SpacingLarge),
        )

        PermissionsCardSection(
            texts =
                PermissionCardTexts(
                    usageTitle = stringResource(R.string.settings_usage_access_title),
                    usageDescription = stringResource(R.string.permissions_usage_desc),
                    contactsTitle = stringResource(R.string.settings_contacts_permission_title),
                    contactsDescription = stringResource(R.string.permissions_contacts_desc),
                    filesTitle = stringResource(R.string.settings_files_permission_title),
                    filesDescription = stringResource(R.string.permissions_files_desc),
                    calendarTitle = stringResource(R.string.settings_calendar_permission_title),
                    calendarDescription = stringResource(R.string.permissions_calendar_desc),
                    callingTitle = stringResource(R.string.settings_call_permission_title),
                    callingDescription = stringResource(R.string.permissions_calling_desc),
                ),
            modifier = Modifier.fillMaxWidth(),
            onRequestUsagePermission = onRequestUsagePermission,
            onRequestContactPermission = onRequestContactPermission,
            onRequestFilePermission = onRequestFilePermission,
            onRequestCalendarPermission = onRequestCalendarPermission,
            onRequestCallPermission = onRequestCallPermission,
            cardContainer = { cardModifier, content ->
                ElevatedCard(
                    modifier = cardModifier,
                    shape = DesignTokens.ExtraLargeCardShape,
                ) {
                    content()
                }
            },
        )
    }
}
