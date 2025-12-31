package com.tk.quicksearch.search.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.Xml
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.core.IconPackInfo
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.Locale

/**
 * Discovers installed icon packs and loads icons from their appfilter definitions.
 */
object IconPackManager {
    private const val TAG = "IconPackManager"

    // Common intents/categories used by popular icon packs (Nova, Lawnchair, etc.)
    private val iconPackActions = listOf(
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "org.adw.launcher.THEMES",
        "org.adw.launcher.icons.ACTION_PICK_ICON"
    )
    private val iconPackCategories = listOf(
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME"
    )

    private val mappingCache = mutableMapOf<String, Map<String, String>>()
    private val resourcesCache = mutableMapOf<String, Resources>()

    /**
     * Returns a sorted list of installed icon packs.
     */
    fun findInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val packages = mutableSetOf<String>()

        iconPackActions.forEach { action ->
            val intent = Intent(action)
            packages.addAll(queryPackages(pm, intent))
        }

        iconPackCategories.forEach { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            packages.addAll(queryPackages(pm, intent))
        }

        // Fallback: scan for appfilter.xml on installed apps if nothing matched the known intents.
        if (packages.isEmpty()) {
            packages.addAll(findAppFilterCandidates(pm))
        }

        return packages.mapNotNull { packageName ->
            runCatching {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo)?.toString().orEmpty()
                IconPackInfo(
                    packageName = packageName,
                    label = label.ifBlank { packageName }
                )
            }.getOrNull()
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    /**
     * Loads an app icon bitmap from the specified icon pack, or null if unavailable.
     * Caller must invoke this on a background thread.
     */
    fun loadIconBitmap(
        context: Context,
        iconPackPackage: String,
        targetPackage: String
    ): ImageBitmap? {
        val resources = getIconPackResources(context, iconPackPackage) ?: return null
        val mapping = loadAppFilterMapping(context, iconPackPackage, resources)
        val drawableName = mapping[targetPackage] ?: return null

        val drawable = loadDrawable(resources, iconPackPackage, drawableName) ?: return null
        return drawable.toBitmapSafely()
    }

    fun clearCachesFor(iconPackPackage: String?) {
        if (iconPackPackage.isNullOrBlank()) return
        synchronized(mappingCache) { mappingCache.remove(iconPackPackage) }
        synchronized(resourcesCache) { resourcesCache.remove(iconPackPackage) }
    }

    fun clearAllCaches() {
        synchronized(mappingCache) { mappingCache.clear() }
        synchronized(resourcesCache) { resourcesCache.clear() }
    }

    private fun queryPackages(
        pm: PackageManager,
        intent: Intent
    ): Set<String> {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }.mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query icon packs for intent $intent", e)
            emptySet()
        }
    }

    private fun findAppFilterCandidates(pm: PackageManager): Set<String> {
        return runCatching {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { appInfo ->
                    val hasAppFilter = runCatching {
                        val res = pm.getResourcesForApplication(appInfo.packageName)
                        hasAppFilter(res, appInfo.packageName)
                    }.getOrDefault(false)
                    if (hasAppFilter) appInfo.packageName else null
                }
                .toSet()
        }.getOrDefault(emptySet())
    }

    private fun hasAppFilter(resources: Resources, packageName: String): Boolean {
        return try {
            resources.assets.open("appfilter.xml").close()
            true
        } catch (_: Exception) {
            val resId = resources.getIdentifier("appfilter", "xml", packageName)
            resId != 0
        }
    }

    private fun getIconPackResources(
        context: Context,
        iconPackPackage: String
    ): Resources? {
        synchronized(resourcesCache) {
            resourcesCache[iconPackPackage]?.let { return it }
        }

        val resources = runCatching {
            context.packageManager.getResourcesForApplication(iconPackPackage)
        }.onFailure {
            Log.w(TAG, "Unable to load resources for icon pack $iconPackPackage", it)
        }.getOrNull() ?: return null

        synchronized(resourcesCache) {
            resourcesCache[iconPackPackage] = resources
        }
        return resources
    }

    private fun loadAppFilterMapping(
        context: Context,
        iconPackPackage: String,
        resources: Resources
    ): Map<String, String> {
        synchronized(mappingCache) {
            mappingCache[iconPackPackage]?.let { return it }
        }

        val mapping = parseAppFilter(resources, iconPackPackage)
        synchronized(mappingCache) {
            mappingCache[iconPackPackage] = mapping
        }
        return mapping
    }

    private fun parseAppFilter(resources: Resources, packageName: String): Map<String, String> {
        // Try assets/appfilter.xml first
        val assetStream = runCatching { resources.assets.open("appfilter.xml") }.getOrNull()
        assetStream?.use { stream ->
            parseIconPackXml(stream)?.let { return it }
        }

        // Fallback to res/xml/appfilter.xml
        val xmlResId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlResId != 0) {
            return try {
                val parser = resources.getXml(xmlResId)
                parseIconPackXml(parser) ?: emptyMap()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse appfilter.xml from resources for $packageName", e)
                emptyMap()
            }
        }

        return emptyMap()
    }

    private fun parseIconPackXml(stream: InputStream): Map<String, String>? {
        return try {
            val parser = Xml.newPullParser()
            parser.setInput(stream, null)
            parseIconPackXml(parser)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse appfilter.xml from assets", e)
            null
        }
    }

    private fun parseIconPackXml(parser: XmlPullParser): Map<String, String>? {
        val mapping = mutableMapOf<String, String>()

        return try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawableName = parser.getAttributeValue(null, "drawable")
                    val packageAttr = parser.getAttributeValue(null, "package")

                    val packageName = when {
                        !packageAttr.isNullOrBlank() -> packageAttr
                        else -> extractPackageFromComponent(component)
                    }

                    if (!packageName.isNullOrBlank() && !drawableName.isNullOrBlank()) {
                        // Keep the first mapping we see for a package to avoid overriding with aliases.
                        mapping.putIfAbsent(packageName, drawableName)
                    }
                }
                eventType = parser.next()
            }
            mapping
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing icon pack XML", e)
            null
        }
    }

    private fun extractPackageFromComponent(component: String?): String? {
        if (component.isNullOrBlank()) return null
        // ComponentInfo{com.package/.Activity}
        val start = component.indexOf('{')
        val slash = component.indexOf('/', startIndex = start + 1)
        val end = component.indexOf('}', startIndex = slash + 1)

        if (start != -1 && slash != -1 && (end == -1 || end > slash)) {
            return component.substring(start + 1, slash).takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun loadDrawable(
        resources: Resources,
        packageName: String,
        drawableName: String
    ): Drawable? {
        val drawableId = resources.getIdentifier(drawableName, "drawable", packageName)
        val mipmapId = resources.getIdentifier(drawableName, "mipmap", packageName)
        val resId = when {
            drawableId != 0 -> drawableId
            mipmapId != 0 -> mipmapId
            else -> 0
        }

        if (resId == 0) return null

        return runCatching {
            ResourcesCompat.getDrawable(resources, resId, null)
        }.onFailure {
            Log.w(TAG, "Unable to load drawable $drawableName from $packageName", it)
        }.getOrNull()
    }
}

private fun Drawable.toBitmapSafely(): ImageBitmap? {
    return runCatching {
        when (this) {
            is BitmapDrawable -> bitmap?.asImageBitmap()
            is AdaptiveIconDrawable -> toBitmap().asImageBitmap()
            else -> toBitmap().asImageBitmap()
        }
    }.getOrNull()
}

