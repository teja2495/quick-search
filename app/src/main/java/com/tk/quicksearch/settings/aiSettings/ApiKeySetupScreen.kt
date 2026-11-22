package com.tk.quicksearch.settings.aiSettings

import androidx.compose.ui.semantics.Role
import com.tk.quicksearch.shared.ui.components.AppMessages
import com.tk.quicksearch.shared.ui.components.AppIconButton
import com.tk.quicksearch.shared.ui.components.AppOutlinedTextField
import com.tk.quicksearch.shared.ui.theme.AppShapes
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.shared.ui.components.AppButtonTone
import com.tk.quicksearch.shared.ui.components.ChoiceOption
import com.tk.quicksearch.shared.ui.components.ConnectedChoiceGroup
import com.tk.quicksearch.shared.ui.components.AnimatedSegmentedTiles
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContent
import com.tk.quicksearch.shared.ui.components.SegmentedGroup
import com.tk.quicksearch.shared.ui.components.SegmentedTileRow
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.shared.ui.components.TonalButton
import com.tk.quicksearch.shared.ui.components.TipBanner
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.readText
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.TavilyWebSearchMode
import com.tk.quicksearch.settings.searchEngineSettings.dialogs.ExpandScrollDelayMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tk.quicksearch.shared.ui.components.TonalIconBadge

/**
 * API key setup as Material 3 tiles: one group with a tile per provider (built-in, then custom)
 * ending in Add Custom Provider, then a Tavily group for web search.
 */
@Composable
fun ApiKeySetupScreen(
    apiKeyLast4ByProvider: Map<AiSearchLlmProviderId, String>,
    customProviderBaseUrlByProvider: Map<AiSearchLlmProviderId, String>,
    isSavingApiKey: Boolean,
    onSetApiKey: (AiSearchLlmProviderId, String?) -> Unit,
    onAddCustomProvider: (baseUrl: String, apiKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val freeApiKeyGuideUrl = stringResource(R.string.settings_gemini_guide_url)
    val openFreeApiKeyGuide: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(freeApiKeyGuideUrl)),
            )
        }
    }
    // Includes custom providers restored from a backup without their API key.
    val providers = (AiSearchLlmProviderId.entries + customProviderBaseUrlByProvider.keys).distinct()
    val tileCount = providers.size + 1

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
    ) {
        Text(
            text = stringResource(R.string.settings_api_key_setup_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DesignTokens.SpacingLarge),
        )

        if (apiKeyLast4ByProvider.isEmpty()) {
            TipBanner(
                annotatedText =
                    buildAnnotatedString {
                        pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                        append(stringResource(R.string.settings_direct_search_how_to))
                        pop()
                        append("\n")
                        append(stringResource(R.string.settings_free_api_key_tip_description))
                    },
                onContentClick = openFreeApiKeyGuide,
                actionIcon = MaterialSymbols.OpenInNew,
                actionContentDescription = stringResource(R.string.settings_open_free_api_key_guide),
                onActionClick = openFreeApiKeyGuide,
                showDismissButton = false,
            )
        }

        SegmentedGroup {
            providers.forEachIndexed { index, providerId ->
                ProviderApiKeyTile(
                    providerId = providerId,
                    apiKeyLast4 = apiKeyLast4ByProvider[providerId],
                    configuredBaseUrl = customProviderBaseUrlByProvider[providerId],
                    isSavingApiKey = isSavingApiKey,
                    onSetApiKey = onSetApiKey,
                    index = index,
                    count = tileCount,
                )
            }
            AddCustomProviderTile(
                isSaving = isSavingApiKey,
                onSave = onAddCustomProvider,
                index = providers.size,
                count = tileCount,
            )
        }

        TavilyWebSearchSection()
    }
}

@Composable
private fun TavilyWebSearchSection() {
    val context = LocalContext.current
    val preferences = remember(context) { UserAppPreferences(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    var apiKeyLast4 by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(TavilyWebSearchMode.DEFAULT) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(preferences) {
        val (key, savedMode) =
            withContext(Dispatchers.IO) {
                preferences.getTavilyApiKey() to preferences.getTavilyWebSearchMode()
            }
        apiKeyLast4 = key?.trim()?.takeLast(4)
        mode = savedMode
    }

    fun saveApiKey(key: String?) {
        coroutineScope.launch {
            isSaving = true
            withContext(Dispatchers.IO) { preferences.setTavilyApiKey(key) }
            apiKeyLast4 = key?.trim()?.takeIf { it.isNotEmpty() }?.takeLast(4)
            isSaving = false
        }
    }

    val hasSavedKey = apiKeyLast4 != null
    SegmentedGroup {
        AnimatedSegmentedTiles {
            item { index, count ->
                ApiKeyTile(
                    title = stringResource(R.string.settings_tavily_title),
                    description = stringResource(R.string.settings_tavily_desc),
                    leading = { TonalIconBadge(icon = MaterialSymbols.Public) },
                    savedLast4 = apiKeyLast4,
                    isSaving = isSaving,
                    onSave = ::saveApiKey,
                    onDelete = { saveApiKey(null) },
                    index = index,
                    count = count,
                )
            }
            tile(visible = hasSavedKey) {
                SegmentedTileContent {
                    ConnectedChoiceGroup(
                        options =
                            TavilyWebSearchMode.entries.map { option ->
                                ChoiceOption(option, stringResource(option.labelResId()))
                            },
                        selected = mode,
                        onSelect = { selected ->
                            if (selected != mode) {
                                mode = selected
                                coroutineScope.launch(Dispatchers.IO) {
                                    preferences.setTavilyWebSearchMode(selected)
                                }
                                AppMessages.show(context, selected.descriptionResId())
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun TavilyWebSearchMode.labelResId(): Int =
    when (this) {
        TavilyWebSearchMode.ALWAYS -> R.string.settings_tavily_mode_always
        TavilyWebSearchMode.WHEN_MODEL_UNSUPPORTED -> R.string.settings_tavily_mode_when_unsupported
    }

private fun TavilyWebSearchMode.descriptionResId(): Int =
    when (this) {
        TavilyWebSearchMode.ALWAYS -> R.string.settings_tavily_mode_always_desc
        TavilyWebSearchMode.WHEN_MODEL_UNSUPPORTED -> R.string.settings_tavily_mode_when_unsupported_desc
    }

/**
 * The last provider tile: tapping its header expands fields for an OpenAI-compatible base URL and
 * a pasted API key.
 */
@Composable
private fun AddCustomProviderTile(
    isSaving: Boolean,
    onSave: (baseUrl: String, apiKey: String) -> Unit,
    index: Int,
    count: Int,
) {
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    var baseUrlInput by rememberSaveable { mutableStateOf("") }
    var apiKeyInput by rememberSaveable { mutableStateOf("") }
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "customProviderChevronRotation",
    )
    val canSave = baseUrlInput.trim().isNotBlank() && apiKeyInput.trim().isNotBlank()

    fun pasteKeyFromClipboard() {
        clipboardScope.launch {
            apiKeyInput = clipboard.readText()?.trim().orEmpty()
        }
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(ExpandScrollDelayMillis)
            bringIntoViewRequester.bringIntoView()
        }
    }

    SegmentedTile(index = index, count = count, modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SegmentedTileRow(
                title = stringResource(R.string.settings_add_custom_provider_title),
                description = stringResource(R.string.settings_add_custom_provider_desc),
                leading = { TonalIconBadge(icon = MaterialSymbols.Add) },
                trailing = {
                    Icon(
                        imageVector = MaterialSymbols.ExpandMore,
                        contentDescription =
                            stringResource(
                                if (isExpanded) R.string.desc_collapse else R.string.desc_expand,
                            ),
                        modifier = Modifier.rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable(role = Role.Button) { isExpanded = !isExpanded },
            )
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                // The header row's padding already sits above, so only the sides and bottom pad here.
                Column(
                    modifier =
                        Modifier.fillMaxWidth().padding(
                            start = SegmentedTileContentPadding,
                            end = SegmentedTileContentPadding,
                            bottom = SegmentedTileContentPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingMedium),
                ) {
                    Text(
                        text = stringResource(R.string.settings_custom_provider_base_url_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AppOutlinedTextField(
                        value = baseUrlInput,
                        onValueChange = { baseUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.settings_custom_provider_base_url_input_label),
                        singleLine = true,
                        shape = AppShapes.large,
                    )
                    if (apiKeyInput.isEmpty()) {
                        TonalButton(
                            text = stringResource(R.string.settings_gemini_api_key_paste_hint),
                            icon = MaterialSymbols.Key,
                            tone = AppButtonTone.Custom(AppColors.LinkColor),
                            enabled = !isSaving,
                            onClick = ::pasteKeyFromClipboard,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        AppOutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(imageVector = MaterialSymbols.Key, contentDescription = null) },
                            trailingIcon = {
                                AppIconButton(onClick = { apiKeyInput = "" }, tooltip = stringResource(R.string.common_action_clear)) {
                                    Icon(
                                        imageVector = MaterialSymbols.Close,
                                        contentDescription = stringResource(R.string.common_action_clear),
                                    )
                                }
                            },
                            shape = AppShapes.large,
                            label = null,
                        )
                    }
                    ApiKeySaveActions(
                        isSaving = isSaving,
                        saveEnabled = canSave,
                        clearEnabled = baseUrlInput.isNotBlank() || apiKeyInput.isNotBlank(),
                        onClear = {
                            baseUrlInput = ""
                            apiKeyInput = ""
                        },
                        onSave = { onSave(baseUrlInput.trim(), apiKeyInput.trim()) },
                    )
                }
            }
        }
    }
}
