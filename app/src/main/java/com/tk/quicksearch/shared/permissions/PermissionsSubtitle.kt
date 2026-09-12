package com.tk.quicksearch.shared.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.tk.quicksearch.R

@Composable
fun permissionsScreenSubtitle(): AnnotatedString {
    val text = stringResource(R.string.permissions_screen_subtitle)
    val emphasis = stringResource(R.string.permissions_screen_optional_emphasis)

    return remember(text, emphasis) {
        val emphasisStart = text.indexOf(emphasis)
        if (emphasisStart < 0) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text.substring(0, emphasisStart))
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(emphasis)
                }
                append(text.substring(emphasisStart + emphasis.length))
            }
        }
    }
}
