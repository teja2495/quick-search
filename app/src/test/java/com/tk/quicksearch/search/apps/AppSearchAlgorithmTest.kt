package com.tk.quicksearch.search.apps

import com.tk.quicksearch.search.fuzzy.FuzzySearchConfig
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.SecondaryRankingSignal
import com.tk.quicksearch.search.utils.CachedSearchMatcher
import com.tk.quicksearch.search.utils.SearchTextCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchAlgorithmTest {

    @Test
    fun secondaryRankingSignalChangesOrderWithinSameMatchTier() {
        val alpha = app("Alpha Search", "alpha", launchCount = 2, lastUsedTime = 300L)
        val beta = app("Beta Search", "beta", launchCount = 8, lastUsedTime = 100L)
        val source = listOf(beta, alpha)

        fun ranked(signal: SecondaryRankingSignal) =
            AppSearchAlgorithm.findMatches(
                query = "search",
                source = source,
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = signal,
            )

        assertEquals(listOf(alpha, beta), ranked(SecondaryRankingSignal.RECENCY))
        assertEquals(listOf(beta, alpha), ranked(SecondaryRankingSignal.MOST_OPENED))
        assertEquals(listOf(alpha, beta), ranked(SecondaryRankingSignal.NONE))
    }

    @Test
    fun secondaryRankingSignalDoesNotOverridePrimaryMatchQuality() {
        val exactMatch = app("Search", "exact", launchCount = 1, lastUsedTime = 1L)
        val weakerMatch = app("Alpha Search", "weaker", launchCount = 1_000, lastUsedTime = 1_000L)

        SecondaryRankingSignal.entries.forEach { signal ->
            val matches =
                AppSearchAlgorithm.findMatches(
                    query = "search",
                    source = listOf(weakerMatch, exactMatch),
                    limit = 10,
                    fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                    appNicknames = emptyMap(),
                    secondaryRankingSignal = signal,
                )

            assertEquals("Unexpected order for $signal", listOf(exactMatch, weakerMatch), matches)
        }
    }

    @Test
    fun deterministicMatchesRankBeforeFuzzyOnlyMatches() {
        val exactMatch = app("Settings", "settings", launchCount = 1)
        val fuzzyOnlyMatch = app("Settlings", "settlings", launchCount = 100)

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "settings",
                source = listOf(fuzzyOnlyMatch, exactMatch),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.RECENCY,
            )

        assertEquals(listOf(exactMatch, fuzzyOnlyMatch), matches)
    }

    @Test
    fun shortTypoQueriesDoNotReturnNoisyFuzzyMatches() {
        val matches =
            AppSearchAlgorithm.findMatches(
                query = "zz",
                source = listOf(app("Gmail", "gmail")),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun typoQueriesCanFindAppsWhenDeterministicMatchingMisses() {
        val settings = app("Settings", "settings")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "setings",
                source = listOf(settings),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertEquals(listOf(settings), matches)
    }

    @Test
    fun localizedAppLabelMatchesItsCachedEnglishAlias() {
        val whatsapp = app("واتساب", "whatsapp", searchAliases = listOf("WhatsApp"))

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "whatsapp",
                source = listOf(whatsapp),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertEquals(listOf(whatsapp), matches)
    }

    @Test
    fun disabledFuzzySearchDoesNotReturnTypoOnlyMatches() {
        val settings = app("Settings", "settings")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "setings",
                source = listOf(settings),
                limit = 10,
                fuzzySearchStrategy =
                    FuzzyAppSearchStrategy(
                        config = FuzzySearchConfig.DEFAULT_APP_CONFIG,
                        isFuzzySearchEnabled = { false },
                    ),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun lowRamDevicesDoNotReturnTypoOnlyMatches() {
        val settings = app("Settings", "settings")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "setings",
                source = listOf(settings),
                limit = 10,
                fuzzySearchStrategy =
                    FuzzyAppSearchStrategy(
                        config = FuzzySearchConfig.DEFAULT_APP_CONFIG,
                        isLowRamDevice = true,
                    ),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertTrue(matches.isEmpty())
    }

    @Test
    fun githubTypoQueriesReturnGithubResult() {
        val github = app("GitHub", "github")

        val deletedCharMatches =
            AppSearchAlgorithm.findMatches(
                query = "Githb",
                source = listOf(github),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )
        val substitutedCharMatches =
            AppSearchAlgorithm.findMatches(
                query = "Githbb",
                source = listOf(github),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertEquals(listOf(github), deletedCharMatches)
        assertEquals(listOf(github), substitutedCharMatches)
    }

    @Test
    fun punctuationSeparatedAppNameMatchesCompactPrefixQuery() {
        val fdroid = app("F-Droid", "fdroid")

        listOf("fdr", "fdro", "fdroi").forEach { query ->
            val matches =
                AppSearchAlgorithm.findMatches(
                    query = query,
                    source = listOf(fdroid),
                    limit = 10,
                    fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                    appNicknames = emptyMap(),
                    secondaryRankingSignal = SecondaryRankingSignal.NONE,
                )

            assertEquals("Expected F-Droid to match $query", listOf(fdroid), matches)
        }
    }

    @Test
    fun typoEligibleCandidatesAreNotStarvedByUnrelatedApps() {
        val github = app("GitHub", "github")
        val unrelatedApps =
            (1..1_300).map { index ->
                app("Camera$index", "camera$index", launchCount = 100 + index)
            }

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "githb",
                source = unrelatedApps + github,
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.RECENCY,
            )

        assertTrue(matches.contains(github))
    }

    @Test
    fun preparedCatalogPathProducesTheSameResultsAsTheUncachedPath() {
        val apps =
            listOf(
                app("Settings", "settings", launchCount = 3),
                app("Settlings", "settlings", launchCount = 8),
                app("واتساب", "whatsapp", searchAliases = listOf("WhatsApp")),
                app("F-Droid", "fdroid"),
                app("Passport Office", "passport"),
            )
        val nicknames = mapOf("com.example.passport" to "Bala Guna Teja")
        val textCache = SearchTextCache()
        val cachedStrategy =
            FuzzyAppSearchStrategy(
                config = FuzzySearchConfig.DEFAULT_APP_CONFIG,
                textCache = textCache,
            )
        val preparedData = apps.associate { it.launchCountKey() to PreparedAppSearchData.from(it) }

        listOf("settings", "setings", "whatsapp", "fdroi", "balagunateja", "missing").forEach { query ->
            val uncached =
                AppSearchAlgorithm.findMatches(
                    query = query,
                    source = apps,
                    limit = 10,
                    fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                    appNicknames = nicknames,
                    secondaryRankingSignal = SecondaryRankingSignal.MOST_OPENED,
                )
            val cached =
                AppSearchAlgorithm.findMatches(
                    query = query,
                    source = apps,
                    limit = 10,
                    fuzzySearchStrategy = cachedStrategy,
                    appNicknames = nicknames,
                    secondaryRankingSignal = SecondaryRankingSignal.MOST_OPENED,
                    matcher = CachedSearchMatcher(textCache),
                    preparedAppData = preparedData,
                )

            assertEquals("Unexpected prepared-path results for $query", uncached, cached)
        }
    }

    @Test
    fun fuzzyPreferenceIsReadOncePerPreparedQueryAndToken() {
        var preferenceReads = 0
        val strategy =
            FuzzyAppSearchStrategy(
                config = FuzzySearchConfig.DEFAULT_APP_CONFIG,
                isFuzzySearchEnabled = {
                    preferenceReads += 1
                    true
                },
            )
        val apps = (1..100).map { app("Settings Search $it", "settings$it") }

        AppSearchAlgorithm.findMatches(
            query = "settings search",
            source = apps,
            limit = 10,
            fuzzySearchStrategy = strategy,
            appNicknames = emptyMap(),
            secondaryRankingSignal = SecondaryRankingSignal.NONE,
        )

        assertEquals(3, preferenceReads)
    }

    @Test
    fun firstAndLastCharactersMatchAppNameAndNickname() {
        val obsidian = app("Obsidian", "obsidian")
        val notes = app("Keep", "keep")
        val unrelated = app("Calendar", "calendar")
        val source = listOf(unrelated, obsidian, notes)

        fun matches(query: String) =
            AppSearchAlgorithm.findMatches(
                query = query,
                source = source,
                limit = 10,
                fuzzySearchStrategy =
                    FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG, isFuzzySearchEnabled = { false }),
                appNicknames = mapOf(notes.packageName to "MyNotes"),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertEquals(listOf(obsidian), matches("OBAN"))
        assertEquals(listOf(obsidian), matches("obsian"))
        assertEquals(listOf(notes), matches("MYES"))
        assertEquals(emptyList<AppInfo>(), matches("oan"))
        assertEquals(emptyList<AppInfo>(), matches("obidian"))
    }

    @Test
    fun clonedAppNicknameIsSeparateFromMainApp() {
        val main = app("WhatsApp", "whatsapp")
        val clone = main.copy(userHandleId = 999)
        val source = listOf(main, clone)

        fun matches(query: String) =
            AppSearchAlgorithm.findMatches(
                query = query,
                source = source,
                limit = 10,
                fuzzySearchStrategy =
                    FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG, isFuzzySearchEnabled = { false }),
                appNicknames =
                    mapOf(
                        main.launchCountKey() to "personal",
                        clone.launchCountKey() to "business",
                    ),
                secondaryRankingSignal = SecondaryRankingSignal.NONE,
            )

        assertEquals(listOf(main), matches("personal"))
        assertEquals(listOf(clone), matches("business"))
    }

    @Test
    fun subsequenceQueriesFindAppsWhenFuzzySearchIsOn() {
        val firefox = app("Firefox", "firefox")
        val keepass = app("KeePassDX", "keepassdx")
        val source = listOf(firefox, keepass, app("Gmail", "gmail"))

        fun matches(
            query: String,
            fuzzyEnabled: Boolean = true,
        ) = AppSearchAlgorithm.findMatches(
            query = query,
            source = source,
            limit = 10,
            fuzzySearchStrategy =
                FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG, isFuzzySearchEnabled = { fuzzyEnabled }),
            appNicknames = emptyMap(),
            secondaryRankingSignal = SecondaryRankingSignal.NONE,
        )

        assertEquals(listOf(firefox), matches("ffx"))
        assertEquals(listOf(keepass), matches("kdx"))
        assertEquals(listOf(keepass), matches("kpdx"))
        assertEquals(emptyList<AppInfo>(), matches("ffx", fuzzyEnabled = false))
    }

    @Test
    fun subsequenceMatchesRankAfterDirectMatches() {
        val firefox = app("Firefox", "firefox", launchCount = 100)
        val direct = app("FFX Tools", "ffxtools")

        val matches =
            AppSearchAlgorithm.findMatches(
                query = "ffx",
                source = listOf(firefox, direct),
                limit = 10,
                fuzzySearchStrategy = FuzzyAppSearchStrategy(FuzzySearchConfig.DEFAULT_APP_CONFIG),
                appNicknames = emptyMap(),
                secondaryRankingSignal = SecondaryRankingSignal.MOST_OPENED,
            )

        assertEquals(listOf(direct, firefox), matches)
    }

    private fun app(
        appName: String,
        packageSuffix: String,
        searchAliases: List<String> = emptyList(),
        launchCount: Int = 0,
        lastUsedTime: Long = 0L,
    ): AppInfo =
        AppInfo(
            appName = appName,
            packageName = "com.example.$packageSuffix",
            lastUsedTime = lastUsedTime,
            totalTimeInForeground = 0L,
            launchCount = launchCount,
            firstInstallTime = 0L,
            isSystemApp = false,
            searchAliases = searchAliases,
        )
}
