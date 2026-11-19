package com.tk.quicksearch.search.searchScreen.searchScreenLayout.glance

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.ContactBirthday
import com.tk.quicksearch.search.data.ContactBirthdaysRepository
import com.tk.quicksearch.search.data.ContactRepository
import com.tk.quicksearch.search.data.preferences.GlancePreferences
import com.tk.quicksearch.search.models.ContactInfo
import com.tk.quicksearch.shared.ui.icons.MaterialSymbols
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Today's contact birthdays and anniversaries for the home At a Glance card. */
internal class BirthdaysGlance(
    val birthdays: List<ContactBirthday>,
    val open: (ContactBirthday) -> Unit,
    /** Hides the birthday until tomorrow. */
    val dismiss: (ContactBirthday) -> Unit,
)

/**
 * Reads today's birthdays on resume while [enabled], the toggle is on and contacts access is granted.
 * Opening one shows the contact's options drawer through [onShowContactMethods], like a contact result.
 */
@Composable
internal fun rememberBirthdaysGlance(
    enabled: Boolean,
    onShowContactMethods: (ContactInfo) -> Unit,
): BirthdaysGlance {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val repository = remember(appContext) { ContactBirthdaysRepository(appContext) }
    val contactRepository = remember(appContext) { ContactRepository(appContext) }
    val preferences = remember(appContext) { GlancePreferences(appContext) }
    val refreshKey = rememberResumeRefreshKey()
    var birthdays by remember { mutableStateOf<List<ContactBirthday>>(emptyList()) }

    // Not keyed on [enabled]: typing a query hides the rows without reading contacts again.
    LaunchedEffect(refreshKey) {
        birthdays =
            if (preferences.isShowBirthdaysEnabled() && repository.hasPermission()) {
                withContext(Dispatchers.IO) {
                    val today = LocalDate.now()
                    val dismissed = preferences.getDismissedBirthdays(today.toString())
                    repository.birthdaysOn(today).filterNot { it.contactId in dismissed }
                }
            } else {
                emptyList()
            }
    }

    return BirthdaysGlance(
        birthdays = if (enabled) birthdays else emptyList(),
        open = { birthday ->
            scope.launch {
                val contact =
                    withContext(Dispatchers.IO) {
                        contactRepository.getContactsByIds(setOf(birthday.contactId)).firstOrNull()
                    }
                // Falls back to the contacts app if the contact vanished since the card loaded.
                if (contact != null) onShowContactMethods(contact) else repository.open(birthday)
            }
        },
        dismiss = { birthday ->
            preferences.dismissBirthday(LocalDate.now().toString(), birthday.contactId)
            birthdays = birthdays.filterNot { it.contactId == birthday.contactId }
        },
    )
}

@Composable
internal fun BirthdayRow(
    birthday: ContactBirthday,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlanceStatusRow(
        icon = {
            Icon(
                imageVector = MaterialSymbols.Cake,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = birthdayTitle(birthday),
        subtitle = stringResource(R.string.home_birthday_wish),
        onClick = onClick,
        onDismiss = onDismiss,
    )
}

/** "Michael's birthday", or "Michael's 30th birthday" when the contact's date has a year. */
@Composable
private fun birthdayTitle(birthday: ContactBirthday): String {
    val years = birthday.years ?: return stringResource(
        if (birthday.isAnniversary) R.string.home_anniversary_title else R.string.home_birthday_title,
        birthday.name,
    )
    // The ordinal stays in English ("30th") in every language, for simplicity.
    val ordinal = englishOrdinal(years)
    return stringResource(
        if (birthday.isAnniversary) R.string.home_anniversary_title_ordinal else R.string.home_birthday_title_ordinal,
        birthday.name,
        ordinal,
    )
}

internal fun englishOrdinal(number: Int): String {
    val suffix =
        if (number % 100 in 11..13) {
            "th"
        } else {
            when (number % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
    return "$number$suffix"
}
