package com.tk.quicksearch.search.data.bookmarks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import com.tk.quicksearch.searchEngines.downloadImageBytes
import com.tk.quicksearch.searchEngines.normalizeImage
import com.tk.quicksearch.searchEngines.readBytesUpTo
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Bookmark favicons, stored as small PNG files in app storage and keyed by bookmark ID.
 *
 * Icons are fetched only from the bookmarked site itself (its declared icons, then /favicon.ico);
 * no third-party favicon service sees the user's bookmarks.
 */
class BookmarkFaviconStore(context: Context) {
    private val directory = File(context.applicationContext.filesDir, DIRECTORY_NAME)

    fun fileFor(bookmarkId: Long): File = File(directory, "$bookmarkId.png")

    /** Normalizes and saves image bytes. Returns false when they are not a decodable image. */
    fun save(bookmarkId: Long, imageBytes: ByteArray): Boolean {
        val png = normalizeImage(imageBytes, maxSizePx = MAX_ICON_SIZE_PX) ?: return false
        return runCatching {
            directory.mkdirs()
            val target = fileFor(bookmarkId)
            val temp = File(directory, "$bookmarkId.png.tmp")
            temp.writeBytes(png)
            if (!temp.renameTo(target)) {
                target.writeBytes(png)
                temp.delete()
            }
            memoryCache.evictBookmark(bookmarkId)
            true
        }.getOrDefault(false)
    }

    fun saveBase64(bookmarkId: Long, base64: String): Boolean {
        val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return false
        return save(bookmarkId, bytes)
    }

    /** The stored favicon as base64 PNG data, or null when there is none. Call off the main thread. */
    fun readBase64(bookmarkId: Long): String? =
        runCatching { fileFor(bookmarkId).takeIf { it.isFile }?.readBytes() }
            .getOrNull()
            ?.let { Base64.encodeToString(it, Base64.NO_WRAP) }

    fun delete(bookmarkId: Long) {
        fileFor(bookmarkId).delete()
        memoryCache.evictBookmark(bookmarkId)
    }

    fun deleteAll() {
        directory.listFiles()?.forEach { it.delete() }
        memoryCache.evictAll()
    }

    /** Decodes a stored favicon. Call off the main thread. */
    fun loadBitmap(bookmarkId: Long, faviconVersion: Long): Bitmap? {
        if (faviconVersion <= 0L) return null
        val key = cacheKey(bookmarkId, faviconVersion)
        memoryCache.get(key)?.let { return it }
        val file = fileFor(bookmarkId)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        memoryCache.put(key, bitmap)
        return bitmap
    }

    /** Downloads the site's favicon and saves it. Blocking network work; call off the main thread. */
    fun fetchAndSave(bookmarkId: Long, url: String): Boolean {
        val origin = BookmarkUrls.originOf(url) ?: return false
        val candidates =
            buildList {
                addAll(declaredIconUrls(url))
                add("$origin/favicon.ico")
                add("$origin/apple-touch-icon.png")
            }.distinct()
        for (candidate in candidates) {
            val bytes = runCatching { downloadImageBytes(candidate) }.getOrNull() ?: continue
            if (save(bookmarkId, bytes)) return true
        }
        return false
    }

    /** Icon URLs from the page's <link rel="...icon..."> tags, largest declared size first. */
    private fun declaredIconUrls(pageUrl: String): List<String> {
        val html = downloadPageHead(pageUrl) ?: return emptyList()
        return linkTagRegex.findAll(html)
            .mapNotNull { match ->
                val tag = match.value
                val rel = attribute(tag, "rel")?.lowercase() ?: return@mapNotNull null
                if (!rel.split(' ').any { it == "icon" || it == "apple-touch-icon" }) return@mapNotNull null
                val href = attribute(tag, "href")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                if (href.endsWith(".svg", ignoreCase = true) || href.startsWith("data:")) return@mapNotNull null
                val resolved = runCatching { URL(URL(pageUrl), href).toString() }.getOrNull()
                    ?: return@mapNotNull null
                val size =
                    attribute(tag, "sizes")
                        ?.substringBefore('x')
                        ?.toIntOrNull()
                        ?: if (rel.contains("apple-touch-icon")) APPLE_TOUCH_ICON_SIZE else 0
                resolved to size
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .toList()
    }

    private fun downloadPageHead(pageUrl: String): String? {
        val connection = runCatching { URL(pageUrl).openConnection() as? HttpURLConnection }.getOrNull()
            ?: return null
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", "QuickSearch/1.0")
        return try {
            if (connection.responseCode !in 200..299) return null
            val contentType = connection.contentType.orEmpty().lowercase()
            if (contentType.isNotEmpty() && !contentType.contains("html")) return null
            val bytes = connection.inputStream.use { it.readBytesUpTo(maxBytes = MAX_PAGE_BYTES) }
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun attribute(tag: String, name: String): String? =
        Regex("\\b$name\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE)
            .find(tag)
            ?.let { it.groups[2]?.value ?: it.groups[3]?.value ?: it.groups[4]?.value }

    private companion object {
        const val DIRECTORY_NAME = "bookmark_favicons"
        const val MAX_ICON_SIZE_PX = 96
        const val APPLE_TOUCH_ICON_SIZE = 180
        const val MAX_PAGE_BYTES = 256 * 1024
        const val TIMEOUT_MILLIS = 5000
        val linkTagRegex = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)

        fun cacheKey(bookmarkId: Long, faviconVersion: Long) = "$bookmarkId:$faviconVersion"

        private val memoryCache =
            object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
                override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount

                fun evictBookmark(bookmarkId: Long) {
                    snapshot().keys.filter { it.startsWith("$bookmarkId:") }.forEach { remove(it) }
                }
            }
    }
}
