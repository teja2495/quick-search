package com.tk.quicksearch.app

import com.tk.quicksearch.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class PastReleaseNotes(
    val versionName: String,
    val markdown: String,
)

class ReleaseNotesRepository {
    suspend fun getPastReleaseNotes(currentVersionName: String?): List<PastReleaseNotes> {
        val responseBody = executeCancellable(releasesRequest) ?: return emptyList()
        val releases = JSONArray(responseBody)
        val normalizedCurrentVersion = currentVersionName?.normalizeVersionName()

        return buildList {
            for (index in 0 until releases.length()) {
                val release = releases.getJSONObject(index)
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue

                val versionName = release.getString("tag_name").normalizeVersionName()
                // Temporarily hide the skipped Play release; its changes are included in 4.7.
                if (BuildConfig.FLAVOR == "standard" && versionName == "4.6") continue
                if (normalizedCurrentVersion == null || !isVersionBefore(versionName, normalizedCurrentVersion)) continue

                add(
                    PastReleaseNotes(
                        versionName = versionName,
                        markdown = release.optString("body"),
                    ),
                )
            }
        }
    }

    private suspend fun executeCancellable(request: Request): String? =
        suspendCancellableCoroutine { continuation ->
            val call: Call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) continuation.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            val body = if (it.isSuccessful) it.body?.string() else null
                            if (continuation.isActive) continuation.resume(body)
                        }
                    }
                },
            )
        }

    private companion object {
        private val client =
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build()
        private val releasesRequest =
            Request.Builder()
                .url("https://api.github.com/repos/teja2495/quick-search/releases?per_page=10")
                .header("Accept", "application/vnd.github+json")
                .build()
    }
}

private fun String.normalizeVersionName(): String = removePrefix("v").trim()

internal fun isVersionBefore(versionName: String, currentVersionName: String): Boolean {
    val versionParts = versionName.split('.').map { it.toIntOrNull() ?: 0 }
    val currentVersionParts = currentVersionName.split('.').map { it.toIntOrNull() ?: 0 }
    val partCount = maxOf(versionParts.size, currentVersionParts.size)

    for (index in 0 until partCount) {
        val comparison =
            versionParts.getOrElse(index) { 0 }.compareTo(currentVersionParts.getOrElse(index) { 0 })
        if (comparison != 0) return comparison < 0
    }

    return false
}
