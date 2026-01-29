package com.tk.quicksearch.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.settings.settingsDetailScreen.MessagingSection
import com.tk.quicksearch.settings.shared.SettingsToggleRow

/** Gets the icon for a file type. */
private fun getFileTypeIcon(fileType: FileType): androidx.compose.ui.graphics.vector.ImageVector {
        return when (fileType) {
                FileType.DOCUMENTS -> Icons.Rounded.InsertDriveFile
                FileType.PICTURES -> Icons.Rounded.Image
                FileType.VIDEOS -> Icons.Rounded.VideoLibrary
                FileType.AUDIO -> Icons.Rounded.AudioFile
                FileType.APKS -> Icons.Rounded.Android
                FileType.OTHER -> Icons.Rounded.InsertDriveFile
        }
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
        hasCallPermission: Boolean,
        currentStep: Int,
        totalSteps: Int,
        onShowToast: (String) -> Unit = {},
        modifier: Modifier = Modifier
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current

        Column(
                modifier =
                        modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .safeDrawingPadding()
                                .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
        ) {
                OnboardingHeader(
                        title = stringResource(R.string.setup_final_title),
                        currentStep = currentStep,
                        totalSteps = totalSteps
                )

                Spacer(modifier = Modifier.height(16.dp))

                val scrollState = rememberScrollState()

                Column(
                        modifier = Modifier.weight(1f).verticalScroll(scrollState),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                        ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.extraLarge
                        ) {
                                SettingsToggleRow(
                                        title = stringResource(R.string.settings_overlay_mode_title),
                                        subtitle = stringResource(R.string.settings_overlay_mode_desc),
                                        checked = uiState.overlayModeEnabled,
                                        onCheckedChange = { viewModel.setOverlayModeEnabled(it) },
                                        leadingIcon = Icons.Rounded.Layers,
                                        isFirstItem = true,
                                        isLastItem = true
                                )
                        }

                        if (hasContactsPermission) {
                                Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        MessagingSection(
                                        messagingApp = uiState.messagingApp,
                                        onSetMessagingApp = viewModel::setMessagingApp,
                                        directDialEnabled = uiState.directDialEnabled,
                                        onToggleDirectDial = viewModel::setDirectDialEnabled,
                                        hasCallPermission = hasCallPermission,
                                        contactsSectionEnabled = true,
                                        isWhatsAppInstalled = uiState.isWhatsAppInstalled,
                                        isTelegramInstalled = uiState.isTelegramInstalled,
                                        showDirectDial = false,
                                        showTitle = false,
                                        onMessagingAppSelected = { app ->
                                                val isInstalled =
                                                        when (app) {
                                                                MessagingApp.MESSAGES -> true
                                                                MessagingApp.WHATSAPP ->
                                                                        uiState.isWhatsAppInstalled
                                                                MessagingApp.TELEGRAM ->
                                                                        uiState.isTelegramInstalled
                                                        }

                                                if (isInstalled) {
                                                        viewModel.setMessagingApp(app)
                                                } else {
                                                        val appName =
                                                                when (app) {
                                                                        MessagingApp.WHATSAPP ->
                                                                                context.getString(
                                                                                        R.string
                                                                                                .settings_messaging_option_whatsapp
                                                                                )
                                                                        MessagingApp.TELEGRAM ->
                                                                                context.getString(
                                                                                        R.string
                                                                                                .settings_messaging_option_telegram
                                                                                )
                                                                        MessagingApp.MESSAGES -> ""
                                                                }
                                                        onShowToast(
                                                                context.getString(
                                                                        R.string
                                                                                .settings_messaging_app_not_installed,
                                                                        appName
                                                                )
                                                        )
                                                }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                )
                                }
                        }

                        // Show file types section only if files permission is granted
                        if (hasFilesPermission) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                                text =
                                                        stringResource(
                                                                R.string.settings_file_types_title
                                                        ),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        // Folders toggle
                                        ElevatedCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.extraLarge
                                        ) {
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .padding(
                                                                                horizontal = 16.dp,
                                                                                vertical = 12.dp
                                                                        ),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Row(
                                                                modifier = Modifier.weight(1f),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                Icon(
                                                                        imageVector =
                                                                                Icons.Rounded
                                                                                        .Folder,
                                                                        contentDescription = null,
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant,
                                                                        modifier =
                                                                                Modifier.size(24.dp)
                                                                )
                                                                Text(
                                                                        text =
                                                                                stringResource(
                                                                                        R.string
                                                                                                .settings_folders_toggle
                                                                                ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyLarge,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurface
                                                                )
                                                        }
                                                        Switch(
                                                                checked = uiState.showFolders,
                                                                onCheckedChange = { enabled ->
                                                                        viewModel.setShowFolders(
                                                                                enabled
                                                                        )
                                                                }
                                                        )
                                                }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        ElevatedCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.extraLarge
                                        ) {
                                                Column {
                                                        val fileTypes = FileType.values()
                                                        val lastIndex = fileTypes.lastIndex

                                                        fileTypes.forEachIndexed { index, fileType
                                                                ->
                                                                Row(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                horizontal =
                                                                                                        16.dp,
                                                                                                vertical =
                                                                                                        12.dp
                                                                                        ),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .SpaceBetween,
                                                                        verticalAlignment =
                                                                                Alignment
                                                                                        .CenterVertically
                                                                ) {
                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.weight(
                                                                                                1f
                                                                                        ),
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically,
                                                                                horizontalArrangement =
                                                                                        Arrangement
                                                                                                .spacedBy(
                                                                                                        12.dp
                                                                                                )
                                                                        ) {
                                                                                Icon(
                                                                                        imageVector =
                                                                                                getFileTypeIcon(
                                                                                                        fileType
                                                                                                ),
                                                                                        contentDescription =
                                                                                                null,
                                                                                        tint =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        24.dp
                                                                                                )
                                                                                )
                                                                                Text(
                                                                                        text =
                                                                                                when (fileType
                                                                                                ) {
                                                                                                        FileType.DOCUMENTS ->
                                                                                                                stringResource(
                                                                                                                        R.string
                                                                                                                                .file_type_documents
                                                                                                                )
                                                                                                        FileType.PICTURES ->
                                                                                                                stringResource(
                                                                                                                        R.string
                                                                                                                                .file_type_pictures
                                                                                                                )
                                                                                                        FileType.VIDEOS ->
                                                                                                                stringResource(
                                                                                                                        R.string
                                                                                                                                .file_type_videos
                                                                                                                )
                                                                                                        FileType.AUDIO ->
                                                                                                                stringResource(
                                                                                                                        R.string
                                                                                                                                .file_type_audio
                                                                                                                )
                                                                                                        FileType.APKS ->
                                                                                                                stringResource(
                                                                                                                        R.string
                                                                                                                                .file_type_apks
                                                                                                                )
                                                                                                        FileType.OTHER ->
                                                                                                                stringResource(
                                                                                                                        R.string
                                                                                                                                .file_type_other
                                                                                                                )
                                                                                                },
                                                                                        style =
                                                                                                MaterialTheme
                                                                                                        .typography
                                                                                                        .bodyLarge,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurface
                                                                                )
                                                                        }
                                                                        Switch(
                                                                                checked =
                                                                                        fileType in
                                                                                                uiState.enabledFileTypes,
                                                                                onCheckedChange = {
                                                                                        enabled ->
                                                                                        viewModel
                                                                                                .setFileTypeEnabled(
                                                                                                        fileType,
                                                                                                        enabled
                                                                                                )
                                                                                }
                                                                        )
                                                                }

                                                                if (index < lastIndex) {
                                                                        HorizontalDivider(
                                                                                color =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .outlineVariant
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
                ) {
                        Text(
                                text = stringResource(R.string.setup_action_start),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                        )
                }

                Spacer(modifier = Modifier.height(32.dp))
        }
}
