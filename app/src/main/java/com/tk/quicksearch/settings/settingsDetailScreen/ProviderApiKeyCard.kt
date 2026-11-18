package com.tk.quicksearch.settings.settingsDetailScreen

import com.tk.quicksearch.shared.ui.components.AppIconButton
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.SegmentedTile
import com.tk.quicksearch.shared.ui.components.SegmentedTileContentPadding
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.shared.util.readText
import com.tk.quicksearch.tools.aiSearch.AiProviderBadge
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.aiProviderDisplayName
import kotlinx.coroutines.launch
import com.tk.quicksearch.shared.ui.components.AppButton
import com.tk.quicksearch.shared.ui.components.AppTextButton

/**
 * One AI provider's API key as a tile at [index] of [count]: the provider badge and name, and for
 * a custom provider its base URL. See [ApiKeyTile] for the key states.
 */
@Composable
internal fun ProviderApiKeyTile(
    providerId: AiSearchLlmProviderId,
    apiKeyLast4: String?,
    configuredBaseUrl: String?,
    isSavingApiKey: Boolean,
    onSetApiKey: (AiSearchLlmProviderId, String?) -> Unit,
    index: Int,
    count: Int,
) {
    val customBaseUrl = configuredBaseUrl?.takeIf { providerId.isCustom }
    ApiKeyTile(
        title =
            customBaseUrl?.let { url -> runCatching { Uri.parse(url).host }.getOrNull() }
                ?: aiProviderDisplayName(providerId),
        description = customBaseUrl,
        leading = { AiProviderBadge(providerId = providerId) },
        savedLast4 = apiKeyLast4,
        isSaving = isSavingApiKey,
        onSave = { key -> onSetApiKey(providerId, key) },
        onDelete = { onSetApiKey(providerId, null) },
        // A custom provider without a key (restored from a backup) can still be removed.
        showDelete = apiKeyLast4 != null || providerId.isCustom,
        index = index,
        count = count,
    )
}

/**
 * A settings tile for one API key. With no key saved, the whole tile pastes the clipboard and says
 * so in the accent color; a pasted key is shown with Clear and Save. With a key saved, it shows the
 * key's last four characters and a delete button. Keys are paste-only so they're never typed.
 */
@Composable
internal fun ApiKeyTile(
    title: String,
    leading: @Composable () -> Unit,
    savedLast4: String?,
    isSaving: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    index: Int,
    count: Int,
    description: String? = null,
    showDelete: Boolean = savedLast4 != null,
) {
    val clipboard = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    var pendingKey by rememberSaveable(title) { mutableStateOf("") }
    val hasSavedKey = savedLast4 != null

    fun pasteFromClipboard() {
        clipboardScope.launch {
            pendingKey = clipboard.readText()?.trim().orEmpty()
        }
    }

    SegmentedTile(
        index = index,
        count = count,
        onClick = if (!hasSavedKey && pendingKey.isEmpty() && !isSaving) ::pasteFromClipboard else null,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = ApiKeyTileMinHeight)
                        .padding(SegmentedTileContentPadding),
                // With a description this is a three-line item, which Material 3 top-aligns.
                verticalAlignment = if (description != null) Alignment.Top else Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingLarge),
            ) {
                leading()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.SpacingXSmall / 2),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (description != null) Spacer(modifier = Modifier.height(DesignTokens.SpacingXSmall))
                    when {
                        hasSavedKey ->
                            Text(
                                // Only the masked tail; the delete button already marks it as saved.
                                text = "****${savedLast4.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        pendingKey.isNotEmpty() ->
                            Text(
                                // The whole key, wrapped, so it can be checked before saving.
                                text = pendingKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        else ->
                            Text(
                                text = stringResource(R.string.settings_gemini_api_key_paste_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppColors.Accent,
                            )
                    }
                }
                if (showDelete) {
                    AppIconButton(
                        enabled = !isSaving,
                        onClick = {
                            pendingKey = ""
                            onDelete()
                        },
                        tooltip = stringResource(R.string.settings_gemini_api_key_reset),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.DeleteOutline,
                            contentDescription = stringResource(R.string.settings_gemini_api_key_reset),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (!hasSavedKey && pendingKey.isNotEmpty()) {
                ApiKeySaveActions(
                    isSaving = isSaving,
                    saveEnabled = true,
                    clearEnabled = true,
                    onClear = { pendingKey = "" },
                    onSave = {
                        onSave(pendingKey)
                        pendingKey = ""
                    },
                    modifier =
                        Modifier.padding(
                            start = SegmentedTileContentPadding,
                            end = SegmentedTileContentPadding,
                            bottom = SegmentedTileContentPadding,
                        ),
                )
            }
        }
    }
}

/** Clear and Save, end-aligned, for an API key or custom provider waiting to be saved. */
@Composable
internal fun ApiKeySaveActions(
    isSaving: Boolean,
    saveEnabled: Boolean,
    clearEnabled: Boolean,
    onClear: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTextButton(enabled = !isSaving && clearEnabled, onClick = onClear) {
            Text(text = stringResource(R.string.common_action_clear))
        }
        AppButton(
            enabled = !isSaving && saveEnabled,
            onClick = onSave,
        ) {
            Text(
                text =
                    stringResource(
                        if (isSaving) R.string.settings_gemini_api_key_saving else R.string.dialog_save,
                    ),
            )
        }
    }
}

private val ApiKeyTileMinHeight = 72.dp
