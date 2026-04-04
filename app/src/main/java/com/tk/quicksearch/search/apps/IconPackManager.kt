package com.tk.quicksearch.search.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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
import com.tk.quicksearch.search.data.UserAppPreferences
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Discovers installed icon packs and loads icons from their appfilter definitions.
 */
object IconPackManager {
    private const val TAG = "IconPackManager"
    private const val DEFAULT_MASK_SCALE_FACTOR = 1f
    private const val MIN_MASK_SCALE_FACTOR = 0.4f
    private const val MAX_MASK_SCALE_FACTOR = 1.6f
    private const val DEFAULT_FALLBACK_ICON_SIZE = 192
    private const val MAX_FALLBACK_ICON_SIZE = 512

    private data class IconPackRenderData(
        val packageMapping: Map<String, String>,
        val backDrawables: List<String> = emptyList(),
        val maskDrawable: String? = null,
        val overlayDrawable: String? = null,
        val scaleFactor: Float = DEFAULT_MASK_SCALE_FACTOR,
    )

    // Common intents/categories used by popular icon packs (Nova, Lawnchair, etc.)
    private val ICON_PACK_ACTIONS =
        listOf(
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
        )
    private val ICON_PACK_CATEGORIES =
        listOf(
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
        )

    private val renderDataCache = ConcurrentHashMap<String, IconPackRenderData>()
    private val resourcesCache = ConcurrentHashMap<String, Resources>()

    /**
     * Returns a sorted list of installed icon packs.
     */
    fun findInstalledIconPacks(context: Context): List<IconPackInfo> {
        val packageManager = context.packageManager
        val packages = discoverIconPackPackages(packageManager)
        return packages
            .mapNotNull { packageName ->
                createIconPackInfo(packageManager, packageName)
            }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    /**
     * Discovers packages that contain icon packs by querying known intent actions and categories,
     * then excluding home launchers (e.g. Nova, Lawnchair) that also declare THEME intents.
     *
     * Avoid scanning all installed APK resources as a fallback. Opening arbitrary APK assets can
     * trigger native allocation failures on some devices/firmware.
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

        packages.removeAll(getHomeLauncherPackages(packageManager))
        return packages
    }

    private fun getHomeLauncherPackages(packageManager: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return queryPackages(packageManager, intent)
    }

    /**
     * Creates an IconPackInfo object for the given package, or null if the package cannot be resolved.
     */
    private fun createIconPackInfo(
        packageManager: PackageManager,
        packageName: String,
    ): IconPackInfo? =
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(appInfo)?.toString().orEmpty()
            IconPackInfo(
                packageName = packageName,
                label = label.ifBlank { packageName },
            )
        }.getOrNull()

    /**
     * Loads an app icon bitmap from the specified icon pack, or null if unavailable.
     * Caller must invoke this on a background thread.
     */
    fun loadIconBitmap(
        context: Context,
        iconPackPackage: String,
        targetPackage: String,
    ): ImageBitmap? {
        val resources = getIconPackResources(context, iconPackPackage) ?: return null
        val renderData = loadAppFilterRenderData(iconPackPackage, resources)
        val drawableName = renderData.packageMapping[targetPackage]

        if (!drawableName.isNullOrBlank()) {
            val drawable = loadDrawable(resources, iconPackPackage, drawableName) ?: return null
            return drawable.toBitmapSafely()
        }

        val shouldMaskUnsupportedIcons =
            UserAppPreferences(context).isIconPackUnsupportedIconMaskEnabled()
        if (!shouldMaskUnsupportedIcons) return null

        return buildMaskedFallbackIcon(
            context = context,
            resources = resources,
            iconPackPackage = iconPackPackage,
            targetPackage = targetPackage,
            renderData = renderData,
        )
    }

    fun clearAllCaches() {
        renderDataCache.clear()
        resourcesCache.clear()
    }

    /**
     * Queries the package manager for activities that match the given intent,
     * returning a set of package names.
     */
    private fun queryPackages(
        packageManager: PackageManager,
        intent: Intent,
    ): Set<String> =
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }.mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query icon packs for intent $intent", e)
            emptySet()
        }

    /**
     * Retrieves the Resources object for the specified icon pack package,
     * using cached values when available.
     */
    private fun getIconPackResources(
        context: Context,
        iconPackPackage: String,
    ): Resources? {
        resourcesCache[iconPackPackage]?.let { return it }

        val resources =
            runCatching {
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
    private fun loadAppFilterRenderData(
        iconPackPackage: String,
        resources: Resources,
    ): IconPackRenderData {
        renderDataCache[iconPackPackage]?.let { return it }

        val renderData = parseAppFilter(resources, iconPackPackage)
        renderDataCache[iconPackPackage] = renderData
        return renderData
    }

    /**
     * Parses the appfilter.xml file from the icon pack, trying assets first, then resources.
     * Returns a mapping of package names to drawable names.
     */
    private fun parseAppFilter(
        resources: Resources,
        packageName: String,
    ): IconPackRenderData {
        // Try assets/appfilter.xml first
        getAppFilterFromAssets(resources)?.let { return it }

        // Fallback to res/xml/appfilter.xml
        return getAppFilterFromResources(resources, packageName)
    }

    /**
     * Attempts to parse appfilter.xml from the assets directory.
     */
    private fun getAppFilterFromAssets(resources: Resources): IconPackRenderData? =
        runCatching { resources.assets.open("appfilter.xml") }
            .getOrNull()
            ?.use { stream -> parseIconPackXml(stream) }

    /**
     * Attempts to parse appfilter.xml from the res/xml directory.
     */
    private fun getAppFilterFromResources(
        resources: Resources,
        packageName: String,
    ): IconPackRenderData {
        val xmlResId = resources.getIdentifier("appfilter", "xml", packageName)
        if (xmlResId == 0) return IconPackRenderData(emptyMap())

        return try {
            val parser = resources.getXml(xmlResId)
            parseIconPackXml(parser) ?: IconPackRenderData(emptyMap())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse appfilter.xml from resources for $packageName", e)
            IconPackRenderData(emptyMap())
        }
    }

    /**
     * Parses an InputStream containing icon pack XML data.
     */
    private fun parseIconPackXml(stream: InputStream): IconPackRenderData? =
        try {
            val parser = Xml.newPullParser()
            parser.setInput(stream, null)
            parseIconPackXml(parser)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse appfilter.xml from assets", e)
            null
        }

    /**
     * Parses XML using the provided XmlPullParser, extracting package-to-drawable mappings
     * from "item" elements with "component" and "drawable" attributes.
     */
    private fun parseIconPackXml(parser: XmlPullParser): IconPackRenderData? {
        val mapping = mutableMapOf<String, String>()
        val backDrawables = mutableListOf<String>()
        var maskDrawable: String? = null
        var overlayDrawable: String? = null
        var scaleFactor = DEFAULT_MASK_SCALE_FACTOR

        return try {
            var xmlEventType = parser.eventType
            while (xmlEventType != XmlPullParser.END_DOCUMENT) {
                if (xmlEventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> {
                            val component = parser.getAttributeValue(null, "component")
                            val drawableName = parser.getAttributeValue(null, "drawable")
                            val packageAttr = parser.getAttributeValue(null, "package")

                            val packageName =
                                when {
                                    !packageAttr.isNullOrBlank() -> packageAttr
                                    else -> extractPackageFromComponent(component)
                                }

                            if (!packageName.isNullOrBlank() && !drawableName.isNullOrBlank()) {
                                // Keep the first mapping we see for a package to avoid overriding with aliases.
                                mapping.putIfAbsent(packageName, drawableName)
                            }
                        }
                        "iconback" -> {
                            backDrawables += extractImageAttributes(parser)
                        }
                        "iconmask" -> {
                            if (maskDrawable.isNullOrBlank()) {
                                maskDrawable = extractImageAttributes(parser).firstOrNull()
                            }
                        }
                        "iconupon" -> {
                            if (overlayDrawable.isNullOrBlank()) {
                                overlayDrawable = extractImageAttributes(parser).firstOrNull()
                            }
                        }
                        "scale" -> {
                            val parsedScale = parser.getAttributeValue(null, "factor")?.toFloatOrNull()
                            if (parsedScale != null && parsedScale > 0f) {
                                scaleFactor = parsedScale.coerceIn(MIN_MASK_SCALE_FACTOR, MAX_MASK_SCALE_FACTOR)
                            }
                        }
                    }
                }
                xmlEventType = parser.next()
            }
            IconPackRenderData(
                packageMapping = mapping,
                backDrawables = backDrawables.distinct(),
                maskDrawable = maskDrawable,
                overlayDrawable = overlayDrawable,
                scaleFactor = scaleFactor,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing icon pack XML", e)
            null
        }
    }

    private fun extractImageAttributes(parser: XmlPullParser): List<String> =
        buildList {
            for (index in 0 until parser.attributeCount) {
                val name = parser.getAttributeName(index) ?: continue
                if (name.startsWith("img", ignoreCase = true) || name.equals("drawable", ignoreCase = true)) {
                    val value = parser.getAttributeValue(index)
                    if (!value.isNullOrBlank()) {
                        add(value)
                    }
                }
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
        drawableName: String,
    ): Drawable? {
        val drawableId = resources.getIdentifier(drawableName, "drawable", packageName)
        val mipmapId = resources.getIdentifier(drawableName, "mipmap", packageName)
        val resId =
            when {
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

    private fun buildMaskedFallbackIcon(
        context: Context,
        resources: Resources,
        iconPackPackage: String,
        targetPackage: String,
        renderData: IconPackRenderData,
    ): ImageBitmap? {
        if (
            renderData.backDrawables.isEmpty() &&
            renderData.maskDrawable.isNullOrBlank() &&
            renderData.overlayDrawable.isNullOrBlank()
        ) {
            return null
        }

        val appDrawable =
            runCatching { context.packageManager.getApplicationIcon(targetPackage) }
                .onFailure { Log.w(TAG, "Unable to load base icon for $targetPackage", it) }
                .getOrNull() ?: return null

        val selectedBackName =
            renderData
                .backDrawables
                .takeIf { it.isNotEmpty() }
                ?.let { backgrounds ->
                    backgrounds[Math.floorMod(targetPackage.hashCode(), backgrounds.size)]
                }

        val backDrawable = selectedBackName?.let { loadDrawable(resources, iconPackPackage, it) }
        val maskDrawable = renderData.maskDrawable?.let { loadDrawable(resources, iconPackPackage, it) }
        val overlayDrawable = renderData.overlayDrawable?.let { loadDrawable(resources, iconPackPackage, it) }

        if (backDrawable == null && maskDrawable == null && overlayDrawable == null) return null

        val targetSize = resolveTargetSize(appDrawable, backDrawable, maskDrawable, overlayDrawable)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val outputCanvas = Canvas(output)

        backDrawable?.run {
            setBounds(0, 0, targetSize, targetSize)
            draw(outputCanvas)
        }

        val iconLayer = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val iconCanvas = Canvas(iconLayer)
        val iconSize = (targetSize * renderData.scaleFactor).toInt().coerceIn(1, targetSize)
        val iconOffset = (targetSize - iconSize) / 2

        appDrawable.setBounds(iconOffset, iconOffset, iconOffset + iconSize, iconOffset + iconSize)
        appDrawable.draw(iconCanvas)

        if (maskDrawable != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
            // Render mask into an owned bitmap buffer so recycling cannot affect shared resource bitmaps.
            val maskBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val maskCanvas = Canvas(maskBitmap)
            maskDrawable.setBounds(0, 0, targetSize, targetSize)
            maskDrawable.draw(maskCanvas)
            iconCanvas.drawBitmap(maskBitmap, 0f, 0f, paint)
            paint.xfermode = null
            if (!maskBitmap.isRecycled) {
                maskBitmap.recycle()
            }
        }

        outputCanvas.drawBitmap(iconLayer, 0f, 0f, null)
        if (!iconLayer.isRecycled) {
            iconLayer.recycle()
        }

        overlayDrawable?.run {
            setBounds(0, 0, targetSize, targetSize)
            draw(outputCanvas)
        }

        return output.asImageBitmap()
    }

    private fun resolveTargetSize(vararg drawables: Drawable?): Int {
        val maxIntrinsic =
            drawables
                .asSequence()
                .flatMap { drawable ->
                    sequenceOf(drawable?.intrinsicWidth ?: 0, drawable?.intrinsicHeight ?: 0)
                }.filter { it > 0 }
                .maxOrNull()
        return (maxIntrinsic ?: DEFAULT_FALLBACK_ICON_SIZE).coerceAtMost(MAX_FALLBACK_ICON_SIZE)
    }
}

private fun Drawable.toBitmapSafely(): ImageBitmap? =
    runCatching {
        when (this) {
            is BitmapDrawable -> bitmap?.toStableImageBitmap()
            else -> if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
                toBitmap().toStableImageBitmap()
            } else {
                toBitmap().toStableImageBitmap()
            }
        }
    }.getOrNull()

private fun Bitmap.toStableImageBitmap(): ImageBitmap? {
    if (isRecycled || width <= 0 || height <= 0) return null
    val stableBitmap = copy(Bitmap.Config.ARGB_8888, false) ?: return null
    return stableBitmap.asImageBitmap()
}
