package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.tk.quicksearch.search.core.SearchTarget
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
        iconBase64: String?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if (!isUserCreatedShortcut(shortcut)) return@withContext false
            val normalizedName = shortcutName.trim()
            if (normalizedName.isBlank()) return@withContext false
            val keyToUpdate = shortcutKey(shortcut)
            val existing = shortcutCache.loadCustomShortcuts().orEmpty()
            var updatedAny = false
            val updated =
                existing.map { existingShortcut ->
                    if (shortcutKey(existingShortcut) != keyToUpdate) {
                        existingShortcut
                    } else {
                        updatedAny = true
                        existingShortcut.copy(
                            shortLabel = normalizedName,
                            longLabel = normalizedName,
                            iconBase64 = iconBase64?.takeIf { it.isNotBlank() },
                        )
                    }
                }
            if (!updatedAny) return@withContext false
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

    suspend fun addSearchTargetQueryShortcut(
        target: SearchTarget,
        shortcutName: String,
        shortcutQuery: String,
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
            val launchIntent = createSearchTargetShortcutIntent(context, target, query) ?: return@withContext null
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
}