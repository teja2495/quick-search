package com.tk.quicksearch.search.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.util.Locale
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.SettingsCard
import com.tk.quicksearch.ui.theme.DesignTokens

data class StaticShortcut(
    val packageName: String,
    val appLabel: String,
    val id: String,
    val shortLabel: String?,
    val longLabel: String?,
    val iconResId: Int?,
    val enabled: Boolean,
    val intents: List<Intent>
)

private class AppShortcutCache(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCachedShortcuts(): List<StaticShortcut>? {
        if (!prefs.contains(KEY_SHORTCUT_LIST)) return null

        return runCatching {
            val json = prefs.getString(KEY_SHORTCUT_LIST, null) ?: return null
            if (json.length < 10 || json == "[]") return null
            JSONArray(json).toShortcutList()
        }.getOrNull()
    }

    fun saveShortcuts(shortcuts: List<StaticShortcut>): Boolean {
        return runCatching {
            val json = shortcuts.toShortcutJsonArray().toString()
            prefs.edit()
                .putString(KEY_SHORTCUT_LIST, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply()
            true
        }.getOrDefault(false)
    }

    fun clearCache() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "app_shortcut_cache"
        private const val KEY_SHORTCUT_LIST = "shortcut_list"
        private const val KEY_LAST_UPDATE = "last_update"

        private const val FIELD_PACKAGE_NAME = "packageName"
        private const val FIELD_APP_LABEL = "appLabel"
        private const val FIELD_ID = "id"
        private const val FIELD_SHORT_LABEL = "shortLabel"
        private const val FIELD_LONG_LABEL = "longLabel"
        private const val FIELD_ICON_RES_ID = "iconResId"
        private const val FIELD_ENABLED = "enabled"
        private const val FIELD_INTENTS = "intents"

        private const val FIELD_ACTION = "action"
        private const val FIELD_TARGET_PACKAGE = "targetPackage"
        private const val FIELD_TARGET_CLASS = "targetClass"
        private const val FIELD_DATA = "data"
        private const val FIELD_EXTRAS = "extras"
        private const val FIELD_EXTRA_NAME = "name"
        private const val FIELD_EXTRA_TYPE = "type"
        private const val FIELD_EXTRA_VALUE = "value"

        private const val EXTRA_TYPE_STRING = "string"
        private const val EXTRA_TYPE_BOOLEAN = "boolean"
        private const val EXTRA_TYPE_INT = "int"
        private const val EXTRA_TYPE_LONG = "long"
        private const val EXTRA_TYPE_FLOAT = "float"
        private const val EXTRA_TYPE_DOUBLE = "double"
        private const val EXTRA_TYPE_URI = "uri"

        private fun JSONArray.toShortcutList(): List<StaticShortcut> {
            val shortcuts = mutableListOf<StaticShortcut>()
            for (index in 0 until length()) {
                val jsonObject = getJSONObject(index)
                val id = jsonObject.getString(FIELD_ID).trim()
                val enabled = jsonObject.optBoolean(FIELD_ENABLED, true)
                if (!enabled || !isValidShortcutId(id)) continue
                val packageName = jsonObject.getString(FIELD_PACKAGE_NAME)
                val appLabel = jsonObject.optString(FIELD_APP_LABEL, packageName)
                val shortLabel = jsonObject.getNullableString(FIELD_SHORT_LABEL)
                val longLabel = jsonObject.getNullableString(FIELD_LONG_LABEL)
                val iconResId = if (jsonObject.has(FIELD_ICON_RES_ID)) {
                    jsonObject.getInt(FIELD_ICON_RES_ID)
                } else {
                    null
                }
                val intentsJson = jsonObject.optJSONArray(FIELD_INTENTS) ?: JSONArray()
                val intents = intentsJson.toIntentList(packageName)

                shortcuts.add(
                    StaticShortcut(
                        packageName = packageName,
                        appLabel = appLabel,
                        id = id,
                        shortLabel = shortLabel,
                        longLabel = longLabel,
                        iconResId = iconResId,
                        enabled = enabled,
                        intents = intents
                    )
                )
            }
            return shortcuts
        }

        private fun List<StaticShortcut>.toShortcutJsonArray(): JSONArray {
            return JSONArray().apply {
                forEach { shortcut ->
                    put(
                        JSONObject().apply {
                            put(FIELD_PACKAGE_NAME, shortcut.packageName)
                            put(FIELD_APP_LABEL, shortcut.appLabel)
                            put(FIELD_ID, shortcut.id)
                            shortcut.shortLabel?.let { put(FIELD_SHORT_LABEL, it) }
                            shortcut.longLabel?.let { put(FIELD_LONG_LABEL, it) }
                            shortcut.iconResId?.let { put(FIELD_ICON_RES_ID, it) }
                            put(FIELD_ENABLED, shortcut.enabled)
                            put(FIELD_INTENTS, shortcut.intents.toIntentJsonArray())
                        }
                    )
                }
            }
        }

        private fun List<Intent>.toIntentJsonArray(): JSONArray {
            return JSONArray().apply {
                forEach { intent ->
                    val component = intent.component
                    val targetPackage = component?.packageName ?: intent.`package`
                    val targetClass = component?.className
                    val data = intent.dataString
                    val action = intent.action
                    val extras = intent.extras?.toExtrasJsonArray()
                    put(
                        JSONObject().apply {
                            if (!action.isNullOrBlank()) put(FIELD_ACTION, action)
                            if (!targetPackage.isNullOrBlank()) {
                                put(FIELD_TARGET_PACKAGE, targetPackage)
                            }
                            if (!targetClass.isNullOrBlank()) put(FIELD_TARGET_CLASS, targetClass)
                            if (!data.isNullOrBlank()) put(FIELD_DATA, data)
                            if (extras != null && extras.length() > 0) {
                                put(FIELD_EXTRAS, extras)
                            }
                        }
                    )
                }
            }
        }

        private fun JSONArray.toIntentList(defaultPackage: String): List<Intent> {
            val intents = mutableListOf<Intent>()
            for (index in 0 until length()) {
                val jsonObject = getJSONObject(index)
                val action = jsonObject.getNullableString(FIELD_ACTION)
                val targetPackage = jsonObject.getNullableString(FIELD_TARGET_PACKAGE)
                    ?: defaultPackage
                val targetClass = jsonObject.getNullableString(FIELD_TARGET_CLASS)
                val data = jsonObject.getNullableString(FIELD_DATA)
                val extrasJson = jsonObject.optJSONArray(FIELD_EXTRAS)

                intents.add(
                    Intent().apply {
                        if (!action.isNullOrBlank()) setAction(action)
                        if (!data.isNullOrBlank()) setData(Uri.parse(data))
                        if (!targetClass.isNullOrBlank()) {
                            component = ComponentName(targetPackage, targetClass)
                        } else {
                            `package` = targetPackage
                        }
                        extrasJson?.applyExtras(this)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            return intents
        }

        private fun JSONObject.getNullableString(name: String): String? {
            if (!has(name) || isNull(name)) return null
            return getString(name).takeIf { it.isNotBlank() }
        }

        private fun Bundle.toExtrasJsonArray(): JSONArray {
            return JSONArray().apply {
                for (key in keySet()) {
                    val value = get(key) ?: continue
                    val type = when (value) {
                        is Boolean -> EXTRA_TYPE_BOOLEAN
                        is Int -> EXTRA_TYPE_INT
                        is Long -> EXTRA_TYPE_LONG
                        is Float -> EXTRA_TYPE_FLOAT
                        is Double -> EXTRA_TYPE_DOUBLE
                        is Uri -> EXTRA_TYPE_URI
                        is CharSequence -> EXTRA_TYPE_STRING
                        is String -> EXTRA_TYPE_STRING
                        else -> null
                    } ?: continue
                    val jsonValue = when (value) {
                        is Uri -> value.toString()
                        else -> value
                    }
                    put(
                        JSONObject().apply {
                            put(FIELD_EXTRA_NAME, key)
                            put(FIELD_EXTRA_TYPE, type)
                            put(FIELD_EXTRA_VALUE, jsonValue)
                        }
                    )
                }
            }
        }

        private fun JSONArray.applyExtras(intent: Intent) {
            for (index in 0 until length()) {
                val jsonObject = optJSONObject(index) ?: continue
                val name = jsonObject.optString(FIELD_EXTRA_NAME).takeIf { it.isNotBlank() }
                    ?: continue
                val type = jsonObject.optString(FIELD_EXTRA_TYPE)
                if (!jsonObject.has(FIELD_EXTRA_VALUE)) continue
                when (type) {
                    EXTRA_TYPE_BOOLEAN -> intent.putExtra(
                        name,
                        jsonObject.optBoolean(FIELD_EXTRA_VALUE)
                    )
                    EXTRA_TYPE_INT -> intent.putExtra(
                        name,
                        jsonObject.optInt(FIELD_EXTRA_VALUE)
                    )
                    EXTRA_TYPE_LONG -> intent.putExtra(
                        name,
                        jsonObject.optLong(FIELD_EXTRA_VALUE)
                    )
                    EXTRA_TYPE_FLOAT -> intent.putExtra(
                        name,
                        jsonObject.optDouble(FIELD_EXTRA_VALUE).toFloat()
                    )
                    EXTRA_TYPE_DOUBLE -> intent.putExtra(
                        name,
                        jsonObject.optDouble(FIELD_EXTRA_VALUE)
                    )
                    EXTRA_TYPE_URI -> {
                        val raw = jsonObject.optString(FIELD_EXTRA_VALUE)
                        if (raw.isNotBlank()) {
                            intent.putExtra(name, Uri.parse(raw))
                        }
                    }
                    EXTRA_TYPE_STRING -> intent.putExtra(
                        name,
                        jsonObject.optString(FIELD_EXTRA_VALUE)
                    )
                }
            }
        }
    }
}

class AppShortcutRepository(
    private val context: Context
) {

    private val packageManager: PackageManager = context.packageManager
    private val shortcutCache = AppShortcutCache(context)

    @Volatile
    private var inMemoryShortcuts: List<StaticShortcut>? = null

    suspend fun loadCachedShortcuts(): List<StaticShortcut>? {
        inMemoryShortcuts?.let { return it }
        return withContext(Dispatchers.IO) {
            val cached = shortcutCache.loadCachedShortcuts()
            val filtered = cached?.let { filterShortcuts(it) }
            if (filtered != null) {
                inMemoryShortcuts = filtered
            }
            filtered
        }
    }

    fun clearCache() {
        shortcutCache.clearCache()
        inMemoryShortcuts = null
    }

    suspend fun loadStaticShortcuts(): List<StaticShortcut> = withContext(Dispatchers.IO) {
        val shortcuts = loadShortcutsFromSystem()
        shortcutCache.saveShortcuts(shortcuts)
        inMemoryShortcuts = shortcuts
        shortcuts
    }

    private fun loadShortcutsFromSystem(): List<StaticShortcut> {
        val shortcuts = mutableListOf<StaticShortcut>()
        val resolveInfos = queryLaunchableApps()
        val labelMap = resolveInfos.asSequence()
            .distinctBy { it.activityInfo.packageName }
            .associate { info ->
                val packageName = info.activityInfo.packageName
                val label = runCatching { info.loadLabel(packageManager)?.toString() }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: packageName
                packageName to label
            }

        val appResIdCache = mutableMapOf<String, Int?>()
        val parsedResources = mutableSetOf<Pair<String, Int>>()

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            val appLabel = labelMap[packageName] ?: packageName
            val activityResId = resolveInfo.activityInfo.metaData
                ?.getInt(META_DATA_SHORTCUTS, 0)
                ?: 0
            val xmlResId = if (activityResId != 0) {
                activityResId
            } else {
                appResIdCache.getOrPut(packageName) {
                    getStaticShortcutsXmlResId(packageName)
                } ?: 0
            }

            if (xmlResId == 0) continue
            val key = packageName to xmlResId
            if (!parsedResources.add(key)) continue

            val parsedShortcuts = parseStaticShortcuts(
                packageName = packageName,
                appLabel = appLabel,
                xmlResId = xmlResId
            )
            if (parsedShortcuts.isNotEmpty()) {
                shortcuts.addAll(filterShortcuts(parsedShortcuts))
            }
        }

        val locale = Locale.getDefault()
        return shortcuts.sortedWith(
            compareBy<StaticShortcut> { it.appLabel.lowercase(locale) }
                .thenBy { shortcutDisplayName(it).lowercase(locale) }
                .thenBy { it.id }
        )
    }

    private fun queryLaunchableApps() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.ResolveInfoFlags.of(
                (PackageManager.MATCH_ALL or PackageManager.GET_META_DATA).toLong()
            )
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            PackageManager.MATCH_ALL or PackageManager.GET_META_DATA
        )
    }

    private fun getStaticShortcutsXmlResId(packageName: String): Int? {
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }
        }.getOrNull() ?: return null

        val metadata = appInfo.metaData ?: return null
        val resId = metadata.getInt(META_DATA_SHORTCUTS, 0)
        return resId.takeIf { it != 0 }
    }

    private fun parseStaticShortcuts(
        packageName: String,
        appLabel: String,
        xmlResId: Int
    ): List<StaticShortcut> {
        val targetContext = runCatching {
            context.createPackageContext(packageName, 0)
        }.getOrNull() ?: return emptyList()

        val res = targetContext.resources
        val parser = runCatching { res.getXml(xmlResId) }.getOrNull()
            ?: return emptyList()

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
                                currentId = parser.getAttributeValue(ANDROID_NS, "shortcutId")
                                    ?: parser.getAttributeValue(null, "shortcutId")
                                currentIcon = parser.getAttributeResourceValue(
                                    ANDROID_NS,
                                    "icon",
                                    0
                                ).takeIf { it != 0 }
                                currentShortLabel = readLabelAttr(
                                    res,
                                    parser,
                                    "shortcutShortLabel",
                                    "shortLabel"
                                )
                                currentLongLabel = readLabelAttr(
                                    res,
                                    parser,
                                    "shortcutLongLabel",
                                    "longLabel"
                                )
                                currentEnabled = parser.getAttributeBooleanValue(
                                    ANDROID_NS,
                                    "enabled",
                                    true
                                )
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
                            "intent" -> currentIntent = null
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
                                            intents = currentIntents.toList()
                                        )
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
            runCatching { parser.close() }
        }

        return shortcuts
    }

    private fun parseShortcutIntent(
        packageName: String,
        parser: XmlPullParser
    ): Intent {
        val action = parser.getAttributeValue(ANDROID_NS, "action")
        val targetClass = parser.getAttributeValue(ANDROID_NS, "targetClass")
        val targetPackage = parser.getAttributeValue(ANDROID_NS, "targetPackage") ?: packageName
        val dataUri = parser.getAttributeValue(ANDROID_NS, "data")

        return Intent().apply {
            if (action != null) setAction(action)
            if (dataUri != null) data = Uri.parse(dataUri)
            if (targetClass != null) {
                val resolvedClass = resolveClassName(targetPackage, targetClass)
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
        intent: Intent?
    ) {
        val targetIntent = intent ?: return
        val name = parser.getAttributeValue(ANDROID_NS, "name")
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
        parser: android.content.res.XmlResourceParser
    ): Any? {
        val valueResId = parser.getAttributeResourceValue(ANDROID_NS, "value", 0)
            .takeIf { it != 0 }
            ?: parser.getAttributeResourceValue(null, "value", 0).takeIf { it != 0 }
        if (valueResId != null) {
            return readResourceValue(res, valueResId)
        }

        val valueBoolean = parser.getAttributeValue(ANDROID_NS, "valueBoolean")
            ?: parser.getAttributeValue(null, "valueBoolean")
        if (!valueBoolean.isNullOrBlank()) {
            val parsedBoolean = when {
                valueBoolean.equals("true", ignoreCase = true) -> true
                valueBoolean.equals("false", ignoreCase = true) -> false
                else -> null
            }
            if (parsedBoolean != null) return parsedBoolean
        }

        val valueInt = parser.getAttributeValue(ANDROID_NS, "valueInt")
            ?: parser.getAttributeValue(null, "valueInt")
            ?: parser.getAttributeValue(ANDROID_NS, "valueInteger")
            ?: parser.getAttributeValue(null, "valueInteger")
        valueInt?.toIntOrNull()?.let { return it }

        val valueLong = parser.getAttributeValue(ANDROID_NS, "valueLong")
            ?: parser.getAttributeValue(null, "valueLong")
        valueLong?.toLongOrNull()?.let { return it }

        val valueFloat = parser.getAttributeValue(ANDROID_NS, "valueFloat")
            ?: parser.getAttributeValue(null, "valueFloat")
        valueFloat?.toFloatOrNull()?.let { return it }

        val valueDouble = parser.getAttributeValue(ANDROID_NS, "valueDouble")
            ?: parser.getAttributeValue(null, "valueDouble")
        valueDouble?.toDoubleOrNull()?.let { return it }

        val valueUri = parser.getAttributeValue(ANDROID_NS, "valueUri")
            ?: parser.getAttributeValue(null, "valueUri")
        if (!valueUri.isNullOrBlank()) {
            return Uri.parse(valueUri)
        }

        val valueString = parser.getAttributeValue(ANDROID_NS, "valueString")
            ?: parser.getAttributeValue(null, "valueString")
        if (!valueString.isNullOrBlank()) {
            return valueString
        }

        val rawValue = parser.getAttributeValue(ANDROID_NS, "value")
            ?: parser.getAttributeValue(null, "value")
        return rawValue?.let { parseLiteralExtra(it) }
    }

    private fun readResourceValue(
        res: android.content.res.Resources,
        resId: Int
    ): Any? {
        val typedValue = TypedValue()
        return runCatching {
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

    private fun resolveClassName(targetPackage: String, targetClass: String): String {
        return when {
            targetClass.startsWith(".") -> targetPackage + targetClass
            targetClass.contains(".") -> targetClass
            else -> "$targetPackage.$targetClass"
        }
    }

    private fun readLabelAttr(
        res: android.content.res.Resources,
        parser: android.content.res.XmlResourceParser,
        vararg attrNames: String
    ): String? {
        for (attrName in attrNames) {
            val resId = parser.getAttributeResourceValue(ANDROID_NS, attrName, 0)
            if (resId != 0) {
                return runCatching { res.getString(resId) }.getOrNull()
            }
            val rawValue = parser.getAttributeValue(ANDROID_NS, attrName)
                ?: parser.getAttributeValue(null, attrName)
            if (!rawValue.isNullOrBlank()) {
                return rawValue
            }
        }
        return null
    }

    private fun canLaunchShortcut(shortcut: StaticShortcut): Boolean {
        val intent = shortcut.intents.lastOrNull() ?: return false
        val resolved = packageManager.resolveActivity(intent, 0) ?: return false
        val activityInfo = resolved.activityInfo
        if (!activityInfo.exported) return false
        val requiredPermission = activityInfo.permission?.takeIf { it.isNotBlank() }
            ?: return true
        return context.checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED
    }

    private fun filterShortcuts(shortcuts: List<StaticShortcut>): List<StaticShortcut> {
        return shortcuts.filter { shortcut ->
            shortcut.enabled &&
                shortcut.intents.isNotEmpty() &&
                canLaunchShortcut(shortcut)
        }
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        private const val META_DATA_SHORTCUTS = "android.app.shortcuts"
    }
}

private const val CHROME_PACKAGE = "com.android.chrome"

private object AppShortcutSpacing {
    val cardHorizontalPadding = DesignTokens.CardHorizontalPadding
    val cardVerticalPadding = DesignTokens.CardVerticalPadding
    val rowSpacing = DesignTokens.ItemRowSpacing
    val messageSpacing = DesignTokens.TextColumnSpacing
    val sectionSpacing = DesignTokens.SpacingSmall
    val iconSize = 32.dp
}

@Composable
fun AppShortcutsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { AppShortcutRepository(context) }
    var isLoading by remember { mutableStateOf(true) }
    var shortcuts by remember { mutableStateOf<List<StaticShortcut>>(emptyList()) }
    val toggleStates = remember { mutableStateMapOf<String, Boolean>() }
    val visibleShortcuts = remember(shortcuts) {
        shortcuts.filterNot { it.packageName == CHROME_PACKAGE }
    }
    val shortcutsByApp = remember(visibleShortcuts) {
        visibleShortcuts.groupBy { it.packageName }
    }
    val shortcutSections = remember(shortcutsByApp) { shortcutsByApp.entries.toList() }
    val appCount = shortcutsByApp.size

    LaunchedEffect(Unit) {
        val cached = repository.loadCachedShortcuts()
        if (cached != null) {
            shortcuts = cached
        }

        isLoading = true
        shortcuts = runCatching { repository.loadStaticShortcuts() }
            .getOrElse { cached ?: emptyList() }
        isLoading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (visibleShortcuts.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(
                        R.string.settings_app_shortcuts_summary,
                        appCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            isLoading && visibleShortcuts.isEmpty() -> {
                item {
                    Text(
                        text = stringResource(R.string.settings_app_shortcuts_loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            visibleShortcuts.isEmpty() -> {
                item {
                    Text(
                        text = stringResource(R.string.settings_app_shortcuts_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            else -> {
                items(
                    items = shortcutSections,
                    key = { it.key }
                ) { entry ->
                    val appShortcuts = entry.value
                    AppShortcutSection(
                        appLabel = appShortcuts.first().appLabel,
                        shortcuts = appShortcuts,
                        toggleStates = toggleStates,
                        onShortcutClick = { shortcut ->
                            launchStaticShortcut(context, shortcut)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppShortcutSection(
    appLabel: String,
    shortcuts: List<StaticShortcut>,
    toggleStates: MutableMap<String, Boolean>,
    onShortcutClick: (StaticShortcut) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppShortcutSpacing.sectionSpacing)) {
        Text(
            text = appLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingsCard {
            Column {
                shortcuts.forEachIndexed { index, shortcut ->
                    val toggleKey = shortcutKey(shortcut)
                    key(toggleKey) {
                        ShortcutRow(
                            shortcut = shortcut,
                            checked = toggleStates[toggleKey] ?: true,
                            onCheckedChange = { enabled ->
                                toggleStates[toggleKey] = enabled
                            },
                            onClick = { onShortcutClick(shortcut) }
                        )
                    }
                    if (index < shortcuts.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    shortcut: StaticShortcut,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val displayName = shortcutDisplayName(shortcut)
    val iconSize = AppShortcutSpacing.iconSize
    val density = LocalDensity.current
    val iconSizePx = remember(iconSize, density) {
        with(density) { iconSize.roundToPx().coerceAtLeast(1) }
    }
    val iconBitmap = rememberShortcutIcon(shortcut, iconSizePx)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = AppShortcutSpacing.cardHorizontalPadding,
                vertical = AppShortcutSpacing.cardVerticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppShortcutSpacing.messageSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppShortcutSpacing.rowSpacing)
        ) {
            ShortcutIcon(
                icon = iconBitmap,
                displayName = displayName,
                size = iconSize
            )

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }

    }
}

@Composable
internal fun ShortcutIcon(
    icon: ImageBitmap?,
    displayName: String,
    size: Dp
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            val fallback = displayName.trim().take(1)
                .uppercase(Locale.getDefault())
                .ifBlank { "?" }
            Text(
                text = fallback,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun rememberShortcutIcon(
    shortcut: StaticShortcut,
    iconSizePx: Int
): ImageBitmap? {
    val context = LocalContext.current
    val iconState = produceState<ImageBitmap?>(
        initialValue = null,
        key1 = shortcut.packageName,
        key2 = shortcut.iconResId,
        key3 = iconSizePx
    ) {
        value = withContext(Dispatchers.IO) {
            loadShortcutIconBitmap(
                context = context,
                shortcut = shortcut,
                iconSizePx = iconSizePx
            )
        }
    }
    return iconState.value
}

private fun loadShortcutIconBitmap(
    context: Context,
    shortcut: StaticShortcut,
    iconSizePx: Int
): ImageBitmap? {
    val resId = shortcut.iconResId ?: return null
    val targetContext = runCatching {
        context.createPackageContext(shortcut.packageName, 0)
    }.getOrNull() ?: return null

    val drawable = runCatching {
        targetContext.resources.getDrawable(resId, targetContext.theme)
    }.getOrNull() ?: return null

    val sizePx = iconSizePx.coerceAtLeast(1)
    return runCatching {
        drawable.toBitmap(width = sizePx, height = sizePx)
            .asImageBitmap()
    }.getOrNull()
}

internal fun shortcutDisplayName(shortcut: StaticShortcut): String {
    return shortcut.shortLabel?.takeIf { it.isNotBlank() }
        ?: shortcut.longLabel?.takeIf { it.isNotBlank() }
        ?: shortcut.id
}

internal fun shortcutKey(shortcut: StaticShortcut): String {
    return "${shortcut.packageName}:${shortcut.id}"
}

private fun isValidShortcutId(id: String): Boolean {
    val trimmed = id.trim()
    if (trimmed.isBlank()) return false
    if (trimmed.startsWith("@")) return false
    if (trimmed.all { it.isDigit() }) return false
    return true
}

internal fun launchStaticShortcut(
    context: Context,
    shortcut: StaticShortcut
): String? {
    if (!shortcut.enabled) {
        return "Shortcut is disabled."
    }

    val baseIntent = shortcut.intents.lastOrNull()
        ?: return "Shortcut has no intent."
    val intent = Intent(baseIntent).apply {
        putExtra(Intent.EXTRA_SHORTCUT_ID, shortcut.id)
    }

    val pm = context.packageManager
    val details = formatIntentDetails(intent)
    val resolved = pm.resolveActivity(intent, 0)
        ?: return "No activity resolves intent.${details.toSuffixDetail()}"

    val activityInfo = resolved.activityInfo
    if (!activityInfo.exported) {
        return "Resolved activity is not exported.${details.toSuffixDetail()}"
    }
    val requiredPermission = activityInfo.permission?.takeIf { it.isNotBlank() }
    if (requiredPermission != null &&
        context.checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED
    ) {
        return "Shortcut requires permission: $requiredPermission.${details.toSuffixDetail()}"
    }

    val error = runCatching { context.startActivity(intent) }.exceptionOrNull()
    if (error != null) {
        return "Failed to launch shortcut: ${error.message ?: "unknown error"}.${details.toSuffixDetail()}"
    }

    return null
}

private fun formatIntentDetails(intent: Intent): String {
    val parts = mutableListOf<String>()
    intent.action?.takeIf { it.isNotBlank() }?.let { parts.add("action=$it") }
    intent.component?.className?.takeIf { it.isNotBlank() }?.let { parts.add("component=$it") }
    intent.`package`?.takeIf { it.isNotBlank() }?.let { parts.add("package=$it") }
    intent.dataString?.takeIf { it.isNotBlank() }?.let { parts.add("data=$it") }
    if (parts.isEmpty()) return ""
    return "(${parts.joinToString(", ")})"
}

private fun String.toSuffixDetail(): String {
    return if (isBlank()) "" else " $this"
}
