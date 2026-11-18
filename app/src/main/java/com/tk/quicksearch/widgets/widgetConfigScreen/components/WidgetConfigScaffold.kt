package com.tk.quicksearch.widgets.widgetConfigScreen.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.shared.ui.components.AppMessageSnackbarHost
import com.tk.quicksearch.shared.ui.components.AppTopAppBar
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import com.tk.quicksearch.widgets.utils.WidgetConfigConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetConfigScaffold(
    title: String,
    onCancel: () -> Unit,
    onSave: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    saveLabel: String = stringResource(R.string.dialog_save),
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopAppBar(
                title = title,
                onBack = onCancel,
                navigationIcon = MaterialSymbols.Close,
                navigationContentDescription = stringResource(R.string.dialog_cancel),
                windowInsets = TopAppBarDefaults.windowInsets,
            )
        },
        snackbarHost = { AppMessageSnackbarHost(inScaffold = true) },
        bottomBar = {
            if (onSave != null) {
                // Material 3 bottom bar: a tonal container rather than a shadow.
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth().padding(
                                start = WidgetConfigConstants.BOTTOM_BAR_HORIZONTAL_PADDING,
                                end = WidgetConfigConstants.BOTTOM_BAR_HORIZONTAL_PADDING,
                                top = WidgetConfigConstants.BOTTOM_BAR_VERTICAL_PADDING,
                                bottom = WidgetConfigConstants.BOTTOM_BAR_BOTTOM_PADDING,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        WidgetSaveButton(text = saveLabel, enabled = saveEnabled, onClick = onSave)
                    }
                }
            }
        },
        content = content,
    )
}
