package com.tk.quicksearch.widgets.mediaControlsWidget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.tk.quicksearch.widgets.utils.TextIconColorOverride
import com.tk.quicksearch.widgets.utils.WidgetPreferences
import com.tk.quicksearch.widgets.utils.WidgetTheme

private const val LightColorLuminance = 0.5f
private const val ProminentCircleBorderAlpha = 0.35f
private const val ArtPlaceholderAlpha = 0.15f
private const val ArtPlaceholderIconAlpha = 0.7f
private const val SubtitleAlpha = 0.7f

/**
 * Media Controls colors from the chosen background and text/icon colors. Play/pause is a filled
 * circle in the content color, with its icon in whichever of black or white reads on it.
 */
internal data class MediaControlsColors(
    /** Background at full opacity; the transparency setting is applied where it is drawn. */
    val background: Color,
    val content: Color,
) {
    val playPauseCircle: Color get() = content
    val playPauseIcon: Color get() = if (content.luminance() > LightColorLuminance) Color.Black else Color.White
    val subtitle: Color get() = content.copy(alpha = SubtitleAlpha)
    val prominentCircleBorder: Color get() = content.copy(alpha = ProminentCircleBorderAlpha)
    val artPlaceholder: Color get() = content.copy(alpha = ArtPlaceholderAlpha)
    val artPlaceholderIcon: Color get() = content.copy(alpha = ArtPlaceholderIconAlpha)
}

internal fun WidgetPreferences.mediaControlsColors(): MediaControlsColors {
    val background =
        backgroundColor?.let(::Color)
            ?: if (theme == WidgetTheme.LIGHT) Color.White else Color.Black
    val content =
        customTextIconColor?.let(::Color)
            ?: if (textIconColorOverride == TextIconColorOverride.BLACK) Color.Black else Color.White
    return MediaControlsColors(background = background, content = content)
}
