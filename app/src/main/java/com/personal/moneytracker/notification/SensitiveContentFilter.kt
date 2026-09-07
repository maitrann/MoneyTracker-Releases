package com.personal.moneytracker.notification

import java.text.Normalizer
import java.util.Locale

object SensitiveContentFilter {
    private val phrases = listOf(
        "otp", "one time password", "verification code", "smart otp",
        "ma xac thuc", "ma otp", "do not share", "khong chia se", "passcode",
    )

    fun isSensitive(vararg values: String?): Boolean {
        val normalized = values.filterNotNull().joinToString(" ").normalize()
        return phrases.any { phrase -> Regex("(^|[^a-z])${Regex.escape(phrase)}([^a-z]|$)").containsMatchIn(normalized) }
    }

    private fun String.normalize(): String = Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
}
