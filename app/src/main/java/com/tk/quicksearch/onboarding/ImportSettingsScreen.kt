package com.tk.quicksearch.onboarding

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.settings.settingsScreen.SettingsBackupManager
import com.tk.quicksearch.settings.shared.SettingsScreenBackground
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var screenState by remember { mutableStateOf(ImportScreenState.Idle) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        screenState = ImportScreenState.Importing
        coroutineScope.launch(Dispatchers.IO) {
            val isSuccess = runCatching {
                SettingsBackupManager.importFromUri(context, uri)
            }.isSuccess
            withContext(Dispatchers.Main) {
                screenState = if (isSuccess) ImportScreenState.Success else ImportScreenState.Error
            }
        }
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
                currentStep = currentStep,
                totalSteps = totalSteps,
            )

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingCompactSpacing))

            // Main content — centred in remaining space
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.SpacingXLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Icon circle
                val iconAlpha by animateFloatAsState(
                    targetValue = if (screenState == ImportScreenState.Importing) 0.38f else 1f,
                    animationSpec = tween(DesignTokens.AnimationDurationMedium),
                    label = "iconAlpha",
                )
                Surface(
                    shape = CircleShape,
                    color = when (screenState) {
                        ImportScreenState.Success -> MaterialTheme.colorScheme.primaryContainer
                        ImportScreenState.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> AppColors.Accent.copy(alpha = 0.12f)
                    },
                    modifier = Modifier
                        .size(88.dp)
                        .alpha(iconAlpha),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = screenState,
                            transitionSpec = {
                                fadeIn(tween(DesignTokens.AnimationDurationShort)) togetherWith
                                    fadeOut(tween(DesignTokens.AnimationDurationShort))
                            },
                            label = "iconContent",
                        ) { state ->
                            Icon(
                                imageVector = when (state) {
                                    ImportScreenState.Success -> Icons.Rounded.CheckCircle
                                    ImportScreenState.Error -> Icons.Rounded.ErrorOutline
                                    else -> Icons.Rounded.FileDownload
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
                        fadeIn(tween(DesignTokens.AnimationDurationMedium)) togetherWith
                            fadeOut(tween(DesignTokens.AnimationDurationShort))
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
                                ImportScreenState.Idle -> stringResource(R.string.setup_import_description_title)
                                ImportScreenState.Importing -> stringResource(R.string.setup_import_importing)
                                ImportScreenState.Success -> stringResource(R.string.setup_import_success_title)
                                ImportScreenState.Error -> stringResource(R.string.setup_import_error_title)
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        val subtitleText = when (state) {
                            ImportScreenState.Idle -> stringResource(R.string.setup_import_description_subtitle)
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
                AnimatedVisibility(
                    visible = screenState == ImportScreenState.Idle || screenState == ImportScreenState.Error,
                    enter = fadeIn(tween(DesignTokens.AnimationDurationShort)),
                    exit = fadeOut(tween(DesignTokens.AnimationDurationShort)),
                ) {
                    Button(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Accent,
                            contentColor = AppColors.OnAccent,
                        ),
                        contentPadding = PaddingValues(
                            horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                            vertical = DesignTokens.OnboardingButtonVerticalPadding,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(DesignTokens.IconSize),
                        )
                        Spacer(modifier = Modifier.width(DesignTokens.SpacingSmall))
                        Text(
                            text = stringResource(R.string.setup_import_button),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Progress indicator — visible only while importing
                AnimatedVisibility(
                    visible = screenState == ImportScreenState.Importing,
                    enter = fadeIn(tween(DesignTokens.AnimationDurationShort)),
                    exit = fadeOut(tween(DesignTokens.AnimationDurationShort)),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))

            // Bottom CTA: outlined "Skip" normally, primary "Start Searching!" after successful import
            if (screenState == ImportScreenState.Success) {
                Button(
                    onClick = onImportSuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
                    shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                        vertical = DesignTokens.OnboardingButtonVerticalPadding,
                    ),
                    enabled = screenState != ImportScreenState.Importing,
                ) {
                    Text(
                        text = stringResource(R.string.setup_action_start),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.OnboardingButtonOuterHorizontalPadding),
                    shape = RoundedCornerShape(DesignTokens.OnboardingButtonCornerRadius),
                    contentPadding = PaddingValues(
                        horizontal = DesignTokens.OnboardingButtonHorizontalPadding,
                        vertical = DesignTokens.OnboardingButtonVerticalPadding,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    enabled = screenState != ImportScreenState.Importing,
                ) {
                    Text(
                        text = stringResource(R.string.setup_import_skip),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.OnboardingSectionSpacing))
        }
    }
}
