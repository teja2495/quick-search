package com.tk.quicksearch.search.core

import android.widget.Toast
import com.tk.quicksearch.R
import com.tk.quicksearch.overlay.OverlayModeController
import com.tk.quicksearch.search.apps.IconPackService
import com.tk.quicksearch.search.data.userAppPreferences.UserAppPreferences
import com.tk.quicksearch.search.data.preferences.SwipeGestureAction
import com.tk.quicksearch.search.data.preferences.UiPreferences
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.tools.aiSearch.AiSearchHandler
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderRegistry
import com.tk.quicksearch.tools.aiSearch.GeminiTextModel
import com.tk.quicksearch.tools.aiSearch.resolveModelSelection
import com.tk.quicksearch.settings.settingsDetailScreen.AiBackedToolConfigId
import com.tk.quicksearch.shared.util.isLowRamDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun SearchPreferencesDelegate.setSearchEngineAliasSuffixEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setSearchEngineAliasSuffixEnabled(enabled)
            updateFeatureState { it.copy(isSearchEngineAliasSuffixEnabled = enabled) }
        }
    }

internal fun SearchPreferencesDelegate.setAliasTriggerAfterSpaceEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setAliasTriggerAfterSpaceEnabled(enabled)
            updateFeatureState { it.copy(isAliasTriggerAfterSpaceEnabled = enabled) }
        }
    }

internal fun SearchPreferencesDelegate.setFileTypeEnabled(fileType: FileType, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            val updated =
                stateAccess.enabledFileTypes.toMutableSet().apply {
                    if (enabled) add(fileType) else remove(fileType)
                }
            stateAccess.enabledFileTypes = updated
            userPreferences.setEnabledFileTypes(stateAccess.enabledFileTypes)
            updateUiState { it.copy(enabledFileTypes = stateAccess.enabledFileTypes) }
            rerunSecondarySearchIfNeeded()
        }
    }

internal fun SearchPreferencesDelegate.setShowFolders(show: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setShowFoldersInResults(show)
            stateAccess.showFolders = show
            updateUiState { it.copy(showFolders = show) }
            rerunSecondarySearchIfNeeded()
        }
    }

internal fun SearchPreferencesDelegate.setFilePreviewsEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setFilePreviewsEnabled(enabled)
            stateAccess.filePreviewsEnabled = enabled
            updateUiState { it.copy(filePreviewsEnabled = enabled) }
        }
    }

internal fun SearchPreferencesDelegate.setShowSystemFiles(show: Boolean) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setShowSystemFiles(show)
            stateAccess.showSystemFiles = show
            updateUiState { it.copy(showSystemFiles = show) }
            rerunSecondarySearchIfNeeded()
        }
    }

internal fun SearchPreferencesDelegate.setFolderWhitelistPatterns(patterns: Set<String>) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setFolderWhitelistPatterns(patterns)
            stateAccess.folderWhitelistPatterns = userPreferences.getFolderWhitelistPatterns()
            updateUiState { it.copy(folderWhitelistPatterns = stateAccess.folderWhitelistPatterns) }
            rerunSecondarySearchIfNeeded()
        }
    }

internal fun SearchPreferencesDelegate.setFolderBlacklistPatterns(patterns: Set<String>) {
        scope.launch(Dispatchers.IO) {
            userPreferences.setFolderBlacklistPatterns(patterns)
            stateAccess.folderBlacklistPatterns = userPreferences.getFolderBlacklistPatterns()
            updateUiState { it.copy(folderBlacklistPatterns = stateAccess.folderBlacklistPatterns) }
            rerunSecondarySearchIfNeeded()
        }
    }

internal fun SearchPreferencesDelegate.setOneHandedMode(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setOneHandedMode,
            stateUpdater = {
                stateAccess.oneHandedMode = it
                updateUiState { state -> state.copy(oneHandedMode = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

internal fun SearchPreferencesDelegate.setBottomSearchBarEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setBottomSearchBarEnabled,
            stateUpdater = {
                stateAccess.bottomSearchBarEnabled = it
                updateUiState { state -> state.copy(bottomSearchBarEnabled = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

internal fun SearchPreferencesDelegate.setUnifiedPinnedItemsEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setUnifiedPinnedItemsEnabled,
            stateUpdater = {
                stateAccess.unifiedPinnedItemsEnabled = it
                updateUiState { state -> state.copy(unifiedPinnedItemsEnabled = it) }
            },
        )
    }

internal fun SearchPreferencesDelegate.setSearchHintsEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setSearchHintsEnabled,
            stateUpdater = {
                updateUiState { state -> state.copy(searchHintsEnabled = it) }
            },
        )
    }

internal fun SearchPreferencesDelegate.setSettingsIconEnabled(enabled: Boolean) {
        if (!enabled && userPreferences.getSwipeLeftAction() != SwipeGestureAction.SETTINGS) {
            return
        }
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setSettingsIconEnabled,
            stateUpdater = {
                stateAccess.settingsIconEnabled = it
                updateUiState { state -> state.copy(settingsIconEnabled = it) }
            },
        )
    }

internal fun SearchPreferencesDelegate.setOpenKeyboardOnLaunchEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setOpenKeyboardOnLaunchEnabled,
            stateUpdater = {
                stateAccess.openKeyboardOnLaunch = it
                updateUiState { state -> state.copy(openKeyboardOnLaunch = it) }
                stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
            },
        )
    }

internal fun SearchPreferencesDelegate.setTopResultIndicatorEnabled(enabled: Boolean) {
        if (!enabled && userPreferences.isPhysicalKeyboardConnected()) {
            return
        }
        scope.launch(Dispatchers.IO) {
            if (stateAccess.topResultIndicatorEnabled == enabled) return@launch
            userPreferences.setTopResultIndicatorEnabled(enabled)
            userPreferences.setTopResultIndicatorManuallyDisabled(!enabled)
            stateAccess.topResultIndicatorEnabled = enabled
            updateUiState { state -> state.copy(topResultIndicatorEnabled = enabled) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

internal fun SearchPreferencesDelegate.setOpenTopResultUsingKeyboardEnabled(enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.openTopResultUsingKeyboardEnabled == enabled) return@launch

            userPreferences.setOpenTopResultUsingKeyboardEnabled(enabled)
            val restoreTopResultIndicator =
                enabled && !userPreferences.isTopResultIndicatorManuallyDisabled()
            if (!enabled || restoreTopResultIndicator) {
                userPreferences.setTopResultIndicatorEnabled(enabled)
            }

            stateAccess.openTopResultUsingKeyboardEnabled = enabled
            stateAccess.topResultIndicatorEnabled = if (!enabled) false else restoreTopResultIndicator || stateAccess.topResultIndicatorEnabled
            updateUiState { state ->
                state.copy(
                    openTopResultUsingKeyboardEnabled = enabled,
                    topResultIndicatorEnabled =
                        if (!enabled) false else restoreTopResultIndicator || state.topResultIndicatorEnabled,
                )
            }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

internal fun SearchPreferencesDelegate.setAccentColorMode(mode: AccentColorMode) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.accentColorMode == mode) return@launch
            userPreferences.setAccentColorMode(mode)
            stateAccess.accentColorMode = mode
            updateConfigState { it.copy(accentColorMode = mode) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

internal fun SearchPreferencesDelegate.setCustomAccentColorArgb(argb: Int) {
        scope.launch(Dispatchers.IO) {
            if (stateAccess.customAccentColorArgb == argb) return@launch
            userPreferences.setCustomAccentColorArgb(argb)
            stateAccess.customAccentColorArgb = argb
            updateConfigState { it.copy(customAccentColorArgb = argb) }
            stateAccess.saveStartupSurfaceSnapshotAsync(allowDuringQuery = true)
        }
    }

internal fun SearchPreferencesDelegate.setClearQueryOnLaunchEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setClearQueryOnLaunchEnabled,
            stateUpdater = {
                stateAccess.clearQueryOnLaunch = it
                updateUiState { state -> state.copy(clearQueryOnLaunch = it) }
            },
        )
    }

internal fun SearchPreferencesDelegate.setAutoCloseOverlayEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setAutoCloseOverlayEnabled,
            stateUpdater = {
                stateAccess.autoCloseOverlay = it
                updateUiState { state -> state.copy(autoCloseOverlay = it) }
            },
        )
    }

internal fun SearchPreferencesDelegate.setOverlayModeEnabled(enabled: Boolean) {
        updateBooleanPreference(
            value = enabled,
            preferenceSetter = userPreferences::setOverlayModeEnabled,
            stateUpdater = {
                stateAccess.overlayModeEnabled = it
                updateUiState { state ->
                    state.copy(
                        overlayModeEnabled = it,
                        wallpaperBackgroundAlpha = stateAccess.wallpaperBackgroundAlpha,
                        wallpaperBlurRadius = stateAccess.wallpaperBlurRadius,
                    )
                }
                if (!it) {
                    OverlayModeController.stopOverlay(applicationProvider())
                }
            },
        )
    }

internal fun SearchPreferencesDelegate.setAmazonDomain(domain: String?) {
        stateAccess.amazonDomain = domain
        userPreferences.setAmazonDomain(domain)
        updateFeatureState { state -> state.copy(amazonDomain = stateAccess.amazonDomain) }
    }

internal fun SearchPreferencesDelegate.setGeminiApiKey(apiKey: String?) {
        val providerId =
            apiKey?.takeIf { it.isNotBlank() }?.let(AiSearchLlmProviderId::detectFromApiKey)
                ?: aiSearchHandler.getAiSearchProviderId()
        setLlmApiKey(providerId, apiKey)
    }

internal fun SearchPreferencesDelegate.setLlmApiKey(
        providerId: AiSearchLlmProviderId,
        apiKey: String?,
    ) {
        scope.launch(Dispatchers.IO) {
            updateFeatureState { it.copy(isSavingGeminiApiKey = true) }
            try {
                val hasKey = !apiKey.isNullOrBlank()
                val isNewKey = hasKey && userPreferences.getLlmApiKey(providerId).isNullOrBlank()

                aiSearchHandler.setLlmApiKey(providerId, apiKey)

                val hasAnyKey = userPreferences.hasAnyLlmApiKey()
                searchEngineManager.updateSearchTargetsForGemini(hasAnyKey)

                val providerModels =
                    if (hasKey) {
                        fetchAvailableModels(providerId, apiKey.orEmpty()).getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                if (isNewKey) {
                    userPreferences.setLlmModel(providerId, null)
                    userPreferences.setLlmGroundingEnabled(providerId, true)
                    userPreferences.setLlmThinkingEnabled(providerId, false)
                    if (providerId == aiSearchHandler.getAiSearchProviderId()) {
                        aiSearchHandler.setSelectedModelId(null)
                        aiSearchHandler.setGroundingEnabled(true)
                        aiSearchHandler.setThinkingEnabled(false)
                    }
                }
                val availableModels =
                    if (hasKey && providerId == aiSearchHandler.getAiSearchProviderId()) {
                        aiSearchHandler.updateAvailableModels(providerModels)
                        aiSearchHandler.getAvailableGeminiModels()
                    } else {
                        aiSearchHandler.getAvailableGeminiModels()
                    }

                updateFeatureState {
                    val modelMap =
                        if (hasKey) {
                            it.availableLlmModelsByProvider + (providerId to providerModels)
                        } else {
                            it.availableLlmModelsByProvider - providerId
                        }
                    it.copy(
                        hasApiKey = hasAnyKey,
                        geminiApiKeyLast4 = aiSearchHandler.getLlmApiKey()?.trim()?.takeLast(4),
                        llmApiKeyLast4ByProvider = userPreferences.getLlmApiKeyLast4ByProvider(),
                        customLlmBaseUrlByProvider = userPreferences.getCustomLlmBaseUrlByProvider(),
                        customLlmAdvancedPayloadByProvider = userPreferences.getCustomLlmAdvancedPayloadByProvider(),
                        aiSearchLlmProviderId = aiSearchHandler.getAiSearchProviderId(),
                        personalContext = aiSearchHandler.getPersonalContext(),
                        geminiModel = aiSearchHandler.getGeminiModel(),
                        geminiGroundingEnabled = aiSearchHandler.isGeminiGroundingEnabled(),
                        geminiThinkingEnabled = aiSearchHandler.isGeminiThinkingEnabled(),
                        availableGeminiModels = availableModels,
                        availableLlmModelsByProvider =
                            modelMap + (aiSearchHandler.getAiSearchProviderId() to availableModels),
                    )
                }
            } finally {
                updateFeatureState { it.copy(isSavingGeminiApiKey = false) }
            }
        }
    }

internal fun SearchPreferencesDelegate.addCustomLlmProvider(
        baseUrl: String,
        apiKey: String,
    ) {
        scope.launch(Dispatchers.IO) {
            updateFeatureState { it.copy(isSavingGeminiApiKey = true) }
            try {
                val provider = userPreferences.addCustomLlmProvider(baseUrl, apiKey) ?: return@launch
                val providerId = AiSearchLlmProviderId.custom(provider.id)
                aiSearchHandler.setLlmApiKey(providerId, provider.apiKey)
                aiSearchHandler.setAiSearchProviderId(providerId)

                val modelsResult = fetchAvailableModels(providerId, provider.apiKey)
                modelsResult.exceptionOrNull()?.let { error ->
                    showCustomProviderModelsError(error)
                }
                val models = modelsResult.getOrDefault(emptyList())
                aiSearchHandler.setSelectedModelId(null)
                aiSearchHandler.setGroundingEnabled(true)
                aiSearchHandler.setThinkingEnabled(false)
                aiSearchHandler.updateAvailableModels(models)
                val hasAnyKey = userPreferences.hasAnyLlmApiKey()
                searchEngineManager.updateSearchTargetsForGemini(hasAnyKey)

                updateFeatureState {
                    it.copy(
                        hasApiKey = hasAnyKey,
                        geminiApiKeyLast4 = aiSearchHandler.getLlmApiKey()?.trim()?.takeLast(4),
                        llmApiKeyLast4ByProvider = userPreferences.getLlmApiKeyLast4ByProvider(),
                        customLlmBaseUrlByProvider = userPreferences.getCustomLlmBaseUrlByProvider(),
                        customLlmAdvancedPayloadByProvider = userPreferences.getCustomLlmAdvancedPayloadByProvider(),
                        aiSearchLlmProviderId = aiSearchHandler.getAiSearchProviderId(),
                        personalContext = aiSearchHandler.getPersonalContext(),
                        geminiModel = aiSearchHandler.getGeminiModel(),
                        geminiGroundingEnabled = aiSearchHandler.isGeminiGroundingEnabled(),
                        geminiThinkingEnabled = aiSearchHandler.isGeminiThinkingEnabled(),
                        availableGeminiModels = models,
                        availableLlmModelsByProvider =
                            it.availableLlmModelsByProvider + (providerId to models),
                    )
                }
            } finally {
                updateFeatureState { it.copy(isSavingGeminiApiKey = false) }
            }
        }
    }

internal fun SearchPreferencesDelegate.setPersonalContext(context: String?) {
        scope.launch(Dispatchers.IO) {
            aiSearchHandler.setPersonalContext(context)
            updateFeatureState { it.copy(personalContext = context?.trim().orEmpty()) }
        }
    }

internal fun SearchPreferencesDelegate.setGeminiModel(modelId: String?) {
        scope.launch(Dispatchers.IO) {
            aiSearchHandler.setGeminiModel(modelId)
            val normalized = modelId?.trim().takeUnless { it.isNullOrBlank() }
            updateFeatureState {
                it.copy(
                    geminiModel = normalized.orEmpty(),
                )
            }
        }
    }
