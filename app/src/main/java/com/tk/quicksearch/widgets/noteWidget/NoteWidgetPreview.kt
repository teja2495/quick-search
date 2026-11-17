package com.tk.quicksearch.widgets.noteWidget

import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.theme.DesignTokens
import com.tk.quicksearch.widgets.utils.WidgetPreferences

private val PreviewWallpaperPadding = 16.dp

/** Settings-screen preview of [NoteWidget], drawn over the wallpaper like the other widget previews. */
@Composable
fun NoteWidgetPreview(
    state: WidgetPreferences,
    wallpaperBitmap: ImageBitmap?,
) {
    val note = rememberWidgetNote(state.noteId)
    val colors = NoteWidgetColors.forWidget(state, isSystemDark = isSystemInDarkTheme())
    val fontScale = LocalDensity.current.fontScale

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                // Explicit height: paint() without intrinsic sizing expands to the max constraints.
                .height(NoteWidgetDimens.PREVIEW_HEIGHT + PreviewWallpaperPadding * 2)
                .clip(AppShapes.large)
                .then(
                    wallpaperBitmap?.let { bitmap ->
                        Modifier.paint(
                            painter = BitmapPainter(bitmap),
                            sizeToIntrinsics = false,
                            contentScale = ContentScale.Crop,
                        )
                    } ?: Modifier,
                ).padding(PreviewWallpaperPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(NoteWidgetDimens.PREVIEW_HEIGHT)
                    .background(colors.background, RoundedCornerShape(state.borderRadiusDp.dp))
                    .padding(NoteWidgetDimens.CONTENT_PADDING),
        ) {
            val placeholderStyle = TextStyle(color = colors.placeholder, fontSize = NoteWidgetDimens.BODY_FONT_SIZE)
            when {
                state.noteId == null ->
                    Text(text = stringResource(R.string.widget_note_picker_title), style = placeholderStyle)
                // Still loading.
                note == null -> Unit
                else -> {
                    val title = note.title.takeIf { it.isNotBlank() }
                    if (title != null) {
                        Text(
                            text = title,
                            style =
                                TextStyle(
                                    color = colors.title,
                                    fontSize = NoteWidgetDimens.TITLE_FONT_SIZE,
                                    fontWeight = FontWeight.Medium,
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(NoteWidgetDimens.TITLE_BODY_SPACING))
                    }
                    val maxLines =
                        NoteWidgetDimens.bodyMaxLines(
                            heightDp = NoteWidgetDimens.PREVIEW_HEIGHT.value,
                            hasTitle = title != null,
                            fontScale = fontScale,
                        )
                    val body = NoteWidgetDimens.bodyText(note.markdownContent, maxLines)
                    if (body.isBlank()) {
                        Text(
                            text = stringResource(R.string.notes_empty_note_subtext),
                            style = placeholderStyle,
                            maxLines = 1,
                        )
                    } else {
                        Text(
                            text = body,
                            style = TextStyle(color = colors.body, fontSize = NoteWidgetDimens.BODY_FONT_SIZE),
                            maxLines = maxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
