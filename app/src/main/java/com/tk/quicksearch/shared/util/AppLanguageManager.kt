package com.tk.quicksearch.shared.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.tk.quicksearch.R
import com.tk.quicksearch.search.data.preferences.BootstrapPreferences
import java.util.Locale

data class AppLanguageOption(
    val languageTag: String?,
    val label: String,
    val supportingLabel: String? = null,
)

object AppLanguageManager {
    private val supportedLanguageTags =
        listOf(
            "en",
            "ar",
            "de",
            "el",
            "es",
            "fr",
            "hi",
            "it",
            "pt-BR",
            "ru",
            "te",
            "tr",
            "zh-CN",
        )

    fun applySavedAppLanguage(context: Context) {
        val savedLanguageTag = BootstrapPreferences.getAppLanguageTag(context)
        val targetLocales = savedLanguageTag.toLocaleListCompat()
        if (AppCompatDelegate.getApplicationLocales() == targetLocales) return

        val activeResourceLanguage =
            context.resources.configuration.locales.get(0)?.language.orEmpty()
        val savedLanguage = savedLanguageTag?.let(Locale::forLanguageTag)?.language.orEmpty()
        if (savedLanguage.isNotEmpty() && activeResourceLanguage == savedLanguage) return

        AppCompatDelegate.setApplicationLocales(targetLocales)
    }

    fun wrapContext(context: Context): Context {
        val languageTag = BootstrapPreferences.getAppLanguageTag(context)
        if (languageTag.isNullOrBlank()) return context

        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        }
        return context.createConfigurationContext(configuration)
    }

    fun setAppLanguage(
        context: Context,
        languageTag: String?,
    ) {
        BootstrapPreferences.setAppLanguageTag(context, languageTag)
        // Keep the legacy value for one release so downgrade and v1 backup behavior is stable.
        com.tk.quicksearch.search.data.UserAppPreferences(context).setAppLanguageTag(languageTag)
        AppCompatDelegate.setApplicationLocales(languageTag.toLocaleListCompat())
        context.findActivity()?.recreate()
    }

    fun getSelectedLanguageTag(context: Context): String? {
        val appLocales = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return when {
            appLocales.isBlank() -> BootstrapPreferences.getAppLanguageTag(context)
            else -> appLocales.substringBefore(',').ifBlank { null }
        }
    }

    fun getAvailableLanguages(context: Context): List<AppLanguageOption> {
        val systemDefaultLabel = context.getString(R.string.settings_app_language_system_default)
        return buildList {
            add(
                AppLanguageOption(
                    languageTag = null,
                    label = systemDefaultLabel,
                ),
            )
            supportedLanguageTags.forEach { languageTag ->
                val locale = Locale.forLanguageTag(languageTag)
                val nativeLabel = locale.toLanguageOptionLabel()
                val englishLabel = locale.getDisplayName(Locale.ENGLISH).toTitleCase(locale = Locale.ENGLISH)
                add(
                    AppLanguageOption(
                        languageTag = languageTag,
                        label = nativeLabel,
                        supportingLabel = englishLabel.takeIf { it != nativeLabel },
                    ),
                )
            }
        }
    }

    fun getSelectedLanguageLabel(context: Context): String {
        val selectedLanguageTag = getSelectedLanguageTag(context)
        return getAvailableLanguages(context)
            .firstOrNull { it.languageTag == selectedLanguageTag }
            ?.label
            ?: context.getString(R.string.settings_app_language_system_default)
    }

    private fun String?.toLocaleListCompat(): LocaleListCompat =
        if (this.isNullOrBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(this)
        }

    private fun Locale.toLanguageOptionLabel(): String = getDisplayName(this).toTitleCase(this)

    private tailrec fun Context.findActivity(): Activity? =
        when (this) {
            is Activity -> this
            is ContextWrapper -> baseContext.findActivity()
            else -> null
        }

    private fun String.toTitleCase(locale: Locale): String =
        replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(locale)
            } else {
                char.toString()
            }
        }
}
