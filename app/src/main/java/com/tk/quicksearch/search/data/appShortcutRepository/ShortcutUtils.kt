package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserManager
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
import com.tk.quicksearch.searchEngines.SearchTargetQueryShortcutActivity
import com.tk.quicksearch.tools.tasker.TaskerIntegration
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
    sourcePackageName: String? = null,
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
    val explicitSourcePackageName = sourcePackageName?.trim()?.takeIf { it.isNotBlank() }
    val packageName =
        explicitSourcePackageName
            ?: data.getStringExtra("sourcePackageName")?.trim()?.takeIf { it.isNotBlank() }
            ?: resolvedLaunchPackage
            ?: return null
    val appLabel = resolveAppLabel(context, packageName, packageManager)
    val customLabel =
        data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.trim().takeIf { !it.isNullOrBlank() }
            ?: launchIntent.component?.shortClassName
            ?: packageName
    val customId =
        "custom_${System.currentTimeMillis()}_${(Math.random() * 100000).toInt()}"
    val iconBase64 = extractCustomShortcutIconBase64(data, context) ?: loadAppIconBase64(context, packageName)
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
                    ?: resolveAppLabel(context, packageName, packageManager)
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
    // Custom shortcuts have already been validated when the picker returned them. Do not
    // rediscover them through PackageManager during a later catalog refresh: some providers
    // (notably Tasker's task-shortcut picker) return an intent that only resolves while their
    // setup activity is active. Revalidating it later makes a successfully saved shortcut
    // disappear even though its persisted launch intent is still the user's configuration.
    val validCustomShortcuts =
        customShortcuts.filter { shortcut ->
            shortcut.enabled && shortcut.intents.isNotEmpty() && isUserCreatedShortcut(shortcut)
        }
    return (filterShortcuts(staticShortcuts + hardcodedShortcuts, packageManager, context) +
        validCustomShortcuts)
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
                                val iconBase64 =
                                    currentIcon
                                        ?.let { iconResId ->
                                            kotlin.runCatching {
                                                res.getDrawable(iconResId, targetContext.theme)
                                            }.getOrNull()
                                                ?.toBitmap(width = 96, height = 96)
                                                ?.let(::bitmapToBase64Png)
                                        }
                                shortcuts.add(
                                    StaticShortcut(
                                        packageName = packageName,
                                        appLabel = appLabel,
                                        id = id,
                                        shortLabel = currentShortLabel,
                                        longLabel = currentLongLabel,
                                        iconResId = currentIcon,
                                        iconBase64 = iconBase64,
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
    if (shortcut.intents.isEmpty()) return false
    return shortcut.intents.asReversed().any { intent ->
        val resolved = packageManager.resolveActivity(intent, 0) ?: return@any false
        val activityInfo = resolved.activityInfo
        if (!activityInfo.exported) return@any false
        val requiredPermission = activityInfo.permission?.takeIf { it.isNotBlank() } ?: return@any true
        context.checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED
    }
}

fun filterShortcuts(
    shortcuts: List<StaticShortcut>,
    packageManager: PackageManager,
    context: Context,
): List<StaticShortcut> =
    shortcuts.filter { shortcut ->
        val isCustomDeepLink = shortcut.id.startsWith("custom_deeplink_")
        val isLauncherAppsShortcut = isLauncherAppsSentinelShortcut(shortcut)
        val shortcutKey = "${shortcut.packageName}:${shortcut.id}"
        // The browser block list exists because the XML parser surfaced low-quality / broken
        // entries from Chrome/Brave's shortcuts.xml. Shortcuts fetched via the official
        // LauncherApps API are the same ones the system launcher shows, so no need to hide them.
        val isBlockedBrowserShortcut =
            (shortcut.packageName == "com.android.chrome" ||
                shortcut.packageName == "com.brave.browser") &&
                !isUserCreatedShortcut(shortcut) &&
                !isLauncherAppsShortcut &&
                shortcutKey !in HARDCODED_SHORTCUT_KEYS
        val isLaunchable =
            when {
                isCustomDeepLink -> shortcut.intents.any { !it.dataString.isNullOrBlank() }
                // LauncherApps-fetched shortcuts can't be resolved by PackageManager because
                // their real intent is hidden from us. Trust them and rely on
                // LauncherApps.startShortcut at launch time.
                isLauncherAppsShortcut -> true
                else -> canLaunchShortcut(shortcut, packageManager, context)
            }
        shortcut.enabled &&
            !isBlockedBrowserShortcut &&
            shortcut.intents.isNotEmpty() &&
            isLaunchable
    }

private fun isLauncherAppsSentinelShortcut(shortcut: StaticShortcut): Boolean =
    shortcut.intents.firstOrNull()?.action == ACTION_LAUNCHER_APPS_SHORTCUT

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
private const val META_DATA_SHORTCUTS = "android.app.shortcuts"
