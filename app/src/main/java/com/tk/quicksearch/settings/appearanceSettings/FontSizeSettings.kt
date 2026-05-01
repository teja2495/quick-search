package com.tk.quicksearch.settings.AppearanceSettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tk.quicksearch.R
import com.tk.quicksearch.settings.shared.SettingsCard
import com.tk.quicksearch.settings.shared.SettingsToggleRow
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.util.hapticToggle

@Composable
fun FontSizeCard(
        fontScaleMultiplier: Float,
        onFontScaleMultiplierChange: (Float) -> Unit,
        useSystemFont: Boolean,
        onUseSystemFontChange: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val smallSelected = kotlin.math.abs(fontScaleMultiplier - 0.95f) < 0.001f
    val mediumSelected = kotlin.math.abs(fontScaleMultiplier - 1.0f) < 0.001f
    val bigSelected = kotlin.math.abs(fontScaleMultiplier - 1.05f) < 0.001f

    SettingsCard(modifier = modifier.fillMaxWidth()) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                    text = stringResource(R.string.settings_font_size_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )

            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FontSizeChip(
                            labelRes = R.string.settings_font_size_small,
                            selected = smallSelected,
                            onClick = {
                                hapticToggle(view)()
                                onFontScaleMultiplierChange(0.95f)
                            },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    FontSizeChip(
                            labelRes = R.string.settings_font_size_medium,
                            selected = mediumSelected,
                            onClick = {
                                hapticToggle(view)()
                                onFontScaleMultiplierChange(1.0f)
                            },
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    FontSizeChip(
                            labelRes = R.string.settings_font_size_big,
                            selected = bigSelected,
                            onClick = {
                                hapticToggle(view)()
                                onFontScaleMultiplierChange(1.05f)
                            },
                    )
                }
            }

            SettingsToggleRow(
                    title = stringResource(R.string.settings_use_system_font_title),
                    checked = useSystemFont,
                    onCheckedChange = onUseSystemFontChange,
                    horizontalPadding = 4.dp,
                    showDivider = false,
            )
        }
    }
}

@Composable
private fun FontSizeChip(
        labelRes: Int,
        selected: Boolean,
        onClick: () -> Unit,
) {
    AssistChip(
            onClick = {
                if (!selected) {
                    onClick()
                }
            },
            label = {
                Text(
                        text = stringResource(labelRes),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                )
            },
            shape = RoundedCornerShape(999.dp),
            border =
                    if (selected) {
                        null
                    } else {
                        BorderStroke(1.dp, AppColors.SettingsDivider)
                    },
            colors =
                    AssistChipDefaults.assistChipColors(
                            containerColor =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                            labelColor =
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                    ),
            modifier = Modifier.fillMaxWidth(),
    )
}
