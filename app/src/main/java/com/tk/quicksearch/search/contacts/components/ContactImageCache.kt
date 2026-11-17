package com.tk.quicksearch.search.contacts.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.data.ContactsChangeTracker
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Process-wide caches for contact row images: avatar photos decoded at the size they are drawn,
 * and the app icons shown on contact-method buttons. Cleared by `UiSurfaceMemoryManager`.
 */
internal object ContactImageCache {
    private const val PHOTO_CACHE_BYTES = 4 * 1024 * 1024
    private const val APP_ICON_CACHE_ENTRIES = 24

    private val photos =
        object : LruCache<String, ImageBitmap>(PHOTO_CACHE_BYTES) {
            override fun sizeOf(
                key: String,
                value: ImageBitmap,
            ): Int = value.width * value.height * 4
        }
    private val appIcons = LruCache<String, ImageBitmap>(APP_ICON_CACHE_ENTRIES)

    @Volatile private var photoGeneration = Long.MIN_VALUE

    private fun photoKey(
        photoUri: String,
        sizePx: Int,
    ) = "$sizePx|$photoUri"

    /** A cached photo, or null when it has not been decoded at [sizePx] since contacts last changed. */
    fun cachedPhoto(
        photoUri: String,
        sizePx: Int,
    ): ImageBitmap? {
        if (photoGeneration != ContactsChangeTracker.currentGeneration()) return null
        return photos.get(photoKey(photoUri, sizePx))
    }

    /** Decodes [photoUri] no larger than needed to fill [sizePx]. Call off the main thread. */
    fun loadPhoto(
        context: Context,
        photoUri: String,
        sizePx: Int,
    ): ImageBitmap? {
        val generation = ContactsChangeTracker.currentGeneration()
        if (photoGeneration != generation) {
            // A contact edit can keep its photo URI while replacing the image.
            photos.evictAll()
            photoGeneration = generation
        }
        val key = photoKey(photoUri, sizePx)
        photos.get(key)?.let { return it }
        val photo = decodeSampled(context, Uri.parse(photoUri), sizePx) ?: return null
        if (ContactsChangeTracker.currentGeneration() == generation) photos.put(key, photo)
        return photo
    }

    fun cachedAppIcon(packageName: String): ImageBitmap? = appIcons.get(packageName)

    /** Loads [packageName]'s launcher icon. Call off the main thread. */
    fun loadAppIcon(
        context: Context,
        packageName: String,
    ): ImageBitmap? {
        appIcons.get(packageName)?.let { return it }
        val icon =
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap()
                    .asImageBitmap()
            }.getOrNull() ?: return null
        appIcons.put(packageName, icon)
        return icon
    }

    fun clear() {
        photos.evictAll()
        appIcons.evictAll()
    }

    private fun decodeSampled(
        context: Context,
        uri: Uri,
        sizePx: Int,
    ): ImageBitmap? =
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val sourceMin = minOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            if (sourceMin > 0) {
                while (sourceMin / (sampleSize * 2) >= sizePx) sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded =
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: return@runCatching null
            scaleToFill(decoded, sizePx).asImageBitmap()
        }.getOrNull()

    /** Scales so the shorter side matches [sizePx]; the avatar crops the rest. */
    private fun scaleToFill(
        bitmap: Bitmap,
        sizePx: Int,
    ): Bitmap {
        val shorterSide = minOf(bitmap.width, bitmap.height)
        if (shorterSide <= sizePx) return bitmap
        val scale = sizePx.toFloat() / shorterSide
        val width = max(1, (bitmap.width * scale).roundToInt())
        val height = max(1, (bitmap.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
