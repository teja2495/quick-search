package com.tk.quicksearch.shared.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * Decodes the small cached startup background preview off the main thread as soon as the search
 * surface knows its path (while the activity is still being created), so the first composition can
 * draw the image instead of a solid color. Never blocks: [peek] only returns a finished decode.
 *
 * The decoded preview is dropped once the full-size background has loaded ([release]), and whenever
 * the preview file is rewritten or memory caches are cleared, so a stale image can't be reused.
 */
object StartupBackgroundPreview {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var pendingPath: String? = null
    private var pending: Deferred<Bitmap?>? = null

    @Volatile
    private var decodedPath: String? = null

    @Volatile
    private var decodedBitmap: Bitmap? = null

    /** Starts decoding [previewPath] in the background unless that decode is already running or done. */
    fun prefetch(previewPath: String?) {
        val normalized = normalize(previewPath) ?: return
        decodeAsync(normalized)
    }

    /** The decoded preview for [previewPath] if it is already in memory, else null. */
    fun peek(previewPath: String?): Bitmap? {
        val normalized = normalize(previewPath) ?: return null
        val bitmap = decodedBitmap ?: return null
        return bitmap.takeIf { decodedPath == normalized }
    }

    /** Decodes [previewPath], sharing an in-flight or finished prefetch of the same file. */
    suspend fun load(previewPath: String?): Bitmap? {
        val normalized = normalize(previewPath) ?: return null
        peek(normalized)?.let { return it }
        return decodeAsync(normalized).await()
    }

    /** Drops the decoded preview (the full background is showing, or the file changed). */
    fun release() {
        synchronized(lock) {
            pending = null
            pendingPath = null
            decodedBitmap = null
            decodedPath = null
        }
    }

    private fun decodeAsync(path: String): Deferred<Bitmap?> =
        synchronized(lock) {
            pending?.takeIf { pendingPath == path }
                ?: scope.async {
                    val bitmap =
                        runCatching {
                            val file = File(path)
                            if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null
                        }.getOrNull()
                    synchronized(lock) {
                        // Publish only if this decode wasn't released or replaced meanwhile.
                        if (pendingPath == path && bitmap != null) {
                            decodedBitmap = bitmap
                            decodedPath = path
                        }
                    }
                    bitmap
                }.also {
                    pending = it
                    pendingPath = path
                }
        }

    private fun normalize(path: String?): String? = path?.trim()?.takeIf { it.isNotEmpty() }
}
