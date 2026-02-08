package com.tk.quicksearch.search.searchEngines

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Patterns
import android.util.Base64
import com.tk.quicksearch.search.core.CustomSearchEngine
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

const val CUSTOM_QUERY_PLACEHOLDER = "{{query}}"
const val CUSTOM_ID_PREFIX = "custom:"

sealed class CustomSearchTemplateValidation {
    data class Valid(
        val normalizedTemplate: String,
    ) : CustomSearchTemplateValidation()

    data class Invalid(
        val reason: Reason,
    ) : CustomSearchTemplateValidation()

    enum class Reason {
        EMPTY,
        INVALID_URL,
        UNSUPPORTED_SCHEME,
        MISSING_QUERY_PLACEHOLDER,
        MULTIPLE_QUERY_PLACEHOLDERS,
    }
}

fun validateCustomSearchTemplate(input: String): CustomSearchTemplateValidation {
    val candidate = input.trim()
    if (candidate.isEmpty()) {
        return CustomSearchTemplateValidation.Invalid(CustomSearchTemplateValidation.Reason.EMPTY)
    }

    val occurrences =
        "(\\{\\{query\\}\\})".toRegex(RegexOption.IGNORE_CASE).findAll(candidate).count()
    if (occurrences == 0) {
        return CustomSearchTemplateValidation.Invalid(
            CustomSearchTemplateValidation.Reason.MISSING_QUERY_PLACEHOLDER,
        )
    }
    if (occurrences > 1) {
        return CustomSearchTemplateValidation.Invalid(
            CustomSearchTemplateValidation.Reason.MULTIPLE_QUERY_PLACEHOLDERS,
        )
    }

    val normalizedTemplate =
        candidate.replace(
            "(\\{\\{query\\}\\})".toRegex(RegexOption.IGNORE_CASE),
            CUSTOM_QUERY_PLACEHOLDER,
        )

    val templateWithScheme =
        if (normalizedTemplate.startsWith("http://", ignoreCase = true) ||
            normalizedTemplate.startsWith("https://", ignoreCase = true)
        ) {
            normalizedTemplate
        } else {
            "https://$normalizedTemplate"
        }

    val testUrl = templateWithScheme.replace(CUSTOM_QUERY_PLACEHOLDER, "test")
    val uri = runCatching { Uri.parse(testUrl) }.getOrNull()
        ?: return CustomSearchTemplateValidation.Invalid(CustomSearchTemplateValidation.Reason.INVALID_URL)
    val scheme = uri.scheme?.lowercase(Locale.getDefault())
    if (scheme != "https" && scheme != "http") {
        return CustomSearchTemplateValidation.Invalid(CustomSearchTemplateValidation.Reason.UNSUPPORTED_SCHEME)
    }
    if (uri.host.isNullOrBlank() || !Patterns.WEB_URL.matcher(testUrl).matches()) {
        return CustomSearchTemplateValidation.Invalid(CustomSearchTemplateValidation.Reason.INVALID_URL)
    }

    return CustomSearchTemplateValidation.Valid(templateWithScheme)
}

fun buildCustomSearchUrl(
    query: String,
    urlTemplate: String,
): String {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isBlank()) {
        val parts = urlTemplate.split("?", limit = 2)
        if (parts.size == 1) {
            return parts[0].replace(CUSTOM_QUERY_PLACEHOLDER, "")
        }

        val baseUrl = parts[0].replace(CUSTOM_QUERY_PLACEHOLDER, "")
        val queryString = parts[1]
        val params =
            queryString
                .split("&")
                .filter { !it.contains(CUSTOM_QUERY_PLACEHOLDER) && it.isNotBlank() }

        return if (params.isEmpty()) {
            baseUrl
        } else {
            "$baseUrl?${params.joinToString("&")}"
        }
    }

    return urlTemplate.replace(CUSTOM_QUERY_PLACEHOLDER, Uri.encode(trimmedQuery))
}

fun createCustomSearchEngine(
    normalizedTemplate: String,
    faviconBase64: String?,
): CustomSearchEngine? {
    val validated = validateCustomSearchTemplate(normalizedTemplate)
    if (validated !is CustomSearchTemplateValidation.Valid) {
        return null
    }

    val host = Uri.parse(validated.normalizedTemplate).host?.removePrefix("www.")
    val name = host?.let(::formatHostNameForDisplay) ?: "Custom Search"
    val id = "custom_${sha256Hex(validated.normalizedTemplate).take(12)}"

    return CustomSearchEngine(
        id = id,
        name = name,
        urlTemplate = validated.normalizedTemplate,
        faviconBase64 = faviconBase64,
    )
}

fun fetchFaviconAsBase64(normalizedTemplate: String): String? {
    val validated = validateCustomSearchTemplate(normalizedTemplate)
    if (validated !is CustomSearchTemplateValidation.Valid) {
        return null
    }

    val uri = Uri.parse(validated.normalizedTemplate)
    val host = uri.host ?: return null

    val updatedHost = host.removePrefix("www.")
    val googleFaviconUrl = "https://www.google.com/s2/favicons?sz=128&domain=$updatedHost"
    val googleBytes = downloadImageBytes(googleFaviconUrl)
    if (googleBytes != null) {
        val normalizedBytes = normalizeImage(googleBytes)
        if (normalizedBytes != null) {
            return Base64.encodeToString(normalizedBytes, Base64.NO_WRAP)
        }
    }

    return null
}

private fun downloadImageBytes(url: String): ByteArray? {
    val connection = (URL(url).openConnection() as? HttpURLConnection) ?: return null
    connection.connectTimeout = 5000
    connection.readTimeout = 5000
    connection.instanceFollowRedirects = true
    connection.requestMethod = "GET"
    connection.setRequestProperty("User-Agent", "QuickSearch/1.0")

    return try {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            null
        } else {
            val contentType = connection.contentType.orEmpty().lowercase(Locale.getDefault())
            if (contentType.isNotEmpty() &&
                !contentType.startsWith("image/") &&
                contentType != "application/octet-stream"
            ) {
                null
            } else {
                connection.inputStream.use { input ->
                    val bytes = input.readBytesUpTo(maxBytes = 200 * 1024)
                    if (bytes.isEmpty()) {
                        null
                    } else {
                        bytes
                    }
                }
            }
        }
    } catch (e: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private fun normalizeImage(input: ByteArray): ByteArray? {
    val bitmap = BitmapFactory.decodeByteArray(input, 0, input.size)
    if (bitmap == null) {
        return null
    }
    return ByteArrayOutputStream().use { out ->
        val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        if (!compressed) {
            null
        } else {
            out.toByteArray()
        }
    }
}

fun loadCustomIconAsBase64(
    context: Context,
    uri: Uri,
): String? {
    val bytes =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytesUpTo(maxBytes = 2 * 1024 * 1024)
            }
        }.getOrNull()
            ?: return null

    val normalizedBytes = normalizeImage(bytes) ?: return null

    return Base64.encodeToString(normalizedBytes, Base64.NO_WRAP)
}

private fun InputStream.readBytesUpTo(maxBytes: Int): ByteArray {
    val buffer = ByteArrayOutputStream()
    val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0

    while (true) {
        val read = read(chunk)
        if (read <= 0) break
        total += read
        if (total > maxBytes) break
        buffer.write(chunk, 0, read)
    }

    return buffer.toByteArray()
}

private fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun formatHostNameForDisplay(host: String): String {
    val primary = host.substringBefore(".").ifBlank { host }
    return primary
        .replace("-", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase(Locale.getDefault()) else first.toString()
            }
        }.ifBlank { host }
}
