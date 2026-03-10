package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.tk.quicksearch.search.core.SearchTarget
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.search.data.AppShortcutRepository.AppShortcutCache
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.createSearchTargetShortcutIntent
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.loadHardcodedShortcuts
import com.tk.quicksearch.search.data.AppShortcutRepository.resolveSearchTargetIconBase64
import com.tk.quicksearch.search.data.AppShortcutRepository.resolveSearchTargetShortcutPackageName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import com.tk.quicksearch.search.data.AppShortcutRepository.filterShortcuts
import com.tk.quicksearch.search.data.AppShortcutRepository.loadShortcutsFromSystem
import com.tk.quicksearch.search.data.AppShortcutRepository.mergeAndSortShortcuts
import com.tk.quicksearch.search.data.AppShortcutRepository.parseCustomShortcutFromPickerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppShortcutRepository(
    private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager
    private val shortcutCache = AppShortcutCache(context)

    @Volatile private var inMemoryShortcuts: List<StaticShortcut>? = null

    suspend fun loadCachedShortcuts(): List<StaticShortcut>? {
        inMemoryShortcuts?.let {
            return it
        }
        return withContext(Dispatchers.IO) {
            val cached = shortcutCache.loadCachedShortcuts().orEmpty()
            val custom = shortcutCache.loadCustomShortcuts().orEmpty()
            val merged = mergeAndSortShortcuts(staticShortcuts = cached, customShortcuts = custom, context = context, packageManager = packageManager)
            if (merged.isNotEmpty()) {
                inMemoryShortcuts = merged
                merged
            } else {
                null
            }
        }
    }

    fun clearCache() {
        shortcutCache.clearCache()
        inMemoryShortcuts = null
    }

    suspend fun loadStaticShortcuts(): List<StaticShortcut> =
        withContext(Dispatchers.IO) {
            val systemShortcuts = loadShortcutsFromSystem(context, packageManager)
            shortcutCache.saveShortcuts(systemShortcuts)
            val customShortcuts = shortcutCache.loadCustomShortcuts().orEmpty()
            val merged =
                mergeAndSortShortcuts(
                    staticShortcuts = systemShortcuts,
                    customShortcuts = customShortcuts,
                    context = context,
                    packageManager = packageManager,
                )
            inMemoryShortcuts = merged
            merged
        }

    suspend fun addCustomShortcutFromPickerResult(
        resultData: Intent?,
        sourcePackageName: String? = null,
    ): StaticShortcut? =
        withContext(Dispatchers.IO) {
            val shortcut =
                parseCustomShortcutFromPickerResult(
                    resultData = resultData,
                    context = context,
                    packageManager = packageManager,
                    sourcePackageName = sourcePackageName,
                ) ?: return@withContext null
            val customShortcuts = shortcutCache.loadCustomShortcuts().orEmpty().toMutableList()
            customShortcuts.add(shortcut)
            if (!shortcutCache.saveCustomShortcuts(customShortcuts)) {
                return@withContext null
            }
            inMemoryShortcuts =
                mergeAndSortShortcuts(
                    staticShortcuts = inMemoryShortcuts.orEmpty().filterNot(::isUserCreatedShortcut),
                    customShortcuts = customShortcuts,
                    context = context,
                    packageManager = packageManager,
                )
            shortcut
        }

    suspend fun removeCustomShortcut(shortcut: StaticShortcut): Boolean =
        withContext(Dispatchers.IO) {
            if (!isUserCreatedShortcut(shortcut)) return@withContext false
            val keyToRemove = shortcutKey(shortcut)
            val existing = shortcutCache.loadCustomShortcuts().orEmpty()
            val updated = existing.filterNot { shortcutKey(it) == keyToRemove }
            if (existing.size == updated.size) return@withContext false
            if (!shortcutCache.saveCustomShortcuts(updated)) return@withContext false
            inMemoryShortcuts =
                mergeAndSortShortcuts(
                    staticShortcuts = inMemoryShortcuts.orEmpty().filterNot(::isUserCreatedShortcut),
                    customShortcuts = updated,
                    context = context,
                    packageManager = packageManager,
                )
            true
        }

    suspend fun updateCustomShortcut(
        shortcut: StaticShortcut,
        shortcutName: String,
        shortcutValue: String?,
        iconBase64: String?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isUserCreatedShortcut(shortcut)) return@withContext false
            val normalizedName = shortcutName.trim()
            if (normalizedName.isBlank()) return@withContext false
            val normalizedValue = shortcutValue?.trim()
            if (shortcutValue != null && normalizedValue.isNullOrBlank()) return@withContext false
            val keyToUpdate = shortcutKey(shortcut)
            val existing = shortcutCache.loadCustomShortcuts().orEmpty()
            var updatedAny = false
            var valueUpdateFailed = false
            val updated =
                existing.map { existingShortcut ->
                    if (shortcutKey(existingShortcut) != keyToUpdate) {
                        existingShortcut
                    } else {
                        updatedAny = true
                        val renamedShortcut =
                            existingShortcut.copy(
                            shortLabel = normalizedName,
                            longLabel = normalizedName,
                            iconBase64 = iconBase64?.takeIf { it.isNotBlank() },
                        )
                        if (normalizedValue == null) {
                            renamedShortcut
                        } else {
                            updateConfiguredValue(renamedShortcut, normalizedValue).also { updatedShortcut ->
                                if (updatedShortcut == null) {
                                    valueUpdateFailed = true
                                }
                            } ?: existingShortcut
                        }
                    }
                }
            if (!updatedAny || valueUpdateFailed) return@withContext false
            if (!shortcutCache.saveCustomShortcuts(updated)) return@withContext false
            inMemoryShortcuts =
                mergeAndSortShortcuts(
                    staticShortcuts = inMemoryShortcuts.orEmpty().filterNot(::isUserCreatedShortcut),
                    customShortcuts = updated,
                    context = context,
                    packageManager = packageManager,
                )
            true
        }

    private fun updateConfiguredValue(
        shortcut: StaticShortcut,
        value: String,
    ): StaticShortcut? {
        if (shortcut.id.startsWith(CUSTOM_DEEP_LINK_ID_PREFIX)) {
            return shortcut.copy(intents = createDeepLinkIntents(shortcut.packageName, value))
        }

        val targetIntentIndex =
            shortcut.intents.indexOfFirst {
                it.action == SearchTargetQueryShortcutActivity.ACTION_LAUNCH_SEARCH_TARGET_QUERY_SHORTCUT
            }
        if (targetIntentIndex < 0) return null

        val originalIntent = shortcut.intents[targetIntentIndex]
        val targetType =
            originalIntent.getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_TARGET_TYPE)
                .orEmpty()
        if (targetType.isBlank()) return null

        if (targetType == SearchTargetQueryShortcutActivity.TARGET_TYPE_BROWSER) {
            val mode =
                originalIntent
                    .getStringExtra(SearchTargetQueryShortcutActivity.EXTRA_BROWSER_SHORTCUT_MODE)
                    ?.let { runCatching { SearchTargetShortcutMode.valueOf(it) }.getOrNull() }
                    ?: SearchTargetShortcutMode.AUTO
            if (mode == SearchTargetShortcutMode.FORCE_URL && value.any { it.isWhitespace() }) {
                return null
            }
        }

        val updatedIntents =
            shortcut.intents.mapIndexed { index, intent ->
                if (index == targetIntentIndex) {
                    Intent(intent).apply {
                        putExtra(SearchTargetQueryShortcutActivity.EXTRA_QUERY, value)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(intent)
                }
            }
        return shortcut.copy(intents = updatedIntents)
    }

    private fun createDeepLinkIntents(
        packageName: String,
        deepLink: String,
    ): List<Intent> {
        val appIntent =
            runCatching { Intent.parseUri(deepLink, Intent.URI_INTENT_SCHEME) }
                .getOrElse {
                    Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
                }.apply {
                    if (component?.packageName?.equals(packageName, ignoreCase = true) == false) {
                        component = null
                    }
                    `package` = packageName
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        val fallbackIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return listOf(fallbackIntent, appIntent)
    }

    private companion object {
        const val CUSTOM_DEEP_LINK_ID_PREFIX = "custom_deeplink_"
    }

    suspend fun addSearchTargetQueryShortcut(
        target: SearchTarget,
        shortcutName: String,
        shortcutQuery: String,
        mode: SearchTargetShortcutMode = SearchTargetShortcutMode.AUTO,
    ): StaticShortcut? =
        withContext(Dispatchers.IO) {
            val name = shortcutName.trim()
            val query = shortcutQuery.trim()
            if (name.isBlank() || query.isBlank()) return@withContext null

            val targetPackageName =
                resolveSearchTargetShortcutPackageName(
                    context = context,
                    target = target,
                    packageManager = packageManager,
                )
            val launchIntent =
                createSearchTargetShortcutIntent(
                    context = context,
                    target = target,
                    query = query,
                    mode = mode,
                ) ?: return@withContext null
            val iconBase64 = resolveSearchTargetIconBase64(context, target)
            val shortcutId =
                "custom_query_${System.currentTimeMillis()}_${(Math.random() * 100000).toInt()}"

            val shortcut =
                StaticShortcut(
                    packageName = targetPackageName,
                    appLabel = resolveSearchTargetLabel(context, target),
                    id = shortcutId,
                    shortLabel = name,
                    longLabel = name,
                    iconResId = null,
                    iconBase64 = iconBase64,
                    enabled = true,
                    intents = listOf(launchIntent),
                )

            val customShortcuts = shortcutCache.loadCustomShortcuts().orEmpty().toMutableList()
            customShortcuts.add(shortcut)
            if (!shortcutCache.saveCustomShortcuts(customShortcuts)) {
                return@withContext null
            }

            inMemoryShortcuts =
                mergeAndSortShortcuts(
                    staticShortcuts = inMemoryShortcuts.orEmpty().filterNot(::isUserCreatedShortcut),
                    customShortcuts = customShortcuts,
                    context = context,
                    packageManager = packageManager,
                )
            shortcut
        }

    suspend fun addCustomShortcutForAppActivity(
        packageName: String,
        activityClassName: String,
        activityLabel: String,
    ): StaticShortcut? =
        withContext(Dispatchers.IO) {
            val normalizedPackageName = packageName.trim()
            val normalizedClassName = activityClassName.trim()
            val normalizedLabel = activityLabel.trim()
            if (normalizedPackageName.isBlank() ||
                normalizedClassName.isBlank() ||
                normalizedLabel.isBlank()
            ) {
                return@withContext null
            }

            val resolvedClassName = resolveClassName(normalizedPackageName, normalizedClassName)
            val launchIntent =
                Intent(Intent.ACTION_MAIN).apply {
                    component = android.content.ComponentName(normalizedPackageName, resolvedClassName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val shortcut =
                StaticShortcut(
                    packageName = normalizedPackageName,
                    appLabel = resolveAppLabel(context, normalizedPackageName, packageManager),
                    id =
                        "custom_activity_${System.currentTimeMillis()}_${(Math.random() * 100000).toInt()}",
                    shortLabel = normalizedLabel,
                    longLabel = normalizedLabel,
                    iconResId = null,
                    iconBase64 = loadAppIconBase64(context, normalizedPackageName),
                    enabled = true,
                    intents = listOf(launchIntent),
                )
            val validShortcut = filterShortcuts(listOf(shortcut), packageManager, context).firstOrNull() ?: return@withContext null

            val customShortcuts = shortcutCache.loadCustomShortcuts().orEmpty().toMutableList()
            customShortcuts.add(validShortcut)
            if (!shortcutCache.saveCustomShortcuts(customShortcuts)) {
                return@withContext null
            }

            inMemoryShortcuts =
                mergeAndSortShortcuts(
                    staticShortcuts = inMemoryShortcuts.orEmpty().filterNot(::isUserCreatedShortcut),
                    customShortcuts = customShortcuts,
                    context = context,
                    packageManager = packageManager,
                )
            validShortcut
        }

    suspend fun addCustomShortcutForAppDeepLink(
        packageName: String,
        shortcutName: String,
        deepLink: String,
        iconBase64: String?,
    ): StaticShortcut? =
        withContext(Dispatchers.IO) {
            val normalizedPackageName = packageName.trim()
            val normalizedShortcutName = shortcutName.trim()
            val normalizedDeepLink = deepLink.trim()
            if (normalizedPackageName.isBlank() ||
                normalizedShortcutName.isBlank() ||
                normalizedDeepLink.isBlank() ||
                normalizedDeepLink.any { it.isWhitespace() }
            ) {
                return@withContext null
            }

            val appIntent =
                runCatching { Intent.parseUri(normalizedDeepLink, Intent.URI_INTENT_SCHEME) }
                    .getOrElse {
                        Intent(Intent.ACTION_VIEW, Uri.parse(normalizedDeepLink))
                    }.apply {
                        if (component?.packageName?.equals(normalizedPackageName, ignoreCase = true) == false) {
                            component = null
                        }
                        `package` = normalizedPackageName
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            val fallbackIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(normalizedDeepLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

            val shortcut =
                StaticShortcut(
                    packageName = normalizedPackageName,
                    appLabel = resolveAppLabel(context, normalizedPackageName, packageManager),
                    id =
                        "custom_deeplink_${System.currentTimeMillis()}_${(Math.random() * 100000).toInt()}",
                    shortLabel = normalizedShortcutName,
                    longLabel = normalizedShortcutName,
                    iconResId = null,
                    iconBase64 = iconBase64?.takeIf { it.isNotBlank() } ?: loadAppIconBase64(context, normalizedPackageName),
                    enabled = true,
                    intents = listOf(fallbackIntent, appIntent),
                )
            val validShortcut =
                filterShortcuts(listOf(shortcut), packageManager, context).firstOrNull()
                    ?: return@withContext null

            val customShortcuts = shortcutCache.loadCustomShortcuts().orEmpty().toMutableList()
            customShortcuts.add(validShortcut)
            if (!shortcutCache.saveCustomShortcuts(customShortcuts)) {
                return@withContext null
            }

            inMemoryShortcuts =
                mergeAndSortShortcuts(
                    staticShortcuts = inMemoryShortcuts.orEmpty().filterNot(::isUserCreatedShortcut),
                    customShortcuts = customShortcuts,
                    context = context,
                    packageManager = packageManager,
                )
            validShortcut
        }
}
