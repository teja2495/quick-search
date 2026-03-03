package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Base64
import android.util.TypedValue
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.AppShortcutRepository.HARDCODED_SHORTCUT_KEYS
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.isUserCreatedShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutKey
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.util.Locale

private fun <T : Parcelable> Intent.getParcelableExtraCompat(
    key: String,
    clazz: Class<T>,
): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key) as? T
    }

fun parseCustomShortcutFromPickerResult(
    resultData: Intent?,
    context: Context,
    packageManager: PackageManager,
): StaticShortcut? {
    val data = resultData ?: return null
    val shortcutIntent =
        data.getParcelableExtraCompat(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
            ?: return null
    val launchIntent =
        Intent(shortcutIntent).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val resolvedLaunchPackage = resolveShortcutPackage(launchIntent, packageManager)
    if (launchIntent.component == null &&
        launchIntent.`package`.isNullOrBlank() &&
        !resolvedLaunchPackage.isNullOrBlank()
    ) {
        launchIntent.`package` = resolvedLaunchPackage
    }
    val packageName =
        data.getStringExtra("sourcePackageName")?.trim()?.takeIf { it.isNotBlank() }
            ?: resolvedLaunchPackage
            ?: return null
    val appLabel = resolveAppLabel(packageName, packageManager)
    val customLabel =
        data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.trim().takeIf { !it.isNullOrBlank() }
            ?: launchIntent.component?.shortClassName
            ?: packageName
    val customId =
        "custom_${System.currentTimeMillis()}_${(Math.random() * 100000).toInt()}"
    val iconBase64 = extractCustomShortcutIconBase64(data, context)
    val shortcut =
        StaticShortcut(
            packageName = packageName,
            appLabel = appLabel,
            id = customId,
            shortLabel = customLabel,
            longLabel = customLabel,
            iconResId = null,
            iconBase64 = iconBase64,
            enabled = true,
            intents = listOf(launchIntent),
        )
    return filterShortcuts(listOf(shortcut), packageManager, context).firstOrNull()
}

private fun extractCustomShortcutIconBase64(data: Intent, context: Context): String? {
    val directBitmap =
        data.getParcelableExtraCompat(Intent.EXTRA_SHORTCUT_ICON, Bitmap::class.java)
    if (directBitmap != null) {
        return bitmapToBase64Png(directBitmap)
    }

    val iconResource =
        data.getParcelableExtraCompat(
            Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource::class.java,
        )
    if (iconResource != null) {
        val targetContext =
            kotlin.runCatching { context.createPackageContext(iconResource.packageName, 0) }.getOrNull()
                ?: return null
        val resId =
            kotlin.runCatching {
                targetContext.resources.getIdentifier(
                    iconResource.resourceName,
                    null,
                    iconResource.packageName,
                )
            }.getOrDefault(0)
        if (resId != 0) {
            val drawable = kotlin.runCatching { targetContext.resources.getDrawable(resId, targetContext.theme) }.getOrNull()
            val bitmap = drawable?.toBitmap()
            if (bitmap != null) {
                return bitmapToBase64Png(bitmap)
            }
        }
    }

    return null
}

private fun bitmapToBase64Png(bitmap: Bitmap): String? =
    kotlin.runCatching {
        ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) return null
            Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        }
    }.getOrNull()

private fun resolveShortcutPackage(intent: Intent, packageManager: PackageManager): String? =
    intent.component?.packageName
        ?: intent.`package`
        ?: packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName

private fun resolveAppLabel(packageName: String, packageManager: PackageManager): String {
    val appInfo = kotlin.runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
    val localizedLabel =
        appInfo?.let {
            kotlin.runCatching { packageManager.getApplicationLabel(it)?.toString() }.getOrNull()
        }
    return localizedLabel?.takeIf { it.isNotBlank() }
        ?: appInfo?.nonLocalizedLabel?.toString()?.takeIf { it.isNotBlank() }
        ?: formatPackageNameAsLabel(packageName)
}

private fun formatPackageNameAsLabel(packageName: String): String =
    packageName
        .substringAfterLast(".")
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }

fun loadShortcutsFromSystem(
    context: Context,
    packageManager: PackageManager,
): List<StaticShortcut> {
    val shortcuts = mutableListOf<StaticShortcut>()
    val resolveInfos = queryLaunchableApps(packageManager)
    val labelMap =
        resolveInfos.asSequence().distinctBy { it.activityInfo.packageName }.associate { info ->
            val packageName = info.activityInfo.packageName
            val label =
                kotlin.runCatching { info.loadLabel(packageManager)?.toString() }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: info.activityInfo.nonLocalizedLabel
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                    ?: info.activityInfo.applicationInfo.nonLocalizedLabel
                        ?.toString()
                        ?.takeIf { it.isNotBlank() }
                    ?: formatPackageNameAsLabel(packageName)
            packageName to label
        }

    val appResIdCache = mutableMapOf<String, Int?>()
    val parsedResources = mutableSetOf<Pair<String, Int>>()

    for (resolveInfo in resolveInfos) {
        val packageName = resolveInfo.activityInfo.packageName
        val appLabel = labelMap[packageName] ?: packageName
        val activityResId =
            resolveInfo.activityInfo.metaData?.getInt(META_DATA_SHORTCUTS, 0) ?: 0
        val xmlResId =
            if (activityResId != 0) {
                activityResId
            } else {
                appResIdCache.getOrPut(packageName) {
                    getStaticShortcutsXmlResId(packageName, packageManager)
                }
                    ?: 0
            }

        if (xmlResId == 0) continue
        val key = packageName to xmlResId
        if (!parsedResources.add(key)) continue

        val parsedShortcuts =
            parseStaticShortcuts(
                context = context,
                packageName = packageName,
                appLabel = appLabel,
                xmlResId = xmlResId,
            )
        if (parsedShortcuts.isNotEmpty()) {
            shortcuts.addAll(filterShortcuts(parsedShortcuts, packageManager, context))
        }
    }

    val locale = Locale.getDefault()
    return shortcuts.sortedWith(
        compareBy<StaticShortcut> { it.appLabel.lowercase(locale) }
            .thenBy { shortcutDisplayName(it).lowercase(locale) }
            .thenBy { it.id },
    )
}

fun mergeAndSortShortcuts(
    staticShortcuts: List<StaticShortcut>,
    customShortcuts: List<StaticShortcut> = emptyList(),
    context: Context,
    packageManager: PackageManager,
): List<StaticShortcut> {
    val locale = Locale.getDefault()
    val hardcodedShortcuts = loadHardcodedShortcuts(staticShortcuts, context, packageManager)
    return filterShortcuts(staticShortcuts + hardcodedShortcuts + customShortcuts, packageManager, context)
        .distinctBy { shortcutKey(it) }
        .sortedWith(
            compareBy<StaticShortcut> { it.appLabel.lowercase(locale) }
                .thenBy { shortcutDisplayName(it).lowercase(locale) }
                .thenBy { it.id },
        )
}

private fun queryLaunchableApps(packageManager: PackageManager) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(
                (PackageManager.MATCH_ALL or PackageManager.GET_META_DATA).toLong(),
            ),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_ALL or PackageManager.GET_META_DATA,
        )
    }

private fun getStaticShortcutsXmlResId(packageName: String, packageManager: PackageManager): Int? {
    val appInfo =
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(
                        PackageManager.GET_META_DATA.toLong(),
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.GET_META_DATA,
                )
            }
        }.getOrNull()
            ?: return null

    val metadata = appInfo.metaData ?: return null
    val resId = metadata.getInt(META_DATA_SHORTCUTS, 0)
    return resId.takeIf { it != 0 }
}

private fun parseStaticShortcuts(
    context: Context,
    packageName: String,
    appLabel: String,
    xmlResId: Int,
): List<StaticShortcut> {
    val targetContext =
        kotlin.runCatching { context.createPackageContext(packageName, 0) }.getOrNull()
            ?: return emptyList()

    val res = targetContext.resources
    val parser = kotlin.runCatching { res.getXml(xmlResId) }.getOrNull() ?: return emptyList()

    val shortcuts = mutableListOf<StaticShortcut>()
    var currentId: String? = null
    var currentShortLabel: String? = null
    var currentLongLabel: String? = null
    var currentIcon: Int? = null
    var currentEnabled = true
    val currentIntents = mutableListOf<Intent>()
    var currentIntent: Intent? = null

    try {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "shortcut" -> {
                            currentId =
                                parser.getAttributeValue(ANDROID_NS, "shortcutId")
                                    ?: parser.getAttributeValue(null, "shortcutId")
                            currentIcon =
                                parser
                                    .getAttributeResourceValue(ANDROID_NS, "icon", 0)
                                    .takeIf { it != 0 }
                            currentShortLabel =
                                readLabelAttr(
                                    res,
                                    parser,
                                    "shortcutShortLabel",
                                    "shortLabel",
                                )
                            currentLongLabel =
                                readLabelAttr(res, parser, "shortcutLongLabel", "longLabel")
                            currentEnabled =
                                parser.getAttributeBooleanValue(ANDROID_NS, "enabled", true)
                            currentIntents.clear()
                            currentIntent = null
                        }

                        "intent" -> {
                            val intent = parseShortcutIntent(packageName, parser)
                            currentIntents.add(intent)
                            currentIntent = intent
                        }

                        "extra" -> {
                            applyShortcutExtra(res, parser, currentIntent)
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "intent" -> {
                            currentIntent = null
                        }

                        "shortcut" -> {
                            val id = currentId?.trim()
                            if (currentEnabled && id != null && isValidShortcutId(id)) {
                                shortcuts.add(
                                    StaticShortcut(
                                        packageName = packageName,
                                        appLabel = appLabel,
                                        id = id,
                                        shortLabel = currentShortLabel,
                                        longLabel = currentLongLabel,
                                        iconResId = currentIcon,
                                        enabled = currentEnabled,
                                        intents = currentIntents.toList(),
                                    ),
                                )
                            }
                            currentId = null
                            currentShortLabel = null
                            currentLongLabel = null
                            currentIcon = null
                            currentEnabled = true
                            currentIntents.clear()
                            currentIntent = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (_: Exception) {
    } finally {
        kotlin.runCatching { parser.close() }
    }

    return shortcuts
}

private fun parseShortcutIntent(
    packageName: String,
    parser: XmlPullParser,
): Intent {
    val action = parser.getAttributeValue(ANDROID_NS, "action")
    val targetClass = parser.getAttributeValue(ANDROID_NS, "targetClass")
    val targetPackage = parser.getAttributeValue(ANDROID_NS, "targetPackage") ?: packageName
    val dataUri = parser.getAttributeValue(ANDROID_NS, "data")

    return Intent().apply {
        if (action != null) setAction(action)
        if (dataUri != null) data = Uri.parse(dataUri)
        if (targetClass != null) {
            val resolvedClass = resolveShortcutClassName(targetPackage, targetClass)
            component = ComponentName(targetPackage, resolvedClass)
        } else {
            `package` = targetPackage
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private fun applyShortcutExtra(
    res: android.content.res.Resources,
    parser: android.content.res.XmlResourceParser,
    intent: Intent?,
) {
    val targetIntent = intent ?: return
    val name =
        parser.getAttributeValue(ANDROID_NS, "name")
            ?: parser.getAttributeValue(null, "name")
    val value = parseShortcutExtraValue(res, parser) ?: return
    val key = name?.trim().takeIf { !it.isNullOrBlank() } ?: return
    when (value) {
        is Boolean -> targetIntent.putExtra(key, value)
        is Int -> targetIntent.putExtra(key, value)
        is Long -> targetIntent.putExtra(key, value)
        is Float -> targetIntent.putExtra(key, value)
        is Double -> targetIntent.putExtra(key, value)
        is Uri -> targetIntent.putExtra(key, value)
        is String -> targetIntent.putExtra(key, value)
    }
}

private fun parseShortcutExtraValue(
    res: android.content.res.Resources,
    parser: android.content.res.XmlResourceParser,
): Any? {
    val valueResId =
        parser.getAttributeResourceValue(ANDROID_NS, "value", 0).takeIf { it != 0 }
            ?: parser.getAttributeResourceValue(null, "value", 0).takeIf { it != 0 }
    if (valueResId != null) {
        return readResourceValue(res, valueResId)
    }

    val valueBoolean =
        parser.getAttributeValue(ANDROID_NS, "valueBoolean")
            ?: parser.getAttributeValue(null, "valueBoolean")
    if (!valueBoolean.isNullOrBlank()) {
        val parsedBoolean =
            when {
                valueBoolean.equals("true", ignoreCase = true) -> true
                valueBoolean.equals("false", ignoreCase = true) -> false
                else -> null
            }
        if (parsedBoolean != null) return parsedBoolean
    }

    val valueInt =
        parser.getAttributeValue(ANDROID_NS, "valueInt")
            ?: parser.getAttributeValue(null, "valueInt")
            ?: parser.getAttributeValue(ANDROID_NS, "valueInteger")
            ?: parser.getAttributeValue(null, "valueInteger")
    valueInt?.toIntOrNull()?.let {
        return it
    }

    val valueLong =
        parser.getAttributeValue(ANDROID_NS, "valueLong")
            ?: parser.getAttributeValue(null, "valueLong")
    valueLong?.toLongOrNull()?.let {
        return it
    }

    val valueFloat =
        parser.getAttributeValue(ANDROID_NS, "valueFloat")
            ?: parser.getAttributeValue(null, "valueFloat")
    valueFloat?.toFloatOrNull()?.let {
        return it
    }

    val valueDouble =
        parser.getAttributeValue(ANDROID_NS, "valueDouble")
            ?: parser.getAttributeValue(null, "valueDouble")
    valueDouble?.toDoubleOrNull()?.let {
        return it
    }

    val valueUri =
        parser.getAttributeValue(ANDROID_NS, "valueUri")
            ?: parser.getAttributeValue(null, "valueUri")
    if (!valueUri.isNullOrBlank()) {
        return Uri.parse(valueUri)
    }

    val valueString =
        parser.getAttributeValue(ANDROID_NS, "valueString")
            ?: parser.getAttributeValue(null, "valueString")
    if (!valueString.isNullOrBlank()) {
        return valueString
    }

    val rawValue =
        parser.getAttributeValue(ANDROID_NS, "value")
            ?: parser.getAttributeValue(null, "value")
    return rawValue?.let { parseLiteralExtra(it) }
}

private fun readResourceValue(
    res: android.content.res.Resources,
    resId: Int,
): Any? {
    val typedValue = TypedValue()
    return kotlin.runCatching {
        res.getValue(resId, typedValue, true)
        when (typedValue.type) {
            TypedValue.TYPE_STRING -> typedValue.string?.toString()
            TypedValue.TYPE_INT_BOOLEAN -> typedValue.data != 0
            TypedValue.TYPE_FLOAT -> Float.fromBits(typedValue.data)
            in TypedValue.TYPE_FIRST_INT..TypedValue.TYPE_LAST_INT -> typedValue.data
            else -> typedValue.coerceToString()?.toString()
        }
    }.getOrNull()
}

private fun parseLiteralExtra(value: String): Any? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.equals("true", ignoreCase = true)) return true
    if (trimmed.equals("false", ignoreCase = true)) return false

    val hasExponent = trimmed.contains('e', ignoreCase = true)
    val hasDecimal = trimmed.contains('.')
    if (!hasDecimal && !hasExponent) {
        val normalized = trimmed.trimStart('-')
        if (normalized.length > 1 && normalized.startsWith("0")) return trimmed
        val longValue = trimmed.toLongOrNull()
        if (longValue != null) {
            return if (longValue in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                longValue.toInt()
            } else {
                longValue
            }
        }
    }

    val floatValue = trimmed.toFloatOrNull()
    if (floatValue != null && (hasDecimal || hasExponent)) {
        return floatValue
    }

    return trimmed
}

private fun resolveShortcutClassName(
    targetPackage: String,
    targetClass: String,
): String =
    when {
        targetClass.startsWith(".") -> targetPackage + targetClass
        targetClass.contains(".") -> targetClass
        else -> "$targetPackage.$targetClass"
    }

private fun readLabelAttr(
    res: android.content.res.Resources,
    parser: android.content.res.XmlResourceParser,
    vararg attrNames: String,
): String? {
    for (attrName in attrNames) {
        val resId = parser.getAttributeResourceValue(ANDROID_NS, attrName, 0)
        if (resId != 0) {
            return kotlin.runCatching { res.getString(resId) }.getOrNull()
        }
        val rawValue =
            parser.getAttributeValue(ANDROID_NS, attrName)
                ?: parser.getAttributeValue(null, attrName)
        if (!rawValue.isNullOrBlank()) {
            return rawValue
        }
    }
    return null
}

private fun canLaunchShortcut(shortcut: StaticShortcut, packageManager: PackageManager, context: Context): Boolean {
    val intent = shortcut.intents.lastOrNull() ?: return false
    val resolved = packageManager.resolveActivity(intent, 0) ?: return false
    val activityInfo = resolved.activityInfo
    if (!activityInfo.exported) return false
    val requiredPermission = activityInfo.permission?.takeIf { it.isNotBlank() } ?: return true
    return context.checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED
}

fun filterShortcuts(
    shortcuts: List<StaticShortcut>,
    packageManager: PackageManager,
    context: Context,
): List<StaticShortcut> =
    shortcuts.filter { shortcut ->
        val shortcutKey = "${shortcut.packageName}:${shortcut.id}"
        val isBlockedBrowserShortcut =
            (shortcut.packageName == "com.android.chrome" ||
                shortcut.packageName == "com.brave.browser") &&
                !isUserCreatedShortcut(shortcut) &&
                shortcutKey !in HARDCODED_SHORTCUT_KEYS
        shortcut.enabled &&
            !isBlockedBrowserShortcut &&
            shortcut.intents.isNotEmpty() &&
            canLaunchShortcut(shortcut, packageManager, context)
    }

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val META_DATA_SHORTCUTS = "android.app.shortcuts"

fun launchStaticShortcut(
    context: Context,
    shortcut: StaticShortcut,
): String? {
    if (!shortcut.enabled) {
        return context.getString(R.string.error_shortcut_disabled)
    }

    val baseIntent = shortcut.intents.lastOrNull() ?: return context.getString(R.string.error_shortcut_no_intent)
    val intent = Intent(baseIntent).apply { putExtra(Intent.EXTRA_SHORTCUT_ID, shortcut.id) }

    val pm = context.packageManager
    val details = formatIntentDetails(intent)
    val resolved =
        pm.resolveActivity(intent, 0)
            ?: return context.getString(R.string.error_shortcut_no_activity_resolves) + details.toSuffixDetail()

    val activityInfo = resolved.activityInfo
    if (!activityInfo.exported) {
        return context.getString(R.string.error_shortcut_activity_not_exported) + details.toSuffixDetail()
    }
    val requiredPermission = activityInfo.permission?.takeIf { it.isNotBlank() }
    if (requiredPermission != null &&
        context.checkSelfPermission(requiredPermission) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return context.getString(R.string.error_shortcut_requires_permission, requiredPermission) + details.toSuffixDetail()
    }

    val error = kotlin.runCatching { context.startActivity(intent) }.exceptionOrNull()
    if (error != null) {
        return context.getString(R.string.error_shortcut_launch_failed, error.message ?: context.getString(R.string.error_unknown)) + details.toSuffixDetail()
    }

    return null
}

private fun formatIntentDetails(intent: Intent): String {
    val parts = mutableListOf<String>()
    intent.action?.takeIf { it.isNotBlank() }?.let { parts.add("action=$it") }
    intent.component
        ?.className
        ?.takeIf { it.isNotBlank() }
        ?.let { parts.add("component=$it") }
    intent.`package`?.takeIf { it.isNotBlank() }?.let { parts.add("package=$it") }
    intent.dataString?.takeIf { it.isNotBlank() }?.let { parts.add("data=$it") }
    if (parts.isEmpty()) return ""
    return "(${parts.joinToString(", ")})"
}

private fun String.toSuffixDetail(): String = if (isBlank()) "" else " $this"