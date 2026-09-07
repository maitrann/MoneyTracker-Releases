package com.personal.moneytracker.notification

import java.security.MessageDigest
import java.util.Locale

object NotificationFingerprint {
    fun create(packageName: String, key: String?, title: String?, text: String?, bigText: String?, subText: String?): String {
        val normalized = listOf(packageName, key, title, text, bigText, subText)
            .joinToString("|") { it.orEmpty().trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT) }
        return MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
