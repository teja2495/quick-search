package com.tk.quicksearch.settings.searchEnginesScreen

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

/**
 * Setup card for direct search configuration.
 */
@Composable
fun DirectSearchSetupCard(
    directSearchEnabled: Boolean,
    onSetGeminiApiKey: (String?) -> Unit,
    geminiApiKeyLast4: String?,
    isSavingGeminiApiKey: Boolean = false,
    isExpanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null,
) {
    var apiKeyInput by remember { mutableStateOf("") }
    val hasConfiguredApiKey = directSearchEnabled && geminiApiKeyLast4 != null
    val buttonRowBottomPadding = DesignTokens.SpacingXSmall
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val geminiGuideUrl = stringResource(R.string.settings_gemini_guide_url)
    val trimmedKey = apiKeyInput.trim()
    val hasKeyToSave = trimmedKey.isNotEmpty()
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        SettingsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier.padding(
                    horizontal = DesignTokens.CardHorizontalPadding,
                    vertical = DesignTokens.CardTopPadding,
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (onToggleExpanded != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onToggleExpanded,
                                )
                            } else {
                                Modifier
                            },
                        ).padding(bottom = if (isExpanded) 12.dp else 0.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.direct_search),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_direct_search_toggle),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (onToggleExpanded != null) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) stringResource(R.string.desc_collapse) else stringResource(R.string.desc_expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    if (!hasConfiguredApiKey) {
                        Text(
                            text = stringResource(R.string.settings_direct_search_desc_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.settings_direct_search_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 20.dp),
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                placeholder = {
                                    Text(
                                        text = if (apiKeyInput.isEmpty()) "" else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                singleLine = true,
                                readOnly = true,
                            )

                            if (apiKeyInput.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.settings_gemini_api_key_paste_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.LinkColor,
                                    modifier =
                                        Modifier
                                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
                                            .clickable {
                                                clipboardManager
                                                    .getText()
                                                    ?.text
                                                    ?.trim()
                                                    ?.takeIf { it.isNotEmpty() }
                                                    ?.let { pasted ->
                                                        apiKeyInput = pasted
                                                    }
                                            },
                                )
                            }

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (apiKeyInput.isEmpty()) {
                                                        clipboardManager
                                                            .getText()
                                                            ?.text
                                                            ?.trim()
                                                            ?.takeIf { it.isNotEmpty() }
                                                            ?.let { pasted ->
                                                                apiKeyInput = pasted
                                                            }
                                                    } else {
                                                        apiKeyInput = ""
                                                    }
                                                },
                                                onLongPress = {
                                                    if (apiKeyInput.isEmpty()) {
                                                        clipboardManager
                                                            .getText()
                                                            ?.text
                                                            ?.trim()
                                                            ?.takeIf { it.isNotEmpty() }
                                                            ?.let { pasted ->
                                                                apiKeyInput = pasted
                                                            }
                                                    } else {
                                                        apiKeyInput = ""
                                                    }
                                                },
                                            )
                                        },
                            )
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = buttonRowBottomPadding),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (hasKeyToSave) {
                                TextButton(
                                    enabled = !isSavingGeminiApiKey,
                                    onClick = { apiKeyInput = "" },
                                ) {
                                    Text(text = stringResource(R.string.settings_gemini_api_key_clear))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    enabled = !isSavingGeminiApiKey,
                                    onClick = { onSetGeminiApiKey(trimmedKey) },
                                ) {
                                    Text(
                                        text =
                                            if (isSavingGeminiApiKey) {
                                                stringResource(R.string.settings_gemini_api_key_saving)
                                            } else {
                                                stringResource(R.string.dialog_save)
                                            },
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        val intent =
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(geminiGuideUrl),
                                            )
                                        runCatching { context.startActivity(intent) }
                                    },
                                ) {
                                    Text(text = stringResource(R.string.settings_direct_search_how_to))
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
