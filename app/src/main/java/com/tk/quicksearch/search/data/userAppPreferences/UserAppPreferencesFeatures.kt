package com.tk.quicksearch.search.data

import android.content.Context
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.CustomSearchEngine
import com.tk.quicksearch.search.core.CustomTool
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.AppTheme
import com.tk.quicksearch.search.core.AppSuggestionTabType
import com.tk.quicksearch.search.core.SearchEngine
import com.tk.quicksearch.search.core.SearchSection
import com.tk.quicksearch.search.data.preferences.*
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.searchHistory.SearchHistoryPreferences
import com.tk.quicksearch.searchEngines.AliasValidator.isValidGeneralAliasCode
import com.tk.quicksearch.searchEngines.AliasValidator.normalizeShortcutCodeInput
import com.tk.quicksearch.shared.util.isPhysicalKeyboardConnected
import com.tk.quicksearch.tools.aiSearch.AiSearchLlmProviderId
import com.tk.quicksearch.tools.aiSearch.CustomLlmProviderConfig
import com.tk.quicksearch.tools.aiSearch.TavilyWebSearchMode
import com.tk.quicksearch.tools.tasker.TaskerIntentTool

open class UserAppPreferencesFeatures(context: Context) : UserAppPreferencesCore(context) {
    // Search Engine Preferences
    // ============================================================================

    fun hasDisabledSearchEnginesPreference(): Boolean =
            searchEnginePreferences.hasDisabledSearchEnginesPreference()

    fun getDisabledSearchEngines(): Set<String> = searchEnginePreferences.getDisabledSearchEngines()

    fun setDisabledSearchEngines(disabled: Set<String>) =
            searchEnginePreferences.setDisabledSearchEngines(disabled)

    fun getSearchEngineOrder(): List<String> = searchEnginePreferences.getSearchEngineOrder()

    fun setSearchEngineOrder(order: List<String>) =
            searchEnginePreferences.setSearchEngineOrder(order)

    fun isSearchEngineCompactMode(): Boolean = searchEnginePreferences.isSearchEngineCompactMode()

    fun setSearchEngineCompactMode(enabled: Boolean) =
            searchEnginePreferences.setSearchEngineCompactMode(enabled)

    fun getSearchEngineCompactRowCount(): Int = searchEnginePreferences.getSearchEngineCompactRowCount()

    fun setSearchEngineCompactRowCount(rowCount: Int) =
            searchEnginePreferences.setSearchEngineCompactRowCount(rowCount)

    fun isSearchEngineAliasSuffixEnabled(): Boolean =
            searchEnginePreferences.isSearchEngineAliasSuffixEnabled()

    fun setSearchEngineAliasSuffixEnabled(enabled: Boolean) =
            searchEnginePreferences.setSearchEngineAliasSuffixEnabled(enabled)

    fun isAliasTriggerAfterSpaceEnabled(): Boolean =
            searchEnginePreferences.isAliasTriggerAfterSpaceEnabled()

    fun setAliasTriggerAfterSpaceEnabled(enabled: Boolean) =
            searchEnginePreferences.setAliasTriggerAfterSpaceEnabled(enabled)

    fun hasSeenSearchEngineOnboarding(): Boolean =
            searchEnginePreferences.hasSeenSearchEngineOnboarding()

    fun setHasSeenSearchEngineOnboarding(seen: Boolean) =
            searchEnginePreferences.setHasSeenSearchEngineOnboarding(seen)

    fun getCustomSearchEngines(): List<CustomSearchEngine> =
            searchEnginePreferences.getCustomSearchEngines()

    fun setCustomSearchEngines(engines: List<CustomSearchEngine>) =
            searchEnginePreferences.setCustomSearchEngines(engines)

    fun getCustomTools(): List<CustomTool> = searchEnginePreferences.getCustomTools()

    fun setCustomTools(tools: List<CustomTool>) = searchEnginePreferences.setCustomTools(tools)

    fun getDisabledCustomTools(): Set<String> = searchEnginePreferences.getDisabledCustomTools()

    fun setDisabledCustomTools(disabled: Set<String>) =
            searchEnginePreferences.setDisabledCustomTools(disabled)

    fun getTaskerIntentTools(): List<TaskerIntentTool> = taskerIntentPreferences.getTools()

    fun setTaskerIntentTools(tools: List<TaskerIntentTool>) = taskerIntentPreferences.setTools(tools)

    // ============================================================================
    // Alias Preferences
    // ============================================================================

    fun getAliasCode(engine: SearchEngine): String = aliasPreferences.getAliasCode(engine)

    fun setAliasCode(
            engine: SearchEngine,
            code: String,
    ) = aliasPreferences.setAliasCode(engine, code)

    fun getAliasCode(targetId: String): String? = aliasPreferences.getAliasCode(targetId)

    fun getAliasCodeAllowSingleChar(targetId: String): String? =
            aliasPreferences.getAliasCodeAllowSingleChar(targetId)

    fun setAliasCode(
            targetId: String,
            code: String,
    ) = aliasPreferences.setAliasCode(targetId, code)

    fun clearAliasCode(targetId: String) = aliasPreferences.clearAliasCode(targetId)

    fun setAliasCodeAllowSingleChar(
            targetId: String,
            code: String,
    ) = aliasPreferences.setAliasCodeAllowSingleChar(targetId, code)

    fun isAliasEnabled(engine: SearchEngine): Boolean =
            aliasPreferences.isAliasEnabled(engine)

    fun setAliasEnabled(
            engine: SearchEngine,
            enabled: Boolean,
    ) = aliasPreferences.setAliasEnabled(engine, enabled)

    fun isAliasEnabled(
            targetId: String,
            defaultValue: Boolean,
    ): Boolean = aliasPreferences.isAliasEnabled(targetId, defaultValue)

    fun setAliasEnabled(
            targetId: String,
            enabled: Boolean,
    ) = aliasPreferences.setAliasEnabled(targetId, enabled)

    fun getAllAliasCodes(): Map<SearchEngine, String> = aliasPreferences.getAllAliasCodes()

    // ============================================================================
    // Amazon Domain Preferences
    // ============================================================================

    fun getAmazonDomain(): String? = amazonPreferences.getAmazonDomain()

    fun setAmazonDomain(domain: String?) = amazonPreferences.setAmazonDomain(domain)

    // ============================================================================
    // Gemini API Preferences
    // ============================================================================

    fun getAiSearchProviderId(): AiSearchLlmProviderId =
            llmPreferences.getAiSearchProviderId()

    fun setAiSearchProviderId(providerId: AiSearchLlmProviderId) =
            llmPreferences.setAiSearchProviderId(providerId)

    fun shouldShowWebSearchFallbackTip(): Boolean =
            uiPreferences.shouldShowWebSearchFallbackTip()

    fun recordWebSearchFallbackTipShown() =
            uiPreferences.recordWebSearchFallbackTipShown()

    /**
     * Generic LLM API key accessor used by provider-aware callers.
     * New providers should extend this when provider-specific credential keys are added.
     */
    fun getLlmApiKey(providerId: AiSearchLlmProviderId): String? =
        if (providerId.isCustom) {
            customLlmProviderPreferences.getProvider(providerId)?.apiKey?.takeIf { it.isNotBlank() }
        } else {
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.getGeminiApiKey()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.getApiKey()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.getApiKey()
                AiSearchLlmProviderId.GROQ -> groqPreferences.getApiKey()
                AiSearchLlmProviderId.META -> metaPreferences.getApiKey()
                else -> null
            }
        }

    fun setLlmApiKey(providerId: AiSearchLlmProviderId, key: String?) {
        if (providerId.isCustom) {
            if (key.isNullOrBlank()) {
                customLlmProviderPreferences.removeProvider(providerId)
            } else {
                customLlmProviderPreferences.setProviderApiKey(providerId, key)
            }
            refreshConfiguredAiProviderHint()
            return
        }
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setGeminiApiKey(key)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setApiKey(key)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setApiKey(key)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setApiKey(key)
            AiSearchLlmProviderId.META -> metaPreferences.setApiKey(key)
            else -> Unit
        }
        refreshConfiguredAiProviderHint()
    }

    fun getLlmModel(providerId: AiSearchLlmProviderId): String =
        if (providerId.isCustom) {
            customLlmProviderPreferences.getProvider(providerId)?.modelId.orEmpty()
        } else {
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.getGeminiModel()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.getModel()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.getModel()
                AiSearchLlmProviderId.GROQ -> groqPreferences.getModel()
                AiSearchLlmProviderId.META -> metaPreferences.getModel()
                else -> ""
            }
        }

    fun setLlmModel(providerId: AiSearchLlmProviderId, modelId: String?) {
        if (providerId.isCustom) {
            customLlmProviderPreferences.setProviderModel(providerId, modelId)
            return
        }
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setGeminiModel(modelId)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setModel(modelId)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setModel(modelId)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setModel(modelId)
            AiSearchLlmProviderId.META -> metaPreferences.setModel(modelId)
            else -> Unit
        }
    }

    fun isLlmGroundingEnabled(providerId: AiSearchLlmProviderId): Boolean =
        if (providerId.isCustom) {
            customLlmProviderPreferences.getProvider(providerId)?.groundingEnabled ?: true
        } else {
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.isGeminiGroundingEnabled()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.isGroundingEnabled()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.isGroundingEnabled()
                AiSearchLlmProviderId.GROQ -> groqPreferences.isGroundingEnabled()
                AiSearchLlmProviderId.META -> metaPreferences.isGroundingEnabled()
                else -> false
            }
        }

    fun setLlmGroundingEnabled(providerId: AiSearchLlmProviderId, enabled: Boolean) {
        if (providerId.isCustom) {
            customLlmProviderPreferences.setProviderGroundingEnabled(providerId, enabled)
            return
        }
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setGeminiGroundingEnabled(enabled)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setGroundingEnabled(enabled)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setGroundingEnabled(enabled)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setGroundingEnabled(enabled)
            AiSearchLlmProviderId.META -> metaPreferences.setGroundingEnabled(enabled)
            else -> Unit
        }
    }

    fun isLlmThinkingEnabled(providerId: AiSearchLlmProviderId): Boolean =
        if (providerId.isCustom) {
            false
        } else {
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.isThinkingEnabled()
                AiSearchLlmProviderId.OPENAI -> false
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.isThinkingEnabled()
                AiSearchLlmProviderId.GROQ -> groqPreferences.isThinkingEnabled()
                AiSearchLlmProviderId.META -> metaPreferences.isThinkingEnabled()
                else -> false
            }
        }

    fun setLlmThinkingEnabled(providerId: AiSearchLlmProviderId, enabled: Boolean) {
        if (providerId.isCustom) return
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setThinkingEnabled(enabled)
            AiSearchLlmProviderId.OPENAI -> Unit
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setThinkingEnabled(enabled)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setThinkingEnabled(enabled)
            AiSearchLlmProviderId.META -> metaPreferences.setThinkingEnabled(enabled)
            else -> Unit
        }
    }

    fun getLlmPersonalContext(providerId: AiSearchLlmProviderId): String? =
        if (providerId.isCustom) {
            openAiPreferences.getPersonalContext()
        } else {
            when (providerId) {
                AiSearchLlmProviderId.GEMINI -> geminiPreferences.getPersonalContext()
                AiSearchLlmProviderId.OPENAI -> openAiPreferences.getPersonalContext()
                AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.getPersonalContext()
                AiSearchLlmProviderId.GROQ -> groqPreferences.getPersonalContext()
                AiSearchLlmProviderId.META -> metaPreferences.getPersonalContext()
                else -> null
            }
        }

    fun setLlmPersonalContext(providerId: AiSearchLlmProviderId, context: String?) {
        if (providerId.isCustom) {
            openAiPreferences.setPersonalContext(context)
            return
        }
        when (providerId) {
            AiSearchLlmProviderId.GEMINI -> geminiPreferences.setPersonalContext(context)
            AiSearchLlmProviderId.OPENAI -> openAiPreferences.setPersonalContext(context)
            AiSearchLlmProviderId.ANTHROPIC -> anthropicPreferences.setPersonalContext(context)
            AiSearchLlmProviderId.GROQ -> groqPreferences.setPersonalContext(context)
            AiSearchLlmProviderId.META -> metaPreferences.setPersonalContext(context)
            else -> Unit
        }
    }

    /** Returns true if any supported LLM provider has a stored API key. */
    fun hasAnyLlmApiKey(): Boolean =
        !geminiPreferences.getGeminiApiKey().isNullOrBlank() ||
            !openAiPreferences.getApiKey().isNullOrBlank() ||
            !anthropicPreferences.getApiKey().isNullOrBlank() ||
            !groqPreferences.getApiKey().isNullOrBlank() ||
            !metaPreferences.getApiKey().isNullOrBlank() ||
            customLlmProviderPreferences.getProviders().any { it.apiKey.isNotBlank() }

    /** Opens encrypted storage only from an AI/settings or long-idle path. */
    fun refreshConfiguredAiProviderHint(): Boolean =
        hasAnyLlmApiKey().also { configured ->
            aiStartupPreferences.edit()
                    .putBoolean(KEY_HAS_CONFIGURED_AI_PROVIDER, configured)
                    .apply()
        }

    fun getTavilyApiKey(): String? = tavilyPreferences.getApiKey()

    fun setTavilyApiKey(key: String?) = tavilyPreferences.setApiKey(key)

    fun getTavilyWebSearchMode(): TavilyWebSearchMode = tavilyPreferences.getWebSearchMode()

    fun setTavilyWebSearchMode(mode: TavilyWebSearchMode) = tavilyPreferences.setWebSearchMode(mode)

    fun getLlmApiKeyLast4ByProvider(): Map<AiSearchLlmProviderId, String> =
        getConfiguredLlmProviderIds().mapNotNull { providerId ->
            getLlmApiKey(providerId)?.trim()?.takeIf { it.isNotBlank() }?.takeLast(4)?.let { last4 ->
                providerId to last4
            }
        }.toMap()

    fun getCustomLlmBaseUrlByProvider(): Map<AiSearchLlmProviderId, String> =
        customLlmProviderPreferences.getProviders().associate { provider ->
            AiSearchLlmProviderId.custom(provider.id) to provider.baseUrl
        }

    fun getCustomLlmAdvancedPayloadByProvider(): Map<AiSearchLlmProviderId, Pair<Boolean, String>> =
        customLlmProviderPreferences.getProviders().mapNotNull { provider ->
            val payload = provider.advancedPayload?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            AiSearchLlmProviderId.custom(provider.id) to (provider.advancedPayloadEnabled to payload)
        }.toMap()

    fun getConfiguredLlmProviderIds(): List<AiSearchLlmProviderId> =
        AiSearchLlmProviderId.entries +
            customLlmProviderPreferences.getProviders().map { AiSearchLlmProviderId.custom(it.id) }

    fun getCustomLlmProvider(providerId: AiSearchLlmProviderId): CustomLlmProviderConfig? =
        customLlmProviderPreferences.getProvider(providerId)

    fun addCustomLlmProvider(
        baseUrl: String,
        apiKey: String,
    ): CustomLlmProviderConfig? =
        customLlmProviderPreferences.addProvider(baseUrl, apiKey).also {
            if (it != null) refreshConfiguredAiProviderHint()
        }

    fun setCustomLlmAdvancedPayload(
        providerId: AiSearchLlmProviderId,
        payload: String?,
        enabled: Boolean,
    ) = customLlmProviderPreferences.setProviderAdvancedPayload(providerId, payload, enabled)

    // Backward-compatible Gemini facade methods kept for existing call sites.
    fun getGeminiApiKey(): String? = geminiPreferences.getGeminiApiKey()

    fun setGeminiApiKey(key: String?) {
        geminiPreferences.setGeminiApiKey(key)
        refreshConfiguredAiProviderHint()
    }


    fun getPersonalContext(): String? = geminiPreferences.getPersonalContext()

    fun setPersonalContext(context: String?) = geminiPreferences.setPersonalContext(context)

    fun getGeminiModel(): String = geminiPreferences.getGeminiModel()

    fun setGeminiModel(modelId: String?) = geminiPreferences.setGeminiModel(modelId)

    fun isGeminiGroundingEnabled(): Boolean = geminiPreferences.isGeminiGroundingEnabled()

    fun setGeminiGroundingEnabled(enabled: Boolean) =
            geminiPreferences.setGeminiGroundingEnabled(enabled)

    fun isGeminiThinkingEnabled(): Boolean = geminiPreferences.isThinkingEnabled()

    fun setGeminiThinkingEnabled(enabled: Boolean) =
            geminiPreferences.setThinkingEnabled(enabled)

    // ============================================================================
    // UI Preferences
    // ============================================================================

    fun isOneHandedMode(): Boolean = uiPreferences.isOneHandedMode()

    fun setOneHandedMode(enabled: Boolean) = uiPreferences.setOneHandedMode(enabled)

    fun isBottomSearchBarEnabled(): Boolean = uiPreferences.isBottomSearchBarEnabled()

    fun setBottomSearchBarEnabled(enabled: Boolean) =
            uiPreferences.setBottomSearchBarEnabled(enabled)

    fun isUnifiedPinnedItemsEnabled(): Boolean = uiPreferences.isUnifiedPinnedItemsEnabled()

    fun setUnifiedPinnedItemsEnabled(enabled: Boolean) =
            uiPreferences.setUnifiedPinnedItemsEnabled(enabled)

    fun isSearchHintsEnabled(): Boolean = uiPreferences.isSearchHintsEnabled()

    fun setSearchHintsEnabled(enabled: Boolean) = uiPreferences.setSearchHintsEnabled(enabled)

    fun isSettingsIconEnabled(): Boolean = uiPreferences.isSettingsIconEnabled()

    fun setSettingsIconEnabled(enabled: Boolean) = uiPreferences.setSettingsIconEnabled(enabled)

    fun isOpenKeyboardOnLaunchEnabled(): Boolean = uiPreferences.isOpenKeyboardOnLaunchEnabled()

    fun setOpenKeyboardOnLaunchEnabled(enabled: Boolean) =
            uiPreferences.setOpenKeyboardOnLaunchEnabled(enabled)

    fun getReservedKeyboardHeightDp(isLandscape: Boolean): Float =
            uiPreferences.getReservedKeyboardHeightDp(isLandscape)

    fun setReservedKeyboardHeightDp(isLandscape: Boolean, dp: Float) =
            uiPreferences.setReservedKeyboardHeightDp(isLandscape, dp)

    fun applyDefaultLauncherPreferencesIfNeeded(isDefaultLauncher: Boolean): Boolean =
            uiPreferences.applyDefaultLauncherPreferencesIfNeeded(isDefaultLauncher)

    fun isTopResultIndicatorEnabled(): Boolean = uiPreferences.isTopResultIndicatorEnabled()

    fun setTopResultIndicatorEnabled(enabled: Boolean) =
            uiPreferences.setTopResultIndicatorEnabled(enabled)

    fun isOpenTopResultUsingKeyboardEnabled(): Boolean =
            uiPreferences.isOpenTopResultUsingKeyboardEnabled()

    fun setOpenTopResultUsingKeyboardEnabled(enabled: Boolean) =
            uiPreferences.setOpenTopResultUsingKeyboardEnabled(enabled)

    fun isTopResultIndicatorManuallyDisabled(): Boolean =
            uiPreferences.isTopResultIndicatorManuallyDisabled()

    fun setTopResultIndicatorManuallyDisabled(disabled: Boolean) =
            uiPreferences.setTopResultIndicatorManuallyDisabled(disabled)

    fun isPhysicalKeyboardConnected(): Boolean = context.isPhysicalKeyboardConnected()

    fun isTopMatchesEnabled(): Boolean = uiPreferences.isTopMatchesEnabled()

    fun setTopMatchesEnabled(enabled: Boolean) = uiPreferences.setTopMatchesEnabled(enabled)

    fun getTopMatchesLimit(): Int = uiPreferences.getTopMatchesLimit()

    fun setTopMatchesLimit(limit: Int) = uiPreferences.setTopMatchesLimit(limit)

    fun getTopMatchesSectionOrder(): List<SearchSection> =
            uiPreferences.getTopMatchesSectionOrder()

    fun setTopMatchesSectionOrder(order: List<SearchSection>) =
            uiPreferences.setTopMatchesSectionOrder(order)

    fun getHomePinnedSectionOrder(): List<SearchSection> =
            uiPreferences.getHomePinnedSectionOrder()

    fun setHomePinnedSectionOrder(order: List<SearchSection>) =
            uiPreferences.setHomePinnedSectionOrder(order)

    fun isPinnedAppShortcutsInAppGridEnabled(): Boolean =
            uiPreferences.isPinnedAppShortcutsInAppGridEnabled()

    fun setPinnedAppShortcutsInAppGridEnabled(enabled: Boolean) =
            uiPreferences.setPinnedAppShortcutsInAppGridEnabled(enabled)

    fun getPinnedAppGridOrder(): List<String> = uiPreferences.getPinnedAppGridOrder()

    fun setPinnedAppGridOrder(order: List<String>) = uiPreferences.setPinnedAppGridOrder(order)

    fun getAppFolders(): List<com.tk.quicksearch.search.folders.AppFolder> =
            folderPreferences.getAppFolders()

    fun setAppFolders(folders: List<com.tk.quicksearch.search.folders.AppFolder>) =
            folderPreferences.setAppFolders(folders)

    fun getDisabledTopMatchesSections(): Set<SearchSection> =
            uiPreferences.getDisabledTopMatchesSections()

    fun setDisabledTopMatchesSections(disabledSections: Set<SearchSection>) =
            uiPreferences.setDisabledTopMatchesSections(disabledSections)

    fun isClearQueryOnLaunchEnabled(): Boolean = uiPreferences.isClearQueryOnLaunchEnabled()

    fun setClearQueryOnLaunchEnabled(enabled: Boolean) =
            uiPreferences.setClearQueryOnLaunchEnabled(enabled)

    fun isAutoCloseOverlayEnabled(): Boolean = uiPreferences.isAutoCloseOverlayEnabled()

    fun setAutoCloseOverlayEnabled(enabled: Boolean) =
            uiPreferences.setAutoCloseOverlayEnabled(enabled)

    fun isOverlayModeEnabled(): Boolean = uiPreferences.isOverlayModeEnabled()

    fun setOverlayModeEnabled(enabled: Boolean) = uiPreferences.setOverlayModeEnabled(enabled)

    fun getMessagingApp(): MessagingApp = uiPreferences.getMessagingApp()

    fun setMessagingApp(app: MessagingApp) = uiPreferences.setMessagingApp(app)

    fun getCallingApp(): CallingApp = uiPreferences.getCallingApp()

    fun setCallingApp(app: CallingApp) = uiPreferences.setCallingApp(app)

    fun isFirstLaunch(): Boolean = uiPreferences.isFirstLaunch()

    fun setFirstLaunchCompleted() = uiPreferences.setFirstLaunchCompleted()

    fun getWallpaperBackgroundAlpha(isDarkMode: Boolean): Float =
            uiPreferences.getWallpaperBackgroundAlpha(isDarkMode)

    fun setWallpaperBackgroundAlpha(alpha: Float, isDarkMode: Boolean) =
            uiPreferences.setWallpaperBackgroundAlpha(alpha, isDarkMode)

    fun getWallpaperBlurRadius(isDarkMode: Boolean): Float =
            uiPreferences.getWallpaperBlurRadius(isDarkMode)

    fun setWallpaperBlurRadius(radius: Float, isDarkMode: Boolean) =
            uiPreferences.setWallpaperBlurRadius(radius, isDarkMode)

    fun getAppTheme(): AppTheme = uiPreferences.getAppTheme()

    fun setAppTheme(theme: AppTheme) =
            uiPreferences.setAppTheme(theme)

    fun getAppThemeMode(): com.tk.quicksearch.search.core.AppThemeMode = uiPreferences.getAppThemeMode()

    fun setAppThemeMode(theme: com.tk.quicksearch.search.core.AppThemeMode) =
            uiPreferences.setAppThemeMode(theme)

    fun getOverlayThemeIntensity(): Float = uiPreferences.getOverlayThemeIntensity()

    fun setOverlayThemeIntensity(intensity: Float) = uiPreferences.setOverlayThemeIntensity(intensity)

    fun getFontScaleMultiplier(): Float = uiPreferences.getFontScaleMultiplier()

    fun setFontScaleMultiplier(multiplier: Float) = uiPreferences.setFontScaleMultiplier(multiplier)

    fun shouldUseSystemFont(): Boolean = uiPreferences.shouldUseSystemFont()

    fun setUseSystemFont(enabled: Boolean) = uiPreferences.setUseSystemFont(enabled)

    fun getHomeTextColorOverride(): com.tk.quicksearch.search.core.HomeTextColor? =
            uiPreferences.getHomeTextColorOverride()

    fun setHomeTextColorOverride(color: com.tk.quicksearch.search.core.HomeTextColor) =
            uiPreferences.setHomeTextColorOverride(color)

    fun clearHomeTextColorOverride() = uiPreferences.clearHomeTextColorOverride()

    fun getAppLanguageTag(): String? = uiPreferences.getAppLanguageTag()

    fun setAppLanguageTag(languageTag: String?) = uiPreferences.setAppLanguageTag(languageTag)

    fun getBackgroundSource(): BackgroundSource = uiPreferences.getBackgroundSource()

    fun setBackgroundSource(source: BackgroundSource) =
            uiPreferences.setBackgroundSource(source)

    fun getCustomImageUri(): String? = uiPreferences.getCustomImageUri()

    fun setCustomImageUri(uri: String?) = uiPreferences.setCustomImageUri(uri)

    fun getSelectedIconPackPackage(): String? = uiPreferences.getSelectedIconPackPackage()

    fun setSelectedIconPackPackage(packageName: String?) =
            uiPreferences.setSelectedIconPackPackage(packageName)

    fun isIconPackUnsupportedIconMaskEnabled(): Boolean =
            uiPreferences.isIconPackUnsupportedIconMaskEnabled()

    fun setIconPackUnsupportedIconMaskEnabled(enabled: Boolean) =
            uiPreferences.setIconPackUnsupportedIconMaskEnabled(enabled)

    fun getAppIconShape(): com.tk.quicksearch.search.core.AppIconShape =
            uiPreferences.getAppIconShape()

    fun setAppIconShape(shape: com.tk.quicksearch.search.core.AppIconShape) =
            uiPreferences.setAppIconShape(shape)

    fun getLauncherAppIcon(): com.tk.quicksearch.search.core.LauncherAppIcon =
            uiPreferences.getLauncherAppIcon()

    fun setLauncherAppIcon(selection: com.tk.quicksearch.search.core.LauncherAppIcon) =
            uiPreferences.setLauncherAppIcon(selection)

    fun isThemedIconsEnabled(): Boolean = uiPreferences.isThemedIconsEnabled()

    fun setThemedIconsEnabled(enabled: Boolean) = uiPreferences.setThemedIconsEnabled(enabled)

    fun isDeviceThemeEnabled(): Boolean = uiPreferences.isDeviceThemeEnabled()

    fun setDeviceThemeEnabled(enabled: Boolean) = uiPreferences.setDeviceThemeEnabled(enabled)

    fun isAmoledThemeEnabled(): Boolean = uiPreferences.isAmoledThemeEnabled()

    fun setAmoledThemeEnabled(enabled: Boolean) = uiPreferences.setAmoledThemeEnabled(enabled)

    fun isWallpaperAccentEnabled(): Boolean = uiPreferences.isWallpaperAccentEnabled()

    fun setWallpaperAccentEnabled(enabled: Boolean) = uiPreferences.setWallpaperAccentEnabled(enabled)

    fun getAccentColorMode(): com.tk.quicksearch.search.core.AccentColorMode =
            uiPreferences.getAccentColorMode()

    fun setAccentColorMode(mode: com.tk.quicksearch.search.core.AccentColorMode) =
            uiPreferences.setAccentColorMode(mode)

    fun getCustomAccentColorArgb(): Int = uiPreferences.getCustomAccentColorArgb()

    fun setCustomAccentColorArgb(argb: Int) = uiPreferences.setCustomAccentColorArgb(argb)

    fun isAiSearchSetupExpanded(): Boolean = uiPreferences.isAiSearchSetupExpanded()

    fun setAiSearchSetupExpanded(expanded: Boolean) =
            uiPreferences.setAiSearchSetupExpanded(expanded)

    fun isDisabledSearchEnginesExpanded(): Boolean = uiPreferences.isDisabledSearchEnginesExpanded()

    fun setDisabledSearchEnginesExpanded(expanded: Boolean) =
            uiPreferences.setDisabledSearchEnginesExpanded(expanded)

    fun isHomePinnedSectionExpanded(section: SearchSection): Boolean =
            uiPreferences.isHomePinnedSectionExpanded(section)

    fun setHomePinnedSectionExpanded(
            section: SearchSection,
            expanded: Boolean,
    ) = uiPreferences.setHomePinnedSectionExpanded(section, expanded)

    fun isUnifiedPinnedItemsExpanded(): Boolean = uiPreferences.isUnifiedPinnedItemsExpanded()

    fun setUnifiedPinnedItemsExpanded(expanded: Boolean) =
            uiPreferences.setUnifiedPinnedItemsExpanded(expanded)

    fun isInstantStartupSurfaceEnabled(): Boolean = uiPreferences.isInstantStartupSurfaceEnabled()

    fun setInstantStartupSurfaceEnabled(enabled: Boolean) =
            uiPreferences.setInstantStartupSurfaceEnabled(enabled)

    fun hasSeenSearchBarWelcome(): Boolean = uiPreferences.hasSeenSearchBarWelcome()

    fun setHasSeenSearchBarWelcome(seen: Boolean) = uiPreferences.setHasSeenSearchBarWelcome(seen)

    fun shouldForceSearchBarWelcomeOnNextOpen(): Boolean =
            uiPreferences.shouldForceSearchBarWelcomeOnNextOpen()

    fun setForceSearchBarWelcomeOnNextOpen(force: Boolean) =
            uiPreferences.setForceSearchBarWelcomeOnNextOpen(force)

    fun consumeForceSearchBarWelcomeOnNextOpen(): Boolean {
        val shouldForce = uiPreferences.shouldForceSearchBarWelcomeOnNextOpen()
        if (shouldForce) {
            uiPreferences.setForceSearchBarWelcomeOnNextOpen(false)
        }
        return shouldForce
    }

    fun hasSeenContactActionHint(): Boolean = uiPreferences.hasSeenContactActionHint()

    fun setHasSeenContactActionHint(seen: Boolean) = uiPreferences.setHasSeenContactActionHint(seen)

    fun hasSeenOverlayAssistantTip(): Boolean = uiPreferences.hasSeenOverlayAssistantTip()

    fun setHasSeenOverlayAssistantTip(seen: Boolean) =
            uiPreferences.setHasSeenOverlayAssistantTip(seen)

    fun hasSeenSettingsSearchTip(): Boolean = uiPreferences.hasSeenSettingsSearchTip()

    fun setHasSeenSettingsSearchTip(seen: Boolean) =
            uiPreferences.setHasSeenSettingsSearchTip(seen)

    fun getLastSeenVersionName(): String? = uiPreferences.getLastSeenVersionName()

    fun setLastSeenVersionName(versionName: String?) =
            uiPreferences.setLastSeenVersionName(versionName)

    fun getLastSeenVersionCode(): Long? = uiPreferences.getLastSeenVersionCode()

    fun setLastSeenVersionCode(versionCode: Long) =
            uiPreferences.setLastSeenVersionCode(versionCode)

    fun isAccessibilityPermissionDisclaimerPending(): Boolean =
            uiPreferences.isAccessibilityPermissionDisclaimerPending()

    fun setAccessibilityPermissionDisclaimerPending(pending: Boolean) =
            uiPreferences.setAccessibilityPermissionDisclaimerPending(pending)

    fun hasSeenAccessibilityPermissionDisclaimer(): Boolean =
            uiPreferences.hasSeenAccessibilityPermissionDisclaimer()

    fun setHasSeenAccessibilityPermissionDisclaimer(seen: Boolean) =
            uiPreferences.setHasSeenAccessibilityPermissionDisclaimer(seen)

    fun getUsagePermissionBannerDismissCount(): Int =
            uiPreferences.getUsagePermissionBannerDismissCount()

    fun incrementUsagePermissionBannerDismissCount() =
            uiPreferences.incrementUsagePermissionBannerDismissCount()

    fun isUsagePermissionBannerSessionDismissed(): Boolean =
            uiPreferences.isUsagePermissionBannerSessionDismissed()

    fun setUsagePermissionBannerSessionDismissed(dismissed: Boolean) =
            uiPreferences.setUsagePermissionBannerSessionDismissed(dismissed)

    fun resetUsagePermissionBannerSessionDismissed() =
            uiPreferences.resetUsagePermissionBannerSessionDismissed()

    fun shouldShowUsagePermissionBanner(): Boolean = uiPreferences.shouldShowUsagePermissionBanner()

    // ============================================================================
}
