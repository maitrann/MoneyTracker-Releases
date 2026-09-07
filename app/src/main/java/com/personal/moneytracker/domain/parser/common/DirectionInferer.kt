package com.personal.moneytracker.domain.parser.common

import com.personal.moneytracker.domain.parser.DirectionHint

object DirectionInferer {
    private val transfer = listOf("transfer", "chuyen tien", "chuyển tiền")
    private val debit = listOf("debit", "paid", "payment", "thanh toán", "trừ tiền")
    private val credit = listOf("credit", "received", "nhận tiền", "cộng tiền")

    fun infer(text: String): DirectionHint {
        val normalized = NotificationTextNormalizer.normalizeForMatching(text)
        val hasTransfer = transfer.any { NotificationTextNormalizer.normalizeForMatching(it) in normalized }
        val hasDebit = debit.any { NotificationTextNormalizer.normalizeForMatching(it) in normalized }
        val hasCredit = credit.any { NotificationTextNormalizer.normalizeForMatching(it) in normalized }
        if (hasTransfer && !hasDebit && !hasCredit) return DirectionHint.TRANSFER
        if (hasDebit.xor(hasCredit)) return if (hasDebit) DirectionHint.DEBIT else DirectionHint.CREDIT

        val signs = Regex("(?<!\\d)([+-])\\s*\\d{1,3}(?:(?:[., ]\\d{3})+|\\d*)").findAll(text)
            .map { it.groupValues[1] }.distinct().toList()
        return when (signs.singleOrNull()) {
            "-" -> DirectionHint.DEBIT
            "+" -> DirectionHint.CREDIT
            else -> DirectionHint.UNKNOWN
        }
    }
}
