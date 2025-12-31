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
import java.util.concurrent.ConcurrentHashMap

/**
 * Discovers installed icon packs and loads icons from their appfilter definitions.
 */
object IconPackManager {
    private const val TAG = "IconPackManager"

    // Common intents/categories used by popular icon packs (Nova, Lawnchair, etc.)
    private val ICON_PACK_ACTIONS = listOf(
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "org.adw.launcher.THEMES",
        "org.adw.launcher.icons.ACTION_PICK_ICON"
    )
    private val ICON_PACK_CATEGORIES = listOf(
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.teslacoilsw.launcher.THEME"
    )

    private val mappingCache = ConcurrentHashMap<String, Map<String, String>>()
    private val resourcesCache = ConcurrentHashMap<String, Resources>()

    /**
     * Returns a sorted list of installed icon packs.
     */
    fun findInstalledIconPacks(context: Context): List<IconPackInfo> {
        val packageManager = context.packageManager
        val packages = discoverIconPackPackages(packageManager)
        return packages.mapNotNull { packageName ->
            createIconPackInfo(packageManager, packageName)
        }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    /**
     * Discovers packages that contain icon packs by querying known intent actions and categories,
     * with a fallback to scanning for appfilter.xml files.
     */
    private fun discoverIconPackPackages(packageManager: PackageManager): Set<String> {
        val packages = mutableSetOf<String>()

        ICON_PACK_ACTIONS.forEach { action ->
            val intent = Intent(action)
            packages.addAll(queryPackages(packageManager, intent))
        }

        ICON_PACK_CATEGORIES.forEach { category ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            packages.addAll(queryPackages(packageManager, intent))
        }

        // Fallback: scan for appfilter.xml on installed apps if nothing matched the known intents.
        if (packages.isEmpty()) {
            packages.addAll(findAppFilterCandidates(packageManager))
        }

        return packages
    }

    /**
     * Creates an IconPackInfo object for the given package, or null if the package cannot be resolved.
     */
    private fun createIconPackInfo(packageManager: PackageManager, packageName: String): IconPackInfo? {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(appInfo)?.toString().orEmpty()
            IconPackInfo(
                packageName = packageName,
                label = label.ifBlank { packageName }
            )
        }.getOrNull()
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
        mappingCache.remove(iconPackPackage)
        resourcesCache.remove(iconPackPackage)
    }

    fun clearAllCaches() {
        mappingCache.clear()
        resourcesCache.clear()
    }

    /**
     * Queries the package manager for activities that match the given intent,
     * returning a set of package names.
     */
    private fun queryPackages(
        packageManager: PackageManager,
        intent: Intent
    ): Set<String> {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }.mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query icon packs for intent $intent", e)
            emptySet()
        }
    }

    /**
     * Finds installed applications that contain appfilter.xml files,
     * which typically indicate icon pack applications.
     */
    private fun findAppFilterCandidates(packageManager: PackageManager): Set<String> {
        return runCatching {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { appInfo ->
                    val hasAppFilter = runCatching {
                        val res = packageManager.getResourcesForApplication(appInfo.packageName)
                        hasAppFilter(res, appInfo.packageName)
                    }.getOrDefault(false)
                    if (hasAppFilter) appInfo.packageName else null
                }
                .toSet()
        }.getOrDefault(emptySet())
    }

    /**
     * Checks if the given package contains an appfilter.xml file,
     * either in assets or as a resource.
     */
    private fun hasAppFilter(resources: Resources, packageName: String): Boolean {
        return try {
            resources.assets.open("appfilter.xml").close()
            true
        } catch (_: Exception) {
            val resId = resources.getIdentifier("appfilter", "xml", packageName)
            resId != 0
        }
    }

    /**
     * Retrieves the Resources object for the specified icon pack package,
     * using cached values when available.
     */
    private fun getIconPackResources(
        context: Context,
        iconPackPackage: String
    ): Resources? {
        resourcesCache[iconPackPackage]?.let { return it }

        val resources = runCatching {
            val packageManager = context.packageManager
            packageManager.getResourcesForApplication(iconPackPackage)
        }.onFailure {
            Log.w(TAG, "Unable to load resources for icon pack $iconPackPackage", it)
        }.getOrNull() ?: return null

        resourcesCache[iconPackPackage] = resources
        return resources
    }

    /**
     * Loads and caches the appfilter mapping from the icon pack's XML file,
     * returning a map of package names to drawable names.
     */
    private fun loadAppFilterMapping(
        context: Context,
        iconPackPackage: String,
        resources: Resources
    ): Map<String, String> {
        mappingCache[iconPackPackage]?.let { return it }

        val mapping = parseAppFilter(resources, iconPackPackage)
        mappingCache[iconPackPackage] = mapping
        return mapping
    }

    /**
     * Parses the appfilter.xml file from the icon pack, trying assets first, then resources.
     * Returns a mapping of package names to drawable names.
     */
    private fun parseAppFilter(resources: Resources, packageName: String): Map<String, String> {
        // Try assets/appfilter.xml first
        getAppFilterFromAssets(resources)?.let { return it }

        // Fallback to res/xml/appfilter.xml
        return getAppFilterFromResources(resources, packageName)
    }

    /**
     * Attempts to parse appfilter.xml from the assets directory.
     */
    private fun getAppFilterFromAssets(resources: Resources): Map<String, String>? {
        return runCatching { resources.assets.open("appfilter.xml") }.getOrNull()
            ?.use { stream -> parseIconPackXml(stream) }
    }

    /**
     * Attempts to parse appfilter.xml from the res/xml directory.
     */
    private fun getAppFilterFromResources(resources: Resources, packageName: String): Map<String, String> {
        val xmlResId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlResId == 0) return emptyMap()

        return try {
            val parser = resources.getXml(xmlResId)
            parseIconPackXml(parser) ?: emptyMap()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse appfilter.xml from resources for $packageName", e)
            emptyMap()
        }
    }

    /**
     * Parses an InputStream containing icon pack XML data.
     */
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

    /**
     * Parses XML using the provided XmlPullParser, extracting package-to-drawable mappings
     * from "item" elements with "component" and "drawable" attributes.
     */
    private fun parseIconPackXml(parser: XmlPullParser): Map<String, String>? {
        val mapping = mutableMapOf<String, String>()

        return try {
            var xmlEventType = parser.eventType
            while (xmlEventType != XmlPullParser.END_DOCUMENT) {
                if (xmlEventType == XmlPullParser.START_TAG && parser.name == "item") {
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
                xmlEventType = parser.next()
            }
            mapping
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing icon pack XML", e)
            null
        }
    }

    /**
     * Extracts the package name from an Android component string like "ComponentInfo{com.package/.Activity}".
     */
    private fun extractPackageFromComponent(component: String?): String? {
        if (component.isNullOrBlank()) return null
        // ComponentInfo{com.package/.Activity}
        val braceStart = component.indexOf('{')
        val slashIndex = component.indexOf('/', startIndex = braceStart + 1)
        val braceEnd = component.indexOf('}', startIndex = slashIndex + 1)

        if (braceStart != -1 && slashIndex != -1 && (braceEnd == -1 || braceEnd > slashIndex)) {
            return component.substring(braceStart + 1, slashIndex).takeIf { it.isNotBlank() }
        }
        return null
    }

    /**
     * Loads a drawable from the icon pack resources, trying drawable/ and mipmap/ directories.
     */
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

