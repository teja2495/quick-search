package com.tk.quicksearch.widgets.customButtonsWidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.toBitmap
import com.tk.quicksearch.R
import com.tk.quicksearch.search.contacts.contactInitials
import com.tk.quicksearch.search.data.AppShortcutRepository.StaticShortcut
import com.tk.quicksearch.search.data.AppShortcutRepository.loadShortcutIconAndroidBitmap
import com.tk.quicksearch.search.data.UserAppPreferences
import com.tk.quicksearch.search.managers.IconPackManager
import com.tk.quicksearch.search.common.UserHandleUtils
import com.tk.quicksearch.shared.ui.theme.AppColors

data class WidgetButtonIcon(
    val bitmap: Bitmap? = null,
    val drawableResId: Int? = null,
    val shouldTint: Boolean,
)

fun rememberWidgetButtonIcon(
    context: Context,
    action: CustomWidgetButtonAction,
    iconSizePx: Int,
    textIconColor: Color,
    iconPackPackage: String?,
    badgeShortcutWithAppIcon: Boolean = false,
): WidgetButtonIcon {
    // User-set custom icon takes precedence over all type-specific icons.
    action.customIconBase64?.let { encoded ->
        val decoded = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
        val bitmap = decoded?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
        if (bitmap != null) {
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                iconSizePx.coerceAtLeast(1),
                iconSizePx.coerceAtLeast(1),
                true,
            )
            return WidgetButtonIcon(bitmap = scaled, shouldTint = false)
        }
    }

    return when (action) {
        is CustomWidgetButtonAction.App -> {
            val bitmap =
                loadAppIconBitmap(
                    context,
                    action.packageName,
                    iconSizePx,
                    iconPackPackage,
                    action.userHandleId,
                )
            bitmap?.let { WidgetButtonIcon(bitmap = it, shouldTint = false) }
                ?: WidgetButtonIcon(drawableResId = R.drawable.ic_widget_search, shouldTint = true)
        }

        is CustomWidgetButtonAction.AppShortcut -> {
            val shortcutBitmap =
                if (badgeShortcutWithAppIcon) {
                    // Match the app grid, which resolves icons live (LauncherApps, then app
                    // resources) rather than relying only on the persisted embedded icon.
                    loadShortcutIconAndroidBitmap(context, action.toStaticShortcut(), iconSizePx)
                } else {
                    loadShortcutIconBitmap(context, action, iconSizePx)
                }
            val appBitmap = loadAppIconBitmap(context, action.packageName, iconSizePx, iconPackPackage)
            val bitmap =
                if (badgeShortcutWithAppIcon && shortcutBitmap != null && appBitmap != null) {
                    createAppBadgedShortcutBitmap(shortcutBitmap, appBitmap, iconSizePx)
                } else {
                    shortcutBitmap ?: appBitmap
                }
            bitmap?.let { WidgetButtonIcon(bitmap = it, shouldTint = false) }
                ?: WidgetButtonIcon(drawableResId = R.drawable.ic_widget_search, shouldTint = true)
        }

        is CustomWidgetButtonAction.Contact -> {
            val bitmap = loadContactBitmap(context, action, iconSizePx, textIconColor)
            WidgetButtonIcon(bitmap = bitmap, shouldTint = false)
        }

        is CustomWidgetButtonAction.File -> {
            val folderIconColorArgb = action.resolvedFolderIconColorArgb()
            val customFileIconResId = customWidgetFileIconRes(action)
            if (customFileIconResId != null) {
                WidgetButtonIcon(drawableResId = customFileIconResId, shouldTint = false)
            } else if (action.isDirectory && folderIconColorArgb != null) {
                loadTintedFolderBitmap(
                    context = context,
                    iconSizePx = iconSizePx,
                    color = Color(folderIconColorArgb),
                )?.let { bitmap ->
                    WidgetButtonIcon(bitmap = bitmap, shouldTint = false)
                } ?: WidgetButtonIcon(drawableResId = R.drawable.ic_widget_folder, shouldTint = true)
            } else {
                val drawableResId =
                    if (action.isDirectory) {
                        R.drawable.ic_widget_folder
                    } else {
                        R.drawable.ic_widget_file
                    }
                WidgetButtonIcon(drawableResId = drawableResId, shouldTint = true)
            }
        }

        is CustomWidgetButtonAction.Setting -> {
            // Use Material Design settings icon
            WidgetButtonIcon(drawableResId = R.drawable.ic_widget_settings, shouldTint = true)
        }

        is CustomWidgetButtonAction.Note -> {
            WidgetButtonIcon(drawableResId = R.drawable.ic_widget_note, shouldTint = true)
        }
    }
}

private fun CustomWidgetButtonAction.AppShortcut.toStaticShortcut() =
    StaticShortcut(
        packageName = packageName,
        appLabel = appLabel,
        id = id,
        shortLabel = shortLabel,
        longLabel = longLabel,
        iconResId = iconResId,
        iconBase64 = iconBase64,
        enabled = enabled,
        intents = intents,
    )

/**
 * Mirrors the app grid's shortcut presentation: the shortcut icon with the owning app's icon
 * badged at the bottom-right. The main icon is shrunk slightly so the badge's outward offset
 * stays within the bitmap bounds.
 */
private fun createAppBadgedShortcutBitmap(
    shortcutBitmap: Bitmap,
    appBitmap: Bitmap,
    iconSizePx: Int,
): Bitmap {
    val size = iconSizePx.coerceAtLeast(1)
    val mainSize = size / (1f + ShortcutAppBadgeScale * ShortcutBadgeOffsetScale)
    val badgeSize = mainSize * ShortcutAppBadgeScale
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(shortcutBitmap, null, android.graphics.RectF(0f, 0f, mainSize, mainSize), paint)
    canvas.drawBitmap(
        appBitmap,
        null,
        android.graphics.RectF(size - badgeSize, size - badgeSize, size.toFloat(), size.toFloat()),
        paint,
    )
    return output
}

private const val ShortcutAppBadgeScale = 0.42f
private const val ShortcutBadgeOffsetScale = 0.2f

private fun loadTintedFolderBitmap(
    context: Context,
    iconSizePx: Int,
    color: Color,
): Bitmap? {
    val size = iconSizePx.coerceAtLeast(1)
    return ContextCompat.getDrawable(context, R.drawable.ic_widget_folder)
        ?.mutate()
        ?.apply { setTint(color.toArgb()) }
        ?.toBitmap(width = size, height = size)
}

private fun loadAppIconBitmap(
    context: Context,
    packageName: String,
    iconSizePx: Int,
    iconPackPackage: String?,
    userHandleId: Int? = null,
): Bitmap? {
    // Keep widget app icons in sync with search results: an individually selected
    // icon always wins over the currently applied icon pack.
    val iconOverride = UserAppPreferences(context).getAppIconOverride(packageName)
    val iconPackBitmap =
        iconOverride?.takeUnless { it.useSystemDefault }?.let { override ->
            IconPackManager.loadDrawableBitmap(
                context = context,
                iconPackPackage = requireNotNull(override.iconPackPackage),
                drawableName = requireNotNull(override.drawableName),
            )
        } ?: iconPackPackage?.takeUnless { iconOverride?.useSystemDefault == true }?.let { pack ->
            IconPackManager.loadIconBitmap(context, pack, packageName)
        }
    val hasExplicitIconPackIcon =
        iconOverride?.useSystemDefault == false ||
            iconPackPackage?.let { pack ->
                IconPackManager.hasExplicitIcon(context, pack, packageName)
            } == true
    if (iconPackBitmap != null && (userHandleId == null || hasExplicitIconPackIcon)) {
        val androidBitmap = iconPackBitmap.asAndroidBitmap()
        return Bitmap.createScaledBitmap(
            androidBitmap,
            iconSizePx.coerceAtLeast(1),
            iconSizePx.coerceAtLeast(1),
            true,
        )
    }
    val drawable =
        runCatching { context.packageManager.getApplicationIcon(packageName) }
            .getOrNull()
            ?.let { icon ->
                val profile =
                    userHandleId?.let { UserHandleUtils.of(it) }
                        ?: context.getSystemService(android.os.UserManager::class.java)
                            ?.userProfiles
                            ?.firstOrNull { UserHandleUtils.getIdentifier(it) == userHandleId }
                if (profile == null) {
                    icon
                } else {
                    runCatching { context.packageManager.getUserBadgedIcon(icon, profile) }.getOrDefault(icon)
                }
            }
    return drawable?.toBitmap(
        width = iconSizePx.coerceAtLeast(1),
        height = iconSizePx.coerceAtLeast(1),
    )
}

private fun loadShortcutIconBitmap(
    context: Context,
    action: CustomWidgetButtonAction.AppShortcut,
    iconSizePx: Int,
): Bitmap? {
    action.iconBase64?.let { encoded ->
        val decoded = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
        val bitmap = decoded?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        if (bitmap != null) {
            return Bitmap.createScaledBitmap(
                bitmap,
                iconSizePx.coerceAtLeast(1),
                iconSizePx.coerceAtLeast(1),
                true,
            )
        }
    }
    // Do not restore shortcut icons from persisted resource IDs.
    // Resource IDs can be reassigned after app updates, which causes icon mismatches.
    return null
}

private fun loadContactBitmap(
    context: Context,
    action: CustomWidgetButtonAction.Contact,
    iconSizePx: Int,
    textIconColor: Color,
): Bitmap {
    val photoBitmap =
        action.photoUri?.let { uriString ->
            runCatching {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }

    if (photoBitmap != null) {
        val scaledBitmap =
            Bitmap.createScaledBitmap(
                photoBitmap,
                iconSizePx.coerceAtLeast(1),
                iconSizePx.coerceAtLeast(1),
                true,
            )
        return createCircularBitmap(scaledBitmap)
    }

    val initials = contactInitials(action.displayName)

    if (initials.isBlank()) {
        return createVectorBitmap(Icons.Rounded.Person, iconSizePx)
    }

    // Use Material Theme colors to match the preview (ContactAvatar component)
    // Determine theme based on textIconColor luminance to match widget theme
    val isDarkTheme = textIconColor.luminance() > 0.5f // Light text = dark theme

    val (backgroundColor, textColor) =
        if (isDarkTheme) {
            AppColors.WidgetContactAvatarDarkBackground.toArgb() to
                AppColors.WidgetContactAvatarDarkOnBackground.toArgb()
        } else {
            AppColors.WidgetContactAvatarLightBackground.toArgb() to
                AppColors.WidgetContactAvatarLightOnBackground.toArgb()
        }

    return createInitialsBitmap(initials, iconSizePx, backgroundColor, textColor)
}

private fun createInitialsBitmap(
    initials: String,
    sizePx: Int,
    backgroundColor: Int,
    textColor: Int,
): Bitmap {
    val safeSize = sizePx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            style = Paint.Style.FILL
        }

    val radius = safeSize / 2f
    canvas.drawCircle(radius, radius, radius, paint)

    if (initials.isNotBlank()) {
        val textPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = safeSize * 0.6f
            }
        // Add slight stroke for better visibility (semi-bold effect)
        textPaint.strokeWidth = safeSize * 0.02f
        textPaint.style = Paint.Style.FILL_AND_STROKE
        val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initials, radius, textY, textPaint)
    }

    return bitmap
}

private fun createCircularBitmap(sourceBitmap: Bitmap): Bitmap {
    val size = minOf(sourceBitmap.width, sourceBitmap.height)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = android.graphics.Rect(0, 0, size, size)
    val rectF = android.graphics.RectF(rect)

    // Draw circular mask
    paint.style = Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    val radius = size / 2f
    canvas.drawCircle(radius, radius, radius, paint)

    // Use SRC_IN to keep only the intersection of the circle and the bitmap
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(sourceBitmap, null, rect, paint)

    return output
}

private fun createVectorBitmap(
    imageVector: ImageVector,
    sizePx: Int,
): Bitmap {
    val safeSize = sizePx.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // For a simple fallback, create a basic person icon representation
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AppColors.WidgetTextDarkGrey.toArgb()
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
