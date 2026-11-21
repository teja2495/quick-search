package com.tk.quicksearch.onboarding

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tk.quicksearch.shared.ui.theme.AppMotion
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import com.tk.quicksearch.shared.ui.components.AppLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.theme.AppShapes

private enum class ImportScreenState { Idle, Importing, Success, Error }

/**
 * Onboarding screen that offers users the option to restore settings from a backup file.
 * On successful import, the bottom button changes to "Start Searching!" and skips remaining
 * onboarding screens. On skip, the standard onboarding flow continues.
 */
@Composable
fun ImportSettingsScreen(
    currentStep: Int,
    totalSteps: Int,
    onImportSuccess: () -> Unit,
    onSettingsImported: () -> Unit = {},
    onSkip: () -> Unit,
    preFoundFiles: List<File> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf(ImportScreenState.Idle) }
    val singlePreFoundFile = preFoundFiles.singleOrNull()

    fun importFromUri(uri: Uri) {
        screenState = ImportScreenState.Importing
        coroutineScope.launch(Dispatchers.IO) {
            val isSuccess = runCatching {
                SettingsBackupManager.importFromUri(context, uri)
            }.isSuccess
            withContext(Dispatchers.Main) {
                if (isSuccess) {
                    onSettingsImported()
                }
                screenState = if (isSuccess) ImportScreenState.Success else ImportScreenState.Error
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        importFromUri(uri)
    }

    SettingsScreenBackground(
        appTheme = AppTheme.MONOCHROME,
        overlayThemeIntensity = 0.5f,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = DesignTokens.OnboardingHorizontalPadding),
            horizontalAlignment = Alignment.Start,
        ) {
            OnboardingHeader(
                title = stringResource(R.string.setup_import_title),
                topPadding = DesignTokens.SpacingMedium,
                modifier = Modifier.welcomeExitItem(),
            )

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingCompactSpacing))

            // Main content: one expressive tile, centred in the remaining space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                SegmentedTile(index = 0, count = 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = DesignTokens.SpacingXXLarge,
                                vertical = DesignTokens.OnboardingSectionSpacing,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Icon circle
                        val iconAlpha by animateFloatAsState(
                            targetValue = if (screenState == ImportScreenState.Importing) 0.38f else 1f,
                            animationSpec = tween(AppMotion.DurationMedium, easing = AppMotion.EmphasizedEasing),
                            label = "iconAlpha",
                        )
                        Surface(
                            shape = AppShapes.full,
                            color = when (screenState) {
                                ImportScreenState.Success -> MaterialTheme.colorScheme.primaryContainer
                                ImportScreenState.Error -> MaterialTheme.colorScheme.errorContainer
                                else -> AppColors.AccentTintLight
                            },
                            modifier = Modifier
                                .size(88.dp)
                                .alpha(iconAlpha),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = screenState,
                                    transitionSpec = {
                                        fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)) togetherWith
                                            fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing))
                                    },
                                    label = "iconContent",
                                ) { state ->
                                    Icon(
                                        imageVector = when (state) {
                                            ImportScreenState.Success -> MaterialSymbols.CheckCircle
                                            ImportScreenState.Error -> MaterialSymbols.ErrorOutline
                                            else -> MaterialSymbols.FileDownload
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(DesignTokens.IconSizeXLarge),
                                        tint = when (state) {
                                            ImportScreenState.Success -> MaterialTheme.colorScheme.onPrimaryContainer
                                            ImportScreenState.Error -> MaterialTheme.colorScheme.onErrorContainer
                                            else -> AppColors.Accent
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.SpacingXXLarge))

                        // Title + description text
                        AnimatedContent(
                            targetState = screenState,
                            transitionSpec = {
                                fadeIn(tween(AppMotion.DurationMedium, easing = AppMotion.EmphasizedDecelerateEasing)) togetherWith
                                    fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing))
                            },
                            label = "textContent",
                        ) { state ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = when (state) {
                                        ImportScreenState.Idle -> if (singlePreFoundFile != null) {
                                            stringResource(R.string.setup_import_backup_found_title)
                                        } else {
                                            stringResource(R.string.setup_import_description_title)
                                        }
                                        ImportScreenState.Importing -> stringResource(R.string.setup_import_importing)
                                        ImportScreenState.Success -> stringResource(R.string.setup_import_success_title)
                                        ImportScreenState.Error -> stringResource(R.string.setup_import_error_title)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.semantics { heading() },
                                )
                                val subtitleText = when (state) {
                                    ImportScreenState.Idle -> if (singlePreFoundFile != null) {
                                        stringResource(R.string.setup_import_backup_found_subtitle)
                                    } else {
                                        stringResource(R.string.setup_import_description_subtitle)
                                    }
                                    ImportScreenState.Importing -> ""
                                    ImportScreenState.Success -> stringResource(R.string.setup_import_success_subtitle)
                                    ImportScreenState.Error -> stringResource(R.string.setup_import_error_subtitle)
                                }
                                if (subtitleText.isNotEmpty()) {
                                    Text(
                                        text = subtitleText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

                        // Import / Retry button — hidden while importing or after success
                        // In single-file mode, "Restore" button imports the pre-found file directly.
                        // Otherwise, "Import Settings" opens the system file picker.
                        AnimatedVisibility(
                            visible = screenState == ImportScreenState.Idle || screenState == ImportScreenState.Error,
                            enter = fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)),
                            exit = fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing)),
                        ) {
                            OnboardingActionButton(
                                text = stringResource(
                                    if (singlePreFoundFile != null) {
                                        R.string.setup_import_restore_button
                                    } else {
                                        R.string.setup_import_button
                                    },
                                ),
                                icon = MaterialSymbols.FileDownload,
                                onClick = {
                                    if (singlePreFoundFile != null) {
                                        importFromUri(Uri.fromFile(singlePreFoundFile))
                                    } else {
                                        importLauncher.launch(arrayOf("*/*"))
                                    }
                                },
                            )
                        }

                        // Progress indicator — visible only while importing
                        AnimatedVisibility(
                            visible = screenState == ImportScreenState.Importing,
                            enter = fadeIn(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedDecelerateEasing)),
                            exit = fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing)),
                        ) {
                            AppLoadingIndicator(
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

            // Bottom CTA: outlined "Skip" normally, primary "Start Searching!" after successful import
            val isImported = screenState == ImportScreenState.Success
            AnimatedContent(
                targetState = isImported,
                transitionSpec = {
                    fadeIn(tween(AppMotion.DurationMedium, easing = AppMotion.EmphasizedDecelerateEasing)) togetherWith
                        fadeOut(tween(AppMotion.DurationShort, easing = AppMotion.EmphasizedAccelerateEasing))
                },
                label = "bottomAction",
            ) { imported ->
                OnboardingBottomActionButton(
                    text = stringResource(
                        if (imported) R.string.setup_action_start else R.string.setup_import_skip,
                    ),
                    onClick = if (imported) onImportSuccess else onSkip,
                    outlined = !imported,
                    enabled = screenState != ImportScreenState.Importing,
                    finishesSetup = imported,
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
        }
    }
}
