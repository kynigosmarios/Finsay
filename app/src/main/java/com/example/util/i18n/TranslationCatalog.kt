package com.example.util.i18n

import com.example.util.Localization
import java.util.Locale

internal object TranslationCatalog {
    fun lookup(lang: Localization.Language, en: String, el: String): String {
        return when (lang) {
            Localization.Language.ENGLISH -> en
            Localization.Language.GREEK -> el
            Localization.Language.SPANISH -> StringsEs.map[en] ?: en
            Localization.Language.FRENCH -> StringsFr.map[en] ?: en
            Localization.Language.GERMAN -> StringsDe.map[en] ?: en
            Localization.Language.ITALIAN -> StringsIt.map[en] ?: en
            Localization.Language.PORTUGUESE -> StringsPt.map[en] ?: en
            Localization.Language.DUTCH -> StringsNl.map[en] ?: en
            Localization.Language.POLISH -> StringsPl.map[en] ?: en
            Localization.Language.ROMANIAN -> StringsRo.map[en] ?: en
        }
    }

    fun format(
        lang: Localization.Language,
        enTemplate: String,
        elTemplate: String,
        vararg args: Any
    ): String {
        val template = lookup(lang, enTemplate, elTemplate)
        return try {
            String.format(Localization.getDateLocale(lang), template, *args)
        } catch (_: Throwable) {
            try {
                String.format(Locale.US, enTemplate, *args)
            } catch (_: Throwable) {
                template
            }
        }
    }

    fun mapFor(lang: Localization.Language): Map<String, String> = when (lang) {
        Localization.Language.SPANISH -> StringsEs.map
        Localization.Language.FRENCH -> StringsFr.map
        Localization.Language.GERMAN -> StringsDe.map
        Localization.Language.ITALIAN -> StringsIt.map
        Localization.Language.PORTUGUESE -> StringsPt.map
        Localization.Language.DUTCH -> StringsNl.map
        Localization.Language.POLISH -> StringsPl.map
        Localization.Language.ROMANIAN -> StringsRo.map
        else -> emptyMap()
    }
}
