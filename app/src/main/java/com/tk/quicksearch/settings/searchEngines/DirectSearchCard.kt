package com.tk.quicksearch.settings.searchengines

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.searchEngines.SearchEngineSettingsSpacing
import kotlinx.coroutines.delay

/**
 * Toggle card for direct search options.
 */
@Composable
fun SearchEngineToggleCard(
    directSearchEnabled: Boolean,
    onSetGeminiApiKey: (String?) -> Unit,
    geminiApiKeyLast4: String?,
    personalContext: String,
    onSetPersonalContext: ((String?) -> Unit)?,
    isExpanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null
) {
    var showInput by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var showPersonalContextDialog by remember { mutableStateOf(false) }
    var personalContextInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = personalContext,
                selection = TextRange(personalContext.length)
            )
        )
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val personalContextFocusRequester = remember { FocusRequester() }
    val geminiGuideUrl = "https://medium.com/@tejakarlapudi.apps/setting-up-gemini-api-key-in-quick-search-25ee92aa4311"

    LaunchedEffect(personalContext) {
        personalContextInput = TextFieldValue(
            text = personalContext,
            selection = TextRange(personalContext.length)
        )
    }
    LaunchedEffect(showPersonalContextDialog) {
        if (showPersonalContextDialog) {
            // Slight delay so the field is laid out before requesting focus
            delay(100)
            personalContextInput = personalContextInput.copy(
                selection = TextRange(personalContextInput.text.length)
            )
            personalContextFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = SearchEngineSettingsSpacing.cardHorizontalPadding,
                vertical = SearchEngineSettingsSpacing.cardTopPadding
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onToggleExpanded != null) {
                            Modifier.clickable(onClick = onToggleExpanded)
                        } else {
                            Modifier
                        }
                    )
                    .padding(bottom = if (isExpanded) 12.dp else 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.direct_search),
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.settings_direct_search_toggle),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (onToggleExpanded != null) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (!isExpanded) return@Column

            if (directSearchEnabled && geminiApiKeyLast4 != null) {
                Text(
                    text = "Your Gemini API key:  ****$geminiApiKeyLast4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SearchEngineSettingsSpacing.apiKeyButtonBottomPadding),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onSetPersonalContext != null) {
                        TextButton(
                            onClick = {
                                personalContextInput = TextFieldValue(
                                    text = personalContext,
                                    selection = TextRange(personalContext.length)
                                )
                                showPersonalContextDialog = true
                            }
                        ) {
                            Text(text = stringResource(R.string.settings_direct_search_personal_context))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        onClick = {
                            apiKeyInput = ""
                            showInput = false
                            onSetGeminiApiKey(null)
                        }
                    ) {
                        Text(text = stringResource(R.string.settings_gemini_api_key_reset))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.settings_direct_search_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                if (showInput) {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        placeholder = { Text(text = stringResource(R.string.settings_gemini_api_key_placeholder)) },
                        singleLine = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ContentPaste,
                                contentDescription = stringResource(R.string.settings_gemini_api_key_paste),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        clipboardManager.getText()
                                            ?.text
                                            ?.trim()
                                            ?.takeIf { it.isNotEmpty() }
                                            ?.let { pasted ->
                                                apiKeyInput = pasted
                                            }
                                    },
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SearchEngineSettingsSpacing.apiKeyButtonBottomPadding),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                apiKeyInput = ""
                                showInput = false
                            }
                        ) {
                            Text(text = stringResource(R.string.dialog_cancel))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = apiKeyInput.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSetGeminiApiKey(trimmed)
                                    apiKeyInput = ""
                                    showInput = false
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.dialog_save))
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SearchEngineSettingsSpacing.apiKeyButtonBottomPadding),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(geminiGuideUrl)
                                )
                                runCatching { context.startActivity(intent) }
                            }
                        ) {
                            Text(text = stringResource(R.string.settings_direct_search_how_to))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { showInput = true }
                        ) {
                            Text(text = stringResource(R.string.settings_gemini_api_key_add))
                        }
                    }
                }
            }

            if (showPersonalContextDialog && onSetPersonalContext != null) {
                AlertDialog(
                    onDismissRequest = { showPersonalContextDialog = false },
                    title = {
                        Text(text = stringResource(R.string.settings_direct_search_personal_context_title))
                    },
                    text = {
                        OutlinedTextField(
                            value = personalContextInput,
                            onValueChange = { personalContextInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp)
                                .focusRequester(personalContextFocusRequester),
                            placeholder = {
                                Text(text = stringResource(R.string.settings_direct_search_personal_context_hint))
                            },
                            shape = MaterialTheme.shapes.large,
                            singleLine = false,
                            minLines = 5
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val trimmed = personalContextInput.text.trim()
                                onSetPersonalContext(trimmed.takeIf { it.isNotEmpty() })
                                showPersonalContextDialog = false
                            }
                        ) {
                            Text(text = stringResource(R.string.dialog_save))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showPersonalContextDialog = false }
                        ) {
                            Text(text = stringResource(R.string.dialog_cancel))
                        }
                    }
                )
            }
        }
    }
}
