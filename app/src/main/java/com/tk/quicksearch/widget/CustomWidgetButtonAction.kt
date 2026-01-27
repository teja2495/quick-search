package com.tk.quicksearch.widget

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import com.tk.quicksearch.search.data.StaticShortcut
import com.tk.quicksearch.search.deviceSettings.DeviceSetting
import com.tk.quicksearch.search.models.AppInfo
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.search.models.DeviceFile
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject

enum class CustomWidgetButtonType(val value: String) {
    APP("app"),
    CONTACT("contact"),
    FILE("file"),
    SETTING("setting"),
    APP_SHORTCUT("app_shortcut")
}

enum class SettingExtraType(val value: String) {
    STRING("string"),
    BOOLEAN("boolean"),
    INT("int"),
    LONG("long")
}

@Parcelize
data class SettingExtra(
    val key: String,
    val type: SettingExtraType,
    val value: String
) : Parcelable {
    fun toTypedValue(): Any = when (type) {
        SettingExtraType.STRING -> value
        SettingExtraType.BOOLEAN -> value.equals("true", ignoreCase = true)
        SettingExtraType.INT -> value.toIntOrNull() ?: 0
        SettingExtraType.LONG -> value.toLongOrNull() ?: 0L
    }
}

sealed class CustomWidgetButtonAction : Parcelable {
    abstract val type: CustomWidgetButtonType
    abstract fun displayLabel(): String
    abstract fun toJson(): String

    open fun contentDescription(): String = displayLabel()

    @Parcelize
    data class App(
        val packageName: String,
        val appName: String
    ) : CustomWidgetButtonAction() {
        override val type: CustomWidgetButtonType = CustomWidgetButtonType.APP

        override fun displayLabel(): String = appName.ifBlank { packageName }

        fun toAppInfo(): AppInfo = AppInfo(
            appName = displayLabel(),
            packageName = packageName,
            lastUsedTime = 0L,
            totalTimeInForeground = 0L,
            launchCount = 0,
            isSystemApp = false
        )

        override fun toJson(): String = JSONObject()
            .put(KEY_TYPE, type.value)
            .put(KEY_PACKAGE_NAME, packageName)
            .put(KEY_APP_NAME, appName)
            .toString()
    }

    @Parcelize
    data class Contact(
        val contactId: Long,
        val lookupKey: String,
        val displayName: String,
        val photoUri: String?
    ) : CustomWidgetButtonAction() {
        override val type: CustomWidgetButtonType = CustomWidgetButtonType.CONTACT

        override fun displayLabel(): String = displayName.ifBlank { lookupKey }

        fun toContactInfo(): ContactInfo = ContactInfo(
            contactId = contactId,
            lookupKey = lookupKey,
            displayName = displayName,
            phoneNumbers = emptyList(),
            photoUri = photoUri
        )

        override fun toJson(): String = JSONObject()
            .put(KEY_TYPE, type.value)
            .put(KEY_CONTACT_ID, contactId)
            .put(KEY_LOOKUP_KEY, lookupKey)
            .put(KEY_DISPLAY_NAME, displayName)
            .put(KEY_PHOTO_URI, photoUri)
            .toString()
    }

    @Parcelize
    data class File(
        val uri: String,
        val displayName: String,
        val mimeType: String?,
        val lastModified: Long,
        val isDirectory: Boolean,
        val relativePath: String?,
        val volumeName: String?
    ) : CustomWidgetButtonAction() {
        override val type: CustomWidgetButtonType = CustomWidgetButtonType.FILE

        override fun displayLabel(): String = displayName.ifBlank { uri }

        fun toDeviceFile(): DeviceFile = DeviceFile(
            uri = Uri.parse(uri),
            displayName = displayName,
            mimeType = mimeType,
            lastModified = lastModified,
            isDirectory = isDirectory,
            relativePath = relativePath,
            volumeName = volumeName
        )

        override fun toJson(): String = JSONObject()
            .put(KEY_TYPE, type.value)
            .put(KEY_URI, uri)
            .put(KEY_DISPLAY_NAME, displayName)
            .put(KEY_MIME_TYPE, mimeType)
            .put(KEY_LAST_MODIFIED, lastModified)
            .put(KEY_IS_DIRECTORY, isDirectory)
            .put(KEY_RELATIVE_PATH, relativePath)
            .put(KEY_VOLUME_NAME, volumeName)
            .toString()
    }

    @Parcelize
    data class Setting(
        val id: String,
        val title: String,
        val description: String?,
        val keywords: List<String>,
        val action: String,
        val data: String?,
        val categories: List<String>,
        val extras: List<SettingExtra>,
        val minSdk: Int,
        val maxSdk: Int
    ) : CustomWidgetButtonAction() {
        override val type: CustomWidgetButtonType = CustomWidgetButtonType.SETTING

        override fun displayLabel(): String = title.ifBlank { id }

        fun toDeviceSetting(): DeviceSetting = DeviceSetting(
            id = id,
            title = title,
            description = description,
            keywords = keywords,
            action = action,
            data = data,
            categories = categories,
            extras = extras.associate { it.key to it.toTypedValue() },
            minSdk = minSdk,
            maxSdk = maxSdk
        )

        override fun toJson(): String = JSONObject()
            .put(KEY_TYPE, type.value)
            .put(KEY_SETTING_ID, id)
            .put(KEY_TITLE, title)
            .put(KEY_DESCRIPTION, description)
            .put(KEY_KEYWORDS, JSONArray(keywords))
            .put(KEY_ACTION, action)
            .put(KEY_DATA, data)
            .put(KEY_CATEGORIES, JSONArray(categories))
            .put(KEY_EXTRAS, settingExtrasToJson(extras))
            .put(KEY_MIN_SDK, minSdk)
            .put(KEY_MAX_SDK, maxSdk)
            .toString()
    }

    @Parcelize
    data class AppShortcut(
        val packageName: String,
        val appLabel: String,
        val id: String,
        val shortLabel: String?,
        val longLabel: String?,
        val iconResId: Int?,
        val enabled: Boolean,
        val intents: List<Intent>
    ) : CustomWidgetButtonAction() {
        override val type: CustomWidgetButtonType = CustomWidgetButtonType.APP_SHORTCUT

        override fun displayLabel(): String {
            return shortLabel?.takeIf { it.isNotBlank() }
                ?: longLabel?.takeIf { it.isNotBlank() }
                ?: id
        }

        fun toStaticShortcut(): StaticShortcut = StaticShortcut(
            packageName = packageName,
            appLabel = appLabel,
            id = id,
            shortLabel = shortLabel,
            longLabel = longLabel,
            iconResId = iconResId,
            enabled = enabled,
            intents = intents
        )

        override fun toJson(): String = JSONObject()
            .put(KEY_TYPE, type.value)
            .put(KEY_PACKAGE_NAME, packageName)
            .put(KEY_APP_LABEL, appLabel)
            .put(KEY_SHORTCUT_ID, id)
            .put(KEY_SHORT_LABEL, shortLabel)
            .put(KEY_LONG_LABEL, longLabel)
            .put(KEY_ICON_RES_ID, iconResId)
            .put(KEY_ENABLED, enabled)
            .put(KEY_INTENTS, intentsToJson(intents))
            .toString()
    }

    companion object {
        fun fromJson(raw: String?): CustomWidgetButtonAction? {
            if (raw.isNullOrBlank()) return null
            return runCatching {
                val json = JSONObject(raw)
                when (json.optString(KEY_TYPE)) {
                    CustomWidgetButtonType.APP.value -> {
                        val packageName = json.optString(KEY_PACKAGE_NAME)
                        if (packageName.isBlank()) return@runCatching null
                        App(
                            packageName = packageName,
                            appName = json.optString(KEY_APP_NAME, packageName)
                                .nullIfBlankOrLiteralNull()
                                ?: packageName
                        )
                    }
                    CustomWidgetButtonType.CONTACT.value -> {
                        val contactId = json.optLong(KEY_CONTACT_ID, -1L)
                        val lookupKey = json.optString(KEY_LOOKUP_KEY)
                        if (contactId <= 0L || lookupKey.isBlank()) return@runCatching null
                        Contact(
                            contactId = contactId,
                            lookupKey = lookupKey,
                            displayName = json.optString(KEY_DISPLAY_NAME, lookupKey)
                                .nullIfBlankOrLiteralNull()
                                ?: lookupKey,
                            photoUri = json.optString(KEY_PHOTO_URI).nullIfBlankOrLiteralNull()
                        )
                    }
                    CustomWidgetButtonType.FILE.value -> {
                        val uri = json.optString(KEY_URI)
                        if (uri.isBlank()) return@runCatching null
                        File(
                            uri = uri,
                            displayName = json.optString(KEY_DISPLAY_NAME, uri)
                                .nullIfBlankOrLiteralNull()
                                ?: uri,
                            mimeType = json.optString(KEY_MIME_TYPE).nullIfBlankOrLiteralNull(),
                            lastModified = json.optLong(KEY_LAST_MODIFIED, 0L),
                            isDirectory = json.optBoolean(KEY_IS_DIRECTORY, false),
                            relativePath = json.optString(KEY_RELATIVE_PATH).nullIfBlankOrLiteralNull(),
                            volumeName = json.optString(KEY_VOLUME_NAME).nullIfBlankOrLiteralNull()
                        )
                    }
                    CustomWidgetButtonType.SETTING.value -> {
                        val id = json.optString(KEY_SETTING_ID)
                        val action = json.optString(KEY_ACTION)
                        if (id.isBlank() || action.isBlank()) return@runCatching null
                        Setting(
                            id = id,
                            title = json.optString(KEY_TITLE, id)
                                .nullIfBlankOrLiteralNull()
                                ?: id,
                            description = json.optString(KEY_DESCRIPTION).nullIfBlankOrLiteralNull(),
                            keywords = jsonArrayToList(json.optJSONArray(KEY_KEYWORDS)),
                            action = action,
                            data = json.optString(KEY_DATA).nullIfBlankOrLiteralNull(),
                            categories = jsonArrayToList(json.optJSONArray(KEY_CATEGORIES)),
                            extras = jsonToSettingExtras(json.optJSONArray(KEY_EXTRAS)),
                            minSdk = json.optInt(KEY_MIN_SDK, 0),
                            maxSdk = json.optInt(KEY_MAX_SDK, Int.MAX_VALUE)
                        )
                    }
                    CustomWidgetButtonType.APP_SHORTCUT.value -> {
                        val packageName = json.optString(KEY_PACKAGE_NAME)
                        val id = json.optString(KEY_SHORTCUT_ID)
                        if (packageName.isBlank() || id.isBlank()) return@runCatching null
                        AppShortcut(
                            packageName = packageName,
                            appLabel = json.optString(KEY_APP_LABEL, packageName)
                                .nullIfBlankOrLiteralNull()
                                ?: packageName,
                            id = id,
                            shortLabel = json.optString(KEY_SHORT_LABEL).nullIfBlankOrLiteralNull(),
                            longLabel = json.optString(KEY_LONG_LABEL).nullIfBlankOrLiteralNull(),
                            iconResId = if (json.has(KEY_ICON_RES_ID)) {
                                json.optInt(KEY_ICON_RES_ID)
                            } else {
                                null
                            },
                            enabled = json.optBoolean(KEY_ENABLED, true),
                            intents = jsonToIntents(json.optJSONArray(KEY_INTENTS))
                        )
                    }
                    else -> null
                }
            }.getOrNull()
        }
    }
}

private const val KEY_TYPE = "type"
private const val KEY_PACKAGE_NAME = "packageName"
private const val KEY_APP_NAME = "appName"
private const val KEY_APP_LABEL = "appLabel"
private const val KEY_CONTACT_ID = "contactId"
private const val KEY_LOOKUP_KEY = "lookupKey"
private const val KEY_DISPLAY_NAME = "displayName"
private const val KEY_PHOTO_URI = "photoUri"
private const val KEY_URI = "uri"
private const val KEY_MIME_TYPE = "mimeType"
private const val KEY_LAST_MODIFIED = "lastModified"
private const val KEY_IS_DIRECTORY = "isDirectory"
private const val KEY_RELATIVE_PATH = "relativePath"
private const val KEY_VOLUME_NAME = "volumeName"
private const val KEY_SETTING_ID = "settingId"
private const val KEY_TITLE = "title"
private const val KEY_DESCRIPTION = "description"
private const val KEY_KEYWORDS = "keywords"
private const val KEY_ACTION = "action"
private const val KEY_DATA = "data"
private const val KEY_CATEGORIES = "categories"
private const val KEY_EXTRAS = "extras"
private const val KEY_MIN_SDK = "minSdk"
private const val KEY_MAX_SDK = "maxSdk"
private const val KEY_SHORTCUT_ID = "shortcutId"
private const val KEY_SHORT_LABEL = "shortLabel"
private const val KEY_LONG_LABEL = "longLabel"
private const val KEY_ICON_RES_ID = "iconResId"
private const val KEY_ENABLED = "enabled"
private const val KEY_INTENTS = "intents"

private const val KEY_INTENT_ACTION = "action"
private const val KEY_INTENT_TARGET_PACKAGE = "targetPackage"
private const val KEY_INTENT_TARGET_CLASS = "targetClass"
private const val KEY_INTENT_DATA = "data"
private const val KEY_INTENT_EXTRAS = "extras"
private const val KEY_EXTRA_NAME = "name"
private const val KEY_EXTRA_TYPE = "type"
private const val KEY_EXTRA_VALUE = "value"

private const val EXTRA_TYPE_STRING = "string"
private const val EXTRA_TYPE_BOOLEAN = "boolean"
private const val EXTRA_TYPE_INT = "int"
private const val EXTRA_TYPE_LONG = "long"
private const val EXTRA_TYPE_FLOAT = "float"
private const val EXTRA_TYPE_DOUBLE = "double"
private const val EXTRA_TYPE_URI = "uri"

private fun jsonArrayToList(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return List(array.length()) { index -> array.optString(index) }
}

private fun settingExtrasToJson(extras: List<SettingExtra>): JSONArray {
    return JSONArray().apply {
        extras.forEach { extra ->
            put(
                JSONObject()
                    .put(KEY_EXTRA_NAME, extra.key)
                    .put(KEY_EXTRA_TYPE, extra.type.value)
                    .put(KEY_EXTRA_VALUE, extra.value)
            )
        }
    }
}

private fun jsonToSettingExtras(array: JSONArray?): List<SettingExtra> {
    if (array == null) return emptyList()
    val extras = mutableListOf<SettingExtra>()
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val key = item.optString(KEY_EXTRA_NAME)
        if (key.isBlank()) continue
        val type = when (item.optString(KEY_EXTRA_TYPE)) {
            SettingExtraType.BOOLEAN.value -> SettingExtraType.BOOLEAN
            SettingExtraType.INT.value -> SettingExtraType.INT
            SettingExtraType.LONG.value -> SettingExtraType.LONG
            else -> SettingExtraType.STRING
        }
        extras.add(
            SettingExtra(
                key = key,
                type = type,
                value = item.optString(KEY_EXTRA_VALUE, "")
            )
        )
    }
    return extras
}

private fun intentsToJson(intents: List<Intent>): JSONArray {
    return JSONArray().apply {
        intents.forEach { intent ->
            val component = intent.component
            val targetPackage = component?.packageName ?: intent.`package`
            val targetClass = component?.className
            val json = JSONObject()
            if (!intent.action.isNullOrBlank()) {
                json.put(KEY_INTENT_ACTION, intent.action)
            }
            if (!targetPackage.isNullOrBlank()) {
                json.put(KEY_INTENT_TARGET_PACKAGE, targetPackage)
            }
            if (!targetClass.isNullOrBlank()) {
                json.put(KEY_INTENT_TARGET_CLASS, targetClass)
            }
            if (!intent.dataString.isNullOrBlank()) {
                json.put(KEY_INTENT_DATA, intent.dataString)
            }
            val extrasJson = bundleToJson(intent.extras)
            if (extrasJson != null && extrasJson.length() > 0) {
                json.put(KEY_INTENT_EXTRAS, extrasJson)
            }
            put(json)
        }
    }
}

private fun jsonToIntents(array: JSONArray?): List<Intent> {
    if (array == null) return emptyList()
    val intents = mutableListOf<Intent>()
    for (index in 0 until array.length()) {
        val json = array.optJSONObject(index) ?: continue
        val intent = Intent()
        json.optString(KEY_INTENT_ACTION).takeIf { it.isNotBlank() }?.let { intent.action = it }
        val targetPackage = json.optString(KEY_INTENT_TARGET_PACKAGE).takeIf { it.isNotBlank() }
        val targetClass = json.optString(KEY_INTENT_TARGET_CLASS).takeIf { it.isNotBlank() }
        if (targetPackage != null) {
            if (targetClass != null) {
                intent.setClassName(targetPackage, targetClass)
            } else {
                intent.`package` = targetPackage
            }
        }
        json.optString(KEY_INTENT_DATA).takeIf { it.isNotBlank() }?.let { data ->
            intent.data = Uri.parse(data)
        }
        val extrasJson = json.optJSONArray(KEY_INTENT_EXTRAS)
        if (extrasJson != null) {
            jsonToBundle(extrasJson).forEach { (key, value) ->
                when (value) {
                    is String -> intent.putExtra(key, value)
                    is Boolean -> intent.putExtra(key, value)
                    is Int -> intent.putExtra(key, value)
                    is Long -> intent.putExtra(key, value)
                    is Float -> intent.putExtra(key, value)
                    is Double -> intent.putExtra(key, value)
                    is Uri -> intent.putExtra(key, value)
                }
            }
        }
        intents.add(intent)
    }
    return intents
}

private fun bundleToJson(bundle: android.os.Bundle?): JSONArray? {
    if (bundle == null || bundle.isEmpty) return null
    val extras = JSONArray()
    for (key in bundle.keySet()) {
        val value = bundle.get(key) ?: continue
        val (type, encoded) = when (value) {
            is String -> EXTRA_TYPE_STRING to value
            is Boolean -> EXTRA_TYPE_BOOLEAN to value.toString()
            is Int -> EXTRA_TYPE_INT to value.toString()
            is Long -> EXTRA_TYPE_LONG to value.toString()
            is Float -> EXTRA_TYPE_FLOAT to value.toString()
            is Double -> EXTRA_TYPE_DOUBLE to value.toString()
            is Uri -> EXTRA_TYPE_URI to value.toString()
            else -> null to null
        }
        if (type == null || encoded == null) continue
        extras.put(
            JSONObject()
                .put(KEY_EXTRA_NAME, key)
                .put(KEY_EXTRA_TYPE, type)
                .put(KEY_EXTRA_VALUE, encoded)
        )
    }
    return extras
}

private fun jsonToBundle(array: JSONArray): Map<String, Any> {
    val extras = mutableMapOf<String, Any>()
    for (index in 0 until array.length()) {
        val json = array.optJSONObject(index) ?: continue
        val name = json.optString(KEY_EXTRA_NAME)
        if (name.isBlank()) continue
        val type = json.optString(KEY_EXTRA_TYPE)
        val value = json.optString(KEY_EXTRA_VALUE)
        val parsed: Any? = when (type) {
            EXTRA_TYPE_STRING -> value
            EXTRA_TYPE_BOOLEAN -> value.equals("true", ignoreCase = true)
            EXTRA_TYPE_INT -> value.toIntOrNull()
            EXTRA_TYPE_LONG -> value.toLongOrNull()
            EXTRA_TYPE_FLOAT -> value.toFloatOrNull()
            EXTRA_TYPE_DOUBLE -> value.toDoubleOrNull()
            EXTRA_TYPE_URI -> runCatching { Uri.parse(value) }.getOrNull()
            else -> null
        }
        if (parsed != null) {
            extras[name] = parsed
        }
    }
    return extras
}

private fun String.nullIfBlankOrLiteralNull(): String? {
    val trimmed = trim()
    return if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) {
        null
    } else {
        this
    }
}
