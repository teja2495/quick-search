package com.tk.quicksearch.search.files

import com.tk.quicksearch.shared.ui.components.AppFilterChip
import com.tk.quicksearch.shared.ui.theme.AppShapes
import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.models.FileType
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.DesignTokens

private data class FileTypeChip(
    val fileType: FileType?,
    @StringRes val labelRes: Int,
)

private val FILE_TYPE_CHIPS =
    listOf(
        FileTypeChip(null, R.string.widget_custom_buttons_filter_all),
        FileTypeChip(FileType.DOCUMENTS, R.string.file_type_documents),
        FileTypeChip(FileType.VIDEOS, R.string.file_type_videos),
        FileTypeChip(FileType.PICTURES, R.string.file_type_pictures),
        FileTypeChip(FileType.AUDIO, R.string.file_type_audio),
        FileTypeChip(FileType.APKS, R.string.file_type_apks),
    )

/** Single-select file type chips for the blank-query Files alias view; null means All. */
@Composable
internal fun FileTypeFilterChips(
    selectedFileType: FileType?,
    onFileTypeSelected: (FileType?) -> Unit,
    showWallpaperBackground: Boolean,
    modifier: Modifier = Modifier,
) {
    val unselectedLabelColor =
        if (showWallpaperBackground) AppColors.WallpaperTextPrimary else MaterialTheme.colorScheme.onSurface
    val unselectedBorderColor =
        if (showWallpaperBackground) AppColors.WallpaperDivider else AppColors.SettingsDivider

    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.SpacingSmall),
    ) {
        FILE_TYPE_CHIPS.forEach { chip ->
            val selected = chip.fileType == selectedFileType
            AppFilterChip(
                selected = selected,
                onClick = { if (!selected) onFileTypeSelected(chip.fileType) },
                label = stringResource(chip.labelRes),
                unselectedLabelColor = unselectedLabelColor,
                unselectedBorderColor = unselectedBorderColor,
            )
        }
    }
}
