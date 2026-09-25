package com.tk.quicksearch.search.core

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The extractors round-trip the composite [SearchUiState] back into its sub-states after every
 * legacy state update, so any field they forget silently reverts to its data-class default. Each
 * test flips every boolean flag away from its default and asserts the round-trip returns it
 * unchanged, which fails as soon as a newly added flag is left out of the extractor.
 */
class SearchStateExtractorTest {
    @Test
    fun featureStateRoundTripPreservesEveryFlag() {
        val features = withFlippedBooleans(SearchFeatureState())

        assertEquals(features, SearchStateExtractor.extractFeatureState(stateWith(features = features)))
    }

    @Test
    fun configStateRoundTripPreservesEveryFlag() {
        val config = withFlippedBooleans(SearchUiConfigState())

        assertEquals(config, SearchStateExtractor.extractConfigState(stateWith(config = config)))
    }

    @Test
    fun resultsStateRoundTripPreservesRepresentativeNonBooleanValues() {
        val results =
            SearchResultsState(
                query = "active query",
                pendingSearchResults = emptyList(),
                indexedAppCount = 17,
                cacheLastUpdatedMillis = 1234L,
                webSuggestions = listOf("first", "second"),
                detectedAliasSearchSection = SearchSection.NOTES,
                detectedCustomToolId = "custom-tool",
                detectedTaskerIntentId = "tasker-intent",
                nicknameUpdateVersion = 4,
                contactActionsVersion = 9,
            )

        assertEquals(results, SearchStateExtractor.extractResultsState(stateWith(results = results)))
    }

    @Test
    fun permissionStateRoundTripPreservesSelectedApps() {
        val permissions =
            SearchPermissionState(
                messagingApp = MessagingApp.SIGNAL,
                callingApp = CallingApp.TELEGRAM,
            )

        assertEquals(
            permissions,
            SearchStateExtractor.extractPermissionState(stateWith(permissions = permissions)),
        )
    }

    @Test
    fun featureStateRoundTripPreservesRepresentativeNonBooleanValues() {
        val features =
            SearchFeatureState(
                searchTargetsOrder = listOf(SearchTarget.Engine(SearchEngine.KAGI)),
                disabledSearchTargetIds = setOf(SearchEngine.GOOGLE.name),
                searchEngineCompactRowCount = 2,
                amazonDomain = "amazon.co.uk",
                shortcutCodes = mapOf("search" to "s"),
                disabledSections = setOf(SearchSection.FILES),
                personalContext = "Prefer concise answers",
                activeLlmModel = "test-model",
                webSuggestionsCount = 7,
                weatherLocation = "New York",
                recentQueriesDisplayCount = 8,
                appResultRowCount = 3,
                topMatchesLimit = 6,
                topMatchesSectionOrder = listOf(SearchSection.NOTES, SearchSection.APPS),
                disabledTopMatchesSections = setOf(SearchSection.CONTACTS),
            )

        assertEquals(features, SearchStateExtractor.extractFeatureState(stateWith(features = features)))
    }

    @Test
    fun configStateRoundTripPreservesRepresentativeNonBooleanValues() {
        val config =
            SearchUiConfigState(
                startupPhase = StartupPhase.COMPLETE,
                errorMessage = "startup failed",
                wallpaperBackgroundAlpha = 0.42f,
                wallpaperBlurRadius = 18f,
                appTheme = AppTheme.FOREST,
                overlayThemeIntensity = 0.73f,
                appThemeMode = AppThemeMode.DARK,
                backgroundSource = BackgroundSource.CUSTOM_IMAGE,
                customImageUri = "content://wallpaper",
                startupBackgroundPreviewPath = "/tmp/preview",
                fontScaleMultiplier = 1.15f,
                phoneAppGridColumns = 7,
                appIconSizeStep = 2,
                appIconShape = AppIconShape.CIRCLE,
                launcherAppIcon = LauncherAppIcon.FOREST_DARK,
                selectedAppSuggestionTab = AppSuggestionTabType.PINNED,
                enabledAppSuggestionTabs = setOf(AppSuggestionTabType.PINNED),
                selectedIconPackPackage = "com.example.icons",
                availableIconPacks = listOf(IconPackInfo("com.example.icons", "Example")),
                enabledFileTypes = emptySet(),
                folderWhitelistPatterns = setOf("Documents/**"),
                folderBlacklistPatterns = setOf("Android/**"),
                excludedFileExtensions = setOf("tmp"),
                releaseNotesVersionName = "9.9.9",
                pendingDirectCallNumber = "+15551234567",
            )

        assertEquals(config, SearchStateExtractor.extractConfigState(stateWith(config = config)))
    }

    private fun stateWith(
        results: SearchResultsState = SearchResultsState(),
        permissions: SearchPermissionState = SearchPermissionState(),
        features: SearchFeatureState = SearchFeatureState(),
        config: SearchUiConfigState = SearchUiConfigState(),
    ) = SearchUiState(
        results = results,
        permissions = permissions,
        features = features,
        config = config,
    )

    /**
     * Rebuilds [instance] through its primary constructor with every boolean property inverted.
     * Kotlin emits backing fields in constructor-parameter order, so the declared fields line up
     * with the constructor arguments positionally.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> withFlippedBooleans(instance: T): T {
        val constructor = primaryConstructorOf(instance.javaClass)
        val fields =
            instance.javaClass.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }
        check(fields.size == constructor.parameterCount) {
            "Expected ${constructor.parameterCount} backing fields, found ${fields.size}"
        }

        val arguments =
            fields.map { field ->
                field.isAccessible = true
                val value = field.get(instance)
                if (value is Boolean) !value else value
            }
        return constructor.newInstance(*arguments.toTypedArray()) as T
    }

    private fun primaryConstructorOf(type: Class<*>): Constructor<*> =
        type.declaredConstructors
            .filterNot { constructor ->
                constructor.parameterTypes.any { it.name.endsWith("DefaultConstructorMarker") }
            }.maxByOrNull { it.parameterCount }
            ?: error("No primary constructor found for ${type.name}")
}
