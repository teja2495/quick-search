package com.tk.quicksearch.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.material.icons.Icons
import com.tk.quicksearch.R
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.search.managers.IconPackManager
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.search.models.FileTypeUtils

data class WidgetButtonIcon(
    val bitmap: Bitmap? = null,
    val drawableResId: Int? = null,
    val shouldTint: Boolean
)

fun rememberWidgetButtonIcon(
    context: Context,
    action: CustomWidgetButtonAction,
    iconSizePx: Int,
    textIconColor: Color,
    iconPackPackage: String?
): WidgetButtonIcon {
    return when (action) {
        is CustomWidgetButtonAction.App -> {
            val bitmap = loadAppIconBitmap(context, action.packageName, iconSizePx, iconPackPackage)
            bitmap?.let { WidgetButtonIcon(bitmap = it, shouldTint = false) }
                ?: WidgetButtonIcon(drawableResId = R.drawable.ic_widget_search, shouldTint = true)
        }
        is CustomWidgetButtonAction.AppShortcut -> {
            val bitmap = loadShortcutIconBitmap(context, action, iconSizePx)
                ?: loadAppIconBitmap(context, action.packageName, iconSizePx, iconPackPackage)
            bitmap?.let { WidgetButtonIcon(bitmap = it, shouldTint = false) }
                ?: WidgetButtonIcon(drawableResId = R.drawable.ic_widget_search, shouldTint = true)
        }
        is CustomWidgetButtonAction.Contact -> {
            val bitmap = loadContactBitmap(context, action, iconSizePx, textIconColor)
            WidgetButtonIcon(bitmap = bitmap, shouldTint = false)
        }
        is CustomWidgetButtonAction.File -> {
            val drawableResId = when (FileTypeUtils.getFileType(action.toDeviceFile())) {
                FileType.MUSIC -> R.drawable.ic_widget_search // Placeholder, could create specific drawable
                FileType.PICTURES -> R.drawable.ic_widget_search // Placeholder, could create specific drawable
                FileType.VIDEOS -> R.drawable.ic_widget_search // Placeholder, could create specific drawable
                FileType.APKS -> R.drawable.ic_widget_search // Placeholder, could create specific drawable
                else -> R.drawable.ic_widget_search // Generic file icon
            }
            WidgetButtonIcon(drawableResId = drawableResId, shouldTint = true)
        }
        is CustomWidgetButtonAction.Setting -> {
            WidgetButtonIcon(drawableResId = R.drawable.ic_widget_search, shouldTint = true) // Placeholder, could create settings drawable
        }
    }
}

private fun loadAppIconBitmap(
    context: Context,
    packageName: String,
    iconSizePx: Int,
    iconPackPackage: String?
): Bitmap? {
    val iconPackBitmap = iconPackPackage?.let { pack ->
        IconPackManager.loadIconBitmap(context, pack, packageName)
    }
    if (iconPackBitmap != null) {
        return iconPackBitmap.asAndroidBitmap()
    }
    val drawable = runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull()
    return drawable?.toBitmap(
        width = iconSizePx.coerceAtLeast(1),
        height = iconSizePx.coerceAtLeast(1)
    )
}

private fun loadShortcutIconBitmap(
    context: Context,
    action: CustomWidgetButtonAction.AppShortcut,
    iconSizePx: Int
): Bitmap? {
    val resId = action.iconResId ?: return null
    val targetContext = runCatching {
        context.createPackageContext(action.packageName, 0)
    }.getOrNull() ?: return null

    val drawable = runCatching {
        targetContext.resources.getDrawable(resId, targetContext.theme)
    }.getOrNull() ?: return null

    val sizePx = iconSizePx.coerceAtLeast(1)
    return runCatching { drawable.toBitmap(width = sizePx, height = sizePx) }.getOrNull()
}

private fun loadContactBitmap(
    context: Context,
    action: CustomWidgetButtonAction.Contact,
    iconSizePx: Int,
    textIconColor: Color
): Bitmap {
    val photoBitmap = action.photoUri?.let { uriString ->
        runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    if (photoBitmap != null) {
        return Bitmap.createScaledBitmap(
            photoBitmap,
            iconSizePx.coerceAtLeast(1),
            iconSizePx.coerceAtLeast(1),
            true
        )
    }

    val initials = action.displayName
        .trim()
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2)
        .joinToString("")

    if (initials.isBlank()) {
        return createVectorBitmap(Icons.Rounded.Person, iconSizePx)
    }

    val isLightText = textIconColor.luminance() > 0.5f
    val backgroundColor = if (isLightText) Color(0xFF4A4A4A) else Color(0xFFE0E0E0)
    val textColor = if (isLightText) Color.White else Color.Black
    return createInitialsBitmap(initials, iconSizePx, backgroundColor.toArgb(), textColor.toArgb())
}

private fun createInitialsBitmap(
    initials: String,
    sizePx: Int,
    backgroundColor: Int,
    textColor: Int
): Bitmap {
    val safeSize = sizePx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }

    val radius = safeSize / 2f
    canvas.drawCircle(radius, radius, radius, paint)

    if (initials.isNotBlank()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = safeSize * 0.42f
        }
        val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, radius, textY, textPaint)
    }

    return bitmap
}

private fun fileIconVector(action: CustomWidgetButtonAction.File): ImageVector {
    if (action.isDirectory) return Icons.Rounded.Folder
    return when (FileTypeUtils.getFileType(action.toDeviceFile())) {
        FileType.MUSIC -> Icons.Rounded.MusicNote
        FileType.PICTURES -> Icons.Rounded.Image
        FileType.VIDEOS -> Icons.Rounded.VideoLibrary
        FileType.APKS -> Icons.Rounded.Android
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }
}

private fun createVectorBitmap(
    imageVector: ImageVector,
    sizePx: Int
): Bitmap {
    val safeSize = sizePx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // For a simple fallback, create a basic person icon representation
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.Gray.toArgb()
        style = Paint.Style.FILL
    }

    // Draw a simple circle for the head
    val centerX = safeSize / 2f
    val centerY = safeSize / 2f
    val headRadius = safeSize * 0.25f
    canvas.drawCircle(centerX, centerY - safeSize * 0.1f, headRadius, paint)

    // Draw body as a rectangle
    val bodyTop = centerY + headRadius - safeSize * 0.1f
    val bodyBottom = safeSize * 0.9f
    val bodyLeft = centerX - safeSize * 0.15f
    val bodyRight = centerX + safeSize * 0.15f
    canvas.drawRect(bodyLeft, bodyTop, bodyRight, bodyBottom, paint)

    return bitmap
}

@Composable
private fun rememberVectorBitmap(
    imageVector: ImageVector,
    sizePx: Int
): Bitmap {
    val safeSize = sizePx.coerceAtLeast(1)
    val density = LocalDensity.current
    val painter = rememberVectorPainter(imageVector)
    return remember(imageVector, safeSize, density.density, density.fontScale, painter) {
        val imageBitmap = ImageBitmap(safeSize, safeSize)
        val canvas = ComposeCanvas(imageBitmap)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(safeSize.toFloat(), safeSize.toFloat())
        ) {
            with(painter) {
                draw(Size(safeSize.toFloat(), safeSize.toFloat()))
            }
        }
        imageBitmap.asAndroidBitmap()
    }
}
