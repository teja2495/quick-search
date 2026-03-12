package com.tk.quicksearch.search.data.AppShortcutRepository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.core.AppIconShape
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.shortcutDisplayName
import com.tk.quicksearch.search.searchScreen.LocalAppIconShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun ShortcutIcon(
    icon: ImageBitmap?,
    displayName: String,
    size: Dp,
) {
    val appIconShape = LocalAppIconShape.current
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (icon != null) {
            val clipModifier = if (appIconShape == AppIconShape.CIRCLE)
                Modifier.clip(CircleShape) else Modifier
            Image(
                bitmap = icon,
                contentDescription = displayName,
                modifier = Modifier.fillMaxSize().padding(4.dp).then(clipModifier),
                contentScale = ContentScale.Fit,
            )
        } else {
            val fallback =
                displayName
                    .trim()
                    .take(1)
                    .uppercase(Locale.getDefault())
                    .ifBlank { "?" }
            Text(
                text = fallback,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
internal fun rememberShortcutIcon(
    shortcut: StaticShortcut,
    iconSizePx: Int,
): ImageBitmap? {
    val context = LocalContext.current
    val iconState =
        produceState<ImageBitmap?>(
            initialValue = null,
            key1 = shortcut.packageName,
            key2 = shortcut.iconResId,
            key3 = shortcut.iconBase64 to iconSizePx,
        ) {
            value =
                withContext(Dispatchers.IO) {
                    loadShortcutIconBitmap(
                        context = context,
                        shortcut = shortcut,
                        iconSizePx = iconSizePx,
                    )
                }
        }
    return iconState.value
}

private fun loadShortcutIconBitmap(
    context: Context,
    shortcut: StaticShortcut,
    iconSizePx: Int,
): ImageBitmap? {
    shortcut.iconBase64?.let { encoded ->
        val decoded = kotlin.runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
        val bitmap =
            decoded?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        if (bitmap != null) {
            return bitmap.asImageBitmap()
        }
    }

    val resId = shortcut.iconResId ?: return null
    val targetContext =
        kotlin.runCatching { context.createPackageContext(shortcut.packageName, 0) }.getOrNull()
            ?: return null

    val drawable =
        kotlin.runCatching { targetContext.resources.getDrawable(resId, targetContext.theme) }
            .getOrNull()
            ?: return null

    val sizePx = iconSizePx.coerceAtLeast(1)
    return kotlin.runCatching { drawable.toBitmap(width = sizePx, height = sizePx).asImageBitmap() }
        .getOrNull()
}