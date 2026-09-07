package com.personal.moneytracker.domain.parser.common

import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import java.text.Normalizer
import java.util.Locale

object NotificationTextNormalizer {
    fun combine(event: RawNotificationEvent): String = listOf(event.title, event.text, event.bigText, event.subText)
        .filterNotNull()
        .map(::normalizeDisplay)
        .filter(String::isNotBlank)
        .distinct()
        .joinToString("\n")

    fun normalizeDisplay(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .replace('\u00A0', ' ')
        .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
        .replace(Regex(" *\n+ *"), "\n")
        .trim()

    fun normalizeForMatching(value: String): String = Normalizer.normalize(normalizeDisplay(value), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
}
