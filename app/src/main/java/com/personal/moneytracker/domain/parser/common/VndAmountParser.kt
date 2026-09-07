package com.personal.moneytracker.domain.parser.common

sealed interface AmountParseResult {
    data class Found(val amountVnd: Long, val signed: Boolean) : AmountParseResult
    data object Missing : AmountParseResult
    data class Ambiguous(val candidates: List<Long>) : AmountParseResult
}

object VndAmountParser {
    private val candidate = Regex("(?<![\\p{L}\\d])([+-]?)\\s*(\\d{1,3}(?:(?:[., ]\\d{3})+)|\\d+)(?:\\s*(đ|₫|vnd))?", RegexOption.IGNORE_CASE)

    fun parse(text: String): AmountParseResult {
        val matches = candidate.findAll(text).mapNotNull { match ->
            val rawNumber = match.groupValues[2]
            val digits = rawNumber.replace(Regex("[., ]"), "")
            val value = digits.toLongOrNull() ?: return@mapNotNull null
            Candidate(
                signedValue = if (match.groupValues[1] == "-") -value else value,
                isSigned = match.groupValues[1].isNotEmpty(),
                hasCurrency = match.groupValues[3].isNotEmpty(),
            )
        }.toList()

        if (matches.isEmpty()) return AmountParseResult.Missing
        val qualified = matches.filter { it.hasCurrency || it.isSigned }
        if (qualified.isEmpty()) {
            return if (matches.size > 1) AmountParseResult.Ambiguous(matches.map { kotlin.math.abs(it.signedValue) })
            else AmountParseResult.Missing
        }
        val usable = qualified
        if (usable.size != 1) return AmountParseResult.Ambiguous(usable.map { kotlin.math.abs(it.signedValue) })
        val only = usable.single()
        return AmountParseResult.Found(kotlin.math.abs(only.signedValue), only.isSigned)
    }

    private data class Candidate(val signedValue: Long, val isSigned: Boolean, val hasCurrency: Boolean)
}
