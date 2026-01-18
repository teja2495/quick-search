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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.SearchViewModel
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.settings.settingsScreen.MessagingSection
import com.tk.quicksearch.settings.settingsScreen.FileTypesSection


/**
 * Final setup screen shown after search engine setup when permissions are granted.
 * Allows users to configure Direct Dial, default messaging app, and file types.
 * Only displayed when at least one of contacts or files permissions is granted.
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
        modifier = modifier
            .fillMaxSize()
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
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Show Direct Dial and/or Messaging App sections when contacts permission is granted
            if (hasContactsPermission) {
                val showDirectDial = hasCallPermission
                val showMessagingApp = true // Always show when contacts permission is granted
                
                if (showDirectDial && showMessagingApp) {
                    MessagingSection(
                        messagingApp = uiState.messagingApp,
                        onSetMessagingApp = viewModel::setMessagingApp,
                        directDialEnabled = uiState.directDialEnabled,
                        onToggleDirectDial = viewModel::setDirectDialEnabled,
                        hasCallPermission = hasCallPermission,
                        contactsSectionEnabled = true,
                        isWhatsAppInstalled = uiState.isWhatsAppInstalled,
                        isTelegramInstalled = uiState.isTelegramInstalled,
                        onMessagingAppSelected = { app ->
                            val isInstalled = when (app) {
                                MessagingApp.MESSAGES -> true
                                MessagingApp.WHATSAPP -> uiState.isWhatsAppInstalled
                                MessagingApp.TELEGRAM -> uiState.isTelegramInstalled
                            }
                            
                            if (isInstalled) {
                                viewModel.setMessagingApp(app)
                            } else {
                                val appName = when (app) {
                                    MessagingApp.WHATSAPP -> context.getString(R.string.settings_messaging_option_whatsapp)
                                    MessagingApp.TELEGRAM -> context.getString(R.string.settings_messaging_option_telegram)
                                    MessagingApp.MESSAGES -> ""
                                }
                                onShowToast(context.getString(R.string.settings_messaging_app_not_installed, appName))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (showMessagingApp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.settings_messaging_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_messaging_card_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                
                                val messagingOptions = listOf(
                                    MessagingApp.MESSAGES to R.string.settings_messaging_option_messages,
                                    MessagingApp.WHATSAPP to R.string.settings_messaging_option_whatsapp,
                                    MessagingApp.TELEGRAM to R.string.settings_messaging_option_telegram
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectableGroup(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    messagingOptions.forEach { (app, labelRes) ->
                                        FilterChip(
                                            selected = uiState.messagingApp == app,
                                            onClick = {
                                                val isInstalled = when (app) {
                                                    MessagingApp.MESSAGES -> true
                                                    MessagingApp.WHATSAPP -> uiState.isWhatsAppInstalled
                                                    MessagingApp.TELEGRAM -> uiState.isTelegramInstalled
                                                }
                                                
                                                if (isInstalled) {
                                                    viewModel.setMessagingApp(app)
                                                } else {
                                                    val appName = when (app) {
                                                        MessagingApp.WHATSAPP -> context.getString(R.string.settings_messaging_option_whatsapp)
                                                        MessagingApp.TELEGRAM -> context.getString(R.string.settings_messaging_option_telegram)
                                                        MessagingApp.MESSAGES -> ""
                                                    }
                                                    onShowToast(context.getString(R.string.settings_messaging_app_not_installed, appName))
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(labelRes),
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Show file types section only if files permission is granted
            if (hasFilesPermission) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_file_types_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column {
                            val fileTypes = FileType.values()
                            val lastIndex = fileTypes.lastIndex
                            
                            fileTypes.forEachIndexed { index, fileType ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (fileType) {
                                            FileType.PHOTOS_AND_VIDEOS -> stringResource(R.string.file_type_photos_and_videos)
                                            FileType.DOCUMENTS -> stringResource(R.string.file_type_documents)
                                            FileType.OTHER -> stringResource(R.string.file_type_other)
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = fileType in uiState.enabledFileTypes,
                                        onCheckedChange = { enabled ->
                                            viewModel.setFileTypeEnabled(fileType, enabled)
                                        }
                                    )
                                }
                                
                                if (index < lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
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
