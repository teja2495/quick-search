package com.tk.quicksearch.onboarding

import com.tk.quicksearch.settings.shared.SettingsSwitch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.settings.settingsDetailScreen.MessagingSection
import com.tk.quicksearch.settings.shared.SettingsCard
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
            currentStep = currentStep,
            totalSteps = totalSteps,
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
                        isWhatsAppInstalled = uiState.isWhatsAppInstalled,
                        isWhatsAppBusinessInstalled = uiState.isWhatsAppBusinessInstalled,
                        isTelegramInstalled = uiState.isTelegramInstalled,
                        isSignalInstalled = uiState.isSignalInstalled,
                        onMessagingAppSelected = @Suppress("LocalContextGetResourceValueCall") { app ->
                            val isInstalled =
                                when (app) {
                                    MessagingApp.MESSAGES -> {
                                        true
                                    }

                                    MessagingApp.WHATSAPP -> {
                                        uiState.isWhatsAppInstalled
                                    }

                                    MessagingApp.WHATSAPP_BUSINESS -> {
                                        uiState.isWhatsAppBusinessInstalled
                                    }

                                    MessagingApp.TELEGRAM -> {
                                        uiState.isTelegramInstalled
                                    }

                                    MessagingApp.SIGNAL -> {
                                        uiState.isSignalInstalled
                                    }
                                }

                            if (isInstalled) {
                                viewModel.setMessagingApp(app)
                            } else {
                                val appName =
                                    when (app) {
                                        MessagingApp.WHATSAPP -> {
                                            context.getString(
                                                R.string
                                                    .contact_method_whatsapp_message_label,
                                            )
                                        }

                                        MessagingApp.WHATSAPP_BUSINESS -> {
                                            context.getString(R.string.contact_method_whatsapp_business_label)
                                        }

                                        MessagingApp.TELEGRAM -> {
                                            context.getString(
                                                R.string
                                                    .contact_method_telegram_message_label,
                                            )
                                        }

                                        MessagingApp.SIGNAL -> {
                                            context.getString(
                                                R.string
                                                    .contact_method_signal_message_label,
                                            )
                                        }

                                        MessagingApp.MESSAGES -> {
                                            ""
                                        }
                                    }
                                onShowToast(
                                    context.getString(
                                        R.string
                                            .settings_messaging_app_not_installed,
                                        appName,
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
                    Text(
                        text =
                            stringResource(
                                R.string.settings_file_types_title,
                            ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = DesignTokens.SpacingMedium),
                    )

                    // Folders toggle
                    SettingsCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = DesignTokens.SpacingLarge,
                                        vertical = DesignTokens.SpacingMedium,
                                    ),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(DesignTokens.SpacingMedium),
                            ) {
                                Icon(
                                    imageVector =
                                        MaterialSymbols.Folder,
                                    contentDescription = null,
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,
                                    modifier =
                                        Modifier.size(DesignTokens.IconSize),
                                )
                                Text(
                                    text =
                                        stringResource(
                                            R.string
                                                .settings_folders_toggle,
                                        ),
                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodyLarge,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,
                                )
                            }
                            SettingsSwitch(
                                checked = uiState.showFolders,
                                onCheckedChange = { enabled ->
                                    viewModel.setShowFolders(
                                        enabled,
                                    )
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.SpacingMedium))

                    SettingsCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            val fileTypes = FileType.values()
                            val lastIndex = fileTypes.lastIndex

                            fileTypes.forEachIndexed { index, fileType ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal =
                                                    DesignTokens.SpacingLarge,
                                                vertical =
                                                    DesignTokens.SpacingMedium,
                                            ),
                                    horizontalArrangement =
                                        Arrangement
                                            .SpaceBetween,
                                    verticalAlignment =
                                        Alignment
                                            .CenterVertically,
                                ) {
                                    Row(
                                        modifier =
                                            Modifier.weight(
                                                1f,
                                            ),
                                        verticalAlignment =
                                            Alignment
                                                .CenterVertically,
                                        horizontalArrangement =
                                            Arrangement
                                                .spacedBy(
                                                    DesignTokens.SpacingMedium,
                                                ),
                                    ) {
                                        Icon(
                                            imageVector =
                                                getFileTypeIcon(
                                                    fileType,
                                                ),
                                            contentDescription =
                                            null,
                                            tint =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurfaceVariant,
                                            modifier =
                                                Modifier.size(
                                                    DesignTokens.IconSize,
                                                ),
                                        )
                                        Text(
                                            text =
                                                when (fileType) {
                                                    FileType.DOCUMENTS -> {
                                                        stringResource(
                                                            R.string
                                                                .file_type_documents,
                                                        )
                                                    }

                                                    FileType.PICTURES -> {
                                                        stringResource(
                                                            R.string
                                                                .file_type_pictures,
                                                        )
                                                    }

                                                    FileType.VIDEOS -> {
                                                        stringResource(
                                                            R.string
                                                                .file_type_videos,
                                                        )
                                                    }

                                                    FileType.AUDIO -> {
                                                        stringResource(
                                                            R.string
                                                                .file_type_audio,
                                                        )
                                                    }

                                                    FileType.APKS -> {
                                                        stringResource(
                                                            R.string
                                                                .file_type_apks,
                                                        )
                                                    }

                                                    FileType.OTHER -> {
                                                        stringResource(
                                                            R.string
                                                                .contact_method_fallback_label,
                                                        )
                                                    }
                                                },
                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodyLarge,
                                            color =
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface,
                                        )
                                    }
                                    SettingsSwitch(
                                        checked =
                                            fileType in
                                                uiState.enabledFileTypes,
                                        onCheckedChange = { enabled ->
                                            viewModel
                                                .setFileTypeEnabled(
                                                    fileType,
                                                    enabled,
                                                )
                                        },
                                    )
                                }

                                if (index < lastIndex) {
                                    HorizontalDivider(
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .outlineVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingCompactSpacing))
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

        Button(
            onClick = onContinue,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
            shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
            contentPadding =
                PaddingValues(
                    horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                    vertical = DesignTokens.OnboardingButtonVerticalPadding,
                ),
        ) {
            Text(
                text = stringResource(R.string.setup_action_start),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
    }
    } // end SettingsScreenBackground
}
