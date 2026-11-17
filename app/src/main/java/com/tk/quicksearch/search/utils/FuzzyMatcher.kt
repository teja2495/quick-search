package com.tk.quicksearch.search.utils

/**
 * Fuzzy matcher optimized for search-time use.
 *
 * Expected input is pre-normalized text (lowercase, diacritics removed) to avoid
 * repeatedly doing normalization work inside tight matching loops.
 */
object FuzzyMatcher {
    private const val LONG_QUERY_MIN_LENGTH = 6

    data class ScoredMatch<T>(
        val item: T,
        val score: Int,
    )

    /**
     * Computes a token-aware fuzzy score from 0..100.
     */
    fun score(
        query: String,
        target: String,
        maxEditDistance: Int? = null,
    ): Int {
        val trimmedQuery = query.trim()
        val trimmedTarget = target.trim()
        if (trimmedQuery.isEmpty() || trimmedTarget.isEmpty()) return 0
        if (trimmedQuery == trimmedTarget) return 100
        return scoreTrimmed(trimmedQuery, tokenizeUnique(trimmedQuery), trimmedTarget, maxEditDistance)
    }

    /**
     * Computes the best score between primary and optional secondary target text.
     */
    fun score(
        query: String,
        primaryTarget: String,
        secondaryTarget: String?,
        maxEditDistance: Int? = null,
    ): Int {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return 0
        // Tokenize the query at most once for both targets.
        var queryTokens: Tokens? = null
        var bestScore = 0
        for (target in arrayOf(primaryTarget, secondaryTarget)) {
            if (target == null || (target !== primaryTarget && target.isBlank())) continue
            val trimmedTarget = target.trim()
            val targetScore =
                when {
                    trimmedTarget.isEmpty() -> 0
                    trimmedQuery == trimmedTarget -> 100
                    else -> {
                        val tokens = queryTokens ?: tokenizeUnique(trimmedQuery).also { queryTokens = it }
                        scoreTrimmed(trimmedQuery, tokens, trimmedTarget, maxEditDistance)
                    }
                }
            bestScore = maxOf(bestScore, targetScore)
        }
        return bestScore
    }

    /** Scores already-trimmed, non-empty, unequal texts, tokenizing each side only once. */
    private fun scoreTrimmed(
        trimmedQuery: String,
        queryTokens: Tokens,
        trimmedTarget: String,
        maxEditDistance: Int?,
    ): Int {
        val targetTokens = tokenizeUnique(trimmedTarget)
        if (maxEditDistance != null &&
            !hasTokenWithinEditDistance(
                queryTokens = queryTokens.ordered,
                targetTokens = targetTokens.ordered,
                maxDistance = maxEditDistance,
            )
        ) {
            return 0
        }
        return tokenSetRatio(trimmedQuery, queryTokens, trimmedTarget, targetTokens)
    }

    /**
     * Generic fuzzy search helper reusable across feature areas.
     */
    fun <T> search(
        query: String,
        candidates: List<T>,
        minScore: Int = 0,
        limit: Int = Int.MAX_VALUE,
        candidateLimit: Int = Int.MAX_VALUE,
        maxEditDistance: Int? = null,
        primaryTextSelector: (T) -> String,
        secondaryTextSelector: ((T) -> String?)? = null,
    ): List<ScoredMatch<T>> {
        if (query.isBlank() || candidates.isEmpty()) return emptyList()

        val cappedCandidateCount = minOf(candidates.size, candidateLimit.coerceAtLeast(0))
        if (cappedCandidateCount == 0) return emptyList()

        val matches = ArrayList<ScoredMatch<T>>(minOf(cappedCandidateCount, 32))
        for (index in 0 until cappedCandidateCount) {
            val candidate = candidates[index]
            val score =
                if (secondaryTextSelector != null) {
                    score(
                        query = query,
                        primaryTarget = primaryTextSelector(candidate),
                        secondaryTarget = secondaryTextSelector(candidate),
                        maxEditDistance = maxEditDistance,
                    )
                } else {
                    score(
                        query = query,
                        target = primaryTextSelector(candidate),
                        maxEditDistance = maxEditDistance,
                    )
                }

            if (score >= minScore) {
                matches.add(ScoredMatch(item = candidate, score = score))
            }
        }

        if (matches.size <= 1) return matches
        matches.sortByDescending { it.score }
        return if (limit >= matches.size) {
            matches
        } else {
            matches.subList(0, limit).toList()
        }
    }

    fun maxEditDistanceForQuery(
        query: String,
        mediumQueryMaxDistance: Int = 1,
        longQueryMaxDistance: Int = 2,
        longQueryMinLength: Int = LONG_QUERY_MIN_LENGTH,
    ): Int {
        return if (query.trim().length >= longQueryMinLength) {
            longQueryMaxDistance
        } else {
            mediumQueryMaxDistance
        }.coerceAtLeast(0)
    }

    private data class Tokens(
        val ordered: List<String>,
        val tokenSet: Set<String>,
    )

    private fun tokenSetRatio(
        query: String,
        queryTokens: Tokens,
        target: String,
        targetTokens: Tokens,
    ): Int {
        if (queryTokens.ordered.isEmpty() || targetTokens.ordered.isEmpty()) {
            return sequenceRatio(query, target)
        }

        val commonTokens = ArrayList<String>(minOf(queryTokens.ordered.size, targetTokens.ordered.size))
        val queryRemainder = ArrayList<String>(queryTokens.ordered.size)
        for (token in queryTokens.ordered) {
            if (targetTokens.tokenSet.contains(token)) {
                commonTokens.add(token)
            } else {
                queryRemainder.add(token)
            }
        }

        if (commonTokens.isEmpty()) {
            val fullScore = sequenceRatio(query, target)
            val tokenScore = tokenWiseSimilarity(queryTokens.ordered, targetTokens.ordered)
            return maxOf(fullScore, tokenScore)
        }

        val targetRemainder = ArrayList<String>(targetTokens.ordered.size)
        for (token in targetTokens.ordered) {
            if (!queryTokens.tokenSet.contains(token)) {
                targetRemainder.add(token)
            }
        }

        val common = joinTokens(commonTokens)
        val queryCombined = joinTokens(commonTokens, queryRemainder)
        val targetCombined = joinTokens(commonTokens, targetRemainder)

        val partialA = sequenceRatio(common, queryCombined)
        val partialB = sequenceRatio(common, targetCombined)
        val betweenCombined = sequenceRatio(queryCombined, targetCombined)
        return maxOf(partialA, partialB, betweenCombined)
    }

    private fun tokenWiseSimilarity(
        queryTokens: List<String>,
        targetTokens: List<String>,
    ): Int {
        if (queryTokens.isEmpty() || targetTokens.isEmpty()) return 0

        var total = 0
        for (queryToken in queryTokens) {
            var best = 0
            for (targetToken in targetTokens) {
                val score =
                    if (queryToken == targetToken) {
                        100
                    } else {
                        sequenceRatio(queryToken, targetToken)
                    }

                if (score > best) {
                    best = score
                    if (best == 100) break
                }
            }
            total += best
        }

        return total / queryTokens.size
    }

    private fun tokenizeUnique(text: String): Tokens {
        if (text.isBlank()) return Tokens(emptyList(), emptySet())

        val tokens = LinkedHashSet<String>(8)
        var tokenStart = -1
        for (index in text.indices) {
            val ch = text[index]
            if (ch.isLetterOrDigit()) {
                if (tokenStart == -1) tokenStart = index
            } else if (tokenStart != -1) {
                tokens.add(text.substring(tokenStart, index))
                tokenStart = -1
            }
        }
        if (tokenStart != -1) {
            tokens.add(text.substring(tokenStart))
        }

        if (tokens.isEmpty()) return Tokens(emptyList(), emptySet())
        val ordered = ArrayList(tokens)
        return Tokens(ordered = ordered, tokenSet = tokens)
    }

    private fun joinTokens(
        leadingTokens: List<String>,
        trailingTokens: List<String> = emptyList(),
    ): String {
        if (leadingTokens.isEmpty() && trailingTokens.isEmpty()) return ""

        var totalChars = 0
        for (token in leadingTokens) totalChars += token.length
        for (token in trailingTokens) totalChars += token.length
        val totalTokens = leadingTokens.size + trailingTokens.size
        val separatorCount = (totalTokens - 1).coerceAtLeast(0)

        return buildString(totalChars + separatorCount) {
            var first = true
            for (token in leadingTokens) {
                if (!first) append(' ')
                append(token)
                first = false
            }
            for (token in trailingTokens) {
                if (!first) append(' ')
                append(token)
                first = false
            }
        }
    }

    private fun sequenceRatio(
        first: String,
        second: String,
    ): Int {
        if (first == second) return 100
        if (first.isEmpty() || second.isEmpty()) return 0

        val distance = levenshteinDistance(first, second)
        val lengthSum = first.length + second.length
        val similarity = (lengthSum - distance).coerceAtLeast(0)
        return (similarity * 100) / lengthSum
    }

    /**
     * Returns true if any (query token, target token) pair has Levenshtein distance ≤ maxDistance.
     * Use this to enforce a typo cap (e.g. max 2 character edits) on top of fuzzy scoring.
     */
    fun hasTokenWithinEditDistance(
        query: String,
        target: String,
        maxDistance: Int,
    ): Boolean {
        if (query.isBlank() || target.isBlank()) return false
        return hasTokenWithinEditDistance(
            tokenizeUnique(query).ordered,
            tokenizeUnique(target).ordered,
            maxDistance,
        )
    }

    /** The tokens [hasTokenWithinEditDistance] compares, for callers that reuse them across checks. */
    fun editDistanceTokens(text: String): List<String> = tokenizeUnique(text).ordered

    /** [hasTokenWithinEditDistance] over tokens already produced by [editDistanceTokens]. */
    fun hasTokenWithinEditDistance(
        queryTokens: List<String>,
        targetTokens: List<String>,
        maxDistance: Int,
    ): Boolean {
        if (queryTokens.isEmpty() || targetTokens.isEmpty()) return false
        for (queryToken in queryTokens) {
            for (targetToken in targetTokens) {
                if (isWithinEditDistance(queryToken, targetToken, targetToken.length, maxDistance)) return true
            }
        }
        return false
    }

    /**
     * Returns whether the Levenshtein distance between [first] and the first [secondLength]
     * characters of [second] is at most [maxDistance]. Same answer as a full distance computation,
     * but it stops as soon as the bound can no longer be met and reuses per-thread scratch rows.
     */
    internal fun isWithinEditDistance(
        first: String,
        second: String,
        secondLength: Int,
        maxDistance: Int,
    ): Boolean {
        if (maxDistance < 0) return false
        val firstLength = first.length
        if (kotlin.math.abs(firstLength - secondLength) > maxDistance) return false
        if (firstLength == 0 || secondLength == 0) return true
        if (firstLength == secondLength && first.regionMatches(0, second, 0, secondLength)) return true

        val rows = requireNotNull(scratchRows.get())
        var previous = rows.ensure(0, secondLength + 1)
        var current = rows.ensure(1, secondLength + 1)
        for (index in 0..secondLength) previous[index] = index

        for (firstIndex in 1..firstLength) {
            current[0] = firstIndex
            var rowMin = firstIndex
            val firstChar = first[firstIndex - 1]
            for (secondIndex in 1..secondLength) {
                val substitutionCost = if (second[secondIndex - 1] == firstChar) 0 else 1
                val value =
                    minOf(
                        current[secondIndex - 1] + 1,
                        previous[secondIndex] + 1,
                        previous[secondIndex - 1] + substitutionCost,
                    )
                current[secondIndex] = value
                if (value < rowMin) rowMin = value
            }
            // Row minimums never decrease, so once every cell exceeds the bound the result can't recover.
            if (rowMin > maxDistance) return false
            val swap = previous
            previous = current
            current = swap
        }
        return previous[secondLength] <= maxDistance
    }

    private class ScratchRows {
        private val rows = arrayOf(IntArray(32), IntArray(32))

        fun ensure(
            index: Int,
            size: Int,
        ): IntArray {
            if (rows[index].size < size) rows[index] = IntArray(size)
            return rows[index]
        }
    }

    private val scratchRows = ThreadLocal.withInitial(::ScratchRows)

    private fun levenshteinDistance(
        first: String,
        second: String,
    ): Int {
        if (first == second) return 0
        if (first.isEmpty()) return second.length
        if (second.isEmpty()) return first.length

        var shorter = first
        var longer = second
        if (shorter.length > longer.length) {
            shorter = second
            longer = first
        }

        val shorterLength = shorter.length
        var previous = IntArray(shorterLength + 1) { it }
        var current = IntArray(shorterLength + 1)

        for (longIndex in 1..longer.length) {
            current[0] = longIndex
            val longChar = longer[longIndex - 1]

            for (shortIndex in 1..shorterLength) {
                val substitutionCost = if (shorter[shortIndex - 1] == longChar) 0 else 1
                val insertion = current[shortIndex - 1] + 1
                val deletion = previous[shortIndex] + 1
                val substitution = previous[shortIndex - 1] + substitutionCost

                current[shortIndex] = minOf(insertion, deletion, substitution)
            }

            val swap = previous
            previous = current
            current = swap
        }

        return previous[shorterLength]
    }
}
