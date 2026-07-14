package com.tk.quicksearch.search.data.assets

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.MessageDigest

/** Bounded, app-private storage for user-provided image assets. */
class ManagedAssetStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "managed_assets/v1")
    private val index =
        context.applicationContext.getSharedPreferences(INDEX_PREFS_NAME, Context.MODE_PRIVATE)

    fun putBase64(logicalId: String, encoded: String?): Boolean {
        if (encoded.isNullOrBlank()) {
            remove(logicalId)
            return true
        }
        val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull() ?: return false
        if (bytes.isEmpty() || bytes.size > MAX_DECODED_BYTES) return false
        root.mkdirs()
        val hash = bytes.sha256()
        val destination = File(root, hash)
        if (!destination.exists()) {
            val temporary = File(root, ".$hash.tmp")
            temporary.outputStream().use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            if (!temporary.renameTo(destination)) {
                temporary.delete()
                return false
            }
        }
        return index.edit().putString(logicalId, hash).commit()
    }

    fun getBase64(logicalId: String): String? {
        val hash = index.getString(logicalId, null) ?: return null
        val file = File(root, hash)
        if (!file.isFile || file.length() !in 1..MAX_DECODED_BYTES.toLong()) return null
        return runCatching { Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) }.getOrNull()
    }

    fun getAllBase64(prefix: String): Map<String, String> =
        index.all.mapNotNull { (key, _) ->
            if (!key.startsWith(prefix)) return@mapNotNull null
            getBase64(key)?.let { key.removePrefix(prefix) to it }
        }.toMap()

    fun remove(logicalId: String) {
        index.edit().remove(logicalId).commit()
        pruneOrphans()
    }

    fun pruneOrphans() {
        val referenced = index.all.values.filterIsInstance<String>().toSet()
        root.listFiles()?.forEach { file ->
            if (!file.name.startsWith(".") && file.name !in referenced) file.delete()
        }
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

    companion object {
        const val MAX_DECODED_BYTES = 512 * 1024
        const val INDEX_PREFS_NAME = "managed_asset_index"
        const val SHORTCUT_ICON_PREFIX = "shortcut_icon:"
        const val SEARCH_ENGINE_ICON_PREFIX = "search_engine_icon:"
        const val WIDGET_ICON_PREFIX = "widget_icon:"
    }
}
