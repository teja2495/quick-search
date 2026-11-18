package com.tk.quicksearch.onboarding

import com.tk.quicksearch.search.contacts.models.ContactButtonAction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.settingsDetailScreen.MessagingSection
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedToggleTile
import com.tk.quicksearch.settings.shared.SettingsScreenBackground

/** Gets the icon for a file type. */
private fun getFileTypeIcon(fileType: FileType): androidx.compose.ui.graphics.vector.ImageVector =
    when (fileType) {
        FileType.DOCUMENTS -> MaterialSymbols.InsertDriveFile
        FileType.PICTURES -> MaterialSymbols.Image
        FileType.VIDEOS -> MaterialSymbols.VideoLibrary
        FileType.AUDIO -> MaterialSymbols.AudioFile
        FileType.APKS -> MaterialSymbols.Android
        FileType.OTHER -> MaterialSymbols.InsertDriveFile
    }

/**
 * Final setup screen shown after search engine setup. Allows users to configure Direct Dial,
 * default messaging app, and file types.
 */
@Composable
fun FinalSetupScreen(
    onContinue: () -> Unit,
    viewModel: SearchViewModel,
    hasContactsPermission: Boolean,
    hasFilesPermission: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onShowToast: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsScreenBackground(
        appTheme = AppTheme.MONOCHROME,
        overlayThemeIntensity = 0.5f,
        modifier = modifier,
    ) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = DesignTokens.OnboardingHorizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        OnboardingHeader(
            title = stringResource(R.string.setup_final_title),
            topPadding = DesignTokens.SpacingMedium,
            modifier = Modifier.welcomeExitItem(),
        )

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingCompactSpacing))

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier.weight(1f).verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXXLarge),
        ) {
            if (hasContactsPermission) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    MessagingSection(
                        messagingApp = uiState.secondaryContactButton.toMessagingApp(),
                        installedContactProviders = uiState.installedContactProviders,
                        onMessagingAppSelected = @Suppress("LocalContextGetResourceValueCall") { app ->
                            val action = ContactButtonAction.fromMessagingApp(app)
                            if (action.isAppInstalled(uiState.installedContactProviders)) {
                                viewModel.setMessagingApp(app)
                            } else {
                                onShowToast(
                                    context.getString(
                                        R.string.settings_messaging_app_not_installed,
                                        context.getString(action.messagingLabelRes),
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Show file types section only if files permission is granted
            if (hasFilesPermission) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SegmentedGroup(title = stringResource(R.string.settings_file_types_title)) {
                        SegmentedToggleTile(
                            title = stringResource(R.string.settings_folders_toggle),
                            icon = MaterialSymbols.Folder,
                            checked = uiState.showFolders,
                            onCheckedChange = { enabled -> viewModel.setShowFolders(enabled) },
                            index = 0,
                            count = 1,
                        )
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMedium))

                    SegmentedGroup {
                        val fileTypes = FileType.entries
                        fileTypes.forEachIndexed { index, fileType ->
                            SegmentedToggleTile(
                                title =
                                    stringResource(
                                        when (fileType) {
                                            FileType.DOCUMENTS -> R.string.file_type_documents
                                            FileType.PICTURES -> R.string.file_type_pictures
                                            FileType.VIDEOS -> R.string.file_type_videos
                                            FileType.AUDIO -> R.string.file_type_audio
                                            FileType.APKS -> R.string.file_type_apks
                                            FileType.OTHER -> R.string.contact_method_fallback_label
                                        },
                                    ),
                                icon = getFileTypeIcon(fileType),
                                checked = fileType in uiState.enabledFileTypes,
                                onCheckedChange = { enabled -> viewModel.setFileTypeEnabled(fileType, enabled) },
                                index = index,
                                count = fileTypes.size,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingCompactSpacing))
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

        OnboardingBottomActionButton(
            text = stringResource(R.string.setup_action_start),
            onClick = onContinue,
            finishesSetup = true,
        )

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
    }
    } // end SettingsScreenBackground
}
