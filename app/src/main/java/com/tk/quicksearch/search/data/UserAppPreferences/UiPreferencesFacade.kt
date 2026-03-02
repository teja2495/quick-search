package com.tk.quicksearch.search.data

import com.tk.quicksearch.search.core.BackgroundSource
import com.tk.quicksearch.search.core.CallingApp
import com.tk.quicksearch.search.core.MessagingApp
import com.tk.quicksearch.search.core.OverlayGradientTheme
import com.tk.quicksearch.search.data.preferences.UiPreferences

/**
 * Facade for UI preference operations
 */
class UiPreferencesFacade(
    private val uiPreferences: UiPreferences
) {
    fun isOneHandedMode(): Boolean = uiPreferences.isOneHandedMode()

    fun setOneHandedMode(enabled: Boolean) = uiPreferences.setOneHandedMode(enabled)

    fun isBottomSearchBarEnabled(): Boolean = uiPreferences.isBottomSearchBarEnabled()

    fun setBottomSearchBarEnabled(enabled: Boolean) =
            uiPreferences.setBottomSearchBarEnabled(enabled)

    fun isOverlayModeEnabled(): Boolean = uiPreferences.isOverlayModeEnabled()

    fun setOverlayModeEnabled(enabled: Boolean) = uiPreferences.setOverlayModeEnabled(enabled)

    fun getMessagingApp(): MessagingApp = uiPreferences.getMessagingApp()

    fun setMessagingApp(app: MessagingApp) = uiPreferences.setMessagingApp(app)

    fun getCallingApp(): CallingApp = uiPreferences.getCallingApp()

    fun setCallingApp(app: CallingApp) = uiPreferences.setCallingApp(app)

    fun isFirstLaunch(): Boolean = uiPreferences.isFirstLaunch()

    fun setFirstLaunchCompleted() = uiPreferences.setFirstLaunchCompleted()

    fun getWallpaperBackgroundAlpha(): Float = uiPreferences.getWallpaperBackgroundAlpha()

    fun setWallpaperBackgroundAlpha(alpha: Float) = uiPreferences.setWallpaperBackgroundAlpha(alpha)

    fun getWallpaperBlurRadius(): Float = uiPreferences.getWallpaperBlurRadius()

    fun setWallpaperBlurRadius(radius: Float) = uiPreferences.setWallpaperBlurRadius(radius)

    fun getOverlayGradientTheme(): OverlayGradientTheme = uiPreferences.getOverlayGradientTheme()

    fun setOverlayGradientTheme(theme: OverlayGradientTheme) =
            uiPreferences.setOverlayGradientTheme(theme)

    fun getOverlayThemeIntensity(): Float = uiPreferences.getOverlayThemeIntensity()

    fun setOverlayThemeIntensity(intensity: Float) = uiPreferences.setOverlayThemeIntensity(intensity)

    fun getFontScaleMultiplier(): Float = uiPreferences.getFontScaleMultiplier()

    fun setFontScaleMultiplier(multiplier: Float) = uiPreferences.setFontScaleMultiplier(multiplier)

    fun getBackgroundSource(): BackgroundSource = uiPreferences.getBackgroundSource()

    fun setBackgroundSource(source: BackgroundSource) =
            uiPreferences.setBackgroundSource(source)

    fun getCustomImageUri(): String? = uiPreferences.getCustomImageUri()

    fun setCustomImageUri(uri: String?) = uiPreferences.setCustomImageUri(uri)

    fun getSelectedIconPackPackage(): String? = uiPreferences.getSelectedIconPackPackage()

    fun setSelectedIconPackPackage(packageName: String?) =
            uiPreferences.setSelectedIconPackPackage(packageName)

    fun isDirectSearchSetupExpanded(): Boolean = uiPreferences.isDirectSearchSetupExpanded()

    fun setDirectSearchSetupExpanded(expanded: Boolean) =
            uiPreferences.setDirectSearchSetupExpanded(expanded)

    fun isDisabledSearchEnginesExpanded(): Boolean = uiPreferences.isDisabledSearchEnginesExpanded()

    fun setDisabledSearchEnginesExpanded(expanded: Boolean) =
            uiPreferences.setDisabledSearchEnginesExpanded(expanded)

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

    fun hasSeenPersonalContextHint(): Boolean = uiPreferences.hasSeenPersonalContextHint()

    fun setHasSeenPersonalContextHint(seen: Boolean) =
            uiPreferences.setHasSeenPersonalContextHint(seen)

    fun hasDismissedSearchHistoryTip(): Boolean = uiPreferences.hasDismissedSearchHistoryTip()

    fun setSearchHistoryTipDismissed(dismissed: Boolean) =
            uiPreferences.setSearchHistoryTipDismissed(dismissed)

    fun hasSeenOverlayAssistantTip(): Boolean = uiPreferences.hasSeenOverlayAssistantTip()

    fun setHasSeenOverlayAssistantTip(seen: Boolean) =
            uiPreferences.setHasSeenOverlayAssistantTip(seen)

    fun getLastSeenVersionName(): String? = uiPreferences.getLastSeenVersionName()

    fun setLastSeenVersionName(versionName: String?) =
            uiPreferences.setLastSeenVersionName(versionName)

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

    fun areAppSuggestionsEnabled(): Boolean = uiPreferences.areAppSuggestionsEnabled()

    fun setAppSuggestionsEnabled(enabled: Boolean) = uiPreferences.setAppSuggestionsEnabled(enabled)

    fun shouldShowAppLabels(): Boolean = uiPreferences.shouldShowAppLabels()

    fun setShowAppLabels(show: Boolean) = uiPreferences.setShowAppLabels(show)

    fun areWebSuggestionsEnabled(): Boolean = uiPreferences.areWebSuggestionsEnabled()

    fun getWebSuggestionsCount(): Int = uiPreferences.getWebSuggestionsCount()

    fun setWebSuggestionsCount(count: Int) {
        uiPreferences.setWebSuggestionsCount(count)
    }

    fun setWebSuggestionsEnabled(enabled: Boolean) = uiPreferences.setWebSuggestionsEnabled(enabled)

    fun isCalculatorEnabled(): Boolean = uiPreferences.isCalculatorEnabled()

    fun setCalculatorEnabled(enabled: Boolean) = uiPreferences.setCalculatorEnabled(enabled)

    fun getShortcutHintBannerDismissCount(): Int = uiPreferences.getShortcutHintBannerDismissCount()

    fun incrementShortcutHintBannerDismissCount() =
            uiPreferences.incrementShortcutHintBannerDismissCount()

    fun isShortcutHintBannerSessionDismissed(): Boolean =
            uiPreferences.isShortcutHintBannerSessionDismissed()

    fun setShortcutHintBannerSessionDismissed(dismissed: Boolean) =
            uiPreferences.setShortcutHintBannerSessionDismissed(dismissed)

    fun resetShortcutHintBannerSessionDismissed() =
            uiPreferences.resetShortcutHintBannerSessionDismissed()

    fun shouldShowShortcutHintBanner(): Boolean = uiPreferences.shouldShowShortcutHintBanner()

    fun shouldShowDefaultEngineHintBanner(): Boolean =
            uiPreferences.shouldShowDefaultEngineHintBanner()

    fun setDefaultEngineHintBannerDismissed(dismissed: Boolean) =
            uiPreferences.setDefaultEngineHintBannerDismissed(dismissed)

    fun getDisabledSections(): Set<String> = uiPreferences.getDisabledSections()

    fun setDisabledSections(disabled: Set<String>) = uiPreferences.setDisabledSections(disabled)

    fun getFirstAppOpenTime(): Long = uiPreferences.getFirstAppOpenTime()

    fun recordFirstAppOpenTime() = uiPreferences.recordFirstAppOpenTime()

    fun getLastReviewPromptTime(): Long = uiPreferences.getLastReviewPromptTime()

    fun recordReviewPromptTime() = uiPreferences.recordReviewPromptTime()

    fun getReviewPromptedCount(): Int = uiPreferences.getReviewPromptedCount()

    fun incrementReviewPromptedCount() = uiPreferences.incrementReviewPromptedCount()

    fun getAppOpenCount(): Int = uiPreferences.getAppOpenCount()

    fun incrementAppOpenCount() = uiPreferences.incrementAppOpenCount()

    fun recordAppOpenCountAtPrompt() = uiPreferences.recordAppOpenCountAtPrompt()

    fun shouldShowReviewPrompt(): Boolean = uiPreferences.shouldShowReviewPrompt()

    fun hasShownUpdateCheckThisSession(): Boolean = uiPreferences.hasShownUpdateCheckThisSession()

    fun setUpdateCheckShownThisSession() = uiPreferences.setUpdateCheckShownThisSession()

    fun resetUpdateCheckSession() = uiPreferences.resetUpdateCheckSession()
}