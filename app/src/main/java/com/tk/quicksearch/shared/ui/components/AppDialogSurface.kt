package com.tk.quicksearch.shared.ui.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tk.quicksearch.shared.ui.theme.AppColors
import com.tk.quicksearch.shared.ui.theme.AppShapes

/** Standard frame for centered dialogs whose content supplies its own layout and actions. */
@Composable
fun AppDialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    PreserveHostKeyboardStateEffect()
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(
            modifier = modifier,
            shape = AppShapes.extraLarge,
            color = AppColors.DialogBackground,
            content = content,
        )
    }
}
