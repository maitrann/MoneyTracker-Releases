package com.personal.moneytracker.domain.matching

import com.personal.moneytracker.data.local.entity.CanonicalTransaction
import com.personal.moneytracker.data.local.entity.ParsedObservation

data class MatchScore(val score: Double, val reasons: List<String>) {
    val isConservativeMatch: Boolean get() = score >= 85.0 && reasons.any { it.startsWith("reference") || it.startsWith("known chain") }
}

/** Phase 4 accepts a known, evidence-backed source route; amount and time never stand alone. */
class MatchScorer {
    fun score(observation: ParsedObservation, transaction: CanonicalTransaction, linked: List<ParsedObservation>): MatchScore {
        val primary = linked.firstOrNull() ?: return MatchScore(0.0, listOf("no linked observation"))
        val reasons = mutableListOf<String>()
        var score = 0.0
        if (observation.amountVnd == transaction.amountVnd) { score += 50; reasons += "exact amount" } else return MatchScore(-100.0, listOf("amount differs"))
        val seconds = kotlin.math.abs(observation.eventAtEpochMs - transaction.occurredAtEpochMs) / 1_000
        if (seconds <= 60) { score += 25; reasons += "timestamp within 60 seconds" }
        else if (seconds <= 180) { score += 15; reasons += "timestamp within 180 seconds" }
        else if (seconds <= 300) { score += 5; reasons += "timestamp within 5 minutes" }
        else return MatchScore(-100.0, listOf("outside correlation window"))
        val reference = observation.referenceHint?.takeIf { it.isNotBlank() }
        if (reference != null && reference == primary.referenceHint) { score += 60; reasons += "reference hint matches" }
        if (observation.merchant != null && transaction.merchant != null && observation.merchant != transaction.merchant) return MatchScore(-100.0, listOf("conflicting merchant"))
        if (observation.merchant != null && observation.merchant == transaction.merchant) { score += 15; reasons += "merchant matches" }
        val sourceApps = linked.map { it.sourceApp }.toSet()
        if (sourceApps.any { compatibleRoute(it, observation.sourceApp) }) { score += 30; reasons += "known chain compatibility" }
        return MatchScore(score, reasons)
    }

    private fun compatibleRoute(left: com.personal.moneytracker.domain.parser.SourceApp, right: com.personal.moneytracker.domain.parser.SourceApp): Boolean {
        val bankToMomo = setOf(com.personal.moneytracker.domain.parser.SourceApp.TECHCOMBANK, com.personal.moneytracker.domain.parser.SourceApp.TIMO)
        return (left in bankToMomo && right == com.personal.moneytracker.domain.parser.SourceApp.MOMO) ||
            (right in bankToMomo && left == com.personal.moneytracker.domain.parser.SourceApp.MOMO) ||
            (left == com.personal.moneytracker.domain.parser.SourceApp.MOMO && right == com.personal.moneytracker.domain.parser.SourceApp.GRAB) ||
            (right == com.personal.moneytracker.domain.parser.SourceApp.MOMO && left == com.personal.moneytracker.domain.parser.SourceApp.GRAB)
    }
}
