package com.tk.quicksearch.shared.permissions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.AppShapes
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/** Shared permission checklist and Material 3 segmented list for onboarding and Settings. */
@Composable
internal fun PermissionsPageContent(
    isOnboarding: Boolean,
    modifier: Modifier = Modifier,
    showBackgroundUsage: Boolean = false,
    onRequestUsagePermission: () -> Unit = {},
    onRequestFilePermission: () -> Unit = {},
    onStatesChanged: (PermissionCardStates) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        PermissionsTrustPoints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DesignTokens.SpacingSmall, bottom = DesignTokens.SpacingXSmall),
        )

        val listModifier = Modifier.fillMaxWidth()
        PermissionsCardSection(
            texts =
                PermissionCardTexts(
                    usageTitle = stringResource(R.string.settings_shortcut_usage_access),
                    usageDescription = stringResource(R.string.permissions_usage_desc),
                    contactsTitle = stringResource(R.string.contacts_action_button_contacts),
                    contactsDescription = stringResource(R.string.permissions_contacts_desc),
                    filesTitle = stringResource(R.string.permissions_files_title),
                    filesDescription = stringResource(R.string.permissions_files_desc),
                    calendarTitle = stringResource(R.string.settings_calendar_permission_title),
                    calendarDescription = stringResource(R.string.permissions_calendar_desc),
                    callingTitle = stringResource(R.string.settings_call_permission_title),
                    callingDescription = stringResource(R.string.permissions_calling_desc),
                    notificationsTitle = stringResource(R.string.settings_notifications_permission_title),
                    notificationsDescription = stringResource(R.string.permissions_notifications_desc),
                    exactAlarmsTitle = stringResource(R.string.permissions_exact_alarms_title),
                    exactAlarmsDescription = stringResource(R.string.permissions_exact_alarms_desc),
                    notificationAccessTitle = stringResource(R.string.settings_shortcut_notification_listener),
                    notificationAccessDescription = stringResource(R.string.permissions_notification_access_desc),
                    accessibilityTitle = stringResource(R.string.permissions_accessibility_lock_screen_title),
                    accessibilityDescription = stringResource(R.string.permissions_accessibility_lock_screen_desc),
                    backgroundUsageTitle = stringResource(R.string.permissions_background_usage_title),
                    backgroundUsageDescription = stringResource(R.string.permissions_background_usage_desc),
                ),
            modifier =
                if (isOnboarding) {
                    listModifier
                        .weight(1f, fill = false)
                        .heightIn(max = DesignTokens.OnboardingPermissionCardViewportHeight)
                } else {
                    listModifier
                },
            internalScrollEnabled = isOnboarding,
            showCallingPermission = !isOnboarding,
            showNotificationsPermission = !isOnboarding,
            showAccessibilityPermission = !isOnboarding,
            showBackgroundUsage = showBackgroundUsage && !isOnboarding,
            segmented = true,
            showItemIcons = false,
            cardContainer = { cardModifier, content ->
                Column(modifier = cardModifier.clip(AppShapes.extraLarge)) { content() }
            },
            onRequestUsagePermission = onRequestUsagePermission,
            onRequestFilePermission = onRequestFilePermission,
            onStatesChanged = onStatesChanged,
        )
    }
}
